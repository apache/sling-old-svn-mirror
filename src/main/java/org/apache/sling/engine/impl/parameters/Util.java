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
package org.apache.sling.engine.impl.parameters;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;

import org.apache.sling.api.request.RequestParameter;

/**
 * The <code>Util</code> TODO
 */
class Util {

    /** The name of the form encoding parameter */
    public final static String PARAMETER_FORMENCODING = "_charset_";

    // ISO-8859-1 mapps all characters 0..255 to \u0000..\u00ff directly
    public static final String ENCODING_DIRECT = "ISO-8859-1";

    // Default query (and www-form-encoded) parameter encoding as per
    // HTML spec.
    // see http://www.w3.org/TR/html4/appendix/notes.html#h-B.2.1
    public static final String ENCODING_DEFAULT = "UTF-8";

    public static final byte[] NO_CONTENT = new byte[0];

    static String toIdentityEncodedString(byte[] data) {
        if (data == null) {
            return null;
        }

        char[] characters = new char[data.length];
        for (int i = 0; i < characters.length; i++) {
            characters[i] = (char) (data[i] & 0xff);
        }
        return new String(characters);
    }

    static byte[] fromIdentityEncodedString(String string) {
        if (string == null) {
            return NO_CONTENT;
        }

        byte[] data = new byte[string.length()];
        for (int i = 0; i < data.length; i++) {
            data[i] = (byte) (string.charAt(i) & 0xff);
        }
        return data;
    }

    static InputStream getInputStream(String source) {
        byte[] data = fromIdentityEncodedString(source);
        return new ByteArrayInputStream(data);
    }

    static void fixEncoding(ParameterMap parameterMap) {
        // default the encoding to ISO-8859-1 (aka direct, 1:1 encoding)
        String formEncoding = ENCODING_DIRECT;

        // check whether a form encoding parameter overwrites this default
        RequestParameter[] feParm = parameterMap.get(PARAMETER_FORMENCODING);
        if (feParm != null) {
            // get and check form encoding
            byte[] rawEncoding = feParm[0].get();
            formEncoding = toIdentityEncodedString(rawEncoding);

            // check for the existence of the encoding
            try {
                "".getBytes(formEncoding);
            } catch (UnsupportedEncodingException e) {
                // log.warn("HttpMulitpartPost: Character encoding {0} is not "
                // + "supported, using default {1}", formEncodingParam,
                // DEFAULT_ENCODING);
                formEncoding = ENCODING_DIRECT;
            }
        }

        // map for rename parameters due to encoding fixes
        Map<String, String> renameMap = new HashMap<String, String>();

        // convert the map of lists to a map of arrays
        for (Map.Entry<String, RequestParameter[]> paramEntry : parameterMap.entrySet()) {
            RequestParameter[] params = paramEntry.getValue();
            String parName = null;
            for (int i = 0; i < params.length; i++) {
                if (params[i] instanceof AbstractRequestParameter) {
                    AbstractRequestParameter param = (AbstractRequestParameter) params[i];

                    // fix encoding if different
                    if (!formEncoding.equals(param.getEncoding())) {
                        param.setEncoding(formEncoding);

                        // prepare the parameter for renaming
                        try {
                            if (parName == null) {
                                parName = paramEntry.getKey();
                                String name = URLDecoder.decode(parName,
                                    formEncoding);
                                renameMap.put(paramEntry.getKey(), name);
                            }
                        } catch (UnsupportedEncodingException uee) {
                            // unexpected, as the encoding has been checked !
                        }
                    }
                }
            }
        }

        // apply mappings of deinternationalized names
        if (!renameMap.isEmpty()) {
            for (Map.Entry<String, String> entry : renameMap.entrySet()) {
                parameterMap.renameParameter(entry.getKey(), entry.getValue());
            }
        }
    }
}
