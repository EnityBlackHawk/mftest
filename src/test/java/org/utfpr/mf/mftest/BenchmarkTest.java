package org.utfpr.mf.mftest;

import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Projections;
import org.bson.Document;
import org.junit.jupiter.api.Test;
import org.utfpr.mf.interfaces.IMfBinder;
import org.utfpr.mf.interfaces.IMfMigrationStep;
import org.utfpr.mf.metadata.DbMetadata;
import org.utfpr.mf.migration.BenchmarkStep;
import org.utfpr.mf.migration.MfMigrationStepFactory;
import org.utfpr.mf.migration.MfMigrator;
import org.utfpr.mf.migration.params.BenchmarkResultList;
import org.utfpr.mf.migration.params.MongoQueryExecutor;
import org.utfpr.mf.migration.params.RdbQueryExecutor;
import org.utfpr.mf.model.Credentials;
import org.utfpr.mf.model.MongoQuery;
import org.utfpr.mf.model.RdbQuery;
import org.utfpr.mf.mongoConnection.MongoConnection;
import org.utfpr.mf.mongoConnection.MongoConnectionCredentials;
import org.utfpr.mf.tools.DataImporter;
import org.utfpr.mf.tools.QueryResult;

import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

import static com.mongodb.client.model.Filters.eq;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

public class BenchmarkTest {

    private PrintStream log = System.out;

    public BenchmarkTest() throws FileNotFoundException {
    }

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
        public static RdbQuery query1 = new RdbQuery("1", "SELECT f.number, f.departure_time_scheduled, f.gate, a_to.city, a_to.id, a_line.name FROM flight f\n" +
                "    JOIN public.aircraft a on f.aircraft = a.id\n" +
                "    JOIN public.airport a_to on a_to.id = f.airport_to\n" +
                "    JOIN public.airline a_line on a.airline = a_line.id\n" +
                "    WHERE number = 'FL0018';");
        public static  RdbQuery query2 = new RdbQuery("2", "select p.id, p.first_name, p.last_name, b.flight, b.seat from passenger p join booking b on b.passenger = p.id;");
        public static RdbQuery query3 = new RdbQuery("3", "select f.\"number\", p.first_name, p.last_name, p.passport_number, b.seat from flight f \n" +
                "join booking b on b.flight = f.number \n" +
                "join passenger p on b.passenger = p.id where f.\"number\" = 'FL0805';");
        public static RdbQuery query4 = new RdbQuery("4", "select f.\"number\", a.name as airport_from, a.city as city_from, f.arrival_time_scheduled, f.arrival_time_actual\n" +
                "from flight f join airport a on a.id = f.airport_from\n" +
                "where f.airport_to = '890' order by f.departure_time_scheduled;");
    }

    @Test
    void runBenchmark() {

        MongoConnection mongoConnection = new MongoConnection(new MongoConnectionCredentials("localhost", 27017, "mftest-A", null, null));

        MongoQueryExecutor mqe = new MongoQueryExecutor(
                0,
                List.of(MongoEmbedded.query1, MongoEmbedded.query2, MongoEmbedded.query3, MongoEmbedded.query4),
                30,
                mongoConnection
                );


        BenchmarkStep step = new BenchmarkStep(log);

        IMfBinder binder = new MfMigrator.Binder();

        var executor = new MfMigrator(binder, List.of(step), log);

        BenchmarkResultList result = (BenchmarkResultList) executor.execute(mqe);

        var qres = new QueryResult("Query", "Elapsed time");
        for(var x : result) {
            qres.addRow(x.getQueryName(), x.getMilliseconds() + " ms");
        }
        System.out.println("Mongo Embedded:");
        System.out.println(qres.toString());

        assertNotEquals(0, result.size());

    }

    @Test
    void referencesBenchmark() {
        MongoConnection mongoConnection = new MongoConnection(new MongoConnectionCredentials("localhost", 27017, "mftest-References", null, null));

        MongoQueryExecutor mqe = new MongoQueryExecutor(
                0,
                List.of(MongoReferences.query1, MongoReferences.query2, MongoReferences.query3, MongoEmbedded.query4),
                30,
                mongoConnection
        );


        BenchmarkStep step = new BenchmarkStep(log);

        IMfBinder binder = new MfMigrator.Binder();

        var executor = new MfMigrator(binder, List.of(step), log);

        BenchmarkResultList result = (BenchmarkResultList) executor.execute(mqe);

        var qres = new QueryResult("Query", "Elapsed time");
        for(var x : result) {
            qres.addRow(x.getQueryName(), x.getMilliseconds() + " ms");
        }
        System.out.println("Mongo References:");
        System.out.println(qres.toString());

        assertNotEquals(0, result.size());
    }

    @Test
    void rdbBenchmark() throws SQLException, FileNotFoundException {
        Connection conn = DbMetadata.createConnection("jdbc:postgresql://localhost/airport3", "admin", "admin");

        RdbQueryExecutor exec = new RdbQueryExecutor(
            List.of( RdbQueries.query1, RdbQueries.query2, RdbQueries.query3, RdbQueries.query4 ),
                30,
                conn
        );

        MfMigrationStepFactory fac = new MfMigrationStepFactory(log);
        fac.createBenchmarkStep();
        MfMigrator migrator = new MfMigrator(new MfMigrator.Binder(), fac, log);
        BenchmarkResultList result = (BenchmarkResultList) migrator.execute(exec);
        var qres = new QueryResult("Query", "Elapsed time");
        for(var x : result) {
            qres.addRow(x.getQueryName(), x.getMilliseconds() + " ms");
        }
        System.out.println("RDB:");
        System.out.println(qres.toString());

        assertNotEquals(0, result.size());
    }
}
