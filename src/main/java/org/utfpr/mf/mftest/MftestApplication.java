package org.utfpr.mf.mftest;

import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Projections;
import org.bson.Document;
import org.jetbrains.annotations.NotNull;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.utfpr.mf.MockLayer;
import org.utfpr.mf.enums.DefaultInjectParams;
import org.utfpr.mf.markdown.MarkdownContent;
import org.utfpr.mf.markdown.MarkdownDocument;
import org.utfpr.mf.mftest.model.*;
import org.utfpr.mf.mftest.service.*;
import org.utfpr.mf.interfaces.IMfBinder;
import org.utfpr.mf.interfaces.IMfStepObserver;
import org.utfpr.mf.migration.MfMigrationStepFactory;
import org.utfpr.mf.migration.MfMigrator;
import org.utfpr.mf.migration.params.BenchmarkResult;
import org.utfpr.mf.migration.params.MetadataInfo;
import org.utfpr.mf.migration.params.MigrationSpec;
import org.utfpr.mf.migration.params.Model;
import org.utfpr.mf.model.Credentials;
import org.utfpr.mf.model.MongoQuery;
import org.utfpr.mf.model.RdbQuery;
import org.utfpr.mf.prompt.Framework;
import org.utfpr.mf.migration.params.MigrationSpec.Workload;
import org.utfpr.mf.tools.QueryResult;
import org.utfpr.mf.tools.TemplatedThread;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static com.mongodb.client.model.Filters.eq;

@SpringBootApplication
public class MftestApplication {

    public static final boolean INSERT_TEST_DATA = true;

    public static void main(String[] args) throws Exception {
        var context = SpringApplication.run(MftestApplication.class, args);

        var testResultService = context.getBean(TestResultService.class);
        var benchmarkService = context.getBean(BenchmarkService.class);
        var rdbBenchmarkService = context.getBean(RdbBenchmarkService.class);
        var testTypeService = context.getBean(TestTypeRefService.class);
        var queryRelService = context.getBean(QueryRelService.class);

        List<String> selects = WorkloadLoader.getSelects("src/main/resources/simpleWorkload.sql");

        assert selects.size() == 5 : "Expected 5 selects, got " + selects.size();

        Credentials credentials = new Credentials(
                //"jdbc:sqlite:/home/luan/.local/share/DBeaverData/workspace6/.metadata/sample-database-sqlite-1/Chinook.db",
                "jdbc:postgresql://localhost:5432/airport3",
                "admin",
                "admin");

        IMfBinder binder1 = new MfMigrator.Binder();
        binder1.bind(DefaultInjectParams.LLM_KEY.getValue(), System.getenv("LLM_KEY"));

        IMfBinder binder2 = new MfMigrator.Binder();
        binder2.bind(DefaultInjectParams.LLM_KEY.getValue(), System.getenv("LLM_KEY"));

        ArrayList<TestCase> tests = new ArrayList<>();


        var test1 = generateTest1(credentials, selects, binder1, testResultService);

        try {
            test1.start();
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }

    }

    @NotNull
    private static MarkdownContent getMarkdownContent(TestCase case1, TestCase case2) {
        var bench_embedded = case1.getBenchmarks();
        var bench_ref = case2.getBenchmarks();
        var rdbBench = case1.getRdbBenchmarks();

        MarkdownContent content = new MarkdownContent();
        for (int i = 0; i < rdbBench.size(); i++) {

            RdbBenchmark rdb = rdbBench.get(i);
            Benchmark ref = bench_ref.get(i);
            Benchmark embedded = bench_embedded.get(i);

            content.addTitle3(rdb.getName());
            content.addCodeBlock(rdb.getQuery().getQuery(), "sql");

            QueryResult table = new QueryResult("Database", "Execution time (ms)");
            table.addRow("Postgres", String.valueOf(rdb.getMs()));
            table.addRow("MongoDB References", String.valueOf(ref.getMs()));
            table.addRow("MongoDB Embedded", String.valueOf(embedded.getMs()));

            content.addTable(table);

        }
        return content;
    }

    public static TestCase generateTest1(Credentials cred, List<String> selects, IMfBinder binder, TestResultService service) {
        MigrationSpec spec = MigrationSpec.builder()
                .LLM("gpt-4o-mini")
                .framework(Framework.SPRING_DATA)
                .allow_ref(true)
                .prioritize_performance(true)
                .name("JAVA4")
                .workload(List.of())
                .build();
        return new TestCase("JAVA4", cred, spec, binder, service);
    }
}
