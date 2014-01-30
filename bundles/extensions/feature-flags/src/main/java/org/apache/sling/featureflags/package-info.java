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

/**
 * The <i>Feature Flags</i> feature allows applications to dynamically
 * provide features to clients and consumers depending on various criteria such as
 * <ul>
 * <li>Time of Day</li>
 * <li>Static Configuration</li>
 * <li>Request Parameter</li>
 * <li>Some Resource</li>
 * </ul>
 * <p>
 * Feature flag support consists of two parts: The feature flag itself represented
 * by the {@link org.apache.sling.featureflags.Feature Feature} interface and the
 * the application providing a feature guarded by a feature flag. Such applications
 * make use of the {@link org.apache.sling.featureflags.Features Features} service to
 * query feature flags.
 * <p>
 * Feature flags can be provided by registering
 * {@link org.apache.sling.featureflags.Feature Feature} services. Alternatively
 * feature flags can be provided by factory configuration with factory PID
 * {@code org.apache.sling.featureflags.Feature} as follows:
 * <table>
 *  <tr>
 *      <td>{@code name}</td>
 *      <td>Short name of the feature. This name is used to refer to the feature
 *          when checking for it to be enabled or not. This property is required
 *          and defaults to a name derived from the feature's class name and object
 *          identity. It is strongly recommended to define a useful and unique name
 *          for the feature</td>
 *  </tr>
 *  <tr>
 *      <td>{@code description}</td>
 *      <td>Description for the feature. The intent is to describe the behavior
 *          of the application if this feature would be enabled. It is recommended
 *          to define this property. The default value is the name of the feature
 *          as derived from the {@code name} property.</td>
 *  </tr>
 *  <tr>
 *      <td>{@code enabled}</td>
 *      <td>Boolean flag indicating whether the feature is enabled or not by
 *          this configuration</td>
 *  </tr>
 * </table>
 *
 * @version 1.0
 * @see <a href="http://sling.apache.org/documentation/the-sling-engine/featureflags.html">Feature Flags</a>
 */
@Version("1.0")
package org.apache.sling.featureflags;

import aQute.bnd.annotation.Version;

