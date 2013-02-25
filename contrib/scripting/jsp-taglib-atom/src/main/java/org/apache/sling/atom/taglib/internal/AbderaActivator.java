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
package org.apache.sling.atom.taglib.internal;

import javax.servlet.jsp.JspException;

import org.apache.abdera.Abdera;
import org.apache.abdera.ext.media.MediaExtensionFactory;
import org.apache.abdera.ext.opensearch.model.OpenSearchExtensionFactory;
import org.apache.abdera.factory.Factory;
import org.apache.abdera.parser.stax.FOMFactory;

public abstract class AbderaActivator {

    private static Abdera ABDERA;

    private static final Object LOCK = new Object();

    public static Abdera getAbdera() throws JspException {
        synchronized ( LOCK ) {
            if ( ABDERA == null ) {
                final ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
                final ClassLoader osgiClassloader = AbderaActivator.class.getClassLoader();
                Thread.currentThread().setContextClassLoader(osgiClassloader);

                try {

                    final Abdera a = new Abdera();
                    final Factory f = a.getFactory();
                    if (f instanceof FOMFactory) {
                        FOMFactory ff = (FOMFactory) f;
                        ff.registerExtension(new MediaExtensionFactory());
                        ff.registerExtension(new OpenSearchExtensionFactory());
                    }
                    ABDERA = a;

                } finally {
                    Thread.currentThread().setContextClassLoader(classLoader);
                }
            }
        }

        return ABDERA;
    }
}
