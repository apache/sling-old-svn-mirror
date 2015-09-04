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

package org.apache.sling.query;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.query.api.internal.Option;
import org.apache.sling.query.mock.json.JsonToResource;
import org.junit.Assert;

public final class TestUtils {
	private TestUtils() {
	}

	public static Resource getTree() {
		InputStream jsonStream = TestUtils.class.getClassLoader().getResourceAsStream("sample_tree.json");
		try {
			Resource resource = JsonToResource.parse(jsonStream);
			jsonStream.close();
			return resource;
		} catch (IOException e) {
			return null;
		}
	}

	public static <T> List<T> iteratorToList(Iterator<T> iterator) {
		List<T> list = new ArrayList<T>();
		while (iterator.hasNext()) {
			list.add(iterator.next());
		}
		return list;
	}

	public static <T> List<Option<T>> optionList(List<T> list) {
		List<Option<T>> result = new ArrayList<Option<T>>();
		int index = 0;
		for (T element : list) {
			result.add(Option.of(element, index++));
		}
		return result;
	}

	public static void assertEmptyIterator(Iterator<?> iterator) {
		if (iterator.hasNext()) {
			Assert.fail(String.format("Iterator should be empty, but %s is returned", iterator.next()
					.toString()));
		}
	}

	public static void assertResourceSetEquals(Iterator<Resource> iterator, String... names) {
		Set<String> expectedSet = new LinkedHashSet<String>(Arrays.asList(names));
		Set<String> actualSet = new LinkedHashSet<String>(getResourceNames(iterator));
		Assert.assertEquals(expectedSet, actualSet);
	}

	public static void assertResourceListEquals(Iterator<Resource> iterator, String... names) {
		Assert.assertEquals(Arrays.asList(names), getResourceNames(iterator));
	}

	public static List<String> l(String... args) {
		return Arrays.asList(args);
	}

	private static List<String> getResourceNames(Iterator<Resource> iterator) {
		List<String> resourceNames = new ArrayList<String>();
		while (iterator.hasNext()) {
			resourceNames.add(iterator.next().getName());
		}
		return resourceNames;
	}
}
