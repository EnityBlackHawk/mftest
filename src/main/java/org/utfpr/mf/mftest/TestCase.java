package org.utfpr.mf.mftest;

import kotlin.Pair;
import lombok.*;
import org.jetbrains.annotations.Nullable;
import org.utfpr.mf.MockLayer;
import org.utfpr.mf.enums.DefaultInjectParams;
import org.utfpr.mf.metadata.DbMetadata;
import org.utfpr.mf.mftest.model.Benchmark;
import org.utfpr.mf.mftest.model.RdbBenchmark;
import org.utfpr.mf.mftest.model.TestResult;
import org.utfpr.mf.mftest.model.WorkloadData;
import org.utfpr.mf.mftest.service.TestResultService;
import org.utfpr.mf.interfaces.IMfBinder;
import org.utfpr.mf.interfaces.IMfStepObserver;
import org.utfpr.mf.migration.MfMigrationStepFactory;
import org.utfpr.mf.migration.MfMigrator;
import org.utfpr.mf.migration.params.*;
import org.utfpr.mf.model.Credentials;
import org.utfpr.mf.model.MongoQuery;
import org.utfpr.mf.model.RdbQuery;
import org.utfpr.mf.mongoConnection.MongoConnection;
import org.utfpr.mf.mongoConnection.MongoConnectionCredentials;
import org.utfpr.mf.prompt.Framework;
import org.utfpr.mf.stream.*;
import org.utfpr.mf.tools.CodeSession;
import org.utfpr.mf.tools.QueryResult;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;


@Getter
@Setter
public class TestCase extends CodeSession {

    public static boolean MOCK_LAYER = false;

    private String name;
    private Credentials credentials;
    private MigrationSpec migrationSpec;
    private IMfBinder binder;
    private Model generatedModel;
    private GeneratedJavaCode generatedJavaCode;
    private TestResultService testResultService;
    private boolean success = false;
    private MfPrintStream<String> printStream;
    private TestResult testResult;
    private List<RdbBenchmark> rdbBenchmarks;
    private List<Benchmark> benchmarks;

    protected TestCase(String name,
                       Credentials credentials, MigrationSpec migrationSpec,
                       IMfBinder binder, TestResultService testResultService) {
        super("Test " + name);
        this.name = name;
        this.credentials = credentials;
        this.migrationSpec = migrationSpec;
        this.binder = binder;
        this.testResultService = testResultService;
        this.printStream = new CombinedPrintStream(new StringPrintStream(), new SystemPrintStream());
    }

    public void start() throws FileNotFoundException {

        BEGIN("Initializing test " + name);
        BEGIN("Configuring");
        MongoConnectionCredentials mongoCredentials = new MongoConnectionCredentials("localhost", 27017, "mftest-" + name, null, null);
        MongoConnection mongoConnection = new MongoConnection(mongoCredentials);

        BEGIN_SUB("Clearing Mongo database");
        mongoConnection.clearAll();

        BEGIN_SUB("Binding MongoConnection");
        binder.bind(DefaultInjectParams.MONGO_CONNECTION.getValue(), mongoConnection);
        BEGIN_SUB("Binding LLM key");
        binder.bind(DefaultInjectParams.LLM_KEY.getValue(), System.getenv("LLM_KEY"));

        BEGIN_SUB("Creating migration steps");
        ModelObserver mo = new ModelObserver(this);


        MfMigrationStepFactory factory = new MfMigrationStepFactory(printStream);
        factory.createAcquireMetadataStep();
        factory.createGenerateModelStep(migrationSpec, mo);
        factory.createGenerateJavaCodeStep(new JavaObserver(this));
        factory.createMigrateDatabaseStep(null, new MigrationObserver(this));
        factory.createValidatorStep(new VerificationObserver(this));

        MockLayer.isActivated = true;

        MfMigrator migrator = new MfMigrator(binder, factory, printStream);
        BEGIN("Executing");
        var result = migrator.execute(credentials);
        // TODO: Treat errors were (expose the generated classes)

        this.testResult = persist();

    }

    private TestResult persist() {
        BEGIN("Saving result");
        TestResult tr = TestResult.builder()
                .name(name)
                .prompt((String) binder.get("prompt"))
                .response((String) binder.get("llm_response"))
                .llmModel(migrationSpec.getLLM())
                .generatedModel(generatedModel.getModel())
                .javaCode(generatedJavaCode.getFullSourceCode())
                .runSuccess(success)
                .workload( migrationSpec.getWorkload().stream().map(WorkloadData::new).toList())
                .log(printStream.get())
                .date(new Date())
                .mock(MOCK_LAYER)
                .promptDataVersion( (Integer) binder.get(DefaultInjectParams.PROMPT_DATA_VERSION.getValue()))
                .build();
        return testResultService.save(tr);
    }

