/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.sling.servlets.post.impl.helper;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.ValueFactory;

/**
 * Takes a string representation of a node (either a path or a uuid) and tries to parse it.
 * ReferenceParser is only used if JCR is available.
 */
public class ReferenceParser {

    /**
     * Parses the given source string and returns the correct Value object.
     * If no node matches returns <code>null</code>.
     * <p/>
     *
     * @param value a path or UUID
     * @param weak true to create a WeakReference value
     * @return the value or <code>null</code>
     * @throws RepositoryException
     */
    public static Value parse(Session session, String value, boolean weak) throws RepositoryException {
        Node n = parse(session, value);
        if (n == null) {
            return null;
        }
        return createReferenceValue(n, session.getValueFactory(), weak);
    }

    /**
     * Parses the given source strings and returns the respective reference value
     * instances. If no node matches for any of the sources
     * returns <code>null</code>.
     * <p/>
     *
     * @param values path or UUID strings
     * @param factory the value factory
     * @param weak true to create a WeakReference value
     * @return the values or <code>null</code>
     * @throws RepositoryException
     */
    public static Value[] parse(Session session, String[] values, boolean weak) throws RepositoryException {
        Value ret[] = new Value[values.length];
        for (int i=0; i< values.length; i++) {
            Node n = parse(session, values[i]);
            if (n == null) {
                return null;
            }
            ret[i] = createReferenceValue(n, session.getValueFactory(), weak);
        }
        return ret;
    }

    private static Value createReferenceValue(Node node, ValueFactory factory, boolean weak) throws RepositoryException {
        if (weak) {
            return factory.createValue(node, true);
        } else {
            return factory.createValue(node);
        }
    }

    private static Node parse(Session session, String value) throws RepositoryException {
        try {
            if (session.itemExists(value)) {
                return (Node) session.getItem(value);
            }
        } catch (RepositoryException ignore) {
            // we ignore this
        }
        try {
            return session.getNodeByIdentifier(value);
        } catch (RepositoryException ignore) {
            // we ignore this
        }
        return null;
    }

}
