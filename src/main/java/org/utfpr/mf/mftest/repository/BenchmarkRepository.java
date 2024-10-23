package org.utfpr.mf.mftest.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.utfpr.mf.mftest.model.Benchmark;

@Repository
public interface BenchmarkRepository extends JpaRepository<Benchmark, Integer> {
}
