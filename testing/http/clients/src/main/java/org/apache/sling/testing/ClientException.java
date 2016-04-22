/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.sling.testing;

/**
 *
 */
public class ClientException extends Exception {

    private static final long serialVersionUID = 1L;
    private int httpStatusCode = -1;

    public ClientException(String message) {
        this(message, null);
    }

    public ClientException(String message, Throwable throwable) {
        this(message, -1, throwable);
    }

    public ClientException(String message, int htmlStatusCode, Throwable throwable) {
        super(message, throwable);
        this.httpStatusCode = htmlStatusCode;
    }

    /**
     * @return the htmlStatusCode
     */
    public int getHttpStatusCode() {
        return httpStatusCode;
    }

    /*
      * (non-Javadoc)
      *
      * @see java.lang.Throwable#getMessage()
      */
    @Override
    public String getMessage() {
        String message = super.getMessage();
        if (httpStatusCode > -1) {
            message = message + "(return code=" + httpStatusCode + ")";
        }
        return message;
    }

}
