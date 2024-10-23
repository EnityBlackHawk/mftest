package org.utfpr.mf.mftest;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.utfpr.mf.mftest.model.Benchmark;
import org.utfpr.mf.mftest.model.RdbBenchmark;
import org.utfpr.mf.mftest.service.BenchmarkService;
import org.utfpr.mf.mftest.service.RdbBenchmarkService;
import org.utfpr.mf.mftest.service.TestResultService;

import static org.bson.assertions.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

@SpringBootTest
public class PersistenceTest {

    @Autowired
    private BenchmarkService benchmarkService;

    @Autowired
    private TestResultService testResultService;

    @Autowired
    private RdbBenchmarkService rdbBenchmarkService;

    @Test
    void persistBenchmark() {

        var test = testResultService.findAll();

        assertNotEquals(0, test.size());

        var bench = new Benchmark(null, "test", 0.0, test.getFirst(), "log");
        var result = benchmarkService.save(bench);

        assertNotNull(result.getId());
        benchmarkService.delete(bench.getId());
    }

    @Test
    void persistRdbBenchmark() {
        var test = testResultService.findAll();

        assertNotEquals(0, test.size());

        var bench = new RdbBenchmark(null, "test", 0.0, test.getFirst(), "log");
        var result = rdbBenchmarkService.save(bench);

        assertNotNull(result.getId());

        rdbBenchmarkService.delete(bench.getId());
    }


}
