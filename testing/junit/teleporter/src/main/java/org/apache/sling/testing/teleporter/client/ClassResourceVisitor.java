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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/** Finds and visits resources provided by ClassLoaders based
 *  on their path. If a path points to a folder, its child
 *  resources are also visited. Currently works for the "file:"
 *  and "jar:" resource protocols. 
 */
public class ClassResourceVisitor {
    
    private final Class<?> clazz;
    private final String path;
    
    static interface Processor {
        void process(String resourcePath, InputStream resourceStream) throws IOException;
    }
    
    /** Visit the resources provided by clazz's ClassLoader,
     *  based on the supplied path.
     *  
     * @param clazz
     * @param path If that points to a folder, it is visited recursively
     */
    public ClassResourceVisitor(Class<?> clazz, String path) {
        this.clazz = clazz;
        this.path = path;
    }
    
    public void visit(Processor p) throws IOException {
        final URL resourceURL = clazz.getResource(path);
        if(resourceURL == null) {
            return;
        }
        
        final String protocol = resourceURL.getProtocol();
        
        if("file".equals(protocol)) {
            // Get base path and remove ending slash
            //String basePath = clazz.getResource("/").getPath();
            //basePath = basePath.substring(0, basePath.length() - 1);
            final String basePath = new File(clazz.getResource("/").getPath()).getAbsolutePath();
            processFile(basePath, new File(resourceURL.getPath()), p);
            
        } else if("jar".equals(protocol)) {
            // Jar entries use relative paths
            final String rPath = path.startsWith("/") ? path.substring(1) : path;
            final String jarFilePath = resourceURL.getPath().split("!")[0].substring("file:".length());
            final JarFile jar = new JarFile(jarFilePath);
            try {
                final Enumeration<JarEntry> entries = jar.entries();
                while (entries.hasMoreElements()) {
                    final JarEntry e = entries.nextElement();
                    if(!e.isDirectory() && jarEntryMatches(rPath, e.getName())) {
                        final InputStream is = jar.getInputStream(e);
                        try {
                            p.process("/" + e.getName(), is);
                        } finally {
                            is.close();
                        }
                    }
                }
            } finally {
                jar.close();
            }
           
            
        } else {
            throw new IllegalArgumentException("Unkown protocol " + protocol);
        }
    }
    
    /** True if the entry part matches the given path.
     * @param givenPath if it ends with / it's considered a folder name, otherwise
     *              we check for an exact match
     * @param entryPath the path of the jar entry to check
     * @return true if there's a match
     */
    private boolean jarEntryMatches(String givenPath, String entryPath) {
        if(givenPath.endsWith("/")) {
            return entryPath.startsWith(givenPath);
        } else {
            return entryPath.equals(givenPath);
        }
    }
    
    /* Backslashes are valid in Zip-files, BUT Java expects paths to be delimited by forward slashes.
     * Windows provides file paths using backslashes, which need to be converted here.
     */
    static String sanitizeResourceName(String basePath, File resource) {
        return resource.getAbsolutePath().substring(basePath.length()).replace("\\", "/");
    }
    
    private void processFile(String basePath, File f, Processor p) throws IOException {
        if(f.isDirectory()) {
            final String [] names = f.list();
            if(names != null) {
                for(String name : names) {
                    processFile(basePath, new File(f, name), p);
                }
            }
        } else {
            final InputStream is = new BufferedInputStream(new FileInputStream(f));
            try {
                p.process(sanitizeResourceName(basePath, f), is);            
            } finally {
                is.close();
            }
        }
    }
}