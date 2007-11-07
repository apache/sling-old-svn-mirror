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
public interface JcrResourceConstants {

    /**
     * The namespace prefix used by Sling JCR for nodes and node types used by
     * Sling (value is "sling"). This prefix is ensured for any session used by
     * the JCR Resource bundle through the <code>Sling-Namespaces</code>
     * bundle manifest header.
     */
    static final String SLING_NS_PREFIX = "sling";

    /**
     * The topic root for events published by this bundle (value is
     * "org/apache/sling/content/jcr/ContentEvent"). Event consumers should
     * register to "org/apache/sling/content/jcr/ContentEvent/*" to receive all
     * events from this bundle.
     */
    static final String CONTENT_EVENT = "org/apache/sling/content/jcr/ContentEvent";

    /**
     * The name of the event sent after new Object Mappings have been registered
     * (value is "org/apache/sling/content/jcr/ContentEvent/MAPPED"). This name
     * is appended to the {@link #CONTENT_EVENT root topic} to create the event
     * topic.
     * <p>
     * Events of this topics have two additional properties:
     * {@link #MAPPING_CLASS} and {@link #MAPPING_NODE_TYPE}.
     */
    static final String EVENT_MAPPING_ADDED = CONTENT_EVENT + "/MAPPED";

    /**
     * The name of the event sent after Object Mappings have been unregistered
     * (value is "org/apache/sling/content/jcr/ContentEvent/UNMAPPED"). This
     * name is appended to the {@link #CONTENT_EVENT root topic} to create the
     * event topic.
     * <p>
     * Events of this topics have two additional properties:
     * {@link #MAPPING_CLASS} and {@link #MAPPING_NODE_TYPE}.
     */
    static final String EVENT_MAPPING_REMOVED = CONTENT_EVENT + "/UNMAPPED";

    /**
     * The name of the event property providing a <code>String[]</code> of
     * class names mapped at the time the event is sent (value is
     * "MAPPED_CLASS"). This is the complete list of all classes which are
     * supported by the content manager for mapping.
     */
    static final String MAPPING_CLASS = "MAPPED_CLASS";

    /**
     * The name of the event property providing a <code>String[]</code> of
     * node types mapped at the time the event is sent (value is
     * "MAPPED_NODE_TYPE"). This is the complete list of all node types which
     * are supported by the content manager for mapping.
     */
    static final String MAPPING_NODE_TYPE = "MAPPED_NODE_TYPE";
}
