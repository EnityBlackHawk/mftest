package org.utfpr.mf.mftest;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.utfpr.mf.MockLayer;
import org.utfpr.mf.enums.DefaultInjectParams;
import org.utfpr.mf.mftest.service.TestResultService;
import org.utfpr.mf.interfaces.IMfBinder;
import org.utfpr.mf.interfaces.IMfStepObserver;
import org.utfpr.mf.migration.MfMigrationStepFactory;
import org.utfpr.mf.migration.MfMigrator;
import org.utfpr.mf.migration.params.MetadataInfo;
import org.utfpr.mf.migration.params.MigrationSpec;
import org.utfpr.mf.migration.params.Model;
import org.utfpr.mf.model.Credentials;
import org.utfpr.mf.prompt.Framework;
import org.utfpr.mf.migration.params.MigrationSpec.Workload;

import java.util.ArrayList;
import java.util.List;

@SpringBootApplication
public class MftestApplication {

    public static final boolean INSERT_TEST_DATA = true;

    public static void main(String[] args) {
        var context = SpringApplication.run(MftestApplication.class, args);
        var testResultService = context.getBean(TestResultService.class);
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
        }
    }

    public static TestCase generateTest1(Credentials cred, List<String> selects, IMfBinder binder, TestResultService service) {
        MigrationSpec spec = MigrationSpec.builder()
                .LLM("gpt-4o-mini")
                .framework(Framework.SPRING_DATA)
                .allow_ref(true)
                .prioritize_performance(true)
                .name("A")
                .workload(List.of(
                        new Workload(30, selects.get(0)),
                        new Workload(15, selects.get(1)),
                        new Workload(25, selects.get(2)),
                        new Workload(10, selects.get(3)),
                        new Workload(20, selects.get(4))
                ))
                .build();
        return new TestCase("A", cred, spec, binder, service);
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
