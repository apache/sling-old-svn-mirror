/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.scripting;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.TreeSet;

import org.apache.sling.component.ComponentException;
import org.apache.sling.component.ComponentRequest;
import org.apache.sling.component.ComponentResponse;
import org.apache.sling.core.components.AbstractRepositoryComponent;
import org.apache.sling.scripting.core.ScriptManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The <code>ScriptedComponent</code> is the (base class) for scripting in
 * Sling. This class may be used as the default scripting component by just
 * registering instances of this class directly or by creating nodes of type
 * <code>sling:ScriptedComponent</code> which are automatically recognized by
 * the Sling Core as repository based components and loaded.
 * <p>
 * This class may be extended to implement custom behaviour in any of the
 * {@link #service(ComponentRequest, ComponentResponse)},
 * {@link #resolveRenderer(ComponentRequest)},
 * {@link #getDeclaredScript(ComponentRequest)},
 * {@link #getSelectorScript(String)}, {@link #getComponentRenderer(Script)}
 * and
 * {@link #callScript(ComponentRenderer, ComponentRequest, ComponentResponse)}
 * methods.
 *
 * @ocm.mapped jcrType="sling:ScriptedComponent" discriminator="false"
 */
public class ScriptedComponent extends AbstractRepositoryComponent {

    /**
     * Default script name to use when resolving a request to a script, which is
     * not defined (value is "default.jsp").
     *
     * @see #getSelectorScript(String)
     * @see DefaultScript
     */
    public static final String DEFAULT_SCRIPT_REL_PATH = "default.jsp";

    /** default log */
    private final Logger log = LoggerFactory.getLogger(ScriptedComponent.class);

    /** @ocm.field jcrName="sling:baseComponent" */
    private String baseComponentReference;

    /**
     * @ocm.collection jcrName="sling:scripts" jcrType="sling:ScriptList"
     *                 elementClassName="org.apache.sling.scripting.Script"
     */
    private Script[] scripts;

    /**
     * This implementation does nothing. Extensions of this class may overwrite
     * but should call this base class implementation first.
     */
    protected void doInit() {
    }

    /**
     * Calls the {@link Script} responsible for handling the request. If none
     * can be resolved or if no handler is available to handle the resolved
     * script a {@link ComponentException} is thrown.
     * <p>
     * This method is implemented by first calling the
     * {@link #resolveRenderer(ComponentRequest)} method and then the
     * {@link #callScript(ComponentRenderer, ComponentRequest, ComponentResponse)}
     * method with the resolved renderer.
     *
     * @param request The <code>ComponentRequest</code> representing the
     *            request to handle.
     * @param response The <code>ComponentResponse</code> representing the
     *            response of the request to handle.
     * @throws IOException Forwarded from the
     *             {@link ComponentRenderer#service(ComponentRequest, ComponentResponse)}
     *             method.
     * @throws ComponentException If no script can be resolved to handle the
     *             request, if no script handler can be found for a resolved
     *             script or if thrown by the script handling the request.
     */
    public void service(ComponentRequest request, ComponentResponse response)
            throws IOException, ComponentException {

        // resolve the ComponentRenderer for the request, fail if none
        ComponentRenderer renderer = resolveRenderer(request);
        if (renderer == null) {
            throw new ComponentException("Cannot resolve script for request");
        }

        // otherwise call that renderer now
        callScript(renderer, request, response);
    }

    /**
     * Resolves the a {@link ComponentRenderer} for the given request as
     * follows:
     * <ol>
     * <li>Calls the {@link #getDeclaredScript(ComponentRequest)} to resolve a
     * script from the script definitions in this component.
     * <li>If there is a {@link #getBaseComponent() base component}, the same
     * method is called on the base component.
     * <li>Calls the {@link #getSelectorScript(String)} to find a script for
     * the request's selectors.
     * <li>If there is base component, the same method is called on the base
     * component.
     * </ol>
     * <p>
     * As soon as a {@link ComponentRenderer} can be found for the request, this
     * method returns that renderer. If all steps fail, <code>null</code> is
     * returned. Note that, not finding a component renderer may be cause by not
     * being able to resolve a script or not being able to resolve a component
     * renderer for script, which may have been resolved.
     *
     * @param request The ComponentRequest for which to select a script to call.
     * @return The selected {@link ComponentRenderer} as per the above algorithm
     *         or <code>null</code> if no script may be selected and used.
     */
    protected ComponentRenderer resolveRenderer(ComponentRequest request) {
        // 1. Resolve declared script first
        ComponentRenderer renderer = getDeclaredScript(request);
        if (renderer != null) {
            return renderer;
        }

        // 2. Resolve using base component
        ScriptedComponent baseComponent = null; // getBaseComponent();
        if (baseComponent != null) {
            renderer = baseComponent.getDeclaredScript(request);
            if (renderer != null) {
                return renderer;
            }
        }

        // 3. Resolve with selectors
        String selectors = request.getSelectorString();
        renderer = getSelectorScript(selectors);
        if (renderer != null) {
            return renderer;
        }

        // 4. Resolve with selectors using base component
        if (baseComponent != null) {
            return baseComponent.getSelectorScript(selectors);
        }

        // finally, nothing could be found
        log.debug("resolveRenderer: No ComponentRenderer found");
        return null;
    }

    /**
     * Returns a {@link ComponentRenderer} matching the request. This method
     * traverses the registered scripts and selects the first script matching
     * the request. For that selected script the
     * {@link #getComponentRenderer(Script)} method is called and the renderer
     * is returned if no <code>null</code>. Otherwise the list of scripts is
     * further scanned for matching scripts and to hopefully find a script with
     * a renderer. Finally, if no matching script can be found with a non-<code>null</code>
     * renderer, this method returns <code>null</code>.
     *
     * @param request The {@link DeliveryHttpServletRequest} for which to find
     *            the match {@link ScriptInfo}.
     * @return a {@link ComponentRenderer} for the first script configured which
     *         matches the request or <code>null</code> if no such script or
     *         component renderer can be found.
     */
    protected ComponentRenderer getDeclaredScript(ComponentRequest request) {

        // check whether we can handle the script
        Script[] scripts = this.scripts;
        if (scripts != null) {
            for (int i = 0; i < scripts.length && scripts[i] != null; i++) {
                if (scripts[i].matches(request)) {
                    ComponentRenderer renderer = getComponentRenderer(scripts[i]);
                    if (renderer != null) {
                        log.debug("getDeclaredScript: Using script {}",
                            scripts[i]);
                        return renderer;
                    }
                }
            }
        }

        // outsch - cannot handle
        log.debug(
            "getDeclaredScript: No declared script found for request: {}",
            request);
        return null;
    }

    /**
     * Tries to find a default script in this scripted component for the given
     * request selectors. If no selectors exist, i.e. the selector string is
     * empty, the default script is
     * <code>{@link #DEFAULT_SCRIPT_REL_PATH default.jsp}</code> just below
     * this scripted component. Otherwise the selectors the
     * <code>default.jsp</code> script is searched for using the selectors as
     * a path after replacing dots with slashes:
     * <ol>
     * <li>Let path be this components path + "/" + selectors.replace('.', '/')
     * <li>Check the script path + "/default.jsp"
     * <li>If unsuccesfull, cut of the last path segment of path and repeat
     * step two unless the path is shorter than this components path.
     * </ol>
     *
     * @param selectors The selectors of the request URI as returned by the
     *            <code>ComponentRequest.getSelectors()</code> method.
     * @return a {@link ComponentRenderer} for default script selected based on
     *         the selectors or <code>null</code> if no such script exists or
     *         no renderer can be retrieved for the script.
     */
    protected ComponentRenderer getSelectorScript(String selectors) {

        // shortcut for standard case without selectors
        if (selectors.length() == 0) {
            return getComponentRenderer(new DefaultScript(getPath()));
        }

        // build a path string from the
        String path = getPath() + "/" + selectors.replace('.', '/');
        int termCond = getPath().length();
        while (path.length() >= termCond) {

            // try to get a component renderer for the current path
            Script script = new DefaultScript(path);
            ComponentRenderer renderer = getComponentRenderer(script);
            if (renderer != null) {
                log.debug("getSelectorScript: Using script {}", script);
                return renderer;
            }

            // cut off the last element of the path
            int lastSlash = path.lastIndexOf('/');
            if (lastSlash > 0) {
                path = path.substring(0, lastSlash);
            }
        }

        log.debug("getSelectorScript: No script found for selectors {}",
            selectors);
        return null;
    }

    /**
     * Returns a {@link ComponentRenderer} for the {@link Script} or
     * <code>null</code> if <code>script</code> is <code>null</code> or if
     * no {@link ScriptHandler} is registered for the type of the script, in
     * which case an error is also logged.
     *
     * @param script The {@link Script} for which to return the
     *            {@link ComponentRenderer}
     * @return The component renderer or <code>null</code> if
     *         <code>script</code> is <code>null</code> or if no script
     *         handler for the type of the script is registered.
     */
    protected ComponentRenderer getComponentRenderer(Script script) {

        // ensure the script
        if (script == null) {
            log.info("getComponentRenderer: No script to get a renderer for");
            return null;
        }

        // get the handler for the type of the script, throw if none
        ScriptHandler handler = ScriptManager.getScriptHandler(script);
        if (handler == null) {
            log.error("No handler for Script {} (type={}) found",
                script.getScriptName(), script.getType());
            return null;
        }

        return handler.getComponentRenderer(this, script.getScriptName());
    }

    /**
     * Calls
     * {@link ComponentRenderer#service(ComponentRequest, ComponentResponse)}
     * method to handle the request.
     * <p>
     * Before calling the renderer, this instance is set as the
     * {@link Util#ATTR_COMPONENT org.apache.sling.scripting.component}
     * attribute in the request. When the script returns. the former value of
     * the request attribute is reset.
     *
     * @param renderer The {@link ComponentRenderer} to call to handle the
     *            component.
     * @param request The <code>ComponentRequest</code> representing the
     *            request to handle.
     * @param response The <code>ComponentResponse</code> representing the
     *            response of the request to handle.
     * @throws IOException Forwarded from the
     *             {@link ComponentRenderer#service(ComponentRequest, ComponentResponse)}
     *             method.
     * @throws ComponentException Forwarded from the
     *             {@link ComponentRenderer#service(ComponentRequest, ComponentResponse)}
     *             method.
     * @throws NullPointerException if render is <code>null</code>.
     */
    protected void callScript(ComponentRenderer renderer,
            ComponentRequest request, ComponentResponse response)
            throws IOException, ComponentException {

        // replace the component attribute with the current component
        Object oldComponent = Util.replaceAttribute(request,
            Util.ATTR_COMPONENT, this);
        try {

            // call the script, forwarding any exceptions
            renderer.service(request, response);

        } finally {

            // ensure the component is reset
            Util.replaceAttribute(request, Util.ATTR_COMPONENT, oldComponent);
        }
    }

    /**
     * Returns the base component of this scripted component as defined by the
     * {@link #setBaseComponentReference(String)} method or <code>null</code>
     * if the base component is not defined or is not a scripted component.
     * <p>
     * Currently, this method is not implemented and always returns null.
     *
     * @return The base component configured for this component or
     *         <code>null</code> if none is defined or registered.
     */
    protected ScriptedComponent getBaseComponent() {
        // TODO to be implemented ...
        return null;
    }

    // ---------- JCR Mapping support ------------------------------------------

    public void setPath(String path) {
        super.setPath(path);

        // default value for the component ID is its path
        if (this.getId() == null) {
            log.debug("Using path {} as ComponentID", path);
            this.setId(path);
        }
    }

    public String getBaseComponentReference() {
        return this.baseComponentReference;
    }

    public void setBaseComponentReference(String baseComponentRef) {
        this.baseComponentReference = baseComponentRef;
    }

    public void setScripts(Collection scripts) {
        if (scripts == null || scripts.isEmpty()) {
            this.scripts = null;
        } else {
            TreeSet scriptSet = new TreeSet();
            if (scripts != null && !scripts.isEmpty()) {
                scriptSet.addAll(scripts);
            }
            this.scripts = (Script[]) scriptSet.toArray(new Script[scriptSet.size()]);
        }
    }

    public Collection getScripts() {
        if (scripts == null) {
            return Collections.emptyList();
        }

        return Arrays.asList(this.scripts);
    }

    // ---------- Inner class for pseudo default script ------------------------

    /**
     * The <code>DefaultScript</code> class is a synthetic script definition
     * which is used by the {@link ScriptedComponent#getSelectorScript(String)}
     * method to represent a non-configured script.
     */
    protected static class DefaultScript implements Script {

        /** The path of the script */
        private final String scriptName;

        /**
         * Creates an instance of this default script by appending the
         * {@link #DEFAULT_SCRIPT_REL_PATH} to the given path.
         *
         * @param componentPath The path in which the default script should be
         *            located.
         */
        DefaultScript(String componentPath) {
            scriptName = componentPath + "/" + DEFAULT_SCRIPT_REL_PATH;
        }

        /**
         * Returns <code>jsp</code> as the default script is a JSP script.
         */
        public String getType() {
            return "jsp";
        }

        /**
         * Returns the absolute path to the script as configured by the
         * constructor.
         */
        public String getScriptName() {
            return scriptName;
        }

        /**
         * Always returns <code>true</code>.
         */
        public boolean matches(ComponentRequest request) {
            return true;
        }

        @Override
        public String toString() {
            return "DefaultScript: " + getScriptName();
        }
    }

}
