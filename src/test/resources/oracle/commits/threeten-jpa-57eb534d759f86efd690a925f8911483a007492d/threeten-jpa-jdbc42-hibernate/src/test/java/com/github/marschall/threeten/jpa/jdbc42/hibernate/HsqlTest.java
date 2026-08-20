package com.github.marschall.threeten.jpa.jdbc42.hibernate;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.OffsetDateTime;
import java.util.UUID;

import javax.sql.DataSource;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.junit4.AbstractTransactionalJUnit4SpringContextTests;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;

import com.github.marschall.threeten.jpa.jdbc42.hibernate.SqlServerTest.LocalConfiguration;
import com.github.marschall.threeten.jpa.jdbc42.hibernate.configuration.LocalHsqlConfiguration;

@Transactional
@Disabled
@SpringJUnitConfig({LocalHsqlConfiguration.class, LocalConfiguration.class})
public class HsqlTest extends AbstractTransactionalJUnit4SpringContextTests {

  @Autowired
  private DataSource dataSource;

  @Test
  public void getObject() throws SQLException {
    try (Connection connection = this.dataSource.getConnection();
         PreparedStatement preparedStatement = connection.prepareStatement(
                 "SELECT TIMESTAMP '1960-01-01 23:03:20+05:00' "
//                 "SELECT CAST('1960-01-01 23:03:20+05:00' AS TIMESTAMP WITH TIME ZONE) + INTERVAL '5:00' HOUR TO MINUTE "
//                 "SELECT CAST('1960-01-01 23:03:20+05:00' AS TIMESTAMP WITH TIME ZONE) + TIMEZONE() "
//                 "SELECT TIMESTAMP_WITH_ZONE(TIMESTAMP '1960-01-01 23:03:20') "
//                 "SELECT TIMESTAMP '1960-01-01 23:03:20' AT TIME ZONE INTERVAL '2:00' HOUR TO MINUTE "
               + "FROM (VALUES(0))");
         ResultSet resultSet = preparedStatement.executeQuery()) {

      OffsetDateTime expected = OffsetDateTime.parse("1960-01-01T23:03:20+05:00");
      while (resultSet.next()) {
        assertEquals(expected, resultSet.getObject(1, OffsetDateTime.class));
//        assertEquals("1960-01-01 23:03:20+02:00", resultSet.getObject(1, String.class));
      }
    }
  }

  @Test
  public void setUUid() throws SQLException {
    try (Connection connection = this.dataSource.getConnection();
         Statement statement = connection.createStatement()) {
      statement.execute("DROP TABLE IF EXISTS UUID_TEST");
      statement.execute("CREATE TABLE UUID_TEST(UUID_COLUMN UUID)");
      UUID uuid = UUID.fromString("6F9619FF-8B86-D011-B42D-00C04FC964FF");
      try (PreparedStatement insert = connection.prepareStatement("INSERT INTO UUID_TEST(UUID_COLUMN) VALUES(?)")) {
        insert.setObject(1, uuid);
        assertEquals(1, insert.executeUpdate());
      }
      try (PreparedStatement select = connection.prepareStatement("SELECT UUID_COLUMN FROM UUID_TEST");
           ResultSet resultSet = select.executeQuery()) {
        while (resultSet.next()) {
//          assertEquals(uuid, resultSet.getObject(1, UUID.class));
          assertEquals(uuid.toString(), resultSet.getString(1));
//          assertEquals(uuid.toString(), resultSet.getBytes(1));
        }
      }
    }
  }

  @Test
  public void constant() {
    System.out.println(org.hsqldb.persist.HsqlDatabaseProperties.THIS_VERSION);
  }

  @Configuration
  static class LocalConfiguration {

    @Autowired
    private DataSource dataSource;

    @Bean
    public PlatformTransactionManager txManager() {
      return new DataSourceTransactionManager(this.dataSource);
    }

  }

}
