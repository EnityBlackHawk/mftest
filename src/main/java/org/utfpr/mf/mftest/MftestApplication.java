package org.utfpr.mf.mftest;

import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Projections;
import org.bson.Document;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.utfpr.mf.MockLayer;
import org.utfpr.mf.enums.DefaultInjectParams;
import org.utfpr.mf.mftest.service.BenchmarkService;
import org.utfpr.mf.mftest.service.TestResultService;
import org.utfpr.mf.interfaces.IMfBinder;
import org.utfpr.mf.interfaces.IMfStepObserver;
import org.utfpr.mf.migration.MfMigrationStepFactory;
import org.utfpr.mf.migration.MfMigrator;
import org.utfpr.mf.migration.params.MetadataInfo;
import org.utfpr.mf.migration.params.MigrationSpec;
import org.utfpr.mf.migration.params.Model;
import org.utfpr.mf.model.Credentials;
import org.utfpr.mf.model.MongoQuery;
import org.utfpr.mf.prompt.Framework;
import org.utfpr.mf.migration.params.MigrationSpec.Workload;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
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


    public static void main(String[] args) throws FileNotFoundException {
        var context = SpringApplication.run(MftestApplication.class, args);
        var testResultService = context.getBean(TestResultService.class);
        var benchmarkService = context.getBean(BenchmarkService.class);
        List<String> selects = WorkloadLoader.getSelects("src/main/resources/simpleWorkload.sql");

        assert selects.size() == 5 : "Expected 5 selects, got " + selects.size();

        Credentials credentials = new Credentials(
                "jdbc:postgresql://localhost:5432/airport3",
                "admin",
                "admin");

        IMfBinder binder = new MfMigrator.Binder();
        binder.bind(DefaultInjectParams.LLM_KEY.getValue(), System.getenv("LLM_KEY"));

        ArrayList<TestCase> tests = new ArrayList<>();
        tests.add(generateTest1(credentials, selects, binder, testResultService));
        //tests.add(generateTest2(credentials, selects, binder, testResultService));
        for (var test : tests) {
            test.start();
            var result = test.runBenchmark(List.of(MongoEmbedded.query1, MongoEmbedded.query2, MongoEmbedded.query3, MongoEmbedded.query4));
            result.forEach(benchmarkService::save);
        }
    }

    public static TestCase generateTest1(Credentials cred, List<String> selects, IMfBinder binder, TestResultService service) {
        MigrationSpec spec = MigrationSpec.builder()
                .LLM("gpt-4o-mini")
                .framework(Framework.SPRING_DATA)
                .allow_ref(true)
                .prioritize_performance(true)
                .name("B")
                .workload(List.of(
                        new Workload(30, selects.get(0)),
                        new Workload(15, selects.get(1)),
                        new Workload(25, selects.get(2)),
                        new Workload(10, selects.get(3)),
                        new Workload(20, selects.get(4))
                ))
                .build();
        return new TestCase("B", cred, spec, binder, service);
    }

    public static TestCase generateTest2(Credentials cred, List<String> selects, IMfBinder binder, TestResultService service) {
        MigrationSpec spec = MigrationSpec.builder()
                .LLM("gpt-4o-mini")
                .framework(Framework.SPRING_DATA)
                .allow_ref(true)
                .prioritize_performance(false)
                .name("References")
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
