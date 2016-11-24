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
import java.lang.reflect.Array;

import org.apache.sling.caconfig.spi.ConfigurationMetadataProvider;
import org.apache.sling.caconfig.spi.metadata.ConfigurationMetadata;
import org.apache.sling.caconfig.spi.metadata.PropertyMetadata;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

/**
 * Print configuration metadata provided by a {@link ConfigurationMetadata}.
 */
class ConfigurationMetadataPrinter implements ServiceConfigurationPrinter<ConfigurationMetadataProvider> {

    @Override
    public void printConfiguration(PrintWriter pw, ServiceReference<ConfigurationMetadataProvider> serviceReference, BundleContext bundleContext) {
        ConfigurationMetadataProvider service = bundleContext.getService(serviceReference);
        
        for (String configName : service.getConfigurationNames()) {
            ConfigurationMetadata metadata = service.getConfigurationMetadata(configName);
            pw.print(INDENT);
            pw.print(BULLET);
            pw.println(metadata.getName());
            
            for (PropertyMetadata<?> property : metadata.getPropertyMetadata().values()) {
                pw.print(INDENT_2);
                pw.print(BULLET);
                pw.print(property.getName());
                
                pw.print("(");
                pw.print(property.getType().getSimpleName());
                pw.print(")");
                
                if (property.getDefaultValue() != null) {
                    pw.print(" = ");
                    printValue(pw, property.getDefaultValue());
                }
                
                pw.println();
            }
        }
        
        bundleContext.ungetService(serviceReference);
    }
    
    private void printValue(PrintWriter pw, Object value) {
        if (value.getClass().isArray()) {
            for (int i=0; i<Array.getLength(value); i++) {
                if (i > 0) {
                    pw.print(", ");
                }
                printValue(pw, Array.get(value, i));
            }
        }
        else {
            pw.print(value);
        }
    }

}
