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
package org.apache.sling.testing.tools.sling;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Return timeout values that can be multiplied by a configurable
 *  factor. Useful to cope with slower integration testing systems:
 *  use timeout constants in your code that work for usual development
 *  systems, and set a multiplier when running on a slower system.
 */
public class TimeoutsProvider {
    private static final Logger log = LoggerFactory.getLogger(TimeoutsProvider.class);
    public static final String PROP_TIMEOUT_MULTIPLIER = "sling.testing.timeout.multiplier";
    private static float timeoutFactor = -1;
    private static TimeoutsProvider INSTANCE;
    
    private TimeoutsProvider() {
        if(timeoutFactor < 0) {
            timeoutFactor = 1;
            final String str = System.getProperty(PROP_TIMEOUT_MULTIPLIER);
            if(str != null) {
                try {
                    timeoutFactor = Float.valueOf(str.trim());
                    log.info("Timeout factor set to {} from system property {}", 
                            timeoutFactor, PROP_TIMEOUT_MULTIPLIER);
                } catch(NumberFormatException nfe) {
                    throw new IllegalStateException("Invalid timeout factor: " + PROP_TIMEOUT_MULTIPLIER + "=" + str);
                }
            }
        }
    }
    
    public static TimeoutsProvider getInstance() {
        if(INSTANCE == null) {
            synchronized (TimeoutsProvider.class) {
                INSTANCE = new TimeoutsProvider();
            }
        }
        return INSTANCE;
    }
    
    public long getTimeout(long nomimalValue) {
        final long result = (long)(nomimalValue * timeoutFactor);
        return result;
    }
    
    public int getTimeout(int nomimalValue) {
        final int result = (int)(nomimalValue * timeoutFactor);
        return result;
    }
    
    /** Get timeout from a system property, with default value */
    public int getTimeout(String systemPropertyName, int defaultNominalValue) {
        int result = defaultNominalValue;
        final String str = System.getProperty(systemPropertyName);
        if(str != null) {
            result = Integer.parseInt(str);
        }
        return getTimeout(result);
    }
}
