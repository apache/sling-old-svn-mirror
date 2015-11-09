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
package org.apache.sling.discovery.base.its.setup;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.servlet.Servlet;

import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.discovery.InstanceDescription;
import org.apache.sling.discovery.PropertyProvider;
import org.apache.sling.discovery.TopologyEventListener;
import org.apache.sling.discovery.base.commons.BaseDiscoveryService;
import org.apache.sling.discovery.base.commons.ClusterViewService;
import org.apache.sling.discovery.base.commons.UndefinedClusterViewException;
import org.apache.sling.discovery.base.commons.ViewChecker;
import org.apache.sling.discovery.base.connectors.announcement.AnnouncementRegistry;
import org.apache.sling.discovery.base.connectors.ping.ConnectorRegistry;
import org.apache.sling.discovery.base.connectors.ping.TopologyConnectorClientInformation;
import org.apache.sling.discovery.base.connectors.ping.TopologyConnectorServlet;
import org.apache.sling.discovery.base.its.setup.mock.ArtificialDelay;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.nio.SelectChannelConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.osgi.framework.Constants;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.HttpService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import junitx.util.PrivateAccessor;

public class VirtualInstance {

    protected final static Logger logger = LoggerFactory.getLogger(VirtualInstance.class);

    public final String slingId;

    ClusterViewService clusterViewService;

    private final ResourceResolverFactory resourceResolverFactory;

    private final OSGiMock osgiMock;

    private final BaseDiscoveryService discoveryService;

    private final AnnouncementRegistry announcementRegistry;

    private final ConnectorRegistry connectorRegistry;

    protected final String debugName;

    private ResourceResolver resourceResolver;

    private int serviceId = 999;

    private ViewCheckerRunner viewCheckerRunner = null;

    private ServletContextHandler servletContext;

    private Server jettyServer;

    private ModifiableTestBaseConfig config;

    private ViewChecker viewChecker;

    private final VirtualInstanceBuilder builder;

    private final ArtificialDelay delay;

    private class ViewCheckerRunner implements Runnable {

    	private final int intervalInSeconds;

    	private boolean stopping_ = false;

        private volatile boolean stopped_ = false;

        public ViewCheckerRunner(int intervalInSeconds) {
    		this.intervalInSeconds = intervalInSeconds;
    	}

		public synchronized void stop() {
			logger.info("Stopping Instance ["+slingId+"]");
			stopping_ = true;
			this.notifyAll();
		}
		
		public boolean hasStopped() {
		    return stopped_;
		}

		public void run() {
		    try{
		        doRun();
		    } finally {
		        stopped_ = true;
                logger.info("Instance ["+slingId+"] stopped.");
		    }
		}
		
		public void doRun() {
			while(true) {
				synchronized(this) {
					if (stopping_) {
						logger.info("Instance ["+slingId+"] stopps.");
						return;
					}
				}
				try{
				    heartbeatsAndCheckView();
				} catch(Exception e) {
				    logger.error("run: ping connector for slingId="+slingId+" threw exception: "+e, e);
				}
				synchronized(this) {
    				try {
    					this.wait(intervalInSeconds*1000);
    				} catch (InterruptedException e) {
    					e.printStackTrace();
    					return;
    				}
				}
			}
		}

    }
    
