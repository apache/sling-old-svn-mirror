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


/**
 * This enumeration defines the context for executing XSS protection.
 * <p/>
 * The specified rules refer to
 * http://www.owasp.org/index.php/XSS_%28Cross_Site_Scripting%29_Prevention_Cheat_Sheet
 */
public enum ProtectionContext {
    /**
     * Escape HTML for use inside element content (rules #6 and - to some degree - #1),
     * using a policy to remove potentially malicous HTML
     */
    HTML_HTML_CONTENT("htmlToHtmlContent"),

    /**
     * Escape plain text for use inside HTML content (rule #1)
     */
    PLAIN_HTML_CONTENT("plainToHtmlContent");

    /**
     * The name of the protection context
     */
    private String name;

    private ProtectionContext(String name) {
        this.name = name;
    }

    /**
     * Gets the name of the protection context.
     *
     * @return The name of the protection context
     */
    public String getName() {
        return this.name;
    }

    /**
     * Gets a protection context from the specified name.
     *
     * @param name The name to get the protection context from
     * @return The protection context; <code>null</code> if an invalid protection context
     * has been specified
     */
    public static ProtectionContext fromName(String name) {
        ProtectionContext[] values = values();
        for (ProtectionContext contextToCheck : values) {
            if (contextToCheck.getName().equals(name)) {
                return contextToCheck;
            }
        }
        return null;
    }
}
