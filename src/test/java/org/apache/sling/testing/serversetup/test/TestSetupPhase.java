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
package org.apache.sling.testing.serversetup.test;


import org.apache.sling.testing.serversetup.ServerSetup;
import org.apache.sling.testing.serversetup.SetupPhase;

class TestSetupPhase implements SetupPhase {
    static StringBuilder executionLog;
    static String failingPhases = "";
    private final boolean isStartup;
    private final String id;
    
    TestSetupPhase(String id, boolean isStartup) {
        this.id = id;
        this.isStartup = isStartup;
    }
    
    static void clearExecutionLog() {
        executionLog = new StringBuilder();
    }
    
    public void run(ServerSetup owner) throws Exception {
        if(failingPhases.contains(id)) {
            throw new Exception("Failing as failingPhase contains my id");
        }
        if(executionLog.length() > 0) {
            executionLog.append(",");
        }
        executionLog.append(getId());
    }

    public boolean isStartupPhase() {
        return isStartup;
    }
    
    @Override
    public String toString() {
        return getClass().getSimpleName() + "(" + id + ")";
    }

    public String getId() {
        return id;
    }
}
