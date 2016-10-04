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

package org.apache.sling.jobs.it;

import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;

import java.io.IOException;

/**
 */
public class Models {
    public static final int LONG_TIMEOUT_SECONDS = 2; // TODO 10
    public static final int LONG_TIMEOUT_MSEC = LONG_TIMEOUT_SECONDS * 1000;
    public static final int STD_INTERVAL = 250;

    static final String[] DEFAULT_MODELS = {
            "/crankstart-model.txt",
            "/provisioning-model/base.txt",
            "/provisioning-model/jobs-runtime.txt",
            "/provisioning-model/crankstart-test-support.txt"
    };

    static void closeConnection(HttpResponse r) throws IOException {
        if(r != null && r.getEntity() != null) {
            EntityUtils.consume(r.getEntity());
        }
    }
}
