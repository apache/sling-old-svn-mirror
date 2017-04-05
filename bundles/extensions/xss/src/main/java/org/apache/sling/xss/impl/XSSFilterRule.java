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
package org.apache.sling.xss.impl;

/**
 * This interface defines a protection context.
 */
public interface XSSFilterRule {

    /**
     * Check to see if a given string contains policy violations.
     *
     * @param policyHandler the policy handler to use for filtering
     * @param src           the input to check
     * @return true if the source string is free of policy violations (as defined by policyHandler)
     */
    boolean check(PolicyHandler policyHandler, String src);

    /**
     * Filter a given string to remove any policy violations.
     *
     * @param policyHandler the policy handler to use for filtering
     * @param src           the input to filter
     * @return a filtered string which is "safe" (as defined by policyHandler)
     */
    String filter(PolicyHandler policyHandler, String src);

    boolean supportsPolicy();
}
