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
                "jdbc:postgresql://localhost:5432/ticketdb",
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
        String name = String.valueOf(new Date().toInstant().getNano());
        MigrationSpec spec = MigrationSpec.builder()
                .LLM("gpt-o1-mini")
                .framework(Framework.SPRING_DATA)
                .allow_ref(true)
                .prioritize_performance(false)
                .reference_only(true)
                .name(name)
                .workload(List.of(
                        new Workload(25, "SELECT e.eventname, e.starttime, v.venuecity, v.venuestate\n" +
                                "FROM event e\n" +
                                "JOIN venue v ON e.venueid = v.venueid\n" +
                                "WHERE e.starttime > '2005-01-01 00:00'\n" +
                                "  AND v.venuecity = 'San Francisco';"),
                        new Workload(15, "SELECT ec.catname, e.eventname, e.starttime\n" +
                                "FROM event e\n" +
                                "JOIN eventcategory ec ON e.catid = ec.catid\n" +
                                "WHERE ec.catname = 'Musicals';"),
                        new Workload(35, "SELECT l.listid, u.username, l.numtickets, l.priceperticket, l.listtime\n" +
                                "FROM listing l\n" +
                                "JOIN users u ON l.sellerid = u.userid\n" +
                                "WHERE l.eventid = 123\n" +
                                "ORDER BY l.priceperticket ASC;"),
                        new Workload(20, "SELECT d.month, d.year, SUM(s.qtysold) AS total_tickets, SUM(s.pricepaid) AS revenue\n" +
                                "FROM sales s\n" +
                                "JOIN date d ON s.dateid = d.dateid\n" +
                                "GROUP BY d.month, d.year\n" +
                                "ORDER BY d.year DESC, d.month DESC;"),
                        new Workload(15, "SELECT DISTINCT e.eventname, e.starttime, ec.catname\n" +
                                "FROM users u\n" +
                                "JOIN listing l ON u.userid = l.sellerid\n" +
                                "JOIN event e ON l.eventid = e.eventid\n" +
                                "JOIN eventcategory ec ON e.catid = ec.catid\n" +
                                "WHERE u.userid = 395\n" +
                                "  AND (\n" +
                                "    (u.likesports = true AND ec.catgroup = 'Sports') OR\n" +
                                "    (u.likeconcerts = true AND ec.catgroup = 'Concerts') OR\n" +
                                "    (u.liketheater = true AND ec.catgroup = 'Shows')\n" +
                                "  )\n" +
                                "  AND e.starttime > '2005-01-01 00:00';"),
                        new Workload(10, "SELECT e.eventname, SUM(s.qtysold) AS total_tickets, \n" +
                                "       SUM(s.pricepaid) AS total_revenue,\n" +
                                "       SUM(s.pricepaid) - SUM(s.commission) AS net_revenue\n" +
                                "FROM sales s\n" +
                                "JOIN event e ON s.eventid = e.eventid\n" +
                                "GROUP BY e.eventname\n" +
                                "ORDER BY net_revenue DESC;")
                ))
                .build();
        return new TestCase(name, cred, spec, binder, service);
    }
}
