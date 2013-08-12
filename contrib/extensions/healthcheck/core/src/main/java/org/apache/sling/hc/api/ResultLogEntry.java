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



/** A entry in the log of a {@link Result} */
public class ResultLogEntry {
    
    private final String entryType;
    private final String message;
    
    /** Standard log entry type DEBUG */
    public static final String LT_DEBUG = "DEBUG";
    
    /** Standard log entry type INFO: general informational message */
    public static final String LT_INFO = "INFO";
    
    /** Standard log entry type WARN: general warning message */
    public static final String LT_WARN = "WARN";
    
    /** Standard log entry type SECURITY: security-related warning */
    public static final String LT_WARN_SECURITY = "SECURITY";
    
    /** Standard log entry type: configuration-related warning */
    public static final String LT_WARN_CONFIG = "CONFIG";
    
    /** Build a log entry.
     * @param entryType The type of this log entry, this is a String instead of an Enum
     *  so that health checks can invent their own types if needed.
     *  For the usual entry types, use the LT_* constants of this class.
     *  By convention, any log entry that's not LT_DEBUG or LT_INFO causes
     *  the Result to move to the WARN result state, unless it already was set to a higher state.
     *  
     * @param message The log message.
     */
    ResultLogEntry(String entryType, String message) {
        this.entryType = entryType;
        this.message = message;
    }

    public String getEntryType() {
        return entryType;
    }

    /** The log message */
    public String getMessage() {
        return message;
    }

    @Override
    public String toString() {
        return new StringBuilder(entryType).append(" ").append(message).toString();
    }
    
    /** True if the given entryType is one that does not cause Result to
     *  raise its state to WARN.
     */
    public static boolean isInformationalEntryType(String entryType) {
        return LT_DEBUG.equals(entryType) || LT_INFO.equals(entryType);
    }
}
