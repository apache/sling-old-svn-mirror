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
package org.apache.sling.serviceusermapping.impl;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.felix.inventory.Format;
import org.apache.felix.inventory.InventoryPrinter;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.io.JSONWriter;

/** InventoryPrinter for service user mappings */
@Component
@Service(value = InventoryPrinter.class)
@Properties({
    @Property(name = InventoryPrinter.FORMAT, value = { "JSON", "TEXT" }),
    @Property(name = InventoryPrinter.NAME, value = "slingserviceusers"),
    @Property(name = InventoryPrinter.TITLE, value = "Sling Service User Mappings"),
    @Property(name = InventoryPrinter.WEBCONSOLE, boolValue = true)
})
public class MappingInventoryPrinter implements InventoryPrinter {

    @Reference
    private ServiceUserMapperImpl mapper;

    @Override
    public void print(PrintWriter out, Format format, boolean isZip) {
        try {
            if(format.equals(Format.JSON)) {
                renderJson(out);
            } else if(format.equals(Format.TEXT)) {
                renderText(out);
            }
        } catch(Exception e) {
            e.printStackTrace(out);
        }
    }

    private String getMappedUser(Mapping m) {
        return m.map(m.getServiceName(), m.getSubServiceName());
    }

    private SortedMap<String, List<Mapping>> getMappingsByUser(List<Mapping> mappings) {
        SortedMap<String, List<Mapping>> result = new TreeMap<String, List<Mapping>>();
        for(Mapping m : mappings) {
            final String user = getMappedUser(m);
            List<Mapping> list = result.get(user);
            if(list == null) {
                list = new ArrayList<Mapping>();
                result.put(user, list);
            }
            list.add(m);
        }
        return result;
    }

    private void asJSON(JSONWriter w, Mapping m) throws JSONException {
        w.object();
        w.key("serviceName").value(m.getServiceName());
        w.key("subServiceName").value(m.getSubServiceName());
        w.key("user").value(getMappedUser(m));
        w.endObject();
    }

    private void renderJson(PrintWriter out) throws JSONException {
        final List<Mapping> data = mapper.getActiveMappings();
        final Map<String, List<Mapping>> byUser = getMappingsByUser(data);

        final JSONWriter w = new JSONWriter(out);
        w.setTidy(true);
        w.object();
        w.key("title").value("Service User Mappings");
        w.key("mappingsCount").value(data.size());
        w.key("uniqueUsersCount").value(byUser.keySet().size());

        w.key("mappingsByUser");
        w.object();
        for(Map.Entry<String, List<Mapping>> e : byUser.entrySet()) {
            w.key(e.getKey());
            w.array();
            for(Mapping m : e.getValue()) {
                asJSON(w,m);
            }
            w.endArray();
        }
        w.endObject();

        w.endObject();
    }

    private void asText(PrintWriter w, Mapping m, String indent) {
        final String SEP = " / ";
        w.print(indent);
        w.print(m.getServiceName());
        w.print(SEP);
        final String sub = m.getSubServiceName();
        w.print(sub == null ? "" : sub);
        w.print(SEP);
        w.println(getMappedUser(m));
    }

    private void renderText(PrintWriter out) {
        final List<Mapping> data = mapper.getActiveMappings();
        final Map<String, List<Mapping>> byUser = getMappingsByUser(data);

        final String formatInfo = " (format: service name / sub service name / user)";

        out.print("*** Mappings by user (");
        out.print(byUser.keySet().size());
        out.print(" users):");
        out.println(formatInfo);

        for(Map.Entry<String, List<Mapping>> e : byUser.entrySet()) {
            out.print("  ");
            out.println(e.getKey());
            for(Mapping m : e.getValue()) {
                asText(out, m, "    ");
            }
        }
   }
}