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
package org.apache.sling.jcr.ocm.impl.classloader;


abstract class LoaderDelegate {
    protected Loader loader;

    static LoaderDelegate create(Loader loader, String className) {
        return new LoadingDelegate(loader, className);
    }

    static LoaderDelegate create(Loader loader, Class<?> clazz) {
        return new ReturningDelegate(loader, clazz);
    }

    protected LoaderDelegate(Loader loader) {
        this.loader = loader;
    }

    abstract Class<?> loadClass() throws ClassNotFoundException;

    final Object getLoader() {
        return this.loader.getLoader();
    }

    private static class LoadingDelegate extends LoaderDelegate {
        private String className;
        LoadingDelegate(Loader loader, String className) {
            super(loader);
            this.className = className;
        }

        Class<?> loadClass() throws ClassNotFoundException {
            return this.loader.loadClass(this.className);
        }
    }

    private static class ReturningDelegate extends LoaderDelegate {
        private Class<?> clazz;
        ReturningDelegate(Loader loader, Class<?> clazz) {
            super(loader);
            this.clazz = clazz;
        }

        Class<?> loadClass() {
            return this.clazz;
        }
    }
}