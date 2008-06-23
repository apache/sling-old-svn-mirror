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
package org.apache.sling.extensions.gwt.user.server.rpc;

import com.google.gwt.user.client.rpc.SerializationException;
import com.google.gwt.user.server.rpc.RemoteServiceServlet;
import com.google.gwt.user.server.rpc.SerializationPolicy;
import com.google.gwt.user.server.rpc.SerializationPolicyLoader;
import org.osgi.framework.Bundle;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;

/**
 * Extending google's remote service servlet to enable resolving of resources through
 * a bundle (for policy file loading).
 * <p/>
 * This class is for version 1.4.60 of the GWT gwt-servlet.jar edition and it is highly recommended to compile
 * client apps with the corresponding 1.4.60 GWT compiler only!
 * <p/>
 * GWT service servlets that are used in sling are required to extend the <code>SlingRemoteServiceServlet</code>
 * instead of google's own <code>RemoteServiceServlet</code>.
 * <p/>
 * It is important that any bundle using the Sling GWT Servlet Library imports the required packages from this bundle,
 * for otherwise RPC calls will fail due to well hidden <code>ClassNotFoundException</code>s. The client app will in
 * such a case only report "This application is outdated, please hit refresh...". As such, import the following
 * packages:
 * <p/>
 * <code>
 * org.apache.sling.extensions.gwt.user.server.rpc,
 * com.google.gwt.core.client,
 * com.google.gwt.http.client,
 * com.google.gwt.i18n.client,
 * com.google.gwt.i18n.client.constants,
 * com.google.gwt.i18n.client.impl,
 * com.google.gwt.junit.client,
 * com.google.gwt.junit.client.impl,
 * com.google.gwt.user.client,
 * com.google.gwt.user.client.impl,
 * com.google.gwt.user.client.rpc,
 * com.google.gwt.user.client.rpc.core.java.lang,
 * com.google.gwt.user.client.rpc.core.java.util,
 * com.google.gwt.user.client.rpc.impl,
 * com.google.gwt.user.client.ui,
 * com.google.gwt.user.client.ui.impl,
 * com.google.gwt.user.server.rpc,
 * com.google.gwt.user.server.rpc.impl,
 * com.google.gwt.xml.client,
 * com.google.gwt.xml.client.impl
 * </code>
 */
public class SlingRemoteServiceServlet extends RemoteServiceServlet {

    /**
     * The <code>org.osgi.framework.Bundle</code> to load resources from.
     */
    private Bundle bundle;

    /**
     * The <code>ClassLoader</code> to use when GWT reflects on RPC classes.
     */
    private ClassLoader classLoader;

    /**
     * Process a call originating from the given request. Uses the
     * {@link com.google.gwt.user.server.rpc.RPC#invokeAndEncodeResponse(Object, java.lang.reflect.Method, Object[])}
     * method to do the actual work.
     * <p>
     * Subclasses may optionally override this method to handle the payload in any
     * way they desire (by routing the request to a framework component, for
     * instance). The {@link javax.servlet.http.HttpServletRequest} and {@link javax.servlet.http.HttpServletResponse}
     * can be accessed via the {@link #getThreadLocalRequest()} and
     * {@link #getThreadLocalResponse()} methods.
     * </p>
     * This is public so that it can be unit tested easily without HTTP.
     * <p/>
     * In order to properly operate within Sling/OSGi, the classloader used by GWT has to be rerouted from
     * <code>Thread.currentThread().getContextClassLoader()</code> to the classloader provided by the bundle.
     *
     * @param payload the UTF-8 request payload
     * @return a string which encodes either the method's return, a checked
     *         exception thrown by the method, or an
     *         {@link com.google.gwt.user.client.rpc.IncompatibleRemoteServiceException}
     * @throws com.google.gwt.user.client.rpc.SerializationException
     *                          if we cannot serialize the response
     * @throws com.google.gwt.user.server.rpc.UnexpectedException
     *                          if the invocation throws a checked exception
     *                          that is not declared in the service method's signature
     * @throws RuntimeException if the service method throws an unchecked
     *                          exception (the exception will be the one thrown by the service)
     */
    @Override
    public String processCall(String payload) throws SerializationException {
        String result;
        if (classLoader != null) {
            final ClassLoader old = Thread.currentThread().getContextClassLoader();
            Thread.currentThread().setContextClassLoader(classLoader);
            result = super.processCall(payload);
            Thread.currentThread().setContextClassLoader(old);
        } else {
            result = super.processCall(payload);
        }
        return result;
    }

