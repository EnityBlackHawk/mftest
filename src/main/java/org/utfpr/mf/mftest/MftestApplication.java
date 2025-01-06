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

    public static class MongoEmbedded {
        public static MongoQuery query1 = new MongoQuery("Embedded 1",  Arrays.asList(new Document("$match",
                        new Document("_id", "FL0018")),
                new Document("$project",
                        new Document("departureTimeScheduled", 1L)
                                .append("gate", 1L)
                                .append("city", "$airportTo.city")
                                .append("aiportTo", "$airportTo._id")
                                .append("airline", "$aircraft.airline.name"))),
                "flight"
        );
        public static MongoQuery query2 = new MongoQuery("Embedded 2", Arrays.asList(new Document("$lookup",
                        new Document("as", "booking")
                                .append("from", "booking")
                                .append("foreignField", "passenger.passportNumber")
                                .append("localField", "passportNumber")),
                new Document("$unwind",
                        new Document("path", "$booking")),
                new Document("$project",
                        new Document("_id", 1L)
                                .append("flight", "$booking.flight._id")
                                .append("seat", "$booking.seat")
                                .append("last_name", "$lastName")
                                .append("first_name", "$firstName"))),
                "passenger"
        );
        public static MongoQuery query3 = new MongoQuery("Embedded 3", List.of(
                Aggregates.match(eq("flight._id", "FL0805")),
                Aggregates.project(
                        Projections.fields(
                                Projections.excludeId(),
                                new Document("flight", "$flight._id"),
                                new Document("firstName", "$passenger.firstName"),
                                new Document("lastName", "$passenger.lastName"),
                                new Document("passportNumber", "$passenger.passportNumber"),
                                new Document("seat", "seat")
                        )
                )
        ), "booking");
        public static MongoQuery query4 = new MongoQuery("Embedded 4", Arrays.asList(new Document("$match",
                        new Document("airportTo._id", "890")),
                new Document("$project",
                        new Document("_id", 0L)
                                .append("flight", "$_id")
                                .append("airport_from", "$airportFrom.name")
                                .append("city_from", "$airportFrom.city")
                                .append("arrival_time_scheduled", "$arrivalTimeScheduled")
                                .append("arrival_time_actual", "$arrivalTimeActual")),
                new Document("$sort",
                        new Document("arrival_time_scheduled", 1L))), "flight");
    }
    public static class MongoReferences {
        public static MongoQuery query1 = new MongoQuery("Reference 1", Arrays.asList(new Document("$lookup",
                        new Document("from", "airport")
                                .append("localField", "airportFrom.$id")
                                .append("foreignField", "_id")
                                .append("as", "airportTo_result")),
                new Document("$lookup",
                        new Document("from", "aircraft")
                                .append("localField", "aircraft.$id")
                                .append("foreignField", "_id")
                                .append("as", "aircraft_result")),
                new Document("$unwind",
                        new Document("path", "$airportTo_result")),
                new Document("$unwind",
                        new Document("path", "$aircraft_result")),
                new Document("$lookup",
                        new Document("from", "airline")
                                .append("localField", "aircraft_result.airline.$id")
                                .append("foreignField", "_id")
                                .append("as", "airline")),
                new Document("$unwind",
                        new Document("path", "$airline")),
                new Document("$project",
                        new Document("departureTimeScheduled", 1L)
                                .append("gate", 1L)
                                .append("city", "$airportTo_result.city")
                                .append("aiportTo", "$airportTo_result._id")
                                .append("airline", "$airline.name"))),
                "flight"
        );

        public static MongoQuery query2 = new MongoQuery("Reference 2", Arrays.asList(new Document("$lookup",
                        new Document("from", "passenger")
                                .append("localField", "passenger.$id")
                                .append("foreignField", "_id")
                                .append("as", "passenger")),
                new Document("$unwind",
                        new Document("path", "$passenger")),
                new Document("$project",
                        new Document("_id", 0L)
                                .append("passenger", "$passenger._id")
                                .append("firstName", "$passenger.firstName")
                                .append("lastName", "$passenger.lastName")
                                .append("flight", "$flight.$id")
                                .append("seat", 1L))), "booking");

        public static MongoQuery query3 = new MongoQuery("Reference 3",  Arrays.asList(new Document("$match",
                        new Document("flight.$id", "FL0930")),
                new Document("$lookup",
                        new Document("as", "flight")
                                .append("from", "flight")
                                .append("foreignField", "_id")
                                .append("localField", "flight.$id")),
                new Document("$lookup",
                        new Document("as", "passenger")
                                .append("from", "passenger")
                                .append("foreignField", "_id")
                                .append("localField", "passenger.$id")),
                new Document("$unwind",
                        new Document("path", "$flight")),
                new Document("$unwind",
                        new Document("path", "$passenger")),
                new Document("$project",
                        new Document("flight", "$flight._id")
                                .append("firstName", "$passenger.firstName")
                                .append("lastName", "$passenger.lastName")
                                .append("passportNumber", "$passenger.passportNumber")
                                .append("seat", "seat"))), "booking");

    }
    public static class RdbQueries {
        public static RdbQuery query1 = new RdbQuery("1", """
                SELECT f.number, f.departure_time_scheduled, f.gate, a_to.city, a_to.id, a_line.name FROM flight f
                    JOIN public.aircraft a on f.aircraft = a.id
                    JOIN public.airport a_to on a_to.id = f.airport_to
                    JOIN public.airline a_line on a.airline = a_line.id
                    WHERE number = 'FL0018';""");
        public static  RdbQuery query2 = new RdbQuery("2", "select p.id, p.first_name, p.last_name, b.flight, b.seat from passenger p join booking b on b.passenger = p.id;");
        public static RdbQuery query3 = new RdbQuery("3", """
                select f."number", p.first_name, p.last_name, p.passport_number, b.seat from flight f\s
                join booking b on b.flight = f.number\s
                join passenger p on b.passenger = p.id where f."number" = 'FL0805';""");
        public static RdbQuery query4 = new RdbQuery("4", """
                select f."number", a.name as airport_from, a.city as city_from, f.arrival_time_scheduled, f.arrival_time_actual
                from flight f join airport a on a.id = f.airport_from
                where f.airport_to = '890' order by f.departure_time_scheduled;""");
    }


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
                "jdbc:sqlite:/home/luan/.local/share/DBeaverData/workspace6/.metadata/sample-database-sqlite-1/Chinook.db",
                //"jdbc:postgresql://localhost:5432/airport3",
                "admin",
                "admin");

        IMfBinder binder1 = new MfMigrator.Binder();
        binder1.bind(DefaultInjectParams.LLM_KEY.getValue(), System.getenv("LLM_KEY"));

        IMfBinder binder2 = new MfMigrator.Binder();
        binder2.bind(DefaultInjectParams.LLM_KEY.getValue(), System.getenv("LLM_KEY"));

        ArrayList<TestCase> tests = new ArrayList<>();

        var th1 = new TemplatedThread<TestCase>(() -> {
            var test1 = generateTest1(credentials, selects, binder1, testResultService);

            try {
                test1.start();
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }
//            var rdbResult = test1.runRdbBenchmark(List.of(RdbQueries.query1, RdbQueries.query2, RdbQueries.query3, RdbQueries.query4));
//            rdbResult.forEach(rdbBenchmarkService::save);
//
//            var result = test1.runBenchmark(List.of(MongoEmbedded.query1, MongoEmbedded.query2, MongoEmbedded.query3, MongoEmbedded.query4));
//            result.forEach(benchmarkService::save);

            return test1;
        });

        var th2 = new TemplatedThread<TestCase>(() -> {

            var test2 = generateTest2(credentials, selects, binder2, testResultService);
            try {
                test2.start();
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }
            var rdbResult = test2.runRdbBenchmark(List.of(RdbQueries.query1, RdbQueries.query2, RdbQueries.query3, RdbQueries.query4));
            rdbResult.forEach(rdbBenchmarkService::save);

            var result = test2.runBenchmark(List.of(MongoReferences.query1, MongoReferences.query2, MongoReferences.query3, MongoEmbedded.query4));
            result.forEach(benchmarkService::save);
            return test2;
        });

        if(true) {
            th1.runAsync();
        }

        if(false) {
            th2.runAsync();
        }

        TestCase case1 = th1.await();
        //TestCase case2 = th2.await();

        TestResult result1 = case1.getTestResult();
        //TestResult result2 = case2.getTestResult();

        var works1 = result1.getWorkload();
        //var works2 = result2.getWorkload();

