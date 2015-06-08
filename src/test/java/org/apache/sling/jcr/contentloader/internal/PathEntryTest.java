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
package org.apache.sling.jcr.contentloader.internal;

import org.apache.sling.commons.osgi.ManifestHeader;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JMock;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.osgi.framework.Bundle;

import java.util.*;

import static org.junit.Assert.*;

@RunWith(JMock.class)
public class PathEntryTest {
    final String pathEntryPath = "/test/path";
    Mockery mockery = new JUnit4Mockery();

    @Test
    public void testGetContentPaths(){
        final Bundle bundle = mockery.mock(Bundle.class);
        final Dictionary<String, String> dict = new Hashtable();
        dict.put("Bnd-LastModified", "1258555936230");
        dict.put(PathEntry.CONTENT_HEADER, "test1, test2");
        //PathEntry#getContentPaths should return this value
        //since its lower that value of "Bnd-LastModified" key
        final long lastModifiedValue = 1258555936229L;

        mockery.checking(new Expectations() {{
            allowing(bundle).getLastModified(); will(returnValue(lastModifiedValue));
            allowing(bundle).getHeaders(); will(returnValue(dict));
        }});

        int i = 0;
        Iterator<PathEntry> paths = PathEntry.getContentPaths(bundle);
        assertNotNull(paths);
        while(paths.hasNext()){
            i++;
            assertEquals(lastModifiedValue, paths.next().getLastModified());
        }
        assertEquals(2, i);

        dict.remove(PathEntry.CONTENT_HEADER);
        assertNull(PathEntry.getContentPaths(bundle));

        mockery.assertIsSatisfied();
    }

    @Test
    public void testConstructorSetup() throws NoSuchFieldException {
        final String overwriteDirective = "true";
        final String overwritePropertiesDirective = "true";
        final String uninstallDirective = "true";
        final String pathDirective = "PATH_DIRECTIVE";
        final String checkInDirective = "true";
        final String autoCheckoutDirective = "true";
        final String ignoreContentReadersDirective = "mp3,txt,java";
        final String workspaceDirective = "WORKSPACE_DIRECTIVE";
        final Long lastModified = 1L;

        final Map<String, String> props = new HashMap<String, String>();

        props.put(PathEntry.OVERWRITE_DIRECTIVE, overwriteDirective);
        props.put(PathEntry.OVERWRITE_PROPERTIES_DIRECTIVE, overwritePropertiesDirective);
        props.put(PathEntry.UNINSTALL_DIRECTIVE, uninstallDirective);
        props.put(PathEntry.PATH_DIRECTIVE, pathDirective);
        props.put(PathEntry.CHECKIN_DIRECTIVE, checkInDirective);
        props.put(PathEntry.AUTOCHECKOUT_DIRECTIVE, autoCheckoutDirective);
        props.put(PathEntry.IGNORE_CONTENT_READERS_DIRECTIVE, ignoreContentReadersDirective);
        props.put(PathEntry.WORKSPACE_DIRECTIVE, workspaceDirective);

        final ManifestHeader.Entry entry = new TestEntry(props);
        final PathEntry pathEntry = new PathEntry(entry, lastModified);

        assertEquals(pathEntryPath, pathEntry.getPath());
        assertEquals(lastModified, pathEntry.getLastModified(), 0);
        assertEquals(Boolean.valueOf(overwriteDirective), pathEntry.isOverwrite());
        assertEquals(Boolean.valueOf(overwritePropertiesDirective), pathEntry.isPropertyOverwrite());
        assertEquals(Boolean.valueOf(uninstallDirective), pathEntry.isUninstall());
        assertEquals(pathDirective, pathEntry.getTarget());
        assertEquals(Boolean.valueOf(checkInDirective), pathEntry.isCheckin());
        assertEquals(Boolean.valueOf(autoCheckoutDirective), pathEntry.isAutoCheckout());
        assertEquals(workspaceDirective, pathEntry.getWorkspace());
        assertTrue(pathEntry.isIgnoredImportProvider("mp3"));
        assertTrue(pathEntry.isIgnoredImportProvider(".mp3"));
        assertTrue(pathEntry.isIgnoredImportProvider(".txt"));
        assertTrue(pathEntry.isIgnoredImportProvider("java"));
        assertFalse(pathEntry.isIgnoredImportProvider("avi"));
        assertFalse(pathEntry.isIgnoredImportProvider(".avi"));
    }

    private class TestEntry implements ManifestHeader.Entry {
        private final Map<String, String> values;

        private TestEntry(Map<String, String> values){
            this.values = values;
        }

        public String getValue() {
            return pathEntryPath;
        }

        public ManifestHeader.NameValuePair[] getAttributes() {
            return new ManifestHeader.NameValuePair[0];
        }

        public ManifestHeader.NameValuePair[] getDirectives() {
            return new ManifestHeader.NameValuePair[0];
        }

        public String getAttributeValue(String name) {
            return null;
        }

        public String getDirectiveValue(String name) {
            return values.get(name);
        }
    }
}
