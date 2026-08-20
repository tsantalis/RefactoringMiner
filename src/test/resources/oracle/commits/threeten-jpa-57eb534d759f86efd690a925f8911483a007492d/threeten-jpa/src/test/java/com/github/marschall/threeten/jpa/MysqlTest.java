package com.github.marschall.threeten.jpa;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import javax.sql.DataSource;
import javax.transaction.Transactional;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.PlatformTransactionManager;

import com.github.marschall.threeten.jpa.MysqlTest.LocalConfiguration;
import com.github.marschall.threeten.jpa.test.configuration.MysqlConfiguration;

@Transactional
@SpringJUnitConfig({MysqlConfiguration.class, LocalConfiguration.class})
@Disabled
public class MysqlTest {

  @Autowired
  private DataSource dataSource;

  @Test
  public void testPrecision() throws SQLException {
    try (Connection connection = this.dataSource.getConnection();
        PreparedStatement preparedStatement = connection.prepareStatement(
            "SELECT TIMESTAMP '1980-01-01 23:03:20.123456'")) {
      try (ResultSet resultSet = preparedStatement.executeQuery()) {
        LocalDateTime expected = LocalDateTime.parse("1980-01-01T23:03:20.123456");
        while (resultSet.next()) {
          assertEquals(expected, resultSet.getObject(1, LocalDateTime.class));
        }
      }
    }
  }

  @Test
  public void getObject() throws SQLException {
//    TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    try (Connection connection = this.dataSource.getConnection();
         PreparedStatement preparedStatement = connection.prepareStatement(
                 "SELECT ? = DATE '1988-12-25', ? = TIME '15:09:02', ? = TIMESTAMP '1980-01-01 23:03:20'")) {
      preparedStatement.setDate(1, java.sql.Date.valueOf("1988-12-25"));
      preparedStatement.setTime(2, java.sql.Time.valueOf("15:09:02"));
      preparedStatement.setTimestamp(3, java.sql.Timestamp.valueOf("1980-01-01 23:03:20"));
      try (ResultSet resultSet = preparedStatement.executeQuery()) {
        while (resultSet.next()) {
          System.out.println(resultSet.getBoolean(1));
          System.out.println(resultSet.getBoolean(2));
          System.out.println(resultSet.getBoolean(3));
        }
      }

      preparedStatement.setObject(1, LocalDate.parse("1988-12-25"));
      preparedStatement.setObject(2, LocalTime.parse("15:09:02"));
      preparedStatement.setObject(3, LocalDateTime.parse("1980-01-01T23:03:20"));
      try (ResultSet resultSet = preparedStatement.executeQuery()) {
        while (resultSet.next()) {
          System.out.println(resultSet.getBoolean(1));
          System.out.println(resultSet.getBoolean(2));
          System.out.println(resultSet.getBoolean(3));
        }
      }

      try (Statement statement = connection.createStatement();
          ResultSet resultSet = statement.executeQuery("SELECT VERSION()")) {
        while (resultSet.next()) {
          System.out.println(resultSet.getString(1));
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
