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
package org.apache.sling.ide.serialization.simplexml;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.sling.ide.serialization.SerializationManager;
import org.apache.sling.slingclipse.api.ProtectedNodes;
import org.json.JSONException;
import org.json.JSONML;
import org.json.JSONObject;

public class SimpleXmlSerializationManager implements SerializationManager {

    private static final String CONTENT_XML = ".content.xml";
    private static final String TAG_NAME = "tagName";

    @Override
    public boolean isSerializationFile(String filePath) {
        return filePath.endsWith(CONTENT_XML);
    }

    @Override
    public String getSerializationFilePath(String baseFilePath) {
        return baseFilePath + File.separatorChar + CONTENT_XML;
    }

    @Override
    public Map<String, String> readSerializationData(InputStream source) throws IOException {

        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(source));
            StringBuilder out = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                out.append(line);
            }

            return getModifiedProperties(out.toString());
        } catch (JSONException e) {
            // TODO Proper error handling
            throw new RuntimeException(e);
        }

    }

    private Map<String, String> getModifiedProperties(String fileContent) throws JSONException {

        Map<String, String> properties = new HashMap<String, String>();
        JSONObject json = JSONML.toJSONObject(fileContent);
        json.remove(TAG_NAME);
        for (Iterator<?> keys = json.keys(); keys.hasNext();) {
            String key = (String) keys.next();
            if (!ProtectedNodes.exists(key) && !key.contains("xmlns")) {
                properties.put(key, json.optString(key));
            }
        }
        return properties;
    }

    @Override
    public void writeSerializationData(OutputStream destination, Map<String, String> data) throws IOException {
    }

}
