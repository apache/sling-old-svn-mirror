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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * General purpose delay object that can be passed around and 
 * plugged in various places identified by an 'operationDescriptor'
 * which can be used to inject delays at runtime
 * @author egli
 *
 */
public class ArtificialDelay {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final Map<String,Long> operationsMap = new ConcurrentHashMap<String, Long>();

    private String debugName;
    
    private final Object syncObj = new Object();
    
    public void setDebugName(String debugName) {
        this.debugName = debugName;
    }

    public void setDelay(String operationDescriptor, long delayMillis) {
        operationsMap.put(operationDescriptor, delayMillis);
        synchronized(syncObj) {
            syncObj.notifyAll();
        }
    }
    
    public void delay(String operationDescriptor) {
        Long delayMillis = operationsMap.get(operationDescriptor);
        if (delayMillis == null) {
            return;
        }
        if (delayMillis <= 0) {
            return;
        }
        logger.info("delay: delaying ["+debugName+"] '"+operationDescriptor+"' for "+delayMillis+"ms...");
        final long start = System.currentTimeMillis();
        synchronized(syncObj) {
            while(true) {
                delayMillis = operationsMap.get(operationDescriptor);
                if (delayMillis == null) {
                    logger.info("delay: delaying ["+debugName+"]'"+operationDescriptor+"' for "+delayMillis+"ms done.");
                    return;
                }
                if (delayMillis <= 0) {
                    logger.info("delay: delaying ["+debugName+"]'"+operationDescriptor+"' for "+delayMillis+"ms done.");
                    return;
                }
                final long end = start + delayMillis;
                long remaining = end - System.currentTimeMillis();
                if (remaining <= 0) {
                    break;
                }
                try {
                    logger.info("delay: delaying ["+debugName+"] '"+operationDescriptor+"' now for "+remaining+"ms...");
                    syncObj.wait(remaining);
                } catch (InterruptedException e) {
                    logger.error("delay: got interrupted: "+e, e);
                }
            }
        }
        logger.info("delay: delaying ["+debugName+"]'"+operationDescriptor+"' for "+delayMillis+"ms done.");
    }

}
