package org.utfpr.mf.mftest.service;

import org.utfpr.mf.mftest.model.WorkloadData;
import org.utfpr.mf.mftest.repository.WorkloadDataRepository;

public class WorkloadDataService extends GenericService<WorkloadData, Integer, WorkloadDataRepository> {

    public WorkloadDataService(WorkloadDataRepository repository) {
        super(repository);
    }
}
