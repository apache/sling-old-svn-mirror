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
package org.apache.sling.installer.provider.jcr.impl;

/** Used to wait at least SCAN_DELAY_MSEC before rescanning
 *  things after receiving a JCR Event. This avoids useless
 *  scanning, as events usually come in bursts. 
 */

class RescanTimer {
    public static final long DEFAULT_SCAN_DELAY_MSEC = 500L;
    private long nextScanTime = Long.MAX_VALUE;
    private final long scanDelay;
    
    /** Create a timer with a non-default delay - this should ONLY
     *  be used in tests for now, as the existing code makes
     *  some assumptions about this. 
     * @param scanDelayMsec delay in msec
     */
    RescanTimer(long scanDelayMsec) {
        this.scanDelay = scanDelayMsec;
    }
    
    /** Standard constructor for non-test code */
    RescanTimer() {
        this(DEFAULT_SCAN_DELAY_MSEC);
    }
    
    synchronized void scheduleScan() {
        nextScanTime = System.currentTimeMillis() + scanDelay;
    }
    
    synchronized void reset() {
        nextScanTime = Long.MAX_VALUE;
    }
    
    boolean expired() {
        return System.currentTimeMillis() > nextScanTime;
    }
}
