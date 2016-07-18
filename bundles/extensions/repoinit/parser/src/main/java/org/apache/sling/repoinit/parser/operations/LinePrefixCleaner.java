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

package org.apache.sling.repoinit.parser.operations;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;

/** Removes an prefix at the beginning of
 *  each line in a String. 
 *  Used for embedding CND files in repoinit
 *  statements and hiding them from the Sling
 *  provisioning model parser which fails on
 *  statements like [sling:someNodetype] which
 *  are similar to provisioning model sections. 
 */
public class LinePrefixCleaner {
    public String removePrefix(String prefix, String textBlock) {
        final StringBuilder result = new StringBuilder();
        try {
            final BufferedReader r = new BufferedReader(new StringReader(textBlock));
            try {
                String line = null;
                while( (line = r.readLine()) != null) {
                    if(result.length() > 0) {
                        result.append("\n");
                    }
                    if(line.startsWith(prefix)) {
                        result.append(line.substring(prefix.length()));
                    } else {
                        result.append(line);
                    }
                }
            } finally {
                r.close();
            }
        } catch(IOException ioe) {
            throw new RuntimeException("Unexpected IOException", ioe);
        }
        return result.toString();
    }
}
