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
package org.apache.sling.samples.urlfilter.impl;

import static org.junit.Assert.*;

import java.util.HashMap;

import org.apache.sling.api.request.RequestPathInfo;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JMock;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(JMock.class)
public class UrlFilterTest {

    private Mockery context = new JUnit4Mockery();
    private ValueMap properties;

    @Before
    public void setup() {
        properties = new ValueMapDecorator(new HashMap<String, Object>());
    }

    @Test
    public void null_selector() {
        UrlFilter filter = new UrlFilter();

        final RequestPathInfo testInfo = context.mock(RequestPathInfo.class);
        this.context.checking(new Expectations() {
            {
                allowing(testInfo).getSelectorString();
                will(returnValue(null));
            }
        });

        assertTrue(filter.checkSelector(testInfo, null));
    }

    @Test
    public void non_null_selector() {
        UrlFilter filter = new UrlFilter();

        final RequestPathInfo testInfo = context.mock(RequestPathInfo.class);
        this.context.checking(new Expectations() {
            {
                allowing(testInfo).getSelectorString();
                will(returnValue("sample"));
            }
        });

        // null allowedSelectors = ok
        assertTrue(filter.checkSelector(testInfo, properties));

        // empty array allowedSelectors = fail
        properties.put(UrlFilter.PN_ALLOWED_SELECTORS, (Object) new String[0]);
        assertFalse(filter.checkSelector(testInfo, properties));

        // selector string in array = ok
        properties.put(UrlFilter.PN_ALLOWED_SELECTORS, (Object) new String[] { "sample", "sample2" });
        assertTrue(filter.checkSelector(testInfo, properties));

        // selector string not in array = fail
        properties.put(UrlFilter.PN_ALLOWED_SELECTORS, (Object) new String[] { "other" });
        assertFalse(filter.checkSelector(testInfo, properties));
        
        properties.clear();
        
        // matches regex
        properties.put(UrlFilter.PN_ALLOWED_SELECTOR_PATTERN, "^s[a-z]m.*$");
        assertTrue(filter.checkSelector(testInfo, properties));

        // doesn't match regex
        properties.put(UrlFilter.PN_ALLOWED_SELECTOR_PATTERN, "^s[1-2]m$");
        assertFalse(filter.checkSelector(testInfo, properties));
        
        properties.clear();
        
        // matches array or regex = ok
        properties.put(UrlFilter.PN_ALLOWED_SELECTORS, (Object) new String[] { "other" });
        properties.put(UrlFilter.PN_ALLOWED_SELECTOR_PATTERN, "^s[a-z]m.*$");
        assertTrue(filter.checkSelector(testInfo, properties));
        
        properties.put(UrlFilter.PN_ALLOWED_SELECTORS, (Object) new String[] { "sample" });
        properties.put(UrlFilter.PN_ALLOWED_SELECTOR_PATTERN, "^s[a-z]m$");
        assertTrue(filter.checkSelector(testInfo, properties));

        
    }
}
