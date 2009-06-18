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
package org.apache.sling.jcr.ocm;


/**
 * The <code>OcmConstants</code> interface provides constant values
 * for event topics and event properties for events sent from this bundle.
 */
public class OcmConstants {

    /**
     * The topic root for events published by this bundle (value is
     * "org/apache/sling/jcr/ocm/OCMEvent"). Event consumers should
     * register to "org/apache/sling/jcr/ocm/OCMEvent/*" to receive
     * all events from this bundle.
     */
    public static final String RESOURCE_EVENT = "org/apache/sling/jcr/ocm/OCMEvent";

    /**
     * The name of the event sent after new Object Mappings have been registered
     * (value is "org/apache/sling/jcr/ocm/OCMEvent/MAPPED").
     * <p>
     * Events of this topics have two additional properties:
     * {@link #MAPPING_CLASS} and {@link #MAPPING_NODE_TYPE}.
     */
    public static final String EVENT_MAPPING_ADDED = RESOURCE_EVENT + "/MAPPED";

    /**
     * The name of the event sent after Object Mappings have been unregistered
     * (value is "org/apache/sling/jcr/ocm/OCMEvent/UNMAPPED").
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

    /**
     * The name of the bundle manifest header listing the bundle entries
     * providing Object Content Mapping configurations (value is
     * "Sling-Mappings").
     */
    public static final String MAPPER_BUNDLE_HEADER = "Sling-Mappings";

    /**
     * The name of the bundle manifest header listing the resource provider root
     * paths provided by the bundle (value is "Sling-Bundle-Resources").
     */
    public static final String BUNDLE_RESOURCE_ROOTS = "Sling-Bundle-Resources";

}
