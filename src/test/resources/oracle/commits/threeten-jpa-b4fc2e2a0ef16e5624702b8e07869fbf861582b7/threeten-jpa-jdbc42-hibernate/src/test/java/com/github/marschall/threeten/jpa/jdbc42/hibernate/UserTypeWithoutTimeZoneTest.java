package com.github.marschall.threeten.jpa.jdbc42.hibernate;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

import java.math.BigInteger;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.TypedQuery;
import javax.sql.DataSource;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.jdbc.datasource.init.DatabasePopulator;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.github.marschall.threeten.jpa.jdbc42.hibernate.configuration.LocalH2Configuration;
import com.github.marschall.threeten.jpa.jdbc42.hibernate.configuration.LocalHsqlConfiguration;
import com.github.marschall.threeten.jpa.jdbc42.hibernate.configuration.LocalMysqlConfiguration;
import com.github.marschall.threeten.jpa.jdbc42.hibernate.configuration.LocalPostgresConfiguration;

@RunWith(Parameterized.class)
public class UserTypeWithoutTimeZoneTest {

  private final Class<?> jpaConfiguration;
  private AnnotationConfigApplicationContext applicationContext;
  private TransactionTemplate template;

  public UserTypeWithoutTimeZoneTest(Class<?> jpaConfiguration, String persistenceUnitName) {
    this.jpaConfiguration = jpaConfiguration;
  }

  @Parameters(name = "{1}")
  public static Collection<Object[]> parameters() {
    return Arrays.asList(
        new Object[]{LocalHsqlConfiguration.class, "threeten-jpa-hibernate-hsql"},
        new Object[]{LocalMysqlConfiguration.class, "threeten-jpa-hibernate-mysql"},
        new Object[]{LocalH2Configuration.class, "threeten-jpa-hibernate-h2"},
//        new Object[]{LocalDerbyConfiguration.class, "threeten-jpa-hibernate-derby"},
//        new Object[]{LocalSqlServerConfiguration.class, "threeten-jpa-hibernate-sqlserver"},
        new Object[]{LocalPostgresConfiguration.class, "threeten-jpa-hibernate-postgres"}
        );
  }

  @Before
  public void setUp() {
    this.applicationContext = new AnnotationConfigApplicationContext(this.jpaConfiguration);

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

  @After
  public void tearDown() {
    this.applicationContext.close();
  }

  @Test
  public void runTest() {
    EntityManagerFactory factory = this.applicationContext.getBean(EntityManagerFactory.class);
    EntityManager entityManager = factory.createEntityManager();
    try {
      // read the entity inserted by SQL
      this.template.execute(status -> {
        TypedQuery<JavaTime42> query = entityManager.createQuery("SELECT t FROM JavaTime42 t ORDER BY t.id ASC", JavaTime42.class);
        List<JavaTime42> resultList = query.getResultList();
        assertThat(resultList, hasSize(2));

        // validate the entity inserted by SQL
        JavaTime42 javaTime = resultList.get(1);
        assertEquals(LocalTime.parse("02:55:00"), javaTime.getLocalTime());
        assertEquals(LocalDate.parse("2016-03-27"), javaTime.getLocalDate());
        if (this.jpaConfiguration.getName().contains("Hsql")) {
          assertEquals(LocalDateTime.parse("2016-03-27T02:55:00.123456"), javaTime.getLocalDateTime());
        } else if (this.jpaConfiguration.getName().contains("Postgres")) {
          assertEquals(LocalDateTime.parse("2016-03-27T02:55:00.123457"), javaTime.getLocalDateTime());
        } else if (this.jpaConfiguration.getName().contains("Mysql")) {
          // travis ci
//          assertEquals(LocalDateTime.parse("2016-03-27T02:55:00.123457"), javaTime.getLocalDateTime());
          assertEquals(LocalDateTime.parse("2016-03-27T02:55:00"), javaTime.getLocalDateTime());
        } else {
          assertEquals(LocalDateTime.parse("2016-03-27T02:55:00.123456789"), javaTime.getLocalDateTime());
        }
        return null;
       });

      // insert a new entity into the database
      BigInteger newId = new BigInteger("3");
      LocalTime newLocalTime = LocalTime.now();
      LocalDate newLocalDate = LocalDate.now();
      LocalDateTime newLocalDateTime = LocalDateTime.now();

      this.template.execute(status -> {
        JavaTime42 toInsert = new JavaTime42();
        toInsert.setId(newId);
        toInsert.setLocalTime(newLocalTime);
        toInsert.setLocalDate(newLocalDate);
        toInsert.setLocalDateTime(newLocalDateTime);
        entityManager.persist(toInsert);
        // the transaction should trigger a flush and write to the database
        return null;
      });

      // validate the new entity inserted into the database
      this.template.execute(status -> {
        JavaTime42 readBack = entityManager.find(JavaTime42.class, newId);
        assertNotNull(readBack);
        assertEquals(newId, readBack.getId());
        assertEquals(newLocalTime, readBack.getLocalTime());
        assertEquals(newLocalDate, readBack.getLocalDate());
        assertEquals(newLocalDateTime, readBack.getLocalDateTime());
        entityManager.remove(readBack);
        return null;
      });
    } finally {
      entityManager.close();
      // EntityManagerFactory should be closed by spring.
    }
  }

}
