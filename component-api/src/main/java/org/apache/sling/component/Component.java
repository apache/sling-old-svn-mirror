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

import java.io.IOException;
import java.util.Enumeration;

/**
 * The <code>Component</code> interface defines the API to be implemented to
 * handle requests.
 * <p>
 * The life cycle of a component is defined as follows:
 * <ol>
 * <li>Instantiation by the Component Framework. For this to succeed the
 *      component implementation must provide a public default constructor.
 * <li>Setup. The Component Framework sets up the component from some
 *      configuration data by setting properties in Java Beans style. For
 *      example the component may be loaded and configured by reading
 *      repository content.
 * <li>Initialization finalization. To complete setup, the Component Framework
 *      calls the {@link #init(ComponentContext)} method. Only when this method
 *      terminates normally, is the component ready to be used.
 * <li>Normal use by repeatedly calling
 *      {@link #service(ComponentRequest, ComponentResponse)} as requested by
 *      clients.
 * <li>Shutdown. The Component Framework may decide at any time to take a
 *      component instance out of service. In this case, the {@link #destroy()}
 *      method is called and the component object will not be used any more
 *      by the framework. Reasons for destroying a component may be resource
 *      contention, Framework shutdown or replacement by a new version of the
 *      component.
 * </ol>
 * <p>
 * It is under the control of the Component Framework to decide when to
 * instantiate and/or destroy Component instances. But at any one point in time
 * at most one component with a given component ID must be present in the
 * Component Framework.
 * <p>
 * <b>Identification of a Component</b>
 * <p>
 * Each Component has a unique ID by which it is known in the framework. The
 * ID is a string upon which the Component Framework imposes no semantic
 * meaning. The Component ID is used by {@link Content} objects to refer to
 * the Component responsible for their presentation.
 * <p>
 * Here are some recommendations for the definition of the Component ID:
 * <dl>
 * <dt>Repository Based Component</dt>
 * <dd>If the component definition is stored in the repository, it will be
 *   existing in the form of node of type <code>cq:Component</code>. Such nodes
 *   have a UUID as the node type extends <code>mix:referenceable</code>. Hence
 *   the most natural ID for a repository based Component would be the UUID
 *   of the content node.</dd>
 * <dt>Java Based Component</dt>
 * <dd>If the component is registered from Java Component class, which is
 *   provided as part of a Java Archive (or OSGi Bundle), the most natural ID
 *   for the component would be the fully qualified name of the component class.
 *   </dd>
 * </dl>
 *
 * @ocm.mapped discriminator="false"
 */
public interface Component {

    /**
     * @return the ID of this component.
     */
    String getId();

    /**
     * Returns the fully qualified name of the Java class which implements the
     * data container for this component.
     *
     * @return The fully qualified name of the class implementing the data
     *         container of this component.
     */
    String getContentClassName();

    /**
     * Returns the extension of this component with the given <code>name</code>.
     * If no such extension is known to the component, <code>null</code> is
     * returned.
     *
     * @param name The name of the component extension to return.
     *
     * @return The named {@link ComponentExtension} or <code>null</code> if no
     *      extension with the given name is known to this component.
     *
     * @throws IllegalArgumentException if <code>name</code> is <code>null</code>.
     */
    ComponentExtension getExtension(String name);

    /**
     * Returns an <code>Enumeration</code> on all {@link ComponentExtension extensions}
     * known to this component. If this component has no extensions, an empty
     * <code>Enumeration</code> is returned.
     *
     * @return An <code>Enumeration</code> on all extensions known to this component.
     */
    Enumeration<ComponentExtension> getExtensions();

    /**
     * This method returns a clean instance of the content class which may then
     * be used to fill content and store it in the repository. The instance
     * returned will already be properly setup such that its getComponent method
     * returns this Component instance.
     *
     * @return A prepared content instance...
     */
    Content createContentInstance();

    /**
     * Returns the {@link ComponentContext} with which this component has been
     * initialized.
     *
     * @return The <code>ComponentContext</code> of this component
     */
    ComponentContext getComponentContext();

    /**
     * This method is called when the Component is first loaded. Only when this
     * method terminates normally, will the Component be able to handle requests.
     *
     * @param context The {@link ComponentContext context} in which the component
     *      will be operating.
     *
     * @throws ComponentException May be thrown if initialization fails.
     */
    void init(ComponentContext context) throws ComponentException;

    /**
     * Called by the Component Framework to handle the request onj behalf of
     * the {@link Content} object. This method has full access to the original
     * HTTP request and response objects and should implement whatever is
     * requested by the client. However it is strongly recommended to properly
     * separate business logic (that is content modification) and response
     * rendering.
     *
     * @throws ComponentException If an error occurrs while handling the
     *             component action
     * @throws IOException If an error occurrs writing the response
     */
    void service(ComponentRequest request, ComponentResponse response)
        throws ComponentException, IOException;

    /**
     * This method is called when the Component is taken out of service. After
     * this method has completed (successfully or not) the Component will not be
     * used any more.
     */
    void destroy();
}
