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
package org.apache.sling.jcr.resource;

/**
 * The <code>JcrResourceConstants</code> interface provides constant values
 * for event topics and event properties for events sent from this bundle.
 */
public class JcrResourceConstants {

    /**
     * The namespace prefix used by Sling JCR for nodes and node types used by
     * Sling (value is "sling"). This prefix is ensured for any session used by
     * the JCR Resource bundle through the <code>Sling-Namespaces</code>
     * bundle manifest header.
     */
    public static final String SLING_NS_PREFIX = "sling";

    /**
     * The name of the JCR Property that defines the resource type of this node
     * (value is "sling:resourceType"). The resource manager implementation of
     * this bundle uses this property to defined the resource type of a loaded
     * resource. If this property does not exist the primary node type is used
     * as the resource type.
     */
    public static final String SLING_RESOURCE_TYPE_PROPERTY = "sling:resourceType";

    /**
     * The topic root for events published by this bundle (value is
     * "org/apache/sling/jcr/resource/ResourceEvent"). Event consumers should
     * register to "org/apache/sling/jcr/resource/ResourceEvent/*" to receive
     * all events from this bundle.
     */
    public static final String RESOURCE_EVENT = "org/apache/sling/jcr/resource/ResourceEvent";

    /**
     * The name of the event sent after new Object Mappings have been registered
     * (value is "org/apache/sling/jcr/resource/ResourceEvent/MAPPED").
     * <p>
     * Events of this topics have two additional properties:
     * {@link #MAPPING_CLASS} and {@link #MAPPING_NODE_TYPE}.
     */
    public static final String EVENT_MAPPING_ADDED = RESOURCE_EVENT + "/MAPPED";

    /**
     * The name of the event sent after Object Mappings have been unregistered
     * (value is "org/apache/sling/jcr/resource/ResourceEvent/UNMAPPED").
     * <p>
     * Events of this topics have two additional properties:
     * {@link #MAPPING_CLASS} and {@link #MAPPING_NODE_TYPE}.
     */
    public static final String EVENT_MAPPING_REMOVED = RESOURCE_EVENT
        + "/UNMAPPED";

    /**
     * The name of the event property providing a <code>String[]</code> of
     * class names mapped at the time the event is sent (value is
     * "MAPPED_CLASS"). This is the complete list of all classes which are
     * supported by the content manager for mapping.
     */
    public static final String MAPPING_CLASS = "MAPPED_CLASS";

    /**
     * The name of the event property providing a <code>String[]</code> of
     * node types mapped at the time the event is sent (value is
     * "MAPPED_NODE_TYPE"). This is the complete list of all node types which
     * are supported by the content manager for mapping.
     */
    public static final String MAPPING_NODE_TYPE = "MAPPED_NODE_TYPE";

    public static final String MAPPER_BUNDLE_HEADER = "Sling-Mappings";
}
