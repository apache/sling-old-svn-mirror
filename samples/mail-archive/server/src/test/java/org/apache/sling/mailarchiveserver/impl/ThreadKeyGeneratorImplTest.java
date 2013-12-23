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
package org.apache.sling.mailarchiveserver.impl;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class ThreadKeyGeneratorImplTest {
	private static final String UNADDRESSABLE_SUBJECT_KEY = "a/at/unaddressable_subject";
    private ThreadKeyGeneratorImpl generator = new ThreadKeyGeneratorImpl();
	private final String input;
	private final String expected;

	@Parameters(name="{0}")
	public static List<Object[]> data() {
		final List<Object[]> result = new ArrayList<Object[]>();
		
		result.add(new Object[] {"'''''''9>*'''''''''''''''''''''''''''40>*", "9/90/940"} ); 
		result.add(new Object[] {"'abc'''9>*'''''''''''''''''''''''''''40>*", "9/90/abc940"} ); 
		result.add(new Object[] {"abcdefg9>*'''''''''''''''''''''''''''40>*", "9/90/abcdefg940"} ); 
		result.add(new Object[] {"abcdefg9>h'''''''''''''''''''''''''''40>*", "h/h0/abcdefg9h40"} ); 
		result.add(new Object[] {"abcdefg9>hi''''''''''''''''''''''''''40>*", "h/h0/abcdefg9hi40"} ); 
		result.add(new Object[] {"abcdefg9>hijklmnopqrstuvwxyzabcdefghi40>*", "h/h0/abcdefg9hijklmnopqrstuvwxyzabcdefghi40"} ); 
		result.add(new Object[] {"abcdefg9>hijklmnopqrstuvwxyzabcdefghi40>j", "h/hj/abcdefg9hijklmnopqrstuvwxyzabcdefghi40j"} ); 
		result.add(new Object[] {"abcdefg9>hijklmnopqrstuvwxyzabcdefghi40>jk","h/hj/abcdefg9hijklmnopqrstuvwxyzabcdefghi40jk"} ); 
		result.add(new Object[] {"'''''''9>'''''''abc''''''''''''''''''40>*", "9/90/9abc40"} ); 
		result.add(new Object[] {"'''''''9>*'''''''''''''''''''''''''''40>*abc'", "9/90/940abc"} ); 
		result.add(new Object[] {"", UNADDRESSABLE_SUBJECT_KEY} ); 
		result.add(new Object[] {"Re: ", UNADDRESSABLE_SUBJECT_KEY} ); 
		result.add(new Object[] {null, UNADDRESSABLE_SUBJECT_KEY} ); 
		result.add(new Object[] {"*", UNADDRESSABLE_SUBJECT_KEY} ); 
		result.add(new Object[] {"1.5.0", "0/00/1_5_0"} ); 
		result.add(new Object[] {"\"\u628A\u63E1\u6B63\u786E\u65B9\u5411,\u505A\u4E2A\u6548\u7387\u4E3A\u5148\u7684\u9886\u5BFC\u52A9\u624B\"", UNADDRESSABLE_SUBJECT_KEY} ); 
		result.add(new Object[] {"remove   consecutive - . - whitespaces", "c/cs/remove_consecutive_whitespaces"} ); 

		return result;
	}

	public ThreadKeyGeneratorImplTest(String input, String expected) {
		this.input = input;
		this.expected = expected;
	}

	@Test
	public void testGetThreadKey() {
		assertEquals(expected, generator.getThreadKey(input));
	}
}