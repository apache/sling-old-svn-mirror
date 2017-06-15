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

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;

public abstract class SlingshotUtil {

    /**
     * Get the user id for a resource
     * @param resource The resource of a user
     * @return The user id or {@code null}.
     */
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

    /**
     * Get the user content path for the resource
     * @param resource The resource
     * @return The user content path or {@code null}.
     */
    public static String getContentPath(final Resource resource) {
        final String prefix = SlingshotConstants.APP_ROOT_PATH + "/users/" + getUserId(resource) + "/";

        final String path = resource.getPath();
        if ( path != null && path.startsWith(prefix) ) {
            return path.substring(prefix.length() - 1);
        }
        return null;
    }

    /**
     * Checks whether the request is from a known user or from anonymous
     * @param request The request
     * @return {@code true} if it is a known user.
     */
    public static boolean isUser(final SlingHttpServletRequest request) {
        final boolean isUser = request.getRemoteUser() != null && !request.getRemoteUser().equals("anonymous");
        return isUser;
    }

}
