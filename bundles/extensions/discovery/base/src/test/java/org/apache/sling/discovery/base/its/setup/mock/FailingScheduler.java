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
package org.apache.sling.discovery.base.its.setup.mock;

import java.io.Serializable;
import java.util.Date;
import java.util.Map;
import java.util.NoSuchElementException;

import org.apache.sling.commons.scheduler.Scheduler;

public class FailingScheduler implements Scheduler {
    
    @Override
    public void removeJob(String name) throws NoSuchElementException {
        // nothing to do here
    }
    
    @Override
    public boolean fireJobAt(String name, Object job, Map<String, Serializable> config, Date date, int times, long period) {
        return false;
    }
    
    @Override
    public void fireJobAt(String name, Object job, Map<String, Serializable> config, Date date) throws Exception {
        throw new Exception("cos you are really worth it");
    }
    
    @Override
    public boolean fireJob(Object job, Map<String, Serializable> config, int times, long period) {
        return false;
    }
    
    @Override
    public void fireJob(Object job, Map<String, Serializable> config) throws Exception {
        throw new Exception("cos you are really worth it");
    }
    
    @Override
    public void addPeriodicJob(String name, Object job, Map<String, Serializable> config, long period, boolean canRunConcurrently,
            boolean startImmediate) throws Exception {
        throw new Exception("cos you are really worth it");
    }
    
    @Override
    public void addPeriodicJob(String name, Object job, Map<String, Serializable> config, long period, boolean canRunConcurrently)
            throws Exception {
        throw new Exception("cos you are really worth it");
    }
    
    @Override
    public void addJob(String name, Object job, Map<String, Serializable> config, String schedulingExpression,
            boolean canRunConcurrently) throws Exception {
        throw new Exception("cos you are really worth it");
    }
}