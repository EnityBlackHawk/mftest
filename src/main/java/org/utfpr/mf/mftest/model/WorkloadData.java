package org.utfpr.mf.mftest.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.utfpr.mf.migration.params.MigrationSpec;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Data
public class WorkloadData {

    @Id
    @GeneratedValue
    @Column(columnDefinition = "serial")
    private Integer id;
    private int regularity;
    @Column(columnDefinition = "text")
    private String query;

    public WorkloadData(MigrationSpec.Workload workload) {
        this.regularity = workload.getRegularity();
        this.query = workload.getQuery();
    }

}
