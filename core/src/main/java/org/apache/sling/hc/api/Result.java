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

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** The result of executing a {@link HealthCheck} */
public class Result implements Iterable <ResultLogEntry >{

    private final Logger logger;
    private static final Logger CLASS_LOGGER = LoggerFactory.getLogger(Result.class);
    
    private final List<ResultLogEntry> logEntries = new LinkedList<ResultLogEntry>();
    private Status status = Status.OK;
    
    public enum Status {
        OK,                 // no problem
        WARN,               // health check detected something wrong but not critical
        CRITICAL,           // health check detected a critical problem
        HEALTH_CHECK_ERROR  // health check did not execute properly
    }
    
    /** Build a Result using the default logger */
    public Result() {
        this(null);
    }

    /** Build a Result that logs to a specific logger */
    public Result(Logger logger) {
        this.logger = logger != null ? logger : CLASS_LOGGER;
    }
    
    /** Add an entry to our log. Use the {@ResultLogEntry}.LT_* constants
     *  for well-known entry types.
     *  Adding an entry with a type where {@ResultLogEntry#isInformationalEntryType} returns
     *  false causes our status to be set to WARN, unless it was already set higher.
     */
    public void log(String entryType, String message) {
        if(logger.isDebugEnabled() && ResultLogEntry.LT_DEBUG.equals(entryType)) {
            logger.debug(message);
        } else if(logger.isInfoEnabled() && ResultLogEntry.LT_INFO.equals(entryType)) {
            logger.info(message);
        } else {
            logger.warn(message);
        }
        logEntries.add(new ResultLogEntry(entryType, message));
        if(!ResultLogEntry.isInformationalEntryType(entryType) && status.ordinal() < Status.WARN.ordinal()) {
            logger.warn("Setting Result status to WARN due to log entry of type {}", entryType);
            setStatus(Status.WARN);
        }
    }
    
    /** Set this Result's status. Attempts to set it lower than the current
     *  status are ignored.
     */
    public void setStatus(Status s) {
        if(s.ordinal() > status.ordinal()) {
            status = s;
        } else {
            logger.debug("setStatus({}) ignored as current status {} is higher", s, status);
        }
    }
    
    public Iterator<ResultLogEntry> iterator() {
        return logEntries.iterator();
    }
    
    /** True if our status is OK - just to have a convenient way of 
     *  checking that.
     */
    public boolean isOk() {
        return status.ordinal() == Status.OK.ordinal();
    }
    
    /** Return our Status */
    public Status getStatus() {
        return status;
    }
    
}