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
package org.apache.sling.crankstart.extensions.sling;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.URL;
import java.util.Enumeration;

import org.apache.commons.io.IOUtils;
import org.apache.felix.inventory.Format;
import org.apache.felix.inventory.InventoryPrinter;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;

@Component
@Service
@Properties({
    @Property(name = "felix.inventory.printer.name", value = "crankstart"),
    @Property(name = "felix.inventory.printer.title", value = "Sling Crankstart Exporter"),
    @Property(name = "felix.inventory.printer.format", value = "TEXT")
})
public class CrankstartInventoryPrinter implements InventoryPrinter {

    private BundleContext bundleContext;
    
    @Activate
    public void activate(ComponentContext ctx) {
        bundleContext = ctx.getBundleContext();
    }
    
    public void print(PrintWriter out, Format fmt, boolean isZip) {
        try {
            includeResource(out, "/crankstart-header.txt");
            header(out, "bundles");
            bundles(out);
            header(out, "configurations");
            configs(out);
            header(out, "end of " + getClass().getSimpleName() + " status dump");
        } catch(Exception e) {
            e.printStackTrace(out);
        }
    }
    
    private void configs(PrintWriter out) throws IOException {
        out.println("# TODO - dump OSGi configurations with FORMAT:felix.config");
    }
    
    private void bundles(PrintWriter out) throws IOException {
        int ok = 0;
        int errors = 0;
        for(Bundle b : bundleContext.getBundles()) {
            final String coords = mavenCoordinates(b);
            if(coords.length() == 0) {
                errors++;
                out.print("# ERROR: Maven coordinates not found for bundle ");
                out.print(b.getSymbolicName());
                out.print(" version ");
                out.print(b.getVersion());
                out.println();
            } else {
                ok++;
                out.print("bundle ");
                out.println(coords);
            }
        }
        
        out.println("start.all.bundles");
        
        out.print("# ");
        out.print(ok);
        out.print(" bundles processed sucessfully, ");
        out.print(errors);
        out.println(" errors.");
    }
    
    private String mavenCoordinates(Bundle b) throws IOException {
        final StringBuilder sb = new StringBuilder();
        
        @SuppressWarnings("unchecked")
        final Enumeration<URL> entries = b.findEntries("META-INF/maven", "pom.properties", true);
        
        int count=0;
        while(entries != null && entries.hasMoreElements()) {
            final URL u = entries.nextElement();
            java.util.Properties props = new java.util.Properties();
            InputStream is = null;
            try {
                is = u.openStream();
                props.load(u.openStream());
                sb.append("mvn:")
                .append(props.get("groupId"))
                .append("/")
                .append(props.get("artifactId"))
                .append("/")
                .append(props.get("version"));
            } finally {
                IOUtils.closeQuietly(is);
            }
            count++;
        }
        
        if(count > 1) {
            sb.append(" WARNING - multiple entries, how to handle that?");
        }
 
        return sb.toString();
    }
    
    private void header(PrintWriter out, String title) {
        out.println();
        out.print("# ");
        out.println(title);
    }
    
    private void includeResource(PrintWriter out, String path) throws IOException {
        final InputStream resource = getClass().getResourceAsStream(path);
        if(resource == null) {
            throw new IOException("Resource not found: " + path);
        }
        try {
            IOUtils.copy(resource, out);
        } finally {
            resource.close();
        }
    }
    
    
}
