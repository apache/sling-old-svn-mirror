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
package org.apache.sling.resourceresolver.impl;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.jcr.NamespaceException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.sling.api.resource.ResourceResolver;
import org.slf4j.Logger;

public class JcrNamespaceMangler {

    private static final String MANGLE_NAMESPACE_IN_SUFFIX = "_";

    private static final String MANGLE_NAMESPACE_IN_PREFIX = "/_";

    private static final Pattern MANGLE_NAMESPACE_IN_PATTERN = Pattern.compile("/_([^_/]+)_");

    private static final String MANGLE_NAMESPACE_OUT_SUFFIX = ":";

    private static final String MANGLE_NAMESPACE_OUT_PREFIX = "/";

    private static final Pattern MANLE_NAMESPACE_OUT_PATTERN = Pattern.compile("/([^:/]+):");


    public String mangleNamespaces(ResourceResolver resolver, Logger logger, String absPath) {
        if (absPath.contains(MANGLE_NAMESPACE_OUT_SUFFIX)) {
            final Session session = resolver.adaptTo(Session.class);
            if ( session != null ) {
                final Matcher m = MANLE_NAMESPACE_OUT_PATTERN.matcher(absPath);

                final StringBuffer buf = new StringBuffer();
                while (m.find()) {
                    final String namespace = m.group(1);
                    try {

                        // throws if "namespace" is not a registered
                        // namespace prefix
                        session.getNamespaceURI(namespace);
                        final String replacement = MANGLE_NAMESPACE_IN_PREFIX + namespace + MANGLE_NAMESPACE_IN_SUFFIX;
                        m.appendReplacement(buf, replacement);


                    } catch (final NamespaceException ne) {

                        // not a valid prefix
                        logger.debug("mangleNamespaces: '{}' is not a prefix, not mangling", namespace);

                    } catch (final RepositoryException re) {

                        logger.warn("mangleNamespaces: Problem checking namespace '{}'", namespace, re);

                    }
                }

                m.appendTail(buf);

                absPath = buf.toString();
            }
        }

        return absPath;
    }

    public String unmangleNamespaces(ResourceResolver resolver, Logger logger, String absPath) {
        if (absPath.contains(MANGLE_NAMESPACE_IN_PREFIX)) {
            final Session session = resolver.adaptTo(Session.class);
            if ( session != null ) {
                final Matcher m = MANGLE_NAMESPACE_IN_PATTERN.matcher(absPath);
                final StringBuffer buf = new StringBuffer();
                while (m.find()) {
                    final String namespace = m.group(1);
                    try {

                        // throws if "namespace" is not a registered
                        // namespace prefix
                        session.getNamespaceURI(namespace);
                        final String replacement = MANGLE_NAMESPACE_OUT_PREFIX + namespace + MANGLE_NAMESPACE_OUT_SUFFIX;
                        m.appendReplacement(buf, replacement);


                    } catch (final NamespaceException ne) {

                        // not a valid prefix
                        logger.debug("unmangleNamespaces: '{}' is not a prefix, not unmangling", namespace);

                    } catch (final RepositoryException re) {

                        logger.warn("unmangleNamespaces: Problem checking namespace '{}'", namespace, re);

                    }
                }
                m.appendTail(buf);
                absPath = buf.toString();
            }
        }

        return absPath;
    }
}
