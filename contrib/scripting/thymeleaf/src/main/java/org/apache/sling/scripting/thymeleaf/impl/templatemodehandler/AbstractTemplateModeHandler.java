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
package org.apache.sling.scripting.thymeleaf.impl.templatemodehandler;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Modified;
import org.apache.sling.scripting.thymeleaf.SlingTemplateModeHandler;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.thymeleaf.PatternSpec;
import org.thymeleaf.templateparser.ITemplateParser;
import org.thymeleaf.templatewriter.ITemplateWriter;

public abstract class AbstractTemplateModeHandler implements SlingTemplateModeHandler {

    private final String templateModeName;

    private final ITemplateParser templateParser;

    private final ITemplateWriter templateWriter;

    private PatternSpec patternSpec;

    // see StandardTemplateModeHandlers#MAX_PARSERS_POOL_SIZE
    private static final int MAX_PARSERS_POOL_SIZE = 24;

    private final Logger logger = LoggerFactory.getLogger(AbstractTemplateModeHandler.class);

    protected AbstractTemplateModeHandler(final String templateModeName, final ITemplateParser templateParser, final ITemplateWriter templateWriter) {
        this.templateModeName = templateModeName;
        this.templateParser = templateParser;
        this.templateWriter = templateWriter;
    }

    @Activate
    protected void activate(final ComponentContext componentContext) {
        logger.debug("activate");
        configure(componentContext);
    }

    @Modified
    protected void modified(final ComponentContext componentContext) {
        logger.debug("modified");
        configure(componentContext);
    }

    @Deactivate
    protected void deactivate(final ComponentContext componentContext) {
        logger.debug("deactivate");
    }

    protected abstract void configure(final ComponentContext componentContext);

    protected synchronized void configurePatternSpec(final String[] strings) {
        final Set<String> set = new HashSet<String>();
        Collections.addAll(set, strings);
        final PatternSpec patternSpec = new PatternSpec(); // isInitialized() is private, so create a new PatternSpec
        patternSpec.setPatterns(set);
        this.patternSpec = patternSpec;
    }

    @Override
    public String getTemplateModeName() {
        return templateModeName;
    }

    @Override
    public ITemplateParser getTemplateParser() {
        return templateParser;
    }

    @Override
    public ITemplateWriter getTemplateWriter() {
        return templateWriter;
    }

    @Override
    public PatternSpec getPatternSpec() {
        return patternSpec;
    }

    // see StandardTemplateModeHandlers
    protected static int poolSize() {
        final int availableProcessors = Runtime.getRuntime().availableProcessors();
        return Math.min((availableProcessors <= 2 ? availableProcessors : availableProcessors - 1), MAX_PARSERS_POOL_SIZE);
    }

}
