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
package org.apache.sling.commons.compiler;

/**
 * The error handler for the compilation.
 */
public interface ErrorHandler {

    /**
     * Notify the handler of an error.
     * @param msg The error message.
     * @param sourceFile The source file the error occured in
     * @param line The source line number
     * @param position The column
     */
    void onError(String msg, String sourceFile, int line, int position);

    /**
     * Notify the handler of a warning.
     * @param msg The warning message.
     * @param sourceFile The source file the warning occured in
     * @param line The source line number
     * @param position The column
     */
    void onWarning(String msg, String sourceFile, int line, int position);
}
