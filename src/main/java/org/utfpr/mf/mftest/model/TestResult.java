package org.utfpr.mf.mftest.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
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

    public TestResult(String name, String prompt, String response, String llmModel, String generatedModel, String javaCode, boolean runSuccess, List<WorkloadData> workload) {
        this.name = name;
        this.prompt = prompt;
        this.response = response;
        this.llmModel = llmModel;
        this.generatedModel = generatedModel;
        this.javaCode = javaCode;
        this.runSuccess = runSuccess;
        this.workload = workload;
        this.date = new Date();
    }

}
