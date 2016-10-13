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

package org.apache.sling.commons.log.logback.webconsole;

import java.io.IOException;
import java.io.PrintWriter;

import aQute.bnd.annotation.ProviderType;

@ProviderType
public interface LogPanel {
    /**
     * Request param name to control number of lines to include in the log
     */
    String PARAM_TAIL_NUM_OF_LINES = "tail";
    /**
     * Request param name for appender name
     */
    String PARAM_APPENDER_NAME = "name";

    /**
     * Request param capturing the regular expression to search
     */
    String PARAM_TAIL_GREP = "grep";
    /**
     * Let the path end with extension. In that case WebConsole logic would by pass this request's
     * response completely
     */
    String PATH_TAILER = "tailer.txt";

    String APP_ROOT = "slinglog";

    String RES_LOC = APP_ROOT + "/res/ui";

    void tail(PrintWriter pw, String appenderName, TailerOptions options) throws IOException;

    void render(PrintWriter pw, String consoleAppRoot) throws IOException;

    void deleteLoggerConfig(String pid);

    void createLoggerConfig(LoggerConfig config) throws IOException;
}
