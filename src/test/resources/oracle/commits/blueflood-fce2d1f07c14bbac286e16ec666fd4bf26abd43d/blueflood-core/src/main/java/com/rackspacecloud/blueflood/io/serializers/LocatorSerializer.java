/*
 * Copyright 2015 Rackspace
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.rackspacecloud.blueflood.io.serializers;

import com.rackspacecloud.blueflood.types.Locator;
import com.google.common.base.Charsets;
import com.netflix.astyanax.serializers.AbstractSerializer;
import com.netflix.astyanax.serializers.StringSerializer;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

public class LocatorSerializer extends AbstractSerializer<Locator>{
    private static final LocatorSerializer instance = new LocatorSerializer();
    private static final Charset charset = Charsets.UTF_8;


    public static LocatorSerializer get() {
        return instance;
    }

    @Override
    public ByteBuffer toByteBuffer(Locator locator) {
        return StringSerializer.get().toByteBuffer(locator.toString());
    }

    @Override
    public Locator fromByteBuffer(ByteBuffer byteBuffer) {
        if (byteBuffer == null) {
            return null;
        }
        return Locator.createLocatorFromDbKey(charset.decode(byteBuffer).toString());
    }
}
