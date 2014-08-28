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
import java.util.Arrays;
import java.util.List;

import javax.jcr.NamespaceRegistry;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

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
 * namespace mappings.
 */
@Component
@Service(ConfigurationPrinter.class)
@Properties({
    @Property(name = "service.description", value = "JCR Namespace Configuration Printer"),
    @Property(name = "service.vendor", value = "The Apache Software Foundation")
})
public class NamespaceConfigurationPrinter implements ConfigurationPrinter {


    @Reference(policy=ReferencePolicy.DYNAMIC)
    private volatile SlingRepository slingRepository;

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
            Session session = null;
            try {
                session = slingRepository.loginAdministrative(null);
                NamespaceRegistry reg = session.getWorkspace().getNamespaceRegistry();
                List<String> globalPrefixes = Arrays.asList(reg.getPrefixes());
                for (String prefix : session.getNamespacePrefixes()) {
                    if (prefix.length() > 0) {
                        pw.printf("%10s = %s", prefix, session.getNamespaceURI(prefix));
                        if (globalPrefixes.contains(prefix)) {
                            pw.print(" [global]");
                        } else {
                            pw.print(" [local]");
                        }
                        pw.println();
                    }
                }
            } catch (RepositoryException e) {
                pw.println("Unable to output namespace mappings.");
                e.printStackTrace(pw);
            } finally {
                if (session != null) {
                    session.logout();
                }
            }
        } else {
            pw.println("SlingRepository is not available.");
        }

    }

}
