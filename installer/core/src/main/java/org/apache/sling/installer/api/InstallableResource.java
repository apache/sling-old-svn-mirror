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
package org.apache.sling.installer.api;

import java.io.InputStream;
import java.util.Dictionary;


/**
 * A piece of data that can be installed by the {@link OsgiInstaller}
 * Currently the OSGi installer supports bundles and configurations,
 * but it can be extended by additional task factories supporting
 * other formats.
 *
 * The installable resource contains as much information as the client
 * can provide. An input stream or dictionary is mandatory everything
 * else is optional. All optional values will be tried to be evaluated
 * by the OSGi installer. If such evaluation fails the resource will
 * be ignore during installation.
 *
 * If the client provides a configuration it should use the
 * resource type {@link #TYPE_PROPERTIES}. Otherwise the resource
 * type {@link #TYPE_FILE} should be used. These two generic types
 * are transformed by resource transformer services to the appropriate
 * resource type like bundle or configuration etc. This frees the
 * client from having any knowledge about the provided data.
 * However, if the client has the knowledge about the data it can
 * provided a specific resource type.
 *
 * The provider should provide a digest for files (input streams).
 * The installer will calculate a digest for dictionaries, regardless
 * if the provider provided a dictionary.
 */
public class InstallableResource {

    /**
     * The type for properties - in this case {@link #getDictionary()}
     * should contain a dictionary or the {@link #getInputStream()}
     * should point to a property or configuration file.
     * @since 3.1 */
    public static final String TYPE_PROPERTIES = "properties";

    /**
     * The type for all other provided data like a bundle etc.
     * In this case {@link #getInputStream()} must return an input
     * stream to the data. {@link #getDictionary()} might return
     * additional information.
     * @since 3.1 */
    public static final String TYPE_FILE = "file";

    /**
     * The type for a bundle - in this case {@link #getInputStream} must
     * return an input stream to the bundle. {@link #getDictionary()} might
     * return additional information.
     * This type should only be used if the client really knows that the
     * provided data is a bundle.
     */
    public static final String TYPE_BUNDLE = "bundle";

    /**
     * The type for a configuration - in this case {@link #getDictionary()}
     * must return a dictionary with the configuration.
     * This type should only be used if the client really knows that the
     * provided data is an OSGi configuration.
     */
    public static final String TYPE_CONFIG = "config";

    /**
     * Optional parameter in the dictionary if a bundle is installed. If this
     * is set with a valid start level, the bundle is installed in that start level.
     */
    public static final String BUNDLE_START_LEVEL = "bundle.startlevel";

    /**
     * Optional parameter in the dictionary if a resource (not a dict) is installed.
     * This parameter might be used by the installation task for any purpose like
     * bundle start level etc.
     * @since 3.1
     */
    public static final String INSTALLATION_HINT = "installation.hint";

    /**
     * Optional parameter in the dictionary if a resource (not a dict) is installed.
     * If this parameter is specified, the installer uses the URI to get the input
     * stream of the resource! Usually the installer copies the resource into the
     * file system and uses this copy. To optimize this, if the URI of the resource
     * is always available (like a file URI), this property can be used to avoid
     * copying the resource.
     * It is only evaluated if the resource type is either unknown (null) or
     * {@link #TYPE_FILE} and a digest for the resource is delivered.
     * The value of this property is a string.
     * This property might also be set for an {@link UpdateHandler} in order
     * to give a hint for the (file) name the resource or dictionary should
     * have.
     * @since 3.2.2
     */
    public static final String RESOURCE_URI_HINT = "resource.uri.hint";

    /** Default resource priority */
    public static final int DEFAULT_PRIORITY = 100;

    private final String id;
    private final String digest;
    private final InputStream inputStream;
    private final Dictionary<String, Object> dictionary;
    private final int priority;
    private final String resourceType;

    /**
     * Create a data object - this is a simple constructor just using the
     * values as they are provided.
     * @param id Unique id for the resource, For auto detection of the resource
     *           type, the id should contain an extension like .jar, .cfg etc.
     * @param is The input stream to the data or
     * @param dict A dictionary with data
     * @param digest A digest of the data - providers should make sure to set
     *               a digest. Calculating a digest by the installer can be very
     *               expensive for input streams
     * @param type The resource type if known, otherwise {@link #TYPE_PROPERTIES}
     *             or {@link #TYPE_FILE}
     * @param priority Optional priority - if not specified {@link #DEFAULT_PRIORITY}
     *                 is used
     * @throws IllegalArgumentException if something is wrong
     */
    public InstallableResource(final String id,
            final InputStream is,
            final Dictionary<String, Object> dict,
            final String digest,
            final String type,
            final Integer priority) {
        if ( id == null ) {
            throw new IllegalArgumentException("id must not be null.");
        }
        if ( is == null ) {
            // if input stream is null, config through dictionary is expected!
            if ( dict == null ) {
                throw new IllegalArgumentException("dictionary must not be null (or input stream must not be null).");
            }
        }

        this.id = id;
        this.inputStream = is;
        this.dictionary = dict;
        this.digest = digest;
        this.priority = (priority != null ? priority : DEFAULT_PRIORITY);
        this.resourceType = type;
    }

    /**
     * Return this data's id. It is opaque for the {@link OsgiInstaller}
     * but should uniquely identify the resource within the namespace of
     * the used installation mechanism.
     */
    public String getId() {
        return this.id;
    }

    /**
     * Return the type of this resource.
     * @return The resource type or <code>null</code> if the type is unnown for the client.
     */
    public String getType() {
        return this.resourceType;
    }

    /**
     * Return an input stream with the data of this resource.
     * Null if resource contains a configuration instead. Caller is responsible for
     * closing the stream.
     * If this resource is of type CONFIG it must not return an input stream and
     * if this resource is of type BUNDLE it must return an input stream!
     * @return The input stream or null.
     */
    public InputStream getInputStream() {
        return this.inputStream;
    }

    /**
     * Return this resource's dictionary.
     * Null if resource contains an InputStream instead. If this resource is of
     * type CONFIG it must return a dictionary and if this resource is of type BUNDLE
     * it might return a dictionary!
     * @return The resource's dictionary or null.
     */
    public Dictionary<String, Object> getDictionary() {
        return this.dictionary;
    }

    /**
     * Return this resource's digest. Not necessarily an actual md5 or other digest of the
     * data, can be any string that changes if the data changes.
     * @return The digest or null
     */
    public String getDigest() {
        return this.digest;
    }

    /**
     * Return the priority of this resource. Priorities are used to decide which
     * resource to install when several are registered for the same OSGi entity
     * (bundle, config, etc.)
     */
    public int getPriority() {
        return this.priority;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + ", priority=" + priority + ", id=" + id;
    }
}
