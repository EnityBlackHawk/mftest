package org.utfpr.mf.mftest.service;

import org.springframework.data.jpa.repository.JpaRepository;

public class GenericService<TEntity, TPk, TRepository extends JpaRepository<TEntity, TPk>> {

    protected final TRepository repository;

    public GenericService(TRepository repository) {
        this.repository = repository;
    }

    public TEntity save(TEntity entity) {
        return repository.save(entity);
    }

    public void delete(TPk id) {
        repository.deleteById(id);
    }

    public TEntity get(TPk id) {
        return repository.findById(id).orElse(null);
    }

}