    public VirtualInstance(VirtualInstanceBuilder builder) throws Exception {
        this.builder = builder;
    	this.slingId = builder.getSlingId();
        this.debugName = builder.getDebugName();
        this.delay = builder.getDelay();
        logger.info("<init>: starting slingId="+slingId+", debugName="+debugName);

        osgiMock = new OSGiMock();

        this.resourceResolverFactory = builder.getResourceResolverFactory();

        config = builder.getConnectorConfig();
        config.addTopologyConnectorWhitelistEntry("127.0.0.1");
        config.setMinEventDelay(builder.getMinEventDelay());

        clusterViewService = builder.getClusterViewService();
        announcementRegistry = builder.getAnnouncementRegistry();
        connectorRegistry = builder.getConnectorRegistry();
        viewChecker = builder.getViewChecker();
		discoveryService = builder.getDiscoverService();

        osgiMock.addService(clusterViewService);
        osgiMock.addService(announcementRegistry);
        osgiMock.addService(connectorRegistry);
        osgiMock.addService(viewChecker);
        osgiMock.addService(discoveryService);
        osgiMock.addServices(builder.getAdditionalServices(this));

        resourceResolver = resourceResolverFactory
                .getAdministrativeResourceResolver(null);

        if (builder.isResetRepo()) {
            //SLING-4587 : do resetRepo before creating the observationListener
            // otherwise it will get tons of events from the deletion of /var
            // which the previous test could have left over.
            // Doing it before addEventListener should prevent that.
            builder.resetRepo();
        }

        osgiMock.activateAll();
    }
    
    public void setDelay(String operationDescriptor, long delayMillis) {
        delay.setDelay(operationDescriptor, delayMillis);
    }
    
    @Override
    public String toString() {
        return "a [Test]Instance[slingId="+slingId+", debugName="+debugName+"]";
    }

    public void bindPropertyProvider(PropertyProvider propertyProvider,
            String... propertyNames) throws Throwable {
        Map<String, Object> props = new HashMap<String, Object>();
        props.put(Constants.SERVICE_ID, (long) serviceId++);
        props.put(PropertyProvider.PROPERTY_PROPERTIES, propertyNames);

        PrivateAccessor.invoke(discoveryService, "bindPropertyProvider",
                new Class[] { PropertyProvider.class, Map.class },
                new Object[] { propertyProvider, props });
    }

    public String getSlingId() {
        return slingId;
    }

    public ClusterViewService getClusterViewService() {
        return clusterViewService;
    }

    public BaseDiscoveryService getDiscoveryService() {
        return discoveryService;
    }

    public AnnouncementRegistry getAnnouncementRegistry() {
        return announcementRegistry;
    }

    public synchronized void startJetty() throws Throwable {
        if (jettyServer!=null) {
            return;
        }
        servletContext = new ServletContextHandler(ServletContextHandler.NO_SECURITY);
        servletContext.setContextPath("/");

        TopologyConnectorServlet servlet = new TopologyConnectorServlet();
        PrivateAccessor.setField(servlet, "config", config);
        PrivateAccessor.setField(servlet, "clusterViewService", clusterViewService);
        PrivateAccessor.setField(servlet, "announcementRegistry", announcementRegistry);

        Mockery context = new JUnit4Mockery();
        final HttpService httpService = context.mock(HttpService.class);
        context.checking(new Expectations() {
            {
                allowing(httpService).registerServlet(with(any(String.class)),
                        with(any(Servlet.class)),
                        with(any(Dictionary.class)),
                        with(any(HttpContext.class)));
            }
        });
        PrivateAccessor.setField(servlet, "httpService", httpService);
        ComponentContext cc = null;
        PrivateAccessor.invoke(servlet, "activate", new Class[] {ComponentContext.class}, new Object[] {cc});

        ServletHolder holder =
                new ServletHolder(servlet);

        servletContext.addServlet(holder, "/system/console/topology/*");

        jettyServer = new Server();
        jettyServer.setHandler(servletContext);
        Connector connector=new SelectChannelConnector();
        jettyServer.setConnectors(new Connector[]{connector});
        jettyServer.start();
    }

    public synchronized int getJettyPort() {
        if (jettyServer==null) {
            throw new IllegalStateException("jettyServer not started");
        }
        final Connector[] connectors = jettyServer.getConnectors();
        return connectors[0].getLocalPort();
    }

    public TopologyConnectorClientInformation connectTo(String url) throws MalformedURLException {
        return connectorRegistry.registerOutgoingConnector(clusterViewService, new URL(url));
    }

