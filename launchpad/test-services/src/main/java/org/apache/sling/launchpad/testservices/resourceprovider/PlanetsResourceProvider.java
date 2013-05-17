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
package org.apache.sling.launchpad.testservices.resourceprovider;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceProvider;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.SyntheticResource;
import org.apache.sling.api.resource.ValueMap;

/** Test/example ResourceProvider that provides info about
 *  the Solar System's planets at /planets. 
 *  Use /planets.tidy.-1.json to GET the whole thing. 
 */
@Component
@Service
@Properties({
    @Property(name=ResourceProvider.ROOTS, value=PlanetsResourceProvider.ROOT)
})
public class PlanetsResourceProvider implements ResourceProvider {

    /** Our planet data. PlanetResource is created when resolving, as
     *  it points to a specific ResourceResolver. */
    private static final Map<String, ValueMap> PLANETS = new HashMap<String, ValueMap>();
    
    /** This can be configurable of course */ 
    public static final String ROOT = "planets";
    public static final String ABS_ROOT = "/" + ROOT;
    
    /* Planet data from http://nineplanets.org/data.html
     * (not that we care much about accuracy anyway ;-) 
     */
    static {
        definePlanet("Mercury", 57910);
        definePlanet("Venus", 108200);
        definePlanet("Earth", 149600).put("comment", "Resources can have different sets of properties");
        definePlanet("Mars", 227940);
        definePlanet("Jupiter", 4332);
        definePlanet("Saturn", 10759);
        definePlanet("Uranus", 30685);
        definePlanet("Neptune", 60190);

        // Add the moon to test a two-level hierarchy
        final String moonPath = ABS_ROOT + "/earth/moon";
        PLANETS.put(moonPath, new PlanetResource.PlanetValueMap("Moon", 384));
    }
    
    /** ResourceProvider interface */
    public Resource getResource(ResourceResolver resolver, HttpServletRequest req, String path) {
        // Synthetic resource for our root, so that /planets works
        if((ABS_ROOT).equals(path)) {
            return new SyntheticResource(resolver, path, PlanetResource.RESOURCE_TYPE);
        }
        
        // Not root, return a Planet if we have one
        final ValueMap data = PLANETS.get(path);
        return data == null ? null : new PlanetResource(resolver, path, data);
    }

    /** ResourceProvider interface */
    public Resource getResource(ResourceResolver resolver, String path) {
        return getResource(resolver, null, path);
    }

    /** ResourceProvider interface */
    public Iterator<Resource> listChildren(Resource parent) {
        if(parent.getPath().startsWith(ABS_ROOT)) {
            // Not the most efficient thing...good enough for this example
            final List<Resource> kids = new ArrayList<Resource>();
            for(Map.Entry<String, ValueMap> e : PLANETS.entrySet()) {
                if(parent.getPath().equals(parentPath(e.getKey()))) {
                    kids.add(new PlanetResource(parent.getResourceResolver(), e.getKey(), e.getValue()));
                }
            }
            return kids.iterator();
        } else {
            return null;
        }
    }
    
    private static String parentPath(String path) {
        final int lastSlash = path.lastIndexOf("/");
        return lastSlash > 0 ? path.substring(0, lastSlash) : "";
    }
    
    private static ValueMap definePlanet(String name, int distance) {
        final ValueMap valueMap = new PlanetResource.PlanetValueMap(name, distance);
        PLANETS.put(ABS_ROOT + "/" + name.toLowerCase(), valueMap);
        return valueMap;
    }
}