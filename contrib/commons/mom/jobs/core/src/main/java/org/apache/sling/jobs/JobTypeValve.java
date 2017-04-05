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
package org.apache.sling.jobs;

import org.osgi.annotation.versioning.ProviderType;

import javax.annotation.Nonnull;

/**
 * An interface to allow a component, normally a JobConsumer to inspect a JobType and indicate that
 * it can perform further actions on it. JobTypeValves are used in place of static JobType declarations.
 * This interface is only implemented where static declaration of JobTypes does not satisfy the use cases.
 */
@ProviderType
public interface JobTypeValve {

    /**
     * Return true if the component, normally a JobConsumer, can process the jobType.
     * @param jobType the job type.
     * @return true if can be processed.
     */
    boolean accept(@Nonnull Types.JobType jobType);
}
