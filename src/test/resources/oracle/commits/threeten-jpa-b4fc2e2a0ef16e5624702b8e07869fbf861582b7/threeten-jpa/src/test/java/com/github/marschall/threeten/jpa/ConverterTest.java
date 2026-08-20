package com.github.marschall.threeten.jpa;

import static com.github.marschall.threeten.jpa.Constants.PERSISTENCE_UNIT_NAME;
import static java.util.Collections.singletonMap;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assume.assumeThat;

import java.math.BigInteger;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
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
import com.github.marschall.threeten.jpa.test.configuration.DerbyConfiguration;
import com.github.marschall.threeten.jpa.test.configuration.H2Configuration;
import com.github.marschall.threeten.jpa.test.configuration.HsqlConfiguration;
import com.github.marschall.threeten.jpa.test.configuration.MysqlConfiguration;
import com.github.marschall.threeten.jpa.test.configuration.PostgresConfiguration;
import com.github.marschall.threeten.jpa.test.configuration.TransactionManagerConfiguration;

@RunWith(Parameterized.class)
public class ConverterTest {

  private final Class<?> datasourceConfiguration;
  private final Class<?> jpaConfiguration;
  private final String persistenceUnitName;
  private AnnotationConfigApplicationContext applicationContext;
  private TransactionTemplate template;

  public ConverterTest(Class<?> datasourceConfiguration, Class<?> jpaConfiguration, String persistenceUnitName) {
    this.datasourceConfiguration = datasourceConfiguration;
    this.jpaConfiguration = jpaConfiguration;
    this.persistenceUnitName = persistenceUnitName;
  }

  @Parameters(name = "{2}")
  public static Collection<Object[]> parameters() {
    return Arrays.asList(
        new Object[]{DerbyConfiguration.class, EclipseLinkConfiguration.class, "threeten-jpa-eclipselink-derby"},
        new Object[]{H2Configuration.class, EclipseLinkConfiguration.class, "threeten-jpa-eclipselink-h2"},
        new Object[]{HsqlConfiguration.class, EclipseLinkConfiguration.class, "threeten-jpa-eclipselink-hsql"},
        new Object[]{MysqlConfiguration.class, EclipseLinkConfiguration.class, "threeten-jpa-eclipselink-mysql"},
        new Object[]{PostgresConfiguration.class, EclipseLinkConfiguration.class, "threeten-jpa-eclipselink-postgres"},
//        new Object[]{SqlServerConfiguration.class, EclipseLinkConfiguration.class, "threeten-jpa-eclipselink-sqlserver"},
        new Object[]{DerbyConfiguration.class, HibernateConfiguration.class, "threeten-jpa-hibernate-derby"},
        new Object[]{H2Configuration.class, HibernateConfiguration.class, "threeten-jpa-hibernate-h2"},
        new Object[]{HsqlConfiguration.class, HibernateConfiguration.class, "threeten-jpa-hibernate-hsql"},
        new Object[]{MysqlConfiguration.class, HibernateConfiguration.class, "threeten-jpa-hibernate-mysql"},
//        new Object[]{SqlServerConfiguration.class, HibernateConfiguration.class, "threeten-jpa-hibernate-sqlserver"},
        new Object[]{PostgresConfiguration.class, HibernateConfiguration.class, "threeten-jpa-hibernate-postgres"}
        );
  }

  @Before
  public void setUp() {
    this.applicationContext = new AnnotationConfigApplicationContext();
    this.applicationContext.register(this.datasourceConfiguration, TransactionManagerConfiguration.class, this.jpaConfiguration);
    ConfigurableEnvironment environment = this.applicationContext.getEnvironment();
    MutablePropertySources propertySources = environment.getPropertySources();
    Map<String, Object> source = singletonMap(PERSISTENCE_UNIT_NAME, this.persistenceUnitName);
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

  @After
  public void tearDown() {
    this.applicationContext.close();
  }

  private void mysqlHack() {
    // http://stackoverflow.com/questions/43560374/mysql-client-vs-server-time-zone
    if (this.persistenceUnitName.contains("mysql")) {
      DataSource dataSource = this.applicationContext.getBean(DataSource.class);
      JdbcOperations jdbcOperations = new JdbcTemplate(dataSource);
      String serverTimeZone = jdbcOperations.queryForObject("SELECT @@system_time_zone", String.class);
      String systemTimeZone = ZoneId.systemDefault().getId();
      if (systemTimeZone.equals("Etc/UTC")) {
        systemTimeZone = "UTC";
      }
      assumeThat(serverTimeZone, equalTo(systemTimeZone));
    }
  }

  @Test
  public void runTest() {
    mysqlHack();
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
        if (this.datasourceConfiguration.getName().contains("Hsql")) {
          assertEquals(LocalDateTime.parse("1980-01-01T23:03:20.123456"), javaTime.getLocalDateTime());
        } else if (this.datasourceConfiguration.getName().contains("Postgres")) {
          assertEquals(LocalDateTime.parse("1980-01-01T23:03:20.123457"), javaTime.getLocalDateTime());
        } else if (this.datasourceConfiguration.getName().contains("SqlServer")) {
          assertEquals(LocalDateTime.parse("1980-01-01T23:03:20.1234568"), javaTime.getLocalDateTime());
        } else if (this.datasourceConfiguration.getName().contains("Mysql")) {
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
      // EntityManagerFactory should be closed by spring.
    }
  }

}
