/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.amqp;

import io.vertx.core.Vertx;
import io.vertx.proton.ProtonServer;
import io.vertx.proton.ProtonSession;
import org.apache.qpid.proton.amqp.messaging.AmqpValue;
import org.apache.qpid.proton.message.Message;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

public class BlockingClientTest {
    private static final Logger log = LoggerFactory.getLogger(BlockingClientTest.class);

    private Vertx vertx;
    private ProtonServer server;
    private BlockingQueue<Message> inbox;
    private BlockingQueue<Message> outbox;
    private int actualPort;

    @Before
    public void setup() throws Exception {
        vertx = Vertx.vertx();
        server = ProtonServer.create(vertx);
        inbox = new LinkedBlockingDeque<>();
        outbox = new LinkedBlockingDeque<>();
        CountDownLatch latch = new CountDownLatch(1);
        server.connectHandler(conn -> {
            conn.closeHandler(c -> {
                conn.close();
                conn.disconnect();
            });
            conn.disconnectHandler(c -> {
                conn.disconnect();
            }).open();

            conn.sessionOpenHandler(ProtonSession::open);

            conn.receiverOpenHandler(receiver -> {
                log.debug("Receiver open");
                receiver.setTarget(receiver.getRemoteTarget());
                receiver.handler((delivery, message) -> {
                    inbox.add(message);
                });
                receiver.open();
            });

            conn.senderOpenHandler(sender -> {
                vertx.setPeriodic(100, id -> {
                    try {
                        Message m = outbox.poll(0, TimeUnit.SECONDS);
                        if (m != null) {
                            sender.send(m);
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        // Try again later
                    }
                });
                sender.open();
            });
        }).listen(0, res -> latch.countDown());
        latch.await();
        actualPort = server.actualPort();
        log.debug("Using port {}", actualPort);
    }

    @Test
    public void testSend() throws Exception {
        try (BlockingClient client = new BlockingClient("127.0.0.1", actualPort)) {
            Message m = Message.Factory.create();
            m.setAddress("testsend");
            m.setBody(new AmqpValue("hello there"));
            client.send("testsend", Arrays.asList(m), 1, TimeUnit.MINUTES);

            Message received = inbox.poll(1, TimeUnit.MINUTES);
            assertNotNull(received);
            assertThat(received.getMessageId(), is(m.getMessageId()));
            assertThat(((AmqpValue)received.getBody()).getValue(), is("hello there"));
        }
    }

    @Test
    public void testReceive() throws Exception {
        try (BlockingClient client = new BlockingClient("127.0.0.1", actualPort)) {
            Message m = Message.Factory.create();
            m.setAddress("testreceive");
            m.setBody(new AmqpValue("hello there"));
            outbox.put(m);

            List<Message> messages = client.recv("testsend", 1, 1, TimeUnit.MINUTES);
            assertThat(messages.size(), is(1));

            Message received = messages.get(0);
            assertNotNull(received);
            assertThat(received.getMessageId(), is(m.getMessageId()));
            assertThat(((AmqpValue)received.getBody()).getValue(), is("hello there"));
        }
    }

    @After
    public void teardown() {
        server.close();
        vertx.close();
    }
}
