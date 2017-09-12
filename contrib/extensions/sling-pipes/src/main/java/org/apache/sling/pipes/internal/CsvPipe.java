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

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.pipes.AbstractInputStreamPipe;
import org.apache.sling.pipes.Plumber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Csv input stream pipe, similar at what
 */
public class CsvPipe extends AbstractInputStreamPipe {
    private static Logger logger = LoggerFactory.getLogger(JsonPipe.class);
    public static final String RESOURCE_TYPE = RT_PREFIX + "csv";


    protected static final String PN_SEPARATOR = "separator";

    protected static final String DEFAULT_SEPARATOR = ",";

    BufferedReader reader;

    String nextLine = null;

    int index = 0;

    public CsvPipe(Plumber plumber, Resource resource) throws Exception {
        super(plumber, resource);
    }

    @Override
    public Iterator<Resource> getOutput(InputStream inputStream) {
        Iterator<Resource> output = EMPTY_ITERATOR;
        String separator = properties.get(PN_SEPARATOR, DEFAULT_SEPARATOR);
        try {
            reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
            String headersLine = reader.readLine();
            final String[] headers = headersLine.split(separator);
            if (headers.length > 0){
                nextLine = reader.readLine();
                output = new Iterator<Resource>() {
                    @Override
                    public boolean hasNext() {
                        return StringUtils.isNotBlank(nextLine);
                    }
                    @Override
                    public Resource next() {
                        try {
                            String[] values = nextLine.split(separator);
                            if (values.length < headers.length){
                                throw new IllegalArgumentException("wrong format line " + index + " should have at least the same number of columns than the headers");
                            }
                            Map<String, String> map = new HashMap<>();
                            for (int i = 0; i < headers.length; i ++){
                                map.put(headers[i], values[i]);
                            }
                            binding = map;
                            nextLine = reader.readLine();
                        } catch (Exception e) {
                            logger.error("Unable to retrieve {}nth line of csv file", index, e);
                            nextLine = null;
                        }
                        return getInput();
                    }
                };
            }
        } catch (IllegalArgumentException iae){
            logger.error("unable to correctly process csv file", iae);
        } catch (IOException e){
            logger.error("unable to process csv file", e);
        }
        return output;
    }
}