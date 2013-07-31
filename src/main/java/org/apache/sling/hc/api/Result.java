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

/** The result of executing a {@link HealthCheck} */
public class Result {
    
    private final HealthCheck healthCheck;
    private final ResultLog log;
    
    public Result(HealthCheck hc, ResultLog log) {
        healthCheck = hc;
        this.log = log;
    }

    public HealthCheck getHealthCheck() {
        return healthCheck;
    }
    
    public List<ResultLog.Entry> getLogEntries() {
        return log.getEntries();
    }
    
    public boolean isOk() {
        return log.getMaxLevel() != null && log.getMaxLevel().ordinal() < ResultLog.MIN_LEVEL_TO_REPORT.ordinal();
    }
    
    public ResultLog.Level getStatus() {
        return isOk() ? ResultLog.Level.OK : log.getMaxLevel(); 
    }
}
