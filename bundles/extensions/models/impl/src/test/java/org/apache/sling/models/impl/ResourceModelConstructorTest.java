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

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.Hashtable;

import org.apache.commons.lang.RandomStringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.apache.sling.models.impl.ParameterizedTypeFromRequestAttributeTest.TestModel;
import org.apache.sling.models.impl.injectors.ChildResourceInjector;
import org.apache.sling.models.impl.injectors.ValueMapInjector;
import org.apache.sling.models.testmodels.classes.ChildModel;
import org.apache.sling.models.testmodels.classes.constructorinjection.ParentModel;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;

@RunWith(MockitoJUnitRunner.class)
public class ResourceModelConstructorTest {

    @Mock
    private ComponentContext componentCtx;

    @Mock
    private BundleContext bundleContext;

    private ModelAdapterFactory factory;

    @Before
    public void setup() {
        when(componentCtx.getBundleContext()).thenReturn(bundleContext);
        when(componentCtx.getProperties()).thenReturn(new Hashtable<String, Object>());

        factory = new ModelAdapterFactory();
        factory.activate(componentCtx);
        factory.bindInjector(new ValueMapInjector(), new ServicePropertiesMap(2, 2));
        factory.bindInjector(new ChildResourceInjector(), new ServicePropertiesMap(1, 1));
        factory.adapterImplementations.addClassesAsAdapterAndImplementation(ParentModel.class, ChildModel.class);
    }

    @Test
    public void testChildModel() {
        Object firstValue = RandomStringUtils.randomAlphabetic(10);
        ValueMap firstMap = new ValueMapDecorator(Collections.singletonMap("property", firstValue));

        final Resource firstChild = mock(Resource.class);
        when(firstChild.adaptTo(ValueMap.class)).thenReturn(firstMap);
        when(firstChild.adaptTo(ChildModel.class)).thenAnswer(new AdaptToChildModel());

        Object firstGrandChildValue = RandomStringUtils.randomAlphabetic(10);
        ValueMap firstGrandChildMap = new ValueMapDecorator(Collections.singletonMap("property", firstGrandChildValue));
        Object secondGrandChildValue = RandomStringUtils.randomAlphabetic(10);
        ValueMap secondGrandChildMap = new ValueMapDecorator(Collections.singletonMap("property", secondGrandChildValue));

        final Resource firstGrandChild = mock(Resource.class);
        when(firstGrandChild.adaptTo(ValueMap.class)).thenReturn(firstGrandChildMap);
        when(firstGrandChild.adaptTo(ChildModel.class)).thenAnswer(new AdaptToChildModel());

        final Resource secondGrandChild = mock(Resource.class);
        when(secondGrandChild.adaptTo(ValueMap.class)).thenReturn(secondGrandChildMap);
        when(secondGrandChild.adaptTo(ChildModel.class)).thenAnswer(new AdaptToChildModel());

        Resource secondChild = mock(Resource.class);
        when(secondChild.listChildren()).thenReturn(Arrays.asList(firstGrandChild, secondGrandChild).iterator());

        Resource emptyChild = mock(Resource.class);
        when(emptyChild.listChildren()).thenReturn(Collections.<Resource>emptySet().iterator());

        Resource res = mock(Resource.class);
        when(res.getChild("firstChild")).thenReturn(firstChild);
        when(res.getChild("secondChild")).thenReturn(secondChild);
        when(res.getChild("emptyChild")).thenReturn(emptyChild);

        ParentModel model = factory.getAdapter(res, ParentModel.class);
        assertNotNull(model);

        ChildModel childModel = model.getFirstChild();
        assertNotNull(childModel);
        assertEquals(firstValue, childModel.getProperty());
        assertEquals(2, model.getGrandChildren().size());
        assertEquals(firstGrandChildValue, model.getGrandChildren().get(0).getProperty());
        assertEquals(secondGrandChildValue, model.getGrandChildren().get(1).getProperty());
        assertEquals(0, model.getEmptyGrandChildren().size());
    }

    private class AdaptToChildModel implements Answer<ChildModel> {

        @Override
        public ChildModel answer(InvocationOnMock invocation) throws Throwable {
            return factory.getAdapter(invocation.getMock(), ChildModel.class);
        }
    }

}