//        testTypeService.save(
//                TestTypeRef.builder()
//                        .embedded(result1)
//                        .references(result2)
//                        .build()
//        );
//
//        assert works1.size() == works2.size() : "Tamanho nao confere";
//
//        for(int i = 0; i < works1.size(); i++) {
//            queryRelService.save(
//                    QueryRel.builder()
//                            .embedded(works1.get(i))
//                            .reference(works2.get(i))
//                            .build()
//            );
//        }
//
//        MarkdownContent content = getMarkdownContent(case1, case2);
//
//        MarkdownDocument doc = new MarkdownDocument("/home/luan/bench_docs/Result-" + new Date().getTime() + ".md");
//        doc.write(content);
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
                .name("SQLite")
                .workload(List.of(
                        new Workload(30, "SELECT * FROM Album"),
                        new Workload(15, "SELECT * FROM Customer")
                ))
                .build();
        return new TestCase("SQLite-4", cred, spec, binder, service);
    }

    public static TestCase generateTest2(Credentials cred, List<String> selects, IMfBinder binder, TestResultService service) {
        MigrationSpec spec = MigrationSpec.builder()
                .LLM("gpt-4o-mini")
                .framework(Framework.SPRING_DATA)
                .allow_ref(true)
                .prioritize_performance(false)
                .name("SQLite-References")
                .workload(List.of(
                        new Workload(30, selects.get(0)),
                        new Workload(15, selects.get(1)),
                        new Workload(25, selects.get(2)),
                        new Workload(10, selects.get(3)),
                        new Workload(20, selects.get(4))
                ))
                .build();
        return new TestCase("References", cred, spec, binder, service);
    }

}
