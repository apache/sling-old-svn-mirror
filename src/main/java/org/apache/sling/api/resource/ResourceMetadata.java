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
package org.apache.sling.api.resource;

import java.util.HashMap;

/**
 * The <code>ResourceMetadata</code> interface defines the API for the
 * metadata of a Sling {@link Resource}. Essentially the resource's metadata is
 * just a map of objects indexed by string keys.
 * <p>
 * The actual contents of the meta data map is implementation specific with the
 * exception the {@link #RESOLUTION_PATH sling.resolutionPath} and
 * {@link #RESOLUTION_PATH_INFO sling.resolutionPathInfo} properties which must
 * be provided by all implementations and contain the parts of the request URI
 * used to resolve the resource. The type of these property values is defined to
 * be <code>String</code>.
 * <p>
 * Note, that the prefix <em>sling.</em> to key names is reserved for the
 * Sling implementation.
 */
public class ResourceMetadata extends HashMap<String, Object> {

    /**
     * The name of the required property providing the part of the request URI
     * which was used to the resolve the resource to which the meta data
     * instance belongs (value is "sling.resolutionPath").
     */
    public static final String RESOLUTION_PATH = "sling.resolutionPath";

    /**
     * The name of the required property providing the part of the request URI
     * which was not used to the resolve the resource to which the meta data
     * instance belongs (value is "sling.resolutionPathInfo"). The value of this
     * property concatenated to the value of the
     * {@link #RESOLUTION_PATH sling.resolutionPath} property returns the
     * original request URI leading to the resource.
     * 
     * @since 2.0.3-incubator-SNAPSHOT
     */
    public static final String RESOLUTION_PATH_INFO = "sling.resolutionPathInfo";

    /**
     * The name of the optional property providing the content type of the
     * resource if the resource is streamable (value is "sling.contentType").
     * This property may be missing if the resource is not streamable or if the
     * content type is not known.
     */
    public static final String CONTENT_TYPE = "sling.contentType";

    /**
     * The name of the optional property providing the content length of the
     * resource if the resource is streamable (value is "sling.contentLength").
     * This property may be missing if the resource is not streamable or if the
     * content length is not known.
     * <p>
     * Note, that unlike the other properties, this property may be set only
     * after the resource has successfully been adapted to an
     * <code>InputStream</code> for performance reasons.
     */
    public static final String CONTENT_LENGTH = "sling.contentLength";

    /**
     * The name of the optional property providing the character encoding of the
     * resource if the resource is streamable and contains character data (value
     * is "sling.characterEncoding"). This property may be missing if the
     * resource is not streamable or if the character encoding is not known.
     */
    public static final String CHARACTER_ENCODING = "sling.characterEncoding";

    /**
     * Returns the creation time of this resource in the repository in
     * milliseconds (value is "sling.creationTime"). The type of this property
     * is <code>java.lang.Long</code>. The property may be missing if the
     * resource is not streamable or if the creation time is not known.
     */
    public static final String CREATION_TIME = "sling.creationTime";

    /**
     * Returns the last modification time of this resource in the repository in
     * milliseconds (value is "sling.modificationTime"). The type of this
     * property is <code>java.lang.Long</code>. The property may be missing
     * if the resource is not streamable or if the last modification time is not
     * known.
     */
    public static final String MODIFICATION_TIME = "sling.modificationTime";

    /**
     * Sets the {@link #CHARACTER_ENCODING} property to <code>encoding</code>
     * if not <code>null</code>.
     */
    public void setCharacterEncoding(String encoding) {
        if (encoding != null) {
            put(CHARACTER_ENCODING, encoding);
        }
    }

    /**
     * Returns the {@link #CHARACTER_ENCODING} property if not <code>null</code>
     * and a <code>String</code> instance. Otherwise <code>null</code> is
     * returned.
     */
    public String getCharacterEncoding() {
        Object value = get(CHARACTER_ENCODING);
        if (value instanceof String) {
            return (String) value;
        }

        return null;
    }

