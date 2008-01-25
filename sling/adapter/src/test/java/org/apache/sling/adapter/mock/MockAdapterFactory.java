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
package org.apache.sling.adapter.mock;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import org.apache.sling.api.adapter.AdapterFactory;

public class MockAdapterFactory implements AdapterFactory {

    private static final InvocationHandler NOP_INVOCATION_HANDLER = new InvocationHandler() {
        public Object invoke(Object proxy, Method method, Object[] args)
                throws Throwable {
            return null;
        }
    };

    @SuppressWarnings("unchecked")
    public <AdapterType> AdapterType getAdapter(Object adaptable,
            Class<AdapterType> type) {

        try {
            if (type.isInterface()) {
                return (AdapterType) Proxy.newProxyInstance(type.getClassLoader(),
                    new Class[] { type }, NOP_INVOCATION_HANDLER);
            }

            return type.newInstance();
        } catch (Exception e) {
            // ignore
        }

        return null;
    }
}
