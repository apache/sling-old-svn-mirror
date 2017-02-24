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
package org.apache.sling.fsprovider.internal.parser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.File;
import java.util.Map;

import org.junit.experimental.theories.DataPoint;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.runner.RunWith;

@RunWith(Theories.class)
public class ContentFileCacheTest {
    
    @DataPoint
    public static final int NO_CACHE = 0;
    @DataPoint
    public static final int SMALL_CACHE = 1;
    @DataPoint
    public static final int HUGE_CACHE = 1000;

    @Theory
    public void testCache(int cacheSize) {
        ContentFileCache underTest = new ContentFileCache(cacheSize);
        
        Map<String,Object> content1 = underTest.get("/fs-test/folder2/content", new File("src/test/resources/fs-test/folder2/content.json"));
        assertNotNull(content1);
        
        switch (cacheSize) {
        case NO_CACHE:
            assertEquals(0, underTest.size());
            break;
        case SMALL_CACHE:
        case HUGE_CACHE:
            assertEquals(1, underTest.size());
            break;
        }

        Map<String,Object> content2 = underTest.get("/fs-test/folder1/file1a", new File("src/test/resources/fs-test/folder1/file1a.txt"));
        assertNull(content2);

        switch (cacheSize) {
        case NO_CACHE:
            assertEquals(0, underTest.size());
            break;
        case SMALL_CACHE:
            assertEquals(1, underTest.size());
            break;
        case HUGE_CACHE:
            assertEquals(2, underTest.size());
            break;
        }

        underTest.remove("/fs-test/folder1/file1a");

        switch (cacheSize) {
        case NO_CACHE:
        case SMALL_CACHE:
            assertEquals(0, underTest.size());
            break;
        case HUGE_CACHE:
            assertEquals(1, underTest.size());
            break;
        }
        
        underTest.clear();

        assertEquals(0, underTest.size());        
    }

}
