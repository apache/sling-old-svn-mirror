/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.testing.teleporter.client;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class SanitizeResourceNameTest {
    @Parameterized.Parameters(name = "{0}/{1}")
    public static Collection<String[]> data() {
        return Arrays.asList(new String[][] {
                { "some/base", "r/a/b", "/r/a/b" },
                { "some\\base", "r\\c\\d", "/r/c/d" },
                { "some/base", "r\\e\\f", "/r/e/f" },
                { "some\\base", "r/g/h/i", "/r/g/h/i" },
        });
    }
    
    private final String basePath;
    private final String path;
    private final String expected;
    
    public SanitizeResourceNameTest(String basePath, String path, String expected) {
        this.basePath = new File(File.separator + basePath).getAbsolutePath();
        this.path = path;
        this.expected = expected;
    }
    
    @Test
    public void sanitize() {
        final String actual = ClassResourceVisitor.sanitizeResourceName(basePath, new File(basePath, path));
        assertEquals(expected, actual);
    }
}