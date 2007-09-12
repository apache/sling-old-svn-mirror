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
package org.apache.sling.content.jcr;

import org.apache.jackrabbit.ocm.exception.ObjectContentManagerException;

/**
 * The <code>InvalidQueryException</code> is thrown when trying to create a
 * JCR query failed because the query is invalid. It encapsulates the original
 * <code>javax.jcr.query.InvalidQueryException</code> and shows the same message.
 */
public class InvalidQueryException extends ObjectContentManagerException {

    public InvalidQueryException(javax.jcr.query.InvalidQueryException cause) {
        super(cause.getMessage(), cause);
    }

}
