/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.sling.jcr.webconsole.internal;

import java.io.PrintWriter;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.Service;
import org.apache.felix.webconsole.ConfigurationPrinter;
import org.apache.sling.jcr.api.SlingRepository;

/**
 * A Felix WebConsole ConfigurationPrinter which outputs the current JCR
 * repository descriptors.
 */
@Component
@Service(ConfigurationPrinter.class)
@Properties({
    @Property(name = "service.description", value = "JCR Descriptors Configuration Printer"),
    @Property(name = "service.vendor", value = "The Apache Software Foundation")
})
public class DescriptorsConfigurationPrinter implements ConfigurationPrinter {


    @Reference(policy=ReferencePolicy.DYNAMIC)
    private volatile SlingRepository slingRepository;

    /**
     * Get the title of the configuration status page.
     *
     * @return the title
     */
    public String getTitle() {
        return "JCR Descriptors";
    }

    /**
     * Output a list of repository descriptors.
     *
     * @param pw a PrintWriter
     */
    public void printConfiguration(PrintWriter pw) {
        if (slingRepository != null) {
            final String[] descriptorKeys = slingRepository.getDescriptorKeys();
            for (final String key : descriptorKeys) {
                pw.printf("%s = %s\n", key, slingRepository.getDescriptor(key));
            }
        } else {
            pw.println("SlingRepository is not available.");
        }

    }

}
