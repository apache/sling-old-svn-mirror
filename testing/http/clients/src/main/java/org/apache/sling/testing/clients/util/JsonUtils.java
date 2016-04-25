/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.sling.testing.clients.util;

import org.apache.sling.testing.clients.ClientException;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.ObjectMapper;

import java.io.IOException;

public class JsonUtils {
    /**
     * Get {@link JsonNode} from a a String containing JSON.
     *
     * @param jsonString A string containing JSON
     * @return A {@link JsonNode} that is the root node of the JSON structure.
     * @throws ClientException if error occurs while reading json string
     */
    public static JsonNode getJsonNodeFromString(String jsonString) throws ClientException {
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readTree(jsonString);
        } catch (JsonProcessingException e) {
            throw new ClientException("Could not read json file.", e);
        } catch (IOException e) {
            throw new ClientException("Could not read json node.", e);
        }
    }
}