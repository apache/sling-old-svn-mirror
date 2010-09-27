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
package org.apache.sling.servlets.resolver.internal.resource;

import java.util.HashSet;
import java.util.Set;

import javax.servlet.GenericServlet;
import javax.servlet.Servlet;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import junit.framework.TestCase;

import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.commons.testing.osgi.MockServiceReference;
import org.apache.sling.servlets.resolver.internal.ServletResolverConstants;

public class ServletResourceProviderCreateTest extends TestCase {

    private static final Servlet TEST_SERVLET = new GenericServlet() {
        @Override
        public void service(ServletRequest req, ServletResponse res) {
        }
    };

    private static final String ROOT = "/apps/";

    private static final String RES_TYPE = "sling:sample";

    private static final String RES_TYPE_PATH = ResourceUtil.resourceTypeToPath(RES_TYPE);

    private ServletResourceProviderFactory factory = new ServletResourceProviderFactory(
        ROOT, new String[] {"/apps/"});

    public void testCreateMethodsDefault() {
        MockServiceReference msr = new MockServiceReference(null);

        msr.setProperty(ServletResolverConstants.SLING_SERVLET_RESOURCE_TYPES,
            RES_TYPE);
        // msr.setProperty(ServletResolverConstants.SLING_SERVLET_METHODS, "*");
        ServletResourceProvider srp = factory.create(msr);
        assertNotNull(srp);
        srp.setServlet(TEST_SERVLET);

        String[] paths = srp.getServletPaths();
        assertNotNull(paths);
        assertEquals(2, paths.length);

        Set<String> checkerSet = new HashSet<String>();
        checkerSet.add(ROOT + RES_TYPE_PATH + "/" + HttpConstants.METHOD_GET
            + ServletResourceProviderFactory.SERVLET_PATH_EXTENSION);
        checkerSet.add(ROOT + RES_TYPE_PATH + "/" + HttpConstants.METHOD_HEAD
            + ServletResourceProviderFactory.SERVLET_PATH_EXTENSION);

        for (String path : paths) {
            assertTrue(path + " not expected", checkerSet.remove(path));
        }

        assertTrue(checkerSet.isEmpty());
    }

    public void testCreateMethodsSingle() {
        MockServiceReference msr = new MockServiceReference(null);

        msr.setProperty(ServletResolverConstants.SLING_SERVLET_RESOURCE_TYPES,
            RES_TYPE);
        msr.setProperty(ServletResolverConstants.SLING_SERVLET_METHODS, "GET");

        ServletResourceProvider srp = factory.create(msr);
        assertNotNull(srp);
        srp.setServlet(TEST_SERVLET);

        String[] paths = srp.getServletPaths();
        assertNotNull(paths);
        assertEquals(1, paths.length);

        Set<String> checkerSet = new HashSet<String>();
        checkerSet.add(ROOT + RES_TYPE_PATH + "/" + HttpConstants.METHOD_GET
            + ServletResourceProviderFactory.SERVLET_PATH_EXTENSION);

        for (String path : paths) {
            assertTrue(path + " not expected", checkerSet.remove(path));
        }

        assertTrue(checkerSet.isEmpty());
    }

    public void testCreateMethodsMultiple() {
        MockServiceReference msr = new MockServiceReference(null);

        msr.setProperty(ServletResolverConstants.SLING_SERVLET_RESOURCE_TYPES,
            RES_TYPE);
        msr.setProperty(ServletResolverConstants.SLING_SERVLET_METHODS,
            new String[] { "GET", "POST", "PUT" });

        ServletResourceProvider srp = factory.create(msr);
        assertNotNull(srp);
        srp.setServlet(TEST_SERVLET);

        String[] paths = srp.getServletPaths();
        assertNotNull(paths);
        assertEquals(3, paths.length);

        Set<String> checkerSet = new HashSet<String>();
        checkerSet.add(ROOT + RES_TYPE_PATH + "/" + HttpConstants.METHOD_GET
            + ServletResourceProviderFactory.SERVLET_PATH_EXTENSION);
        checkerSet.add(ROOT + RES_TYPE_PATH + "/" + HttpConstants.METHOD_POST
            + ServletResourceProviderFactory.SERVLET_PATH_EXTENSION);
        checkerSet.add(ROOT + RES_TYPE_PATH + "/" + HttpConstants.METHOD_PUT
            + ServletResourceProviderFactory.SERVLET_PATH_EXTENSION);

        for (String path : paths) {
            assertTrue(path + " not expected", checkerSet.remove(path));
        }

        assertTrue(checkerSet.isEmpty());
    }

