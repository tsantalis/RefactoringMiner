/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.amqp;

import io.vertx.core.Vertx;
import io.vertx.proton.ProtonSender;
import io.vertx.proton.ProtonServer;
import io.vertx.proton.ProtonSession;
import org.apache.qpid.proton.amqp.messaging.AmqpValue;
import org.apache.qpid.proton.message.Message;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertTrue;

public class ProtonRequestClientTest {
    private Vertx vertx;
    private ProtonServer server;

    @Before
    public void setup() throws InterruptedException {
        vertx = Vertx.vertx();
        server = ProtonServer.create(vertx);
        CountDownLatch latch = new CountDownLatch(1);
        CompletableFuture<ProtonSender> futureSender = new CompletableFuture<>();
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
                System.out.println("Receiver open");
                receiver.setTarget(receiver.getRemoteTarget());
                receiver.handler((delivery, message) -> {
                    Message response = Message.Factory.create();
                    response.setAddress(message.getAddress());
                    response.setBody(new AmqpValue(true));
                    response.setCorrelationId(message.getCorrelationId());
                    response.setReplyTo(message.getReplyTo());
                    try {
                        futureSender.get().send(response);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
                receiver.open();
            });

            conn.senderOpenHandler(sender -> {
                sender.setSource(sender.getRemoteSource());
                sender.open();
                futureSender.complete(sender);
            });
        }).listen(12347, res -> {
            latch.countDown();
        });
        latch.await();
    }

    @After
    public void teardown() {
        server.close();
        vertx.close();
    }

    @Test
    public void testRequest() throws Exception {
        try (ProtonRequestClient client = new ProtonRequestClient(Vertx.vertx(), "test-client")) {
            CompletableFuture<Void> future = new CompletableFuture<>();
            client.connect("127.0.0.1", 12347, future);
            future.get(10, TimeUnit.SECONDS);
            Message request = Message.Factory.create();
            request.setAddress("health-check");
            request.setBody(new AmqpValue("[{\"name\":\"myqueue\",\"store_and_forward\":true,\"multicast\":false}]"));
            request.setSubject("health-check");
            Message response = client.request(request, 10, TimeUnit.SECONDS);
            assertTrue((Boolean) ((AmqpValue) response.getBody()).getValue());

            future = new CompletableFuture<>();
            client.connect("127.0.0.1", 12347, future);
            future.get(10, TimeUnit.SECONDS);
        }
    }
}
