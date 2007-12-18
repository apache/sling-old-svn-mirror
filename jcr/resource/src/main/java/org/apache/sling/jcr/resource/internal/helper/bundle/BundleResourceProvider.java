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
package org.apache.sling.jcr.resource.internal.helper.bundle;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.jcr.resource.internal.JcrResourceResolver;
import org.apache.sling.jcr.resource.internal.helper.ResourceProvider;
import org.osgi.framework.Bundle;

public class BundleResourceProvider implements ResourceProvider {

    /** The bundle providing the resources */
    private final Bundle bundle;

    /** The root paths */
    private final String[] roots;

    /**
     * Creates Bundle resource provider accessing entries in the given Bundle an
     * supporting resources below root paths given by the rootList which is a
     * comma (and whitespace) separated list of absolute paths.
     */
    public BundleResourceProvider(Bundle bundle, String rootList) {
        this.bundle = bundle;

        StringTokenizer pt = new StringTokenizer(rootList, ", \t\n\r\f");
        List<String> prefixList = new ArrayList<String>();
        while (pt.hasMoreTokens()) {
            String prefix = pt.nextToken().trim();
            if (prefix.length() > 0) {
                prefixList.add(prefix);
            }
        }
        this.roots = prefixList.toArray(new String[prefixList.size()]);
    }

    /** Returns the root paths */
    public String[] getRoots() {
        return roots;
    }

    /**
     * Returns a BundleResource for the path if such an entry exists in the
     * bundle of this provider. The JcrResourceResolver is ignored by this
     * implementation.
     */
    public Resource getResource(JcrResourceResolver jrm, String path) {
        return BundleResource.getResource(bundle, path);
    }

}
