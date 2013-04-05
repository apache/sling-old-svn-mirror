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

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.jacoco.agent.rt.IAgent;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.NamespaceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.MBeanServer;
import javax.management.MBeanServerInvocationHandler;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.Dictionary;

/**
 * SUMMARY:
 * A JaCoCo agent REST Servlet, which exposes code coverage data to HTTP clients by calling
 * {@link IAgent#getExecutionData(boolean)}. A POST method will reset the agent after returning the execution data.
 * A GET method calls the operation without resetting the agent.
 *
 * Requests to this servlet will return 404 if the JaCoCo JVM agent is not attached and registered as an {@link IAgent}
 * MBean.
 *
 * IMPORTANT SECURITY CONSIDERATION:
 * "The ports and connections opened in tcpserver and tcpclient mode and the JMX interface do not provide any
 * authentication mechanism. If you run JaCoCo on production systems make sure that no untrusted sources have access to
 * the TCP server port, or JaCoCo TCP clients only connect to trusted targets. Otherwise internal information of the
 * application might be revealed or DOS attacks are possible." - eclemma.org
 *
 * INSTRUCTIONS:
 * To configure jacoco on a standalone integration test server:
 * 1. extract jacocoagent.jar from org.jacoco:org.jacoco.agent:jar:$VERSION to /path/to/jacocoagent.jar
 * e.g.
 * {@code jar -xf ~/.m2/repository/org/jacoco/org.jacoco.agent/$VERSION/org.jacoco.agent-$VERSION.jar -C /path/to jacocoagent.jar}
 * 2. add the following option to server's java args:
 * {@code -javaagent:/path/to/jacocoagent.jar=dumponexit=false,jmx=true}
 * 3. add additional jacoco option args as necessary (see: http://www.eclemma.org/jacoco/trunk/doc/agent.html)
 */
@SuppressWarnings("serial")
@Component(immediate = true, metatype = true)
public class JacocoServlet extends HttpServlet {
    private static final String JMX_NAME = "org.jacoco:type=Runtime";
    private static final String PARAM_SESSION_ID = ":sessionId";

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Property(value="/system/sling/jacoco.exec")
    static final String SERVLET_PATH_NAME = "servlet.path";

    /** Non-null if we are registered with HttpService */
    private String servletPath;

    @Reference
    private HttpService httpService;

    protected void activate(ComponentContext ctx) throws ServletException, NamespaceException {
        servletPath = getServletPath(ctx);
        if(servletPath == null) {
            log.info("Servlet path is null, not registering with HttpService");
        } else {
            httpService.registerServlet(servletPath, this, null, null);
            log.info("Servlet registered at {}", servletPath);
        }
    }

    /** Return the path at which to mount this servlet, or null
     *  if it must not be mounted.
     */
    protected String getServletPath(ComponentContext ctx) {
        final Dictionary<?, ?> config = ctx.getProperties();
        String result = (String)config.get(SERVLET_PATH_NAME);
        if(result != null && result.trim().length() == 0) {
            result = null;
        }
        return result;
    }

    protected void deactivate(ComponentContext ctx) throws ServletException, NamespaceException {
        if(servletPath != null) {
            httpService.unregister(servletPath);
            log.info("Servlet unregistered from path {}", servletPath);
        }
        servletPath = null;
    }

    /**
     * Get the jacoco execution data without resetting the agent
     * @param req the request
     * @param resp the response
     * @throws ServletException
     * @throws IOException
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        IAgent agent = getAgent();
        if (agent == null) {
            resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
        } else {
            resp.setContentType("application/octet-stream");
            byte[] data = agent.getExecutionData(false);
            resp.getOutputStream().write(data);
        }
    }

    /**
     * Get the jacoco execution data and reset the agent. Set the sessionId if :sessionId param exists.
     * @param req the request
     * @param resp the response
     * @throws ServletException
     * @throws IOException
     */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        IAgent agent = getAgent();
        if (agent == null) {
            resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
        } else {
            resp.setContentType("application/octet-stream");
            String sessionId = req.getParameter(PARAM_SESSION_ID);
            byte[] data = agent.getExecutionData(true);
            if (sessionId != null) {
                agent.setSessionId(sessionId);
            }
            resp.getOutputStream().write(data);
        }
    }

    /**
     * Lookup the jacoco agent mbean and return it if it exists. Return null otherwise.
     * @return jacoco agent MBean if registered, null if it is not registered
     */
    private IAgent getAgent() {
        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        try {
            ObjectName name = new ObjectName(JMX_NAME);
            if (mbs.isRegistered(name)) {
                return MBeanServerInvocationHandler.newProxyInstance(mbs, name, IAgent.class, false);
            }
        } catch (MalformedObjectNameException e) {
            log.error("[getAgent] there is a typo in the JMX_NAME constant", e);
        }

        return null;
    }
}
