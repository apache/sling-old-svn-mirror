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
package org.apache.sling.junit.impl;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

import javax.servlet.ServletException;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.junit.JUnitConstants;
import org.apache.sling.junit.JUnitTestsManager;
import org.junit.runner.JUnitCore;

/** Simple test runner servlet */
@SuppressWarnings("serial")
@Component
@Service
@Property(name="sling.servlet.paths",value="/system/sling/junit")
public class JUnitServlet extends SlingAllMethodsServlet {
    
    @Reference
    private JUnitTestsManager testsManager;
    
    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) 
    throws ServletException, IOException {
        
        response.setContentType("text/plain");
        response.setCharacterEncoding("UTF-8");
        final PrintWriter pw = response.getWriter();
        pw.println("This is " + getClass().getName());
        pw.println();
        
        // Any test classes?
        final List<String> testClasses = testsManager.getTestClasses();
        if(testClasses.isEmpty()) {
            pw.println(
                    "No test classes found, please activate at least one bundle "
                    + "which exports JUnit test classes and points to them using a "
                    + JUnitConstants.SLING_TEST_REGEXP + " header."
                    );
            return;
        }
        
        // List test classes
        pw.println("TEST CLASSES (found in bundles that have a " + JUnitConstants.SLING_TEST_REGEXP + " header):");
        for(String className : testClasses) {
            pw.println(className);
        }
        pw.println();

        // Run tests
        final JUnitCore junit = new JUnitCore();
        junit.addListener(new PlainTextRunListener(pw));
        try {
            for(String className : testClasses) {
                pw.println("**** RUNNING TESTS: " + className);
                junit.run(testsManager.getTestClass(className));
                pw.println();
            }
        } catch(ClassNotFoundException cnfe) {
            throw new ServletException("Test class not found", cnfe);
        }
    }
}
