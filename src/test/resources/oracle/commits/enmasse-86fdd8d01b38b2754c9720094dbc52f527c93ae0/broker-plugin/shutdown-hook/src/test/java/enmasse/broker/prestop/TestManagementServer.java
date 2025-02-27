/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package enmasse.broker.prestop;

import io.enmasse.amqp.SyncRequestClient;
import io.vertx.proton.ProtonClientOptions;
import org.apache.qpid.proton.message.Message;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

class TestManagementServer implements SyncRequestClient {

    private Handler handler;

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

    public void setHandler(Handler handler) {
        this.handler = handler;
    }

    @Override
    public Message request(Message message, long timeout, TimeUnit timeUnit) {
        return handler.handle(message);
    }

    public interface Handler {
        Message handle(Message message);
    }
}
