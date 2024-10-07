package org.utfpr.mf.mftest.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.utfpr.mf.mftest.model.TestResult;
import org.utfpr.mf.mftest.repository.TestResultRepository;

@Service
public class TestResultService extends GenericService<TestResult, Integer, TestResultRepository> {

    @Autowired
    public TestResultService(TestResultRepository repository) {
        super(repository);
    }
}
