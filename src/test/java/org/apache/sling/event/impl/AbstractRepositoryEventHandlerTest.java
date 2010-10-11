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
package org.apache.sling.event.impl;

import java.util.Dictionary;

import org.jmock.Expectations;
import org.jmock.integration.junit4.JMock;
import org.junit.runner.RunWith;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.event.EventAdmin;

@RunWith(JMock.class)
public abstract class AbstractRepositoryEventHandlerTest extends AbstractTest {

    protected volatile AbstractRepositoryEventHandler handler;

    protected abstract AbstractRepositoryEventHandler createHandler();

    protected void activate(final EventAdmin ea) throws Throwable {
        super.activate(ea);
        this.handler = this.createHandler();

        handler.environment = this.environment;

        // lets set up the bundle context
        final BundleContext bundleContext = this.getMockery().mock(BundleContext.class, "beforeBundleContext" + activateCount);

        // lets set up the component configuration
        final Dictionary<String, Object> componentConfig = this.getComponentConfig();

        // lets set up the compnent context
        final ComponentContext componentContext = this.getMockery().mock(ComponentContext.class, "beforeComponentContext" + activateCount);
        this.getMockery().checking(new Expectations() {{
            allowing(componentContext).getBundleContext();
            will(returnValue(bundleContext));
            allowing(componentContext).getProperties();
            will(returnValue(componentConfig));
        }});

        this.handler.activate(componentContext);

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
        this.handler.deactivate(componentContext);
        this.handler = null;
        super.deactivate();
    }
}
