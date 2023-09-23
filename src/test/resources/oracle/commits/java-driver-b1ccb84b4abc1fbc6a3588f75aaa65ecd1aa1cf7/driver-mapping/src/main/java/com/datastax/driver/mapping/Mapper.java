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

import java.util.*;

import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.datastax.driver.core.*;

/**
 * An object handling the mapping of a particular class.
 * <p/>
 * A {@code Mapper} object is obtained from a {@code MappingManager} using the
 * {@link MappingManager#mapper} method.
 */
public class Mapper<T> {

    private static final Logger logger = LoggerFactory.getLogger(EntityMapper.class);

    final MappingManager manager;
    final ProtocolVersion protocolVersion;
    final Class<T> klass;
    final EntityMapper<T> mapper;
    final TableMetadata tableMetadata;

    // Cache prepared statements for each type of query we use.
    private volatile Map<String, PreparedStatement> preparedQueries = Collections.<String, PreparedStatement>emptyMap();

    private static final Function<Object, Void> NOOP = Functions.<Void>constant(null);

    private volatile Option[] defaultSaveOptions;
    private volatile Option[] defaultDeleteOptions;

    final Function<ResultSet, T> mapOneFunction;
    final Function<ResultSet, Result<T>> mapAllFunction;

    Mapper(MappingManager manager, Class<T> klass, EntityMapper<T> mapper) {
        this.manager = manager;
        this.klass = klass;
        this.mapper = mapper;

        KeyspaceMetadata keyspace = session().getCluster().getMetadata().getKeyspace(mapper.getKeyspace());
        this.tableMetadata = keyspace == null ? null : keyspace.getTable(Metadata.quote(mapper.getTable()));

        this.protocolVersion = manager.getSession().getCluster().getConfiguration().getProtocolOptions().getProtocolVersionEnum();
        this.mapOneFunction = new Function<ResultSet, T>() {
            public T apply(ResultSet rs) {
                return Mapper.this.map(rs).one();
            }
        };
        this.mapAllFunction = new Function<ResultSet, Result<T>>() {
            public Result<T> apply(ResultSet rs) {
                return Mapper.this.map(rs);
            }
        };
    }

    Session session() {
        return manager.getSession();
    }

    PreparedStatement getPreparedQuery(QueryType type, Option... options) {
        String queryString;
        queryString = type.makePreparedQueryString(tableMetadata, mapper, options);
        PreparedStatement stmt = preparedQueries.get(queryString);
        if (stmt == null) {
            synchronized (preparedQueries) {
                stmt = preparedQueries.get(queryString);
                if (stmt == null) {
                    logger.debug("Preparing query {}", queryString);
                    stmt = session().prepare(queryString);
                    Map<String, PreparedStatement> newQueries = new HashMap<String, PreparedStatement>(preparedQueries);
                    newQueries.put(queryString, stmt);
                    preparedQueries = newQueries;
                }
            }
        }
        return stmt;
    }

    /**
     * The {@code TableMetadata} for this mapper.
     *
     * @return the {@code TableMetadata} for this mapper or {@code null} if keyspace is not set.
     */
    public TableMetadata getTableMetadata() {
        return tableMetadata;
    }

    /**
     * The {@code MappingManager} managing this mapper.
     *
     * @return the {@code MappingManager} managing this mapper.
     */
    public MappingManager getManager() {
        return manager;
    }

    /**
     * Creates a query that can be used to save the provided entity.
     * <p/>
     * This method is useful if you want to setup a number of options (tracing,
     * conistency level, ...) of the returned statement before executing it manually
     * or need access to the {@code ResultSet} object after execution (to get the
     * trace, the execution info, ...), but in other cases, calling {@link #save}
     * or {@link #saveAsync} is shorter.
     *
     * @param entity the entity to save.
     * @return a query that saves {@code entity} (based on it's defined mapping).
     */
    public Statement saveQuery(T entity) {
        PreparedStatement ps;
        if (this.defaultSaveOptions != null)
            ps = getPreparedQuery(QueryType.SAVE, this.defaultSaveOptions);
        else
            ps = getPreparedQuery(QueryType.SAVE);

        BoundStatement bs = ps.bind();
        int i = 0;
        for (ColumnMapper<T> cm : mapper.allColumns()) {
            Object value = cm.getValue(entity);
            bs.setBytesUnsafe(i++, value == null ? null : cm.getDataType().serialize(value, protocolVersion));
        }

        if (mapper.writeConsistency != null)
            bs.setConsistencyLevel(mapper.writeConsistency);
        return bs;
    }

