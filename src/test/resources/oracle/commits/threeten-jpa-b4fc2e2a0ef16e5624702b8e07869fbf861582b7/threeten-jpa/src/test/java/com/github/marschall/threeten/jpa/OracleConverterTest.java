package com.github.marschall.threeten.jpa;

import static com.github.marschall.threeten.jpa.Constants.PERSISTENCE_UNIT_NAME;
import static java.util.Collections.singletonMap;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.TypedQuery;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.github.marschall.threeten.jpa.configuration.EclipseLinkConfiguration;
import com.github.marschall.threeten.jpa.configuration.HibernateConfiguration;
import com.github.marschall.threeten.jpa.test.configuration.OracleConfiguration;
import com.github.marschall.threeten.jpa.test.configuration.TransactionManagerConfiguration;

@RunWith(Parameterized.class)
@Ignore("needs an oracle instance running")
public class OracleConverterTest {

  private final Class<?> jpaConfiguration;
  private final String persistenceUnitName;
  private AnnotationConfigApplicationContext applicationContext;
  private TransactionTemplate template;

  public OracleConverterTest(Class<?> jpaConfiguration, String persistenceUnitName) {
    this.jpaConfiguration = jpaConfiguration;
    this.persistenceUnitName = persistenceUnitName;
  }

  @Parameters(name = "{1}")
  public static Collection<Object[]> parameters() {
    return Arrays.asList(
        new Object[]{EclipseLinkConfiguration.class, "threeten-jpa-eclipselink-oracle"},
        new Object[]{HibernateConfiguration.class, "threeten-jpa-hibernate-oracle"}
        );
  }

  @Before
  public void setUp() {
    this.applicationContext = new AnnotationConfigApplicationContext();
    this.applicationContext.register(OracleConfiguration.class, TransactionManagerConfiguration.class, this.jpaConfiguration);
    ConfigurableEnvironment environment = this.applicationContext.getEnvironment();
    MutablePropertySources propertySources = environment.getPropertySources();
    Map<String, Object> source = singletonMap(PERSISTENCE_UNIT_NAME, this.persistenceUnitName);
    propertySources.addFirst(new MapPropertySource("persistence unit name", source));
    this.applicationContext.refresh();

    PlatformTransactionManager txManager = this.applicationContext.getBean(PlatformTransactionManager.class);
    this.template = new TransactionTemplate(txManager);
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
      this.template.execute((s) -> {
        TypedQuery<OracleJavaTime> query = entityManager.createQuery("SELECT t FROM OracleJavaTime t", OracleJavaTime.class);
        List<OracleJavaTime> resultList = query.getResultList();
        assertThat(resultList, hasSize(1));

        // validate the entity inserted by SQL
        OracleJavaTime javaTime = resultList.get(0);
        assertEquals(LocalDate.parse("1988-12-25"), javaTime.getLocalDate());
        assertEquals(LocalDateTime.parse("1960-01-01T23:03:20"), javaTime.getLocalDateTime());
        return null;
       });

      // insert a new entity into the database
      BigInteger newId = new BigInteger("2");
      LocalDate newDate = LocalDate.now();
      LocalDateTime newDateTime = LocalDateTime.now();

      this.template.execute((s) -> {
        OracleJavaTime toInsert = new OracleJavaTime();
        toInsert.setId(newId);
        toInsert.setLocalDate(newDate);
        toInsert.setLocalDateTime(newDateTime);
        entityManager.persist(toInsert);
        // the transaction should trigger a flush and write to the database
        return null;
      });

      // validate the new entity inserted into the database
      this.template.execute((s) -> {
        OracleJavaTime readBack = entityManager.find(OracleJavaTime.class, newId);
        assertNotNull(readBack);
        assertEquals(newId, readBack.getId());
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
