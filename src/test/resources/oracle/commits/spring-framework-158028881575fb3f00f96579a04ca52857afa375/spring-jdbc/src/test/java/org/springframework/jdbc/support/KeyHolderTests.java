/*
 * Copyright 2002-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.jdbc.support;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;

import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.dao.InvalidDataAccessApiUsageException;

/**
 * Tests for the KeyHolder and GeneratedKeyHolder
 * and it appears that JdbcUtils doesn't work exactly as documented.
 *
 * @author trisberg
 * @since Jul 18, 2004
 */
public class KeyHolderTests extends TestCase {
	private KeyHolder kh;

	@Override
	public void setUp() {
		kh = new GeneratedKeyHolder();
	}

	public void testSingleKey(){
		List<Map<String, Object>> l = new LinkedList<Map<String, Object>>();
		Map<String, Object> m = new HashMap<String, Object>(1);
		m.put("key", 1);
		l.add(m);
		kh.getKeyList().addAll(l);
		assertEquals("single key should be returned", 1, kh.getKey().intValue());
	}

	public void testSingleKeyNonNumeric(){
		List<Map<String, Object>> l = new LinkedList<Map<String, Object>>();
		Map<String, Object> m = new HashMap<String, Object>(1);
		m.put("key", "1");
		l.add(m);
		kh.getKeyList().addAll(l);
		try {
			kh.getKey().intValue();
		}
		catch (DataRetrievalFailureException e) {
			assertTrue(e.getMessage().startsWith("The generated key is not of a supported numeric type."));
		}
	}

	public void testNoKeyReturnedInMap(){
		List<Map<String, Object>> l = new LinkedList<Map<String, Object>>();
		Map<String, Object> m = new HashMap<String, Object>();
		l.add(m);
		kh.getKeyList().addAll(l);
		try {
			kh.getKey();
		}
		catch (DataRetrievalFailureException e) {
			assertTrue(e.getMessage().startsWith("Unable to retrieve the generated key."));
		}
	}

	public void testMultipleKeys(){
		List<Map<String, Object>> l = new LinkedList<Map<String, Object>>();
		Map<String, Object> m = new HashMap<String, Object>(2);
		m.put("key", 1);
		m.put("seq", 2);
		l.add(m);
		kh.getKeyList().addAll(l);
		Map<String, Object> keyMap = kh.getKeys();
		assertEquals("two keys should be in the map", 2, keyMap.size());
		try {
			kh.getKey();
		}
		catch (InvalidDataAccessApiUsageException e) {
			assertTrue(e.getMessage().startsWith("The getKey method should only be used when a single key is returned."));
		}
	}

	public void testMultipleKeyRows(){
		List<Map<String, Object>> l = new LinkedList<Map<String, Object>>();
		Map<String, Object> m = new HashMap<String, Object>(2);
		m.put("key", 1);
		m.put("seq", 2);
		l.add(m);
		l.add(m);
		kh.getKeyList().addAll(l);

		assertEquals("two rows should be in the list", 2, kh.getKeyList().size());
		try {
			kh.getKeys();
		}
		catch (InvalidDataAccessApiUsageException e) {
			assertTrue(e.getMessage().startsWith("The getKeys method should only be used when keys for a single row are returned."));
		}
	}
}
