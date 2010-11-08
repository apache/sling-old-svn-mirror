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

import javax.servlet.http.HttpServlet;
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
 * This class is for version 2.0.4 of the GWT gwt-servlet.jar edition and it is highly recommended to compile
 * client apps with the corresponding 2.0.4 GWT gwt-user.jar only!
 * <p/>
 * GWT service servlets that are used in sling are required to extend the <code>SlingRemoteServiceServlet</code>
 * instead of google's own <code>RemoteServiceServlet</code>.
 * </code>
 */
public class SlingRemoteServiceServlet extends RemoteServiceServlet {
    
    // This is a verbatim copy of the method of the same signature from
    // RemoteServiceServlet, with one exception (noted inline) which loads
    // the policy file from the bundle, as Sling doesn't support
    // ServletContext.getResourceAsStream().
    static SerializationPolicy loadSerializationPolicy(HttpServlet servlet,
        HttpServletRequest request, String moduleBaseURL, String strongName,
        Bundle bundle) {
      // The request can tell you the path of the web app relative to the
      // container root.
      String contextPath = request.getContextPath();

      String modulePath = null;
      if (moduleBaseURL != null) {
        try {
          modulePath = new URL(moduleBaseURL).getPath();
        } catch (MalformedURLException ex) {
          // log the information, we will default
          servlet.log("Malformed moduleBaseURL: " + moduleBaseURL, ex);
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
        servlet.log(message, null);
      } else {
        // Strip off the context path from the module base URL. It should be a
        // strict prefix.
        String contextRelativePath = modulePath.substring(contextPath.length());

        String serializationPolicyFilePath = SerializationPolicyLoader.getSerializationPolicyFileName(contextRelativePath
            + strongName);

        // BEGIN - REMOVE CODE
        // Open the RPC resource file and read its contents.
        // InputStream is = servlet.getServletContext().getResourceAsStream(
        //    serializationPolicyFilePath);
        // END - REMOVE CODE
        
        // BEGIN - NEW CODE
        InputStream is = null;
        // if the bundle was set by the extending class, load the resource from it instead of the servlet context
        if (bundle != null) {
            try {
                is = bundle.getResource(serializationPolicyFilePath).openStream();
            } catch (IOException e) {
                //ignore
            }
        } else {
            is = servlet.getServletContext().getResourceAsStream(serializationPolicyFilePath);
        }
        // END - NEW CODE
        
        try {
          if (is != null) {
            try {
              serializationPolicy = SerializationPolicyLoader.loadFromStream(is,
                  null);
            } catch (ParseException e) {
              servlet.log("ERROR: Failed to parse the policy file '"
                  + serializationPolicyFilePath + "'", e);
            } catch (IOException e) {
              servlet.log("ERROR: Could not read the policy file '"
                  + serializationPolicyFilePath + "'", e);
            }
          } else {
            String message = "ERROR: The serialization policy file '"
                + serializationPolicyFilePath
                + "' was not found; did you forget to include it in this deployment?";
            servlet.log(message);
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
     * Gets the {@link SerializationPolicy} for given module base URL and strong
     * name if there is one.
     * 
     * Override this method to provide a {@link SerializationPolicy} using an
     * alternative approach.
     * 
     * @param request the HTTP request being serviced
     * @param moduleBaseURL as specified in the incoming payload
     * @param strongName a strong name that uniquely identifies a serialization
     *          policy file
     * @return a {@link SerializationPolicy} for the given module base URL and
     *         strong name, or <code>null</code> if there is none
     */
    protected SerializationPolicy doGetSerializationPolicy(
        HttpServletRequest request, String moduleBaseURL, String strongName) {
      return loadSerializationPolicy(this, request, moduleBaseURL, strongName, bundle);
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
