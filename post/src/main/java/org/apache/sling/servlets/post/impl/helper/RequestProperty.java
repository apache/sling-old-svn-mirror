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
package org.apache.sling.servlets.post.impl.helper;

import java.util.ArrayList;
import java.util.List;

import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.servlets.post.SlingPostConstants;

/**
 * Encapsulates all infos from the respective request parameters that are needed
 * to create the repository property
 */
public class RequestProperty {

    private static final RequestParameter[] EMPTY_PARAM_ARRAY = new RequestParameter[0];

    public static final String DEFAULT_IGNORE = SlingPostConstants.RP_PREFIX
        + "ignore";

    public static final String DEFAULT_NULL = SlingPostConstants.RP_PREFIX
        + "null";

    private final String path;

    private final String name;

    private final String parentPath;

    private RequestParameter[] values;

    private String[] stringValues;

    private String typeHint;

    private boolean hasMultiValueTypeHint;

    private RequestParameter[] defaultValues = EMPTY_PARAM_ARRAY;

    private boolean isDelete;

    private String repositoryResourcePath;

    private boolean isRepositoryResourceMove;

    private boolean ignoreBlanks;

    private boolean useDefaultWhenMissing;

    private boolean patch = false;

    private Chunk chunk;

    public RequestProperty(String path) {
        assert path.startsWith("/");
        this.path = ResourceUtil.normalize(path);
        this.parentPath = ResourceUtil.getParent(path);
        this.name = ResourceUtil.getName(path);
    }

    public String getTypeHint() {
        return typeHint;
    }

    public boolean hasMultiValueTypeHint() {
        return this.hasMultiValueTypeHint;
    }

    public void setTypeHintValue(String typeHint) {
        if ( typeHint != null && typeHint.endsWith("[]") ) {
            this.typeHint = typeHint.substring(0, typeHint.length() - 2);
            this.hasMultiValueTypeHint = true;
        } else {
            this.typeHint = typeHint;
            this.hasMultiValueTypeHint = false;
        }
    }

    public String getPath() {
        return path;
    }

    public String getName() {
        return name;
    }

    public String getParentPath() {
        return parentPath;
    }

    public boolean hasValues() {
        if (useDefaultWhenMissing && defaultValues != null && defaultValues.length > 0) {
            return true;
        } else {
            if (ignoreBlanks) {
                return (values != null && getStringValues().length > 0);
            } else {
                return values != null;
            }
        }
    }

    public RequestParameter[] getValues() {
        return values;
    }

    public void setValues(RequestParameter[] values) {
        this.values = values;
    }

    public RequestParameter[] getDefaultValues() {
        return defaultValues;
    }

    public void setDefaultValues(RequestParameter[] defaultValues) {
        if (defaultValues == null) {
            this.defaultValues = EMPTY_PARAM_ARRAY;
        } else {
            this.defaultValues = defaultValues;
        }
    }

    public boolean isFileUpload() {
        return values != null && !values[0].isFormField();
    }

    /**
     * Checks if this property provides any values. this is the case if one of
     * the values is not empty or if the default handling is not 'ignore'
     *
     * @return <code>true</code> if this property provides values
     */
    public boolean providesValue() {
        // should void double creation of string values
        String[] sv = getStringValues();
        if (sv == null) {
            // is missleading return type. but means that property should not
            // get auto-create values
            return true;
        }
        for (String s : sv) {
            if (!s.equals("")) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns the assembled string array out of the provided request values and
     * default values.
     *
     * @return a String array or <code>null</code> if the property needs to be
     *         removed.
     */
    public String[] getStringValues() {
        if (stringValues == null) {
            if (values == null && useDefaultWhenMissing) {
                stringValues = new String[] { defaultValues[0].getString() };
            } else if (values.length > 1) {
                // TODO: how the default values work for MV props is not very
                // clear
                List<String> stringValueList = new ArrayList<String>(values.length);
                for (int i = 0; i < values.length; i++) {
                    String value = values[i].getString();
                    if ((!ignoreBlanks) || value.length() > 0) {
                        stringValueList.add(value);
                    }
                }
                stringValues = stringValueList.toArray(new String[stringValueList.size()]);
            } else {
                String value = values[0].getString();
                if (value.equals("")) {
                    if (ignoreBlanks) {
                        return new String[0];
                    } else {
                        if (defaultValues.length == 1) {
                            String defValue = defaultValues[0].getString();
                            if (defValue.equals(DEFAULT_IGNORE)) {
                                // ignore means, do not create empty values
                                return new String[0];
                            } else if (defValue.equals(DEFAULT_NULL)) {
                                // null means, remove property if exist
                                return null;
                            }
                            value = defValue;
                        }
                    }
                }
                stringValues = new String[] { value };
            }
        }
        return stringValues;
    }

    /**
     * Specifies whether this property should be deleted before any new content
     * is to be set according to the values stored.
     *
     * @param isDelete <code>true</code> if the repository item described by
     *            this is to be deleted before any other operation.
     */
    public void setDelete(boolean isDelete) {
        this.isDelete = isDelete;
    }

    /**
     * Returns <code>true</code> if the repository item described by this is
     * to be deleted before setting new content to it.
     */
    public boolean isDelete() {
        return isDelete;
    }

    /**
     * Sets the path of the repository item from which the content for this
     * property is to be copied or moved. The path may be relative in which case
     * it will be resolved relative to the absolute path of this property.
     *
     * @param sourcePath The path of the repository item to get the content from
     * @param isMove <code>true</code> if the source content is to be moved,
     *            otherwise the source content is copied from the repository
     *            item.
     */
    public void setRepositorySource(String sourcePath, boolean isMove) {

        // make source path absolute
        if (!sourcePath.startsWith("/")) {
            sourcePath = getParentPath() + "/" + sourcePath;
            sourcePath = ResourceUtil.normalize(sourcePath);
        }

        this.repositoryResourcePath = sourcePath;
        this.isRepositoryResourceMove = isMove;
    }

    /**
     * Returns <code>true</code> if the content of this property is to be set
     * by moving content from another repository item.
     *
     * @see #getRepositorySource()
     */
    public boolean hasRepositoryMoveSource() {
        return isRepositoryResourceMove;
    }

    /**
     * Returns <code>true</code> if the content of this property is to be set
     * by copying content from another repository item.
     *
     * @see #getRepositorySource()
     */
    public boolean hasRepositoryCopySource() {
        return getRepositorySource() != null && !hasRepositoryMoveSource();
    }

    /**
     * Returns the absolute path of the repository item from which the content
     * for this property is to be copied or moved.
     *
     * @see #hasRepositoryCopySource()
     * @see #hasRepositoryMoveSource()
     * @see #setRepositorySource(String, boolean)
     */
    public String getRepositorySource() {
        return repositoryResourcePath;
    }

    public void setIgnoreBlanks(boolean b) {
        ignoreBlanks = b;
    }

    public void setUseDefaultWhenMissing(boolean b) {
        useDefaultWhenMissing = b;
    }

    public void setPatch(boolean b) {
        patch = b;
    }

    /**
     * Returns whether this property is to be handled as a multi-value property
     * seen as set.
     */
    public boolean isPatch() {
        return patch;
    }

    /**
     *  Return true if request is chunk upload.
     */
    public boolean isChunkUpload() {
        return chunk != null;
    }

    public Chunk getChunk() {
        return chunk;
    }

    public void setChunk(Chunk chunk) {
        this.chunk = chunk;
    }
}
