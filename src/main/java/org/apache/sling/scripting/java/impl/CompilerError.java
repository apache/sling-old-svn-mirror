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
package org.apache.sling.scripting.java.impl;

/**
 * This class encapsulates an error message produced by a programming language
 * processor (whether interpreted or compiled)
 */
public class CompilerError {

    /**
     * The start line number of the offending program text
     */
    private final int startline;

    /**
     * The start column number of the offending program text
     */
    private final int startcolumn;

    /**
     * The name of the file containing the offending program text
     */
    private final String file;

    /**
     * The actual error text produced by the language processor
     */
    private final String message;

    /**
     * The error message constructor.
     *
     * @param file The name of the file containing the offending program text
     * @param startline The start line number of the offending program text
     * @param startcolumn The start column number of the offending program text
     * @param message The actual error text produced by the language processor
     */
    public CompilerError(final String file,
                         final int startline,
                         final int startcolumn,
                         final String message) {
        this.file = file;
        this.startline = startline;
        this.startcolumn = startcolumn;
        this.message = message;
    }

    /**
     * Return the filename associated with this compiler error.
     *
     * @return The filename associated with this compiler error
     */
    public String getFile() {
        return file;
    }

    /**
     * Return the starting line number of the program text originating this error
     *
     * @return The starting line number of the program text originating this error
     */
    public int getStartLine() {
        return startline;
    }

    /**
     * Return the starting column number of the program text originating this
     * error
     *
     * @return The starting column number of the program text originating this
     * error
     */
    public int getStartColumn() {
        return startcolumn;
    }

    /**
     * Return the message produced by the language processor
     *
     * @return The message produced by the language processor
     */
    public String getMessage() {
        return message;
    }
}
