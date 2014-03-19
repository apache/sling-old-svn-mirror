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
package org.apache.sling.resourceaccesssecurity;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.security.AccessSecurityException;

/**
 * This abstract implementation of the <code>ResourceAccessGate</code> can be
 * used to implement own resource access gates.
 * This implementation simply allows operations, restricting implementations
 * just need to overwrite the operations they want to restrict.
 */
public abstract class AllowingResourceAccessGate implements ResourceAccessGate {

    @Override
    public GateResult canRead(final Resource resource) {
        return GateResult.CANT_DECIDE;
    }

    @Override
    public GateResult canCreate(final String absPathName,
            final ResourceResolver resourceResolver) {
        return GateResult.CANT_DECIDE;
    }

    @Override
    public GateResult canUpdate(final Resource resource) {
        return GateResult.CANT_DECIDE;
    }

    @Override
    public GateResult canDelete(final Resource resource) {
        return GateResult.CANT_DECIDE;
    }

    @Override
    public GateResult canExecute(final Resource resource) {
        return GateResult.CANT_DECIDE;
    }

    @Override
    public GateResult canReadValue(final Resource resource, final String valueName) {
        return GateResult.CANT_DECIDE;
    }

    @Override
    public GateResult canCreateValue(final Resource resource, final String valueName) {
        return GateResult.CANT_DECIDE;
    }

    @Override
    public GateResult canUpdateValue(final Resource resource, final String valueName) {
        return GateResult.CANT_DECIDE;
    }

    @Override
    public GateResult canDeleteValue(final Resource resource, final String valueName) {
        return GateResult.CANT_DECIDE;
    }

    @Override
    public String transformQuery(final String query, final String language,
            final ResourceResolver resourceResolver) throws AccessSecurityException {
        return query;
    }

    @Override
    public boolean hasReadRestrictions(final ResourceResolver resourceResolver) {
        return false;
    }

    @Override
    public boolean hasCreateRestrictions(final ResourceResolver resourceResolver) {
        return false;
    }

    @Override
    public boolean hasUpdateRestrictions(final ResourceResolver resourceResolver) {
        return false;
    }

    @Override
    public boolean hasDeleteRestrictions(final ResourceResolver resourceResolver) {
        return false;
    }

    @Override
    public boolean hasExecuteRestrictions(final ResourceResolver resourceResolver) {
        return false;
    }

    @Override
    public boolean canReadAllValues(final Resource resource) {
        return true;
    }

    @Override
    public boolean canCreateAllValues(final Resource resource) {
        return true;
    }

    @Override
    public boolean canUpdateAllValues(final Resource resource) {
        return true;
    }

    @Override
    public boolean canDeleteAllValues(final Resource resource) {
        return true;
    }
}
