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
package org.apache.sling.ide.impl.vlt;

import org.apache.sling.ide.transport.RepositoryException;
import org.apache.sling.ide.transport.Result;

// TODO largely duplicates AbstractResult
public class JcrResult<T> implements Result<T> {

    public static <T> JcrResult<T> success(T payload) {

        return new JcrResult<>(true, payload, null);

    }

    public static <T> JcrResult<T> failure(Throwable t) {

        return new JcrResult<>(false, null, new RepositoryException(t));
    }

    private final boolean success;
    private final RepositoryException exception;
    private final T payload;

    private JcrResult(boolean success, T payload, RepositoryException exception) {
        this.success = success;
        this.exception = exception;
        this.payload = payload;
    }

    @Override
    public T get() throws RepositoryException {

        if (success)
            return payload;

        throw exception;
    }

    public boolean isSuccess() {
        return success;
    }

    @Override
    public String toString() {
        StringBuilder out = new StringBuilder();
        out.append(getClass().getSimpleName()).append("[ success:").append(success);

        if (exception != null)
            out.append(", exception: ").append(exception.getClass().getName()).append(" - ")
                    .append(exception.getMessage());

        out.append(']');

        return out.toString();

    }

}
