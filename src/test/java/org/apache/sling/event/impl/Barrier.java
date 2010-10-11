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
package org.apache.sling.event.impl;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/** Simplified version of the cyclic barrier class for testing. */
public class Barrier extends CyclicBarrier {

    public Barrier(int parties) {
        super(parties);
    }

    public void block() {
        try {
            this.await();
        } catch (InterruptedException e) {
            // ignore
        } catch (BrokenBarrierException e) {
            // ignore
        }
    }

    public boolean block(int seconds) {
        try {
            this.await(seconds, TimeUnit.SECONDS);
            return true;
        } catch (InterruptedException e) {
            // ignore
        } catch (BrokenBarrierException e) {
            // ignore
        } catch (TimeoutException e) {
            // ignore
        }
        this.reset();
        return false;
    }
}
