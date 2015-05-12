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

import java.io.IOException;
import java.io.InputStream;

import org.owasp.validator.html.AntiSamy;
import org.owasp.validator.html.Policy;

/**
 * Class that provides the capability of securing input provided as plain text for HTML output.
 */
public class PolicyHandler {

    private Policy policy;
    private AntiSamy antiSamy;

    /**
     * Try to load a policy from the given relative path.
     */
    public PolicyHandler(InputStream policyStream) throws Exception {
        // fix for classloader issue with IBM JVM: see bug #31946
        // (currently: http://bugs.day.com/bugzilla/show_bug.cgi?id=31946)
        Thread currentThread = Thread.currentThread();
        ClassLoader cl = currentThread.getContextClassLoader();
        try {
            currentThread.setContextClassLoader(this.getClass().getClassLoader());
            this.policy = Policy.getInstance(policyStream);
            this.antiSamy = new AntiSamy(this.policy);
        } finally {
            if (policyStream != null) {
                try {
                    policyStream.close();
                } catch (final IOException ioe) {
                    // ignored as we can't do anything about this (besides logging)
                }
            }
            currentThread.setContextClassLoader(cl);
        }
    }

    public Policy getPolicy() {
        return this.policy;
    }

    public AntiSamy getAntiSamy() {
        return this.antiSamy;
    }
}
