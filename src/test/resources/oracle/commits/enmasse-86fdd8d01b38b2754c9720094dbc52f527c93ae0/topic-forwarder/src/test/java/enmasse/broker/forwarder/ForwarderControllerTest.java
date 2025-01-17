/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package enmasse.broker.forwarder;

import enmasse.discovery.Host;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

@RunWith(VertxUnitRunner.class)
public class ForwarderControllerTest {
    private static final Logger log = LoggerFactory.getLogger(ForwarderControllerTest.class.getName());
    private Vertx vertx = Vertx.vertx(new VertxOptions().setWorkerPoolSize(10));
    private String localHost = "127.0.0.1";
    private final String address = "mytopic";
    private TestBroker serverA;
    private TestBroker serverB;
    private TestBroker serverC;

    @Before
    public void setup(TestContext testContext) throws Exception {
        serverA = new TestBroker(1, address);
        serverB = new TestBroker(2, address);
        serverC = new TestBroker(3, address);
        vertx.deployVerticle(serverA, testContext.asyncAssertSuccess());
        vertx.deployVerticle(serverB, testContext.asyncAssertSuccess());
        vertx.deployVerticle(serverC, testContext.asyncAssertSuccess());
    }

    @After
    public void teardown() throws Exception {
        vertx.close();
    }

    @Test
    public void testBrokerReplicator() throws InterruptedException, TimeoutException, ExecutionException {
        Host hostA = new Host(localHost, Collections.singletonMap("amqp", serverA.getPort()));
        Host hostB = new Host(localHost, Collections.singletonMap("amqp", serverB.getPort()));
        Host hostC = new Host(localHost, Collections.singletonMap("amqp", serverC.getPort()));

        ForwarderController replicator = new ForwarderController(hostA, address, null);
        CountDownLatch latch = new CountDownLatch(1);
        vertx.deployVerticle(replicator, id -> latch.countDown());
        latch.await(1, TimeUnit.MINUTES);

        Set<Host> hosts = new LinkedHashSet<>();
        hosts.add(hostB);
        replicator.hostsChanged(hosts);
        Thread.sleep(5000);
        hosts.add(hostC);
        replicator.hostsChanged(hosts);

        long timeout = 60_000;
        waitForConnections(serverA, 2, timeout);
        waitForConnections(serverB, 1, timeout);
        waitForConnections(serverC, 1, timeout);

        CompletableFuture<List<String>> resultB = serverB.recvMessages(2, 60, TimeUnit.SECONDS);
        CompletableFuture<List<String>> resultC = serverC.recvMessages(2, 60, TimeUnit.SECONDS);

        serverA.sendMessage("Hello 1", 60, TimeUnit.SECONDS);
        serverA.sendMessage("Hello 2", 60, TimeUnit.SECONDS);

        assertMessages(resultB.get(120, TimeUnit.SECONDS), "Hello 1", "Hello 2");
        assertMessages(resultC.get(120, TimeUnit.SECONDS), "Hello 1", "Hello 2");

        hosts.remove(hostB);
        replicator.hostsChanged(hosts);

        waitForConnections(serverA, 2, timeout);
        waitForConnections(serverB, 0, timeout);
        waitForConnections(serverC, 1, timeout);

        resultC = serverC.recvMessages(2, 60, TimeUnit.SECONDS);
        serverA.sendMessage("Hello 3", 60, TimeUnit.SECONDS);
        serverA.sendMessage("Hello 4", 60, TimeUnit.SECONDS);
        assertMessages(resultC.get(120, TimeUnit.SECONDS), "Hello 3", "Hello 4");
    }

    private void assertMessages(List<String> result, String...messages) {
        assertThat(messages.length, is(2));
        for (String message : messages) {
            assertThat(result, hasItem(message));
        }
    }

    private static void waitForConnections(TestBroker server, int num, long timeout) throws InterruptedException {
        long endTime = System.currentTimeMillis() + timeout;
        while (System.currentTimeMillis() < endTime) {
            log.info("Num connected on " + server.getId() + " is : " + server.numConnected());
            if (server.numConnected() >= num) {
                break;
            }
            Thread.sleep(1000);
        }
        assertTrue(server.numConnected() >= num);
    }
}
