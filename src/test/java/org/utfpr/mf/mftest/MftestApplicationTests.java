package org.utfpr.mf.mftest;

import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Projections;
import org.bson.Document;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;
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

import java.util.List;
import java.util.Map;

import static com.mongodb.client.model.Filters.eq;
import static org.bson.codecs.configuration.CodecRegistries.fromProviders;
import static org.bson.codecs.configuration.CodecRegistries.fromRegistries;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
class MftestApplicationTests {

    @Autowired
    private MongoClient mongo;

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
    void aggregate() {
        MongoConnection mongoConnection = new MongoConnection(new MongoConnectionCredentials("localhost", 27017, "mftest-A", null, null));
        var db = mongoConnection.getClient().getDatabase(mongoConnection.getCredentials().getDatabase());
        var collection = db.getCollection("booking");
        collection.aggregate(
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
        ).forEach((x) -> System.out.println(x.toJson()));

    }


}
