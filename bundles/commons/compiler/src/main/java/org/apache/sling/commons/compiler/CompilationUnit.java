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

import java.io.IOException;
import java.io.Reader;

/**
 * This interface describes a compilation unit - usually a java class.
 * @since 2.0
 */
public interface CompilationUnit {

    /**
     * Return an input stream for the contents.
     * The compiler will close this stream in all cases!
     */
    Reader getSource()
    throws IOException;

    /**
     * Returns the name of the top level public type.
     * This name includes the package.
     * @return the name of the top level public type.
     */
    String getMainClassName();

    /**
     * Return the last modified for the compilation unit.
     * @return The last modified information or <code>-1</code> if
     *         the information can't be detected.
     */
    long getLastModified();
}
