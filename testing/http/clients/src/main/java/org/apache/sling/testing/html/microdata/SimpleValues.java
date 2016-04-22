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
package org.apache.sling.testing.html.microdata;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.apache.sling.testing.itframework.client.html.Values;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class SimpleValues implements Values {
    private List<NameValuePair> params = new ArrayList<NameValuePair>();

    public SimpleValues add(String name, String value) {
        params.add(new BasicNameValuePair(name, value));
        return this;
    }

    public SimpleValues add(String name, boolean value) {
        add(name, Boolean.toString(value));
        return this;
    }

    @Override
    public Iterator<NameValuePair> iterator() {
        return params.iterator();
    }
}
