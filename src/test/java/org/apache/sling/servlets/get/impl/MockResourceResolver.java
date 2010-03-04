/*
 * Copyright 1997-2009 Day Management AG
 * Barfuesserplatz 6, 4001 Basel, Switzerland
 * All Rights Reserved.
 *
 * This software is the confidential and proprietary information of
 * Day Management AG, ("Confidential Information"). You shall not
 * disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Day.
 */
package org.apache.sling.servlets.get.impl;

import java.util.Iterator;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;

/**
 * The <code>MockResourceResolver</code> implements the {@link #map(String)}
 * method simply returning the path unmodified and the
 * {@link #map(HttpServletRequest, String)} method returning the path prefixed
 * with the request context path.
 * <p>
 * The other methods are not implemented and return <code>null</code>.
 */
public class MockResourceResolver implements ResourceResolver {

    public Iterator<Resource> findResources(String arg0, String arg1) {
        return null;
    }

    public Resource getResource(String arg0) {
        return null;
    }

    public Resource getResource(Resource arg0, String arg1) {
        return null;
    }

    public String[] getSearchPath() {
        return null;
    }

    public Iterator<Resource> listChildren(Resource arg0) {
        return null;
    }

    public String map(String path) {
        return path;
    }

    public String map(HttpServletRequest request, String path) {
        if (request.getContextPath().length() == 0) {
            return path;
        }

        return request.getContextPath() + path;
    }

    public Iterator<Map<String, Object>> queryResources(String arg0, String arg1) {
        return null;
    }

    public Resource resolve(String arg0) {
        return null;
    }

    public Resource resolve(HttpServletRequest arg0) {
        return null;
    }

    public Resource resolve(HttpServletRequest arg0, String arg1) {
        return null;
    }

    public <AdapterType> AdapterType adaptTo(Class<AdapterType> arg0) {
        return null;
    }

    public void close() {
        // nothing to do
    }
}
