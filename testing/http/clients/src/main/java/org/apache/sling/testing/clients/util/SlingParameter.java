/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.sling.testing.clients.util;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class SlingParameter {

    private String typeHint = null;
    private boolean delete = false;

    String parameterName;
    private String[] values = null;
    private boolean multiple = false;

    public SlingParameter(String parameterName) {
        if (parameterName == null || parameterName.length() == 0) {
            throw new IllegalArgumentException("parameterName must not be null or empty");
        }
        this.parameterName = parameterName;
    }

    public SlingParameter value(String value) {
        if (value != null) {
            this.values(new String[]{value});
        } else {
            this.values(new String[]{});
        }
        return this;
    }

    public SlingParameter values(String[] values) {
        if (values == null) {
            this.values = new String[]{};
        } else {
            this.values = values;
        }
        return this;
    }

    public SlingParameter typeHint(String typeHint) {
        this.typeHint = typeHint;
        return this;
    }

    public SlingParameter delete() {
        this.delete = true;
        return this;
    }

    public SlingParameter multiple() {
        this.multiple = true;
        return this;
    }

    public List<NameValuePair> toNameValuePairs() {
        List<NameValuePair> parameters = new ArrayList<NameValuePair>();

        if (multiple) {
            for (String value : values) {
                parameters.add(new BasicNameValuePair(parameterName, value));
            }
        } else if (values != null && values.length == 1) {
            parameters.add(new BasicNameValuePair(parameterName, values[0]));
        } else if (values != null && values.length > 1) {
            // TODO not sure about the proper format of the values in this case?
            // For now, only take the first one.
            parameters.add(new BasicNameValuePair(parameterName, values[0]));
        } else {
            parameters.add(new BasicNameValuePair(parameterName, null));
        }

        // add @TypeHint suffix
        if (typeHint != null) {
            String parameter = parameterName + "@TypeHint";
            parameters.add(new BasicNameValuePair(parameter, typeHint));
        }

        // add @Delete suffix
        if (delete) {
            String parameter = parameterName + "@Delete";
            parameters.add(new BasicNameValuePair(parameter, "true"));
        }

        return parameters;
    }
}
