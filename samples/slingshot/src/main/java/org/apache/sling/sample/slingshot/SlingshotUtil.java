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
package org.apache.sling.sample.slingshot;

import java.util.HashMap;
import java.util.Map;

import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.resource.ValueMap;

public abstract class SlingshotUtil {

    public static String getUserId(final Resource resource) {
        final String prefix = SlingshotConstants.APP_ROOT_PATH + "/";

        String id = null;
        if ( resource.getPath().startsWith(prefix) ) {
            final int areaEnd = resource.getPath().indexOf('/', prefix.length());
            if ( areaEnd != -1 ) {
                final int userEnd = resource.getPath().indexOf('/', areaEnd + 1);
                if ( userEnd == -1 ) {
                    id = resource.getPath().substring(areaEnd + 1);
                } else {
                    id = resource.getPath().substring(areaEnd + 1, userEnd);
                }
            }
        }
        return id;
    }

    public static int getRating(final Resource resource) {
        int rating = 0;

        final Resource ratingsResource = resource.getChild("ratings");
        if ( ratingsResource != null ) {
            int count = 0;
            for(final Resource r : ratingsResource.getChildren()) {
                final ValueMap vm = r.getValueMap();
                final int current = vm.get(SlingshotConstants.PROPERTY_RATING, 0);
                rating += current;
                count++;
            }
            if ( count > 0 ) {
                rating = rating / count;
            }
        }
        return rating;
    }

    public static int getOwnRating(final Resource resource, final String userId) {
        int rating = 0;

        final Resource r = resource.getResourceResolver().getResource(resource.getParent() + "/ratings/" + userId);
        if ( r != null ) {
            final ValueMap vm = r.getValueMap();
            rating = vm.get(SlingshotConstants.PROPERTY_RATING, 0);
        }
        return rating;

    }

    public static void setOwnRating(final Resource resource, final String userId, final int rating)
    throws PersistenceException {
        final Map<String, Object> props = new HashMap<String, Object>();
        props.put(ResourceResolver.PROPERTY_RESOURCE_TYPE, SlingshotConstants.RESOURCETYPE_RATINGS);
        final Resource ratingsResource = ResourceUtil.getOrCreateResource(resource.getResourceResolver(),
                resource.getPath() + "/ratings", props, null, true);

        props.clear();
        props.put(ResourceResolver.PROPERTY_RESOURCE_TYPE, SlingshotConstants.RESOURCETYPE_RATING);
        final Resource r = ResourceUtil.getOrCreateResource(resource.getResourceResolver(),
                ratingsResource.getPath() + "/" + userId, props, null, false);

        final ModifiableValueMap mv = r.adaptTo(ModifiableValueMap.class);
        mv.put(SlingshotConstants.PROPERTY_RATING, rating);

        r.getResourceResolver().commit();
    }
}
