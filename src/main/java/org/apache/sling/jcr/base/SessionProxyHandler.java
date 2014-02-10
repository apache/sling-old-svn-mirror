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
package org.apache.sling.jcr.base;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashSet;
import java.util.Set;

import javax.jcr.Credentials;
import javax.jcr.Session;

/**
 * The session proxy handler creates session proxies to handle
 * the namespace mapping support if impersonate is called on
 * the session.
 * <p>
 * This class is not really part of the API is not intended to be used
 * directly by consumers or implementors of this API. It is used internally
 * to support namespace mapping.
 */
public class SessionProxyHandler  {

    /** The array of proxied interfaces. */
    private Class<?>[] interfaces;

    /** The namespaceSupport */
    private final NamespaceMappingSupport namespaceSupport;

    public SessionProxyHandler(final NamespaceMappingSupport namespaceSupport) {
        this.namespaceSupport = namespaceSupport;
    }

    /** Calculate the interfaces.
     * This is done only once - we simply assume that the same namespaceSupport is
     * emitting session from the same class.
     */
    private Class<?>[] getInterfaces(final Class<?> sessionClass) {
        if ( interfaces == null ) {
            synchronized ( SessionProxyHandler.class ) {
                if ( interfaces == null ) {
                    final HashSet<Class<?>> workInterfaces = new HashSet<Class<?>>();

                    // Get *all* interfaces
                    guessWorkInterfaces( sessionClass, workInterfaces );

                    this.interfaces = workInterfaces.toArray( new Class[workInterfaces.size()] );

                }
            }
        }
        return interfaces;
    }

    /**
     * Create a proxy for the session.
     */
    public Session createProxy(final Session session) {
        final Class<?> sessionClass = session.getClass();
        final Class<?>[] interfaces = getInterfaces(sessionClass);
        return (Session)Proxy.newProxyInstance(sessionClass.getClassLoader(),
                interfaces,
                new SessionProxyInvocationHandler(session, this.namespaceSupport, interfaces));

    }


    public static final class SessionProxyInvocationHandler implements InvocationHandler {
        private final Session delegatee;
        private final NamespaceMappingSupport namespaceSupport;
        private final Class<?>[] interfaces;

        public SessionProxyInvocationHandler(final Session delegatee,
                            final NamespaceMappingSupport namespaceSupport,
                            final Class<?>[] interfaces) {
            this.delegatee = delegatee;
            this.namespaceSupport = namespaceSupport;
            this.interfaces = interfaces;
        }

        /**
         * @see java.lang.reflect.InvocationHandler#invoke(java.lang.Object, java.lang.reflect.Method, java.lang.Object[])
         */
        public Object invoke(Object proxy, Method method, Object[] args)
        throws Throwable {
            if ( method.getName().equals("impersonate") && args != null && args.length == 1) {
                final Session session = this.delegatee.impersonate((Credentials)args[0]);
                this.namespaceSupport.defineNamespacePrefixes(session);
                final Class<?> sessionClass = session.getClass();
                return Proxy.newProxyInstance(sessionClass.getClassLoader(),
                        interfaces,
                        new SessionProxyInvocationHandler(session, this.namespaceSupport, interfaces));
            }
            try {
                return method.invoke(this.delegatee, args);
            } catch (InvocationTargetException ite) {
                throw ite.getTargetException();
            }
        }
    }

    /**
     * Get a list of interfaces to proxy by scanning through
     * all interfaces a class implements.
     *
     * @param clazz           the class
     * @param workInterfaces  the set of current work interfaces
     */
    private void guessWorkInterfaces( final Class<?> clazz,
                                      final Set<Class<?>> workInterfaces ) {
        if ( null != clazz ) {
            addInterfaces( clazz.getInterfaces(), workInterfaces );

            guessWorkInterfaces( clazz.getSuperclass(), workInterfaces );
        }
    }

    /**
     * Get a list of interfaces to proxy by scanning through
     * all interfaces a class implements.
     *
     * @param classInterfaces the array of interfaces
     * @param workInterfaces  the set of current work interfaces
     */
    private void addInterfaces( final Class<?>[] classInterfaces,
                                final Set<Class<?>> workInterfaces ) {
        for ( int i = 0; i < classInterfaces.length; i++ ) {
            workInterfaces.add( classInterfaces[i] );
            addInterfaces(classInterfaces[i].getInterfaces(), workInterfaces);
        }
    }
}
