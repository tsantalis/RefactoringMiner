/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package enmasse.broker.prestop;

import enmasse.discovery.Host;
import io.enmasse.amqp.Artemis;
import io.enmasse.amqp.PubSubBroker;
import io.vertx.core.Vertx;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.proton.ProtonClientOptions;
import org.apache.qpid.proton.Proton;
import org.apache.qpid.proton.amqp.messaging.AmqpValue;
import org.apache.qpid.proton.message.Message;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

@ExtendWith(VertxExtension.class)
public class QueueDrainerTest {
    private QueueDrainer client;
    private PubSubBroker fromServer;
    private PubSubBroker toServer;
    private TestManagementServer localBroker;
    private Host from;
    private Host to;
    private Vertx vertx;


    @BeforeEach
    public void setup(VertxTestContext context) throws Exception {
        vertx = Vertx.vertx();
        fromServer = new PubSubBroker("fromServer");
        toServer = new PubSubBroker("toServer");
        vertx.deployVerticle(fromServer, ar -> {
            if (ar.succeeded()) {
                context.completeNow();
            } else {
                context.failNow(ar.cause());
            }
        });

        vertx.deployVerticle(toServer, ar -> {
            if (ar.succeeded()) {
                context.completeNow();
            } else {
                context.failNow(ar.cause());
            }
        });
        context.awaitCompletion(30, TimeUnit.SECONDS);
        from = TestUtil.createHost("127.0.0.1", fromServer.port());
        to = TestUtil.createHost("127.0.0.1", toServer.port());
        localBroker = new TestManagementServer();

        client = new QueueDrainer(Vertx.vertx(), from, (vertx, clientOptions, endpoint) -> new Artemis(localBroker), new ProtonClientOptions(), Optional.empty());
    }

    @Test
    public void testDrain() throws Exception {
        sendMessages(fromServer, "myqueue", "testfrom", 100);
        sendMessages(fromServer, "queue2", "q2from", 10);
        sendMessages(toServer, "myqueue", "testto", 100);
        sendMessages(toServer, "queue2", "q2to", 1);

        System.out.println("Starting drain");

        localBroker.setHandler(message -> {
            Map<String, Object> props = message.getApplicationProperties().getValue();
            Message response = Proton.message();
            String resourceName = (String) props.get("_AMQ_ResourceName");
            if ("broker".equals(resourceName) &&
                    "getQueueNames".equals(props.get("_AMQ_OperationName"))) {
                response.setBody(new AmqpValue("[[\"myqueue\"],[\"queue2\"]]"));
            } else if ("queue.myqueue".equals(resourceName) &&
                    "messageCount".equals(props.get("_AMQ_Attribute"))) {
                response.setBody(new AmqpValue("[" + fromServer.numMessages("myqueue") + "]"));
            } else if ("queue.queue2".equals(resourceName) &&
                    "messageCount".equals(props.get("_AMQ_Attribute"))) {
                response.setBody(new AmqpValue("[" + fromServer.numMessages("queue2") + "]"));
            } else {
                response.setBody(new AmqpValue("[]"));
            }
            return response;
        });

        client.drainMessages(to.amqpEndpoint(), "");
        assertThat(toServer.numMessages("myqueue"), is(200));
        assertThat(toServer.numMessages("queue2"), is(11));

        assertReceive(toServer, "myqueue", "testto", 100);
        assertReceive(toServer, "myqueue", "testfrom", 100);
        assertReceive(toServer, "queue2", "q2to", 1);
        assertReceive(toServer, "queue2", "q2from", 10);

        System.out.println("Checking shutdown");
    }

    private static void sendMessages(PubSubBroker broker, String address, String prefix, int numMessages) throws IOException, InterruptedException {
        List<String> messages = IntStream.range(0, numMessages)
                .mapToObj(i -> prefix + i)
                .collect(Collectors.toList());
        broker.sendMessages(address, messages);
    }

    private static void assertReceive(PubSubBroker broker, String address, String prefix, int numMessages) throws IOException, InterruptedException {
        List<String> messages = broker.recvMessages(address, numMessages);
        for (int i = 0; i < numMessages; i++) {
            String actualBody = messages.get(i);
            String expectedBody = prefix + i;
            assertThat(actualBody, is(expectedBody));
        }
    }
}
