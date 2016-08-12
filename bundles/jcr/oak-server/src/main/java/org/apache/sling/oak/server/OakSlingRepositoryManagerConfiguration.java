/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The SF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.apache.sling.oak.server;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(
    name = "Apache Sling JCR Oak Repository",
    description = "Configuration to launch an embedded JCR Repository and provide it as a SlingRepository and a standard JCR Repository. In addition, if the registration URL is not empty, the repository is registered as defined."
)
@interface OakSlingRepositoryManagerConfiguration {

    @AttributeDefinition(
        name = "Repository Name",
        description = "The name under which the repository will be registered in JNDI and RMI registries."
    )
    String name() default "oak-sling-repository";

    @AttributeDefinition(
        name = "Default Workspace",
        description = "Name of the workspace to use by default if not is given in any of the login methods. This name is used "
            + "to implement the SlingRepository.getDefaultWorkspace() "
            + "method. If this name is empty, a null value is used in "
            + "JCR calls so that the default workspace provided by the JCR repository is used."
    )
    String defaultWorkspace() default "default";

    // For backwards compatibility loginAdministrative is still enabled
    // In future releases, this default may change to false.
    @AttributeDefinition(
        name = "Enable Administrator Login",
        description = "Whether to enable or disable the SlingRepository.loginAdministrative "
            + "method. The default is 'true'. See "
            + "http://sling.apache.org/documentation/the-sling-engine/service-authentication.html "
            + "for information on deprecating and disabling the loginAdministrative method."
    )
    boolean admin_login_enabled() default true;

    @AttributeDefinition(
        name = "Allow anonymous reads",
        description = "If true, the anonymous user has read access to the whole repository (for backwards compatibility)"
    )
    boolean anonymous_read_all() default true;

    @AttributeDefinition(
        name = "Observation queue length",
        description = "Maximum number of pending revisions in a observation listener queue"
    )
    int oak_observation_queue_length() default 1000;

    @AttributeDefinition(
        name = "Commit rate limiter",
        description = "Limit the commit rate once the number of pending revisions in the observation "
            + "queue exceed 90% of its capacity."
    )
    boolean oak_observation_limitCommitRate() default false;

}
