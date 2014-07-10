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

package org.apache.sling.junit.performance.impl;

import org.apache.sling.junit.performance.runner.Listener;

import java.util.List;

public class Listeners {

    private boolean isWarmUp;

    private final List<Listener> listeners;

    public Listeners(List<Listener> listeners) {
        this.listeners = listeners;
    }

    private interface Invoker {

        void invoke(Listener listener) throws Exception;

    }

    private void invoke(Invoker invoker) throws Exception {
        for (Listener listener : listeners) {
            invoker.invoke(listener);
        }

    }

    public void warmUpStarted(final String className, final String testName) throws Exception {
        isWarmUp = true;

        invoke(new Invoker() {

            public void invoke(Listener listener) throws Exception {
                listener.warmUpStarted(className, testName);
            }

        });
    }

    public void warmUpFinished(final String className, final String testName) throws Exception {
        isWarmUp = false;

        invoke(new Invoker() {

            public void invoke(Listener listener) throws Exception {
                listener.warmUpFinished(className, testName);
            }

        });
    }

    public void executionStarted(final String className, final String testName) throws Exception {
        invoke(new Invoker() {

            public void invoke(Listener listener) throws Exception {
                listener.executionStarted(className, testName);
            }

        });
    }

    public void executionFinished(final String className, final String testName) throws Exception {
        invoke(new Invoker() {

            public void invoke(Listener listener) throws Exception {
                listener.executionFinished(className, testName);
            }

        });
    }

    public void iterationStarted(final String className, final String testName) throws Exception {
        invoke(new Invoker() {

            public void invoke(Listener listener) throws Exception {
                if (isWarmUp) {
                    listener.warmUpIterationStarted(className, testName);
                } else {
                    listener.executionIterationStarted(className, testName);
                }
            }

        });
    }

    public void iterationFinished(final String className, final String testName) throws Exception {
        invoke(new Invoker() {

            public void invoke(Listener listener) throws Exception {
                if (isWarmUp) {
                    listener.warmUpIterationFinished(className, testName);
                } else {
                    listener.executionIterationFinished(className, testName);
                }
            }

        });
    }

}
