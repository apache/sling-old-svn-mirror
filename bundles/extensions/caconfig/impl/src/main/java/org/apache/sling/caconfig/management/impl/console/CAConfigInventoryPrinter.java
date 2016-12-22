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
package org.apache.sling.caconfig.management.impl.console;

import java.io.PrintWriter;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.felix.inventory.Format;
import org.apache.felix.inventory.InventoryPrinter;
import org.apache.sling.caconfig.resource.spi.CollectionInheritanceDecider;
import org.apache.sling.caconfig.resource.spi.ConfigurationResourceResolvingStrategy;
import org.apache.sling.caconfig.resource.spi.ContextPathStrategy;
import org.apache.sling.caconfig.spi.ConfigurationInheritanceStrategy;
import org.apache.sling.caconfig.spi.ConfigurationMetadataProvider;
import org.apache.sling.caconfig.spi.ConfigurationOverrideProvider;
import org.apache.sling.caconfig.spi.ConfigurationPersistenceStrategy;
import org.apache.sling.commons.osgi.Order;
import org.apache.sling.commons.osgi.ServiceUtil;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;

/**
 * Web console configuration printer.
 */
@Component(service=InventoryPrinter.class,
property={Constants.SERVICE_DESCRIPTION + "=Apache Sling Context-Aware Configuration Resolver Console Inventory Printer",
        InventoryPrinter.NAME + "=" + CAConfigInventoryPrinter.NAME,
        InventoryPrinter.TITLE + "=" + CAConfigInventoryPrinter.TITLE,
        InventoryPrinter.FORMAT + "=TEXT"})
public class CAConfigInventoryPrinter implements InventoryPrinter {

    public static final String NAME = "slingcaconfig";
    public static final String TITLE = "Sling Context-Aware Configuration";
    
    private BundleContext bundleContext;
    
    @Activate
    private void activate(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }
    
    @Override
    public void print(PrintWriter pw, Format format, boolean isZip) {
        if (format != Format.TEXT) {
            return;
        }

        printSPISection(pw, ContextPathStrategy.class, "Context Path Strategies");
        printSPISection(pw, ConfigurationResourceResolvingStrategy.class, "Configuration Resource Resolving Strategies");
        printSPISection(pw, CollectionInheritanceDecider.class, "Collection Inheritance Deciders");
        printSPISection(pw, ConfigurationInheritanceStrategy.class, "Configuration Inheritance Strategies");
        printSPISection(pw, ConfigurationPersistenceStrategy.class, "Configuration Persistance Strategies");
        printSPISection(pw, ConfigurationMetadataProvider.class, "Configuration Metadata Providers",
                new ConfigurationMetadataPrinter());
        printSPISection(pw, ConfigurationOverrideProvider.class, "Configuration Override Providers",
                new ConfigurationOverridePrinter());
    }
    
    @SafeVarargs
    private final <T> void printSPISection(PrintWriter pw, Class<T> clazz, String title, ServiceConfigurationPrinter<T>... serviceConfigPrinters) {
        Collection<ServiceReference<T>> serviceReferences = getServiceReferences(clazz);

        pw.println(title);
        pw.println(StringUtils.repeat('-', title.length()));
        
        if (serviceReferences.isEmpty()) {
            pw.println("(none)");
        }
        else {
            for (ServiceReference<T> serviceReference : serviceReferences) {
                pw.print(ServiceConfigurationPrinter.BULLET); 
                pw.print(getServiceClassName(serviceReference));
                pw.print(" [");
                pw.print(getServiceRanking(serviceReference));
                if (!isEnabled(serviceReference)) {
                    pw.print(", disabled");
                }
                pw.print("]");
                pw.println();
                for (ServiceConfigurationPrinter<T> serviceConfigPrinter : serviceConfigPrinters) {
                    serviceConfigPrinter.printConfiguration(pw, serviceReference, bundleContext);
                }
            }
        }
        pw.println();
    }
    
    private <T> Collection<ServiceReference<T>> getServiceReferences(Class<T> clazz) {
        try {
            SortedMap<Comparable<Object>,ServiceReference<T>> sortedServices = new TreeMap<>();
            Collection<ServiceReference<T>> serviceReferences = bundleContext.getServiceReferences(clazz, null);
            for (ServiceReference<T> serviceReference : serviceReferences) {
                Map<String,Object> props = new HashMap<>();
                for (String property : serviceReference.getPropertyKeys()) {
                    props.put(property, serviceReference.getProperty(property));
                } 
                sortedServices.put(
                        ServiceUtil.getComparableForServiceRanking(props, Order.DESCENDING),
                        serviceReference
                );
            }
            return sortedServices.values();
        }
        catch (InvalidSyntaxException ex) {
            throw new RuntimeException(ex);
        }
    }
    
    private <T> String getServiceClassName(ServiceReference<T> serviceReference) {
        Object service = bundleContext.getService(serviceReference);
        String serviceClassName = service.getClass().getName();
        bundleContext.ungetService(serviceReference);
        return serviceClassName;
    }
    
    private <T> int getServiceRanking(ServiceReference<T> serviceReference) {
        Integer serviceRanking = (Integer)serviceReference.getProperty(Constants.SERVICE_RANKING);
        if (serviceRanking == null) {
            return 0;
        }
        else {
            return serviceRanking;
        }
    }

    private <T> boolean isEnabled(ServiceReference<T> serviceReference) {
        Object enabledObject = (Object)serviceReference.getProperty("enabled");
        if (enabledObject != null) {
            if (enabledObject instanceof Boolean) {
                return ((Boolean)enabledObject).booleanValue();            
            }
            else {
                return BooleanUtils.toBoolean(enabledObject.toString());
            }
        }
        return true;
    }

}
