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

import java.util.List;

import org.owasp.validator.html.CleanResults;
import org.owasp.validator.html.PolicyException;
import org.owasp.validator.html.ScanException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class implements an escaping rule to be used for cleaning up existing HTML
 * content. The output will still be HTML.
 * <p/>
 * The cleanup is performed using the AntiSamy library found at
 * <a href="http://www.owasp.org/index.php/AntiSamy">http://www.owasp.org/index.php/AntiSamy</a>
 */
public class HtmlToHtmlContentContext implements XSSFilterRule {

    /**
     * Logger
     */
    private Logger log = LoggerFactory.getLogger(this.getClass());

    /**
     * @see XSSFilterRule#check(PolicyHandler, String)
     */
    public boolean check(final PolicyHandler policyHandler, final String str) {
        try {
            return policyHandler.getAntiSamy().scan(str).getNumberOfErrors() == 0;
        } catch (final ScanException se) {
            throw new RuntimeException("Unable to scan input");
        } catch (final PolicyException pe) {
            return false;
        }
    }

    /**
     * @see XSSFilterRule#filter(PolicyHandler, java.lang.String)
     */
    public String filter(final PolicyHandler policyHandler, final String str) {
        try {
            log.debug("Protecting (HTML -> HTML) :\n{}", str);
            final CleanResults results = policyHandler.getAntiSamy().scan(str);
            final String cleaned = results.getCleanHTML();
            @SuppressWarnings("unchecked")
            final List<String> errors = results.getErrorMessages();
            for (final String error : errors) {
                log.info("AntiSamy warning: {}", error);
            }
            log.debug("Protected (HTML -> HTML):\n{}", cleaned);

            return cleaned;
        } catch (final ScanException se) {
            throw new RuntimeException("Unable to scan input");
        } catch (final PolicyException pe) {
            throw new RuntimeException("Unable to scan input");
        }
    }

    /**
     * @see XSSFilterRule#supportsPolicy()
     */
    public boolean supportsPolicy() {
        return true;
    }
}
