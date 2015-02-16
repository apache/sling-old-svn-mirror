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

import java.util.Iterator;
import java.util.List;

import org.apache.sling.query.TestUtils;
import org.apache.sling.query.api.internal.IteratorToIteratorFunction;
import org.apache.sling.query.api.internal.Option;
import org.apache.sling.query.function.IdentityFunction;
import org.apache.sling.query.function.SliceFunction;
import org.apache.sling.query.iterator.ReverseIterator;
import org.junit.Assert;
import org.junit.Test;

import static org.apache.sling.query.TestUtils.l;

public class ReverseTest {

	@Test
	public void testReverse() {
		test(l("a", "b", "c", "d", "e"), l(null, null, null, null, null), new IdentityFunction<String>());
		test(l("a", "b", "c", "d", "e"), l("a", null, null, null, null), new SliceFunction<String>(1));
		test(l("a", "b", "c", "d", "e"), l(null, null, null, "d", "e"), new SliceFunction<String>(0, 2));
	}

	private static <T> void test(List<T> input, List<T> output, IteratorToIteratorFunction<T> function) {
		List<Option<T>> optionInput = TestUtils.optionList(input);
		List<Option<T>> expectedOutput = TestUtils.optionList(output);
		Iterator<Option<T>> actualOutputIterator = new ReverseIterator<T>(function, optionInput.iterator());
		List<Option<T>> actualOutput = TestUtils.iteratorToList(actualOutputIterator);
		Assert.assertEquals(expectedOutput, actualOutput);
	}

}
