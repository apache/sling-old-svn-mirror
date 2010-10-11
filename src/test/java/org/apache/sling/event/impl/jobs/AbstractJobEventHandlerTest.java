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
package org.apache.sling.event.impl.jobs;

import java.util.Hashtable;

import junitx.util.PrivateAccessor;

import org.apache.sling.event.impl.AbstractTest;
import org.apache.sling.event.impl.SimpleScheduler;
import org.apache.sling.event.impl.jobs.config.QueueConfigurationManager;
import org.apache.sling.event.impl.jobs.jcr.PersistenceHandler;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JMock;
import org.junit.runner.RunWith;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceListener;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.event.EventAdmin;

@RunWith(JMock.class)
public abstract class AbstractJobEventHandlerTest extends AbstractTest {

    protected volatile PersistenceHandler handler;

    protected volatile DefaultJobManager jobManager;

    protected volatile QueueConfigurationManager configManager;

    protected void activate(final EventAdmin ea) throws Throwable {
        super.activate(ea);
        this.jobManager = new DefaultJobManager();
        this.configManager = new QueueConfigurationManager();
        PrivateAccessor.setField(this.jobManager, "configManager", this.configManager);
        PrivateAccessor.setField(this.jobManager, "environment", this.environment);
        PrivateAccessor.setField(this.jobManager, "scheduler", new SimpleScheduler());
        this.handler = new PersistenceHandler();
        PrivateAccessor.setField(this.handler, "environment", this.environment);
        PrivateAccessor.setField(this.handler, "jobManager", this.jobManager);

        // lets set up the bundle context
        final BundleContext bundleContext = this.getMockery().mock(BundleContext.class, "beforeBundleContext" + activateCount);

        // lets set up the component configuration
        final Hashtable<String, Object> componentConfig = this.getComponentConfig();

        // lets set up the compnent context
        final ComponentContext componentContext = this.getMockery().mock(ComponentContext.class, "beforeComponentContext" + activateCount);
        this.getMockery().checking(new Expectations() {{
            allowing(componentContext).getBundleContext();
            will(returnValue(bundleContext));
            allowing(componentContext).getProperties();
            will(returnValue(componentConfig));
            allowing(bundleContext).createFilter(with(any(String.class)));
            allowing(bundleContext).addServiceListener(with(any(ServiceListener.class)));
            allowing(bundleContext).addServiceListener(with(any(ServiceListener.class)), with(any(String.class)));
            allowing(bundleContext).removeServiceListener(with(any(ServiceListener.class)));
            allowing(bundleContext).getServiceReferences(with(any(String.class)), with(aNull(String.class)));
            allowing(bundleContext).getServiceReferences(with(aNull(String.class)), with(aNull(String.class)));
            allowing(bundleContext).getServiceReferences(with(any(String.class)), with(any(String.class)));
            allowing(bundleContext).getServiceReferences(with(aNull(String.class)), with(any(String.class)));
        }});

        PrivateAccessor.invoke(this.handler, "activate", new Class[] {ComponentContext.class}, new Object[] {componentContext});
        this.jobManager.activate(componentConfig);
        PrivateAccessor.invoke(this.configManager, "activate", new Class[] {BundleContext.class}, new Object[] {bundleContext});

        // the session is initialized in the background, so let's sleep some seconds
        try {
            Thread.sleep(2 * 1000);
        } catch (InterruptedException e) {
            // ignore
        }
    }


    protected void deactivate() throws Throwable {
        // lets set up the bundle context with the sling id
        final BundleContext bundleContext = this.getMockery().mock(BundleContext.class, "afterBundleContext" + activateCount);

        final ComponentContext componentContext = this.getMockery().mock(ComponentContext.class, "afterComponentContext" + activateCount);
        this.getMockery().checking(new Expectations() {{
            allowing(componentContext).getBundleContext();
            will(returnValue(bundleContext));
        }});
        PrivateAccessor.invoke(this.configManager, "deactivate", null, null);
        PrivateAccessor.invoke(this.handler, "deactivate", new Class[] {ComponentContext.class}, new Object[] {componentContext});
        this.handler = null;
        this.jobManager.deactivate();
        this.jobManager = null;
        this.configManager = null;
        super.deactivate();
    }
}