    /**
     * Creates a query that can be used to save the provided entity.
     * <p/>
     * This method is useful if you want to setup a number of options (tracing,
     * conistency level, ...) of the returned statement before executing it manually
     * or need access to the {@code ResultSet} object after execution (to get the
     * trace, the execution info, ...), but in other cases, calling {@link #save}
     * or {@link #saveAsync} is shorter.
     *
     * @param entity the entity to save.
     * @return a query that saves {@code entity} (based on it's defined mapping).
     */
    public Statement saveQuery(T entity, Option... options) {
        PreparedStatement ps = getPreparedQuery(QueryType.SAVE, options);

        BoundStatement bs = ps.bind();
        int i = 0;
        for (ColumnMapper<T> cm : mapper.allColumns()) {
            Object value = cm.getValue(entity);
            bs.setBytesUnsafe(i++, value == null ? null : cm.getDataType().serialize(value, protocolVersion));
        }

        if (options != null) {
            for (Option opt : options) {
                if (opt instanceof Option.Ttl) {
                    Option.Ttl ttlOption = (Option.Ttl)opt;
                    bs.setInt(i++, ttlOption.getValue());
                }
                if (opt instanceof Option.Timestamp) {
                    Option.Timestamp tsOption = (Option.Timestamp)opt;
                    bs.setLong(i++, tsOption.getValue());
                }
            }
        }
        if (mapper.writeConsistency != null)
            bs.setConsistencyLevel(mapper.writeConsistency);
        return bs;
    }

    /**
     * Save an entity mapped by this mapper.
     * <p/>
     * This method is basically equivalent to: {@code getManager().getSession().execute(saveQuery(entity))}.
     *
     * @param entity the entity to save.
     */
    public void save(T entity) {
        session().execute(saveQuery(entity));
    }

    /**
     * Save an entity mapped by this mapper and using special options for save.
     * <p/>
     *
     * @param entity  the entity to save.
     * @param options the options object specified defining special options when saving.
     */
    public void save(T entity, Option... options) {
        session().execute(saveQuery(entity, options));
    }

    /**
     * Save an entity mapped by this mapper asynchonously.
     * <p/>
     * This method is basically equivalent to: {@code getManager().getSession().executeAsync(saveQuery(entity))}.
     *
     * @param entity the entity to save.
     * @return a future on the completion of the save operation.
     */
    public ListenableFuture<Void> saveAsync(T entity) {
        return Futures.transform(session().executeAsync(saveQuery(entity)), NOOP);
    }

    /**
     * Save an entity mapped by this mapper asynchonously and using special options for save.
     * <p/>
     *
     * @param entity the entity to save.
     * @return a future on the completion of the save operation.
     */
    public ListenableFuture<Void> saveAsync(T entity, Option... options) {
        return Futures.transform(session().executeAsync(saveQuery(entity, options)), NOOP);
    }

    /**
     * Creates a query that can be used to delete the provided entity.
     * <p/>
     * This method is a shortcut that extract the PRIMARY KEY from the
     * provided entity and call {@link #deleteQuery(Object...)} with it.
     * This method allows you to provide a suite of {@link Option} to include in
     * the DELETE query. Note : currently, only {@link com.datastax.driver.mapping.Mapper.Option.Timestamp}
     * is supported for DELETE queries.
     * <p/>
     * This method is useful if you want to setup a number of options (tracing,
     * conistency level, ...) of the returned statement before executing it manually
     * or need access to the {@code ResultSet} object after execution (to get the
     * trace, the execution info, ...), but in other cases, calling {@link #delete}
     * or {@link #deleteAsync} is shorter.
     *
     * @param entity  the entity to delete.
     * @param options the options to add to the DELETE query.
     * @return a query that delete {@code entity} (based on it's defined mapping) with
     * provided USING options.
     */
    public Statement deleteQuery(T entity, Option... options) {
        Object[] pks = new Object[mapper.primaryKeySize()];
        for (int i = 0; i < pks.length; i++)
            pks[i] = mapper.getPrimaryKeyColumn(i).getValue(entity);

        return deleteQuery(pks, Arrays.asList(options));
    }

