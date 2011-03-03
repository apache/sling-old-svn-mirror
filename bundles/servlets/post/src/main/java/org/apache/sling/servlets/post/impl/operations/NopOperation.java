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
package org.apache.sling.servlets.post.impl.operations;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.servlets.post.PostResponse;
import org.apache.sling.servlets.post.SlingPostConstants;
import org.apache.sling.servlets.post.PostOperation;
import org.apache.sling.servlets.post.SlingPostProcessor;

/**
 * The <code>NopOperation</code> class implements no operation at all. It just
 * sets the response status accroding to the <i>:nopstatus</i> parameter if
 * availables. Otherwise the status is set as 200/OK.
 */
public class NopOperation implements PostOperation {

    public void run(SlingHttpServletRequest request, PostResponse response,
            SlingPostProcessor[] processors) {

        // get the :nopstatus parameter for a specific code
        int status = SlingPostConstants.NOPSTATUS_VALUE_DEFAULT;
        String nopStatusString = request.getParameter(SlingPostConstants.RP_NOP_STATUS);
        if (nopStatusString != null) {
            try {
                int nopStatusPar = Integer.parseInt(nopStatusString);
                if (nopStatusPar >= 100 && nopStatusPar <= 999) {
                    status = nopStatusPar;
                }
            } catch (NumberFormatException nfe) {
                // illegal number, use default
            }
        }

        response.setStatus(status, "Null Operation Status: " + status);
    }

}
