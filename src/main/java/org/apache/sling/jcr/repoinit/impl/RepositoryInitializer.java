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
package org.apache.sling.jcr.repoinit.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URL;
import java.net.URLConnection;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.jcr.Session;

import org.apache.commons.io.IOUtils;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.jcr.api.SlingRepositoryInitializer;
import org.apache.sling.jcr.repoinit.JcrRepoInitOpsProcessor;
import org.apache.sling.provisioning.model.Feature;
import org.apache.sling.provisioning.model.Model;
import org.apache.sling.provisioning.model.Section;
import org.apache.sling.provisioning.model.io.ModelReader;
import org.apache.sling.repoinit.parser.RepoInitParser;
import org.apache.sling.repoinit.parser.operations.Operation;
import org.osgi.framework.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** SlingRepositoryInitializer that executes repoinit statements read
 *  from a configurable URL.
 */
@Component(
        label="Apache Sling Repository Initializer",
        description="Initializes the JCR content repository using repoinit statements",
        metatype=true)
@Service(SlingRepositoryInitializer.class)
@Properties({
    // SlingRepositoryInitializers are executed in ascending
    // order of their service ranking
    @Property(name=Constants.SERVICE_RANKING, intValue=100)
})
public class RepositoryInitializer implements SlingRepositoryInitializer {
    private final Logger log = LoggerFactory.getLogger(getClass());

    public static final String DEFAULT_TEXT_URL = "context:/resources/provisioning/model.txt";
    
    @Property(
            label="Text URL", 
            description="URL of the source text that provides repoinit statements."
                + " That text is processed according to the model section name parameter.", 
            value=DEFAULT_TEXT_URL)
    public static final String PROP_TEXT_URL = "text.url";
    private String textURL;
    
    public static final String DEFAULT_MODEL_SECTION_NAME = "repoinit";
    
    @Property(
            label="Model section name", 
            description=
                "If using the provisioning model format, this specifies the additional section name (without leading colon) used to extract"
                + " repoinit statements from the raw text provided by the configured source text URL.",
            value=DEFAULT_MODEL_SECTION_NAME)
    public static final String PROP_MODEL_SECTION_NAME = "model.section.name";
    private String modelSectionName;
    
    @Property(
            label="Text format", 
            description=
                "The format to use to interpret the text provided by the configured source text URL. "
                + "That text can be either a Sling provisioning model with repoinit statements embedded in additional sections,"
                + " or raw repoinit statements",
            value=DEFAULT_MODEL_SECTION_NAME)
    public static final String PROP_TEXT_FORMAT = "text.format";
    public static enum TextFormat { RAW, MODEL };
    private TextFormat textFormat;
    
    @Reference
    private RepoInitParser parser;
    
    @Reference
    private JcrRepoInitOpsProcessor processor;
    
    @Activate
    public void activate(Map<String, Object> config) {
        textURL = PropertiesUtil.toString(config.get(PROP_TEXT_URL), DEFAULT_TEXT_URL);
        
        final String fmt = PropertiesUtil.toString(config.get(PROP_TEXT_FORMAT), TextFormat.MODEL.toString());
        try {
            textFormat = TextFormat.valueOf(fmt);
        } catch(Exception e) {
            throw new IllegalArgumentException("Invalid text format '" + fmt + "',"
                    + " valid values are " + Arrays.asList(TextFormat.values()));
        }
        
        modelSectionName = PropertiesUtil.toString(config.get(PROP_MODEL_SECTION_NAME), DEFAULT_MODEL_SECTION_NAME);
        
        log.debug("Activated: {}", this.toString());
    }
    
    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName()).append(": ");
        sb.append(PROP_TEXT_URL).append("=").append(textURL).append(", ");
        sb.append(PROP_TEXT_FORMAT).append("=").append(textFormat).append(", ");
        sb.append(PROP_MODEL_SECTION_NAME).append("=").append(modelSectionName);
        return sb.toString();
    }
    
    @Override
    public void processRepository(SlingRepository repo) throws Exception {
        final String repoinit = getRepoInitText();
        
        if(TextFormat.MODEL.equals(textFormat)) {
            if(modelSectionName == null) {
                throw new IllegalStateException("Section name is null, cannot read model");
            }
            if(modelSectionName.trim().length() == 0) {
                throw new IllegalStateException("Empty " + PROP_MODEL_SECTION_NAME + " is not supported anymore, please use " 
                        + PROP_TEXT_FORMAT + " to specify the input text format");
            }
        }
        
        // loginAdministrative is ok here, definitely an admin operation
        final Session s = repo.loginAdministrative(null);
        try {
            final List<Operation> ops = parser.parse(new StringReader(repoinit));
            log.info("Executing {} repoinit operations", ops.size());
            processor.apply(s, ops);
            s.save();
        } finally {
            s.logout();
        }
    }
    
    /** Get the repoinit statements to execute */
    private String getRawRepoInitText() {
        String result = "";
        try {
            final URL url = new URL(textURL);
            final URLConnection c = url.openConnection();
            final InputStream is = c.getInputStream();
            if(is == null) {
                log.warn("Cannot get InputStream for {}", url);
            } else {
                final StringWriter w = new StringWriter();
                IOUtils.copy(is, w, "UTF-8");
                result = w.toString();
            }
        } catch(Exception e) {
            log.warn("Error reading repoinit statements from " + textURL, e);
        }
        return result;
    }
    
    private String getRepoInitText() {
        final String rawText = getRawRepoInitText();
        log.debug("Raw text from {}: \n{}", textURL, rawText);
        log.info("Got {} characters from {}", rawText.length(), textURL);
        if(TextFormat.RAW.equals(textFormat)) {
            log.info("Parsing raw repoinit statements from {}", textURL);
            return rawText;
        } else {
            log.info("Extracting repoinit statements from section '{}' of provisioning model {}", modelSectionName, textURL);
            final StringReader reader = new StringReader(rawText);
            try {
                final Model model = ModelReader.read(reader, textURL);
                final StringBuilder sb = new StringBuilder();
                if(modelSectionName == null) {
                    throw new IllegalStateException("Model section name is null, cannot read model");
                }
                for (final Feature feature : model.getFeatures()) {
                    for (final Section section : feature.getAdditionalSections(modelSectionName)) {
                        sb.append("# ").append(modelSectionName).append(" from ").append(feature.getName()).append("\n");
                        sb.append("# ").append(section.getComment()).append("\n");
                        sb.append(section.getContents()).append("\n");
                    }
                }
                return sb.toString();
            } catch (IOException e) {
                log.warn("Error parsing provisioning model from " + textURL, e);
                return "";
            }
        }
    }

}
