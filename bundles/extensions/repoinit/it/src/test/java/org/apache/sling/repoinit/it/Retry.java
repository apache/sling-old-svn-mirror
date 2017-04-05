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
package org.apache.sling.repoinit.it;

import static org.junit.Assert.fail;
import java.util.concurrent.Callable;

/** RetryRule does not seem to work for teleported tests */
public abstract class Retry implements Callable<Void> {
    public static final int DEFAULT_INTERVAL = 100;
    public static final int DEFAULT_TIMEOUT = 10000;
    
    public Retry() throws Exception {
        this(DEFAULT_TIMEOUT, DEFAULT_INTERVAL);
    }
    
    public Retry(int timeout, int interval) {
        final long endTime = System.currentTimeMillis() + timeout;
        Throwable failure = null;
        while(System.currentTimeMillis() < endTime) {
            try {
                failure = null;
                call();
                break;
            } catch(Throwable t) {
                failure = t;
                try {
                    Thread.sleep(interval);
                } catch(InterruptedException ignore) {
                }
            }
        }
        if(failure != null) {
            if(failure instanceof AssertionError) {
                throw (AssertionError)failure;
            }
            fail("Failed with timeout:" + failure.toString());
        }
    }
}
