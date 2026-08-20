package com.github.marschall.threeten.jpa;

import static com.github.marschall.threeten.jpa.Constants.PERSISTENCE_UNIT_NAME;
import static java.util.Collections.singletonMap;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.math.BigInteger;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.TypedQuery;
import javax.sql.DataSource;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.DatabasePopulator;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.github.marschall.threeten.jpa.configuration.EclipseLinkConfiguration;
import com.github.marschall.threeten.jpa.configuration.HibernateConfiguration;
import com.github.marschall.threeten.jpa.test.Travis;
import com.github.marschall.threeten.jpa.test.configuration.DerbyConfiguration;
import com.github.marschall.threeten.jpa.test.configuration.H2Configuration;
import com.github.marschall.threeten.jpa.test.configuration.HsqlConfiguration;
import com.github.marschall.threeten.jpa.test.configuration.MysqlConfiguration;
import com.github.marschall.threeten.jpa.test.configuration.PostgresConfiguration;
import com.github.marschall.threeten.jpa.test.configuration.SqlServerConfiguration;
import com.github.marschall.threeten.jpa.test.configuration.TransactionManagerConfiguration;

public class ConverterTest {

  private AnnotationConfigApplicationContext applicationContext;
  private TransactionTemplate template;

  public static List<Arguments> parameters() {
    List<Arguments> parameters = new ArrayList<>();
    parameters.add(Arguments.of(DerbyConfiguration.class, EclipseLinkConfiguration.class, "threeten-jpa-eclipselink-derby"));
    parameters.add(Arguments.of(H2Configuration.class, EclipseLinkConfiguration.class, "threeten-jpa-eclipselink-h2"));
    parameters.add(Arguments.of(HsqlConfiguration.class, EclipseLinkConfiguration.class, "threeten-jpa-eclipselink-hsql"));
    parameters.add(Arguments.of(MysqlConfiguration.class, EclipseLinkConfiguration.class, "threeten-jpa-eclipselink-mysql"));
    parameters.add(Arguments.of(PostgresConfiguration.class, EclipseLinkConfiguration.class, "threeten-jpa-eclipselink-postgres"));
    if (!Travis.isTravis()) {
      parameters.add(Arguments.of(SqlServerConfiguration.class, EclipseLinkConfiguration.class, "threeten-jpa-eclipselink-sqlserver"));
    }
    parameters.add(Arguments.of(DerbyConfiguration.class, HibernateConfiguration.class, "threeten-jpa-hibernate-derby"));
    parameters.add(Arguments.of(H2Configuration.class, HibernateConfiguration.class, "threeten-jpa-hibernate-h2"));
    parameters.add(Arguments.of(HsqlConfiguration.class, HibernateConfiguration.class, "threeten-jpa-hibernate-hsql"));
    parameters.add(Arguments.of(MysqlConfiguration.class, HibernateConfiguration.class, "threeten-jpa-hibernate-mysql"));
    if (!Travis.isTravis()) {
      parameters.add(Arguments.of(SqlServerConfiguration.class, HibernateConfiguration.class, "threeten-jpa-hibernate-sqlserver"));
    }
    parameters.add(Arguments.of(PostgresConfiguration.class, HibernateConfiguration.class, "threeten-jpa-hibernate-postgres"));
    return parameters;
  }

