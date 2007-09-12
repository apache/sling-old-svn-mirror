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
package org.apache.sling.core;

import java.util.EventListener;

/**
 * The <code>RequestEventListener2</code> class replaces the
 * {@link org.apache.sling.core.RequestEventListener} interface replacing the
 * original argument list with the {@link RequestEvent}.
 */
public interface RequestEventListener extends EventListener {

    /**
     * Handles request attachement of the indicated request.
     * <p>
     * The attachement event is sent to registered listeners after the
     * <code>Ticket</code> has been extracted from the request and the burst
     * cache was handled. That is, if a request is handled through the burst
     * cache, the attachement event is not sent.
     * <p>
     * Note that though the ticket field of this request object is valid, the
     * request URL has not been mapped yet. Also access has not yet been granted
     * on the request resource.
     *
     * @param event The {@link RequestEvent} for this attachement event.
     */
    void requestAttached(RequestEvent event);

    /**
     * Handles request detachement of the indicated request.
     * <p>
     * The detachment event is sent immediately before the request object is
     * recycled at the end of request processing. Specifically, all script
     * handling including error or exception has already been done for this
     * request.
     * <p>
     * This event is only sent for any given request, if the attachement event
     * event has also been sent. Specifically if the <code>Ticket</code> could
     * not be extracted from the request or if the request was handled through
     * the burst cache, this event is not  sent.
     *
     * @param event The {@link RequestEvent} for this detachement event.
     */
    void requestDetached(RequestEvent event);
}
