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
package org.apache.sling.distribution.serialization.impl;

import java.util.List;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.distribution.component.impl.DistributionComponent;
import org.apache.sling.distribution.component.impl.DistributionComponentKind;
import org.apache.sling.distribution.component.impl.DistributionComponentProvider;
import org.apache.sling.distribution.serialization.DistributionPackageBuilder;
import org.apache.sling.distribution.serialization.DistributionPackageBuilderProvider;

@Component
@Service(DistributionPackageBuilderProvider.class)
public class DefaultDistributionPackageBuilderProvider implements DistributionPackageBuilderProvider {

    @Reference
    private
    DistributionComponentProvider componentProvider;

    public DistributionPackageBuilder getPackageBuilder(String type) {
        List<DistributionComponent> componentList = componentProvider.getComponents(DistributionComponentKind.PACKAGE_BUILDER);

        return filterPackageBuildersByType(componentList, type);
    }

    private static DistributionPackageBuilder filterPackageBuildersByType(List<DistributionComponent> componentList, String type) {

        if (type == null) {
            return null;
        }

        for (DistributionComponent component : componentList) {
            Object service = component.getService();

            if (service instanceof DistributionPackageBuilder) {
                DistributionPackageBuilder packageBuilder = (DistributionPackageBuilder) service;

                if (type.equals(packageBuilder.getType())) {
                    return packageBuilder;
                }
            }
        }

        return null;
    }
}
