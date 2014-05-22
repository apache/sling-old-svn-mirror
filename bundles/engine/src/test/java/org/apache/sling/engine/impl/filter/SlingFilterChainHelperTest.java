/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.sling.engine.impl.filter;

import static org.junit.Assert.assertEquals;

import javax.servlet.Filter;

import org.jmock.Mockery;
import org.jmock.integration.junit4.JMock;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Basic tests for the filter chain.
 */
@RunWith(JMock.class)
public class SlingFilterChainHelperTest {

    private final Mockery context = new JUnit4Mockery();
    @Test public void testOrdering() {
        final SlingFilterChainHelper chain = new SlingFilterChainHelper();

        chain.addFilter(context.mock(Filter.class, "A"), 1L, 100, "1:100");
        chain.addFilter(context.mock(Filter.class, "B"), 2L, 100, "2:100");
        chain.addFilter(context.mock(Filter.class, "C"), 3L, -100, "3:-100");
        chain.addFilter(context.mock(Filter.class, "D"), 4L, -1000, "4:-1000");
        chain.addFilter(context.mock(Filter.class, "E"), 5L, 1000, "5:1000");

        final SlingFilterChainHelper.FilterListEntry[] entries = chain.getFilterListEntries();
        assertEquals(5, entries.length);
        assertEquals("5:1000", entries[0].getOrderSource());
        assertEquals("1:100", entries[1].getOrderSource());
        assertEquals("2:100", entries[2].getOrderSource());
        assertEquals("3:-100", entries[3].getOrderSource());
        assertEquals("4:-1000", entries[4].getOrderSource());
    }
}
