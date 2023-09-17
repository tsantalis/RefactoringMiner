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

import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.CCMBridge;
import com.datastax.driver.mapping.annotations.PartitionKey;
import com.datastax.driver.mapping.annotations.Table;

public class MapperPersistenceStrategiesTest extends CCMBridge.PerClassSingleNodeCluster {

    @Override
    protected Collection<String> getTableDefinitions() {
        return Lists.newArrayList("CREATE TABLE user (key int primary key, v text)");
    }

    @Test(groups = "short")
    void should_include_null_fields_for_save() {
        Mapper<User2> mapper2 = new MappingManager(session).mapper(User2.class);
        User2 user2 = new User2(12, null);
        BoundStatement bs2 = (BoundStatement)mapper2.saveQuery(user2);
        assertThat(bs2.preparedStatement().getQueryString()).contains("\"v\"");
        session.execute(bs2);
        assertThat(mapper2.get(12).getV()).isNull();
    }

    @Test(groups = "short")
    void should_not_include_null_fields_for_save() {
        Mapper<User> mapper = new MappingManager(session).mapper(User.class);
        User user1 = new User(6, null);
        BoundStatement bs = (BoundStatement)mapper.saveQuery(user1);
        assertThat(bs.preparedStatement().getQueryString()).doesNotContain("\"v\"");
    }

    @Table(name = "user", saveStrategy = StrategyType.NOT_NULL_FIELDS_ONLY)
    public static class User {
        @PartitionKey
        private int key;
        private String v;

        public User() {
        }

        public User(int k, String val) {
            this.key = k;
            this.v = val;
        }

        public int getKey() {
            return this.key;
        }

        public void setKey(int pk) {
            this.key = pk;
        }

        public String getV() {
            return this.v;
        }

        public void setV(String val) {
            this.v = val;
        }
    }

    @Table(name = "user")
    public static class User2 {
        @PartitionKey
        private int key;
        private String v;

        public User2() {
        }

        public User2(int k, String val) {
            this.key = k;
            this.v = val;
        }

        public int getKey() {
            return this.key;
        }

        public void setKey(int pk) {
            this.key = pk;
        }

        public String getV() {
            return this.v;
        }

        public void setV(String val) {
            this.v = val;
        }
    }

}
