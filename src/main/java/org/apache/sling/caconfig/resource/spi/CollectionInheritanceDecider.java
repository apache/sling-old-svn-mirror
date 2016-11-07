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
package org.apache.sling.caconfig.resource.spi;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

import org.apache.sling.api.resource.Resource;
import org.osgi.annotation.versioning.ConsumerType;

/**
 * The {@code CollectionInheritanceDecider} is an SPI which should be used by
 * all {@link ConfigurationResourceResolvingStrategy} implementations if they
 * support inheritance for collections.
 *
 * @since 1.1
 */
@ConsumerType
public interface CollectionInheritanceDecider {

    /**
     * Decide whether the provided resource should be included in the collection.
     * The provided resource can either be included, excluded or blocked.
     * If the decider can't decide it must return {@code null}.
     *
     * @param bucketName The bucket name
     * @param resource The resource
     * @return The decision or {@code null}
     */
    @CheckForNull InheritanceDecision decide(@Nonnull Resource resource, @Nonnull String bucketName);

}
