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
package org.apache.sling.scripting.javascript;

import org.mozilla.javascript.Scriptable;

/**
 * The <code>RhinoHostObjectProvider</code> defines the service interface to
 * inject additional ECMAScript host objects as well as to make classes and
 * packages known.
 */
public interface RhinoHostObjectProvider {

    /**
     * Returns an array of classes implementing the Rhino
     * <code>Scriptable</code> interface. These classes will be registered
     * with the global scope as host objects and may then be used in any
     * server-side ECMAScript scripts.
     *
     * @return the host object classes; may return <code>null</code> instead of an empty array for implementations that do not provide
     * any host objects
     */
    Class<? extends Scriptable>[] getHostObjectClasses();

    /**
     * Returns an array of classes, which are transparently converted into
     * ECMAScript host objects in the global scope.
     * <p>
     * Normally any Java class may be used within ECMAScript but it must be
     * prefixed with <code>Packages</code> and the fully qualified package
     * name of the class. For example to use the class
     * <code>org.slf4j.Log</code> in an ECMAScript it must be noted as
     * <code>Packages.org.slf4j.Log</code>. By registering the
     * <code>org.slf4j.Log</code> as an imported class, it may simply be
     * referred to as <code>Log</code> (provided there is no other object of
     * that name, of course).
     *
     * @return the imported classes; may return <code>null</code> instead of an empty array for implementations that do not import any
     * classes
     */
    Class<?>[] getImportedClasses();

    /**
     * Returns an array of Java package names to define name spaces in the
     * global scope.
     * <p>
     * Normally any Java class may be used within ECMAScript but it must be
     * prefixed with <code>Packages</code> and the fully qualified package
     * name of the class. For example to use the class
     * <code>org.slf4j.Log</code> in an ECMAScript it must be noted as
     * <code>Packages.org.slf4j.Log</code>. By registering the
     * <code>org.slf4j</code> package as an imported package, it may simply be
     * referred to as <code>Log</code> (provided there is no other object of
     * that name, of course).
     * <p>
     * The difference between importing packages and importing classes is that
     * for a package import to work, the package must be visible to the
     * ECMAScript bundle.
     * <p>
     * Implementations may return <code>null</code> instead of an empty array
     * if they do not provide any package names.
     *
     * @return the imported packages; may return <code>null</code> instead of an empty array for implementations that do import any packages
     */
    String[] getImportedPackages();
}
