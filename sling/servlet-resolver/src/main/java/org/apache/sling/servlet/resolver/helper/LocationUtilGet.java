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
package org.apache.sling.servlet.resolver.helper;

import java.util.Iterator;
import java.util.Set;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LocationUtilGet extends LocationUtil {

    /** default log */
    private final Logger log = LoggerFactory.getLogger(getClass());

    private final String selectors;

    private final int numSelectors;

    private final String extension;

    private final boolean isGetHtmlRequest;

    protected LocationUtilGet(SlingHttpServletRequest request) {
        super(request);

        selectors = request.getRequestPathInfo().getSelectorString();
        numSelectors = request.getRequestPathInfo().getSelectors().length;
        extension = request.getRequestPathInfo().getExtension();
        isGetHtmlRequest = "html".equals(request.getRequestPathInfo().getExtension());
    }

    @Override
    protected int getLocationResources(int ordinal, Resource location,
            Set<LocationResource> resources) {

        // print/a4.html.esp
        // print.html.esp
        if (selectors != null && selectors.length() > 0) {
            String relString = selectors.replace('.', '/');
            int numSelectors = this.numSelectors;
            while (relString.length() > 0) {

                String parent;
                String name;
                Resource loc;

                int slash = relString.lastIndexOf('/');
                if (slash > 0) {
                    parent = relString.substring(0, slash);
                    name = relString.substring(slash + 1);
                    loc = getResource(location, parent);
                } else {
                    parent = "";
                    name = relString;
                    loc = location;
                }

                ordinal = getLocationResources(ordinal, loc, name, false,
                    numSelectors, resources);
                relString = parent;

                numSelectors--;
            }
        }

        // sample.html.esp
        // sample.esp
        // html.esp
        ordinal = getLocationResources(ordinal, location,
            ResourceUtil.getName(location.getPath()), true, 0, resources);

        // base class implementation supporting the method name
        return super.getLocationResources(ordinal, location, resources);
    }

    protected int getLocationResources(int ordinal, Resource location,
            String locationPrefix, boolean optionalLocationPrefix,
            int numSelectors, Set<LocationResource> resources) {

        // now list the children and check them
        Iterator<Resource> children = getResolver().listChildren(location);
        while (children.hasNext()) {
            Resource child = children.next();
            LocationResource lr = createLocationResource(ordinal,
                locationPrefix, optionalLocationPrefix, child, numSelectors);
            if (lr != null) {
                resources.add(lr);
                ordinal++;
            }
        }

        return ordinal;
    }

    protected LocationResource createLocationResource(int ordinal,
            String locationPrefix, boolean optionalLocationPrefix,
            Resource resource, int numSelectors) {

        // split the remaing name to ease further checks
        String resPath = resource.getPath();
        String name = ResourceUtil.getName(resPath);
        String[] parts = name.split("\\.");
        int i = 0;

        // flag whether we require the request extension in the script name
        // to begin with, this is only required for non-html requests
        boolean requireExtension = !isGetHtmlRequest;

        int methodExtensionWeight = LocationResource.WEIGHT_NONE;
        
        // expect locationPrefix being the last part of the selector
        if (i >= parts.length || !locationPrefix.equals(parts[i])) {
            if (!optionalLocationPrefix) {
                log.debug(
                    "createLocationResource: Ignoring Resource {}: Name does not start with {}",
                    resource, locationPrefix);
                return null;
            }

            log.debug(
                "createLocationResource: Resource {} does not start with {}",
                resource, locationPrefix);

            // flag that the request extension is required in the name
            requireExtension = true;
        } else {
            // increment counter, we have the locationPrefix
            i++;
            methodExtensionWeight += LocationResource.WEIGHT_PREFIX;
        }

        // next may be extension name
        boolean hasExtension = false;
        if (extension != null) {
            if (i < parts.length && extension.equals(parts[i])) {
                i++;
                methodExtensionWeight += LocationResource.WEIGHT_EXTENSION;
            } else if (requireExtension) {
                log.debug(
                    "createLocationResource: Ignoring Resource {} because request extension {} is missing in the name",
                    resource, extension);
                return null;
            }
        }

        // next would be script extension
        if ((i + 1) != parts.length) {
            // more than one more part, we expect just a single one or none
            return null;
        }

        return new LocationResource(ordinal, resource, numSelectors,
            methodExtensionWeight);
    }

}
