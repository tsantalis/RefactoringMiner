/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package enmasse.broker.prestop;

import enmasse.discovery.Endpoint;
import enmasse.discovery.Host;
import io.enmasse.amqp.Artemis;
import io.enmasse.amqp.PubSubBroker;
import io.vertx.core.Vertx;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.proton.ProtonClientOptions;
import org.apache.qpid.proton.Proton;
import org.apache.qpid.proton.amqp.messaging.AmqpValue;
import org.apache.qpid.proton.message.Message;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

@RunWith(VertxUnitRunner.class)
public class TopicMigratorTest {
    private Host from;
    private Host to;
    private PubSubBroker fromServer;
    private PubSubBroker toServer;
    private TestSubscriber subscriber;
    private TestPublisher publisher;
    private TestManagementServer localBroker;
    private Vertx vertx;

    @Before
    public void setup(TestContext context) throws Exception {
        vertx = Vertx.vertx();
        subscriber = new TestSubscriber();
        publisher = new TestPublisher();
        fromServer = new PubSubBroker("fromServer");
        toServer = new PubSubBroker("toServer");
        Async async = context.async(2);
        vertx.deployVerticle(fromServer, ar -> {
            if (ar.succeeded()) {
                async.countDown();
            } else {
                context.fail(ar.cause());
            }
        });

        vertx.deployVerticle(toServer, ar -> {
            if (ar.succeeded()) {
                async.countDown();
            } else {
                context.fail(ar.cause());
            }
        });
        async.awaitSuccess(30_000);
        localBroker = new TestManagementServer();
        from = TestUtil.createHost("127.0.0.1", fromServer.port());
        to = TestUtil.createHost("127.0.0.1", toServer.port());
    }

    @After
    public void teardown() throws Exception {
        subscriber.close();
        publisher.close();
        vertx.close();
    }

    @Test
    @Ignore
    public void testMigrator() throws Exception {
        System.out.println("Attempting to subscribe");
        subscriber.subscribe(from.amqpEndpoint(), "mytopic");
        subscriber.unsubscribe();

        System.out.println("Publishing message");
        publisher.publish(from.amqpEndpoint(), "mytopic", "hello, world");

        System.out.println("Done publishing");
        TopicMigrator migrator = new TopicMigrator(Vertx.vertx(), from, new Endpoint("messaging.example.com", 5672), (vertx, clientOptions, endpoint) -> new Artemis(localBroker), new ProtonClientOptions());

        localBroker.setHandler(message -> {
            Map<String, Object> props = message.getApplicationProperties().getValue();
            Message response = Proton.message();
            String resourceName = (String) props.get("_AMQ_ResourceName");
            if ("broker".equals(resourceName) &&
                    "getQueueNames".equals(props.get("_AMQ_OperationName"))) {
                response.setBody(new AmqpValue("[[\"mytopic\"]]"));
            } else if ("queue.mytopic".equals(resourceName) &&
                    "getAddress".equals(props.get("_AMQ_OperationName"))) {
                response.setBody(new AmqpValue("[\"mytopic\"]"));
            } else if ("queue.mytopic".equals(resourceName) &&
                    "messageCount".equals(props.get("_AMQ_Attribute"))) {
                response.setBody(new AmqpValue("[" + fromServer.numMessages("mytopic") + "]"));
            } else {
                System.out.println("Props: " + props);
                response.setBody(new AmqpValue("[]"));
            }
            return response;
        });

        System.out.println("Starting migrator");
        migrator.migrate(Collections.singleton(to));

        subscriber.subscribe(to.amqpEndpoint(), "mytopic");
        Message message = subscriber.receiveMessage(1, TimeUnit.MINUTES);
        assertThat(((AmqpValue)message.getBody()).getValue(), is("hello, world"));
    }
}