    public List<Benchmark> runBenchmark(List<MongoQuery> queries) {
        MongoQueryExecutor mqe = new MongoQueryExecutor(
                0,
                queries,
                30,
                (MongoConnection) binder.get(DefaultInjectParams.MONGO_CONNECTION.getValue()));

        var factory = new MfMigrationStepFactory(printStream.clean());
        factory.createBenchmarkStep();

        MfMigrator migrator = new MfMigrator(binder, factory, printStream);
        BenchmarkResultList results = (BenchmarkResultList) migrator.execute(mqe);

        ArrayList<Benchmark> ret = new ArrayList<>();

        var qres = new QueryResult("Query", "Elapsed time");
        for(var x : results) {
            qres.addRow(x.getQueryName(), x.getMilliseconds() + " ms");
        }
        INFO("Result:");
        _printStream.println(qres.toString());

        for(int i = 0; i < results.size(); i++) {
            ret.add(
                    new Benchmark(
                            null,
                            results.get(i).getQueryName(),
                            results.get(i).getMilliseconds(),
                            testResult,
                            printStream.get(),
                            queries.get(i).getQuery().stream().map((j) -> j.toBsonDocument().toJson()).reduce("", String::concat),
                            testResult.getWorkload().get(i)
                    )
            );
        }

        benchmarks = ret;
        return ret;
    }

    public List<RdbBenchmark> runRdbBenchmark(List<RdbQuery> queries) {
        DbMetadata dbm = (DbMetadata) binder.get(DefaultInjectParams.DB_METADATA.getValue());
        RdbQueryExecutor mqe = new RdbQueryExecutor(queries, 30, dbm.getConnection());
        var factory = new MfMigrationStepFactory(printStream.clean());
        factory.createBenchmarkStep();
        MfMigrator migrator = new MfMigrator(binder, factory, printStream);
        BenchmarkResultList results = (BenchmarkResultList) migrator.execute(mqe);

        ArrayList<RdbBenchmark> ret = new ArrayList<>();

        var qres = new QueryResult("Query", "Elapsed time");
        for(var x : results) {
            qres.addRow(x.getQueryName(), x.getMilliseconds() + " ms");
        }
        INFO("Result:");
        _printStream.println(qres.toString());

        for(int i = 0; i < results.size(); i++) {
            ret.add(
                    new RdbBenchmark(
                            null,
                            results.get(i).getQueryName(),
                            results.get(i).getMilliseconds(),
                            testResult,
                            printStream.get(),
                            testResult.getWorkload().get(i)
                    )
            );
        }

        rdbBenchmarks = ret;
        return ret;
    }


    protected static class ModelObserver implements IMfStepObserver<MetadataInfo, Model> {

        private TestCase testCase;

        public ModelObserver(TestCase testCase) {
            this.testCase = testCase;
        }

        @Override
        public boolean OnStepStart(String s, MetadataInfo o) {

            if(!MOCK_LAYER) {
                System.out.println("Disabling MockLayer");
                MockLayer.isActivated = false;
            }
            return true;
        }

        @Override
        public boolean OnStepEnd(String s, Model o) {
            System.out.println("Enable MockLayer");
            testCase.generatedModel = o;
            MockLayer.isActivated = true;
            return false;
        }

        @Override
        public boolean OnStepCrash(String s, Throwable throwable) {
            return false;
        }

        @Override
        public boolean OnStepError(String s, String s1) {
            return false;
        }
    }
    protected static class JavaObserver implements IMfStepObserver<Model, GeneratedJavaCode> {

            private TestCase testCase;

            public JavaObserver(TestCase testCase) {
                this.testCase = testCase;
            }

            @Override
            public boolean OnStepStart(String s, Model o) {
                if(!MOCK_LAYER) {
                    System.out.println("Disabling MockLayer");
                    MockLayer.isActivated = false;
                }
                return true;
            }

            @Override
            public boolean OnStepEnd(String s, GeneratedJavaCode o) {
                System.out.println("Enable MockLayer");
                testCase.generatedJavaCode = o;
                // testCase.persist();
                MockLayer.isActivated = true;
                return false;
            }

            @Override
            public boolean OnStepCrash(String s, Throwable throwable) {
                return false;
            }

            @Override
            public boolean OnStepError(String s, String s1) {
                return false;
            }
    }
    protected static class MigrationObserver implements IMfStepObserver<GeneratedJavaCode, MigrationDatabaseReport> {
        private TestCase testCase;

        public MigrationObserver(TestCase testCase) {
            this.testCase = testCase;
        }


        @Override
        public boolean OnStepStart(String s, GeneratedJavaCode generatedJavaCode) {
            return false;
        }

        @Override
        public boolean OnStepEnd(String s, MigrationDatabaseReport migrationDatabaseReport) {
            testCase.success = true;
            return false;
        }

        @Override
        public boolean OnStepCrash(String s, Throwable throwable) {
            testCase.success = false;
            return false;
        }

        @Override
        public boolean OnStepError(String s, String s1) {
            testCase.success = false;
            return false;
        }
    }
    protected static class VerificationObserver implements IMfStepObserver<MigrationDatabaseReport, VerificationReport> {
        private TestCase testCase;

        public VerificationObserver(TestCase testCase) {
            this.testCase = testCase;
        }

        @Override
        public boolean OnStepStart(String s, MigrationDatabaseReport migrationDatabaseReport) {
            return false;
        }

        @Override
        public boolean OnStepEnd(String s, VerificationReport verificationReport) {
            return false;
        }

        @Override
        public boolean OnStepCrash(String s, Throwable throwable) {
            return false;
        }

        @Override
        public boolean OnStepError(String s, String s1) {
            return false;
        }
    }

}
