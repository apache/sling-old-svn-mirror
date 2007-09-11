/*
 * Copyright 2007 The Apache Software Foundation.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at 
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.core;

import org.apache.sling.component.ComponentException;

/**
 * The <code>SlingException</code> is the main exception class to be thrown
 * by Project Sling components.
 * <p>
 * This exception extends the <code>ComponentException</code> and need not be
 * specially wrapped when thrown. Yet it is recommended to handle this exception
 * or an extension of it.
 */
public class SlingException extends ComponentException {

    /** Serialization UID */
    private static final long serialVersionUID = 4038594466117133411L;

    /**
     * Creates an exception with message.
     * 
     * @param message The message of this exception.
     */
    public SlingException(String message) {
        super(message);
    }

    /**
     * Creates an exception with cause.
     * 
     * @param cause The cause of this exception.
     */
    public SlingException(Throwable cause) {
        super(cause);
    }

    /**
     * Creates an exception with message and cause.
     * 
     * @param message The message of this exception.
     * @param cause The cause of this exception.
     */
    public SlingException(String message, Throwable cause) {
        super(message, cause);
    }

}
