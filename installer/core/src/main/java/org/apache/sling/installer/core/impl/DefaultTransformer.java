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
package org.apache.sling.installer.core.impl;

import java.util.HashMap;
import java.util.Map;

import org.apache.sling.installer.api.InstallableResource;
import org.apache.sling.installer.api.ResourceChangeListener;
import org.apache.sling.installer.api.tasks.RegisteredResource;
import org.apache.sling.installer.api.tasks.ResourceTransformer;
import org.apache.sling.installer.api.tasks.RetryHandler;
import org.apache.sling.installer.api.tasks.TransformationResult;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The default transformer transforms:
 * - file resources containing a bundle into OSGI bundle resources
 */
public class DefaultTransformer
    implements InternalService, ResourceTransformer {

    /** Logger */
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    /**
     * @see org.apache.sling.installer.core.impl.InternalService#init(org.osgi.framework.BundleContext, org.apache.sling.installer.api.ResourceChangeListener, RetryHandler)
     */
    public void init(final BundleContext bctx, final ResourceChangeListener rcl, RetryHandler retryHandler) {
        // nothing to do
    }

    /**
     * @see org.apache.sling.installer.core.impl.InternalService#deactivate()
     */
    public void deactivate() {
        // nothing to do
    }

    /**
     * @see org.apache.sling.installer.core.impl.InternalService#getDescription()
     */
    public String getDescription() {
        return "Apache Sling Installer - Default Resource Transformer";
    }

    /**
     * @see org.apache.sling.installer.api.tasks.ResourceTransformer#transform(org.apache.sling.installer.api.tasks.RegisteredResource)
     */
    public TransformationResult[] transform(final RegisteredResource resource) {
        if ( resource.getType().equals(InstallableResource.TYPE_FILE) ) {
            return checkBundle(resource);
        }
        return null;
    }

    /**
     * Check if the registered resource is a bundle.
     * @return
     */
    private TransformationResult[] checkBundle(final RegisteredResource resource) {
        final Util.BundleHeaders headers = Util.readBundleHeaders(resource, logger);
        if ( headers != null ) {
            // check the version for validity
            boolean validVersion = true;
            try {
                new Version(headers.version);
            } catch (final IllegalArgumentException iae) {
                logger.info("Rejecting bundle {} from {} due to invalid version information: {}.", 
                        new Object[] {headers.symbolicName, resource, headers.version});
                validVersion = false;
            }
            if ( validVersion ) {
                final Map<String, Object> attr = new HashMap<String, Object>();
                attr.put(Constants.BUNDLE_SYMBOLICNAME, headers.symbolicName);
                attr.put(Constants.BUNDLE_VERSION, headers.version);

                // check for activation policy
                if ( headers.activationPolicy != null ) {
                    attr.put(Constants.BUNDLE_ACTIVATIONPOLICY, headers.activationPolicy);
                }

                final TransformationResult tr = new TransformationResult();
                tr.setId(headers.symbolicName);
                tr.setResourceType(InstallableResource.TYPE_BUNDLE);
                tr.setAttributes(attr);

                return new TransformationResult[] {tr};
            }
        }
        return null;
    }
}
