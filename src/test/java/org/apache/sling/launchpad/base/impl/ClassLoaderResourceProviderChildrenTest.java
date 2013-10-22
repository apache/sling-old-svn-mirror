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
package org.apache.sling.launchpad.base.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.JarURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mockito;

public class ClassLoaderResourceProviderChildrenTest {
    
    private ClassLoader classLoader;
    private ClassLoaderResourceProvider provider;
    private boolean throwExceptionOnOpenConnection = false;
    
    private static final String [] TEST_PATHS = {
        "resources/install",
        "resources/install/one.jar",
        "resources/install/sub/two.jar",
        "resources/install/sub/six.jar",
        "resources/install.jackrabbit/three.jar",
        "resources/install.jackrabbit/seven.jar",
        "resources/install.oak/four.jar",
        "resources/install.oak/sub/five.jar"
    };
    
    private ClassLoader mockClassLoader(String ... paths) throws MalformedURLException, IOException {
        final ClassLoader cl = Mockito.mock(ClassLoader.class);
        final JarURLConnection conn = Mockito.mock(JarURLConnection.class);
        final URLStreamHandler handler = new URLStreamHandler() {
            @Override
            protected URLConnection openConnection(final URL url) throws IOException {
                if(throwExceptionOnOpenConnection) {
                    throw new IOException("Throwing up for testing that");
                }
                return conn;
            }
        };
        final JarFile f = Mockito.mock(JarFile.class);
        final URL url = new URL("jar://some.jar", "localhost", 1234, "some.jar", handler);
        
        final Vector<JarEntry> entries = new Vector<JarEntry>();
        for(String path : paths) {
            entries.add(new JarEntry(path));
        }
        
        when(cl.getResource(Matchers.contains("install"))).thenReturn(url);
        when(conn.getJarFile()).thenReturn(f);
        when(f.entries()).thenReturn(entries.elements());
        
        return cl;
    }
    
    private void assertChildren(ClassLoaderResourceProvider p, String path, String ... expected) {
        final List<String> result = new ArrayList<String>();
        final Iterator<String> it = p.getChildren(path);
        while(it.hasNext()) {
            result.add(it.next());
        }
        for(String exp : expected) {
            if(!result.contains(exp)) {
                fail(path + ": expected child is not present in result: " + exp + ", result=" + result);
            }
        }
        assertEquals(path + ": expecting " + expected.length + " children, result=" + result, expected.length, result.size());
    }
    
    @Before
    public void setup() throws MalformedURLException, IOException {
        classLoader = mockClassLoader(TEST_PATHS);
        provider = new ClassLoaderResourceProvider(classLoader);
    }
    
    @Test
    public void testInstall() {
        assertChildren(provider, "resources/install", "resources/install/one.jar");
    }
    
    @Test
    public void testInstallTrailingSlahs() {
        assertChildren(provider, "resources/install/", "resources/install/one.jar");
    }
    
    @Test
    public void testInstallSub() {
        assertChildren(provider, 
                "resources/install/sub", 
                "resources/install/sub/two.jar",
                "resources/install/sub/six.jar");
    }
    
    @Test
    public void testInstallJackrabbit() {
        assertChildren(provider, 
                "resources/install.jackrabbit", 
                "resources/install.jackrabbit/three.jar",
                "resources/install.jackrabbit/seven.jar");
    }
    
    @Test
    public void testInstallJackrabbitTrailingSlash() {
        assertChildren(provider, 
                "resources/install.jackrabbit/", 
                "resources/install.jackrabbit/three.jar",
                "resources/install.jackrabbit/seven.jar");
    }
    
    @Test
    public void testInstallOak() {
        assertChildren(provider, 
                "resources/install.oak",
                "resources/install.oak/four.jar");
    }
    
    @Test
    public void testNoResults() {
        final Iterator<String> it = provider.getChildren("FOO");
        assertFalse("Expecting no children", it.hasNext());
    }
    
    @Test
    public void testException() {
        throwExceptionOnOpenConnection = true;
        final Iterator<String> it = provider.getChildren("resources/install");
        assertFalse("Expecting no results with ignored IOException", it.hasNext());
    }
}
