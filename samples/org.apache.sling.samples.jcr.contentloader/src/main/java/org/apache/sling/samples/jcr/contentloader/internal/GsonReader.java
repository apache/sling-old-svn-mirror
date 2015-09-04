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
package org.apache.sling.samples.jcr.contentloader.internal;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import org.apache.commons.io.IOUtils;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.PropertyUnbounded;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.jcr.contentloader.BaseContentReader;
import org.apache.sling.jcr.contentloader.ContentCreator;
import org.apache.sling.jcr.contentloader.ContentReader;
import org.osgi.framework.Constants;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(
    label = "Apache Sling JCR Content Loader Sample GsonReader",
    description = "parses JSON content with Gson",
    immediate = true,
    metatype = true
)
@Service
@Properties({
    @Property(
        name = Constants.SERVICE_VENDOR,
        value = "The Apache Software Foundation"
    ),
    @Property(
        name = Constants.SERVICE_DESCRIPTION,
        value = "Apache Sling JCR Content Loader Sample GsonReader"
    ),
    @Property(
        name = ContentReader.PROPERTY_EXTENSIONS,
        value = {
            "json"
        },
        unbounded = PropertyUnbounded.ARRAY
    ),
    @Property(
        name = ContentReader.PROPERTY_CONTENTTYPES,
        value = {
            "application/json"
        },
        unbounded = PropertyUnbounded.ARRAY
    ),
    @Property(
        name = Constants.SERVICE_RANKING,
        intValue = 1,
        propertyPrivate = false
    )
})
public final class GsonReader extends BaseContentReader implements ContentReader {

    private GsonBuilder gsonBuilder;

    private final Logger logger = LoggerFactory.getLogger(GsonReader.class);

    public GsonReader() {
    }

    @Activate
    protected void activate(final ComponentContext componentContext) {
        configure(componentContext);
        gsonBuilder = new GsonBuilder();
    }

    @Modified
    protected void modified(final ComponentContext componentContext) {
        configure(componentContext);
    }

    @Deactivate
    protected void deactivate(final ComponentContext componentContext) {
        extensions = null;
        contentTypes = null;
        gsonBuilder = null;
    }

    @Override
    public void parse(final URL url, final ContentCreator contentCreator) throws IOException, RepositoryException {
        InputStream inputStream = null;
        try {
            inputStream = url.openStream();
            parse(inputStream, contentCreator);
        } finally {
            IOUtils.closeQuietly(inputStream);
        }
    }

    /**
     * TODO well, implement real parsing...
     */
    @Override
    public void parse(final InputStream inputStream, final ContentCreator contentCreator) throws IOException, RepositoryException {
        final Gson gson = gsonBuilder.create();
        final Reader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
        final JsonObject jsonObject = gson.fromJson(reader, JsonObject.class);
        final String json = jsonObject.toString();
        logger.debug("json: {}", json);
        contentCreator.createNode(null, "nt:unstructured", null);
        contentCreator.createProperty("json", PropertyType.STRING, json);
        contentCreator.finishNode();
    }

}
