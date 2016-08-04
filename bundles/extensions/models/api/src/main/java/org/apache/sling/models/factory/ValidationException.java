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
package org.apache.sling.models.factory;

import aQute.bnd.annotation.ProviderType;

/**
 * Thrown in case an validation could not be performed for the given model.
 * (although it would be required through {@link org.apache.sling.models.annotations.ValidationStrategy#REQUIRED}).
 * Depends on the actual implementation under which exact circumstances this is thrown.
 * @see ModelFactory
 */
@ProviderType
public class ValidationException extends RuntimeException {
    private static final long serialVersionUID = 1115037385798809055L;

    public ValidationException(String message) {
        super(message);
    }

    public ValidationException(Throwable cause) {
        super(cause);
    }

}
