/*
 * Copyright (c)  [2011-2015] "Pivotal Software, Inc." / "Neo Technology" / "Graph Aware Ltd."
 *
 * This product is licensed to you under the Apache License, Version 2.0 (the "License").
 * You may not use this product except in compliance with the License.
 *
 * This product may include a number of subcomponents with
 * separate copyright notices and licence terms.  Your use of the source
 * code for these subcomponents is subject to the terms and
 * conditions of the subcomponent's licence, as noted in the LICENSE file.
 */

package org.springframework.data.neo4j.examples.movies.domain.queryresult;

import org.springframework.data.neo4j.annotation.QueryResult;
import org.springframework.data.neo4j.examples.movies.domain.User;
import org.springframework.data.neo4j.examples.movies.domain.User;

/**
 * {@link QueryResult} that wraps entity objects.
 */
@QueryResult
public class EntityWrappingQueryResult {

    private User user;

    public User getUser() {
        return user;
    }

}
