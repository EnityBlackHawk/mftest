package org.utfpr.mf.mftest;

import org.bson.Document;
import org.junit.jupiter.api.Test;
import org.utfpr.mf.interfaces.IMfBinder;
import org.utfpr.mf.migration.BenchmarkStep;
import org.utfpr.mf.migration.MfMigrator;
import org.utfpr.mf.migration.params.BenchmarkResultList;
import org.utfpr.mf.migration.params.MongoQueryExecutor;
import org.utfpr.mf.model.MongoQuery;
import org.utfpr.mf.mongoConnection.MongoConnection;
import org.utfpr.mf.mongoConnection.MongoConnectionCredentials;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

public class BenchmarkTest {

    @Test
    void runBenchmark() {

        MongoConnection mongoConnection = new MongoConnection(new MongoConnectionCredentials("localhost", 27017, "mftest-A", null, null));

        MongoQueryExecutor mqe = new MongoQueryExecutor(
                0,
                List.of(
                        new MongoQuery("Teste",  Arrays.asList(new Document("$match",
                                        new Document("_id", "FL0018")),
                                new Document("$project",
                                        new Document("departureTimeScheduled", 1L)
                                                .append("gate", 1L)
                                                .append("city", "$airportTo.city")
                                                .append("aiportTo", "$airportTo._id")
                                                .append("airline", "$aircraft.airline.name"))),
                                "flight"
                                )
                ),
                5,
                mongoConnection
                );


        BenchmarkStep step = new BenchmarkStep();

        IMfBinder binder = new MfMigrator.Binder();

        var executor = new MfMigrator(binder, List.of(step) );

        BenchmarkResultList result = (BenchmarkResultList) executor.execute(mqe);

        for(var x : result) {
            System.out.println("Elapsed time: " + x.getMilliseconds());
        }

        assertNotEquals(0, result.size());

    }

}