    /**
     * Creates a query that can be used to delete the provided entity.
     * <p/>
     * This method is a shortcut that extract the PRIMARY KEY from the
     * provided entity and call {@link #deleteQuery(Object...)} with it.
     * <p/>
     * This method is useful if you want to setup a number of options (tracing,
     * conistency level, ...) of the returned statement before executing it manually
     * or need access to the {@code ResultSet} object after execution (to get the
     * trace, the execution info, ...), but in other cases, calling {@link #delete}
     * or {@link #deleteAsync} is shorter.
     *
     * @param entity the entity to delete.
     * @return a query that delete {@code entity} (based on it's defined mapping).
     */
    public Statement deleteQuery(T entity) {
        Object[] pks = new Object[mapper.primaryKeySize()];
        for (int i = 0; i < pks.length; i++)
            pks[i] = mapper.getPrimaryKeyColumn(i).getValue(entity);

        return deleteQuery(pks);
    }

    /**
     * Creates a query that can be used to delete an entity given its PRIMARY KEY.
     * <p/>
     * The values provided must correspond to the columns composing the PRIMARY
     * KEY (in the order of said primary key). The values can also contain, after
     * specifying the primary keys columns, a suite of {@link Option} to include in
     * the DELETE query. Note : currently, only {@link com.datastax.driver.mapping.Mapper.Option.Timestamp}
     * is supported for DELETE queries.
     * <p/>
     * <p/>
     * This method is useful if you want to setup a number of options (tracing,
     * conistency level, ...) of the returned statement before executing it manually
     * or need access to the {@code ResultSet} object after execution (to get the
     * trace, the execution info, ...), but in other cases, calling {@link #delete}
     * or {@link #deleteAsync} is shorter.
     * <p/>
     *
     * @param args the primary key of the entity to delete, or more precisely
     *             the values for the columns of said primary key in the order of the primary key.
     *             Can be followed by {@link Option} to include in the DELETE
     *             query.
     * @return a query that delete the entity of PRIMARY KEY {@code primaryKey}.
     * @throws IllegalArgumentException if the number of value provided differ from
     *                                  the number of columns composing the PRIMARY KEY of the mapped class, or if
     *                                  at least one of those values is {@code null}.
     */
    public Statement deleteQuery(Object... args) {
        List<Object> pks = new ArrayList<Object>();
        List<Option> options = new ArrayList<Option>();
        for (Object o : args) {
            if (o instanceof Option) {
                options.add((Option)o);
            } else {
                pks.add(o);
            }
        }
        return deleteQuery(pks.toArray(), options);
    }

    private Statement deleteQuery(Object[] primaryKey, List<Option> options) {
        if (primaryKey.length != mapper.primaryKeySize())
            throw new IllegalArgumentException(String.format("Invalid number of PRIMARY KEY columns provided, %d expected but got %d", mapper.primaryKeySize(), primaryKey.length));

        PreparedStatement ps;
        if (options.size() != 0){
            ps = getPreparedQuery(QueryType.DEL, options.toArray(new Option[options.size()]));
        }
        else {
            ps = getPreparedQuery(QueryType.DEL, this.defaultDeleteOptions);
        }

        BoundStatement bs = ps.bind();
        int i;
        for (i = 0; i < primaryKey.length; i++) {
            ColumnMapper<T> column = mapper.getPrimaryKeyColumn(i);
            Object value = primaryKey[i];
            if (value == null)
                throw new IllegalArgumentException(String.format("Invalid null value for PRIMARY KEY column %s (argument %d)", column.getColumnName(), i));
            bs.setBytesUnsafe(i, column.getDataType().serialize(value, protocolVersion));
        }

        for (Option opt : options) {
            if (opt instanceof Option.Ttl) {
                Option.Ttl ttlOption = (Option.Ttl)opt;
                bs.setInt(i++, ttlOption.getValue());
            }
        }

        if (mapper.writeConsistency != null)
            bs.setConsistencyLevel(mapper.writeConsistency);
        return bs;
    }

    /**
     * Deletes an entity mapped by this mapper.
     * <p/>
     * This method is basically equivalent to: {@code getManager().getSession().execute(deleteQuery(entity))}.
     *
     * @param entity the entity to delete.
     */
    public void delete(T entity) {
        session().execute(deleteQuery(entity));
    }

    /**
     * Deletes an entity mapped by this mapper.
     * <p/>
     * This method is basically equivalent to: {@code getManager().getSession().execute(deleteQuery(entity, options))}.
     *
     * @param entity the entity to delete.
     */
    public void delete(T entity, Option... options) {
        session().execute(deleteQuery(entity, options));
    }

