/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements. See the
 * NOTICE file distributed with this work for additional information regarding copyright ownership. The ASF
 * licenses this file to you under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 */

package org.apache.sling.jcr.contentloader.internal;

import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.sling.jcr.contentloader.ContentReader;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

@Component(service = ContentReaderWhiteboard.class,
    property = {
        Constants.SERVICE_VENDOR + "=The Apache Software Foundation"
    })
public class ContentReaderWhiteboard {

    private Map<String, ContentReader> readersByExtension = new LinkedHashMap<>();

    private Map<String, ContentReader> readersByType = new LinkedHashMap<>();

    public Map<String, ContentReader> getReadersByExtension() {
        return readersByExtension;
    }

    public Map<String, ContentReader> getReadersByType() {
        return readersByType;
    }

    @Reference(name = "contentReader", service = ContentReader.class,
            cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    protected void bindContentReader(final ContentReader operation, final Map<String, Object> properties) {
        final String[] extensions = PropertiesUtil.toStringArray(properties.get(ContentReader.PROPERTY_EXTENSIONS));
        final String[] types = PropertiesUtil.toStringArray(properties.get(ContentReader.PROPERTY_TYPES));
        if (extensions != null) {
            synchronized (readersByExtension) {
                for (final String extension : extensions) {
                    readersByExtension.put(extension, operation);
                }
            }
        }
        if (types != null) {
            synchronized (this.readersByType) {
                for (final String type : types) {
                    readersByType.put(type, operation);
                }
            }
        }
    }

    protected void unbindContentReader(final ContentReader operation, final Map<String, Object> properties) {
        final String[] extensions = PropertiesUtil.toStringArray(properties.get(ContentReader.PROPERTY_EXTENSIONS));
        final String[] types = PropertiesUtil.toStringArray(properties.get(ContentReader.PROPERTY_TYPES));
        if (readersByExtension != null && extensions != null) {
            synchronized (readersByExtension) {
                for (final String extension : extensions) {
                    readersByExtension.remove(extension);
                }
            }
        }
        if (types != null) {
            synchronized (readersByType) {
                for (final String type : types) {
                    readersByType.remove(type);
                }
            }
        }
    }
}