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
package org.apache.sling.jcr.resource.internal.helper.jcr;


import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.PropertyUnbounded;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@code PathMapper} allows to
 * - map path from the JCR resource tree to resource paths
 * - hide JCR nodes; however this is not a security feature
 */
@Service(value = PathMapper.class)
@Component(metatype = true,
        label = "Apache Sling JCR Resource Provider Path Mapper",
        description = "This service provides path mappings for JCR nodes.")
public class PathMapper {

    /** Logger */
    private final Logger log = LoggerFactory.getLogger(getClass());

    @Property(unbounded = PropertyUnbounded.ARRAY,
            label = "Path mapping", description = "Defines an obtional path mapping for a path." +
            "Each mapping entry is expressed as follow: <JCRPath>:<resourcePath>. As an example: /foo:/libs, " +
            "this maps the JCR node /foo to the resource /libs. If the resource path is specified as '.', " +
            " the JCR tree is not visible in the resource tree. This should not be considered a security feature " +
            " as the nodes are still accessible through the JCR api. Mapping a JCR path to the root is not allowed. " +
            "The mappings are evaluated as ordered in the configuration.")
    private static final String PATH_MAPPING = "path.mapping";

    /** The mappings. */
    private final List<Mapping> mappings = new ArrayList<Mapping>();

    private static final class Mapping {
        public final String jcrPath;
        public final String resourcePath;
        public final String jcrPathPrefix;
        public final String resourcePathPrefix;

        public Mapping(final String path, final String mappedPath) {
            this.jcrPath = path;
            this.jcrPathPrefix = path.concat("/");
            if ( mappedPath.equals(".") ) {
                this.resourcePath = null;
                this.resourcePathPrefix = null;
            } else {
                this.resourcePath = mappedPath;
                this.resourcePathPrefix = mappedPath.concat("/");
            }
        }
    }

    @Activate
    private void activate(final Map<String, Object> props) {
        mappings.clear();
        final String[] config = PropertiesUtil.toStringArray(props.get(PATH_MAPPING), null);
        if ( config != null ) {
            for (final String mapping : config) {
                boolean valid = false;
                final String[] parts = mapping.split(":");
                if (parts.length == 2) {
                    parts[0] = parts[0].trim();
                    parts[1] = parts[1].trim();
                    if ( parts[0].startsWith("/") && (parts[1].startsWith("/") || parts[1].equals(".")) ) {
                        if ( parts[0].endsWith("/") ) {
                            parts[0] = parts[0].substring(0, parts[1].length() - 1);
                        }
                        if ( parts[1].endsWith("/") ) {
                            parts[1] = parts[1].substring(0, parts[1].length() - 1);
                        }
                        if ( parts[0].length() > 1 && (parts[1].length() > 1 || parts[1].equals(".")) ) {
                            mappings.add(new Mapping(parts[0], parts[1]));
                            valid = true;
                        }
                    }
                }
                if ( !valid ) {
                    log.warn("Invalid mapping configuration (skipping): {}", mapping);
                }
            }
        }
    }

    /**
     * Map a resource path to a JCR path
     * @param resourcePath The resource path
     * @return The JCR path or {@code null}
     */
    public String mapResourcePathToJCRPath(final String resourcePath) {
        String jcrPath = resourcePath;
        if (resourcePath != null && !mappings.isEmpty()) {
            for (final Mapping mapping : mappings) {
                if ( mapping.resourcePath == null ) {
                    if ( resourcePath.equals(mapping.jcrPath) || resourcePath.startsWith(mapping.jcrPathPrefix) ) {
                        jcrPath = null;
                        break;
                    }
                } else {
                    if (resourcePath.equals(mapping.resourcePath)) {
                        jcrPath = mapping.jcrPath;
                        break;
                    } else if (resourcePath.startsWith(mapping.resourcePathPrefix)) {
                        jcrPath = mapping.jcrPathPrefix.concat(resourcePath.substring(mapping.resourcePathPrefix.length()));
                        break;
                    }
                }
            }
        }
        return jcrPath;
    }

    /**
     * Map a JCR path to a resource path
     * @param jcrPath The JCR path
     * @return The resource path or {@code null}
     */
    public String mapJCRPathToResourcePath(final String jcrPath) {
        String resourcePath = jcrPath;
        if (jcrPath != null && !mappings.isEmpty()) {
            for (final Mapping mapping : mappings) {
                if (mapping.resourcePath != null && (jcrPath.equals(mapping.resourcePath) || jcrPath.startsWith(mapping.resourcePathPrefix)) ) {
                    resourcePath = null;
                    break;
                } else if (jcrPath.equals(mapping.jcrPath)) {
                    resourcePath = mapping.resourcePath;
                    break;
                } else if (jcrPath.startsWith(mapping.jcrPathPrefix)) {
                    if ( mapping.resourcePath == null ) {
                        resourcePath = null;
                    } else {
                        resourcePath = mapping.resourcePathPrefix.concat(jcrPath.substring(mapping.jcrPathPrefix.length()));
                    }
                    break;
                }
            }
        }
        return resourcePath;
    }
}
