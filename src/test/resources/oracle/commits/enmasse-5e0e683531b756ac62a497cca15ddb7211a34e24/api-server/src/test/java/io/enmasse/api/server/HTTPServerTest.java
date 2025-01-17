/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.api.server;

import io.enmasse.address.model.Address;
import io.enmasse.address.model.AddressSpace;
import io.enmasse.address.model.EndpointSpec;
import io.enmasse.api.auth.AuthApi;
import io.enmasse.api.auth.SubjectAccessReview;
import io.enmasse.api.auth.TokenReview;
import io.enmasse.k8s.api.TestAddressSpaceApi;
import io.enmasse.metrics.api.Metrics;
import io.enmasse.user.api.UserApi;
import io.enmasse.user.model.v1.*;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.Clock;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import static junit.framework.TestCase.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(VertxExtension.class)
public class HTTPServerTest {

    private Vertx vertx;
    private TestAddressSpaceApi instanceApi;
    private AddressSpace addressSpace;

    @BeforeEach
    public void setup(VertxTestContext context) throws InterruptedException {
        vertx = Vertx.vertx();
        instanceApi = new TestAddressSpaceApi();
        String addressSpaceName = "myinstance";
        addressSpace = createAddressSpace(addressSpaceName);
        instanceApi.createAddressSpace(addressSpace);

        AuthApi authApi = mock(AuthApi.class);
        when(authApi.getNamespace()).thenReturn("controller");
        when(authApi.performTokenReview(eq("mytoken"))).thenReturn(new TokenReview("foo", "myid", true));
        when(authApi.performSubjectAccessReviewResource(eq("foo"), any(), any(), any(), anyString())).thenReturn(new SubjectAccessReview("foo", true));
        when(authApi.performSubjectAccessReviewResource(eq("foo"), any(), any(), any(), anyString())).thenReturn(new SubjectAccessReview("foo", true));

        UserApi userApi = mock(UserApi.class);
        UserList users = new UserList();
        users.getItems().add(new UserBuilder()
                .withMetadata(new ObjectMetaBuilder()
                        .withName("myinstance.user1")
                        .withNamespace("myinstance")
                        .build())
                .withSpec(new UserSpecBuilder()
                        .withUsername("user1")
                        .withAuthentication(new UserAuthenticationBuilder()
                                .withType(UserAuthenticationType.password)
                                .withPassword("admin")
                                .build())
                        .withAuthorization(Arrays.asList(new UserAuthorizationBuilder()
                                .withAddresses(Arrays.asList("queue1"))
                                .withOperations(Arrays.asList(Operation.send, Operation.recv))
                                .build()))
                        .build())
                .build());
        when(userApi.listUsers(any())).thenReturn(users);

        ApiServerOptions options = new ApiServerOptions();
        options.setVersion("1.0");
        options.setCertDir("/doesnotexist");
        vertx.deployVerticle(new HTTPServer(instanceApi, new TestSchemaProvider(), authApi, userApi, new Metrics(), options, null, null, Clock.systemUTC()), context.succeeding(arg -> context.completeNow()));
    }

    @AfterEach
    public void teardown(VertxTestContext context) {
        vertx.close(context.succeeding(arg -> context.completeNow()));
    }

    private AddressSpace createAddressSpace(String name) {
        return new AddressSpace.Builder()
                .setName(name)
                .setNamespace(name)
                .setType("type1")
                .setPlan("myplan")
                .setStatus(new io.enmasse.address.model.AddressSpaceStatus(false))
                .appendEndpoint(new EndpointSpec.Builder()
                        .setName("foo")
                        .setService("messaging")
                        .build())
                .build();
    }

