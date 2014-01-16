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

package org.apache.sling.replication.transport.impl;


import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Map;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;


@RunWith(Parameterized.class)
public class HttpTransportHandlerCustomHeadersTest {

    private final String[] inputTransportProperties;
    private final String inputSelector;
    private final String[] inputPaths;

    private final String[] outputHeaders;


    @Parameterized.Parameters
    public static Iterable<Object[]> data() {
        return Arrays.asList(new Object[][]{
                { new String[]{}, "", new String[] {},
                        new String[]{}},
                { new String[]{}, "add", new String[] {},
                        new String[]{}},
                { new String[]{"add -> Header: Add" }, "add", new String[] {},
                        new String[]{ "Header: Add" }},
                { new String[]{"add -> Header: Add", "Header: Always" }, "add", new String[] {},
                        new String[]{ "Header: Add", "Header: Always" }},
                { new String[]{"add -> Header: Add", "* -> Header: Always", "delete -> Header:Del" }, "add", new String[] {},
                        new String[]{"Header: Add", "Header: Always" }},
                { new String[]{"add -> Header: Add", "Header: Always" }, "delete", new String[] {},
                        new String[]{"Header: Always" }},
                { new String[]{"add -> Header: Add", "Header: Always" }, "add", new String[] {},
                        new String[] {"Header: Add", "Header: Always" }},
                { new String[]{"add -> Header: Add", "Header: Always", "PathHeader: {path}" }, "add", new String[] { "/content"},
                        new String[]{"Header: Add", "Header: Always", "PathHeader: /content"}},
        });

    }

    public HttpTransportHandlerCustomHeadersTest(String[] inputTransportProperties, String inputSelector, String[] inputPaths,
                                                 String[] outputHeaders){
        this.inputTransportProperties = inputTransportProperties;
        this.inputSelector = inputSelector;
        this.outputHeaders = outputHeaders;
        this.inputPaths = inputPaths;
    }

    @Test
    public void testHttpTransportProperties () {
        String[] headers = HttpTransportHandler.getCustomizedHeaders (inputTransportProperties, inputSelector, inputPaths);

        assertArrayEquals(outputHeaders, headers);
    }
}
