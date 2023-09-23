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

import static com.datastax.driver.mapping.Mapper.Option;

public class MapperOptionTest extends CCMBridge.PerClassSingleNodeCluster {

    @Override
    protected Collection<String> getTableDefinitions() {
        return Lists.newArrayList("CREATE TABLE user (key int primary key, v text)");
    }

    @Test(groups = "short")
    void should_use_using_options_to_save() {
        Long tsValue = 906L;
        Mapper<User> mapper = new MappingManager(session).mapper(User.class);
        mapper.saveAsync(new User(42, "helloworld"), Option.timestamp(tsValue));
        assertThat(mapper.get(42).getV()).isEqualTo("helloworld");
        Long tsReturned = session.execute("SELECT writetime(v) FROM user WHERE key=" + 42).one().getLong(0);
        assertThat(tsReturned).isEqualTo(tsValue);
    }

    @Test(groups = "short")
    void should_use_using_options_only_once() {
        Long tsValue = 1L;
        Mapper<User> mapper = new MappingManager(session).mapper(User.class);
        mapper.save(new User(43, "helloworld"), Option.timestamp(tsValue));
        mapper.save(new User(44, "test"));
        Long tsReturned = session.execute("SELECT writetime(v) FROM user WHERE key=" + 44).one().getLong(0);
        // Assuming we cannot go back in time (yet) and execute the write at ts=1
        assertThat(tsReturned).isNotEqualTo(tsValue);
    }

    @Test(groups = "short")
    void should_allow_setting_default_options() {
        Mapper<User> mapper = new MappingManager(session).mapper(User.class);
        mapper.setDefaultSaveOptions(Option.timestamp(644746L), Option.ttl(76324));
        BoundStatement bs = (BoundStatement)mapper.saveQuery(new User(46, "rjhrgce"));
        assertThat(bs.preparedStatement().getQueryString()).contains("USING TIMESTAMP");
        mapper.resetDefaultSaveOptions();
        bs = (BoundStatement)mapper.saveQuery(new User(47, "rjhrgce"));
        assertThat(bs.preparedStatement().getQueryString()).doesNotContain("USING TIMESTAMP");
        mapper.setDefaultDeleteOptions(Option.timestamp(3245L));
        bs = (BoundStatement)mapper.deleteQuery(47);
        assertThat(bs.preparedStatement().getQueryString()).contains("USING TIMESTAMP");
        mapper.resetDefaultDeleteOptions();
        bs = (BoundStatement)mapper.deleteQuery(47);
        assertThat(bs.preparedStatement().getQueryString()).doesNotContain("USING TIMESTAMP");
    }

    @Test(groups = "short")
    void should_use_using_options_for_delete() {
        Mapper<User> mapper = new MappingManager(session).mapper(User.class);
        User todelete = new User(45, "todelete");
        mapper.save(todelete);
        Option opt = Option.timestamp(35);
        BoundStatement bs = (BoundStatement)mapper.deleteQuery(45, opt);
        assertThat(bs.preparedStatement().getQueryString()).contains("USING TIMESTAMP");
    }

    @Table(name = "user")
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
}
