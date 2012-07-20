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
package org.apache.sling.installer.core.impl.tasks;

import org.apache.sling.installer.api.tasks.TaskResource;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;

public abstract class BundleUtil {

    private static final String ATTR_START = "sling.osgi.installer.start.bundle";

    public static void markBundleStart(final TaskResource rsrc) {
        rsrc.setAttribute(ATTR_START, "true");
    }

    public static boolean isBundleStart(final TaskResource rsrc) {
        return rsrc.getAttribute(ATTR_START) != null;
    }

    public static void clearBundleStart(final TaskResource rsrc) {
        rsrc.setAttribute(ATTR_START, null);
    }

    public static boolean isSystemBundleFragment(final Bundle installedBundle) {
        final String fragmentHeader = getFragmentHostHeader(installedBundle);
        return fragmentHeader != null
            && fragmentHeader.indexOf(Constants.EXTENSION_DIRECTIVE) > 0;
    }

    /**
     * Check if the bundle is active.
     * This is true if the bundle has the active state or of the bundle
     * is in the starting state and has the lazy activation policy.
     * Or if the bundle is a fragment, it's considered active as well
     */
    public static boolean isBundleActive(final Bundle b) {
        if ( b.getState() == Bundle.ACTIVE ) {
            return true;
        }
        if ( b.getState() == Bundle.STARTING && isLazyActivatian(b) ) {
            return true;
        }
        return ( getFragmentHostHeader(b) != null );
    }

    /**
     * Gets the bundle's Fragment-Host header.
     */
    public static String getFragmentHostHeader(final Bundle b) {
        return (String) b.getHeaders().get( Constants.FRAGMENT_HOST );
    }

    /**
     * Check if the bundle has the lazy activation policy
     */
    public static boolean isLazyActivatian(final Bundle b) {
        return Constants.ACTIVATION_LAZY.equals(b.getHeaders().get(Constants.BUNDLE_ACTIVATIONPOLICY));
    }
}
