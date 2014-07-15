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

import java.util.ArrayList;
import java.util.List;

public class Listeners {

    private final List<Listener> listeners;

    public Listeners(List<Listener> listeners) {
        this.listeners = listeners;
    }

    private interface Invoker {

        void invoke(Listener listener) throws Exception;

    }

    private List<Throwable> invoke(Invoker invoker) {
        List<Throwable> errors = new ArrayList<Throwable>();

        for (Listener listener : listeners) {
            try {
                invoker.invoke(listener);
            } catch (Throwable t) {
                errors.add(t);
            }
        }

        return errors;
    }

    public List<Throwable> warmUpStarted(final String className, final String testName) {
        return invoke(new Invoker() {

            public void invoke(Listener listener) throws Exception {
                listener.warmUpStarted(className, testName);
            }

        });
    }

    public List<Throwable> warmUpFinished(final String className, final String testName) {
        return invoke(new Invoker() {

            public void invoke(Listener listener) throws Exception {
                listener.warmUpFinished(className, testName);
            }

        });
    }

    public List<Throwable> executionStarted(final String className, final String testName) {
        return invoke(new Invoker() {

            public void invoke(Listener listener) throws Exception {
                listener.executionStarted(className, testName);
            }

        });
    }

    public List<Throwable> executionFinished(final String className, final String testName) {
        return invoke(new Invoker() {

            public void invoke(Listener listener) throws Exception {
                listener.executionFinished(className, testName);
            }

        });
    }

    public List<Throwable> warmUpIterationStarted(final String className, final String testName) {
        return invoke(new Invoker() {

            public void invoke(Listener listener) throws Exception {
                listener.warmUpIterationStarted(className, testName);
            }

        });
    }

    public List<Throwable> warmUpIterationFinished(final String className, final String testName) {
        return invoke(new Invoker() {

            public void invoke(Listener listener) throws Exception {
                listener.warmUpIterationFinished(className, testName);
            }

        });
    }

    public List<Throwable> executionIterationStarted(final String className, final String testName) {
        return invoke(new Invoker() {

            public void invoke(Listener listener) throws Exception {
                listener.executionIterationStarted(className, testName);
            }

        });
    }

    public List<Throwable> executionIterationFinished(final String className, final String testName) {
        return invoke(new Invoker() {

            public void invoke(Listener listener) throws Exception {
                listener.executionIterationFinished(className, testName);
            }

        });
    }

}
