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
package org.apache.sling.core.impl.filter;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.sling.component.Component;
import org.apache.sling.component.ComponentContext;
import org.apache.sling.component.ComponentException;
import org.apache.sling.component.ComponentFilter;
import org.apache.sling.components.AbstractRepositoryComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


abstract class ComponentBindingFilter implements ComponentFilter {

    /** default log */
    private static final Logger log = LoggerFactory.getLogger(ComponentBindingFilter.class);
    
    private ComponentContext componentContext;
    private Map components;
    private LinkedHashMap pendingComponents;

    public synchronized void init(ComponentContext context) {
        this.componentContext = context;
        this.components = new HashMap();
        
        if (pendingComponents != null) {
            LinkedHashMap pc = pendingComponents;
            pendingComponents = null;
            
            for (Iterator pi=pc.values().iterator(); pi.hasNext(); ) {
                Component component = (Component) pi.next();
                
                // initalize the component
                try {
                    component.init(componentContext);
            
                    // only register it internally, if initialization succeeds
                    log.debug("addComponent: Adding componnent {}", component.getId());
                    insert(components, component.getId(), component);
                } catch (ComponentException ce) {
                    log.error("Component " + component.getId() + " failed to initialize", ce);
                } catch (Throwable t) {
                    log.error("Unexpected problem initializing component " + component.getId(), t);
                }
            }
        }
    }

    public synchronized void destroy() {
        this.componentContext = null;
        this.pendingComponents = null;
    }

    protected Component getComponent(String id) {
        return (Component) components.get(id);
    }
    
    protected Iterator getComponents() {
        return components.values().iterator();
    }
    
    protected abstract boolean accept(Component component);
    
    protected synchronized void bindComponent(Component component) {
        if (accept(component)) {
            if (componentContext != null) {
                // initalize the component
                try {
                    component.init(componentContext);
            
                    // only register it internally, if initialization succeeds
                    log.debug("addComponent: Adding componnent {}", component.getId());
                    update(component.getId(), component);
                } catch (ComponentException ce) {
                    log.error("Component " + component.getId() + " failed to initialize", ce);
                } catch (Throwable t) {
                    log.error("Unexpected problem initializing component " + component.getId(), t);
                }
            } else {
                // no context yet
                if (pendingComponents == null) {
                    pendingComponents = new LinkedHashMap();
                }
                pendingComponents.put(component.getId(), component);
            }
        }
    }

    protected synchronized void unbindComponent(Component component) {
        if (accept(component)) {
            // check whether the component is an unintialized one
            if (pendingComponents != null) {
                if (pendingComponents.remove(component.getId()) != null) {
                    log.debug(
                        "unbindComponent: Component {} pending initialization unbound",
                        component.getId());
                    return;
                }
            }

            // only try "removing" if we are active (initialized and not
            // destroyed)
            if (componentContext != null
                && update(component.getId(), null) != null) {
                log.debug("removeComponent: Component {} removed",
                    component.getId());

                // only call destroy, if init succeeded and hence the component
                // was registered
                try {
                    component.destroy();
                } catch (Throwable t) {
                    log.error("Unexpected problem destroying component "
                        + component.getId(), t);
                }
            }
        }
    }

    /**
     * 
     * <p>
     * This is synchronized to prevent paralell update of the store thus enabling
     * the loss of updates.
     * 
     * @param componentId
     * @param component
     * @return
     */
    private Component update(String componentId, Component component) {
        // create new working map as a copy of the old
        Map newStore = new HashMap(components);

        // replace the component actually
        Component replaced = insert(newStore, componentId, component);

        // put new store in action now
        components = newStore;
        
        // return the removed or replaced Component (may be null)
        return replaced;
    }
    
    private Component insert(Map components, String componentId, Component component) {
        // add or remove component
        Component replaced;
        if (component == null) {
            replaced = (Component) components.remove(componentId);
        } else {
            replaced = (Component) components.put(componentId, component);
        }

        // if a component has been removed from the map; if it is
        // an AbstractRepository Component, also remove it by path
        if (replaced instanceof AbstractRepositoryComponent) {
            components.remove(((AbstractRepositoryComponent) replaced).getPath());
        }

        // if the new component is an AbstractRepositoryComponent, register it
        // by path.
        if (component instanceof AbstractRepositoryComponent) {
            replaced = (Component) components.put(
                ((AbstractRepositoryComponent) component).getPath(), component);

            // if the by-path registration removed yet another component,
            // remove if by id (if not componentId)
            if (replaced != null && !replaced.getId().equals(componentId)) {
                components.remove(replaced.getId());
            }
        }

        return replaced;
    }

}