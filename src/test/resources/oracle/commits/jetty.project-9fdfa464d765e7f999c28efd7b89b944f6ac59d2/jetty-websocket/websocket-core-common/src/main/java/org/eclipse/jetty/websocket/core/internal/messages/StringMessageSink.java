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

package org.eclipse.jetty.websocket.core.internal.messages;

import java.lang.invoke.MethodHandle;

import org.eclipse.jetty.util.Callback;
import org.eclipse.jetty.util.Utf8StringBuilder;
import org.eclipse.jetty.websocket.core.CoreSession;
import org.eclipse.jetty.websocket.core.Frame;
import org.eclipse.jetty.websocket.core.exception.MessageTooLargeException;

public class StringMessageSink extends AbstractMessageSink
{
    private Utf8StringBuilder out;
    private int size;

    public StringMessageSink(CoreSession session, MethodHandle methodHandle)
    {
        super(session, methodHandle);
        this.size = 0;
    }

    @Override
    public void accept(Frame frame, Callback callback)
    {
        try
        {
            size += frame.getPayloadLength();
            long maxTextMessageSize = session.getMaxTextMessageSize();
            if (maxTextMessageSize > 0 && size > maxTextMessageSize)
            {
                throw new MessageTooLargeException(String.format("Text message too large: (actual) %,d > (configured max text message size) %,d",
                    size, maxTextMessageSize));
            }

            if (out == null)
                out = new Utf8StringBuilder(session.getInputBufferSize());
            out.append(frame.getPayload());

            if (frame.isFin())
            {
                methodHandle.invoke(out.toString());
                reset();
            }

            callback.succeeded();
            session.demand(1);
        }
        catch (Throwable t)
        {
            reset();
            callback.failed(t);
        }
    }

    @Override
    public void fail(Throwable failure)
    {
        reset();
    }

    private void reset()
    {
        out = null;
        size = 0;
    }
}
