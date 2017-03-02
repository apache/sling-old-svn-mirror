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

import org.junit.Test;

public class ContentFileParserUtilTest {

    @SuppressWarnings("unchecked")
    @Test
    public void testParseJson() {
        File file = new File("src/test/resources/fs-test/folder2/content.json");
        Map<String,Object> content = ContentFileParserUtil.parse(file);
        assertNotNull(content);
        assertEquals("app:Page", content.get("jcr:primaryType"));
        assertEquals("app:PageContent", ((Map<String,Object>)content.get("jcr:content")).get("jcr:primaryType"));
    }

    @Test
    public void testParseInvalidJson() {
        File file = new File("src/test/resources/invalid-test/invalid.json");
        Map<String,Object> content = ContentFileParserUtil.parse(file);
        assertNull(content);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testParseJcrXml() {
        File file = new File("src/test/resources/fs-test/folder3/content.jcr.xml");
        Map<String,Object> content = ContentFileParserUtil.parse(file);
        assertNotNull(content);
        assertEquals("app:Page", content.get("jcr:primaryType"));
        assertEquals("app:PageContent", ((Map<String,Object>)content.get("jcr:content")).get("jcr:primaryType"));
    }

    @Test
    public void testParseInvalidJcrXml() {
        File file = new File("src/test/resources/invalid-test/invalid.jcr.xml");
        Map<String,Object> content = ContentFileParserUtil.parse(file);
        assertNull(content);
    }

}
