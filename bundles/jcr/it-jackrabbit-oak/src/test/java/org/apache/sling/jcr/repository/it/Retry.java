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
package org.apache.sling.jcr.repository.it;

import static org.junit.Assert.fail;

/** Simple Retry loop for tests */
public abstract class Retry {
    
    public Retry(int timeoutMsec) {
        final long timeout = System.currentTimeMillis() + timeoutMsec;
        Throwable lastT = null;
        while(System.currentTimeMillis() < timeout) {
            try {
                lastT = null;
                exec();
                break;
            } catch(Throwable t) {
                lastT = t;
            }
        }
        
        if(lastT != null) {
            fail("Failed after " + timeoutMsec + " msec: " + lastT);
        }
    }
    
    protected abstract void exec() throws Exception;
}