    /**
     * Deletes an entity mapped by this mapper asynchronously.
     * <p/>
     * This method is basically equivalent to: {@code getManager().getSession().executeAsync(deleteQuery(entity))}.
     *
     * @param entity the entity to delete.
     * @return a future on the completion of the deletion.
     */
    public ListenableFuture<Void> deleteAsync(T entity) {
        return Futures.transform(session().executeAsync(deleteQuery(entity)), NOOP);
    }

    /**
     * Deletes an entity mapped by this mapper asynchronously.
     * <p/>
     * This method is basically equivalent to: {@code getManager().getSession().executeAsync(deleteQuery(entity, options))}.
     *
     * @param entity the entity to delete.
     * @return a future on the completion of the deletion.
     */
    public ListenableFuture<Void> deleteAsync(T entity, Option... options) {
        return Futures.transform(session().executeAsync(deleteQuery(entity, options)), NOOP);
    }

    /**
     * Deletes an entity based on its primary key.
     * <p/>
     * This method is basically equivalent to: {@code getManager().getSession().execute(deleteQuery(objects))}.
     *
     * @param objects the primary key of the entity to delete, or more precisely
     *                the values for the columns of said primary key in the order
     *                of the primary key.Can be followed by {@link Option} to include
     *                in the DELETE query.
     * @throws IllegalArgumentException if the number of value provided differ from
     *                                  the number of columns composing the PRIMARY KEY of the mapped class, or if
     *                                  at least one of those values is {@code null}.
     */
    public void delete(Object... objects) {
        session().execute(deleteQuery(objects));
    }

    /**
     * Deletes an entity based on its primary key asynchronously.
     * <p/>
     * This method is basically equivalent to: {@code getManager().getSession().executeAsync(deleteQuery(objects))}.
     *
     * @param objects the primary key of the entity to delete, or more precisely
     *                the values for the columns of said primary key in the order
     *                of the primary key.Can be followed by {@link Option} to include
     *                in the DELETE query.
     * @throws IllegalArgumentException if the number of value provided differ from
     *                                  the number of columns composing the PRIMARY KEY of the mapped class, or if
     *                                  at least one of those values is {@code null}.
     */
    public ListenableFuture<Void> deleteAsync(Object... objects) {
        return Futures.transform(session().executeAsync(deleteQuery(objects)), NOOP);
    }

    /**
     * Map the rows from a {@code ResultSet} into the class this is mapper of.
     *
     * @param resultSet the {@code ResultSet} to map.
     * @return the mapped result set. Note that the returned mapped result set
     * will encapsulate {@code resultSet} and so consuming results from this
     * returned mapped result set will consume results from {@code resultSet}
     * and vice-versa.
     */
    public Result<T> map(ResultSet resultSet) {
        return new Result<T>(resultSet, mapper, protocolVersion);
    }

    /**
     * Creates a query to fetch entity given its PRIMARY KEY.
     * <p/>
     * The values provided must correspond to the columns composing the PRIMARY
     * KEY (in the order of said primary key).
     * <p/>
     * This method is useful if you want to setup a number of options (tracing,
     * conistency level, ...) of the returned statement before executing it manually,
     * but in other cases, calling {@link #get} or {@link #getAsync} is shorter.
     *
     * @param primaryKey the primary key of the entity to fetch, or more precisely
     *                   the values for the columns of said primary key in the order of the primary key.
     * @return a query that fetch the entity of PRIMARY KEY {@code primaryKey}.
     * @throws IllegalArgumentException if the number of value provided differ from
     *                                  the number of columns composing the PRIMARY KEY of the mapped class, or if
     *                                  at least one of those values is {@code null}.
     */
    public Statement getQuery(Object... primaryKey) {
        if (primaryKey.length != mapper.primaryKeySize())
            throw new IllegalArgumentException(String.format("Invalid number of PRIMARY KEY columns provided, %d expected but got %d", mapper.primaryKeySize(), primaryKey.length));

        PreparedStatement ps = getPreparedQuery(QueryType.GET);

        BoundStatement bs = ps.bind();
        for (int i = 0; i < primaryKey.length; i++) {
            ColumnMapper<T> column = mapper.getPrimaryKeyColumn(i);
            Object value = primaryKey[i];
            if (value == null)
                throw new IllegalArgumentException(String.format("Invalid null value for PRIMARY KEY column %s (argument %d)", column.getColumnName(), i));
            bs.setBytesUnsafe(i, column.getDataType().serialize(value, protocolVersion));
        }

        if (mapper.readConsistency != null)
            bs.setConsistencyLevel(mapper.readConsistency);
        return bs;
    }

