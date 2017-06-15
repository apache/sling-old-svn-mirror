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
package org.apache.sling.launchpad.webapp.integrationtest.util;

import java.io.IOException;

import javax.json.JsonException;
import javax.json.JsonObject;

import org.apache.sling.commons.testing.integration.HttpTest;
import org.apache.sling.commons.testing.integration.HttpTestBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RepositoryTestUtil {
    
    private static final Logger log = LoggerFactory.getLogger(RepositoryTestUtil.class);
    
    public static final String DESCRIPTORS_KEY = "descriptors";
    
    public static String getDescriptor(HttpTestBase H, String descriptorName) throws JsonException, IOException {
        final String path = "/testing/RepositoryDescriptors.json";
        final JsonObject json = JsonUtil.parseObject(H.getContent(HttpTest.HTTP_BASE_URL + path, HttpTest.CONTENT_TYPE_JSON));
        return json.getJsonObject("descriptors").getString(descriptorName);
    }
    
    public static void logDescriptors(HttpTestBase H, String ... names) throws JsonException, IOException {
        for(String name : names) {
            log.info("Repository descriptor {}={}", name, getDescriptor(H, name));
        }
    }
}