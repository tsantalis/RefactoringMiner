package com.github.marschall.threeten.jpa.mssql.hibernate;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigInteger;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.junit.jupiter.api.Test;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.annotation.Transactional;

import com.github.marschall.threeten.jpa.mssql.hibernate.configuration.LocalSqlServerConfiguration;
import com.github.marschall.threeten.jpa.test.DisabledOnTravis;

@Transactional
@SpringJUnitConfig(LocalSqlServerConfiguration.class)
@Sql("classpath:sqlserver-schema.sql")
@Sql("classpath:sqlserver-data.sql")
@DisabledOnTravis
public class MssqlTest {

  @PersistenceContext
  private EntityManager entityManager;

  @Test
  public void readFirstRow() {
    JavaTime42WithZone firstRow = this.entityManager.find(JavaTime42WithZone.class, new BigInteger("1"));

    ZoneOffset zoneOffset = ZoneOffset.ofHoursMinutes(2, 30);
    OffsetDateTime expectedOffset = OffsetDateTime.of(1960, 1, 1, 23, 3, 20, 123456700, zoneOffset);
    assertEquals(expectedOffset, firstRow.getOffsetDateTime());
  }

  @Test
  public void readSecondRow() {
    JavaTime42WithZone secondRow = this.entityManager.find(JavaTime42WithZone.class, new BigInteger("2"));
    ZoneOffset zoneOffset = ZoneOffset.ofHoursMinutes(-5, -30);
    OffsetDateTime expectedOffset = OffsetDateTime.of(1999, 1, 23, 8, 26, 56, 123456700, zoneOffset);
    assertEquals(expectedOffset, secondRow.getOffsetDateTime());
  }

  @Test
  public void jpqlNamed() {
    OffsetDateTime offsetDateTime = OffsetDateTime.parse("1998-01-31T09:26:56.66+02:00");
    TypedQuery<JavaTime42WithZone> query = this.entityManager.createQuery("SELECT t "
            + " FROM JavaTime42WithZone t "
            + " WHERE t.offsetDateTime < :now",
            JavaTime42WithZone.class);
//    query.setParameter(1, offsetDateTime);
    query.setParameter("now", offsetDateTime);
    JavaTime42WithZone entity = query.getSingleResult();

    assertEquals(new BigInteger("1"), entity.getId());

    ZoneOffset zoneOffset = ZoneOffset.ofHoursMinutes(2, 30);
    OffsetDateTime expectedOffset = OffsetDateTime.of(1960, 1, 1, 23, 3, 20, 123456700, zoneOffset);
    assertEquals(expectedOffset, entity.getOffsetDateTime());
  }

  @Test
  public void jpqlIndexed() {
    OffsetDateTime offsetDateTime = OffsetDateTime.parse("1998-01-31T09:26:56.66+02:00");
    TypedQuery<JavaTime42WithZone> query = this.entityManager.createQuery("SELECT t "
            + " FROM JavaTime42WithZone t "
            + " WHERE t.offsetDateTime < ?1",
            JavaTime42WithZone.class);
    query.setParameter(1, offsetDateTime);
    JavaTime42WithZone entity = query.getSingleResult();

    assertEquals(new BigInteger("1"), entity.getId());

    ZoneOffset zoneOffset = ZoneOffset.ofHoursMinutes(2, 30);
    OffsetDateTime expectedOffset = OffsetDateTime.of(1960, 1, 1, 23, 3, 20, 123456700, zoneOffset);
    assertEquals(expectedOffset, entity.getOffsetDateTime());
  }

  @Test
  public void criteriaApi() {
    OffsetDateTime offsetDateTime = OffsetDateTime.parse("1998-01-31T09:26:56.66+02:00");
    CriteriaBuilder builder = this.entityManager.getCriteriaBuilder();
    CriteriaQuery<JavaTime42WithZone> query = builder.createQuery(JavaTime42WithZone.class);
    Root<JavaTime42WithZone> root = query.from(JavaTime42WithZone.class);
    CriteriaQuery<JavaTime42WithZone> beforeTwelfeFive = query.where(
            builder.lessThan(root.get(JavaTime42WithZone_.offsetDateTime), offsetDateTime));


    JavaTime42WithZone entity = this.entityManager.createQuery(beforeTwelfeFive).getSingleResult();

    assertEquals(new BigInteger("1"), entity.getId());

    ZoneOffset zoneOffset = ZoneOffset.ofHoursMinutes(2, 30);
    OffsetDateTime expectedOffset = OffsetDateTime.of(1960, 1, 1, 23, 3, 20, 123456700, zoneOffset);
    assertEquals(expectedOffset, entity.getOffsetDateTime());

  }

}
