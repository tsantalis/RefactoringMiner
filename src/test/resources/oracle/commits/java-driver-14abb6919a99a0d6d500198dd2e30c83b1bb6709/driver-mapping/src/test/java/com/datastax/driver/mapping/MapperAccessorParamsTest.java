/*
 *      Copyright (C) 2012-2015 DataStax Inc.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package com.datastax.driver.mapping;

import java.util.Collection;

import com.google.common.collect.Lists;
import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;

import com.datastax.driver.core.CCMBridge;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.mapping.annotations.*;

public class MapperAccessorParamsTest extends CCMBridge.PerClassSingleNodeCluster {
    @Override
    protected Collection<String> getTableDefinitions() {
        return Lists.newArrayList(
            "CREATE TABLE user ( key int primary key, gender int, home_phone text, work_phone text)",
            "CREATE INDEX on user(gender)",
            "CREATE TABLE user_str ( key int primary key, gender text)",
            "CREATE INDEX on user_str(gender)"
        );
    }

    @Test(groups = "short")
    public void should_allow_enum_as_int_in_accessor_params() {
        UserAccessor accessor = new MappingManager(session)
                .createAccessor(UserAccessor.class);

        accessor.addUser(0, Enum.FEMALE);
        accessor.addUser(1, Enum.MALE);

        assertThat(accessor.getUser(Enum.MALE).one().getGender()).isEqualTo(Enum.MALE);
        assertThat(accessor.getUser(Enum.MALE).one().getKey()).isEqualTo(1);

        assertThat(accessor.getUser(Enum.FEMALE).one().getGender()).isEqualTo(Enum.FEMALE);
        assertThat(accessor.getUser(Enum.FEMALE).one().getKey()).isEqualTo(0);
    }

    @Test(groups = "short")
    public void should_allow_enum_as_string_in_accessor_params() {
        UserAccessor accessor = new MappingManager(session)
                .createAccessor(UserAccessor.class);

        accessor.addUserStr(0, Enum.FEMALE);
        accessor.addUserStr(1, Enum.MALE);

        assertThat(accessor.getUserStr(Enum.MALE).one().getGender()).isEqualTo(Enum.MALE);
        assertThat(accessor.getUserStr(Enum.MALE).one().getKey()).isEqualTo(1);

        assertThat(accessor.getUserStr(Enum.FEMALE).one().getGender()).isEqualTo(Enum.FEMALE);
        assertThat(accessor.getUserStr(Enum.FEMALE).one().getKey()).isEqualTo(0);
    }

    @Test(groups = "short")
    public void should_allow_less_parameters_than_bind_markers_if_there_are_repeated_names() {
        UserPhoneAccessor accessor = new MappingManager(session)
            .createAccessor(UserPhoneAccessor.class);

        session.execute("delete from user where key = 0");
        accessor.updatePhones_positional("1111", "2222", 0);
        assertPhonesEqual(0, "1111", "2222");

        session.execute("delete from user where key = 0");
        accessor.updatePhones_named(0, "1111", "2222");
        assertPhonesEqual(0, "1111", "2222");

        session.execute("delete from user where key = 0");
        accessor.updatePhones_fallback("1111", "2222", 0);
        assertPhonesEqual(0, "1111", "2222");

        session.execute("delete from user where key = 0");
        accessor.updateBothPhones(0, "1111");
        assertPhonesEqual(0, "1111", "1111");

        session.execute("delete from user where key = 0");
        accessor.updatePhones_fallback2("1111", "2222", 0);
        assertPhonesEqual(0, "1111", "2222");
    }

    @Test(groups = "short", expectedExceptions = RuntimeException.class)
    public void should_fail_if_not_enough_parameters() {
        new MappingManager(session)
            .createAccessor(UserPhoneAccessor_NotEnoughParams.class);
    }

    @Test(groups = "short", expectedExceptions = RuntimeException.class)
    public void should_fail_if_too_many_parameters() {
        new MappingManager(session)
            .createAccessor(UserPhoneAccessor_TooManyParams.class);
    }

    private void assertPhonesEqual(int key, String home, String work) {
        Row row = session.execute("select * from user where key = " + key).one();
        assertThat(row.getString("home_phone")).isEqualTo(home);
        assertThat(row.getString("work_phone")).isEqualTo(work);
    }

    enum Enum {
        MALE, FEMALE
    }

    @Accessor
    public interface UserAccessor {
        @Query("select * from user where gender=?")
        Result<User> getUser(@Enumerated(EnumType.ORDINAL) Enum value);

        @Query("select * from user_str where gender=?")
        Result<UserStr> getUserStr(@Enumerated(EnumType.STRING) Enum value);

        @Query("insert into user (key, gender) values (?,?)")
        ResultSet addUser(int key, @Enumerated(EnumType.ORDINAL) Enum value);

        @Query("insert into user_str (key, gender) values (?,?)")
        ResultSet addUserStr(int key, @Enumerated(EnumType.STRING) Enum value);
    }

    /** Tests various ways to match method parameters to query bind markers. */
    @Accessor
    public interface UserPhoneAccessor {
        /** Standard positional markers */
        @Query("update user set home_phone = ?, work_phone = ? where key = ?")
        void updatePhones_positional(String homePhone, String workPhone, int key);

        /** Standard named markers */
        @Query("update user set home_phone = :home, work_phone = :work where key = :key")
        void updatePhones_named(@Param("key") int key, @Param("home") String homePhone, @Param("work") String workPhone);

        /** Named markers with no @Param. Should fallback to positional matching */
        @Query("update user set home_phone = :home, work_phone = :work where key = :key")
        void updatePhones_fallback(String homePhone, String workPhone, int key);

        /** Named markers with repeated names */
        @Query("update user set home_phone = :phone, work_phone = :phone where key = :key")
        void updateBothPhones(@Param("key") int key, @Param("phone") String uniquePhone);

        /** Named markers with repeated names, but no @Param. Should fallback to positional matching */
        @Query("update user set home_phone = :phone, work_phone = :phone where key = :key")
        void updatePhones_fallback2(String homePhone, String workPhone, int key);
    }

    @Accessor
    public interface UserPhoneAccessor_NotEnoughParams {
        @SuppressWarnings("unused")
        @Query("update user set home_phone = :phone, work_phone = :phone where key = :key")
        void updateBothPhones(@Param("key") int key);
    }

    public interface UserPhoneAccessor_TooManyParams {
        @SuppressWarnings("unused")
        @Query("update user set home_phone = ?, work_phone = ? where key = ?")
        void updatePhones(String homePhone, String workPhone, int key, int extra);
    }

    @Table(name = "user")
    public static class User {
        @PartitionKey
        private int key;

        @Enumerated(EnumType.ORDINAL)
        private Enum gender;

        public User() {
        }

        public User(int k, Enum val) {
            this.key = k;
            this.gender = val;
        }

        public int getKey() {
            return this.key;
        }

        public void setKey(int pk) {
            this.key = pk;
        }

        public Enum getGender() {
            return this.gender;
        }

        public void setGender(Enum val) {
            this.gender = val;
        }
    }

    @Table(name = "user")
    public static class UserStr {
        @PartitionKey
        private int key;

        private Enum gender;

        public UserStr() {
        }

        public UserStr(int k, Enum val) {
            this.key = k;
            this.gender = val;
        }

        public int getKey() {
            return this.key;
        }

        public void setKey(int pk) {
            this.key = pk;
        }

        public Enum getGender() {
            return this.gender;
        }

        public void setGender(Enum val) {
            this.gender = val;
        }
    }
}