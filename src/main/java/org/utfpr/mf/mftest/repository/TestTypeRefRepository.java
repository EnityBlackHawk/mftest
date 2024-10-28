package org.utfpr.mf.mftest.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.utfpr.mf.mftest.model.TestTypeRef;

@Repository
public interface TestTypeRefRepository extends JpaRepository<TestTypeRef, Integer> {
}
