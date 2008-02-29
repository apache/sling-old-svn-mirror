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
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.StringTokenizer;

import javax.servlet.http.HttpServletRequest;

import org.apache.sling.api.SlingException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceProvider;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.SyntheticResource;
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

    public Resource getResource(ResourceResolver resourceResolver,
            HttpServletRequest request, String path) {
        return getResource(resourceResolver, path);
    }

    /**
     * Returns a BundleResource for the path if such an entry exists in the
     * bundle of this provider. The JcrResourceResolver is ignored by this
     * implementation.
     */
    public Resource getResource(ResourceResolver resourceResolver, String path) {
        return BundleResource.getResource(resourceResolver, bundle, path);
    }

    public Iterator<Resource> listChildren(final Resource parent)
            throws SlingException {

        // bundle resources can handle this request directly
        if (parent instanceof BundleResource) {
            return ((BundleResource) parent).listChildren();
        }

        // otherwise create an iterator which builds the entries based on
        // the contents of the roots list
        return new Iterator<Resource>() {
            private final String parentPath;

            private final ResourceResolver resolver;

            private final Iterator<String> roots;

            private Resource nextResult;
            
            private final Set<String> visited = new HashSet<String>();
            {
                String pp = parent.getPath();
                if (!pp.endsWith("/")) {
                    pp = pp.concat("/");
                }
                parentPath = pp;
                resolver = parent.getResourceResolver();
                roots = Arrays.asList(getRoots()).iterator();
                nextResult = seek();
            }

            public boolean hasNext() {
                return nextResult != null;
            }

            public Resource next() {
                if (nextResult == null) {
                    throw new NoSuchElementException();
                }
                
                Resource result = nextResult;
                nextResult = seek();
                return result;
            }

            public void remove() {
                throw new UnsupportedOperationException("remove");
            }

            private Resource seek() {
                Resource result = null;
                while (result == null && roots.hasNext()) {
                    String path = roots.next();
                    if (path.startsWith(parentPath)) {
                        int nextSlash = path.indexOf('/', parentPath.length());
                        if (nextSlash < 0) {
                            result = BundleResource.getResource(resolver,
                                bundle, path);
                        } else {

                            path = path.substring(0, nextSlash);
                            if (!visited.contains(path)) {
                                visited.add(path);
                                if (resolver.getResource(path) == null) {
                                    result = new SyntheticResource(resolver,
                                        path, RESOURCE_TYPE_SYNTHETIC);
                                }
                            }
                        }
                    }

                }
                
                return result;
            }
        };

    }
}
