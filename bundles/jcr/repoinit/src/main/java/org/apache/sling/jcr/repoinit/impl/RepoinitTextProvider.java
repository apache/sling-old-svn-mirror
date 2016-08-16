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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.apache.sling.provisioning.model.Feature;
import org.apache.sling.provisioning.model.Model;
import org.apache.sling.provisioning.model.Section;
import org.apache.sling.provisioning.model.io.ModelReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Retrieves repoinit statements from URLs that return either
 *  raw repoinit text or Sling provisioning models that are parsed
 *  to extract the repoinit text.
 *  
 *  Uses references like
 *  
 *  <code>model@repoinit:context:/resources/provisioning/model</code>,
 *  
 *  meaning that the supplied context:/ URI returns a provisioning model 
 *  containing repoinit statements in its "repoinit" additional section, or
 *    
 *
 *  <code>raw:classpath://com.example.sling.repoinit/repoinit.txt</code>
 *  
 *  meaning that the supplied classpath: URI returns raw repoinit statements.
 */
public class RepoinitTextProvider {
    public static enum TextFormat { raw, model };
    private static final String DEFAULT_MODEL_SECTION = "repoinit";
    
    public static final Pattern REF_PATTERN = Pattern.compile("([a-z]+)(@([a-zA-Z0-9_-]+))?:(.*)");
    
    private Logger log = LoggerFactory.getLogger(getClass());
    
    static class Reference {
        final TextFormat format;
        final String modelSection;
        final String url;
        
        Reference(String ref) {
            if(ref == null) {
                throw new IllegalArgumentException("Null reference");
            }
            final Matcher m = REF_PATTERN.matcher(ref);
            if(!m.matches()) {
                throw new IllegalArgumentException("Invalid reference '" + ref + "', should match " + REF_PATTERN);
            }
            format = TextFormat.valueOf(m.group(1));
            if(format.equals(TextFormat.raw)) {
                modelSection = null;
            } else if(format.equals(TextFormat.model) && m.group(3) == null) {
                modelSection = DEFAULT_MODEL_SECTION;
            } else {
                modelSection = m.group(3);
            }
            url = m.group(4);
        }
        
        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder();
            sb.append(getClass().getSimpleName()).append(":");
            sb.append("format=").append(format);
            if(modelSection != null) {
                sb.append(", model section=").append(modelSection);
            }
            sb.append(", URL=").append(url);
            return sb.toString();
        }
    }
    
    public String getRepoinitText(String referenceString) throws IOException {
        final Reference ref = new Reference(referenceString);
        log.info("Reading repoinit statements from {}", ref);
        final String rawText = getRawText(ref.url);
        log.debug("Raw text from {}: \n{}", ref.url, rawText);
        if(TextFormat.model.equals(ref.format)) {
            log.debug("Extracting provisioning model section {}", ref.modelSection);
            return extractFromModel(ref.url, rawText, ref.modelSection); 
        } else {
            return rawText;
        }
    }
    
    private String extractFromModel(String sourceInfo, String rawText, String modelSection) throws IOException {
        final StringReader reader = new StringReader(rawText);
        final Model model = ModelReader.read(reader, sourceInfo);
        final StringBuilder sb = new StringBuilder();
        if(modelSection == null) {
            throw new IllegalStateException("Model section name is null, cannot read model");
        }
        for (final Feature feature : model.getFeatures()) {
            for (final Section section : feature.getAdditionalSections(modelSection)) {
                sb.append("# ").append(modelSection).append(" from ").append(feature.getName()).append("\n");
                sb.append("# ").append(section.getComment()).append("\n");
                sb.append(section.getContents()).append("\n");
            }
        }
        return sb.toString();
    }
    
    private String getRawText(String urlString) throws IOException {
        String result = "";
        final URL url = new URL(urlString);
        final URLConnection c = url.openConnection();
        final InputStream is = c.getInputStream();
        if(is == null) {
            log.warn("Cannot get InputStream for {}", url);
        } else {
            final StringWriter w = new StringWriter();
            IOUtils.copy(is, w, "UTF-8");
            result = w.toString();
        }
        return result;
    }
}