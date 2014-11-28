/*******************************************************************************
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
 ******************************************************************************/

package org.apache.sling.scripting.sightly.impl.compiler;

import org.apache.sling.scripting.sightly.SightlyException;

/**
 * The runtime {@code SightlyParsingException} is thrown during the parsing stage for any grammar offending input.
 */
public class SightlyParsingException extends SightlyException {

    private String offendingInput;

    public SightlyParsingException() {
    }

    public SightlyParsingException(String message) {
        super(message);
    }

    public SightlyParsingException(String message, String offendingInput) {
        super(message);
        this.offendingInput = offendingInput;
    }

    public SightlyParsingException(String message, Throwable cause) {
        super(message, cause);
    }

    public SightlyParsingException(String message, String offendingInput, Throwable cause) {
        super(message, cause);
        this.offendingInput = offendingInput;
    }

    public SightlyParsingException(Throwable cause) {
        super(cause);
    }

    public String getOffendingInput() {
        return offendingInput;
    }
}
