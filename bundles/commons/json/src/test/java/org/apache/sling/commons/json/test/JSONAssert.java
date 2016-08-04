/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.commons.json.test;

import java.util.Iterator;

import org.apache.sling.commons.json.JSONArray;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.junit.Assert;

/**
 * Provides assertions on equality for JSON Arrays and Objects.
 *
 * Based on code written by Andres Almiray <aalmiray@users.sourceforge.net> as
 * part of the json-lib project - http://json-lib.sourceforge.net/
 */
public class JSONAssert extends Assert {
	/**
	 * Asserts that two JSONArrays are equal.
	 */
	public static void assertEquals(JSONArray expected, JSONArray actual)
			throws JSONException {
		assertEquals(null, expected, actual);
	}

	/**
	 * Asserts that two JSONObjects are equal.
	 */
	public static void assertEquals(JSONObject expected, JSONObject actual)
			throws JSONException {
		assertEquals(null, expected, actual);
	}

	/**
	 * Asserts that two JSONArrays are equal.
	 */
	public static void assertEquals(String message, JSONArray expected,
			JSONArray actual) throws JSONException {
		String header = message == null ? "" : message + ": ";
		if (expected == null) {
			fail(header + "expected array was null");
		}
		if (actual == null) {
			fail(header + "actual array was null");
		}
		if (expected == actual || expected.equals(actual)) {
			return;
		}
		if (actual.length() != expected.length()) {
			fail(header + "arrays sizes differed, expected.length()="
					+ expected.length() + " actual.length()=" + actual.length());
		}

		int max = expected.length();
		for (int i = 0; i < max; i++) {
			Object o1 = expected.get(i);
			Object o2 = actual.get(i);

			// handle nulls
			if (JSONObject.NULL.equals(o1)) {
				if (JSONObject.NULL.equals(o2)) {
					continue;
                }
				fail(header + "arrays first differed at element [" + i
						+ "];");
			} else {
				if (JSONObject.NULL.equals(o2)) {
					fail(header + "arrays first differed at element [" + i
							+ "];");
				}
			}

			if (o1 instanceof JSONArray && o2 instanceof JSONArray) {
				JSONArray e = (JSONArray) o1;
				JSONArray a = (JSONArray) o2;
				assertEquals(header + "arrays first differed at element " + i
						+ ";", e, a);
			} else if (o1 instanceof JSONObject && o2 instanceof JSONObject) {
				assertEquals(header + "arrays first differed at element [" + i
						+ "];", (JSONObject) o1, (JSONObject) o2);
			} else if (o1 instanceof String) {
				assertEquals(header + "arrays first differed at element [" + i
						+ "];", (String) o1, String.valueOf(o2));
			} else if (o2 instanceof String) {
				assertEquals(header + "arrays first differed at element [" + i
						+ "];", String.valueOf(o1), (String) o2);
			} else {
				assertEquals(header + "arrays first differed at element [" + i
						+ "];", o1, o2);
			}
		}
	}

	/**
	 * Asserts that two JSONObjects are equal.
	 */
	public static void assertEquals(String message, JSONObject expected,
			JSONObject actual) throws JSONException {
		String header = message == null ? "" : message + ": ";
		if (expected == null) {
			fail(header + "expected object was null");
		}
		if (actual == null) {
			fail(header + "actual object was null");
		}
		if (expected == actual /* || expected.equals( actual ) */) {
			return;
		}

		JSONArray expectedNames = expected.names();
		JSONArray actualNames = actual.names();

		if (expectedNames == null && actualNames == null) {
			return;
		}

		if (expectedNames == null) {
		    expectedNames = new JSONArray();
		}

		if (actualNames == null) {
		    actualNames = new JSONArray();
		}

		assertEquals(header
				+ "names sizes differed, expected.names().length()="
				+ expectedNames.length() + " actual.names().length()="
				+ actualNames.length(), expectedNames.length(), actualNames
				.length());
		for (Iterator<String> keys = expected.keys(); keys.hasNext();) {
			String key = keys.next();
			Object o1 = expected.opt(key);
			Object o2 = actual.opt(key);

			if (JSONObject.NULL.equals(o1)) {
				if (JSONObject.NULL.equals(o2)) {
					continue;
                }
				fail(header + "objects differed at key [" + key + "];");
			} else {
				if (JSONObject.NULL.equals(o2)) {
					fail(header + "objects differed at key [" + key + "];");
				}
			}

			if (o1 instanceof JSONObject && o2 instanceof JSONObject) {
				assertEquals(header + "objects differed at key [" + key + "];",
						(JSONObject) o1, (JSONObject) o2);
			} else if (o1 instanceof JSONArray && o2 instanceof JSONArray) {
				assertEquals(header + "objects differed at key [" + key + "];",
						(JSONArray) o1, (JSONArray) o2);
			} else if (o1 instanceof String) {
				assertEquals(header + "objects differed at key [" + key + "];",
						(String) o1, String.valueOf(o2));
			} else if (o2 instanceof String) {
				assertEquals(header + "objects differed at key [" + key + "];",
						String.valueOf(o1), (String) o2);
			} else {
				assertEquals(header + "objects differed at key [" + key + "];",
						o1, o2);
			}
		}
	}

}
