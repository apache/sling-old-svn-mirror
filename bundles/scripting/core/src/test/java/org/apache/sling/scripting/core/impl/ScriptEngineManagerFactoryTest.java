/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.sling.scripting.core.impl;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Dictionary;

import javax.script.ScriptEngineManager;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JMock;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleListener;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentContext;

/**
 * Test of the ScriptEngineManagerFactory.
 */
@RunWith(JMock.class)
public class ScriptEngineManagerFactoryTest {

    private static Class<?> SCRIPT_ENGINE_FACTORY = DummyScriptEngineFactory.class;

    private Mockery context = new JUnit4Mockery();

    private ComponentContext componentCtx;

    private BundleContext bundleCtx;

    @Before
    public void setup() throws Exception {
        componentCtx = context.mock(ComponentContext.class);
        bundleCtx = context.mock(BundleContext.class);
        context.checking(new Expectations(){{
            atLeast(1).of(componentCtx).getBundleContext();
            will(returnValue(bundleCtx));

            allowing(bundleCtx).createFilter(with(any(String.class)));
            allowing(bundleCtx).addServiceListener(with(any(ServiceListener.class)));
            allowing(bundleCtx).addServiceListener(with(any(ServiceListener.class)), with(any(String.class)));
            allowing(bundleCtx).getServiceReferences(with(any(String.class)), with(aNull(String.class)));
            allowing(bundleCtx).getServiceReferences(with(aNull(String.class)), with(aNull(String.class)));
            allowing(bundleCtx).getServiceReferences(with(any(String.class)), with(any(String.class)));
            allowing(bundleCtx).getServiceReferences(with(aNull(String.class)), with(any(String.class)));

            one(bundleCtx).addBundleListener(with(any(BundleListener.class)));
            one(bundleCtx).getBundles();
            will(returnValue(new Bundle[0]));

            allowing(bundleCtx).registerService(with(equal("org.apache.sling.scripting.core.impl.ScriptEngineConsolePlugin")), with(any(Object.class)), with(any(Dictionary.class)));
            will(returnValue(new MockServiceRegistration()));


        }});
    }


    @Test
    public void checkNonNullManagerAfterActivate() throws Exception  {
        context.checking(new Expectations(){{
            one(bundleCtx).registerService(with(equal(new String[] {"javax.script.ScriptEngineManager", "org.apache.sling.scripting.core.impl.helper.SlingScriptEngineManager"})), with(any(Object.class)), with(any(Dictionary.class)));
            will(returnValue(new MockServiceRegistration()));
        }});

        ScriptEngineManagerFactory factory = new ScriptEngineManagerFactory();
        factory.activate(componentCtx);

        assertNotNull(factory.getScriptEngineManager());
    }

    @Test
    public void checkAddingScriptBundle() throws Exception {
        context.checking(new Expectations(){{
            exactly(1).of(bundleCtx).registerService(with(equal(new String[] {"javax.script.ScriptEngineManager", "org.apache.sling.scripting.core.impl.helper.SlingScriptEngineManager"})), with(any(Object.class)), with(any(Dictionary.class)));
            will(returnValue(new MockServiceRegistration()));
        }});

        ScriptEngineManagerFactory factory = new ScriptEngineManagerFactory();
        factory.activate(componentCtx);

        ScriptEngineManager first = factory.getScriptEngineManager();

        assertNull(first.getEngineByName("dummy"));

        final Bundle bundle  = context.mock(Bundle.class);

        final File factoryFile = createFactoryFile();

        context.checking(new Expectations() {{

            atLeast(1).of(bundle).getEntry("META-INF/services/javax.script.ScriptEngineFactory");
            will(returnValue(factoryFile.toURI().toURL()));

            atLeast(1).of(bundle).loadClass(SCRIPT_ENGINE_FACTORY.getName());
            will(returnValue(SCRIPT_ENGINE_FACTORY));
        }});

        factory.bundleChanged(new BundleEvent(BundleEvent.STARTED, bundle));

        ScriptEngineManager second = factory.getScriptEngineManager();

        assertNotNull(second.getEngineByName("dummy"));
    }

    private File createFactoryFile() throws IOException {
        File tempFile = File.createTempFile("scriptEngine", "tmp");
        tempFile.deleteOnExit();

        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(tempFile);
            fos.write("#I'am a test-comment\n".getBytes());
            fos.write(SCRIPT_ENGINE_FACTORY.getName().getBytes());
        } finally {
            if (fos != null) {
                fos.close();
            }
        }
        return tempFile;
    }

    private class MockServiceRegistration implements ServiceRegistration {

        public ServiceReference getReference() {
            throw new UnsupportedOperationException();
        }

        @SuppressWarnings("unchecked")
        public void setProperties(Dictionary properties) {
            // NO-OP
        }

        public void unregister() {
            // NO-OP
        }

    }

}
