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

import java.util.ArrayList;
import java.util.Collection;

import aQute.bnd.annotation.ProviderType;

/**
 * Exception which is triggered whenever a Sling Model cannot be instantiated
 * due to some missing elements (i.e. required fields/methods/constructor parameters
 * could not be injected).
 * Contains a number of {@link MissingElementException}s.
 * 
 * @see ModelFactory
 *
 */
@ProviderType
public final class MissingElementsException extends RuntimeException {
    private static final long serialVersionUID = 7870762030809272254L;

    private Collection<MissingElementException> missingElements;

    
    public MissingElementsException(String message) {
        super(message);
        missingElements = new ArrayList<MissingElementException>();
    }

    @Override
    public String getMessage() {
        StringBuilder message = new StringBuilder(super.getMessage());
        for (MissingElementException e : missingElements) {
            message.append('\n');
            message.append(e.getMessage());
            if (e.getCause() != null) {
                message.append(" caused by ");
                message.append(e.getCause().getMessage());
            }
        }
        return message.toString();
    }

    public void addMissingElementExceptions(MissingElementException e) {
        missingElements.add(e);
    }
    
    public boolean isEmpty() {
        return missingElements.isEmpty();
    }
    
    public Collection<MissingElementException> getMissingElements() {
        return missingElements;
    }
}
