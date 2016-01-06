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

package org.apache.sling.metrics.internal;

import java.util.concurrent.TimeUnit;

import org.apache.sling.metrics.Timer;


final class TimerImpl implements Timer {
    private final com.codahale.metrics.Timer timer;

    TimerImpl(com.codahale.metrics.Timer timer) {
        this.timer = timer;
    }

    @Override
    public void update(long duration, TimeUnit unit) {
        timer.update(duration, unit);
    }

    @Override
    public Context time() {
        return new ContextImpl(timer.time());
    }

    @Override
    public long getCount() {
        return timer.getCount();
    }

    @SuppressWarnings("unchecked")
    @Override
    public <A> A adaptTo(Class<A> type) {
        if (type == com.codahale.metrics.Timer.class) {
            return (A) timer;
        }
        return null;
    }

    private static final class ContextImpl implements Context {
        private final com.codahale.metrics.Timer.Context context;

        private ContextImpl(com.codahale.metrics.Timer.Context context) {
            this.context = context;
        }

        public long stop() {
            return context.stop();
        }

        /**
         * Equivalent to calling {@link #stop()}.
         */
        @Override
        public void close() {
            stop();
        }
    }
}
