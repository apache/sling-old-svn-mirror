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
package org.apache.sling.hc.api;

import java.util.List;

/** The result of evaluating a {@link Rule} */
public class EvaluationResult {
    
    private final Rule rule;
    private final RuleLogger ruleLogger;
    
    /** Log messages at or above this level cause {@link #anythingToReport} to return true */
    public static final LogLevel MIN_LEVEL_TO_REPORT = LogLevel.INFO;
    
    public enum LogLevel {
        TRACE,
        DEBUG,
        INFO,
        WARN,
        ERROR
    }
    
    public static class LogMessage {
        private final LogLevel level;
        private final String message;
        
        public LogMessage(LogLevel level, String message) {
            this.level = level;
            this.message = message;
        }
        
        public String toString() {
            return level + ": " + message;
        }
        
        public LogLevel getLevel() {
            return level;
        }
        
        public String getMessage() {
            return message;
        }
    }
    
    EvaluationResult(Rule r, RuleLogger logger) {
        rule = r;
        ruleLogger = logger;
    }
    
    public Rule getRule() {
        return rule;
    }
    
    /** True if there's anything to report, i.e. if rule execution
     *  logged any messages at the INFO level or above 
     */
    public boolean anythingToReport() {
        return ruleLogger.getMaxLevel().ordinal() >= MIN_LEVEL_TO_REPORT.ordinal();
    }

    /** Return all log messages */
    public List<LogMessage> getLogMessages() {
        return ruleLogger.getMessages();
    }
}
