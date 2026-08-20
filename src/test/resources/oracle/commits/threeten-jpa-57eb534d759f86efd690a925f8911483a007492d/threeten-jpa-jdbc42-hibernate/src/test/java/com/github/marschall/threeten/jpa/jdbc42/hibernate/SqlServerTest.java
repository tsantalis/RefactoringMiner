package com.github.marschall.threeten.jpa.jdbc42.hibernate;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;

import javax.sql.DataSource;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;

import com.github.marschall.threeten.jpa.jdbc42.hibernate.SqlServerTest.LocalConfiguration;
import com.github.marschall.threeten.jpa.jdbc42.hibernate.configuration.LocalSqlServerConfiguration;

@Transactional
@Disabled
@SpringJUnitConfig({LocalSqlServerConfiguration.class, LocalConfiguration.class})
@Sql("classpath:sqlserver-schema.sql")
@Sql("classpath:sqlserver-data.sql")
public class SqlServerTest {

  @Autowired
  private DataSource dataSource;

  @Test
  public void getObject() throws SQLException {
    try (Connection connection = this.dataSource.getConnection();
         PreparedStatement preparedStatement = connection.prepareStatement(
                 "SELECT OFFSET_TIME"
              + " FROM java_time_42_with_zone");
         ResultSet resultSet = preparedStatement.executeQuery()) {
      while (resultSet.next()) {
        Object dateTimeOffset = resultSet.getObject(1);
        assertEquals("microsoft.sql.DateTimeOffset", dateTimeOffset.getClass().getName());
        System.out.println(dateTimeOffset);
      }
    }
  }

  @Test
  public void setUUid() throws SQLException {
    try (Connection connection = this.dataSource.getConnection();
         Statement statement = connection.createStatement()) {
      statement.execute("DROP TABLE IF EXISTS UUID_TEST");
      statement.execute("CREATE TABLE UUID_TEST(UUID_COLUMN uniqueidentifier)");
      UUID uuid = UUID.fromString("6F9619FF-8B86-D011-B42D-00C04FC964FF");
      try (PreparedStatement insert = connection.prepareStatement("INSERT INTO UUID_TEST(UUID_COLUMN) VALUES(?)")) {
        insert.setObject(1, uuid);
        assertEquals(1, insert.executeUpdate());
      }
      try (PreparedStatement select = connection.prepareStatement("SELECT UUID_COLUMN FROM UUID_TEST");
           ResultSet resultSet = select.executeQuery()) {
        while (resultSet.next()) {
          assertEquals(uuid.toString().toUpperCase(), resultSet.getString(1));
        }
      }
    }
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