    /**
     * Gets the {@link com.google.gwt.user.server.rpc.SerializationPolicy} for given module base URL and strong
     * name if there is one.
     * <p/>
     * Override this method to provide a {@link com.google.gwt.user.server.rpc.SerializationPolicy} using an
     * alternative approach.
     * <p/>
     * This method has been overriden, so that the serialization policy can be properly loaded as a bundle entry,
     * as Sling does not support <code>ServletContext.getResourceAsStream()</code>.
     *
     * @param request       the HTTP request being serviced
     * @param moduleBaseURL as specified in the incoming payload
     * @param strongName    a strong name that uniquely identifies a serialization
     *                      policy file
     * @return a {@link com.google.gwt.user.server.rpc.SerializationPolicy} for the given module base URL and
     *         strong name, or <code>null</code> if there is none
     */
    @Override
    protected SerializationPolicy doGetSerializationPolicy(HttpServletRequest request, String moduleBaseURL, String strongName) {

        // The request can tell you the path of the web app relative to the
        // container root.
        String contextPath = request.getContextPath();

        String modulePath = null;
        if (moduleBaseURL != null) {
            try {
                modulePath = new URL(moduleBaseURL).getPath();
            } catch (MalformedURLException ex) {
                // log the information, we will default
                getServletContext().log("Malformed moduleBaseURL: " + moduleBaseURL, ex);
            }
        }

        SerializationPolicy serializationPolicy = null;

        /*
        * Check that the module path must be in the same web app as the servlet
        * itself. If you need to implement a scheme different than this, override
        * this method.
        */
        if (modulePath == null || !modulePath.startsWith(contextPath)) {
            String message = "ERROR: The module path requested, "
                    + modulePath
                    + ", is not in the same web application as this servlet, "
                    + contextPath
                    + ".  Your module may not be properly configured or your client and server code maybe out of date.";
            getServletContext().log(message);
        } else {
            // Strip off the context path from the module base URL. It should be a
            // strict prefix.
            String contextRelativePath = modulePath.substring(contextPath.length());

            String serializationPolicyFilePath = SerializationPolicyLoader.getSerializationPolicyFileName(contextRelativePath
                    + strongName);

            // Open the RPC resource file read its contents.
            InputStream is = null;
            // if the bundle was set by the extending class, load the resource from it instead of the servlet context
            if (bundle != null) {
                try {
                    is = bundle.getResource(serializationPolicyFilePath).openStream();
                } catch (IOException e) {
                    //ignore
                }
            } else {
                is = getServletContext().getResourceAsStream(
                        serializationPolicyFilePath);
            }
            try {
                if (is != null) {
                    try {
                        serializationPolicy = SerializationPolicyLoader.loadFromStream(is);
                    } catch (ParseException e) {
                        getServletContext().log(
                                "ERROR: Failed to parse the policy file '"
                                        + serializationPolicyFilePath + "'", e);
                    } catch (ClassNotFoundException e) {
                        getServletContext().log(
                                "ERROR: Could not find class '" + e.getMessage()
                                        + "' listed in the serialization policy file '"
                                        + serializationPolicyFilePath + "'"
                                        + "; your server's classpath may be misconfigured", e);
                    } catch (IOException e) {
                        getServletContext().log(
                                "ERROR: Could not read the policy file '"
                                        + serializationPolicyFilePath + "'", e);
                    }
                } else {
                    String message = "ERROR: The serialization policy file '"
                            + serializationPolicyFilePath
                            + "' was not found; did you forget to include it in this deployment?";
                    getServletContext().log(message);
                }
            } finally {
                if (is != null) {
                    try {
                        is.close();
                    } catch (IOException e) {
                        // Ignore this error
                    }
                }
            }
        }

        return serializationPolicy;
    }

    /**
     * Allows the extending OSGi service to set the bundle it is part of. The bundle is used to provide access
     * to the policy file otherwise loaded by <code>getServletContext().getResourceAsStream()</code> which is not
     * supported in Sling.
     *
     * @param bundle The bundle to load the resource (policy file) from.
     */
    protected void setBundle(Bundle bundle) {
        this.bundle = bundle;
    }

    /**
     * Allows the extending OSGi service to set its classloader.
     *
     * @param classLoader The classloader to provide to the SlingRemoteServiceServlet.
     */
    protected void setClassLoader(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }
}
