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

public class CompilerException extends RuntimeException {

    private CompilerExceptionCause cause;

    public CompilerException(String message) {
        super(message);
    }

    public CompilerException(CompilerExceptionCause cause) {
        super();
        this.cause = cause;
    }

    public CompilerException(CompilerExceptionCause cause, String message) {
        super(message);
    }

    public CompilerException(CompilerExceptionCause cause, Throwable throwable) {
        super(throwable);
        this.cause = cause;
    }

    public CompilerExceptionCause getFailureCause() {
        return cause;
    }

    public enum CompilerExceptionCause {
        MISSING_REPO_POJO,
        COMPILER_ERRORS
    }
}
