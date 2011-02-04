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
import java.util.Collection;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.junit.JUnitTestsManager;
import org.junit.runner.JUnitCore;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.NamespaceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Simple test runner servlet */
@SuppressWarnings("serial")
@Component
@Service
public class JUnitServlet extends HttpServlet {
    
    private final Logger log = LoggerFactory.getLogger(getClass());
    
    /** TODO make this configurable */
    public static final String SERVLET_PATH = "/system/sling/junit";
    
    @Reference
    private JUnitTestsManager testsManager;
    
    @Reference(cardinality=ReferenceCardinality.OPTIONAL_UNARY)
    private HttpService httpService;

    protected void bindHttpService(HttpService h) throws ServletException, NamespaceException {
        httpService = h;
        httpService.registerServlet(SERVLET_PATH, this, null, null);
        log.info("Servlet registered at path {}", SERVLET_PATH);
    }
    
    protected void unbindHttpService(HttpService h) throws ServletException, NamespaceException {
        h.unregister(SERVLET_PATH);
        httpService = null;
        log.info("Servlet unregistered from path {}", SERVLET_PATH);
    }
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
    throws ServletException, IOException {
        
        response.setContentType("text/plain");
        response.setCharacterEncoding("UTF-8");
        final PrintWriter pw = response.getWriter();
        pw.println("This is " + getClass().getName());
        pw.println();
        
        // Any test classes?
        final Collection<String> testClasses = testsManager.getTestNames();
        if(testClasses.isEmpty()) {
            pw.println(
                    "No test classes found, check the requirements of the active " +
                    "TestsProvider services for how to supply tests." 
                    );
            return;
        }
        
        // List test classes
        pw.println("TEST CLASSES");
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