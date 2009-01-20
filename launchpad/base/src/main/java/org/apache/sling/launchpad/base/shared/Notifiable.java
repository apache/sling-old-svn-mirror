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
package org.apache.sling.launchpad.base.shared;

import java.io.File;

/**
 * The <code>Notifiable</code> interface is implemented by the real main class
 * and Sling Servlet for them to be notified from the launcher JAR when the
 * framework has been stopped or updated.
 */
public interface Notifiable {

    /**
     * Called when the OSGi framework has been stopped because the
     * <code>Bundle.stop</code> method has been called on the system bundle.
     */
    void stopped();

    /**
     * Called when the OSGi framework has been stopped because any of the
     * <code>Bundle.update</code> methods has been called on the system bundle.
     * <p>
     * If a temporary file is provided in the <code>tmpFile</code> parameter,
     * that file must be used to replace the current Launcher JAR file and must
     * be used for restarting the framework. Otherwise the framework is
     * restarted from the existing Launcher JAR file.
     * 
     * @param tmpFile A temporary file containing the contents of the
     *            <code>InputStream</code> given to the
     *            <code>Bundle.update(InputStream)</code> method. If no input
     *            stream has been provided, this parameter is <code>null</code>.
     */
    void updated(File tmpFile);

}
