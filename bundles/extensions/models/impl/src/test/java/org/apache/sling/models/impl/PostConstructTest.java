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
package org.apache.sling.models.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.util.Hashtable;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.factory.PostConstructException;
import org.apache.sling.models.testmodels.classes.FailingPostConstuctModel;
import org.apache.sling.models.testmodels.classes.SubClass;
import org.apache.sling.models.testmodels.classes.SubClassOverriddenPostConstruct;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;

@RunWith(MockitoJUnitRunner.class)
public class PostConstructTest {

    @Mock
    private ComponentContext componentCtx;

    @Mock
    private BundleContext bundleContext;

    @Mock
    private Resource resource;

    ModelAdapterFactory factory = new ModelAdapterFactory();

    @Before
    public void setup() {
        when(componentCtx.getBundleContext()).thenReturn(bundleContext);
        when(componentCtx.getProperties()).thenReturn(new Hashtable<String, Object>());
        factory.activate(componentCtx);
        // no injectors are necessary
        factory.adapterImplementations.addClassesAsAdapterAndImplementation(SubClass.class, SubClassOverriddenPostConstruct.class, FailingPostConstuctModel.class);
    }

    @Test
    public void testClassOrder() {
        SubClass sc = factory.getAdapter(resource, SubClass.class);
        assertTrue(sc.getPostConstructCalledTimestampInSub() > sc.getPostConstructCalledTimestampInSuper());
        assertTrue(sc.getPostConstructCalledTimestampInSuper() > 0);
    }

    @Test
    public void testOverriddenPostConstruct() {
        SubClassOverriddenPostConstruct sc = factory.getAdapter(resource, SubClassOverriddenPostConstruct.class);
        assertEquals("Post construct not called exactly one time in sub class!", 1, sc.getPostConstructorCalledCounter());
        assertEquals("Post construct was called on super class although overridden in sub class", 0, sc.getPostConstructCalledTimestampInSuper());
    }

    @Test
    public void testPostConstructMethodWhichThrowsException() {
        FailingPostConstuctModel model = factory.getAdapter(resource, FailingPostConstuctModel.class);
        assertNull(model);
    }

    @Test
    public void testPostConstructMethodWhichThrowsExceptionThrowingException() {
        boolean thrown = false;
        try {
            factory.createModel(resource, FailingPostConstuctModel.class);
        } catch (PostConstructException e) {
            assertTrue(e.getMessage().contains("Post-construct"));
            assertEquals("FAIL", e.getCause().getMessage());
            thrown = true;
        }
        assertTrue(thrown);
    }
}
