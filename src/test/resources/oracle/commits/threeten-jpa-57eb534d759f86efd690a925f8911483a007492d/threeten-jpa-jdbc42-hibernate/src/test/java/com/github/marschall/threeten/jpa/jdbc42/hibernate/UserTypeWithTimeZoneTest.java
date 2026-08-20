package com.github.marschall.threeten.jpa.jdbc42.hibernate;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.math.BigInteger;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.TypedQuery;
import javax.sql.DataSource;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.jdbc.datasource.init.DatabasePopulator;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.github.marschall.threeten.jpa.jdbc42.hibernate.configuration.LocalH2Configuration;
import com.github.marschall.threeten.jpa.jdbc42.hibernate.configuration.LocalPostgresConfiguration;

public class UserTypeWithTimeZoneTest {

  private AnnotationConfigApplicationContext applicationContext;
  private TransactionTemplate template;

  public static Stream<Arguments> parameters() {
    return Stream.of(
//            Arguments.of(LocalHsqlConfiguration.class, "threeten-jpa-hibernate-hsql"),
//            Arguments.of(LocalSqlServerConfiguration.class, "threeten-jpa-hibernate-sqlserver"),
//            Arguments.of(LocalDerbyConfiguration.class, "threeten-jpa-hibernate-derby"),
            Arguments.of(LocalH2Configuration.class, "threeten-jpa-hibernate-h2"),
            Arguments.of(LocalPostgresConfiguration.class, "threeten-jpa-hibernate-postgres")
            );
  }

  private void setUp(Class<?> jpaConfiguration, String persistenceUnitName) {
    this.applicationContext = new AnnotationConfigApplicationContext();
    this.applicationContext.register(jpaConfiguration);
    this.applicationContext.refresh();

    PlatformTransactionManager txManager = this.applicationContext.getBean(PlatformTransactionManager.class);
    this.template = new TransactionTemplate(txManager);

    this.template.execute(status -> {
      Map<String, DatabasePopulator> beans = this.applicationContext.getBeansOfType(DatabasePopulator.class);
      DataSource dataSource = this.applicationContext.getBean(DataSource.class);
      try (Connection connection = dataSource.getConnection()) {
        for (DatabasePopulator populator : beans.values()) {
          populator.populate(connection);
        }
      } catch (SQLException e) {
        throw new RuntimeException("could initialize database", e);
      }
      return null;
    });
  }

  private void tearDown() {
    this.applicationContext.close();
  }

  @ParameterizedTest
  @MethodSource("parameters")
  public void read(Class<?> jpaConfiguration, String persistenceUnitName) {
    this.setUp(jpaConfiguration, persistenceUnitName);
    try {

    } finally {
      this.tearDown();
    }
    EntityManagerFactory factory = this.applicationContext.getBean(EntityManagerFactory.class);
    EntityManager entityManager = factory.createEntityManager();
    try {
      // read the entity inserted by SQL
      this.template.execute(status -> {
        TypedQuery<JavaTime42WithZone> query = entityManager.createQuery(
                "SELECT t FROM JavaTime42WithZone t ORDER BY t.id ASC", JavaTime42WithZone.class);
        List<JavaTime42WithZone> resultList = query.getResultList();
        assertThat(resultList, hasSize(2));

        // validate the entity inserted by SQL
        JavaTime42WithZone javaTime = resultList.get(0);
        OffsetDateTime inserted = OffsetDateTime.parse("1960-01-01T23:03:20.123456789+02:30");
        if (jpaConfiguration.getName().contains("Postgres")) {
          // postgres stores in UTC
          OffsetDateTime inUtc = OffsetDateTime.parse("1960-01-01T23:03:20.123457+02:30").withOffsetSameInstant(ZoneOffset.UTC);
          assertEquals(inUtc, javaTime.getOffset());
        } else {
          assertEquals(inserted, javaTime.getOffset());
        }
        javaTime = resultList.get(1);
        inserted = OffsetDateTime.parse("1999-01-23T08:26:56.123456789-05:30");
        if (jpaConfiguration.getName().contains("Postgres")) {
          // postgres stores in UTC
          OffsetDateTime inUtc = OffsetDateTime.parse("1999-01-23T08:26:56.123457-05:30").withOffsetSameInstant(ZoneOffset.UTC);
          assertEquals(inUtc, javaTime.getOffset());
        } else {
          assertEquals(inserted, javaTime.getOffset());
        }
        return null;
      });
    } finally {
      entityManager.close();
      // EntityManagerFactory should be closed by spring.
    }
  }

  @ParameterizedTest
  @MethodSource("parameters")
  public void write(Class<?> jpaConfiguration, String persistenceUnitName) {
    this.setUp(jpaConfiguration, persistenceUnitName);
    try {
      EntityManagerFactory factory = this.applicationContext.getBean(EntityManagerFactory.class);
      EntityManager entityManager = factory.createEntityManager();
      try {
        // insert a new entity into the database
        BigInteger newId = new BigInteger("3");
        OffsetDateTime newOffset = OffsetDateTime.now();

        this.template.execute(status -> {
          JavaTime42WithZone toInsert = new JavaTime42WithZone();
          toInsert.setId(newId);
          toInsert.setOffset(newOffset);
          entityManager.persist(toInsert);
          // the transaction should trigger a flush and write to the database
          return null;
        });

        // validate the new entity inserted into the database
        this.template.execute(status -> {
          JavaTime42WithZone readBack = entityManager.find(JavaTime42WithZone.class, newId);
          assertNotNull(readBack);
          assertEquals(newId, readBack.getId());
          assertEquals(newOffset, readBack.getOffset());
          entityManager.remove(readBack);
          return null;
        });
      } finally {
        entityManager.close();
        // EntityManagerFactory should be closed by spring.
      }
    } finally {
      this.tearDown();
    }
  }

}
