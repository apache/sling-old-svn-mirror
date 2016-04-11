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

package org.apache.sling.distribution.packaging.impl;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.junit.Test;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class DistributionPackageUtilsTest {


    @Test
    public void testInfoEmptyStreams(){
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        Map<String, Object> info = new HashMap<String, Object>();
        DistributionPackageUtils.writeInfo(outputStream, info);

        InputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());

        Map<String, Object> resultInfo = new HashMap<String, Object>();
        DistributionPackageUtils.readInfo(inputStream, resultInfo);

        assertEquals(info.size(), resultInfo.size());

    }


    @Test
    public void testInfoFullStreams(){
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        Map<String, Object> info = new HashMap<String, Object>();
        info.put("test1", "value1");
        info.put("test2", "value2");
        info.put("test3", new String[] { "value1", "value2" });

        DistributionPackageUtils.writeInfo(outputStream, info);

        InputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());

        Map<String, Object> resultInfo = new HashMap<String, Object>();
        DistributionPackageUtils.readInfo(inputStream, resultInfo);

        assertEquals(info.size(), resultInfo.size());
        assertEquals("value1", resultInfo.get("test1"));
        assertEquals("value2", resultInfo.get("test2"));
        String[] array = (String[]) resultInfo.get("test3");

        assertEquals("value1", array[0]);
        assertEquals("value2", array[1]);
    }


    @Test
    public void testStreamsWithoutInfo() throws IOException {

        byte[] bytes =new byte[100];
        for (int i=0; i< bytes.length; i++) {
            bytes[i] = (byte) i;
        }

        InputStream inputStream = new BufferedInputStream(new ByteArrayInputStream(bytes));

        Map<String, Object> resultInfo = new HashMap<String, Object>();
        DistributionPackageUtils.readInfo(inputStream, resultInfo);

        assertEquals(0, resultInfo.size());

        byte[] resultBytes = new byte[100];

        inputStream.read(resultBytes, 0, 100);

        assertEquals(-1, inputStream.read());

        for (int i=0; i < bytes.length; i++) {
            assertEquals((byte)i, resultBytes[i]);
        }

    }
}
