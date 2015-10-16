/*******************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 ******************************************************************************/

package org.apache.sling.hapi.client.forms;

import java.io.UnsupportedEncodingException;

import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.jsoup.nodes.Element;

/**
 *
 */
public class FormValues {
    private Element form;

    private Iterable<NameValuePair> values;

    private Vals list = new Vals();

    public FormValues(Element form, Iterable<NameValuePair> values) {
        this.form = form;
        this.values = values;

        build();
        resolve();
    }

    /**
     * @return
     * {@see http://www.w3.org/TR/html5/forms.html#constructing-the-form-data-set}
     */
    private FormValues build() {
        for (Element input : form.select("button, input, select, textarea")) {
            String type = input.attr("type");

            if (input.hasAttr("disabled")) continue;
            if (input.tagName().equalsIgnoreCase("button") && !type.equals("submit")) continue;
            if (input.tagName().equalsIgnoreCase("input") && (type.equals("button") || type.equals("reset"))) continue;
            if (type.equals("checkbox") && input.hasAttr("checked")) continue;
            if (type.equals("radio") && input.hasAttr("checked")) continue;
            if (!type.equals("image") && input.attr("name").length() == 0) continue;
            if (input.parents().is("datalist")) continue;

            if (type.equals("image") || type.equals("file")) continue; // don't support files for now
            String name = input.attr("name");

            if (input.tagName().equalsIgnoreCase("select")) {
                for (Element o : input.select("option[selected]")) {
                    if (o.hasAttr("disabled")) continue;
                    list.add(name, new BasicNameValuePair(name, o.val()));
                }
            } else if (type.equals("checkbox") || type.equals("radio")) {
                String value = input.hasAttr("value") ? input.val() : "on";
                list.add(name, new BasicNameValuePair(name, value));
            } else {
                list.add(name, new BasicNameValuePair(name, input.val()));
            }
        }
        return this;
    }

    private FormValues resolve() {
        for (NameValuePair o : values) {
            if (list.has(o.getName())) {
                list.set(o);
            } else {
                // for now just set the value even if the form doesn't have a submittable input for the name.
                // this is to support custom field that generate input dynamically
                list.set(o);
            }
        }
        return this;
    }

    public String toString() {
        return URLEncodedUtils.format(list.flatten(), "UTF-8");
    }

    public HttpEntity toUrlEncodedEntity() {
        try {
            return new UrlEncodedFormEntity(list.flatten(), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    public HttpEntity toMultipartEntity() {
        MultipartEntityBuilder eb = MultipartEntityBuilder.create();
        for (NameValuePair p : list.flatten()) {
            eb.addTextBody(p.getName(), p.getValue());
        }
        return eb.build();
    }
}
