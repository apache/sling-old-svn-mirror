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
package org.apache.sling.serviceusermapping;

import aQute.bnd.annotation.ProviderType;

/**
 * The <code>ServiceUserMapped</code> is a marker service that can be used to ensure that there is an already registered mapping for a certain service/subService.
 * A service reference targeting a <code>ServiceUserMapped</code> will be satisfied only if <code>ServiceUserMapper.getServiceUserID</code>
 * will resolve the subService to an userID.
 * For example setting the reference target to "(subServiceName=mySubService)" ensures that your component only starts when the subService is available.
 * The subServiceName will not be set for mappings that do not have one, and those can be referenced with a negating target "(!(subServiceName=*))".
 * Trying to reference a sub service from a bundle for which it was not registered for will not work.
 *
 * As the service user mapper implementation is using a fallback, it is usually best to use a reference target that includes both
 * options, the sub service name and the fallback, therefore a target like "(|((subServiceName=mySubService)(!(subServiceName=*))))" should be used.
 */
@ProviderType
public interface ServiceUserMapped {


    /**
     * The name of the osgi property holding the sub service name.
     */
    static String SUBSERVICENAME = "subServiceName";

}
