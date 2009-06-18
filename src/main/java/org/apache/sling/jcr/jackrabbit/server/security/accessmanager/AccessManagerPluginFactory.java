/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.sling.jcr.jackrabbit.server.security.accessmanager;

/**
 * <p>Implementations of this interface, provided as an OSGi service,
 * will be used by {@link org.apache.sling.jcr.jackrabbit.server.impl.security.PluggableDefaultAccessManager
 * PluggableDefaultAccessManager}.</p>
 * <p>This makes it possible to use a custom <code>AccessManager</code> with Sling.</p>
 * <p>See <a href="https://issues.apache.org/jira/browse/SLING-880">SLING-880</a></p>
 */
public interface AccessManagerPluginFactory {

    AccessManagerPlugin getAccessManager();

}
