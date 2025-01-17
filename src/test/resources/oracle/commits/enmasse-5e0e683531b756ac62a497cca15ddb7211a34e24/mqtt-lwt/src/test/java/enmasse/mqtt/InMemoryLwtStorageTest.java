/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package enmasse.mqtt;

import enmasse.mqtt.messages.AmqpWillMessage;
import enmasse.mqtt.storage.LwtStorage;
import enmasse.mqtt.storage.impl.InMemoryLwtStorage;
import io.netty.handler.codec.mqtt.MqttQoS;
import io.vertx.core.buffer.Buffer;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests related to the LWT storage service only
 */
@ExtendWith(VertxExtension.class)
public class InMemoryLwtStorageTest {

    protected final Logger LOG = LoggerFactory.getLogger(InMemoryLwtStorageTest.class);
    private static final AmqpWillMessage WILL_MESSAGE =
            new AmqpWillMessage(true, "will_topic", MqttQoS.AT_MOST_ONCE, Buffer.buffer("Hello"));
    private static final String CLIENT_ID = "client_id";
    private LwtStorage lwtStorage;

    @BeforeEach
    public void before(VertxTestContext context) {

        this.lwtStorage = new InMemoryLwtStorage();
        this.lwtStorage.open(context.succeeding(id -> context.completeNow()));
    }

    @AfterEach
    public void after() {
        this.lwtStorage.close();
    }

    @Test
    public void addNotExistingWill(VertxTestContext context) {
        this.lwtStorage.add(CLIENT_ID, WILL_MESSAGE, done -> {
            if (done.succeeded()) {
                this.lwtStorage.get(CLIENT_ID, done1 -> {
                    if (done1.succeeded()) {
                        AmqpWillMessage willMessage1 = done1.result();
                        context.verify(() -> assertEquals(willMessage1, WILL_MESSAGE));
                        LOG.info("Added a not existing will");
                    } else {
                        context.verify(() -> fail(""));
                    }
                    context.completeNow();
                });
            } else {
                context.verify(() -> fail(""));
                context.completeNow();
            }
        });
    }

    @Test
    public void addExistingWill(VertxTestContext context) {
        this.lwtStorage.add(CLIENT_ID, WILL_MESSAGE, done -> {
            if (done.succeeded()) {
                this.lwtStorage.add(CLIENT_ID, WILL_MESSAGE, done1 -> {
                    context.verify(() -> assertTrue(!done1.succeeded()));
                    LOG.info("Will to add already exists");
                    context.completeNow();
                });
            } else {
                context.verify(() -> fail(""));
                context.completeNow();
            }
        });
    }

    @Test
    public void updateExistingWill(VertxTestContext context) {
        this.lwtStorage.add(CLIENT_ID, WILL_MESSAGE, done -> {
            if (done.succeeded()) {
                AmqpWillMessage willMessage1 = new AmqpWillMessage(false, "will_topic_1", MqttQoS.AT_LEAST_ONCE, Buffer.buffer("Hello_1"));
                this.lwtStorage.update(CLIENT_ID, willMessage1, done1 -> {
                    if (done1.succeeded()) {
                        this.lwtStorage.get(CLIENT_ID, done2 -> {
                            if (done2.succeeded()) {
                                AmqpWillMessage willMessage2 = done2.result();
                                // updated message not equals to the original one but to the one got from the storage
                                context.verify(() -> assertTrue(!willMessage1.equals(WILL_MESSAGE) && willMessage1.equals(willMessage2)));
                                LOG.info("Existing will updated");

                            } else {
                                context.verify(() -> fail(""));
                            }
                            context.completeNow();
                        });

                    } else {
                        context.verify(() -> fail(""));
                        context.completeNow();
                    }
                });
            } else {
                context.verify(() -> fail(""));
                context.completeNow();
            }
        });
    }

    @Test
    public void updateNotExistingWill(VertxTestContext context) {
        this.lwtStorage.update(CLIENT_ID, WILL_MESSAGE, done -> {
            context.verify(() -> assertTrue(!done.succeeded()));
            LOG.info("Trying to update a not existing will");
            context.completeNow();
        });
    }

    @Test
    public void deleteExistingWill(VertxTestContext context) {
        this.lwtStorage.add(CLIENT_ID, WILL_MESSAGE, done -> {
            if (done.succeeded()) {
                this.lwtStorage.delete(CLIENT_ID, done1 -> {
                    context.verify(() -> assertTrue(done1.succeeded()));
                    LOG.info("Existing will deleted");
                    context.completeNow();
                });
            } else {
                context.verify(() -> fail(""));
                context.completeNow();
            }
        });
    }

    @Test
    public void deleteNotExistingWill(VertxTestContext context) {
        this.lwtStorage.delete(CLIENT_ID, done -> {
            context.verify(() -> assertTrue(!done.succeeded()));
            LOG.info("Trying to delete a not existing will");
            context.completeNow();
        });
    }

}
