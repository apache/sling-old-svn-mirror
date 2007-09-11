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
package org.apache.sling.core.impl;

import java.util.Enumeration;
import java.util.NoSuchElementException;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionContext;

import org.apache.sling.component.ComponentContext;
import org.apache.sling.component.ComponentSession;
import org.apache.sling.component.ComponentSessionUtil;

/**
 * The <code>ComponentSessionImpl</code> TODO
 */
public class ComponentSessionImpl implements ComponentSession {

    private final ComponentContext componentContext;
    private final HttpSession delegatee;

    /**
     *
     */
    public ComponentSessionImpl(ComponentContext componentContext, HttpSession delegatee) {
        this.componentContext = componentContext;
        this.delegatee = delegatee;
    }

    /**
     * @see javax.servlet.http.HttpSession#getAttribute(java.lang.String)
     */
    public Object getAttribute(String name) {
        return this.getAttribute(name, COMPONENT_SCOPE);
    }

    /**
     * @see org.apache.sling.core.component.ComponentSession#getAttribute(java.lang.String, int)
     */
    public Object getAttribute(String name, int scope) {
        return this.delegatee.getAttribute(this.toAttributeName(name, scope));
    }

    /**
     * @see javax.servlet.http.HttpSession#getAttributeNames()
     */
    public Enumeration getAttributeNames() {
        return this.getAttributeNames(COMPONENT_SCOPE);
    }

    /**
     * @see org.apache.sling.core.component.ComponentSession#getAttributeNames(int)
     */
    public Enumeration getAttributeNames(final int scope) {
        return new AttributeNamesEnumeration(this.delegatee.getAttributeNames(), scope);
    }

    /**
     * @see org.apache.sling.core.component.ComponentSession#getComponentContext()
     */
    public ComponentContext getComponentContext() {
        return this.componentContext;
    }

    /**
     * @see javax.servlet.http.HttpSession#getCreationTime()
     */
    public long getCreationTime() {
        return this.delegatee.getCreationTime();
    }

    /**
     * @see javax.servlet.http.HttpSession#getId()
     */
    public String getId() {
        return this.delegatee.getId();
    }

    /**
     * @see javax.servlet.http.HttpSession#getLastAccessedTime()
     */
    public long getLastAccessedTime() {
        return this.delegatee.getLastAccessedTime();
    }

    /**
     * @see javax.servlet.http.HttpSession#getMaxInactiveInterval()
     */
    public int getMaxInactiveInterval() {
        return this.delegatee.getMaxInactiveInterval();
    }

    /**
     * @see javax.servlet.http.HttpSession#invalidate()
     */
    public void invalidate() {
        this.delegatee.invalidate();
    }

    /**
     * @see javax.servlet.http.HttpSession#isNew()
     */
    public boolean isNew() {
        return this.delegatee.isNew();
    }

    /**
     * @see javax.servlet.http.HttpSession#removeAttribute(java.lang.String)
     */
    public void removeAttribute(String name) {
        this.removeAttribute(name, COMPONENT_SCOPE);
    }

    /**
     * @see org.apache.sling.core.component.ComponentSession#removeAttribute(java.lang.String, int)
     */
    public void removeAttribute(String name, int scope) {
        this.delegatee.removeAttribute(this.toAttributeName(name, scope));
    }

    /**
     * @see javax.servlet.http.HttpSession#setAttribute(java.lang.String, java.lang.Object)
     */
    public void setAttribute(String name, Object value) {
        this.setAttribute(name, value, COMPONENT_SCOPE);
    }

    /**
     * @see org.apache.sling.core.component.ComponentSession#setAttribute(java.lang.String, java.lang.Object, int)
     */
    public void setAttribute(String name, Object value, int scope) {
        this.delegatee.setAttribute(this.toAttributeName(name, scope), value);
    }

    /**
     * @see javax.servlet.http.HttpSession#setMaxInactiveInterval(int)
     */
    public void setMaxInactiveInterval(int interval) {
        this.delegatee.setMaxInactiveInterval(interval);
    }

    public ServletContext getServletContext() {
        return this.delegatee.getServletContext();
    }

    public HttpSessionContext getSessionContext() {
        return null;
    }

    public Object getValue(String name) {
        return this.delegatee.getValue(name);
    }

    public String[] getValueNames() {
        return this.delegatee.getValueNames();
    }

    public void putValue(String name, Object value) {
        this.delegatee.putValue(name, value);
    }

    public void removeValue(String name) {
        this.delegatee.removeValue(name);
    }

    //---------- internal -----------------------------------------------------

    /**
     * Encodes the atttribute name using the correct scope prefix if the scope
     * is component scope. Otherwise the name is returned unmodified.
     *
     * @param name The attribute name to encode
     * @param scope The scope in which the attribute should be placed. May be
     *      <code>COMPONENT_SCOPE</code> or <code>APPLICATION_SCOPE</code>, any
     *      other value is assuemd to be <code>APPLICATION_SCOPE</code>.
     *
     * @return The encoded name
     */
    private String toAttributeName(String name, int scope) {
        // encode if scope is component scope
        if (scope == COMPONENT_SCOPE) {
            // template: <SCOPE>.<ID>?<NAME>
            // ID is currently always 0, will have to defined somehow ...
            return ComponentSession.COMPONENT_SCOPE_NAMESPACE + "0?" + name;
        }

        // otherwise return unmodified
        return name;
    }

    private static class AttributeNamesEnumeration implements Enumeration {
        private final Enumeration delegatee;
        private int scope;
        private String nextName;

        AttributeNamesEnumeration(Enumeration delegatee, int scope) {
            this.delegatee = delegatee;
            this.scope = scope;
            this.nextName = this.seek();
        }

        public boolean hasMoreElements() {
            return this.nextName != null;
        }

        public Object nextElement() {
            if (!this.hasMoreElements()) {
                throw new NoSuchElementException();
            }

            String toReturn = this.nextName;
            this.nextName = this.seek();
            return toReturn;
        }

        public void remove() {
            throw new UnsupportedOperationException("remove");
        }

        private String seek() {
            while (this.delegatee.hasMoreElements()) {
                String nextName = (String) this.delegatee.nextElement();
                if (ComponentSessionUtil.decodeScope(nextName) == this.scope) {
                    return ComponentSessionUtil.decodeAttributeName(nextName);
                }
            }

            // no name found anymore
            return null;
        }
    }
}
