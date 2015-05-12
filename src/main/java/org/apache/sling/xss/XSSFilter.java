/*******************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one or
 * more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information regarding
 * copyright ownership. The ASF licenses this file to you under the
 * Apache License, Version 2.0 (the "License"); you may not use
 * this file except in compliance with the License. You may obtain
 * a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by
 * applicable law or agreed to in writing, software distributed
 * under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions
 * and limitations under the License.
 ******************************************************************************/
package org.apache.sling.xss;

import aQute.bnd.annotation.ProviderType;

/**
 * This service should be used to protect output against potential XSS attacks.
 * The protection is context based.
 */
@ProviderType
public interface XSSFilter {

    /**
     * Default context.
     */
    ProtectionContext DEFAULT_CONTEXT = ProtectionContext.HTML_HTML_CONTENT;

    /**
     * Indicates whether or not a given source string contains XSS policy violations.
     *
     * @param context context to use for checking
     * @param src     source string
     * @return true if the source is violation-free
     * @throws NullPointerException if context is <code>null</code>
     */
    boolean check(ProtectionContext context, String src);

    /**
     * Prevents the given source string from containing XSS stuff.
     * <p/>
     * The default protection context is used for checking.
     *
     * @param src source string
     * @return string that does not contain XSS stuff
     */
    String filter(String src);

    /**
     * Protects the given source string from containing XSS stuff.
     *
     * @param context context to use for checking
     * @param src     source string
     * @return string that does not contain XSS stuff
     * @throws NullPointerException if context is <code>null</code>
     */
    String filter(ProtectionContext context, String src);
}
