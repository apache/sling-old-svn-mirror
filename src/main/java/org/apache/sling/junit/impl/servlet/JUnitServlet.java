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
package org.apache.sling.junit.impl.servlet;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

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
    
    public static final String CSS = "junit.css";
    
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
    
    /** Return the list of available tests
     * @param prefix optionally select only names that match this prefix
     */
    private List<String> getTestNames(String prefix) {
        final Collection<String> testClassesCollection = testsManager.getTestNames();
        final List<String> testClasses = new LinkedList<String>();
        if(prefix == null || prefix.length() == 0) {
            testClasses.addAll(testClassesCollection);
        } else {
            for(String name : testClassesCollection) {
                if(name.startsWith(prefix)) {
                    testClasses.add(name);
                }
            }
        }
        Collections.sort(testClasses);
        return testClasses;
    }
    
    private Renderer getRenderer(RequestInfo requestInfo) {
        if(".txt".equals(requestInfo.extension)) {
            return new PlainTextRenderer();
        } else {
            return new HtmlRenderer();
        }
    }
    
    private void sendCss(HttpServletResponse response) throws IOException {
        final InputStream str = getClass().getResourceAsStream("/" + CSS);
        if(str == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, CSS);
        } else {
            response.setContentType("text/css");
            final OutputStream out = response.getOutputStream();
            final byte[] buffer = new byte[16384];
            int count = 0;
            while( (count = str.read(buffer)) > 0) {
                out.write(buffer, 0, count);
            }
            out.flush();
        }
    }
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
    throws ServletException, IOException {
        {
            final String pi = request.getPathInfo();
            if(pi != null && pi.endsWith(CSS)) {
                sendCss(response);
                return;
            }
        }
        
        final RequestInfo requestInfo = new RequestInfo(request);
        final Renderer renderer = getRenderer(requestInfo);
        log.debug("GET request: {}", requestInfo);

        renderer.setup(response, getClass().getSimpleName());
        
        if(requestInfo.testSelector.length() > 0) {
            renderer.info("info", "Test selector: " + requestInfo.testSelector); 
        } else {
            renderer.info("info", "Test selector is empty: " 
                    + "add class name prefix + extension at the end of the URL to select a subset of tests"); 
        }
        
        // Any test classes?
        final List<String> testNames = getTestNames(requestInfo.testSelector); 
        if(testNames.isEmpty()) {
            renderer.info(
                    "warning",
                    "No test classes found with prefix=" + requestInfo.testSelector
                    + ", check the requirements of the active " +
                    "TestsProvider services for how to supply tests." 
                    );
        } else {
            renderer.title(2, "Test classes");
            renderer.list("testNames", testNames);
            
            renderer.title(2, "Running tests");
            final JUnitCore junit = new JUnitCore();
            junit.addListener(renderer);
            try {
                for(String className : testNames) {
                    renderer.title(3, className);
                    junit.run(testsManager.getTestClass(className));
                }
            } catch(ClassNotFoundException cnfe) {
                throw new ServletException("Test class not found", cnfe);
            }
        }
    }
}