    /**
     * Fetch an entity based on its primary key.
     * <p/>
     * This method is basically equivalent to: {@code map(getManager().getSession().execute(getQuery(primaryKey))).one()}.
     *
     * @param primaryKey the primary key of the entity to fetch, or more precisely
     *                   the values for the columns of said primary key in the order of the primary key.
     * @return the entity fetched or {@code null} if it doesn't exist.
     * @throws IllegalArgumentException if the number of value provided differ from
     *                                  the number of columns composing the PRIMARY KEY of the mapped class, or if
     *                                  at least one of those values is {@code null}.
     */
    public T get(Object... primaryKey) {
        return map(session().execute(getQuery(primaryKey))).one();
    }

    /**
     * Fetch an entity based on its primary key asynchronously.
     * <p/>
     * This method is basically equivalent to mapping the result of: {@code getManager().getSession().executeAsync(getQuery(primaryKey))}.
     *
     * @param primaryKey the primary key of the entity to fetch, or more precisely
     *                   the values for the columns of said primary key in the order of the primary key.
     * @return a future on the fetched entity. The return future will yield
     * {@code null} if said entity doesn't exist.
     * @throws IllegalArgumentException if the number of value provided differ from
     *                                  the number of columns composing the PRIMARY KEY of the mapped class, or if
     *                                  at least one of those values is {@code null}.
     */
    public ListenableFuture<T> getAsync(Object... primaryKey) {
        return Futures.transform(session().executeAsync(getQuery(primaryKey)), mapOneFunction);
    }

    /**
     * Set the default save {@link Option} for this object mapper, that will be used
     * in all save operations. Refer to {@link Mapper#save)} with Option argument
     * to check available save options.
     *
     * @param options the options to set. To reset, use {@link Mapper#resetDefaultSaveOptions}
     *                instead of putting null argument here.
     */
    public void setDefaultSaveOptions(Option... options) {
        this.defaultSaveOptions = Arrays.copyOf(options, options.length);
    }

    /**
     * Reset the default save options for this object mapper.
     */
    public void resetDefaultSaveOptions() {
        this.defaultSaveOptions = null;
    }

    /**
     * Set the default delete {@link Option} for this object mapper, that will be used
     * in all delete operations. Refer to {@link Mapper#delete)} with Option argument
     * to check available delete options.
     *
     * @param options the options to set. To reset, use {@link Mapper#resetDefaultDeleteOptions}
     *                instead of putting null argument here.
     */
    public void setDefaultDeleteOptions(Option... options) {
        this.defaultDeleteOptions = Arrays.copyOf(options, options.length);
    }

    /**
     * Reset the default delete options for this object mapper.
     */
    public void resetDefaultDeleteOptions() {
        this.defaultDeleteOptions = null;
    }

    /**
     * An object to allow defining specific options during a
     * {@link Mapper#save(Object)} or {@link Mapper#delete(Object...)} operation.
     * <p/>
     * The options will be added as : 'INSERT [...] USING option-name option-value [AND option-name option value... ].
     */
    public static abstract class Option {

        static class Ttl extends Option {

            private int ttlValue;

            Ttl(int value) {
                this.ttlValue = value;
            }

            /**
             * Get the TTL value configured in the object.
             *
             * @return the TTL value.
             */
            public int getValue() {
                return this.ttlValue;
            }
        }

        static class Timestamp extends Option {

            private long tsValue;

            Timestamp(long value) {
                this.tsValue = value;
            }

            /**
             * Get the TIMESTAMP value configured in the object.
             *
             * @return the TIMESTAMP value.
             */
            public long getValue() {
                return this.tsValue;
            }
        }

        /**
         * Creates a new SaveOptions object for adding a TTL value in a save
         * or delete operation.
         *
         * @param value the value to use for the operation.
         * @return the SaveOptions object configured to set a TTL value to a
         * save or delete operation.
         */
        public static Option ttl(int value) {
            return new Ttl(value);
        }

        /**
         * Creates a new SaveOptions object for adding a TIMESTAMP value in a save
         * or delete operation.
         *
         * @param value the value to use for the operation.
         * @return the SaveOptions object configured to set a TIMESTAMP value to a
         * save or delete operation.
         */
        public static Option timestamp(long value) {
            return new Timestamp(value);
        }
    }

}
