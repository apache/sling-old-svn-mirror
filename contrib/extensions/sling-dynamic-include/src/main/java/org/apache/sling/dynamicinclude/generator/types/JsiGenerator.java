/*-
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

package org.apache.sling.dynamicinclude.generator.types;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URL;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.dynamicinclude.generator.IncludeGenerator;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Client side include generator - using Ajax/JQuery.
 */
@Component
@Service
public class JsiGenerator implements IncludeGenerator {
    private static final String TEMPLATE_FILENAME = "generators/jquery.html";

    private static final String UUID_FIELD = "${uniqueId}";

    private static final String URL_FIELD = "${url}";

    private static final Logger LOG = LoggerFactory.getLogger(JsiGenerator.class);

    private static final String GENERATOR_NAME = "JSI";

    private volatile int divId = 1000;

    private String template;

    @Activate
    public void activate(ComponentContext ctx) {
        URL url = ctx.getBundleContext().getBundle().getResource(TEMPLATE_FILENAME);
        if (url == null) {
            LOG.error("File " + TEMPLATE_FILENAME + " not found in bundle.");
            return;
        }
        readTemplateFromUrl(url);
    }

    @Override
    public String getType() {
        return GENERATOR_NAME;
    }

    @Override
    public String getInclude(String url) {
        if (template == null) {
            throw new IllegalStateException("JSI generator hasn't be initialized");
        }

        String divName;
        synchronized (this) {
            divName = "dynamic_include_filter_div_" + divId++;
        }

        return template.replace(UUID_FIELD, divName).replace(URL_FIELD, StringEscapeUtils.escapeJavaScript(url));
    }

    private void readTemplateFromUrl(URL url) {
        BufferedReader br = null;
        try {
            InputStream in = url.openStream();
            br = new BufferedReader(new InputStreamReader(in, "UTF-8"));
            StringBuilder builder = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                builder.append(line).append('\n');
            }
            template = builder.toString();
        } catch (UnsupportedEncodingException e) {
            LOG.error("Error while reading template", e);
        } catch (IOException e) {
            LOG.error("Error while reading template", e);
        } finally {
            try {
                if (br != null) {
                    br.close();
                }
            } catch (Exception e) {
                LOG.error("Error while closing reader", e);
            }
        }
    }

}
