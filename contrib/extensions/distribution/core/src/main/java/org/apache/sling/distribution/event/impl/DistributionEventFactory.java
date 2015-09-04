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
package org.apache.sling.distribution.event.impl;

import javax.annotation.Nonnull;

import org.apache.sling.distribution.component.impl.DistributionComponentKind;
import org.apache.sling.distribution.packaging.DistributionPackageInfo;

/**
 * generate distribution related events
 */
public interface DistributionEventFactory {

    /**
     * generate a distribution event
     *
     * @param distributionEventType the type of event to be generated
     */
    void generatePackageEvent(@Nonnull String distributionEventType, @Nonnull DistributionComponentKind kind,
                              @Nonnull String name, @Nonnull DistributionPackageInfo info);

}
