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
package org.apache.sling.testing.clients.util;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.sling.testing.clients.ClientException;
import org.apache.sling.testing.clients.SlingHttpResponse;


import java.net.URI;


public class HttpUtils {

    /**
     * Verify expected status and dump response in case expected status is not returned.
     * Warning! It will try to consume the entity in case of error
     *
     * @param response       The Sling HTTP response
     * @param expectedStatus List of acceptable HTTP Statuses
     * @throws ClientException if status is not expected
     */
    public static void verifyHttpStatus(SlingHttpResponse response, int... expectedStatus) throws ClientException {
        if (!checkStatus(response, expectedStatus)) {
            throwError(response, buildDefaultErrorMessage(response), expectedStatus);
        }
    }

    /**
     * Verify expected status and show error message in case expected status is not returned.
     *
     * @param response       The SlingHttpResponse of an executed request.
     * @param errorMessage   error message; if {@code null}, errorMessage is extracted from response
     * @param expectedStatus List of acceptable HTTP Statuses
     * @throws ClientException if status is not expected
     */
    public static void verifyHttpStatus(HttpResponse response, String errorMessage, int... expectedStatus)
            throws ClientException {
        if (!checkStatus(response, expectedStatus)) {
            throwError(response, errorMessage, expectedStatus);
        }
    }

    private static boolean checkStatus(HttpResponse response, int... expectedStatus)
            throws ClientException {

        // if no HttpResponse was given
        if (response == null) {
            throw new NullPointerException("The response is null!");
        }

        // if no expected statuses are given
        if (expectedStatus == null || expectedStatus.length == 0) {
            throw new IllegalArgumentException("At least one expected HTTP Status must be set!");
        }

        // get the returned HTTP Status
        int givenStatus = getHttpStatus(response);

        // check if it matches with an expected one
        for (int expected : expectedStatus) {
            if (givenStatus == expected) {
                return true;
            }
        }

        return false;
    }

    private static boolean throwError(HttpResponse response, String errorMessage, int... expectedStatus)
            throws ClientException {
        // build error message
        String errorMsg = "Expected HTTP Status: ";
        for (int expected : expectedStatus) {
            errorMsg += expected + " ";
        }

        // get the returned HTTP Status
        int givenStatus = getHttpStatus(response);

        errorMsg += ". Instead " + givenStatus + " was returned!\n";
        if (errorMessage != null) {
            errorMsg += errorMessage;
        }

        // throw the exception
        throw new ClientException(errorMsg);
    }


    /**
     * Build default error message
     *
     * @param resp The response of a sling request
     * @return default error message
     */
    public static String buildDefaultErrorMessage(SlingHttpResponse resp) {

        String content = resp.getContent();

        // if no response content is available
        if (content == null) return "";
        String errorMsg = resp.getSlingMessage();

        errorMsg = (errorMsg == null || errorMsg.length() == 0)
                // any other returned content
                ? " Response Content:\n" + content
                // response message from sling response
                : "Error Message: \n" + errorMsg;

        return errorMsg;
    }

    /**
     * Get HTTP Status of the response.
     *
     * @param response The RequestExecutor of an executed request.
     * @return The HTTP Status of the response
     * @throws ClientException never (kept for uniformity)
     */
    public static int getHttpStatus(HttpResponse response) throws ClientException {
        return response.getStatusLine().getStatusCode();
    }

    /**
     * Get the first 'Location' header and verify it's a valid URI.
     *
     * @param response HttpResponse the http response
     * @return the location path
     * @throws ClientException never (kept for uniformity)
     */
    public static String getLocationHeader(HttpResponse response) throws ClientException {
        if (response == null) throw new ClientException("Response must not be null!");

        String locationPath = null;
        Header locationHeader = response.getFirstHeader("Location");
        if (locationHeader != null) {
            String location = locationHeader.getValue();
            URI locationURI = URI.create(location);
            locationPath = locationURI.getPath();
        }

        if (locationPath == null) {
            throw new ClientException("not able to determine location path");
        }
        return locationPath;
    }

    /**
     * Check if expected status is in range
     *
     * @param response the http response
     * @param range    the http status range
     * @return true if response is in range
     */
    public static boolean isInHttpStatusRange(HttpResponse response, int range) {
        return range == response.getStatusLine().getStatusCode() / 100 * 100;
    }

    public static int[] getExpectedStatus(int defaultStatus, int... expectedStatus) {
        if (expectedStatus == null || expectedStatus.length == 0) {
            expectedStatus = new int[]{defaultStatus};
        }
        return expectedStatus;
    }


}