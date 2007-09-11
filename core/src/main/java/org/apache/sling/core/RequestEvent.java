/*
 * Copyright 2007 The Apache Software Foundation.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at 
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.core;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * The <code>RequestEvent</code> class provides the contents of a request
 * event sent to the methods of the {@link RequestEventListener2} interface.
 */
public class RequestEvent {

    /** The request object attached to this event. */
    private final HttpServletRequest request;

    /** The response object attached to this event. */
    private final HttpServletResponse response;

    /**
     * Creates an event for the given event identification.
     *
     * @param request The request object, which should not be <code>null</code>.
     * @param response The response object, which should not be
     *      <code>null</code>.
     */
    public RequestEvent(HttpServletRequest request,
            HttpServletResponse response) {
        this.response = response;
        this.request = request;
    }

    /**
     * Returns the {@link DeliveryHttpServletRequest request} object pertaining
     * to the HTTP request represented by this event.
     * <p>
     * This property is available for all events.
     */
    public HttpServletRequest getRequest() {
        return request;
    }


    /**
     * Returns the {@link DeliveryHttpServletResponse response} object
     * pertaining to the HTTP request represented by this event.
     * <p>
     * This property is available for all events.
     */
    public HttpServletResponse getResponse() {
        return response;
    }
}
