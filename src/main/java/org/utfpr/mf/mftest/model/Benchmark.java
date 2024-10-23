package org.utfpr.mf.mftest.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "benchmark")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Benchmark {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(columnDefinition = "SERIAL")
    private Integer id;
    private String name;
    @Column(columnDefinition = "REAL")
    private double ms;
    @JoinColumn(name = "test_id")
    @ManyToOne
    private TestResult result;
    @Column(columnDefinition = "text")
    private String log;

}