    @Test
    public void testAddressingApi(VertxTestContext context) throws InterruptedException {
        instanceApi.withAddressSpace(addressSpace).createAddress(
                new Address.Builder()
                        .setAddressSpace("myinstance")
                        .setName("myinstance.addr1")
                        .setAddress("addR1")
                        .setNamespace("ns")
                        .setType("queue")
                        .setPlan("myplan")
                        .build());

        HttpClient client = vertx.createHttpClient();
        try {
            {
                HttpClientRequest r1 = client.get(8080, "localhost", "/apis/enmasse.io/v1alpha1/namespaces/ns/addressspaces/myinstance/addresses", response -> {
                    context.verify(() -> assertEquals(200, response.statusCode()));
                    response.bodyHandler(buffer -> {
                        JsonObject data = buffer.toJsonObject();
                        context.verify(() -> {
                            assertTrue(data.containsKey("items"));
                            assertEquals("myinstance.addr1", data.getJsonArray("items").getJsonObject(0).getJsonObject("metadata").getString("name"));
                        });
                        context.completeNow();
                    });
                });
                putAuthzToken(r1);
                r1.end();
                context.awaitCompletion(60, TimeUnit.SECONDS);
            }
            {
                HttpClientRequest r2 = client.get(8080, "localhost", "/apis/enmasse.io/v1alpha1/namespaces/ns/addresses/myinstance.addr1", response -> {
                    response.bodyHandler(buffer -> {
                        JsonObject data = buffer.toJsonObject();
                        context.verify(() -> {
                            assertTrue(data.containsKey("metadata"));
                            assertEquals("myinstance.addr1", data.getJsonObject("metadata").getString("name"));
                        });
                        context.completeNow();
                    });
                });
                putAuthzToken(r2);
                r2.end();
                context.awaitCompletion(60, TimeUnit.SECONDS);
            }
            {
                HttpClientRequest r3 = client.post(8080, "localhost", "/apis/enmasse.io/v1alpha1/namespaces/ns/addressspaces/myinstance/addresses", response -> {
                    response.bodyHandler(buffer -> {
                        context.verify(() -> assertEquals(201, response.statusCode()));
                        context.completeNow();
                    });
                });
                r3.putHeader("Content-Type", "application/json");
                putAuthzToken(r3);
                r3.end("{\"apiVersion\":\"enmasse.io/v1alpha1\",\"kind\":\"AddressList\",\"items\":[{\"metadata\":{\"name\":\"a4\"},\"spec\":{\"address\":\"a4\",\"type\":\"queue\",\"plan\":\"plan1\"}}]}");
                context.awaitCompletion(60, TimeUnit.SECONDS);
            }
            {
                HttpClientRequest r4 = client.get(8080, "localhost", "/apis/enmasse.io/v1alpha1/namespaces/ns/addressspaces/myinstance/addresses?address=addR1", response -> {
                    response.bodyHandler(buffer -> {
                        JsonObject data = buffer.toJsonObject();
                        System.out.println(data.toString());
                        context.verify(() -> {
                            assertTrue(data.containsKey("metadata"));
                            assertEquals("addR1", data.getJsonObject("spec").getString("address"));
                        });
                        context.completeNow();
                    });
                });
                putAuthzToken(r4);
                r4.end();
                context.awaitCompletion(60, TimeUnit.SECONDS);
            }
        } finally {
            client.close();
        }
    }

    private static HttpClientRequest putAuthzToken(HttpClientRequest request) {
        request.putHeader("Authorization", "Bearer mytoken");
        return request;
    }

    @Test
    public void testApiResources(VertxTestContext context) throws InterruptedException {
        HttpClient client = vertx.createHttpClient();
        try {
            {
                HttpClientRequest rootReq = client.get(8080, "localhost", "/apis/enmasse.io/v1alpha1", response -> {
                    context.verify(() -> assertEquals(200, response.statusCode()));
                    response.bodyHandler(buffer -> {
                        JsonObject data = buffer.toJsonObject();
                        context.verify(() -> assertTrue(data.containsKey("resources")));
                        JsonArray resources = data.getJsonArray("resources");
                        context.verify(() -> assertEquals(3, resources.size()));
                        context.completeNow();
                    });
                });
                putAuthzToken(rootReq);
                rootReq.end();
                context.awaitCompletion(60, TimeUnit.SECONDS);
            }
            {
                HttpClientRequest rootReq = client.get(8080, "localhost", "/apis/user.enmasse.io/v1alpha1", response -> {
                    context.verify(() -> assertEquals(200, response.statusCode()));
                    response.bodyHandler(buffer -> {
                        JsonObject data = buffer.toJsonObject();
                        context.verify(() -> assertTrue(data.containsKey("resources")));
                        JsonArray resources = data.getJsonArray("resources");
                        context.verify(() -> assertEquals(1, resources.size()));
                        context.completeNow();
                    });
                });
                putAuthzToken(rootReq);
                rootReq.end();
                context.awaitCompletion(60, TimeUnit.SECONDS);
            }
        } finally {
            client.close();
        }
    }

