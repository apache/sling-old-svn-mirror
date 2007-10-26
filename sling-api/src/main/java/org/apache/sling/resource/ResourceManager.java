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
package org.apache.sling.resource;

import org.apache.sling.exceptions.SlingException;

/**
 * The <code>ResourceManager</code> interface extends the
 * {@link ResourceResolver} in case the Sling Framework supports Object Content
 * Mapping of some sort and thus able to manage the
 * {@link Resource#getData() data field of the resource}.
 * <p>
 * Any data modification operations executed through objects of this interface
 * must be persisted explicitly by calling the {@link #save()} method. Likewise
 * any modifications may be rolled back by calling {@link #rollback()} instead.
 * <p>
 * Implementations of this interface will (of course) also implement the methods
 * of the base interface. In addition to just loading the {@link Resource}
 * instances, though, implementations of this interface are expected to also try
 * to set the {@link Resource#getData() data field} of the resource if possible.
 */
public interface ResourceManager extends ResourceResolver {

    /**
     * Stores the {@link Resource#getData() resource data} mapping the Java
     * object back into the underlying persistence structure.
     * <p>
     * This method may be used to update existing data or to create new data in
     * the persistence. The implementation of the method must make care to
     * create or update the persistent data as appropriate.
     * <p>
     * After this method, the changes are only stored transiently. To persist
     * the update, the {@link #save()} method must be called.
     *
     * @param resource The {@link Resource} whose (mapped) data is to be stored
     *            back into the persistence layer.
     * @throws SlingException if any error occurrs mapping the data to the
     *             persistence layer. The cause of the exception should be
     *             available through the <code>getCause</code> method.
     */
    void store(Resource resource) throws SlingException;

    /**
     * Returns a <code>Resource</code> object whose
     * {@link Resource#getData() data field} is mapped from the persistent data
     * to an instance of the given <code>type</code>. If persistent data can
     * be found at the requested path which cannot be mapped into an object of
     * the requested type a <code>ResourceNotFoundException</code> is thrown
     * as if no persistent data would be existing at the requested path.
     *
     * @param path The absolute path to the item to load
     * @param type The required concrete type of the loaded data of the
     *            <code>Resource</code> object.
     * @return The <code>Resource</code> object loaded from the path with the
     *         data field set to an object of the given type.
     * @throws java.security.AccessControlException if an item exists at the
     *             <code>path</code> but the session of this resource manager
     *             has no read access to the item.
     * @throws ResourceNotFoundException If no resource can be resolved at the
     *             resolved path or if the path is not an absolute path or if
     *             the data cannot be mapped to an object of the requested
     *             <code>type</code>.
     * @throws SlingException If an error occurrs trying to load the resource
     *             object from the path or if <code>base</code> is
     *             <code>null</code> and <code>path</code> is relative.
     */
    Resource getResource(String path, Class<?> type)
            throws ResourceNotFoundException, SlingException;

    /**
     * Deletes the persistent data at the location pointed to by the
     * {@link Resource#getURI()} method. If no data exists at the specified
     * location, this method has no effect.
     * <p>
     * After this method, the changes are only stored transiently. To persist
     * the update, the {@link #save()} method must be called.
     *
     * @param resource The resource whose underlying persistent data is to be
     *            removed.
     * @throws SlingException If an error occurrs trying to delete the data.
     *             This exception must NOT be thrown if there is actually no
     *             data at the indicated location.
     */
    void delete(Resource resource) throws SlingException;

    /**
     * Copies persistent data from the location indicated by the resource in its
     * {@link Resource#getURI() URI path field} to the new location.
     * <p>
     * After this method, the changes are only stored transiently. To persist
     * the update, the {@link #save()} method must be called.
     *
     * @param resource The resource to copy to the new location.
     * @param destination The absolute path of the new location to store the
     *            resource in.
     * @param deep <code>true</code> if not only the indicated resource but
     *            also all child resources should be copied. Otherwise, only the
     *            given resource is copied to the new location.
     * @throws IllegalArgumentException If the <code>destination</code> is not
     *             an absolute path.
     * @throws SlingException If an error occurrs copying the resource. In
     *             particular this exception must be thrown, if there is already
     *             data existing at the <code>destinatiopn</code> location.
     */
    void copy(Resource resource, String destination, boolean deep)
            throws SlingException;

    /**
     * Moves the persistent data rooted at the location indicated by the
     * resource in its {@link Resource#getURI() URI path field} to the new
     * location. If the resource has child resources, these are moved together
     * with the resource itself.
     * <p>
     * After this method, the changes are only stored transiently. To persist
     * the update, the {@link #save()} method must be called.
     *
     * @param resource The resource to move to the new location.
     * @param destination The absolute path of the new location to move the
     *            resource and its children to.
     * @throws IllegalArgumentException If the <code>destination</code> is not
     *             an absolute path.
     * @throws SlingException If an error occurrs copying the resource. In
     *             particular this exception must be thrown, if there is already
     *             data existing at the <code>destinatiopn</code> location.
     */
    void move(Resource resource, String destination) throws SlingException;

    /**
     * Orders the given resource within its parent such that it will be listed
     * in child resource listings (see
     * {@link ResourceResolver#listChildren(Resource)} right before the resource
     * indicated by the <code>afterName</code>.
     * <p>
     * After this method, the changes are only stored transiently. To persist
     * the update, the {@link #save()} method must be called.
     *
     * @param resource The resource to reorder in the child listing of its
     *            parent.
     * @param afterName The name of the resource before which the resource is
     *            ordered. If <code>null</code>, the resource is moved to the
     *            end of the child resource list of the parent.
     * @throws SlingException If an error occurrs reordering the resource. The
     *             <code>getCause</code> method should provide the cause for
     *             the error.
     */
    void orderBefore(Resource resource, String afterName) throws SlingException;

    /**
     * Returns <code>true</code> if there are any unsaved changes in the
     * persistent data. In this case the {@link #save()} method should be called
     * to persist the changes. Alternatively the {@link #rollback()} method may
     * be called to undo these changes.
     */
    boolean hasChanges();

    /**
     * Saves any changes applied to resources executed through this instance
     * persistently.
     * <p>
     * This method must be called explicitly to make sure changes are actually
     * stored. Otherwise these changes may be lost and the end of the request.
     * <p>
     * This method has no effect if there are no unsaved changes, that is if the
     * {@link #hasChanges()} method returns <code>false</code>.
     *
     * @throws SlingException If an error occurrs persisting the changes. The
     *             exception should provide access to any causing throwable the
     *             <code>getCause</code> method.
     */
    void save() throws SlingException;

    /**
     * Rolls back any changes applied to resources executed through this
     * instance.
     * <p>
     * This method has no effect if there are no unsaved changes, that is if the
     * {@link #hasChanges()} method returns <code>false</code>.
     *
     * @throws SlingException If an error occurrs rolling back the changes. The
     *             exception should provide access to any causing throwable the
     *             <code>getCause</code> method.
     */
    void rollback();

}
