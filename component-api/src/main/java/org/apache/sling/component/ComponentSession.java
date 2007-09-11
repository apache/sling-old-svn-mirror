/*
 * Copyright 2007 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.component;

import java.util.Enumeration;

import javax.servlet.http.HttpSession;

/**
 * The <code>ComponentSession</code> interface provides a way to identify a
 * user across more than one request and to store transient information about
 * that user.
 * <p>
 * A component can bind an object attribute into a <code>ComponentSession</code>
 * by name. The <code>ComponentSession</code> interface defines two scopes for
 * storing objects:
 * <ul>
 * <li><code>APPLICATION_SCOPE</code>
 * <li><code>COMPONENT_SCOPE</code>
 * </ul>
 * All objects stored in the session using the <code>APPLICATION_SCOPE</code>
 * must be available to all the components, servlets and JSPs that belong to the
 * same component application and that handle a request identified as being a
 * part of the same session. Objects stored in the session using the
 * <code>COMPONENT_SCOPE</code> must be available to the component during
 * requests for the same content that the objects where stored from. Attributes
 * stored in the <code>COMPONENT_SCOPE</code> are not protected from other web
 * components of the component application. They are just conveniently
 * namespaced.
 * <p>
 * The component session is based on the <code>HttpSession</code>. Therefore
 * all <code>HttpSession</code> listeners do apply to the component session
 * and attributes set in the component session are visible in the
 * <code>HttpSession</code> and vice versa.
 * <p>
 * The attribute accessor methods without the <code>scope</code> parameter
 * always refer to <code>COMPONENT_SCOPE</code> attributes. To access
 * <code>APPLICATION_SCOPE</code> attributes use the accessors taking an
 * explicit <code>scope</code> parameter.
 */
public interface ComponentSession extends HttpSession {

    /**
     * This constant defines an application wide scope for the session attribute
     * (value is 1). <code>APPLICATION_SCOPE</code> session attributes enable
     * components within one component application to share data.
     * <p>
     * Components may need to prefix attributes set in this scope with some ID,
     * to avoid overwriting each other's attributes in the case where two
     * components of the same component definition are created (actually "the
     * component for different content").
     */
    static final int APPLICATION_SCOPE = 0x01;

    /**
     * This constant defines the scope of the session attribute to be private to
     * the component and its included resources (value is 2).
     */
    static final int COMPONENT_SCOPE = 0x02;

    /**
     * The string prefixed to component scoped session attributes
     */
    static final String COMPONENT_SCOPE_NAMESPACE = "org.apache.sling.core.component.p.";

    /**
     * Returns the object bound with the specified name in this session, or
     * <code>null</code> if no object is bound under the name in the given
     * scope.
     *
     * @param name a string specifying the name of the object
     * @param scope session scope of this attribute
     * @return the object with the specified name
     * @throws IllegalStateException if this method is called on an invalidated
     *             session
     * @throws IllegalArgumentException if name is <code>null</code>.
     */
    Object getAttribute(String name, int scope);

    /**
     * Returns an <code>Iterator</code> of String objects containing the names
     * of all the objects bound to this session in the given scope, or an empty
     * <code>Iterator</code> if no attributes are available in the given
     * scope.
     *
     * @param scope session scope of the attribute names
     * @return an <code>Iterator</code> of <code>String</code> objects
     *         specifying the names of all the objects bound to this session, or
     *         an empty <code>Iterator</code> if no attributes are available
     *         in the given scope.
     * @throws IllegalStateException if this method is called on an invalidated
     *             session
     */
    Enumeration<String> getAttributeNames(int scope);

    /**
     * Removes the object bound with the specified name and the given scope from
     * this session. If the session does not have an object bound with the
     * specified name, this method does nothing.
     *
     * @param name the name of the object to be removed from this session
     * @param scope session scope of this attribute
     * @throws IllegalStateException if this method is called on a session which
     *             has been invalidated
     * @throws IllegalArgumentException if name is <code>null</code>.
     */
    void removeAttribute(String name, int scope);

    /**
     * Binds an object to this session in the given scope, using the name
     * specified. If an object of the same name in this scope is already bound
     * to the session, that object is replaced.
     * <p>
     * After this method has been executed, and if the new object implements
     * <code>HttpSessionBindingListener</code>, the container calls
     * <code>HttpSessionBindingListener.valueBound</code>. The container then
     * notifies any <code>HttpSessionAttributeListeners</code> in the web
     * application.
     * <p>
     * If an object was already bound to this session that implements
     * <code>HttpSessionBindingListener</code>, its
     * <code>HttpSessionBindingListener.valueUnbound</code> method is called.
     * <p>
     * If the value is <code>null</code>, this has the same effect as calling
     * <code>removeAttribute()</code>.
     *
     * @param name the name to which the object is bound; this cannot be
     *            <code>null</code>.
     * @param value the object to be bound
     * @param scope session scope of this attribute
     * @throws IllegalStateException if this method is called on a session which
     *             has been invalidated
     * @throws IllegalArgumentException if name is <code>null</code>.
     */
    void setAttribute(String name, Object value, int scope);

    /**
     * Returns the component context associated with this session.
     *
     * @return the component context
     */
    ComponentContext getComponentContext();
}
