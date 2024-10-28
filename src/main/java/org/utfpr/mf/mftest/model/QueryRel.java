package org.utfpr.mf.mftest.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "queries_rel")
@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class QueryRel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @ManyToOne()
    @JoinColumn(name = "query_embedded")
    private WorkloadData embedded;
    @ManyToOne()
    @JoinColumn(name =  "query_references")
    private WorkloadData reference;

}
