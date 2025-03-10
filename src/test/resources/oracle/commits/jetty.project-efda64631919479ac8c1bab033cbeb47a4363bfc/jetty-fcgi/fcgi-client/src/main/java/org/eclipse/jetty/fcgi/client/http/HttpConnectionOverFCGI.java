//
// ========================================================================
// Copyright (c) 1995 Mort Bay Consulting Pty Ltd and others.
//
// This program and the accompanying materials are made available under the
// terms of the Eclipse Public License v. 2.0 which is available at
// https://www.eclipse.org/legal/epl-2.0, or the Apache License, Version 2.0
// which is available at https://www.apache.org/licenses/LICENSE-2.0.
//
// SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
// ========================================================================
//

package org.eclipse.jetty.fcgi.client.http;

import java.io.EOFException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousCloseException;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.jetty.client.HttpChannel;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.HttpConnection;
import org.eclipse.jetty.client.HttpDestination;
import org.eclipse.jetty.client.HttpExchange;
import org.eclipse.jetty.client.HttpRequest;
import org.eclipse.jetty.client.IConnection;
import org.eclipse.jetty.client.SendFailure;
import org.eclipse.jetty.client.api.Connection;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.api.Response;
import org.eclipse.jetty.fcgi.FCGI;
import org.eclipse.jetty.fcgi.generator.Flusher;
import org.eclipse.jetty.fcgi.parser.ClientParser;
import org.eclipse.jetty.http.HttpField;
import org.eclipse.jetty.http.HttpFields;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.http.HttpHeaderValue;
import org.eclipse.jetty.io.AbstractConnection;
import org.eclipse.jetty.io.EndPoint;
import org.eclipse.jetty.io.RetainableByteBuffer;
import org.eclipse.jetty.io.RetainableByteBufferPool;
import org.eclipse.jetty.util.Attachable;
import org.eclipse.jetty.util.BufferUtil;
import org.eclipse.jetty.util.Callback;
import org.eclipse.jetty.util.Promise;
import org.eclipse.jetty.util.thread.AutoLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpConnectionOverFCGI extends AbstractConnection implements IConnection, Attachable
{
    private static final Logger LOG = LoggerFactory.getLogger(HttpConnectionOverFCGI.class);

    private final RetainableByteBufferPool networkByteBufferPool;
    private final AutoLock lock = new AutoLock();
    private final LinkedList<Integer> requests = new LinkedList<>();
    private final AtomicBoolean closed = new AtomicBoolean();
    private final HttpDestination destination;
    private final Promise<Connection> promise;
    private final Flusher flusher;
    private final Delegate delegate;
    private final ClientParser parser;
    private HttpChannelOverFCGI channel;
    private RetainableByteBuffer networkBuffer;
    private Object attachment;

    public HttpConnectionOverFCGI(EndPoint endPoint, HttpDestination destination, Promise<Connection> promise)
    {
        super(endPoint, destination.getHttpClient().getExecutor());
        this.destination = destination;
        this.promise = promise;
        this.flusher = new Flusher(endPoint);
        this.delegate = new Delegate(destination);
        this.parser = new ClientParser(new ResponseListener());
        requests.addLast(0);
        HttpClient client = destination.getHttpClient();
        this.networkByteBufferPool = client.getByteBufferPool().asRetainableByteBufferPool();
    }

    public HttpDestination getHttpDestination()
    {
        return destination;
    }

    @Override
    public SocketAddress getLocalSocketAddress()
    {
        return delegate.getLocalSocketAddress();
    }

    @Override
    public SocketAddress getRemoteSocketAddress()
    {
        return delegate.getRemoteSocketAddress();
    }

    protected Flusher getFlusher()
    {
        return flusher;
    }

    @Override
    public void send(Request request, Response.CompleteListener listener)
    {
        delegate.send(request, listener);
    }

    @Override
    public SendFailure send(HttpExchange exchange)
    {
        return delegate.send(exchange);
    }

    @Override
    public void onOpen()
    {
        super.onOpen();
        fillInterested();
        promise.succeeded(this);
    }

    @Override
    public void onFillable()
    {
        networkBuffer = newNetworkBuffer();
        process();
    }

    private void reacquireNetworkBuffer()
    {
        if (networkBuffer == null)
            throw new IllegalStateException();
        if (networkBuffer.hasRemaining())
            throw new IllegalStateException();
        networkBuffer.release();
        networkBuffer = newNetworkBuffer();
        if (LOG.isDebugEnabled())
            LOG.debug("Reacquired {}", networkBuffer);
    }

    private RetainableByteBuffer newNetworkBuffer()
    {
        HttpClient client = destination.getHttpClient();
        return networkByteBufferPool.acquire(client.getResponseBufferSize(), client.isUseInputDirectByteBuffers());
    }

    private void releaseNetworkBuffer()
    {
        if (networkBuffer == null)
            throw new IllegalStateException();
        if (networkBuffer.hasRemaining())
            throw new IllegalStateException();
        networkBuffer.release();
        if (LOG.isDebugEnabled())
            LOG.debug("Released {}", networkBuffer);
        this.networkBuffer = null;
    }

    void process()
    {
        EndPoint endPoint = getEndPoint();
        try
        {
            while (true)
            {
                if (parse(networkBuffer.getBuffer()))
                    return;

                if (networkBuffer.isRetained())
                    reacquireNetworkBuffer();

                // The networkBuffer may have been reacquired.
                int read = endPoint.fill(networkBuffer.getBuffer());
                if (LOG.isDebugEnabled())
                    LOG.debug("Read {} bytes from {}", read, endPoint);

                if (read == 0)
                {
                    releaseNetworkBuffer();
                    fillInterested();
                    return;
                }
                else if (read < 0)
                {
                    releaseNetworkBuffer();
                    shutdown();
                    return;
                }
            }
        }
        catch (Exception x)
        {
            if (LOG.isDebugEnabled())
                LOG.debug("Unable to fill from endpoint {}", endPoint, x);
            networkBuffer.clear();
            releaseNetworkBuffer();
            close(x);
        }
    }

    private boolean parse(ByteBuffer buffer)
    {
        return parser.parse(buffer);
    }

    private void shutdown()
    {
        // Close explicitly only if we are idle, since the request may still
        // be in progress, otherwise close only if we can fail the responses.
        HttpChannelOverFCGI channel = this.channel;
        if (channel == null || channel.getRequest() == 0)
            close();
        else
            failAndClose(new EOFException(String.valueOf(getEndPoint())));
    }

    @Override
    public boolean onIdleExpired()
    {
        long idleTimeout = getEndPoint().getIdleTimeout();
        TimeoutException failure = new TimeoutException("Idle timeout " + idleTimeout + " ms");
        boolean close = delegate.onIdleTimeout(idleTimeout, failure);
        if (close)
            close(failure);
        return false;
    }

    protected void release(HttpChannelOverFCGI channel)
    {
        HttpChannelOverFCGI existing = this.channel;
        if (existing == channel)
        {
            channel.setRequest(0);
            // Recycle only non-failed channels.
            if (channel.isFailed())
            {
                channel.destroy();
                this.channel = null;
            }
            destination.release(this);
        }
        else
        {
            if (existing == null)
                channel.destroy();
            else
                throw new UnsupportedOperationException("FastCGI Multiplex");
        }
    }

    @Override
    public void close()
    {
        close(new AsynchronousCloseException());
    }

    protected void close(Throwable failure)
    {
        if (closed.compareAndSet(false, true))
        {
            getHttpDestination().remove(this);

            abort(failure);

            getEndPoint().shutdownOutput();
            if (LOG.isDebugEnabled())
                LOG.debug("Shutdown {}", this);
            getEndPoint().close();
            if (LOG.isDebugEnabled())
                LOG.debug("Closed {}", this);

            delegate.destroy();
        }
    }

    @Override
    public boolean isClosed()
    {
        return closed.get();
    }

    @Override
    public void setAttachment(Object obj)
    {
        this.attachment = obj;
    }

    @Override
    public Object getAttachment()
    {
        return attachment;
    }

    protected boolean closeByHTTP(HttpFields fields)
    {
        if (!fields.contains(HttpHeader.CONNECTION, HttpHeaderValue.CLOSE.asString()))
            return false;
        close();
        return true;
    }

    protected void abort(Throwable failure)
    {
        HttpChannelOverFCGI channel = this.channel;
        if (channel != null)
        {
            HttpExchange exchange = channel.getHttpExchange();
            if (exchange != null)
                exchange.getRequest().abort(failure);
            channel.destroy();
            this.channel = null;
        }
    }

    private void failAndClose(Throwable failure)
    {
        HttpChannelOverFCGI channel = this.channel;
        if (channel != null)
        {
            boolean result = channel.responseFailure(failure);
            channel.destroy();
            if (result)
                close(failure);
        }
    }

    private int acquireRequest()
    {
        try (AutoLock ignored = lock.lock())
        {
            int last = requests.getLast();
            int request = last + 1;
            requests.addLast(request);
            return request;
        }
    }

    private void releaseRequest(int request)
    {
        try (AutoLock ignored = lock.lock())
        {
            requests.removeFirstOccurrence(request);
        }
    }

    protected HttpChannelOverFCGI acquireHttpChannel(int id, Request request)
    {
        if (channel == null)
            channel = newHttpChannel(request);
        channel.setRequest(id);
        return channel;
    }

    protected HttpChannelOverFCGI newHttpChannel(Request request)
    {
        return new HttpChannelOverFCGI(this, getFlusher(), request.getIdleTimeout());
    }

    @Override
    public String toConnectionString()
    {
        return String.format("%s@%x[l:%s<->r:%s]",
            getClass().getSimpleName(),
            hashCode(),
            getEndPoint().getLocalSocketAddress(),
            getEndPoint().getRemoteSocketAddress());
    }

    private class Delegate extends HttpConnection
    {
        private Delegate(HttpDestination destination)
        {
            super(destination);
        }

        @Override
        protected Iterator<HttpChannel> getHttpChannels()
        {
            HttpChannel channel = HttpConnectionOverFCGI.this.channel;
            return channel == null ? Collections.emptyIterator() : Collections.singleton(channel).iterator();
        }

        @Override
        public SocketAddress getLocalSocketAddress()
        {
            return getEndPoint().getLocalSocketAddress();
        }

        @Override
        public SocketAddress getRemoteSocketAddress()
        {
            return getEndPoint().getRemoteSocketAddress();
        }

        @Override
        public SendFailure send(HttpExchange exchange)
        {
            HttpRequest request = exchange.getRequest();
            normalizeRequest(request);

            int id = acquireRequest();
            HttpChannelOverFCGI channel = acquireHttpChannel(id, request);

            return send(channel, exchange);
        }

        @Override
        public void close()
        {
            HttpConnectionOverFCGI.this.close();
        }

        protected void close(Throwable failure)
        {
            HttpConnectionOverFCGI.this.close(failure);
        }

        @Override
        public boolean isClosed()
        {
            return HttpConnectionOverFCGI.this.isClosed();
        }

        @Override
        public String toString()
        {
            return HttpConnectionOverFCGI.this.toString();
        }
    }

    private class ResponseListener implements ClientParser.Listener
    {
        @Override
        public void onBegin(int request, int code, String reason)
        {
            HttpChannelOverFCGI channel = HttpConnectionOverFCGI.this.channel;
            if (channel != null)
                channel.responseBegin(code, reason);
            else
                noChannel(request);
        }

        @Override
        public void onHeader(int request, HttpField field)
        {
            HttpChannelOverFCGI channel = HttpConnectionOverFCGI.this.channel;
            if (channel != null)
                channel.responseHeader(field);
            else
                noChannel(request);
        }

        @Override
        public boolean onHeaders(int request)
        {
            HttpChannelOverFCGI channel = HttpConnectionOverFCGI.this.channel;
            if (channel != null)
                return !channel.responseHeaders();
            noChannel(request);
            return false;
        }

        @Override
        public boolean onContent(int request, FCGI.StreamType stream, ByteBuffer buffer)
        {
            switch (stream)
            {
                case STD_OUT:
                {
                    HttpChannelOverFCGI channel = HttpConnectionOverFCGI.this.channel;
                    if (channel != null)
                    {
                        networkBuffer.retain();
                        return !channel.content(buffer, Callback.from(networkBuffer::release, HttpConnectionOverFCGI.this::close));
                    }
                    else
                    {
                        noChannel(request);
                    }
                    break;
                }
                case STD_ERR:
                {
                    LOG.info(BufferUtil.toUTF8String(buffer));
                    break;
                }
                default:
                {
                    throw new IllegalArgumentException();
                }
            }
            return false;
        }

        @Override
        public void onEnd(int request)
        {
            HttpChannelOverFCGI channel = HttpConnectionOverFCGI.this.channel;
            if (channel != null)
            {
                if (channel.responseSuccess())
                    releaseRequest(request);
            }
            else
            {
                noChannel(request);
            }
        }

        @Override
        public void onFailure(int request, Throwable failure)
        {
            HttpChannelOverFCGI channel = HttpConnectionOverFCGI.this.channel;
            if (channel != null)
            {
                if (channel.responseFailure(failure))
                    releaseRequest(request);
            }
            else
            {
                noChannel(request);
            }
        }

        private void noChannel(int request)
        {
            if (LOG.isDebugEnabled())
                LOG.debug("Channel not found for request {}", request);
        }
    }
}
