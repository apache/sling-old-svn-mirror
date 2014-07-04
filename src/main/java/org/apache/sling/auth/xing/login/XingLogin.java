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
package org.apache.sling.auth.xing.login;

/**
 * for <code>XING_COOKIE_PREFIX</code> and <code>HASH_ALGORITHM</code>
 * see chapter <i>5. Server-side verification of the user data</i> from
 * <code>Documentation-Login-with-XING-plugin-110414.pdf</code>
 */
public class XingLogin {

    public static final String AUTH_TYPE = "xing-login";

    public static final String AUTHENTICATION_CREDENTIALS_HASH_KEY = "xing-hash";

    public static final String AUTHENTICATION_CREDENTIALS_USERDATA_KEY = "xing-userdata";

    public static final String XING_COOKIE_PREFIX = "xing_p_lw_s_";

    public static final String HASH_ALGORITHM = "HmacSHA256";

    public static final String SERVICE_VENDOR = "The Apache Software Foundation";

}
