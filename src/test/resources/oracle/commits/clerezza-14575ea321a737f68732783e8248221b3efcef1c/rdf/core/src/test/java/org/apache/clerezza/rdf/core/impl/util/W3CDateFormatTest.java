/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.clerezza.rdf.core.impl.util;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author reto
 */
public class W3CDateFormatTest {

    @Test
    public void noMillis() throws Exception {
        Calendar calendar = new GregorianCalendar(2009, 0, 1, 1, 33, 58);
        calendar.setTimeZone(TimeZone.getTimeZone("GMT+7:00"));
        Date date = calendar.getTime();
        Date parsedDate = new W3CDateFormat().parse("2009-01-01T01:33:58+07:00");
        assertEquals(date, parsedDate);
    }

    @Test
    public void noMillisinZ() throws Exception {
        Calendar calendar = new GregorianCalendar(2009, 0, 1, 1, 33, 58);
        calendar.setTimeZone(TimeZone.getTimeZone("GMT"));
        Date date = calendar.getTime();
        Date parsedDate = new W3CDateFormat().parse("2009-01-01T01:33:58Z");
        assertEquals(date, parsedDate);
    }

    @Test
    public void dateObjectSerializedWithoutTimeZone() throws Exception {
        Calendar calendar = new GregorianCalendar(2009, 0, 1, 1, 33, 58);
        calendar.setTimeZone(TimeZone.getTimeZone("GMT+7:00"));
        Date date = calendar.getTime();
        String serializedDate = new W3CDateFormat().format(date);
        assertEquals("2008-12-31T18:33:58Z", serializedDate);
    }

    @Test
    public void roundTrip() throws Exception {
        Calendar calendar = new GregorianCalendar(2009, 0, 1,
                1, 33, 58);
        Date date = calendar.getTime();
        String formattedDate = new W3CDateFormat().format(date);
        Date parsedDate = new W3CDateFormat().parse(formattedDate);
        assertEquals(date, parsedDate);
    }

}