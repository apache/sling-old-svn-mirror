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
package org.apache.sling.sample.slingshot.model;

import org.apache.sling.api.resource.Resource;

public class StreamEntry extends PropertiesSupport {

    /** The resource type for a stream entry. */
    public static final String RESOURCETYPE = "slingshot/Streamentry";

    public static final String PROPERTY_TITLE = "title";

    public static final String PROPERTY_DESCRIPTION = "description";

    public static final String PROPERTY_LOCATION = "location";

    public static final String PROPERTY_TAGS = "tags";

    private volatile Stream stream;

    public StreamEntry(final Resource resource) {
        super(resource);
    }

    public String getTitle() {
        String value = this.getProperties().get(PROPERTY_TITLE, String.class);
        if ( value == null ) {
            if ( resource != null ) {
                value = resource.getName();
            } else {
                value = "No Title";
            }
        }
        return value;
    }

    public String getDescription() {
        final String value = this.getProperties().get(PROPERTY_DESCRIPTION, "");
        return value;
    }

    public Stream getStream() {
        if ( this.stream == null ) {
            if ( resource == null ) {
                stream = new Stream(null);
            } else {
                Resource rsrc = this.resource.getParent();
                while (rsrc != null && !rsrc.isResourceType(Stream.RESOURCETYPE) ) {
                    rsrc = rsrc.getParent();
                }
                stream = new Stream(rsrc);
            }
        }
        return stream;
    }

    public String getLocation() {
        return this.getProperties().get(PROPERTY_LOCATION, String.class);
    }
}