    public InstanceDescription getLocalInstanceDescription() throws UndefinedClusterViewException {
    	final Iterator<InstanceDescription> it = getClusterViewService().getLocalClusterView().getInstances().iterator();
    	while(it.hasNext()) {
    		final InstanceDescription id = it.next();
    		if (slingId.equals(id.getSlingId())) {
    			return id;
    		}
    	}
    	fail("no local instanceDescription found");
    	// never called:
    	return null;
    }

    public void heartbeatsAndCheckView() {
    	logger.info("Instance ["+slingId+"] issues a pulse now "+new Date());
        viewChecker.heartbeatAndCheckView();
    }

    public void startViewChecker(int intervalInSeconds) throws IllegalAccessException, InvocationTargetException {
    	logger.info("startViewChecker: intervalInSeconds="+intervalInSeconds);
    	if (viewCheckerRunner!=null) {
    		logger.info("startViewChecker: stopping first...");
    		viewCheckerRunner.stop();
    		logger.info("startViewChecker: stopped.");
    	}
		logger.info("startViewChecker: activating...");
    	try{
    		OSGiMock.activate(viewChecker);
    	} catch(Error er) {
    		er.printStackTrace(System.out);
    		throw er;
    	} catch(RuntimeException re) {
    		re.printStackTrace(System.out);
    	}
		logger.info("startViewChecker: initializing...");
    	viewCheckerRunner = new ViewCheckerRunner(intervalInSeconds);
    	Thread th = new Thread(viewCheckerRunner, "Test-ViewCheckerRunner ["+debugName+"]");
    	th.setDaemon(true);
		logger.info("startViewChecker: starting thread...");
    	th.start();
		logger.info("startViewChecker: done.");
    }

	public boolean isViewCheckerRunning() {
		return (viewCheckerRunner!=null);
	}

    public void stopViewChecker() throws Throwable {
    	if (viewCheckerRunner!=null) {
    		viewCheckerRunner.stop();
    		while(!viewCheckerRunner.hasStopped()) {
    		    logger.info("stopViewChecker: ["+getDebugName()+"] waiting for viewCheckerRunner to stop");
    		    Thread.sleep(500);
    		}
            logger.info("stopViewChecker: ["+getDebugName()+"] viewCheckerRunner stopped");
    		viewCheckerRunner = null;
    	}
        try{
            OSGiMock.deactivate(viewChecker);
        } catch(Error er) {
            er.printStackTrace(System.out);
            throw er;
        } catch(RuntimeException re) {
            re.printStackTrace(System.out);
            throw re;
        }
    }

    public void dumpRepo() throws Exception {
        VirtualInstanceHelper.dumpRepo(resourceResolverFactory);
    }
    
    public ResourceResolverFactory getResourceResolverFactory() {
        return resourceResolverFactory;
    }

    public void stop() throws Exception {
        logger.info("stop: stopping slingId="+slingId+", debugName="+debugName);
        try {
            stopViewChecker();
        } catch (Throwable e) {
            throw new Exception("Caught Throwable in stop(): "+e, e);
        }

        if (resourceResolver != null) {
            resourceResolver.close();
        }
        osgiMock.deactivateAll();
        logger.info("stop: stopped slingId="+slingId+", debugName="+debugName);
    }

    public void bindTopologyEventListener(TopologyEventListener eventListener)
            throws Throwable {
        PrivateAccessor.invoke(discoveryService, "bindTopologyEventListener",
                new Class[] { TopologyEventListener.class },
                new Object[] { eventListener });
    }

    public ModifiableTestBaseConfig getConfig() {
        return config;
    }

    public ViewChecker getViewChecker() {
        return viewChecker;
    }

    public void assertEstablishedView() {
        assertTrue(getDiscoveryService().getTopology().isCurrent());
    }

    public VirtualInstanceBuilder getBuilder() {
        return builder;
    }

    public String getDebugName() {
        return debugName;
    }

}
