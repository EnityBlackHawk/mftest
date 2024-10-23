package org.utfpr.mf.mftest.service;

import org.springframework.stereotype.Service;
import org.utfpr.mf.mftest.model.RdbBenchmark;
import org.utfpr.mf.mftest.repository.RdbBenchmarkRepository;

@Service
public class RdbBenchmarkService extends GenericService<RdbBenchmark, Integer, RdbBenchmarkRepository> {
    public RdbBenchmarkService(RdbBenchmarkRepository repository) {
        super(repository);
    }
}
