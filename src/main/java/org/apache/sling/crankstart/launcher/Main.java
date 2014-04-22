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
package org.apache.sling.crankstart.launcher;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import org.apache.sling.crankstart.api.CrankstartConstants;

/** Execute a crankstart file */
public class Main {
    public static final String CLASSPATH_PREFIX = "classpath ";
    
    public static void main(String [] args) throws Exception {
        String crankFile = "default.crank.txt";
        if(args.length < 1) {
            System.err.println("Using default crank file " + crankFile);
            System.err.println("To use a different one, provide its name as a jar file argument");
        } else {
            crankFile = args[0];
        }
        System.setProperty(CrankstartConstants.CRANKSTART_INPUT_FILENAME, crankFile);
        System.setProperty( "java.protocol.handler.pkgs", "org.ops4j.pax.url" );
        final URL [] launcherClasspath = getClasspath(crankFile);
        
        final URLClassLoader launcherClassloader = new URLClassLoader(launcherClasspath, null);
        final String callableClass = "org.apache.sling.crankstart.core.CrankstartFileProcessor";
        
        @SuppressWarnings("unchecked")
        final Callable<Object> c = (Callable<Object>)launcherClassloader.loadClass(callableClass).newInstance();
        c.call();
    }
    
    private static URL[] getClasspath(String filename) throws IOException {
        final List<URL> urls = new ArrayList<URL>();
        final Reader input = new FileReader(new File(filename));
        final BufferedReader r = new BufferedReader(input);
        try {
            String line = null;
            while((line = r.readLine()) != null) {
                if(line.length() == 0 || line.startsWith("#")) {
                    // ignore comments and blank lines
                } else if(line.startsWith(CLASSPATH_PREFIX)){
                    urls.add(new URL(line.substring(CLASSPATH_PREFIX.length()).trim()));
                }
            }
            return urls.toArray(new URL[] {});
        } finally {
            r.close();
        }
    }
}