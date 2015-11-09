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
package org.apache.sling.commons.testing.util;

import java.io.IOException;

import org.apache.sling.commons.testing.util.JavascriptEngine;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class JavascriptEngineTest {
	private final JavascriptEngine engine = new JavascriptEngine();
	
	@Test
	public void testSimpleScripts() throws IOException {
		
		// Each triplet of data is: json data, code, expected output
		final String [] data = {
			"{}", "out.print('hello')", "hello",	
			"{ i:26, j:'a' }", "out.print(data.i + data.j)", "26a",
			"{ i:'ab', j:'cd' }", "out.print(data.i) ; out.print('+') ; out.print(data.j)", "ab+cd"
		};
		
		for(int i=0; i < data.length; i+= 3) {
			final String json = data[i];
			final String code = data[i+1];
			final String expected = data[i+2];
			final String actual = engine.execute(code, json);
			assertEquals("At index " + i, expected, actual);
		}
	}
}
