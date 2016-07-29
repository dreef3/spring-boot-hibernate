package com.example;

import javax.persistence.*;

@Entity
public class ParentEntity {
    @Id
    @GeneratedValue
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private TestEntity testEntity;

    public ParentEntity() {
    }

    public ParentEntity(TestEntity testEntity) {
        this.testEntity = testEntity;
    }

    public TestEntity getTestEntity() {
        return testEntity;
    }

    public void setTestEntity(TestEntity testEntity) {
        this.testEntity = testEntity;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
}
