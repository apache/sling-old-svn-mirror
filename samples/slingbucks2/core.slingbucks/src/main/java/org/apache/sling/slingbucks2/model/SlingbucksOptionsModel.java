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
package org.apache.sling.slingbucks2.model;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Pattern;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;

@Model(adaptables = {Resource.class, SlingHttpServletRequest.class})
public class SlingbucksOptionsModel {

    private static final Logger LOGGER = LoggerFactory.getLogger(SlingbucksOptionsModel.class);
    private static final String OPT_PREFIX_REGEXP = "^opt_";
    private static final String FIELDS_PATH = "/content/slingbucks2Content/readonly/options/fields";

    private static Pattern optPrefixPattern = Pattern.compile(OPT_PREFIX_REGEXP);

    @SlingObject
    private ResourceResolver resourceResolver;

    @SlingObject
    private Resource resource;

    public SlingbucksOptionsModel() {
        LOGGER.trace("Model Instance created");
    }

    /** @return True if this order is confirmed **/
    public boolean isConfirmed() {
        return resource.getValueMap().containsKey("orderConfirmed");
    }

    /** @return Map of the Selected Order Options **/
    public Map<String,String> getSelectedOptions() {
        Map<String, String> answer = new HashMap<>();
        for(String key: resource.getValueMap().keySet()) {
            Matcher matcher = optPrefixPattern.matcher(key);
            if(matcher.find()) {
                String name = matcher.replaceFirst("");
                String value = resource.getValueMap().get(key, "");
                answer.put(name, value);
            }
        }
        LOGGER.trace("Return Selected Options Map: '{}'", answer);
        return answer;
    }

    /** @return The child 'fields' of the given resource if found otherwise null **/
    public Resource getFields() {
        Resource fields = resource.getChild("fields");
        return fields;
    }

    /** @return The calculated Price of the order **/
    public double getPrice() {
        // Enumerate the properties of the current order node,
        // find the price in our fields definition, and
        // accumulate if found
        double answer = 0;

        // Get the fields node, where prices are defined
        Resource fields = resourceResolver.resolve(FIELDS_PATH);

        // First using priceOffset, to compute the base price
        for(String type: resource.getValueMap().keySet()) {
            String option = resource.getValueMap().get(type, "");
            double offset = getPriceProperty(type, option, "priceOffset");
            answer += offset;
        }

        // Then using priceFactor and multiply
        for(String type: resource.getValueMap().keySet()) {
            String option = resource.getValueMap().get(type, "");
            double factor = getPriceProperty(type, option, "priceFactor");
            if(factor > 0) {
                answer *= factor;
            }
        }

        // And round price
        answer = Math.round(answer * 10) / 10;

        return answer;
    }

    private double getPriceProperty(String typeName, String optionName, String propertyName) {
        double answer = 0;
        Matcher matcher = optPrefixPattern.matcher(typeName);
        boolean matchFound = matcher.find();
        if(matchFound) {
            // Path of price property P is like
            // FIELDS_PATH + coffeetype/capuccino/priceOffset
            // Get that resource and get our property under it
            String name = matcher.replaceFirst("");
            String path = FIELDS_PATH + "/" + name + "/" + optionName;
            Resource type = resourceResolver.getResource(path);
            if(type != null) {
                answer = type.getValueMap().get(propertyName, 0.0d);
            }
        }
        return answer;
    }
}