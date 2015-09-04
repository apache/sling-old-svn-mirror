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
package org.apache.sling.bgservlets;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.Mockito;

@RunWith(Parameterized.class)
public class QueryStringFilterTest {

    private final String slingBgParamName;
    
    @Parameters(name="{0}")
    public static List<Object[]> data() {
        final List<Object[]> result = new ArrayList<Object[]>();
        result.add(new Object[] { "sling:bg" });
        result.add(new Object[] { "sling%3Abg" });
        return result;
    }
    
    public QueryStringFilterTest(String slingBgParamName) {
        this.slingBgParamName = slingBgParamName;
    }
    
    private void assertFilter(String [] toRemove, String orig, String expected) {
        final HttpServletRequest mock = Mockito.mock(HttpServletRequest.class);
        Mockito.when(mock.getQueryString()).thenReturn(orig);
        final BackgroundHttpServletRequest r = new BackgroundHttpServletRequest(mock, toRemove);
        final String result = r.getQueryString();
        assertEquals("Expecting correct queryString after removal of " + Arrays.asList(toRemove), expected, result);

    }
    
    @Test 
    public void testNothingToRemove() {
        final String [] toRemove =  {};
        assertFilter(toRemove, null, null);
        assertFilter(toRemove, slingBgParamName + "=true", slingBgParamName + "=true&");
        assertFilter(toRemove, "a=b&" + slingBgParamName + "=true", "a=b&" + slingBgParamName + "=true&");
        assertFilter(toRemove, "sling:bg=true&c=d", "sling:bg=true&c=d&");
        assertFilter(toRemove, "a=b&sling:bg=true&c=d", "a=b&sling:bg=true&c=d&");
    }
    
    @Test 
    public void testRemoveOne() {
        final String [] toRemove =  { "sling:bg" };
        assertFilter(toRemove, null, null);
        assertFilter(toRemove, slingBgParamName + "=true", "");
        assertFilter(toRemove, "a=b&" + slingBgParamName + "=true", "a=b&");
        assertFilter(toRemove, slingBgParamName + "=true&c=d", "c=d&");
        assertFilter(toRemove, "a=b&" + slingBgParamName + "=true&c=d", "a=b&c=d&");
        assertFilter(toRemove, "a=b&" + slingBgParamName + " = true+with+spaces&c=d", "a=b&c=d&");
    }
    
    @Test 
    public void testRemoveTwo() {
        final String [] toRemove =  { "sling:bg", "some_other_param" };
        assertFilter(toRemove, null, null);
        assertFilter(toRemove, slingBgParamName + "=true", "");
        assertFilter(toRemove, "a=b&" + slingBgParamName + "=true", "a=b&");
        assertFilter(toRemove, slingBgParamName + "=true&c=d", "c=d&");
        assertFilter(toRemove, "a=b&" + slingBgParamName + "=true&c=d", "a=b&c=d&");
        assertFilter(toRemove, "a=b&" + slingBgParamName + " = true+with+spaces&c=d", "a=b&c=d&");
        assertFilter(toRemove, "a=b&" + slingBgParamName + " = true+with+spaces&c=d&some_other_param=foo", "a=b&c=d&");
        assertFilter(toRemove, slingBgParamName + "=true&some_other_param=foo", "");
        assertFilter(toRemove, slingBgParamName + "=true&some_other_param=foo&", "");
    }
}
