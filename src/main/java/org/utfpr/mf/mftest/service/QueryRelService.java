package org.utfpr.mf.mftest.service;

import org.springframework.stereotype.Service;
import org.utfpr.mf.mftest.model.QueryRel;
import org.utfpr.mf.mftest.repository.QueryRelRepository;

@Service
public class QueryRelService extends GenericService<QueryRel, Integer, QueryRelRepository> {
    public QueryRelService(QueryRelRepository repository) {
        super(repository);
    }
}
