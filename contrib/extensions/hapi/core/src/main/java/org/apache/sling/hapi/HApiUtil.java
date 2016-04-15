/*******************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 ******************************************************************************/

package org.apache.sling.hapi;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.sling.api.resource.ResourceResolver;

public interface HApiUtil {

    String DEFAULT_RESOURCE_TYPE = "sling/hapi/components/type";

    /**
     * <p>Get a HApi type object from a type identifier.</p>
     * <p>The JCR node must be [nt:unstructured], a descendant of any of the HAPi search path defined by the
     * {@see HAPI_PATHS} config and the sling:resourceType should be set to the value defined by the {@see HAPI_RESOURCE_TYPE} config</p>
     * <p>The first result is returned</p>
     * @param resolver The sling resource resolver object
     * @param type The type identifier, which is either in the form of a jcr path,
     *             same as the path for {@link: ResourceResolver#getResource(String)}. If the path cannot be resolved, type is treated like
     *             a fully qualified domain name, which has to match the "fqdn" property on the JCR node which represents the type.
     * @return The first node that matches that type or null if none is found.
     * @throws RepositoryException
     */
    Node getTypeNode(ResourceResolver resolver, String type) throws RepositoryException;


    /**
     * <p>Get a HApi type object from a type identifier.</p>
     * <p>The type identifier is resolved to a {@link javax.jcr.Node} and then
     * {@link #fromNode(org.apache.sling.api.resource.ResourceResolver, javax.jcr.Node)} is called.</p>
     * <p>For restrictions on the {@link javax.jcr.Node}
     * see {@link HApiUtil#getTypeNode(org.apache.sling.api.resource.ResourceResolver, String)}</p>
     * @param resolver The sling resource resolver object
     * @param type The type identifier, which is either in the form of a jcr path,
     *             same as the path for{@link: ResourceResolver#getResource(String)}. If the path cannot be resolved, type is treated like
     *             a fully qualified domain name, which has to match the "fqdn" property on the JCR node which represents the type.
     * @return The HApiType resolved from the type identifier
     * @throws javax.jcr.RepositoryException
     */
    HApiType fromPath(ResourceResolver resolver, String type) throws RepositoryException;

    /**
     * <p>Get a HApi type object from the {@link javax.jcr.Node}.</p>
     * The Node has the following properties:
     * <ul>
     *     <li>name: A 'Name' of the type (mandatory)</li>
     *     <li>description: A 'String' with the description text for this type (mandatory)</li>
     *     <li>fqdn: A 'String' with the fully qualified domain name; A namespace like a java package (mandatory)</li>
     *     <li>extends: A type identifier (either a path or a fqdn); (optional). This defines the parent type of this type</li>
     *     <li>parameter: A multivalue property to define a list of java-like generic types
     *     that can be used as types for properties; (optional)</li>
     * </ul>
     *
     * <p>The properties of this type are defined as children nodes.</p>
     * <p>The name of property node defines the name of the property for this type. </p>
     * The children property nodes have the following properties:
     * <ul>
     *     <li>type: The type identifier (mandatory). Can be of type 'Name' or 'Path'
     *      See {@link HApiUtil#getTypeNode(org.apache.sling.api.resource.ResourceResolver, String)}
     *      for the format of this value</li>
     *     <li>description: A 'String' with the description for this property (mandatory)</li>
     *     <li>multiple: A 'Boolean' that defines whether this property can exist multiple times on an object of this type (optional)</li>
     * </ul>
     *
     * @param resolver The resource resolver
     * @param typeNode The jcr node of the HApi type
     * @return The HApiType
     * @throws RepositoryException
     */
    HApiType fromNode(ResourceResolver resolver, Node typeNode) throws RepositoryException;

    /**
     * Get a new instance of AttributeHelper for the type identified by 'type'
     * @param resolver
     * @param type See {@link #getTypeNode(org.apache.sling.api.resource.ResourceResolver, String)}
     *             for the format of the type identifier
     * @return
     * @throws RepositoryException
     */
    MicrodataAttributeHelper getHelper(ResourceResolver resolver, String type) throws RepositoryException;
}
