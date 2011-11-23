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
package org.apache.sling.jcr.resource.internal;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.auth.core.spi.AbstractAuthenticationHandler;
import org.apache.sling.auth.core.spi.AuthenticationInfo;
import org.apache.sling.auth.core.spi.AuthenticationInfoPostProcessor;
import org.apache.sling.jcr.resource.JcrResourceConstants;
import org.osgi.framework.Constants;

/**
 * The <code>WorkspaceAuthInfoPostProcessor</code> is a simple
 * AuthenticationInfo post processor which sets the
 * {@link JcrResourceConstants#AUTHENTICATION_INFO_WORKSPACE} property (unless
 * set already) if the {@link #J_WORKSPACE} request attribute or parameter is
 * set to a non-empty string.
 * <p>
 * This allows logging into any workspace for a given request provided the
 * requested workspace exists.
 */
@Component
@Service
@Property(name = Constants.SERVICE_DESCRIPTION, value = "JCR Workspace property setter")
public class WorkspaceAuthInfoPostProcessor implements AuthenticationInfoPostProcessor {

    /**
     * The name of the request parameter (or request attribute) indicating the
     * workspace to use.
     * <p>
     * The {@link AuthenticationSupport} service implemented by this bundle will
     * respect this parameter and attribute and ensure the
     * <code>jcr.user.workspace</code> attribute of the
     * {@link org.apache.sling.auth.core.spi.AuthenticationInfo} used for
     * accessing the resource resolver is set to this value (unless the property
     * has already been set by the
     * {@link org.apache.sling.auth.core.spi.AuthenticationHandler} providing
     * the {@link org.apache.sling.auth.core.spi.AuthenticationInfo} instance).
     */
    public static final String J_WORKSPACE = "j_workspace";

    /**
     * Sets the {@link JcrResourceConstants#AUTHENTICATION_INFO_WORKSPACE} if
     * the {@link #J_WORKSPACE} request parameter or attribute is defined and
     * the {@link JcrResourceConstants#AUTHENTICATION_INFO_WORKSPACE} does not
     * exist yet in the authentication info.
     */
    public void postProcess(AuthenticationInfo info, HttpServletRequest request, HttpServletResponse response) {
        final String workspace = AbstractAuthenticationHandler.getAttributeOrParameter(request, J_WORKSPACE, "");
        if (workspace.length() > 0 && !info.containsKey(JcrResourceConstants.AUTHENTICATION_INFO_WORKSPACE)) {
            info.put(JcrResourceConstants.AUTHENTICATION_INFO_WORKSPACE, workspace);
        }
    }
}
