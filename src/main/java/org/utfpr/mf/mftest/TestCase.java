package org.utfpr.mf.mftest;

import lombok.*;
import org.jetbrains.annotations.Nullable;
import org.utfpr.mf.MockLayer;
import org.utfpr.mf.enums.DefaultInjectParams;
import org.utfpr.mf.mftest.model.TestResult;
import org.utfpr.mf.mftest.model.WorkloadData;
import org.utfpr.mf.mftest.service.TestResultService;
import org.utfpr.mf.migration.IMfBinder;
import org.utfpr.mf.migration.IMfStepObserver;
import org.utfpr.mf.migration.MfMigrationStepFactory;
import org.utfpr.mf.migration.MfMigrator;
import org.utfpr.mf.migration.params.*;
import org.utfpr.mf.model.Credentials;
import org.utfpr.mf.mongoConnection.MongoConnection;
import org.utfpr.mf.mongoConnection.MongoConnectionCredentials;
import org.utfpr.mf.prompt.Framework;
import org.utfpr.mf.tools.CodeSession;

import java.util.List;


@Getter
@Setter
@Builder
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

    protected TestCase(String name,
                       Credentials credentials, MigrationSpec migrationSpec,
                       IMfBinder binder, TestResultService testResultService) {
        super("Test " + name);
        this.name = name;
        this.credentials = credentials;
        this.migrationSpec = migrationSpec;
        this.binder = binder;
        this.testResultService = testResultService;
    }

    public void start() {

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
        MfMigrationStepFactory factory = new MfMigrationStepFactory();
        factory.createAcquireMetadataStep();
        factory.createGenerateModelStep(migrationSpec, mo);
        factory.createGenerateJavaCodeStep(new JavaObserver(this));
        factory.createMigrateDatabaseStep(null, new MigrationObserver(this));
        factory.createValidatorStep(new VerificationObserver(this));

        MockLayer.isActivated = true;

        MfMigrator migrator = new MfMigrator(binder, factory);
        BEGIN("Executing");
        var result = migrator.execute(credentials);
        // TODO: Treat errors were (expose the generated classes)

        persist();
    }

    private void persist() {
        BEGIN("Saving result");
        TestResult tr = new TestResult(
                name,
                (String) binder.get("prompt"),
                (String) binder.get("llm_response"),
                migrationSpec.getLLM(),
                generatedModel.getModel(),
                generatedJavaCode.getFullSourceCode(),
                success,
                migrationSpec.getWorkload().stream().map(WorkloadData::new).toList()
        );
        testResultService.save(tr);
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
