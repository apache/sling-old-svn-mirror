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

import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.servlets.post.SlingPostConstants;

/**
 * Encapsulates all infos from the respective request parameters that are needed
 * to create the repository property
 */
public class RequestProperty {

    private static final RequestParameter[] EMPTY_PARAM_ARRAY = new RequestParameter[0];

    public static final String DEFAULT_IGNORE = SlingPostConstants.RP_PREFIX + "ignore";

    public static final String DEFAULT_NULL = SlingPostConstants.RP_PREFIX + "null";

    private final String path;

    private final String name;

    private final String parentPath;

    private final RequestParameter[] values;

    private String[] stringValues;

    private String typeHint;

    private RequestParameter[] defaultValues = EMPTY_PARAM_ARRAY;

    public RequestProperty(String path, RequestParameter[] values) {
        assert path.startsWith("/");
        this.path = ResourceUtil.normalize(path);
        this.values = values;
        this.parentPath = ResourceUtil.getParent(path);
        this.name = ResourceUtil.getName(path);
    }

    public String getTypeHint() {
        return typeHint;
    }


    public void setTypeHint(String typeHint) {
        this.typeHint = typeHint;
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

    public RequestParameter[] getValues() {
        return values;
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
        return !values[0].isFormField();
    }

    /**
     * Checks if this property provides any values. this is the case if
     * one of the values is not empty or if the default handling is not
     * 'ignore'
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
        for (String s: sv) {
            if (!s.equals("")) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns the assembled string array out of the provided request values
     * and default values.
     * @return a String array or <code>null</code> if the property needs to be
     *         removed.
     */
    public String[] getStringValues() {
        if (stringValues == null) {
            if (values.length > 1) {
                // TODO: how the default values work for MV props is not very clear
                stringValues = new String[values.length];
                for (int i=0; i<stringValues.length; i++) {
                    stringValues[i] = values[i].getString();
                }
            } else {
                String value = values[0].getString();
                if (value.equals("")) {
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
                stringValues = new String[]{value};
            }
        }
        return stringValues;
    }
}