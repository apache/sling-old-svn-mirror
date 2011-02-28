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
import java.util.LinkedHashMap;
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

    // the default encoding used in #fixEncoding if the _charset_ request
    // parameter is not set
    private static String defaultFixEncoding = ENCODING_DIRECT;

    // Default query (and www-form-encoded) parameter encoding as per
    // HTML spec.
    // see http://www.w3.org/TR/html4/appendix/notes.html#h-B.2.1
    public static final String ENCODING_DEFAULT = "UTF-8";

    public static final byte[] NO_CONTENT = new byte[0];

    static void setDefaultFixEncoding(final String encoding) {
        defaultFixEncoding = validateEncoding(encoding);
    }

    static String getDefaultFixEncoding() {
        return defaultFixEncoding;
    }

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
        // default the encoding to defaultFixEncoding
        String formEncoding = defaultFixEncoding;

        // check whether a form encoding parameter overwrites this default
        RequestParameter[] feParm = parameterMap.get(PARAMETER_FORMENCODING);
        if (feParm != null) {
            // get and check form encoding
            byte[] rawEncoding = feParm[0].get();
            formEncoding = toIdentityEncodedString(rawEncoding);
            formEncoding = validateEncoding(formEncoding);
        }

        // map for rename parameters due to encoding fixes
        LinkedHashMap<String, String> renameMap = new LinkedHashMap<String, String>();

        // convert the map of lists to a map of arrays
        for (Map.Entry<String, RequestParameter[]> paramEntry : parameterMap.entrySet()) {
            RequestParameter[] params = paramEntry.getValue();
            String parName = null;
            for (int i = 0; i < params.length; i++) {
                if (params[i] instanceof AbstractRequestParameter) {
                    AbstractRequestParameter param = (AbstractRequestParameter) params[i];

                    // fix encoding if different
                    if (!formEncoding.equalsIgnoreCase(param.getEncoding())) {
                        param.setEncoding(formEncoding);

                        // prepare the parameter for renaming
                        if (parName == null) {
                            parName = paramEntry.getKey();
                            String name = reencode(parName, formEncoding);
                            if (!parName.equals(name)) {
                                renameMap.put(parName, name);
                            }
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

    private static String reencode(String parName, String encoding) {
        // re-encode the parameter to the encoding
        if (!ENCODING_DIRECT.equalsIgnoreCase(encoding)) {
            try {
                return new String(parName.getBytes(ENCODING_DIRECT), encoding);
            } catch (UnsupportedEncodingException uee) {
                // unexpected, as the encoding is assumed to have been checked !
            }
        }

        // otherwise just return the name unmodified
        return parName;
    }

    /**
     * Checks whether the given encoding is known and supported by the platform
     * or not. If the platform supports the encoding the parameter is returned.
     * Otherwise or if the encoding argument is <code>null</code> or an empty
     * string {@link #defaultFixEncoding} is returned.
     *
     * @param encoding The encoding to validate
     * @return The encoding if supported or {@link #defaultFixEncoding}
     */
    private static String validateEncoding(final String encoding) {
        if (encoding != null && encoding.length() > 0) {
            // check for the existence of the encoding
            try {
                "".getBytes(encoding);
                return encoding;
            } catch (UnsupportedEncodingException e) {
                // log.warn("HttpMulitpartPost: Character encoding {0} is not "
                // + "supported, using default {1}", formEncodingParam,
                // DEFAULT_ENCODING);
            }
        }

        // no encoding or unsupported encoding
        return defaultFixEncoding;
    }
}
