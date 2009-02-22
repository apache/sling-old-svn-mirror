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
package org.apache.sling.launchpad.webapp.integrationtest;

import java.io.IOException;

/** A GET to *.html and *.json must work even if there is no Node
 *  at the specified path (SLING-344)
 */
public class GetStarTest extends RenderingTestBase {
    private final String random = getClass().getSimpleName() + String.valueOf(System.currentTimeMillis());

    public void testGetStarHtml() throws IOException {
        getContent(HTTP_BASE_URL + "/*.html", CONTENT_TYPE_HTML);
        getContent(HTTP_BASE_URL + "/" + random + "/*.html", CONTENT_TYPE_HTML);
        getContent(HTTP_BASE_URL + "/" + random + "/" + random + "/*.html", CONTENT_TYPE_HTML);
        getContent(HTTP_BASE_URL + "/" + random + "/*.someselector.html", CONTENT_TYPE_HTML);
    }

    public void testGetStarJson() throws IOException {
        getContent(HTTP_BASE_URL + "/*.json", CONTENT_TYPE_JSON);
        getContent(HTTP_BASE_URL + "/" + random + "/*.json", CONTENT_TYPE_JSON);
        getContent(HTTP_BASE_URL + "/" + random + "/*.12.json", CONTENT_TYPE_JSON);
    }
}
