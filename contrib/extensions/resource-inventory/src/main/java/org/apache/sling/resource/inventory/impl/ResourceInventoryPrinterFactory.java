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
package org.apache.sling.resource.inventory.impl;

import java.io.PrintWriter;
import java.util.Map;

import org.apache.felix.inventory.Format;
import org.apache.felix.inventory.InventoryPrinter;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.commons.json.JSONException;

@Component(configurationFactory=true, policy=ConfigurationPolicy.REQUIRE, metatype=true)
@Service(value=InventoryPrinter.class)
@Properties({
    @Property(name=InventoryPrinter.FORMAT, value="JSON", propertyPrivate=true),
    @Property(name=InventoryPrinter.NAME),
    @Property(name=InventoryPrinter.TITLE),
    @Property(name=InventoryPrinter.WEBCONSOLE, boolValue=false, propertyPrivate=true)
})
public class ResourceInventoryPrinterFactory implements InventoryPrinter {

    @Property(value = "")
    private static final String PROP_PATH = "path";

    private String path;

    @Activate
    protected void activate(final Map<String, Object> props) {
        this.path = (String)props.get(PROP_PATH);
    }

    @Reference
    private ResourceResolverFactory factory;

    /**
     * @see org.apache.felix.inventory.InventoryPrinter#print(java.io.PrintWriter, org.apache.felix.inventory.Format, boolean)
     */
    public void print(PrintWriter printWriter, Format format, boolean isZip) {
        if ( this.path == null || !format.equals(Format.JSON) ) {
            return;
        }
        ResourceResolver resolver = null;
        try {
            resolver = factory.getAdministrativeResourceResolver(null);
            final Resource rootResource = resolver.getResource(this.path);
            if ( rootResource != null ) {
                final ResourceTraversor rt = new ResourceTraversor(rootResource);
                rt.collectResources();
                printWriter.write(rt.getJSONObject().toString(2));

            }
        } catch (final LoginException e) {
            // ignore
        } catch (JSONException e) {
            // ignore
        } finally {
            if ( resolver != null ) {
                resolver.close();
            }
        }
    }

}
