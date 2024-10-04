package org.utfpr.mf.mftest;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.utfpr.mf.MockLayer;
import org.utfpr.mf.enums.DefaultInjectParams;
import org.utfpr.mf.migration.IMfBinder;
import org.utfpr.mf.migration.IMfStepObserver;
import org.utfpr.mf.migration.MfMigrationStepFactory;
import org.utfpr.mf.migration.MfMigrator;
import org.utfpr.mf.migration.params.MetadataInfo;
import org.utfpr.mf.migration.params.MigrationSpec;
import org.utfpr.mf.migration.params.Model;
import org.utfpr.mf.model.Credentials;
import org.utfpr.mf.prompt.Framework;
import org.utfpr.mf.migration.params.MigrationSpec.Workload;

import java.util.List;

@SpringBootApplication
public class MftestApplication {

    public static final boolean INSERT_TEST_DATA = true;

    public static void main(String[] args) {
        SpringApplication.run(MftestApplication.class, args);

        List<String> selects = WorkloadLoader.getSelects("src/main/resources/workload.sql");

        assert selects.size() == 5 : "Expected 5 selects, got " + selects.size();

        String testName = "Test A";

        Credentials credentials = new Credentials(
                "jdbc:postgresql://localhost:5432/airport3",
                "admin",
                "admin");

        MigrationSpec spec = MigrationSpec.builder()
                .LLM("gpt-4o-mini")
                .framework(Framework.SPRING_DATA)
                .allow_ref(true)
                .prioritize_performance(true)
                .name(testName)
                .workload(List.of(
                        new Workload(30, selects.get(0)),
                        new Workload(15, selects.get(1)),
                        new Workload(25, selects.get(2)),
                        new Workload(10, selects.get(3)),
                        new Workload(20, selects.get(4))
                ))
                .build();

        IMfBinder binder = new MfMigrator.Binder();
        binder.bind(DefaultInjectParams.LLM_KEY.getValue(), System.getenv("LLM_KEY"));

        MockLayer.isActivated = true;

        IMfStepObserver<MetadataInfo, Model> observer = new IMfStepObserver<>() {
            @Override
            public boolean OnStepStart(String s, MetadataInfo o) {
                System.out.println("Disabling MockLayer");
                MockLayer.isActivated = false;
                return true;
            }

            @Override
            public boolean OnStepEnd(String s, Model o) {
                System.out.println("Enable MockLayer");
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
        };

        MfMigrationStepFactory factory = new MfMigrationStepFactory();
        factory.createAcquireMetadataStep();
        factory.createGenerateModelStep(spec, observer);

        MfMigrator migrator = new MfMigrator(binder, factory);
        Model model = (Model) migrator.execute(credentials);


    }

}
