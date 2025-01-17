/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.amqp;

import io.vertx.proton.ProtonClientOptions;
import org.apache.qpid.proton.Proton;
import org.apache.qpid.proton.amqp.messaging.AmqpValue;
import org.apache.qpid.proton.message.Message;
import org.junit.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ArtemisTest {

    private static class TestClient implements SyncRequestClient {

        Message response;
        Message request;

        @Override
        public void connect(String host, int port, ProtonClientOptions clientOptions, String address, CompletableFuture<Void> connectedPromise) {

        }

        @Override
        public String getRemoteContainer() {
            return null;
        }

        @Override
        public String getReplyTo() {
            return "tome";
        }

        @Override
        public void close() {

        }

        @Override
        public Message request(Message message, long timeout, TimeUnit timeUnit) {
            request = message;
            return response;
        }
    }

    @Test
    public void testManagement() throws InterruptedException, ExecutionException, TimeoutException {
        TestClient testClient = new TestClient();
        try (Artemis artemis = new Artemis(testClient)) {

            testClient.response = Proton.message();
            artemis.deployQueue("queue1", "queue1");
            String body = (String)((AmqpValue)testClient.request.getBody()).getValue();
            assertEquals("[\"queue1\",\"queue1\",null,false]", body);
            assertEquals("broker", testClient.request.getApplicationProperties().getValue().get("_AMQ_ResourceName"));
            assertEquals("deployQueue", testClient.request.getApplicationProperties().getValue().get("_AMQ_OperationName"));

            artemis.deployQueue("queue2", "queue2");
            body = (String)((AmqpValue)testClient.request.getBody()).getValue();
            assertEquals("[\"queue2\",\"queue2\",null,false]", body);
            assertEquals("broker", testClient.request.getApplicationProperties().getValue().get("_AMQ_ResourceName"));
            assertEquals("deployQueue", testClient.request.getApplicationProperties().getValue().get("_AMQ_OperationName"));

            testClient.response = Proton.message();
            testClient.response.setBody(new AmqpValue("[[\"q1\"],[\"q2\"],[\"tome\"]]"));
            long numQueues = artemis.getNumQueues();
            assertEquals(2, numQueues);
            body = (String)((AmqpValue)testClient.request.getBody()).getValue();
            assertEquals("[]", body);
            assertEquals("broker", testClient.request.getApplicationProperties().getValue().get("_AMQ_ResourceName"));
            assertEquals("getQueueNames", testClient.request.getApplicationProperties().getValue().get("_AMQ_OperationName"));

            testClient.response = Proton.message();
            testClient.response.setBody(new AmqpValue("[42]"));
            long messageCount = artemis.getQueueMessageCount("q1");
            assertEquals(42, messageCount);
            body = (String)((AmqpValue)testClient.request.getBody()).getValue();
            assertEquals("[]", body);
            assertEquals("queue.q1", testClient.request.getApplicationProperties().getValue().get("_AMQ_ResourceName"));
            assertEquals("messageCount", testClient.request.getApplicationProperties().getValue().get("_AMQ_Attribute"));

        }
    }
}
