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
package org.apache.sling.testing.mock.sling.context;

import org.apache.sling.testing.mock.sling.context.models.OsgiServiceModel;
import org.apache.sling.testing.mock.sling.context.models.RequestAttributeModel;
import org.apache.sling.testing.mock.sling.context.models.ServiceInterfaceImpl;
import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.junit.Before;
import org.junit.Rule;

public class ModelAdapterFactoryUtilClassesTest extends AbstractModelAdapterFactoryUtilTest {

    @Rule
    public SlingContext context = new SlingContext();
    
    @Override
    protected SlingContext context() {
        return context;
    }

    @Before
    public void setUp() throws Exception {
        // add Model classes individually
        // although this is not supported before Sling Models Impl 1.3.4 in mocks this works even with older sling models versions
        // because the class path scanning is implemented differently
        context.addModelsForClasses(
                OsgiServiceModel.class.getName()
                );
        context.addModelsForClasses(
                RequestAttributeModel.class,
                ServiceInterfaceImpl.class
                );
    }

}
