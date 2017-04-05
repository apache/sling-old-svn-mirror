package org.apache.sling.launchpad.webapp.integrationtest;

/* Licensed to the Apache Software Foundation (ASF) under one or more
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
import java.io.IOException;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.sling.commons.testing.integration.HttpTestBase;

public class FiltersTest extends HttpTestBase {

    public void testCounters() throws IOException {
        HttpMethod get = assertHttpStatus(HTTP_BASE_URL + "/index.html", HttpServletResponse.SC_OK);
        final String [] headers = {
            "FILTER_COUNTER_SLING",
            "FILTER_COUNTER_NOPROP"
        };
        for(String header : headers) {
            assertNotNull("Expecting header '" + header + "'", get.getResponseHeader(header));
            assertEquals("Expecting value 1 for header '" + header + "'", "1", get.getResponseHeader(header).getValue());
        }
        assertNull(get.getResponseHeader("FILTER_COUNTER_SLING_WITH_PATTERN"));
    }
}
