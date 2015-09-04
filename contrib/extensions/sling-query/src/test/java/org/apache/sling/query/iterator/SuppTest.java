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

package org.apache.sling.query.iterator;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.apache.sling.query.TestUtils;
import org.apache.sling.query.api.internal.ElementToIteratorFunction;
import org.apache.sling.query.api.internal.IteratorToIteratorFunction;
import org.apache.sling.query.api.internal.Option;
import org.apache.sling.query.function.IdentityFunction;
import org.apache.sling.query.function.IteratorToIteratorFunctionWrapper;
import org.apache.sling.query.function.SliceFunction;
import org.apache.sling.query.iterator.SuppIterator;
import org.junit.Assert;
import org.junit.Test;

import static org.apache.sling.query.TestUtils.l;

public class SuppTest {

	@Test
	public void testIdentity() {
		test(l("a", "b", "c", "d", "e"), l("a", "b", "c", "d", "e"), new IdentityFunction<String>());
	}

	@Test
	public void testNoFirst() {
		test(l("a", "b", "c", "d", "e"), l(null, "b", "c", "d", "e"), new SliceFunction<String>(1));
	}

	@Test
	public void testNoSecond() {
		testExpanding(l("a", "---"), l("a"));
	}

	@Test
	public void testNoTwoFirst() {
		test(l("a", "b", "c", "d", "e"), l(null, null, "c", "d", "e"), new SliceFunction<String>(2));
	}

	@Test
	public void testNoLast() {
		test(l("a", "b", "c", "d", "e"), l("a", "b", "c", "d"), new SliceFunction<String>(0, 3));
	}

	@Test
	public void testNoTwoLast() {
		test(l("a", "b", "c", "d", "e"), l("a", "b", "c"), new SliceFunction<String>(0, 2));
	}

	@Test
	public void testJustFirst() {
		test(l("a", "b", "c", "d", "e"), l("a"), new SliceFunction<String>(0, 0));
	}

	@Test
	public void testExpandFirst() {
		testExpanding(l("+", "b", "c", "d", "e"), l("+", "b", "c", "d", "e"));
	}

	@Test
	public void testExpandMiddle() {
		testExpanding(l("a", "b", "+", "d", "e"), l("a", "b", "+", "d", "e"));
	}

	@Test
	public void testExpandLast() {
		testExpanding(l("a", "b", "c", "d", "+"), l("a", "b", "c", "d", "+"));
	}

	@Test
	public void testRemoveFirst() {
		testExpanding(l("-", "b", "c", "d", "e"), l(null, "b", "c", "d", "e"));
		testExpanding(l("---", "b", "c", "d", "e"), l(null, "b", "c", "d", "e"));
	}

	@Test
	public void testRemoveMiddle() {
		testExpanding(l("a", "b", "-", "d", "e"), l("a", "b", null, "d", "e"));
		testExpanding(l("a", "b", "---", "d", "e"), l("a", "b", null, "d", "e"));
	}

	@Test
	public void testRemoveLast() {
		testExpanding(l("a", "b", "c", "d", "-"), l("a", "b", "c", "d"));
		testExpanding(l("a", "b", "c", "d", "---"), l("a", "b", "c", "d"));
	}

	private static void testExpanding(List<String> input, List<String> output) {
		test(input, output, EXPANDING_FUNCTION);
	}

	private static <T> void test(List<T> input, List<T> output, IteratorToIteratorFunction<T> function) {
		List<Option<T>> optionInput = TestUtils.optionList(input);
		List<Option<T>> expectedOutput = TestUtils.optionList(output);
		Iterator<Option<T>> actualOutputIterator = new SuppIterator<T>(optionInput, function);
		List<Option<T>> actualOutput = TestUtils.iteratorToList(actualOutputIterator);
		Assert.assertEquals(expectedOutput, actualOutput);
	}

	private static final IteratorToIteratorFunctionWrapper<String> EXPANDING_FUNCTION = new IteratorToIteratorFunctionWrapper<String>(
			new ElementToIteratorFunction<String>() {
				@Override
				public Iterator<String> apply(String input) {
					if ("+".equals(input)) {
						return Arrays.asList("a", "b", "c").iterator();
					} else if ("-".equals(input)) {
						return Arrays.<String> asList().iterator();
					} else if ("---".equals(input)) {
						return Arrays.<String> asList(null, null, null).iterator();
					} else {
						return Arrays.asList(input).iterator();
					}
				}
			});
}