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

package org.apache.sling.replication.packaging;


/**
 * Additional information about a package.
 * Additional information is optional and components should expect every piece of it to be null.
 */
public interface ReplicationPackageInfo {

    /**
     * retrieves the origin of the package.
     * @return the package origin
     */
    String getOrigin();

    /**
     * sets the origin of the package.
     * @param origin the originating instance of this package
     */
    void setOrigin(String origin);

    /**
     * fills the current info object from the provided one.
     * @param packageInfo package metadata
     */
    void fillInfo(ReplicationPackageInfo packageInfo);
}
