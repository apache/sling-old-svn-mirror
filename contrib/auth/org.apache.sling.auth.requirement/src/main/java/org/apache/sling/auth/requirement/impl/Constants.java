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
package org.apache.sling.auth.requirement.impl;

interface Constants {

    /**
     * Name of the mixin node type used to enforce authentication requirement at
     * a given subtree.
     */
    String MIX_SLING_AUTHENTICATION_REQUIRED = "sling:AuthenticationRequired";

    /**
     * Name of the optional property associated with nodes having "sling:AuthenticationRequired"
     * mixin type.
     */
    String NAME_SLING_LOGIN_PATH = "sling:loginPath";
}