/*-
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

package org.apache.sling.query.selector.parser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.sling.query.selector.parser.Attribute;
import org.apache.sling.query.selector.parser.Modifier;
import org.apache.sling.query.selector.parser.SelectorParser;
import org.apache.sling.query.selector.parser.SelectorSegment;
import org.junit.Assert;
import org.junit.Test;

public class SelectorTest {
	@Test
	public void parseResourceType() {
		SelectorSegment selector = getFirstSegment("my/resource/type");
		Assert.assertEquals(selector.getType(), "my/resource/type");
	}

	@Test
	public void parseProperty() {
		SelectorSegment selector = getFirstSegment("[key=value]");
		Assert.assertEquals(Arrays.asList(pp("key", "value")), selector.getAttributes());
	}

	@Test
	public void parseProperties() {
		SelectorSegment selector = getFirstSegment("[key=value][key2=value2]");
		Assert.assertEquals(Arrays.asList(pp("key", "value"), pp("key2", "value2")), selector.getAttributes());
	}

	@Test
	public void parseResourceTypeAndName() {
		SelectorSegment selector = getFirstSegment("my/resource/type#some-name");
		Assert.assertEquals("my/resource/type", selector.getType());
		Assert.assertEquals("some-name", selector.getName());
	}

	@Test
	public void parseResourceTypeAndProperty() {
		SelectorSegment selector = getFirstSegment("my/resource/type[key=value]");
		Assert.assertEquals(Arrays.asList(pp("key", "value")), selector.getAttributes());
		Assert.assertEquals("my/resource/type", selector.getType());
	}

	@Test
	public void parseResourceTypeAndNameAndProperty() {
		SelectorSegment selector = getFirstSegment("my/resource/type#some-name[key=value]");
		Assert.assertEquals(Arrays.asList(pp("key", "value")), selector.getAttributes());
		Assert.assertEquals("my/resource/type", selector.getType());
		Assert.assertEquals("some-name", selector.getName());
	}

	@Test
	public void parseResourceTypeAndProperties() {
		SelectorSegment selector = getFirstSegment("my/resource/type[key=value][key2=value2]");
		Assert.assertEquals(Arrays.asList(pp("key", "value"), pp("key2", "value2")), selector.getAttributes());
		Assert.assertEquals("my/resource/type", selector.getType());
	}

	@Test
	public void parseResourceTypeAndNameAndProperties() {
		SelectorSegment selector = getFirstSegment("my/resource/type#some-name[key=value][key2=value2]");
		Assert.assertEquals(Arrays.asList(pp("key", "value"), pp("key2", "value2")), selector.getAttributes());
		Assert.assertEquals("my/resource/type", selector.getType());
		Assert.assertEquals("some-name", selector.getName());
	}

	@Test
	public void parseFunction() {
		SelectorSegment selector = getFirstSegment(":eq(12)");
		Assert.assertEquals(Arrays.asList(f("eq", "12")), selector.getModifiers());
	}

	@Test
	public void parseNameAndFunction() {
		SelectorSegment selector = getFirstSegment("#some-name:eq(12)");
		Assert.assertEquals("some-name", selector.getName());
		Assert.assertEquals(Arrays.asList(f("eq", "12")), selector.getModifiers());
	}

	@Test
	public void parseEscapedNameAndFunction() {
		SelectorSegment selector = getFirstSegment("#'jcr:content':eq(12)");
		Assert.assertEquals("jcr:content", selector.getName());
		Assert.assertEquals(Arrays.asList(f("eq", "12")), selector.getModifiers());
	}

	@Test
	public void parseFunctionWithFilter() {
		SelectorSegment selector = getFirstSegment(":has([key=value])");
		Assert.assertEquals(Arrays.asList(f("has", "[key=value]")), selector.getModifiers());
	}

	@Test
	public void parseNameAndFunctionWithFilter() {
		SelectorSegment selector = getFirstSegment("#some-name:has([key=value])");
		Assert.assertEquals(Arrays.asList(f("has", "[key=value]")), selector.getModifiers());
		Assert.assertEquals("some-name", selector.getName());
	}

	@Test
	public void parseNestedFunction() {
		SelectorSegment selector = getFirstSegment(":not(:has(cq:Page))");
		Assert.assertEquals(Arrays.asList(f("not", ":has(cq:Page)")), selector.getModifiers());
	}

	@Test
	public void parseFunctionWithoutArgument() {
		SelectorSegment selector = getFirstSegment(":first");
		Assert.assertEquals(Arrays.asList(f("first", null)), selector.getModifiers());
	}

	@Test
	public void parseFunctions() {
		SelectorSegment selector = getFirstSegment(":eq(12):first");
		Assert.assertEquals(Arrays.asList(f("eq", "12"), f("first", null)), selector.getModifiers());
	}

	@Test
	public void parsePrimaryTypeAndFunction() {
		SelectorSegment selector = getFirstSegment("cq:Page:first");
		Assert.assertEquals("cq:Page", selector.getType());
		Assert.assertEquals(Arrays.asList(f("first", null)), selector.getModifiers());
	}

	@Test
	public void parsePrimaryTypeAndFunctions() {
		SelectorSegment selector = getFirstSegment("cq:Page:first:eq(12)");
		Assert.assertEquals("cq:Page", selector.getType());
		Assert.assertEquals(Arrays.asList(f("first", null), f("eq", "12")), selector.getModifiers());
	}

	@Test
	public void parseResourceTypeAndFunction() {
		SelectorSegment selector = getFirstSegment("my/resource/type:first");
		Assert.assertEquals("my/resource/type", selector.getType());
		Assert.assertEquals(Arrays.asList(f("first", null)), selector.getModifiers());
	}

	@Test
	public void parseResourceTypeAndNameAndFunction() {
		SelectorSegment selector = getFirstSegment("my/resource/type#some-name:first");
		Assert.assertEquals("my/resource/type", selector.getType());
		Assert.assertEquals(Arrays.asList(f("first", null)), selector.getModifiers());
		Assert.assertEquals("some-name", selector.getName());
	}

	@Test
	public void parseResourceTypeAndFunctions() {
		SelectorSegment selector = getFirstSegment("my/resource/type:first:eq(12)");
		Assert.assertEquals("my/resource/type", selector.getType());
		Assert.assertEquals(Arrays.asList(f("first", null), f("eq", "12")), selector.getModifiers());
	}

	@Test
	public void parseResourceTypeAndPropertyAndFunction() {
		SelectorSegment selector = getFirstSegment("my/resource/type[key=value]:first");
		Assert.assertEquals("my/resource/type", selector.getType());
		Assert.assertEquals(Arrays.asList(pp("key", "value")), selector.getAttributes());
		Assert.assertEquals(Arrays.asList(f("first", null)), selector.getModifiers());
	}

	@Test
	public void parseResourceTypeAndNameAndPropertyAndFunction() {
		SelectorSegment selector = getFirstSegment("my/resource/type#some-name[key=value]:first");
		Assert.assertEquals(selector.getType(), "my/resource/type");
		Assert.assertEquals(Arrays.asList(pp("key", "value")), selector.getAttributes());
		Assert.assertEquals(Arrays.asList(f("first", null)), selector.getModifiers());
		Assert.assertEquals("some-name", selector.getName());
	}

	@Test
	public void parseResourceTypeAndPropertiesAndFunction() {
		SelectorSegment selector = getFirstSegment("my/resource/type[key=value][key2=value2]:first");
		Assert.assertEquals("my/resource/type", selector.getType());
		Assert.assertEquals(Arrays.asList(pp("key", "value"), pp("key2", "value2")), selector.getAttributes());
		Assert.assertEquals(Arrays.asList(f("first", null)), selector.getModifiers());
	}

	@Test
	public void parseResourceTypeAndPropertyAndFunctions() {
		SelectorSegment selector = getFirstSegment("my/resource/type[key=value]:first:eq(12)");
		Assert.assertEquals("my/resource/type", selector.getType());
		Assert.assertEquals(Arrays.asList(pp("key", "value")), selector.getAttributes());
		Assert.assertEquals(Arrays.asList(f("first", null), f("eq", "12")), selector.getModifiers());
	}

	@Test
	public void parseResourceTypeAndPropertiesAndFunctions() {
		SelectorSegment selector = getFirstSegment("my/resource/type[key=value][key2=value2]:first:eq(12)");
		Assert.assertEquals("my/resource/type", selector.getType());
		Assert.assertEquals(Arrays.asList(pp("key", "value"), pp("key2", "value2")), selector.getAttributes());
		Assert.assertEquals(Arrays.asList(f("first", null), f("eq", "12")), selector.getModifiers());
	}

	@Test
	public void parseMultiSegments() {
		List<SelectorSegment> segments = getSegments("cq:Page cq:Page");
		Assert.assertEquals(getSegments("cq:Page", " ", "cq:Page"), segments);

		segments = getSegments("cq:Page > cq:Page");
		Assert.assertEquals(getSegments("cq:Page", ">", "cq:Page"), segments);

		segments = getSegments("cq:Page ~ cq:Page");
		Assert.assertEquals(getSegments("cq:Page", "~", "cq:Page"), segments);

		segments = getSegments("cq:Page + cq:Page");
		Assert.assertEquals(getSegments("cq:Page", "+", "cq:Page"), segments);

		segments = getSegments("cq:Page   cq:Page2 +  cq:Page3");
		Assert.assertEquals(getSegments("cq:Page", " ", "cq:Page2", "+", "cq:Page3"), segments);
	}

	private static Attribute pp(String key, String value) {
		return new Attribute(key, "=", value);
	}

	private static Modifier f(String functionId, String argument) {
		return new Modifier(functionId, argument);
	}

	private static List<SelectorSegment> getSegments(String selector) {
		return SelectorParser.parse(selector).get(0).getSegments();
	}

	private static SelectorSegment getFirstSegment(String selector) {
		return getSegments(selector).get(0);
	}

	private static List<SelectorSegment> getSegments(String... segments) {
		List<SelectorSegment> list = new ArrayList<SelectorSegment>();
		if (segments.length > 0) {
			list.add(getFirstSegment(segments[0]));
		}
		for (int i = 1; i < segments.length; i += 2) {
			SelectorSegment parsed = getFirstSegment(segments[i + 1]);
			char operator = segments[i].charAt(0);
			SelectorSegment segment = new SelectorSegment(parsed.getType(), null, parsed.getAttributes(),
					parsed.getModifiers(), operator);
			list.add(segment);
		}
		return list;
	}
}