package com.example;

import javax.persistence.Cacheable;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

@Entity
@Cacheable
public class TestEntity {
    public TestEntity() {
    }

    public TestEntity(String code) {
        this.code = code;
    }

    public enum LanguageCodes {EN, FR, DE}

    @Id
    @GeneratedValue
    private Long id;

    private String code;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }
}
