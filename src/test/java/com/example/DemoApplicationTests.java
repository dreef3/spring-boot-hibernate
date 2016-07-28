package com.example;

import org.hibernate.SessionFactory;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = DemoApplication.class)
@WebAppConfiguration
@Transactional
public class DemoApplicationTests {
    @Autowired
    TestEntityRepository testEntityRepository;
    @Autowired
    EntityManager em;
    @Autowired
    EntityManagerFactory entityManagerFactory;

    @Autowired
    PlatformTransactionManager txManager;

    private TransactionTemplate txTemplate;

    @Before
    public void setup() throws Exception {
        entityManagerFactory.getCache().evictAll();

        DefaultTransactionDefinition def = new DefaultTransactionDefinition();
        def.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        txTemplate = new TransactionTemplate(txManager, def);
    }

    @Test
    public void cacheTestEntityManager() throws Exception {
        txTemplate.execute(status -> {
            em.clear();
            em.persist(new TestEntity("EN"));
            em.flush();
            return true;
        });

        TestEntity testEntity = txTemplate.execute(status -> {
            em.clear();
            return em.find(TestEntity.class, 1L);
        });

        assertL2Cache(testEntity);
    }

    private void assertL2Cache(TestEntity testEntity) {
        assertThat(testEntity, is(notNullValue()));
        assertThat(entityManagerFactory.unwrap(SessionFactory.class).getStatistics().getSecondLevelCachePutCount(), is(greaterThan(1L)));
        assertThat(entityManagerFactory.unwrap(SessionFactory.class).getStatistics().getSecondLevelCacheHitCount(), is(greaterThan(1L)));
    }

    @Test
    public void cacheTestSpringDataJPA() throws Exception {
        Long id = txTemplate.execute(status -> {
            em.clear();
            TestEntity testEntity = testEntityRepository.save(new TestEntity("FR"));
            em.flush();
            return testEntity.getId();
        });

        TestEntity testEntity = txTemplate.execute(status -> {
            em.clear();
            return testEntityRepository.findOne(id);
        });

        assertL2Cache(testEntity);
    }
}
