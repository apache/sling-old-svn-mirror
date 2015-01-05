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
package org.apache.sling.installer.it;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.sling.installer.api.InstallableResource;
import org.apache.sling.installer.api.UpdateHandler;
import org.apache.sling.installer.api.UpdateResult;
import org.apache.sling.installer.api.event.InstallationEvent;
import org.apache.sling.installer.api.event.InstallationListener;
import org.apache.sling.installer.api.tasks.ChangeStateTask;
import org.apache.sling.installer.api.tasks.InstallTask;
import org.apache.sling.installer.api.tasks.InstallTaskFactory;
import org.apache.sling.installer.api.tasks.RegisteredResource;
import org.apache.sling.installer.api.tasks.ResourceState;
import org.apache.sling.installer.api.tasks.ResourceTransformer;
import org.apache.sling.installer.api.tasks.TaskResource;
import org.apache.sling.installer.api.tasks.TaskResourceGroup;
import org.apache.sling.installer.api.tasks.TransformationResult;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;

@RunWith(PaxExam.class)

public class UpdateHandlerTest extends OsgiInstallerTestBase {

    private static final String TYPE = "special";

    private final List<ServiceRegistration<?>> serviceRegistrations = new ArrayList<ServiceRegistration<?>>();

    private final Map<String, Dictionary<String, Object>> installed = new HashMap<String, Dictionary<String,Object>>();

    @org.ops4j.pax.exam.Configuration
    public Option[] config() {
        return defaultConfiguration();
    }

    @Before
    public void setUp() {
        setupInstaller();
        serviceRegistrations.clear();

        final Dictionary<String, Object> props = new Hashtable<String, Object>();
        props.put(Constants.SERVICE_RANKING, 1000);

        serviceRegistrations.add(this.bundleContext.registerService(ResourceTransformer.class,
            new ResourceTransformer() {

                public TransformationResult[] transform(final RegisteredResource resource) {
                    final int lastDot = resource.getURL().lastIndexOf('.');
                    final int lastSlash = resource.getURL().lastIndexOf('/');
                    if ( resource.getURL().substring(lastDot + 1).equals(TYPE) ) {
                        final String id = resource.getURL().substring(lastSlash + 1, lastDot);
                        final TransformationResult tr = new TransformationResult();
                        tr.setId(id);
                        tr.setResourceType(TYPE);

                        return new TransformationResult[] {tr};
                    }
                    return null;
                }
            }, props));
        serviceRegistrations.add(this.bundleContext.registerService(InstallTaskFactory.class,
            new InstallTaskFactory() {

                public InstallTask createTask(final TaskResourceGroup toActivate) {
                    final TaskResource tr = toActivate.getActiveResource();
                    if ( tr != null && tr.getEntityId().startsWith(TYPE) ) {
                        if ( tr.getState() == ResourceState.INSTALL ) {
                            installed.put(tr.getEntityId(), tr.getDictionary());
                            return new ChangeStateTask(toActivate, ResourceState.INSTALLED);
                        } else {
                            installed.remove(tr.getEntityId());
                            return new ChangeStateTask(toActivate, ResourceState.UNINSTALLED);
                        }
                    }
                    return null;
                }
            }, props));
    }

    @Override
    @After
    public void tearDown() {
        for(final ServiceRegistration<?> reg : this.serviceRegistrations) {
            reg.unregister();
        }
        this.serviceRegistrations.clear();
        super.tearDown();
    }

    private static final class UpdateHandlerImpl implements UpdateHandler {

        private final Barrier barrier = new Barrier(2);

        private volatile UpdateResult result;

        public UpdateResult handleUpdate(String resourceType, String id,
                String url, InputStream is, Map<String, Object> attributes) {
            // we only test dictionaries
            return null;
        }

        public UpdateResult handleUpdate(final String resourceType,
                final String id,
                final String url,
                final Dictionary<String, Object> dict,
                final Map<String, Object> attributes) {
            if ( resourceType.equals(TYPE)) {
                final UpdateResult ur = new UpdateResult(TYPE + ":/resource/b/" + id + "." + resourceType);
                ur.setPriority(InstallableResource.DEFAULT_PRIORITY * 2);
                this.result = ur;
                this.barrier.block();

                return ur;
            }
            return null;
        }

        public UpdateResult handleRemoval(final String resourceType,
                final String id,
                final String url) {
            final UpdateResult ur = new UpdateResult(url);
            this.result = ur;
            this.barrier.block();

            return ur;
        }

        public UpdateResult waitForUpdate() {
            barrier.block();

            final UpdateResult r = this.result;
            this.result = null;
            barrier.reset();
            return r;
        }
    };

    private Barrier getInstallerListenerBarrier() {
        final Barrier b = new Barrier(2);
        final InstallationListener il = new InstallationListener() {
            public void onEvent(final InstallationEvent event) {
                if ( event.getType() == InstallationEvent.TYPE.PROCESSED ) {
                    b.block();
                }
            }
        };
        b.reg = bundleContext.registerService(InstallationListener.class.getName(), il, null);
        return b;
    }

    @Test
    public void testSimpleUpdate() throws Exception {
        final UpdateHandlerImpl up = new UpdateHandlerImpl();
        final Dictionary<String, Object> dict = new Hashtable<String, Object>();
        dict.put(UpdateHandler.PROPERTY_SCHEMES, TYPE);

        this.serviceRegistrations.add(this.bundleContext.registerService(UpdateHandler.class, up, dict));

        final Dictionary<String, Object> data = new Hashtable<String, Object>();
        data.put("foo", "bar");

        final InstallableResource ir = new InstallableResource("/resource/a." + TYPE,
                null, data, null, InstallableResource.TYPE_PROPERTIES, null);

        final Barrier b = this.getInstallerListenerBarrier();
        this.installer.registerResources(TYPE, new InstallableResource[] {ir});
        b.block();
        b.reg.unregister();

        assertNotNull("Resource should be installed: " + installed, installed.get(TYPE) + ":a");

        final Dictionary<String, Object> newData = new Hashtable<String, Object>();
        data.put("bar", "foo");
        this.resourceChangeListener.resourceAddedOrUpdated(TYPE, "a", null, newData, null);

        final UpdateResult ur = up.waitForUpdate();
        assertNotNull(ur);
        assertEquals(TYPE + ":/resource/b/a." + TYPE, ur.getURL());

        this.resourceChangeListener.resourceRemoved(TYPE, "a");
        final UpdateResult r2 = up.waitForUpdate();
        assertNotNull(r2);
        assertEquals(TYPE + ":/resource/b/a." + TYPE, r2.getURL());
    }

    /** Simplified version of the cyclic barrier class for testing. */
    public static final class Barrier extends CyclicBarrier {

        public Barrier(final int parties) {
            super(parties);
        }

        public void block() {
            try {
                this.await();
            } catch (InterruptedException e) {
                // ignore
            } catch (BrokenBarrierException e) {
                // ignore
            }
        }

        public boolean block(int seconds) {
            try {
                this.await(seconds, TimeUnit.SECONDS);
                return true;
            } catch (InterruptedException e) {
                // ignore
            } catch (BrokenBarrierException e) {
                // ignore
            } catch (TimeoutException e) {
                // ignore
            }
            this.reset();
            return false;
        }

        public ServiceRegistration<?> reg;
    }
}