    public void testCreateMethodsAll() {
        MockServiceReference msr = new MockServiceReference(null);

        msr.setProperty(ServletResolverConstants.SLING_SERVLET_RESOURCE_TYPES,
            RES_TYPE);
        msr.setProperty(ServletResolverConstants.SLING_SERVLET_METHODS, "*");

        ServletResourceProvider srp = factory.create(msr);
        assertNotNull(srp);
        srp.setServlet(TEST_SERVLET);

        String[] paths = srp.getServletPaths();
        assertNotNull(paths);
        assertEquals(1, paths.length);

        Set<String> checkerSet = new HashSet<String>();
        checkerSet.add(ROOT + RES_TYPE_PATH
            + ServletResourceProviderFactory.SERVLET_PATH_EXTENSION);

        for (String path : paths) {
            assertTrue(path + " not expected", checkerSet.remove(path));
        }

        assertTrue(checkerSet.isEmpty());
    }

    public void testCreateSelectorsExtensions() {
        MockServiceReference msr = new MockServiceReference(null);

        msr.setProperty(ServletResolverConstants.SLING_SERVLET_RESOURCE_TYPES,
            RES_TYPE);
        msr.setProperty(ServletResolverConstants.SLING_SERVLET_METHODS,
            new String[] { HttpConstants.METHOD_GET });
        msr.setProperty(ServletResolverConstants.SLING_SERVLET_SELECTORS,
            new String[] { "ext" });
        msr.setProperty(ServletResolverConstants.SLING_SERVLET_EXTENSIONS,
            new String[] { "json" });

        ServletResourceProvider srp = factory.create(msr);
        assertNotNull(srp);
        srp.setServlet(TEST_SERVLET);

        String[] paths = srp.getServletPaths();
        assertNotNull(paths);
        assertEquals(1, paths.length);

        Set<String> checkerSet = new HashSet<String>();
        checkerSet.add(ROOT + RES_TYPE_PATH + "/ext.json."
            + HttpConstants.METHOD_GET
            + ServletResourceProviderFactory.SERVLET_PATH_EXTENSION);

        for (String path : paths) {
            assertTrue(path + " not expected", checkerSet.remove(path));
        }

        assertTrue(checkerSet.isEmpty());
    }
    public void testCreateMethodsExtensions() {
        MockServiceReference msr = new MockServiceReference(null);

        msr.setProperty(ServletResolverConstants.SLING_SERVLET_RESOURCE_TYPES,
            RES_TYPE);
        msr.setProperty(ServletResolverConstants.SLING_SERVLET_METHODS,
            new String[] { HttpConstants.METHOD_GET, HttpConstants.METHOD_POST });
        msr.setProperty(ServletResolverConstants.SLING_SERVLET_EXTENSIONS,
            new String[] { "json", "html" });

        ServletResourceProvider srp = factory.create(msr);
        assertNotNull(srp);
        srp.setServlet(TEST_SERVLET);

        String[] paths = srp.getServletPaths();
        assertNotNull(paths);
        assertEquals(4, paths.length);

        Set<String> checkerSet = new HashSet<String>();
        checkerSet.add(ROOT + RES_TYPE_PATH + "/json."
            + HttpConstants.METHOD_GET
            + ServletResourceProviderFactory.SERVLET_PATH_EXTENSION);
        checkerSet.add(ROOT + RES_TYPE_PATH + "/html."
            + HttpConstants.METHOD_GET
            + ServletResourceProviderFactory.SERVLET_PATH_EXTENSION);
        checkerSet.add(ROOT + RES_TYPE_PATH + "/json."
            + HttpConstants.METHOD_POST
            + ServletResourceProviderFactory.SERVLET_PATH_EXTENSION);
        checkerSet.add(ROOT + RES_TYPE_PATH + "/html."
            + HttpConstants.METHOD_POST
            + ServletResourceProviderFactory.SERVLET_PATH_EXTENSION);

        for (String path : paths) {
            assertTrue(path + " not expected", checkerSet.remove(path));
        }

        assertTrue(checkerSet.isEmpty());
    }

}
