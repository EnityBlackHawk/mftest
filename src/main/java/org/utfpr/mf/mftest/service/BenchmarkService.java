package org.utfpr.mf.mftest.service;

import org.springframework.stereotype.Service;
import org.utfpr.mf.mftest.model.Benchmark;
import org.utfpr.mf.mftest.repository.BenchmarkRepository;

@Service
public class BenchmarkService extends GenericService<Benchmark, Integer, BenchmarkRepository> {
    public BenchmarkService(BenchmarkRepository repository) {
        super(repository);
    }
}
