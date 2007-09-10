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
package org.apache.sling.core;

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

    /* (non-Javadoc)
     * @see com.day.components.ComponentSession#getAttribute(java.lang.String)
     */
    public Object getAttribute(String name) {
        return getAttribute(name, COMPONENT_SCOPE);
    }

    /* (non-Javadoc)
     * @see com.day.components.ComponentSession#getAttribute(java.lang.String, int)
     */
    public Object getAttribute(String name, int scope) {
        return delegatee.getAttribute(toAttributeName(name, scope));
    }

    /* (non-Javadoc)
     * @see com.day.components.ComponentSession#getAttributeNames()
     */
    public Enumeration getAttributeNames() {
        return getAttributeNames(COMPONENT_SCOPE);
    }

    /* (non-Javadoc)
     * @see com.day.components.ComponentSession#getAttributeNames(int)
     */
    public Enumeration getAttributeNames(final int scope) {
        return new AttributeNamesEnumeration(delegatee.getAttributeNames(), scope);
    }

    /* (non-Javadoc)
     * @see com.day.components.ComponentSession#getComponentContext()
     */
    public ComponentContext getComponentContext() {
        return componentContext;
    }

    /* (non-Javadoc)
     * @see com.day.components.ComponentSession#getCreationTime()
     */
    public long getCreationTime() {
        return delegatee.getCreationTime();
    }

    /* (non-Javadoc)
     * @see com.day.components.ComponentSession#getId()
     */
    public String getId() {
        return delegatee.getId();
    }

    /* (non-Javadoc)
     * @see com.day.components.ComponentSession#getLastAccessedTime()
     */
    public long getLastAccessedTime() {
        return delegatee.getLastAccessedTime();
    }

    /* (non-Javadoc)
     * @see com.day.components.ComponentSession#getMaxInactiveInterval()
     */
    public int getMaxInactiveInterval() {
        return delegatee.getMaxInactiveInterval();
    }

    /* (non-Javadoc)
     * @see com.day.components.ComponentSession#invalidate()
     */
    public void invalidate() {
        delegatee.invalidate();
    }

    /* (non-Javadoc)
     * @see com.day.components.ComponentSession#isNew()
     */
    public boolean isNew() {
        return delegatee.isNew();
    }

    /* (non-Javadoc)
     * @see com.day.components.ComponentSession#removeAttribute(java.lang.String)
     */
    public void removeAttribute(String name) {
        removeAttribute(name, COMPONENT_SCOPE);
    }

    /* (non-Javadoc)
     * @see com.day.components.ComponentSession#removeAttribute(java.lang.String, int)
     */
    public void removeAttribute(String name, int scope) {
        delegatee.removeAttribute(toAttributeName(name, scope));
    }

    /* (non-Javadoc)
     * @see com.day.components.ComponentSession#setAttribute(java.lang.String, java.lang.Object)
     */
    public void setAttribute(String name, Object value) {
        setAttribute(name, value, COMPONENT_SCOPE);
    }

    /* (non-Javadoc)
     * @see com.day.components.ComponentSession#setAttribute(java.lang.String, java.lang.Object, int)
     */
    public void setAttribute(String name, Object value, int scope) {
        delegatee.setAttribute(toAttributeName(name, scope), value);
    }

    /* (non-Javadoc)
     * @see com.day.components.ComponentSession#setMaxInactiveInterval(int)
     */
    public void setMaxInactiveInterval(int interval) {
        delegatee.setMaxInactiveInterval(interval);
    }
    
    public ServletContext getServletContext() {
        return delegatee.getServletContext();
    }

    public HttpSessionContext getSessionContext() {
        return null;
    }

    public Object getValue(String name) {
        return delegatee.getValue(name);
    }

    public String[] getValueNames() {
        return delegatee.getValueNames();
    }

    public void putValue(String name, Object value) {
        delegatee.putValue(name, value);
    }

    public void removeValue(String name) {
        delegatee.removeValue(name);
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
            this.nextName = seek();
        }

        public boolean hasMoreElements() {
            return nextName != null;
        }

        public Object nextElement() {
            if (!hasMoreElements()) {
                throw new NoSuchElementException();
            }
            
            String toReturn = nextName;
            nextName = seek();
            return toReturn;
        }
        
        public void remove() {
            throw new UnsupportedOperationException("remove");
        }
        
        private String seek() {
            while (delegatee.hasMoreElements()) {
                String nextName = (String) delegatee.nextElement();
                if (ComponentSessionUtil.decodeScope(nextName) == scope) {
                    return ComponentSessionUtil.decodeAttributeName(nextName);
                }
            }
            
            // no name found anymore
            return null;
        }
    }
}
