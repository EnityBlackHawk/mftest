package org.utfpr.mf.mftest;

import com.mongodb.ExplainVerbosity;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Projections;
import org.bson.Document;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.query.BasicQuery;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.TextQuery;
import org.utfpr.mf.mongoConnection.MongoConnection;
import org.utfpr.mf.mongoConnection.MongoConnectionCredentials;
import org.utfpr.mf.runtimeCompiler.MfCompilerParams;
import org.utfpr.mf.runtimeCompiler.MfRuntimeCompiler;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static com.mongodb.client.model.Filters.eq;
import static org.bson.codecs.configuration.CodecRegistries.fromProviders;
import static org.bson.codecs.configuration.CodecRegistries.fromRegistries;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
class MftestApplicationTests {

    public static final String MONGO_DATABASE = "mftest-A";

    private MongoConnection mongoConnection;
    private MongoDatabase db;

    @Autowired
    private MongoClient mongo;

    MftestApplicationTests() {
        mongoConnection = new MongoConnection(new MongoConnectionCredentials("localhost", 27017, MONGO_DATABASE, null, null));
        db = mongoConnection.getClient().getDatabase(mongoConnection.getCredentials().getDatabase());
    }

    @Test
    void contextLoads() throws Exception {

        String clazz = """
                import lombok.Data;
                import org.springframework.data.annotation.Id;
                import org.springframework.data.mongodb.core.mapping.Document;
               \s
                @Data
                public class Airline {
                    @Id
                    private String id;
                    private String name;
                   \s
//                    public Airline() {}
//                    public void setName(String name) {
//                        this.name = name;
//                    }
//                    public String getName() {
//                        return name;
//                    }
//                    public void setId(String id) {
//                        this.id = id;
//                    }
//                    public String getId() {
//                        return id;
//                    }
                }
               \s""";

        MfCompilerParams params = MfCompilerParams.builder()
                .classpathBasePath("/home/luan/jars/")
                .classPath(List.of("lombok.jar", "spring-data-commons-3.3.4.jar", "spring-data-mongodb-4.3.4.jar"))
                .build();
        MfRuntimeCompiler compiler = new MfRuntimeCompiler();
        Map<String, Class<?>> classMap = compiler.compile(Map.of("Airline", clazz), params);
        Class<?> airlineClass = classMap.get("Airline");

        CodecRegistry pojoCodecRegistry = fromRegistries(MongoClientSettings.getDefaultCodecRegistry(),
                fromProviders(PojoCodecProvider.builder().automatic(true).build()));


        MongoConnection mongoConnection = new MongoConnection(new MongoConnectionCredentials("localhost", 27017, "mftest-A", null, null));
        var db = mongoConnection.getClient().getDatabase(mongoConnection.getCredentials().getDatabase()).withCodecRegistry(pojoCodecRegistry);
        var field = airlineClass.getDeclaredField("name");
        field.setAccessible(true);
        Query q = new BasicQuery("{'_id' : '2'}");
        var list = mongoConnection.getTemplate().find(q, airlineClass);
        for(var a : list) {
            System.out.println( field.get(a) );
        }
    }

    @Test
    void aggregate1() {

        var collection = db.getCollection("passenger");
        var result = collection.aggregate(
                Arrays.asList(new Document("$lookup",
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
                                        .append("first_name", "$firstName")))
        );
        int count = 0;
        for(var x : result) {
            System.out.println(x.toJson());
            count++;
        }
        assertNotEquals(0, count);
    }

    @Test
    void aggregate2() {

        var collection = db.getCollection("booking");

        var startTime = System.nanoTime();
        var it = collection.aggregate(
                List.of(
                        Aggregates.match(eq("flight._id", "FL0930")),
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
                )
        );
        var endTime = System.nanoTime();
        var elapsedTime = endTime - startTime;

        System.out.println("Elapsed time: " + elapsedTime / Math.pow(10, 6) + "ms");
    }

    @Test
    void aggregate2_ref() {

        var collection = db.getCollection("booking");
        var startTime = System.nanoTime();
        collection.aggregate(
                Arrays.asList(new Document("$match",
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
                                        .append("seat", "seat")))
        );

        var endTime = System.nanoTime();
        var elapsedTime = endTime - startTime;

        System.out.println("Elapsed time: " + elapsedTime / Math.pow(10, 6) + "ms");
    }

    @Test
    void aggregate3() {

        var collection = db.getCollection("flight");

        var startTime = System.nanoTime();

        collection.aggregate(
                Arrays.asList(new Document("$match",
                                        new Document("airportTo._id", "890")),
                                new Document("$project",
                                        new Document("_id", 0L)
                                                .append("flight", "$_id")
                                                .append("airport_from", "$airportFrom.name")
                                                .append("city_from", "$airportFrom.city")
                                                .append("arrival_time_scheduled", "$arrivalTimeScheduled")
                                                .append("arrival_time_actual", "$arrivalTimeActual")),
                                new Document("$sort",
                                        new Document("arrival_time_scheduled", 1L)))
        );

        var endTime = System.nanoTime();
        var elapsedTime = endTime - startTime;

        System.out.println("Elapsed time: " + elapsedTime / Math.pow(10, 6) + "ms");

    }

}
