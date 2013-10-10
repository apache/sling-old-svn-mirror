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
 */package org.apache.sling.ide.transport;

import org.apache.sling.ide.osgi.OsgiClient;

public final class CommandExecutionProperties {

    public static final String REPOSITORY_TOPIC = Repository.class.getPackage().getName().replace('.', '/');
    public static final String OSGI_CLIENT_TOPIC = OsgiClient.class.getPackage().getName().replace('.', '/');

    public static final String TIMESTAMP_START = "timestamp.start";
    public static final String TIMESTAMP_END = "timestamp.end";
    public static final String ACTION_TYPE = "action.type";
    public static final String ACTION_TARGET = "action.target";
    public static final String RESULT_TEXT = "result.txt";
    public static final String RESULT_THROWABLE = "result.throwable";

    private CommandExecutionProperties() {

    }
}
