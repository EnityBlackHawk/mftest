package org.utfpr.mf.mftest.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TestTypeRef {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @ManyToOne
    @JoinColumn(name = "references_id")
    private TestResult references;
    @ManyToOne
    @JoinColumn(name = "embedded_id")
    private TestResult embedded;

}
