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
package org.apache.sling.tenant;

/**
 * <p>
 * The {@code TenantConstants} interface provides some symbolic constants
 * for well known constant strings in Sling Tenant bundle.
 * </p>
 *
 * @since 1.1
 */
public interface TenantConstants {

    /**
     * <p>The topic for the OSGi event which is sent when a tenant has been created.</p>
     * <p>The event contains at least the {@link #PROPERTY_TENANTID}.</p>
     *
     * @since 1.1
     */
    public static final String TOPIC_TENANT_CREATED = "org/apache/sling/tenant/CREATED";


    /**
     * <p>The topic for the OSGi event which is sent when a tenant has been removed.</p>
     * <p>The event contains at least the {@link #PROPERTY_TENANTID}.</p>
     *
     * @since 1.1
     */
    public static final String TOPIC_TENANT_REMOVED = "org/apache/sling/tenant/REMOVED";

    /**
     * <p>The topic for the OSGi event which is sent when one or more properties have been set to a tenant.</p>
     * <p>The event contains at least the {@link #PROPERTY_TENANTID} and {@link #PROPERTY_NAMES}.</p>
     *
     * @since 1.1
     */
    public static final String TOPIC_TENANT_PROPERTY_SET = "org/apache/sling/tenant/properties/SET";

    /**
     * <p>The topic for the OSGi event which is sent when one or more properties have been removed from a tenant.</p>
     * <p>The event contains at least the {@link #PROPERTY_TENANTID} and {@link #PROPERTY_NAMES}.</p>
     *
     * @since 1.1
     */
    public static final String TOPIC_TENANT_PROPERTY_REMOVED = "org/apache/sling/tenant/properties/REMOVED";

    /**
     * <p>The name of the event property holding the identifier of the tenant affected by a change.</p>
     * <p>The value of the property is a {@code String}.</p>
     *
     * @since 1.1
     */
    public static final String PROPERTY_TENANTID = "tenantId";

    /**
     * <p>The name of the event property holding the names of the properties affected by a change.</p>
     * <p>The value of the property is a {@code String[]}.</p>
     *
     * @since 1.1
     */
    public static final String PROPERTY_NAMES = "propertyNames";

}
