package com.example;

import org.springframework.data.repository.CrudRepository;

public interface ParentEntityRepository extends CrudRepository<ParentEntity, Long> {
}
