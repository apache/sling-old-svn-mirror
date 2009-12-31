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
package org.apache.sling.jcr.webconsole.internal.webconsole;

import java.io.PrintWriter;

import javax.jcr.NamespaceRegistry;
import javax.jcr.RepositoryException;

import org.apache.felix.webconsole.ConfigurationPrinter;
import org.apache.sling.jcr.api.SlingRepository;

/**
 * A Felix WebConsole ConfigurationPrinter which outputs the current JCR
 * namespace mappings.
 *
 * @scr.component immediate="true" label="%namespace.printer.name"
 *                description="%namespace.printer.description"
 *                metatype="no"
 * @scr.property name="service.description"
 *               value="JCR Namespace Configuration Printer"
 * @scr.property name="service.vendor" value="The Apache Software Foundation"
 * @scr.service interface="org.apache.felix.webconsole.ConfigurationPrinter"
 *
 */
public class NamespaceConfigurationPrinter implements ConfigurationPrinter {

    /**
     * @scr.reference policy="dynamic"
     */
    private SlingRepository slingRepository;

    /**
     * Get the title of the configuration status page.
     * 
     * @return the title
     */
    public String getTitle() {
        return "JCR Namespaces";
    }

    /**
     * Output a list of namespace prefixes and URIs from the NamespaceRegistry.
     * 
     * @param pw a PrintWriter
     */
    public void printConfiguration(PrintWriter pw) {
        if (slingRepository != null) {
            try {
                NamespaceRegistry reg = slingRepository.loginAdministrative(slingRepository.getDefaultWorkspace()).getWorkspace()
                        .getNamespaceRegistry();
                for (String prefix : reg.getPrefixes()) {
                    if (prefix.length() > 0) {
                        pw.printf("%10s = %s", prefix, reg.getURI(prefix));
                        pw.println();
                    }
                }
            } catch (RepositoryException e) {
                pw.println("Unable to output namespace mappings.");
                e.printStackTrace(pw);
            }
        } else {
            pw.println("SlingRepsoitory is not available.");
        }

    }

}