  private void setUp(Class<?> datasourceConfiguration, Class<?> jpaConfiguration, String persistenceUnitName) {
    this.applicationContext = new AnnotationConfigApplicationContext();
    this.applicationContext.register(datasourceConfiguration, TransactionManagerConfiguration.class, jpaConfiguration);
    ConfigurableEnvironment environment = this.applicationContext.getEnvironment();
    MutablePropertySources propertySources = environment.getPropertySources();
    Map<String, Object> source = singletonMap(PERSISTENCE_UNIT_NAME, persistenceUnitName);
    propertySources.addFirst(new MapPropertySource("persistence unit name", source));
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

  private void mysqlHack(String persistenceUnitName) {
    // http://stackoverflow.com/questions/43560374/mysql-client-vs-server-time-zone
    if (persistenceUnitName.contains("mysql")) {
      DataSource dataSource = this.applicationContext.getBean(DataSource.class);
      JdbcOperations jdbcOperations = new JdbcTemplate(dataSource);
      String serverTimeZone = jdbcOperations.queryForObject("SELECT @@system_time_zone", String.class);
      String systemTimeZone = ZoneId.systemDefault().getId();
      if (systemTimeZone.equals("Etc/UTC")) {
        systemTimeZone = "UTC";
      }
      assumeTrue(serverTimeZone.equals(systemTimeZone));
    }
  }

  @ParameterizedTest
  @MethodSource("parameters")
  public void runTest(Class<?> datasourceConfiguration, Class<?> jpaConfiguration, String persistenceUnitName) {
    this.setUp(datasourceConfiguration, jpaConfiguration, persistenceUnitName);
    try {
      mysqlHack(persistenceUnitName);
      EntityManagerFactory factory = this.applicationContext.getBean(EntityManagerFactory.class);
      EntityManager entityManager = factory.createEntityManager();
      try {
        // read the entity inserted by SQL
        this.template.execute((s) -> {
          TypedQuery<JavaTime> query = entityManager.createQuery("SELECT t FROM JavaTime t ORDER BY t.id ASC", JavaTime.class);
          List<JavaTime> resultList = query.getResultList();
          assertThat(resultList, hasSize(2));

          // validate the entity inserted by SQL
          JavaTime javaTime = resultList.get(0);
          assertEquals(LocalTime.parse("15:09:02"), javaTime.getLocalTime());
          assertEquals(LocalDate.parse("1988-12-25"), javaTime.getLocalDate());
          if (datasourceConfiguration.getName().contains("Hsql")) {
            assertEquals(LocalDateTime.parse("1980-01-01T23:03:20.123456"), javaTime.getLocalDateTime());
          } else if (datasourceConfiguration.getName().contains("Postgres")) {
            assertEquals(LocalDateTime.parse("1980-01-01T23:03:20.123457"), javaTime.getLocalDateTime());
          } else if (datasourceConfiguration.getName().contains("SqlServer")) {
            assertEquals(LocalDateTime.parse("1980-01-01T23:03:20.1234568"), javaTime.getLocalDateTime());
          } else if (datasourceConfiguration.getName().contains("Mysql")) {
            // version in Travis is older than the sin
            assertEquals(LocalDateTime.parse("1980-01-01T23:03:20"), javaTime.getLocalDateTime());
          } else {
            assertEquals(LocalDateTime.parse("1980-01-01T23:03:20.123456789"), javaTime.getLocalDateTime());
          }
          return null;
        });

        // insert a new entity into the database
        BigInteger newId = new BigInteger("3");
        LocalTime newTime = LocalTime.now();
        LocalDate newDate = LocalDate.now();
        LocalDateTime newDateTime = LocalDateTime.now();

        this.template.execute((s) -> {
          JavaTime toInsert = new JavaTime();
          toInsert.setId(newId);
          toInsert.setLocalDate(newDate);
          toInsert.setLocalTime(newTime);
          toInsert.setLocalDateTime(newDateTime);
          entityManager.persist(toInsert);
          // the transaction should trigger a flush and write to the database
          return null;
        });

        // validate the new entity inserted into the database
        this.template.execute((s) -> {
          JavaTime readBack = entityManager.find(JavaTime.class, newId);
          assertNotNull(readBack);
          assertEquals(newId, readBack.getId());
          assertEquals(newTime, readBack.getLocalTime());
          assertEquals(newDate, readBack.getLocalDate());
          assertEquals(newDateTime, readBack.getLocalDateTime());
          entityManager.remove(readBack);
          return null;
        });
      } finally {
        entityManager.close();
      }
    } finally {
      this.tearDown();
    }
  }

}
