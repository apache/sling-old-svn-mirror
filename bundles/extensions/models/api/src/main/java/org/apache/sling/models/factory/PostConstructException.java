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
package org.apache.sling.models.factory;

import aQute.bnd.annotation.ProviderType;

/**
 * Exception which is triggered, whenever the post-construct method has thrown an exception.
 * The cause (accessible via {@link #getCause()}) is always the original exception being thrown from the post-construct method.
 */
@ProviderType
public class PostConstructException extends ModelClassException {
    private static final long serialVersionUID = -2527043835215727726L;

    /**
     * @param message some message
     * @param cause the original exception being thrown in the post-construct
     */
    public PostConstructException(String message, Throwable cause) {
        super(message, cause);
    }

}
