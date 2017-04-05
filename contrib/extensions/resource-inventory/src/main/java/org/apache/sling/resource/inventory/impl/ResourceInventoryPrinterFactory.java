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

import org.apache.felix.inventory.Format;
import org.apache.felix.inventory.InventoryPrinter;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.commons.json.JSONException;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.LoggerFactory;

@Component(service = InventoryPrinter.class,
           configurationPolicy=ConfigurationPolicy.REQUIRE,
           property = {
                   InventoryPrinter.FORMAT + "=JSON",
                   InventoryPrinter.WEBCONSOLE + ":Boolean=false"
           })
@Designate(ocd=ResourceInventoryPrinterFactory.Config.class, factory=true)
public class ResourceInventoryPrinterFactory implements InventoryPrinter {

    @ObjectClassDefinition(name = "Apache Sling Resource Inventory Printer Factory",
                           description = "This factory can be used to add " +
                                         "resource trees to the inventory of the system.")
    public @interface Config {

        @AttributeDefinition(name="Name", description="The unique name of the inventory printer.")
        String felix_inventory_printer_name();

        @AttributeDefinition(name="Title", description="The title of the inventory printer.")
        String felix_inventory_printer_title();

        @AttributeDefinition(name="Path", description="The resource path to include.")
        String path() default "";
    }
    private String path;

    @Reference
    private ResourceResolverFactory factory;

    @Activate
    protected void activate(final Config config) {
        this.path = config.path();
    }

    /**
     * @see org.apache.felix.inventory.InventoryPrinter#print(java.io.PrintWriter, org.apache.felix.inventory.Format, boolean)
     */
    @Override
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
        } catch (final JSONException ignore) {
            LoggerFactory.getLogger(this.getClass()).warn("Unable to create resource json", ignore);
        } finally {
            if ( resolver != null ) {
                resolver.close();
            }
        }
    }

}
