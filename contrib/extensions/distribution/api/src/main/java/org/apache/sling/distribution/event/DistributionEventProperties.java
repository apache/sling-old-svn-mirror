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
package org.apache.sling.distribution.event;


import aQute.bnd.annotation.ProviderType;

/**
 * an interface containing of the possible properties of events related to distribution
 */
@ProviderType
public interface DistributionEventProperties {

    /**
     * property containing the name of component generating the event
     */
    public static final String DISTRIBUTION_COMPONENT_NAME = "distribution.component.name";


    /**
     * property containing the kind of the component generating the event
     */
    public static final String DISTRIBUTION_COMPONENT_KIND= "distribution.component.kind";

    /**
     * property containing the type of the distribution request
     */
    public static final String DISTRIBUTION_TYPE = "distribution.type";

    /**
     * property containing the type of the distribution paths
     */
    public static final String DISTRIBUTION_PATHS= "distribution.paths";
}
