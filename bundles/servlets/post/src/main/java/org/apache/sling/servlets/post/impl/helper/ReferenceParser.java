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

import java.lang.reflect.Method;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.ValueFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Takes a string representation of a node (either a path or a uuid) and tries for parse it.
 */
public class ReferenceParser {

    private final Session session;

    private static final Logger logger = LoggerFactory.getLogger(ReferenceParser.class);

    public ReferenceParser(Session session) {
        this.session = session;
    }

    /**
     * Parses the given source string and returns the correct Value object.
     * If no node matches returns <code>null</code>.
     * <p/>
     *
     * @param value a path or UUID
     * @param factory the value factory
     * @param weak true to create a WeakReference value
     * @return the value or <code>null</code>
     * @throws RepositoryException
     */
    public Value parse(String value, ValueFactory factory, boolean weak) throws RepositoryException {
        Node n = parse(value);
        if (n == null) {
            return null;
        }
        return createReferenceValue(n, factory, weak);
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
    public Value[] parse(String[] values, ValueFactory factory, boolean weak) throws RepositoryException {
        Value ret[] = new Value[values.length];
        for (int i=0; i< values.length; i++) {
            Node n = parse(values[i]);
            if (n == null) {
                return null;
            }
            ret[i] = createReferenceValue(n, factory, weak);
        }
        return ret;
    }

    private Value createReferenceValue(Node node, ValueFactory factory, boolean weak) throws RepositoryException {
        if (weak) {
            try {
                final Method m = factory.getClass().getMethod("createValue", new Class[] { Node.class, Boolean.TYPE });
                return (Value) m.invoke(factory, node, true);
            } catch (NoSuchMethodException e) {
                logger.warn("A WeakReference type hint was received, but JCR 2 isn't available. Falling back to Reference type.");
                return factory.createValue(node);
            } catch (Exception e) {
                logger.error("Unable to create WeakReference Value.", e);
                return null;
            }
        } else {
            return factory.createValue(node);
        }
    }

    private Node parse(String value) throws RepositoryException {
        try {
            if (session.itemExists(value)) {
                return (Node) session.getItem(value);
            }
        } catch (RepositoryException ignore) {
            // we ignore this
        }
        try {
            return session.getNodeByUUID(value);
        } catch (RepositoryException ignore) {
            // we ignore this
        }
        return null;
    }

}
