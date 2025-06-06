/*
 * Copyright (c) 2008-2015, Hazelcast, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hazelcast.client.test;

import com.hazelcast.client.HazelcastClient;
import com.hazelcast.client.config.ClientAwsConfig;
import com.hazelcast.client.config.ClientConfig;
import com.hazelcast.client.connection.AddressTranslator;
import com.hazelcast.client.connection.ClientConnectionManager;
import com.hazelcast.client.connection.nio.ClientConnection;
import com.hazelcast.client.connection.nio.ClientConnectionManagerImpl;
import com.hazelcast.client.impl.ClientServiceFactory;
import com.hazelcast.client.impl.HazelcastClientInstanceImpl;
import com.hazelcast.client.impl.protocol.ClientMessage;
import com.hazelcast.client.impl.protocol.codec.MapMessageType;
import com.hazelcast.client.spi.impl.AwsAddressTranslator;
import com.hazelcast.client.spi.impl.DefaultAddressTranslator;
import com.hazelcast.core.HazelcastException;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.instance.MemberImpl;
import com.hazelcast.instance.Node;
import com.hazelcast.instance.TestUtil;
import com.hazelcast.logging.ILogger;
import com.hazelcast.logging.Logger;
import com.hazelcast.nio.Address;
import com.hazelcast.nio.Connection;
import com.hazelcast.nio.ConnectionType;
import com.hazelcast.nio.SocketWritable;
import com.hazelcast.spi.impl.NodeEngineImpl;
import com.hazelcast.test.TestNodeRegistry;
import com.hazelcast.util.ExceptionUtil;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.logging.Level;

public class TestClientRegistry {

    private static final ILogger LOGGER = Logger.getLogger(HazelcastClient.class);
    private final TestNodeRegistry nodeRegistry;

    public TestClientRegistry(TestNodeRegistry nodeRegistry) {
        this.nodeRegistry = nodeRegistry;
    }


    ClientServiceFactory createClientServiceFactory(Address clientAddress) {
        return new MockClientServiceFactory(clientAddress);
    }

    private class MockClientServiceFactory implements ClientServiceFactory {

        private final Address clientAddress;

        public MockClientServiceFactory(Address clientAddress) {
            this.clientAddress = clientAddress;
        }

        @Override
        public ClientConnectionManager createConnectionManager(ClientConfig config, HazelcastClientInstanceImpl client) {
            final ClientAwsConfig awsConfig = config.getNetworkConfig().getAwsConfig();
            AddressTranslator addressTranslator;
            if (awsConfig != null && awsConfig.isEnabled()) {
                try {
                    addressTranslator = new AwsAddressTranslator(awsConfig);
                } catch (NoClassDefFoundError e) {
                    LOGGER.log(Level.WARNING, "hazelcast-cloud.jar might be missing!");
                    throw e;
                }
            } else {
                addressTranslator = new DefaultAddressTranslator();
            }
            return new MockClientConnectionManager(client, addressTranslator, clientAddress);
        }
    }


    private class MockClientConnectionManager extends ClientConnectionManagerImpl {

        private final Address clientAddress;
        private final HazelcastClientInstanceImpl client;

        public MockClientConnectionManager(HazelcastClientInstanceImpl client, AddressTranslator addressTranslator,
                                           Address clientAddress) {
            super(client, addressTranslator);
            this.client = client;
            this.clientAddress = clientAddress;
        }

        @Override
        protected void initializeSelectors(HazelcastClientInstanceImpl client) {

        }

        @Override
        protected void startSelectors() {

        }

        @Override
        protected void shutdownSelectors() {

        }

        @Override
        protected ClientConnection createSocketConnection(Address address) throws IOException {
            if (!alive) {
                throw new HazelcastException("ConnectionManager is not active!!!");
            }
            try {
                HazelcastInstance instance = nodeRegistry.getInstance(address);
                if (instance == null) {
                    throw new IOException("Can not connected to " + address + ": instance does not exist");
                }
                Node node = TestUtil.getNode(instance);
                return new MockedClientConnection(client, connectionIdGen.incrementAndGet(),
                        node.nodeEngine, address, clientAddress);
            } catch (Exception e) {
                throw ExceptionUtil.rethrow(e, IOException.class);
            }
        }


    }


    private class MockedClientConnection extends ClientConnection {
        private volatile long lastReadTime;
        private volatile long lastWriteTime;
        private final NodeEngineImpl serverNodeEngine;
        private final Address remoteAddress;
        private final Address localAddress;
        private final Connection serverSideConnection;

        public MockedClientConnection(HazelcastClientInstanceImpl client, int connectionId, NodeEngineImpl serverNodeEngine,
                                      Address address, Address localAddress) throws IOException {
            super(client, connectionId);
            this.serverNodeEngine = serverNodeEngine;
            this.remoteAddress = address;
            this.localAddress = localAddress;
            this.serverSideConnection = new MockedNodeConnection(connectionId, remoteAddress,
                    localAddress, serverNodeEngine, this);
        }

        void handleClientMessage(ClientMessage clientMessage) {
            lastReadTime = System.currentTimeMillis();
            getConnectionManager().handleClientMessage(clientMessage, this);
        }

        @Override
        public boolean write(SocketWritable socketWritable) {
            ClientMessage newPacket = readFromPacket((ClientMessage) socketWritable);
            MemberImpl member = serverNodeEngine.getClusterService().getMember(remoteAddress);
            lastWriteTime = System.currentTimeMillis();
            if (member != null) {
                member.didRead();
            }
            serverNodeEngine.getNode().clientEngine.handleClientMessage(newPacket, serverSideConnection);
            return true;
        }

        private ClientMessage readFromPacket(ClientMessage packet) {
            return ClientMessage.createForDecode(packet.buffer(), 0);
        }

        @Override
        public void init() throws IOException {

        }

        @Override
        public long lastReadTime() {
            return lastReadTime;
        }

        @Override
        public long lastWriteTime() {
            return lastWriteTime;
        }

        @Override
        public InetAddress getInetAddress() {
            try {
                return remoteAddress.getInetAddress();
            } catch (UnknownHostException e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        public InetSocketAddress getRemoteSocketAddress() {
            try {
                return remoteAddress.getInetSocketAddress();
            } catch (UnknownHostException e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        public int getPort() {
            return remoteAddress.getPort();
        }

        @Override
        public InetSocketAddress getLocalSocketAddress() {
            try {
                return localAddress.getInetSocketAddress();
            } catch (UnknownHostException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void innerClose() throws IOException {
            serverSideConnection.close();
        }
    }

    private class MockedNodeConnection extends TestNodeRegistry.MockConnection {

        private final MockedClientConnection responseConnection;
        private final int connectionId;

        public MockedNodeConnection(int connectionId, Address localEndpoint, Address remoteEndpoint, NodeEngineImpl nodeEngine
                , MockedClientConnection responseConnection) {
            super(localEndpoint, remoteEndpoint, nodeEngine);
            this.responseConnection = responseConnection;
            this.connectionId = connectionId;
        }

        @Override
        public boolean write(SocketWritable socketWritable) {
            final ClientMessage packet = (ClientMessage) socketWritable;
            if (nodeEngine.getNode().isActive()) {
                ClientMessage newPacket = readFromPacket(packet);
                MemberImpl member = nodeEngine.getClusterService().getMember(localEndpoint);
                if (member != null) {
                    member.didRead();
                }
                responseConnection.handleClientMessage(newPacket);
                return true;
            }
            return false;
        }

        @Override
        public boolean isClient() {
            return true;
        }

        private ClientMessage readFromPacket(ClientMessage packet) {
            return ClientMessage.createForDecode(packet.buffer(), 0);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            MockedNodeConnection that = (MockedNodeConnection) o;

            if (connectionId != that.connectionId) return false;
            Address remoteEndpoint = getEndPoint();
            return !(remoteEndpoint != null ? !remoteEndpoint.equals(that.getEndPoint()) : that.getEndPoint() != null);

        }

        @Override
        public void close() {
            super.close();
            responseConnection.close();
        }

        @Override
        public int hashCode() {
            int result = connectionId;
            Address remoteEndpoint = getEndPoint();
            result = 31 * result + (remoteEndpoint != null ? remoteEndpoint.hashCode() : 0);
            return result;
        }

        @Override
        public ConnectionType getType() {
            return ConnectionType.JAVA_CLIENT;
        }

        @Override
        public String toString() {
            return "MockedNodeConnection{" +
                    " remoteEndpoint = " + getEndPoint() +
                    ", localEndpoint = " + localEndpoint +
                    ", connectionId = " + connectionId +
                    '}';
        }
    }
}
