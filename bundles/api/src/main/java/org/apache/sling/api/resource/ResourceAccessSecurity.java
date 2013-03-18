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
package org.apache.sling.api.resource;


/**
 * The <code>ResourceAccessSecurity</code> defines a service API which might 
 * be used in implementations of resource providers where the underlaying 
 * persistence layer does not have any ACLs. The service should it make
 * easy to implement a lightweight access control in such sort of providers.
 *
 * - Expected to only be implemented once in the framework/application
 *   (much like the OSGi LogService or Configuration Admin Service)
 * - ResourceProvider implementations are encouraged to use 
 *   this service for access control unless the underlying
 *   storage already has it.
 *
 */

public interface ResourceAccessSecurity {
    
    public Resource checkReadPermission( Resource resource );
    public boolean canCreate( String absPathName, String user );
    public boolean canUpdate( Resource resource );
    public boolean canDelete( Resource resource );
    public boolean canExecute( Resource resource );

    public boolean canReadValue( Resource resource, String valueName );
    public boolean canCreateValue( Resource resource, String valueName );
    public boolean canUpdateValue( Resource resource, String valueName );
    public boolean canDeleteValue( Resource resource, String valueName );

    public String sanitizeQuery( String query, String language, String user ) throws AccessSecurityException;

}
