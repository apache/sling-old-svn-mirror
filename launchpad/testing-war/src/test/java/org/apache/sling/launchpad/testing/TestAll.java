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
package org.apache.sling.launchpad.testing;

import junit.framework.JUnit4TestAdapter;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.junit.runner.RunWith;
import org.junit.runners.AllTests;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Pattern;

/**
 *
 */
@RunWith(AllTests.class)
public class TestAll extends TestCase {
    private static final Logger LOGGER = LoggerFactory.getLogger(TestAll.class);

    @SuppressWarnings("unchecked")
    public static TestSuite suite() {
        final ClassLoader sysClassLoader = TestAll.class.getClassLoader();
        final List<String> matchingClasses = new ArrayList<String>();
        // Get the URLs
        final URL[] urls = ((URLClassLoader) sysClassLoader).getURLs();
        final String testPattern = System.getProperty("integrationTestPattern",
            "**/launchpad/webapp/integrationtest/**/*Test");
        final String testRegex = convertToRegex(testPattern);
        final Pattern pattern = Pattern.compile(testRegex);
        for (URL u : urls) {
            try {
                matchingClasses.addAll(scanFile(new File(u.toURI()), pattern));
            } catch (URISyntaxException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        
        TestSuite suite = new LoggingSuite("Sling Integration Tests matching " + testPattern, LOGGER);
        int counter = 0;
        final Set<Class<TestCase>> classSet = new HashSet<Class<TestCase>>();
        for (String classFile : matchingClasses) {
            String className = classFileToName(classFile);
            try {
                final Class<TestCase> c = (Class<TestCase>) sysClassLoader.loadClass(className);
                if (!c.isInterface() && !Modifier.isAbstract(c.getModifiers())) {
                    LOGGER.info("Added " + className);
                    suite.addTest(new JUnit4TestAdapter(c));
                    counter++;
                }
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
        if ( counter == 0 ) {
            fail("No test classes found in classpath using Pattern " + testRegex);
        }
        LOGGER.info(classSet.size() + " test classes found using Pattern "
            + testRegex);

        return suite;
    }

    /**
     * @param classFile
     * @return
     */
    private static String classFileToName(String classFile) {
        String className = classFile.substring(0,
            classFile.length() - (".class".length())).replace('/', '.');
        if (className.charAt(0) == '.') {
            className = className.substring(1);
        }
        return className;
    }

    /**
     * @param testPattern
     * @return
     */
    private static String convertToRegex(String testPattern) {
        return testPattern.replace("**/", ".a?").replace("*", ".a?").replace(
            ".a?", ".*?").replace("/", "\\/")
            + "\\.class$";
    }

    /**
     * @param u
     * @return
     * @throws IOException
     * @throws URISyntaxException
     */
    private static List<String> scanFile(File f, Pattern pattern)
            throws URISyntaxException, IOException {
        List<String> classPathMatches = new ArrayList<String>();
        if (f.isFile()) {
            scanJar(f, pattern, classPathMatches);
        } else {
            scanDir(f, pattern, classPathMatches, f.getPath().length());
        }
        return classPathMatches;
    }

    /**
     * @param f
     * @param pattern
     * @return
     */
    private static void scanDir(File f, Pattern pattern,
            List<String> classPathMatches, int trim) {
        if (f.isFile()) {
            String name = f.getPath().substring(trim);
            if (pattern.matcher(name).matches()) {
                classPathMatches.add(name);
            }
        } else {
            for (File cf : f.listFiles()) {
                scanDir(cf, pattern, classPathMatches, trim);
            }
        }
    }

    /**
     * @param u
     * @return
     * @throws URISyntaxException
     * @throws IOException
     */
    private static void scanJar(File f, Pattern pattern,
            List<String> classPathMatches) throws URISyntaxException,
            IOException {
        JarFile jarFile = new JarFile(f);
        for (Enumeration<JarEntry> e = jarFile.entries(); e.hasMoreElements();) {
            JarEntry je = e.nextElement();
            String entryName = je.getName();
            if (pattern.matcher(entryName).matches()) {
                classPathMatches.add(entryName);
            }
        }
        jarFile.close();
    }

}
