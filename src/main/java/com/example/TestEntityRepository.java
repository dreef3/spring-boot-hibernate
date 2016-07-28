package com.example;

import org.springframework.data.repository.CrudRepository;

public interface TestEntityRepository extends CrudRepository<TestEntity, Long> {
    TestEntity findOneByCode(String code);
}
