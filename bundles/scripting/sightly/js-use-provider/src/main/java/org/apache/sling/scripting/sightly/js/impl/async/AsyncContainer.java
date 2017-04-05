/*******************************************************************************
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
 ******************************************************************************/
package org.apache.sling.scripting.sightly.js.impl.async;

import java.util.ArrayList;
import java.util.List;

/**
 * Simple container for asynchronously provided values
 */
public class AsyncContainer {

    private Object value;
    private boolean completed;
    private List<UnaryCallback> callbacks = new ArrayList<UnaryCallback>();

    /**
     * Add a listener that will receive the value in this container when it will
     * be filled. If the container already has a value, the callback is called
     * immediately.
     * @param unaryCallback the callback that will receive the result
     */
    public void addListener(UnaryCallback unaryCallback) {
        callbacks.add(unaryCallback);
        if (completed) {
            notifyListener(unaryCallback);
        }
    }

    /**
     * Get the result of this holder
     * @return the holder result
     * @throws java.util.NoSuchElementException if the result has not yet been set
     */
    public Object getResult() {
        return value;
    }

    /**
     * Check whether the container was completed with a value
     * @return the completion status
     */
    public boolean isCompleted() {
        return completed;
    }

    /**
     * Complete this async container with a value
     * @param value the result value
     * @throws java.lang.IllegalStateException if the container has been previously filled
     */
    public void complete(Object value) {
        if (completed) {
            throw new IllegalStateException("Value was already completed");
        }
        completed = true;
        this.value = value;
        for (UnaryCallback callback : callbacks) {
            notifyListener(callback);
        }
    }

    /**
     * Create a callback that will complete this container
     * @return the completion callback
     */
    public UnaryCallback createCompletionCallback() {
        return new UnaryCallback() {
            @Override
            public void invoke(Object arg) {
                complete(arg);
            }
        };
    }

    private void notifyListener(UnaryCallback unaryCallback) {
        unaryCallback.invoke(value);
    }
}
