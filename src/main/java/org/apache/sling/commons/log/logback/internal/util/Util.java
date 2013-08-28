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

package org.apache.sling.commons.log.logback.internal.util;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.xml.sax.InputSource;

public class Util {

    public static List<String> toList(Object values) {
        if (values == null) {
            return Collections.emptyList();
        }

        Object[] valueArray;
        if (values.getClass().isArray()) {
            valueArray = (Object[]) values;
        } else if (values instanceof Collection<?>) {
            valueArray = ((Collection<?>) values).toArray();
        } else {
            valueArray = new Object[] {
                values
            };
        }

        List<String> valuesList = new ArrayList<String>(valueArray.length);
        for (Object valueObject : valueArray) {
            if (valueObject != null) {
                String[] splitValues = valueObject.toString().split(",");
                for (String value : splitValues) {
                    value = value.trim();
                    if (value.length() > 0) {
                        valuesList.add(value);
                    }
                }
            }
        }

        return valuesList;
    }

    public static void close(InputSource is) {
        Closeable c = is.getByteStream();
        if (c == null) {
            c = is.getCharacterStream();
        }
        if (c != null) {
            try {
                c.close();
            } catch (IOException e) {
                // Ignore
            }
        }
    }

}
