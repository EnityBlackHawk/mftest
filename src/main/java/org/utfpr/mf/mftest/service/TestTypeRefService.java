package org.utfpr.mf.mftest.service;

import org.springframework.stereotype.Service;
import org.utfpr.mf.mftest.model.TestTypeRef;
import org.utfpr.mf.mftest.repository.TestTypeRefRepository;

@Service
public class TestTypeRefService extends GenericService<TestTypeRef, Integer, TestTypeRefRepository> {


    public TestTypeRefService(TestTypeRefRepository repository) {
        super(repository);
    }
}
