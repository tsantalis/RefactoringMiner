/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.camel.component.sjms.batch;

import org.apache.camel.Endpoint;
import org.apache.camel.impl.UriEndpointComponent;
import org.apache.camel.util.ObjectHelper;

import javax.jms.ConnectionFactory;
import java.util.Map;

/**
 * @author jkorab
 */
public class SjmsBatchComponent extends UriEndpointComponent {

    private ConnectionFactory connectionFactory;

    public SjmsBatchComponent() {
        super(SjmsBatchEndpoint.class);
    }

    @Override
    protected Endpoint createEndpoint(String uri, String remaining, Map<String, Object> parameters) throws Exception {
        ObjectHelper.notNull(connectionFactory, "connectionFactory is null");
        SjmsBatchEndpoint sjmsBatchEndpoint = new SjmsBatchEndpoint(uri, this, remaining);
        setProperties(sjmsBatchEndpoint, parameters);
        return sjmsBatchEndpoint;
    }

    public ConnectionFactory getConnectionFactory() {
        return connectionFactory;
    }

    public void setConnectionFactory(ConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

}
