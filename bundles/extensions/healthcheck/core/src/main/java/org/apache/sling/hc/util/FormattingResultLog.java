/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The SF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.apache.sling.hc.util;
import org.apache.sling.hc.api.Result;
import org.apache.sling.hc.api.ResultLog;
import org.slf4j.helpers.MessageFormatter;

/** Utility that provides a logging-like facade on a ResultLog */
public class FormattingResultLog extends ResultLog {
    
    private ResultLog.Entry createEntry(Result.Status status, String format, Object ... args) {
        return new ResultLog.Entry(status, MessageFormatter.arrayFormat(format, args).getMessage());
    }
    
    public void debug(String format, Object ... args) {
        add(createEntry(Result.Status.DEBUG, format, args));
    }
    
    public void info(String format, Object ... args) {
        add(createEntry(Result.Status.INFO, format, args));
    }
    
    public void warn(String format, Object ... args) {
        add(createEntry(Result.Status.WARN, format, args));
    }
    
    public void critical(String format, Object ... args) {
        add(createEntry(Result.Status.CRITICAL, format, args));
    }

    public void healthCheckError(String format, Object ... args) {
        add(createEntry(Result.Status.HEALTH_CHECK_ERROR, format, args));
    }
}