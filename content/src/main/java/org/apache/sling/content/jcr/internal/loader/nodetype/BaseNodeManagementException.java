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
package org.apache.sling.content.jcr.internal.loader.nodetype;

/**
 * Base exception for all JCR Node Type Management exceptions.
 */
public class BaseNodeManagementException extends Exception {

    /**
     * Root exception.
     */
    private Exception wrappedException;

    /** Creates a new instance of BaseNodeManagementException. */
    public BaseNodeManagementException() {
    }

    /**
     * Creates a new instance of BaseNodeManagementException.
     *
     * @param message Exception message
     */
    public BaseNodeManagementException(String message) {
        super(message);
    }

    /**
     * Creates a new instance of BaseNodeManagementException.
     *
     * @param rootException Root Exception
     */
    public BaseNodeManagementException(Exception rootException) {
        this.setWrappedException(rootException);
    }

    /**
     * Getter for property wrappedException.
     *
     * @return wrappedException
     */
    public Exception getWrappedException() {
        return this.wrappedException;
    }

    /**
     * Setter for property wrappedException.
     *
     * @param object wrappedException
     */
    public void setWrappedException(Exception object) {
        this.wrappedException = object;
    }
}
