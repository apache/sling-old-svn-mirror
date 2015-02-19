/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.serviceusermapping;

import aQute.bnd.annotation.ConsumerType;

/**
 * The {@code ServiceUserValidator} allows to implement validation of configured
 * service user mappings.
 */
@ConsumerType
public interface ServiceUserValidator {

    /**
     * Validates the configured service user ID.
     *
     * @param serviceUserId The ID of the configured service user.
     * @return {@code true} if the configured service user is valid; {@code false} otherwise.
     */
    boolean isValid(String serviceUserId, String serviceName, String subServiceName);
}