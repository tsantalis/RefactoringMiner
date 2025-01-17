/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package enmasse.broker.prestop;

import enmasse.discovery.Endpoint;
import io.vertx.core.Vertx;
import io.vertx.proton.ProtonClient;
import io.vertx.proton.ProtonConnection;
import io.vertx.proton.ProtonLinkOptions;
import io.vertx.proton.ProtonReceiver;
import org.apache.qpid.proton.amqp.Symbol;
import org.apache.qpid.proton.amqp.messaging.Source;
import org.apache.qpid.proton.amqp.messaging.TerminusDurability;
import org.apache.qpid.proton.message.Message;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipFile;

public class TestSubscriber implements AutoCloseable {
    private final Vertx vertx = Vertx.vertx();
    private volatile ProtonConnection connection;

    private final BlockingQueue<Message> received = new LinkedBlockingQueue<>();

    public void subscribe(Endpoint endpoint, String address) throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        String containerId = "test-subscriber";
        ProtonClient client = ProtonClient.create(vertx);
        client.connect(endpoint.hostname(), endpoint.port(), connection -> {
            if (connection.succeeded()) {
                ProtonConnection conn = connection.result();
                conn.setContainer(containerId);
                conn.closeHandler(res -> {
                    System.out.println("CLIENT cONN cLOSED");
                });
                this.connection = conn;
                System.out.println("Connected: " + connection.result().getRemoteContainer());
                Source source = new Source();
                source.setAddress(address);
                source.setCapabilities(Symbol.getSymbol("topic"));
                source.setDurable(TerminusDurability.UNSETTLED_STATE);
                ProtonReceiver receiver = conn.createReceiver(address, new ProtonLinkOptions().setLinkName(containerId));
                receiver.setSource(source);
                receiver.openHandler(res -> {
                    if (res.succeeded()) {
                        System.out.println("Opened receiver");
                        latch.countDown();
                    } else {
                        System.out.println("Failed opening received: " + res.cause().getMessage());
                    }
                });
                receiver.closeHandler(res -> {
                    System.out.println("CLIENT CLOSED");
                    conn.close();
                });
                receiver.handler((delivery, message) -> {
                    System.out.println("GOT MESSAGE");
                    received.add(message);
                });
                receiver.open();
                conn.open();
            } else {
                System.out.println("Connection failed: " + connection.cause().getMessage());
            }
        });
        latch.await(1, TimeUnit.MINUTES);
    }

    public void unsubscribe() {
        vertx.runOnContext(v -> connection.close());
    }

    @Override
    public void close() throws Exception {
        unsubscribe();
        vertx.close();
    }


    public Message receiveMessage(long timeout, TimeUnit timeUnit) throws InterruptedException {
        return received.poll(timeout, timeUnit);
    }
}