    @Test
    public void testSchemaApi(VertxTestContext context) throws InterruptedException {
        HttpClient client = vertx.createHttpClient();
        try {
            {
                HttpClientRequest request = client.get(8080, "localhost", "/apis/enmasse.io/v1alpha1/namespaces/myinstance/addressspaceschemas", response -> {
                    context.verify(() -> assertEquals(200, response.statusCode()));
                    response.bodyHandler(buffer -> {
                        JsonObject data = buffer.toJsonObject();
                        System.out.println(data.toString());
                        context.verify(() -> {
                            assertTrue(data.containsKey("items"));
                            assertEquals(1, data.getJsonArray("items").size());
                        });
                        context.completeNow();
                    });
                });
                putAuthzToken(request);
                request.end();
                context.awaitCompletion(60, TimeUnit.SECONDS);
            }
        } finally {
            client.close();
        }
    }

    @Test
    public void testUserApi(VertxTestContext context) throws Exception {
        HttpClient client = vertx.createHttpClient();
        try {
            {
                HttpClientRequest r1 = client.get(8080, "localhost", "/apis/user.enmasse.io/v1alpha1/namespaces/ns/messagingusers", response -> {
                    context.verify(() -> assertEquals(200, response.statusCode()));
                    response.bodyHandler(buffer -> {
                        JsonObject data = buffer.toJsonObject();
                        context.verify(() -> {
                            assertTrue(data.containsKey("items"));
                            assertEquals("myinstance.user1", data.getJsonArray("items").getJsonObject(0).getJsonObject("metadata").getString("name"));
                        });
                        context.completeNow();
                    });
                });
                putAuthzToken(r1);
                r1.end();
                context.awaitCompletion(60, TimeUnit.SECONDS);
            }
        } finally {
            client.close();
        }
    }

    /*
    @Test
    public void testInstanceApi() throws InterruptedException {
        Instance instance = new Instance.Builder(AddressSpaceId.withId("myinstance"))
                .messagingHost(Optional.of("messaging.example.com"))
                .build();
        addressSpaceApi.createAddressSpace(instance);

        HttpClient client = vertx.createHttpClient();
        try {
            {
                final CountDownLatch latch = new CountDownLatch(1);
                client.getNow(8080, "localhost", "/v3/addressspace", response -> {
                    response.bodyHandler(buffer -> {
                        JsonObject data = buffer.toJsonObject();
                        assertTrue(data.containsKey("kind"));
                        assertThat(data.getString("kind"), is("InstanceList"));
                        assertTrue(data.containsKey("items"));
                        JsonArray items = data.getJsonArray("items");
                        assertThat(items.size(), is(1));
                        assertThat(items.getJsonObject(0).getJsonObject("spec").getString("messagingHost"), is("messaging.example.com"));
                        latch.countDown();
                    });
                });
                assertTrue(latch.await(1, TimeUnit.MINUTES));
            }

            {
                final CountDownLatch latch = new CountDownLatch(1);
                client.getNow(8080, "localhost", "/v3/addressspace/myinstance", response -> {
                    response.bodyHandler(buffer -> {
                        JsonObject data = buffer.toJsonObject();
                        assertTrue(data.containsKey("metadata"));
                        assertThat(data.getJsonObject("metadata").getString("name"), is("myinstance"));
                        assertThat(data.getString("kind"), is("Instance"));
                        assertThat(data.getJsonObject("spec").getString("messagingHost"), is("messaging.example.com"));
                        latch.countDown();
                    });
                });
                assertTrue(latch.await(1, TimeUnit.MINUTES));
            }
        } finally {
            client.close();
        }
    }
    */

    @Test
    public void testOpenApiSpec(VertxTestContext context) throws InterruptedException {
        HttpClientOptions options = new HttpClientOptions();
        HttpClient client = vertx.createHttpClient(options);
        try {
            HttpClientRequest request = client.get(8080, "localhost", "/swagger.json", response -> {
                response.bodyHandler(buffer -> {
                    JsonObject data = buffer.toJsonObject();
                    context.verify(() -> assertTrue(data.containsKey("paths")));
                    context.completeNow();
                });
            });
            putAuthzToken(request);
            request.end();
            context.awaitCompletion(60, TimeUnit.SECONDS);
        } finally {
            client.close();
        }
    }
}
