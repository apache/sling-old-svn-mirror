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
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;

import java.util.ArrayList;
import java.util.List;

public class URLParameterBuilder {

    public final static String DEFAULT_ENCODING = "UTF-8";

    private List<NameValuePair> params;
    private String encoding;

    public static URLParameterBuilder create() {
        return new URLParameterBuilder();
    }

    URLParameterBuilder() {
        params = new ArrayList<NameValuePair>();
        encoding = DEFAULT_ENCODING;
    }

    public URLParameterBuilder add(String name, String value) {
        params.add(new BasicNameValuePair(name, value));
        return this;
    }

    public URLParameterBuilder add(NameValuePair pair) {
        params.add(pair);
        return this;
    }

    public URLParameterBuilder add(List<NameValuePair> list) {
        params.addAll(list);
        return this;
    }

    public URLParameterBuilder add(String name, String[] values) {
        for (String value : values) this.add(name, value);
        return this;
    }

    public URLParameterBuilder setEncoding(String encoding) {
        this.encoding = encoding;
        return this;
    }

    /**
     * Build the URL parameters
     *
     * @return The URL parameters string without the leading question mark.
     */
    public String getURLParameters() {
        return URLEncodedUtils.format(params, encoding);
    }

    public List<NameValuePair> getList() {
        return params;
    }
}
