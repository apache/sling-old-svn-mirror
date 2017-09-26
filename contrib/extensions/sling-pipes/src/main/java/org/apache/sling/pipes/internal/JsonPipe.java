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
package org.apache.sling.pipes.internal;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.pipes.AbstractInputStreamPipe;
import org.apache.sling.pipes.Plumber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonException;
import javax.json.JsonObject;
import javax.json.JsonStructure;
import javax.json.JsonValue.ValueType;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Pipe outputting binding related to a json stream: either an object
 */
public class JsonPipe extends AbstractInputStreamPipe {
    private static Logger logger = LoggerFactory.getLogger(JsonPipe.class);
    public final static String RESOURCE_TYPE = RT_PREFIX + "json";

    /**
     * property specifying the json path where to fetched the used value
     */
    protected static final String PN_VALUEPATH = "valuePath";

    protected static final String JSONPATH_ROOT = "$";

    protected static final String ARRAY_START = "[";

    protected static final String OBJ_START = ".";

    protected static final Pattern JSONPATH_FIRSTTOKEN = Pattern.compile("^\\" + JSONPATH_ROOT + "([\\" + OBJ_START + "\\" + ARRAY_START + "])([^\\" + OBJ_START + "\\]\\" + ARRAY_START + "]+)\\]?");

    JsonArray array;
    int index = -1;

    public JsonPipe(Plumber plumber, Resource resource) throws Exception {
        super(plumber, resource);
    }

    /**
     * in case there is no successful retrieval of some JSON data, we cut the pipe here
     * @return input resource of the pipe, can be reouputed N times in case output json binding is an array of
     * N element (output binding would be here each time the Nth element of the array)
     */
    public Iterator<Resource> getOutput(InputStream is) {
        Iterator<Resource> output = EMPTY_ITERATOR;
        Iterator<Resource> inputSingletonIterator = Collections.singleton(getInput()).iterator();
        String jsonString = null;
        try {
            jsonString = IOUtils.toString(is, StandardCharsets.UTF_8);
            if (StringUtils.isNotBlank(jsonString)) {
                JsonStructure json;
                try {
                    json = JsonUtil.parse(jsonString);

                } catch (JsonException ex) {
                    json = null;
                }
                if (json == null) {
                    binding = jsonString.trim();
                    output = inputSingletonIterator;
                } else {
                    String valuePath = properties.get(PN_VALUEPATH, String.class);
                    if (StringUtils.isNotBlank(valuePath)){
                        json = getValue(json, valuePath);
                    }
                    if (json.getValueType() != ValueType.ARRAY) {
                        binding = JsonUtil.unbox(json);
                        output = inputSingletonIterator;
                    } else {
                        binding = array = (JsonArray) json;
                        index = 0;
                        output = new Iterator<Resource>() {
                            @Override
                            public boolean hasNext() {
                                return index < array.size();
                            }

                            @Override
                            public Resource next() {
                                try {
                                    binding = JsonUtil.unbox(array.get(index));
                                } catch (Exception e) {
                                    logger.error("Unable to retrieve {}nth item of jsonarray", index, e);
                                }
                                index++;
                                return getInput();
                            }
                        };
                    }
                }
            }
        }catch (Exception e) {
            logger.error("unable to parse JSON {} ", jsonString, e);
        }
        return output;
    }

    /**
     * Returns fetched json value from value path
     * @param json json structure from which to start
     * @param valuePath path to follow
     * @return value fetched after following the path
     */
    protected JsonStructure getValue(JsonStructure json, String valuePath){
        Matcher matcher = JSONPATH_FIRSTTOKEN.matcher(valuePath);
        if (matcher.find()){
            String firstChar = matcher.group(1);
            String content = matcher.group(2);
            logger.trace("first char is {}, content is {}", firstChar, content);
            if (ARRAY_START.equals(firstChar)){
                JsonArray array = (JsonArray)json;
                int index = Integer.parseInt(content);
                json = (JsonStructure)array.get(index);
            } else if (OBJ_START.equals(firstChar)){
                JsonObject object = (JsonObject)json;
                json = (JsonStructure)object.get(content);
            }
            valuePath = StringUtils.removeStart(valuePath, matcher.group(0));
            if (StringUtils.isNotBlank(valuePath)){
                valuePath = JSONPATH_ROOT + valuePath;
                return getValue(json, valuePath);
            }
        }
        return json;
    }

}
