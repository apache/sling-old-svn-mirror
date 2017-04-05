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

package org.apache.sling.tracer.internal;

import javax.servlet.http.HttpServletResponse;

import org.apache.sling.api.request.RequestProgressTracker;
import org.mockito.ArgumentCaptor;

import static java.util.Arrays.asList;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TestUtil {

    static RequestProgressTracker createTracker(String ... logs){
        RequestProgressTracker tracker = mock(RequestProgressTracker.class);
        when(tracker.getMessages()).thenReturn(asList(logs).iterator());
        return tracker;
    }

    static String getRequestId(HttpServletResponse response){
        ArgumentCaptor<String> requestIdCaptor = ArgumentCaptor.forClass(String.class);
        verify(response).setHeader(eq(TracerLogServlet.HEADER_TRACER_REQUEST_ID), requestIdCaptor.capture());
        return requestIdCaptor.getValue();
    }
}