    /**
     * Sets the {@link #CONTENT_TYPE} property to <code>contentType</code> if
     * not <code>null</code>.
     */
    public void setContentType(String contentType) {
        if (contentType != null) {
            put(CONTENT_TYPE, contentType);
        }
    }

    /**
     * Returns the {@link #CONTENT_TYPE} property if not <code>null</code> and
     * a <code>String</code> instance. Otherwise <code>null</code> is
     * returned.
     */
    public String getContentType() {
        Object value = get(CONTENT_TYPE);
        if (value instanceof String) {
            return (String) value;
        }

        return null;
    }

    /**
     * Sets the {@link #CONTENT_LENGTH} property to <code>contentType</code>
     * if not <code>null</code>.
     */
    public void setContentLength(long contentLength) {
        if (contentLength > 0) {
            put(CONTENT_LENGTH, contentLength);
        }
    }

    /**
     * Returns the {@link #CONTENT_LENGTH} property if not <code>null</code>
     * and a <code>long</code>. Otherwise <code>-1</code> is returned.
     */
    public long getContentLength() {
        Object value = get(CONTENT_LENGTH);
        if (value instanceof Long) {
            return (Long) value;
        }

        return -1;
    }

    /**
     * Sets the {@link #CREATION_TIME} property to <code>creationTime</code>
     * if not negative.
     */
    public void setCreationTime(long creationTime) {
        if (creationTime >= 0) {
            put(CREATION_TIME, creationTime);
        }
    }

    /**
     * Returns the {@link #CREATION_TIME} property if not <code>null</code>
     * and a <code>long</code>. Otherwise <code>-1</code> is returned.
     */
    public long getCreationTime() {
        Object value = get(CREATION_TIME);
        if (value instanceof Long) {
            return (Long) value;
        }

        return -1;
    }

    /**
     * Sets the {@link #MODIFICATION_TIME} property to
     * <code>modificationTime</code> if not negative.
     */
    public void setModificationTime(long modificationTime) {
        if (modificationTime >= 0) {
            put(MODIFICATION_TIME, modificationTime);
        }
    }

    /**
     * Returns the {@link #MODIFICATION_TIME} property if not <code>null</code>
     * and a <code>long</code>. Otherwise <code>-1</code> is returned.
     */
    public long getModificationTime() {
        Object value = get(MODIFICATION_TIME);
        if (value instanceof Long) {
            return (Long) value;
        }

        return -1;
    }

    /**
     * Sets the {@link #RESOLUTION_PATH} property to <code>resolutionPath</code>
     * if not <code>null</code>.
     */
    public void setResolutionPath(String resolutionPath) {
        if (resolutionPath != null) {
            put(RESOLUTION_PATH, resolutionPath);
        }
    }

    /**
     * Returns the {@link #RESOLUTION_PATH} property if not <code>null</code>
     * and a <code>String</code> instance. Otherwise <code>null</code> is
     * returned.
     */
    public String getResolutionPath() {
        Object value = get(RESOLUTION_PATH);
        if (value instanceof String) {
            return (String) value;
        }

        return null;
    }

    /**
     * Sets the {@link #RESOLUTION_PATH_INFO} property to
     * <code>resolutionPathInfo</code> if not <code>null</code>.
     */
    public void setResolutionPathInfo(String resolutionPathInfo) {
        if (resolutionPathInfo != null) {
            put(RESOLUTION_PATH_INFO, resolutionPathInfo);
        }
    }

    /**
     * Returns the {@link #RESOLUTION_PATH_INFO} property if not
     * <code>null</code> and a <code>String</code> instance. Otherwise
     * <code>null</code> is returned.
     */
    public String getResolutionPathInfo() {
        Object value = get(RESOLUTION_PATH_INFO);
        if (value instanceof String) {
            return (String) value;
        }

        return null;
    }
}
