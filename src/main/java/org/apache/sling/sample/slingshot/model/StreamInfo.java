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
package org.apache.sling.sample.slingshot.model;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;

public class StreamInfo extends PropertiesSupport {

    /** The resource type for a stream info. */
    public static final String RESOURCETYPE = "slingshot/Streaminfo";

    public static final String PROPERTY_TITLE = "title";

    public static final String PROPERTY_DESCRIPTION = "description";

    public static final String PATH_PHOTO = "photo";

    private volatile long entryCount = -1;

    public StreamInfo(final Resource resource) {
        super(resource);
    }

    public String getTitle() {
        String value = this.getProperties().get(PROPERTY_TITLE, String.class);
        if ( value == null ) {
            if ( resource != null ) {
                value = resource.getParent().getName();
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

    public long getEntryCount() {
        if ( entryCount == -1 ) {
            entryCount = 0;
            if ( this.resource != null ) {
                for(final Resource rsrc : this.resource.getParent().getChildren()) {
                    if ( rsrc.isResourceType(StreamEntry.RESOURCETYPE) ) {
                        entryCount++;
                    }
                }
            }
        }
        return entryCount;
    }
}
