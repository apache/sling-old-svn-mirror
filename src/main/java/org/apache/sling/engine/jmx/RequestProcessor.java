/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.sling.engine.jmx;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.commons.math.stat.descriptive.SynchronizedSummaryStatistics;

/**
 * This is the implementation of the management interface for the RequestProcessor.
 */
public class RequestProcessor implements RequestProcessorMBean {

    private final SynchronizedSummaryStatistics durationStatistics;
    
    private final ExecutorService operationExecutor;

    public RequestProcessor() {
        this.durationStatistics = new SynchronizedSummaryStatistics();
        this.operationExecutor = Executors.newSingleThreadExecutor();
    }

    public void addRequestDuration(final long value) {
        operationExecutor.execute(new Runnable() {

            public void run() {
                durationStatistics.addValue(value);
            }
        });
    }

    public long getCount() {
        return durationStatistics.getN();
    }
    
    public double getMeanRequestDuration() {
        return durationStatistics.getMean();
    }
    
    public void resetStatistics() {
        operationExecutor.execute(new Runnable() {
            
            public void run() {
                durationStatistics.clear();
                
            }
        });
    }

}
