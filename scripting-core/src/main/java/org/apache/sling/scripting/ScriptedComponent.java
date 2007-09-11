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
package org.apache.sling.scripting;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.TreeSet;

import org.apache.sling.component.Component;
import org.apache.sling.component.ComponentException;
import org.apache.sling.component.ComponentRequest;
import org.apache.sling.component.ComponentResponse;
import org.apache.sling.core.components.AbstractRepositoryComponent;
import org.apache.sling.scripting.core.ScriptManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The <code>ScriptedComponent</code> Is the (base class) for scripting in
 * Sling. This class may be used as the default scripting component by just
 * registering instance of this class directly or by creating nodes of type
 * <code>sling:ScriptedComponent</code> which are automatically recognized by
 * the Sling Core as repository based components and loaded.
 * <p>
 * This class may be extended to implement custom behaviour in any of the
 * {@link #service(ComponentRequest, ComponentResponse)},
 * {@link #getScript(ComponentRequest)}, {@link #getComponentRenderer(Script)}
 * and
 * {@link #callScript(ComponentRenderer, ComponentRequest, ComponentResponse)}
 * methods.
 * 
 * @ocm.mapped jcrNodeType="sling:ScriptedComponent" discriminator="false"
 */
public class ScriptedComponent extends AbstractRepositoryComponent {

    /** default log */
    private static final Logger log = LoggerFactory.getLogger(ScriptedComponent.class);

    /** @ocm.field jcrName="sling:baseComponent" */
    private String baseComponentReference;

    private Component superComponent;

    /**
     * @ocm.collection jcrName="sling:scripts" jcrNodeType="sling:ScriptList"
     *                 elementClassName="org.apache.sling.core.scripting.Script"
     */
    private Script[] scripts;

    protected void doInit() {
        // TODO Auto-generated method stub
    }

    /**
     * Resolves the {@link Script} responsible for handling the request and
     * calls that script. if no script is found, the base component is called
     * (if available). If no base component is available, a warning message is
     * just logged and nothing more happens.
     * <p>
     * If a script may be resolved for which no script handler is known, a
     * <code>ComponentException</code> is thrown.
     * <p>
     * This method may be overwritten by extending classes completely replacing
     * this method or wrapping this method with additional processing. For
     * information, this method executes these steps:
     * <ol>
     * <li>Get the script calling {@link #getScript(ComponentRequest)}
     * <li>Retrieve the component renderer calling
     * {@link #getComponentRenderer(Script)} with the script found in the first
     * step
     * <li>Call
     * {@link ComponentRenderer#service(ComponentRequest, ComponentResponse)} on
     * the component renderer found in the second step
     * <li>Call the base component if no script can be resolved for the request
     * </ol>
     * 
     * @param request The <code>ComponentRequest</code> representing the
     *            request to handle.
     * @param response The <code>ComponentResponse</code> representing the
     *            response of the request to handle.
     * @throws IOException Forwarded from the
     *             {@link ComponentRenderer#service(ComponentRequest, ComponentResponse)}
     *             method.
     * @throws ComponentException If no script handler can be found for a
     *             resolved script or forwarded from the
     *             {@link ComponentRenderer#service(ComponentRequest, ComponentResponse)}
     *             method.
     */
    public void service(ComponentRequest request, ComponentResponse response)
            throws IOException, ComponentException {

        // resolve the script and its renderer
        Script script = getScript(request);
        ComponentRenderer cr = getComponentRenderer(script);

        if (cr != null) {

            // render it if found
            callScript(cr, request, response);

        } else if (getSuperComponent() != null) {

            // otherwise use super component (if available)
            getSuperComponent().service(request, response);

        } else {

            // we cannot render anything ....
            log.warn("service: Cannot service request {}",
                request.getRequestURI());

        }
    }

    /**
     * Returns the {@link ScriptInfo} matching the query. The matching strategy
     * is first-match. That is the first {@link ScriptInfo} found in the Vector
     * matching the request details is returned.
     * <p>
     * A match is first searched for in the template itself. If no match can be
     * found and a base template has been configured, the base template is asked
     * for a script info. If the base template does not have any either,
     * <code>null</code> is returned.
     * 
     * @param request The {@link DeliveryHttpServletRequest} for which to find
     *            the match {@link ScriptInfo}.
     * @return the first {@link ScriptInfo} matching the request or
     *         <code>null</code> if no script info could be found.
     */
    protected Script getScript(ComponentRequest request) {

        // check whether we can handle the script
        for (int i = 0; i < scripts.length && scripts[i] != null; i++) {
            if (scripts[i].matches(request)) {
                return scripts[i];
            }
        }

        // outsch - cannot handle
        log.debug("No scriptinfo for request: {0}, using default", request);
        return new DefaultScript(getPath());
    }

    /**
     * Returns a {@link ComponentRenderer} for the {@link Script} or
     * <code>null</code> if <code>script</code> is <code>null</code>.
     * 
     * @param script The {@link Script} for which to return the
     *            {@link ComponentRenderer}
     * @return The component renderer or <code>null</code> of
     *         <code>script</code> is <code>null</code>.
     * @throws ComponentException is thrown, if no {@link ScriptHandler} is
     *             registered for the type of the <code>script</code>
     */
    protected ComponentRenderer getComponentRenderer(Script script)
            throws ComponentException {

        // ensure the script
        if (script == null) {
            log.info("getComponentRenderer: No script to get a renderer for");
            return null;
        }

        // get the handler for the type of the script, throw if none
        ScriptHandler handler = ScriptManager.getScriptHandler(script);
        if (handler == null) {
            throw new ComponentException("No handler for Script "
                + script.getScriptName() + " (type=" + script.getType()
                + ") found");
        }

        return handler.getComponentRenderer(this, script.getScriptName());
    }

    /**
     * Calls
     * {@link ComponentRenderer#service(ComponentRequest, ComponentResponse)}
     * method to handle the request. If the <code>renderer</code> is
     * <code>null</code>, this method does nothing.
     * <p>
     * Before calling the renderer, this instance is set as the
     * {@link Util#ATTR_COMPONENT org.apache.sling.core.scripting.component}
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
     */
    protected void callScript(ComponentRenderer renderer,
            ComponentRequest request, ComponentResponse response)
            throws IOException, ComponentException {

        if (renderer == null) {
            log.debug("callScript: No ComponentRenderer to call");
            return;
        }

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

    // ---------- internal -----------------------------------------------------

    public void setSuperComponent(Component superComponent) {
        this.superComponent = superComponent;
    }

    public Component getSuperComponent() {
        return superComponent;
    }

    // ---------- JCR Mapping support ------------------------------------------

    public void setPath(String path) {
        super.setPath(path);

        // default value for the component ID is its path
        if (getId() == null) {
            log.debug("Using path {} as ComponentID", path);
            setId(path);
        }
    }

    public String getBaseComponentReference() {
        return baseComponentReference;
    }

    public void setBaseComponentReference(String baseComponentRef) {
        this.baseComponentReference = baseComponentRef;
    }

    public void setScripts(Collection scripts) {
        TreeSet scriptSet = new TreeSet();
        if (scripts != null && !scripts.isEmpty()) {
            scriptSet.addAll(scripts);
        }
        this.scripts = (Script[]) scriptSet.toArray(new Script[scriptSet.size()]);
    }

    public Collection getScripts() {
        return Arrays.asList(scripts);
    }

    // ---------- Inner class for pseudo default script ------------------------

    private static class DefaultScript implements Script {

        public static final String DEFAULT_SCRIPT_REL_PATH = "/jsp/start.jsp";

        private String scriptName;

        DefaultScript(String componentPath) {
            scriptName = componentPath + DEFAULT_SCRIPT_REL_PATH;
        }

        /**
         * Returns <code>jsp</code> as the default script is a JSP script.
         */
        public String getType() {
            return "jsp";
        }

        public String getScriptName() {
            return scriptName;
        }

        public boolean matches(ComponentRequest request) {
            return true;
        }
    }

}
