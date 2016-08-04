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
package org.apache.sling.engine.impl.request;

import java.text.DateFormat;
import java.text.MessageFormat;
import java.text.NumberFormat;
import java.util.Date;

/**
 * Fast MessageFormat implementation which is not thread-safe. It is based on the assumptions that
 * <ul>
 *     <li>most formats do not contain format types and styles</li>
 *     <li>do not use escaping</li>
 *     <li>format elements are in order</li>
 * </ul>
 *
 * If one of these assumptions fails, then it falls back to the original {@link MessageFormat}.<br>
 * To increase the benefit of this implementation, every instance should be reused as often as possible.
 */
public class FastMessageFormat {

    // Reusable formats instances. Cannot be static because these classes are not thread safe.
    private NumberFormat numberFormat;
    private DateFormat dateFormat;

    private NumberFormat getNumberFormat() {
        if (numberFormat == null) {
            numberFormat = NumberFormat.getNumberInstance();
        }
        return numberFormat;
    }

    private DateFormat getDateFormat() {
        if (dateFormat == null) {
            dateFormat = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);
        }
        return dateFormat;
    }

    /**
     * Use this method exactly like {@link MessageFormat#format(String, Object...)}.
     *
     * @see MessageFormat#format(String, Object...)
     */
    public String format(String pattern, Object... arguments) {
        if (arguments == null || arguments.length == 0) {
            return pattern;
        } else {
            if (pattern.indexOf('\'') != -1) {
                // Escaping is not supported, fall back
                return MessageFormat.format(pattern, arguments);
            } else {
                StringBuilder message = new StringBuilder();
                int previousEnd = 0;
                for (int i = 0; i < arguments.length; i++) {
                    String placeholder = '{' + String.valueOf(i);
                    int placeholderIndex = pattern.indexOf(placeholder);
                    // -1 or before previous placeholder || format element with type/style
                    if (placeholderIndex < previousEnd
                            || pattern.charAt(placeholderIndex + placeholder.length()) != '}') {
                        // Type, style and random order are not supported, fall back
                        return MessageFormat.format(pattern, arguments);
                    } else {
                        // Format argument if necessary
                        Object argument = arguments[i];
                        if (argument instanceof Number) {
                            argument = getNumberFormat().format(argument);
                        } else if (argument instanceof Date) {
                            argument = getDateFormat().format(argument);
                        }

                        // Append previous part of the string and formatted argument
                        message.append(pattern.substring(previousEnd, placeholderIndex));
                        message.append(argument);
                        previousEnd = placeholderIndex + placeholder.length() + 1;
                    }
                }
                message.append(pattern.substring(previousEnd, pattern.length()));
                return message.toString();
            }
        }
    }

}
