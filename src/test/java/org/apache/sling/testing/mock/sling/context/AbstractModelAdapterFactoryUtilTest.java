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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.apache.sling.commons.mime.MimeTypeService;
import org.apache.sling.testing.mock.sling.context.models.OsgiServiceModel;
import org.apache.sling.testing.mock.sling.context.models.RequestAttributeModel;
import org.apache.sling.testing.mock.sling.context.models.ServiceInterface;
import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.apache.sling.testing.mock.sling.services.MockMimeTypeService;
import org.junit.Test;

public abstract class AbstractModelAdapterFactoryUtilTest {
    
    protected abstract SlingContext context();

    @Test
    public void testRequestAttribute() {
        context().request().setAttribute("prop1", "myValue");
        RequestAttributeModel model = context().request().adaptTo(RequestAttributeModel.class);
        assertNotNull(model);
        assertEquals("myValue", model.getProp1());
    }

    @Test
    public void testOsgiService() {
        context().registerService(MimeTypeService.class, new MockMimeTypeService());

        OsgiServiceModel model = context().resourceResolver().adaptTo(OsgiServiceModel.class);
        assertNotNull(model);
        assertNotNull(model.getMimeTypeService());
        assertEquals("text/html", model.getMimeTypeService().getMimeType("html"));
    }

    @Test
    public void testInvalidAdapt() {
        OsgiServiceModel model = context().request().adaptTo(OsgiServiceModel.class);
        assertNull(model);
    }

    @Test
    public void testAdaptToInterface() {
        context().request().setAttribute("prop1", "myValue");
        ServiceInterface model = context().request().adaptTo(ServiceInterface.class);
        assertNotNull(model);
        assertEquals("myValue", model.getPropValue());
    }

}
