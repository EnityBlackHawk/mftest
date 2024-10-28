package org.utfpr.mf.mftest.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TestResult {

    @Id
    @GeneratedValue
    @Column(columnDefinition = "serial")
    private Integer id;
    private String name;
    @Column(columnDefinition = "text")
    private String prompt;
    @Column(columnDefinition = "text")
    private String response;
    @Column(name = "llm_model")
    private String llmModel;
    @Column(name = "generated_model", columnDefinition = "text")
    private String generatedModel;
    @Column(columnDefinition = "text")
    private String javaCode;
    private boolean runSuccess;
    @OneToMany(cascade = CascadeType.ALL)
    private List<WorkloadData> workload;
    private Date date;
    @Column(name = "log", columnDefinition = "text")
    private String log;
    @Column(name = "mock", columnDefinition = "bool")
    private boolean mock;
    @Column(name = "prompt_data_version", columnDefinition = "INT")
    private Integer promptDataVersion;



}
