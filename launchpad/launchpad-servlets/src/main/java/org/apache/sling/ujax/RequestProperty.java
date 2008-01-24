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
package org.apache.sling.ujax;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.request.RequestParameter;

/**
 * Encapsulates all infos from the respective request parameters that are needed
 * to create the repository property
 */
public class RequestProperty {

    private static final RequestParameter[] EMPTY_PARAM_ARRAY = new RequestParameter[0];

    public static final String DEFAULT_IGNORE = UjaxPostServlet.RP_PREFIX + "ignore";

    public static final String DEFAULT_NULL = UjaxPostServlet.RP_PREFIX + "null";

    private final String typeHint;

    private final String keyName;

    private final String propName;

    private final String parentPath;

    private final RequestParameter[] values;

    private RequestParameter[] defaultValues;

    public RequestProperty(SlingHttpServletRequest req, String savePrefix,
                           String keyName, RequestParameter[] values) {
        this.keyName = keyName;
        this.values = values;
        if (savePrefix == null) {
            savePrefix = "";
        }

        // split the relative path identifying the property to be saved
        if (keyName.indexOf("/")>=0) {
            parentPath = keyName.substring(0, keyName.lastIndexOf("/"));
            propName = keyName.substring(keyName.lastIndexOf("/") + 1);
        } else {
            parentPath = "";
            propName = keyName;
        }

        // @TypeHint example
        // <input type="text" name="./age" />
        // <input type="hidden" name="./age@TypeHint" value="long" />
        // causes the setProperty using the 'long' property type
        final String thName = savePrefix + keyName + UjaxPostServlet.TYPE_HINT_SUFFIX;
        final RequestParameter rp = req.getRequestParameter(thName);
        typeHint = rp == null ? null : rp.getString();

        // @DefaultValue
        final String dvName = savePrefix + keyName + UjaxPostServlet.DEFAULT_VALUE_SUFFIX;
        defaultValues = req.getRequestParameters(dvName);
        if (defaultValues == null) {
            defaultValues = EMPTY_PARAM_ARRAY;
        }

    }

    public String getTypeHint() {
        return typeHint;
    }

    public String getKeyName() {
        return keyName;
    }

    public String getName() {
        return propName;
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
        } else {
            for (String s: sv) {
                if (!s.equals("")) {
                    return true;
                }
            }
            return false;
        }
    }

    /**
     * Returns the assembled string array out of the provided request values
     * and default values.
     * @return a String array or <code>null</code> if the property needs to be
     *         removed.
     */
    public String[] getStringValues() {
        if (values.length > 1) {
            // TODO: how the default values work for MV props is not very clear
            String[] ret = new String[values.length];
            for (int i=0; i<ret.length; i++) {
                ret[i] = values[i].getString();
            }
            return ret;
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
            return new String[]{value};
        }
    }
}