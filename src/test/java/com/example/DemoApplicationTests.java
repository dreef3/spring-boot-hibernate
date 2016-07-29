package com.example;

import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
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
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = DemoApplication.class)
@WebAppConfiguration
@Transactional
public class DemoApplicationTests {
    @Autowired
    TestEntityRepository testEntityRepository;
    @Autowired
    ParentEntityRepository parentEntityRepository;

    @Autowired
    EntityManager em;
    @Autowired
    EntityManagerFactory entityManagerFactory;

    @Autowired
    PlatformTransactionManager txManager;

    private TransactionTemplate txTemplate;
    private Statistics statistics;

    @Before
    public void setup() throws Exception {
        entityManagerFactory.getCache().evictAll();
        if (statistics == null) {
            statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        }
        statistics.clear();

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

        assertThat(testEntity, is(notNullValue()));
        assertL2Cache(1, 1);
    }

    private void assertL2Cache(long put, long hit) {
        assertThat(statistics.getSecondLevelCachePutCount(), is(equalTo(put)));
        assertThat(statistics.getSecondLevelCacheHitCount(), is(equalTo(hit)));
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

        assertThat(testEntity, is(notNullValue()));
        assertL2Cache(1, 1);
    }

    @Test
    public void cacheTestNested() throws Exception {
        TestEntity entity = txTemplate.execute(status -> {
            em.clear();
            TestEntity testEntity = new TestEntity("FOO");
            parentEntityRepository.save(new ParentEntity(testEntity));
            parentEntityRepository.save(new ParentEntity(testEntity));
            em.flush();
            return testEntity;
        });

        final TransactionCallback<Iterable<ParentEntity>> cb = status -> {
            em.clear();
            Iterable<ParentEntity> all = parentEntityRepository.findAll();
            em.flush();
            all.forEach(parentEntity -> assertThat(parentEntity.getTestEntity().getCode(), is(equalTo("FOO"))));
            return all;
        };

        txTemplate.execute(cb);
        txTemplate.execute(cb);

        assertL2Cache(1, 2);
    }
}
