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
package org.apache.sling.event.dea;


/**
 * The <code>DEAConstants</code> provides some constants for
 * handling distributed OSGi events.
 * <p>
 * If an event should be sent to other instances, the event
 * property {@link #PROPERTY_DISTRIBUTE} should be set to
 * an empty string.
 * <p>
 * An event, regardless if distributed or not, should never be
 * created with the property {@link #PROPERTY_APPLICATION}. In
 * addition properties starting with "event.dea." are reserved
 * attributes of this implementation and must not be used
 * by custom events.
 * <p>
 * If the event is a local event, the {@link #PROPERTY_APPLICATION}
 * is not available. If it is available, it contains the application
 * (Sling ID) of the instance where the event originated.
 */
public abstract class DEAConstants {

    /** This event property indicates, if the event should be distributed in the cluster. */
    public static final String PROPERTY_DISTRIBUTE = "event.distribute";

    /** This event property specifies the application node. */
    public static final String PROPERTY_APPLICATION = "event.application";
}