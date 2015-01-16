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

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

import org.apache.commons.io.IOUtils;
import org.apache.felix.cm.file.ConfigurationHandler;
import org.apache.felix.inventory.Format;
import org.apache.felix.inventory.InventoryPrinter;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.startlevel.StartLevel;

@Component
@Service
@Properties({
    @Property(name = "felix.inventory.printer.name", value = "crankstart"),
    @Property(name = "felix.inventory.printer.title", value = "Sling Crankstart Exporter"),
    @Property(name = "felix.inventory.printer.format", value = "TEXT")
})
public class CrankstartInventoryPrinter implements InventoryPrinter {

    private BundleContext bundleContext;
    private static final String INDENT = "  ";
    private static final String SEPARATOR = "# ----------------------------------------------------------------------------"; 
    
    @Reference
    private StartLevel startLevel;
    
    // Properties to ignore when dumping configs
    private static final String [] PROPS_TO_IGNORE = {
        Constants.SERVICE_PID,
        "service.factoryPid"
    };
    
    @Activate
    public void activate(ComponentContext ctx) {
        bundleContext = ctx.getBundleContext();
    }
    
    public void print(PrintWriter out, Format fmt, boolean isZip) {
        try {
            includeResource(out, "/crankstart-header.txt");
            bundles(out);
            header(out, "configurations");
            configs(out);
            header(out, "end of " + getClass().getSimpleName() + " status dump");
        } catch(Exception e) {
            e.printStackTrace(out);
        }
    }
    
    private void configs(PrintWriter out) throws IOException {
        final ServiceReference ref = bundleContext.getServiceReference(ConfigurationAdmin.class.getName());
        if(ref == null) {
            out.println("WARN - ConfigurationAdmin service not available");
            return;
        }
        
        out.println("# The CRANKSTART_CONFIG_ID property that we add to configs is meant to detect which factory configs have already been created");
        
        final ConfigurationAdmin ca = (ConfigurationAdmin)bundleContext.getService(ref);
        try {
            final Configuration [] allCfg = ca.listConfigurations(null);
            int count = 0;
            if(allCfg != null) {
                for(Configuration cfg : allCfg) {
                    count++;
                    if(cfg.getFactoryPid() != null && cfg.getFactoryPid().length() > 0) {
                        out.print("config.factory ");
                        out.print(cfg.getFactoryPid());
                    } else {
                        out.print("config ");
                        out.print(cfg.getPid());
                    }
                    out.println(" FORMAT:felix.config");
                    
                    out.print(INDENT);
                    out.print("CRANKSTART_CONFIG_ID=\"");
                    out.print(UUID.randomUUID());
                    out.println("\"");
                    
                    // Need to indent the config properties 
                    final ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    ConfigurationHandler.write(bos, cfg.getProperties());
                    final BufferedReader r = new BufferedReader(new StringReader(new String(bos.toString())));
                    String line = null;
                    
                    readingLines:
                    while((line = r.readLine()) != null) {
                        for(String p : PROPS_TO_IGNORE) {
                            if(line.startsWith(p)) {
                                continue readingLines;
                            }
                        }
                        out.print(INDENT);
                        out.println(line);
                    }
                    out.println();
                }
            }
            out.print("# ");
            out.print(count);
            out.println(" configurations processed");
        } catch(InvalidSyntaxException ise) {
            throw new RuntimeException("Unexpected InvalidSyntaxException", ise);
        } finally {
            bundleContext.ungetService(ref);
        }
    }
    
    private void bundles(PrintWriter out) throws IOException {
        for(int i=1; i<=startLevel.getStartLevel(); i++) {
            bundles(out, i);
        }
    }
    
    private void bundles(PrintWriter out, int level) throws IOException {
        int ok = 0;
        int errors = 0;
        int warnings = 0;
        int count = 0;
        
        // Get the list of Maven coordinates from any fragment bundles,
        // so that we can ignore their entries when returned by the
        // bundles that they are attached to
        final Set<String> fragmentCoords = new TreeSet<String>();
        for(Bundle b : bundleContext.getBundles()) {
            if(isFragment(b)) {
                final List<String> coords = getMavenCoordinates(b, null);
                if(coords.size() > 1) {
                    warnings++;
                    multipleWarning(out, b, coords);
                } else {
                    fragmentCoords.add(coords.get(0));
                }
            }
        }
        
        for(Bundle b : bundleContext.getBundles()) {
            if(startLevel.getBundleStartLevel(b) != level) {
                continue;
            }
            
            if(count==0) {
                out.println();
                out.print("# bundles for start level ");
                out.println(level);
                out.println(SEPARATOR);
                out.print("defaults crankstart.bundle.start.level ");
                out.println(level);
                out.println();
            }
            count++;
            
            final List<String> coords = getMavenCoordinates(b, fragmentCoords);
            if(coords.isEmpty()) {
                errors++;
                out.print("# ERROR: Maven coordinates not found for ");
                out.print(getBundleInfo(b));
                out.println();
            } else if(coords.size() > 1){
                warnings++;
                multipleWarning(out, b, coords);
            } else {
                ok++;
                out.print("bundle ");
                out.println(coords.get(0));
            }
        }
        
        if(count > 0) {
            out.println();
            out.print("# ");
            out.print("start level ");
            out.print(level);
            out.print(": ");
            out.print(ok);
            out.print(" bundles processed sucessfully, ");
            out.print(errors);
            out.print(" errors, ");
            out.print(warnings);
            out.print(" warnings.");
            out.println();
            out.println("start.all.bundles");
        }
    }
    
    private void multipleWarning(PrintWriter out, Bundle b, List<String> coords) {
        out.print("# WARN: multiple Maven coordinates for ");
        out.print(getBundleInfo(b));
        out.print(": ");
        out.print(coords);
        out.println();
    }
    
    private static boolean isFragment(final Bundle bundle) {
        Dictionary<?, ?> headerMap = bundle.getHeaders();
        return headerMap.get(Constants.FRAGMENT_HOST) != null;
    }
    
    private List<String> getMavenCoordinates(Bundle b, Collection<String> fragmentCoordinates) throws IOException {
        final List<String> result = new ArrayList<String>();
        
        @SuppressWarnings("unchecked")
        final Enumeration<URL> entries = b.findEntries("META-INF/maven", "pom.properties", true);
        
        // Get the pom.properties from the bundle itself, ignoring any attached fragments
        while(entries != null && entries.hasMoreElements()) {
            final URL u = entries.nextElement();
            java.util.Properties props = new java.util.Properties();
            InputStream is = null;
            try {
                is = u.openStream();
                props.load(is);
                final StringBuilder thisBundle = new StringBuilder();
                thisBundle.append("mvn:")
                .append(props.get("groupId"))
                .append("/")
                .append(props.get("artifactId"))
                .append("/")
                .append(props.get("version"));
                
                if(fragmentCoordinates != null && !isFragment(b) && fragmentCoordinates.contains(thisBundle.toString())) {
                    // fragment bundle - ignore
                } else {
                    result.add(thisBundle.toString());
                }
            } finally {
                IOUtils.closeQuietly(is);
            }
        }
        
        return result;
    }
    
    private static String getBundleInfo(Bundle b) {
        final StringBuilder sb = new StringBuilder();
        sb.append("bundle ")
        .append(b.getSymbolicName())
        .append(" ")
        .append(b.getVersion())
        .append(" (")
        .append(b.getBundleId())
        .append(")");
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