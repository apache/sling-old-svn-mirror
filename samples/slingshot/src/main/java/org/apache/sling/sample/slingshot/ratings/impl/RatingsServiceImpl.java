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
package org.apache.sling.sample.slingshot.ratings.impl;

import java.util.HashMap;
import java.util.Map;

import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.sample.slingshot.SlingshotConstants;
import org.apache.sling.sample.slingshot.SlingshotUtil;
import org.apache.sling.sample.slingshot.ratings.RatingsService;
import org.apache.sling.sample.slingshot.ratings.RatingsUtil;
import org.osgi.service.component.annotations.Component;

/**
 * Implementation of the ratings service
 */
@Component(service=RatingsService.class)
public class RatingsServiceImpl implements RatingsService {

    /** The resource type for the rating holder. */
    public static final String RESOURCETYPE_RATINGS = "sling:Folder";

    /**
     * @see org.apache.sling.sample.slingshot.ratings.RatingsService#getRatingsResourcePath(org.apache.sling.api.resource.Resource)
     */
    @Override
    public String getRatingsResourcePath(final Resource resource) {
        final String contentPath = SlingshotUtil.getContentPath(resource);
        if ( contentPath != null ) {
            final String fullPath = SlingshotConstants.APP_ROOT_PATH
                    + "/users/" + SlingshotUtil.getUserId(resource)
                    + "/ugc/ratings" + contentPath;
            return fullPath;
        }
        return null;
    }

    /**
     * @see org.apache.sling.sample.slingshot.ratings.RatingsService#getRating(org.apache.sling.api.resource.Resource)
     */
    @Override
    public int getRating(final Resource resource) {
        final String fullPath = getRatingsResourcePath(resource);
        int rating = 0;
        if ( fullPath != null ) {
            final Resource ratingsResource = resource.getChild(fullPath);
            if ( ratingsResource != null ) {
                int count = 0;
                for(final Resource r : ratingsResource.getChildren()) {
                    final ValueMap vm = r.getValueMap();
                    final int current = vm.get(RatingsUtil.PROPERTY_RATING, 0);
                    rating += current;
                    count++;
                }
                if ( count > 0 ) {
                    rating = rating / count;
                }
            }
        }
        return rating;
    }

    /**
     * @see org.apache.sling.sample.slingshot.ratings.RatingsService#getRating(org.apache.sling.api.resource.Resource, java.lang.String)
     */
    @Override
    public int getRating(final Resource resource, final String userId) {
        final String fullPath = getRatingsResourcePath(resource);
        int rating = 0;

        final Resource r = resource.getResourceResolver().getResource(fullPath + "/" + userId);
        if ( r != null ) {
            final ValueMap vm = r.getValueMap();
            rating = vm.get(RatingsUtil.PROPERTY_RATING, 0);
        }
        return rating;
    }

    /**
     * @see org.apache.sling.sample.slingshot.ratings.RatingsService#setRating(org.apache.sling.api.resource.Resource, java.lang.String, int)
     */
    @Override
    public void setRating(final Resource resource, final String userId, final int rating)
    throws PersistenceException {
        final String ratingsPath = getRatingsResourcePath(resource) ;

        final Map<String, Object> props = new HashMap<String, Object>();
        props.put(ResourceResolver.PROPERTY_RESOURCE_TYPE, RESOURCETYPE_RATINGS);
        final Resource ratingsResource = ResourceUtil.getOrCreateResource(resource.getResourceResolver(),
                ratingsPath, props, null, true);

        final Resource ratingRsrc = resource.getResourceResolver().getResource(ratingsResource, userId);
        if ( ratingRsrc == null ) {
            props.clear();
            props.put(ResourceResolver.PROPERTY_RESOURCE_TYPE, RatingsUtil.RESOURCETYPE_RATING);
            props.put(RatingsUtil.PROPERTY_RATING, rating);

            resource.getResourceResolver().create(ratingsResource, userId, props);
        } else {
            final ModifiableValueMap mvm = ratingRsrc.adaptTo(ModifiableValueMap.class);
            mvm.put(RatingsUtil.PROPERTY_RATING, rating);
        }
        resource.getResourceResolver().commit();
    }
}
