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
use(function(_) {

    function convertParams(paramMap) {
        var result = {};
        var it = paramMap.entrySet().iterator();
        while (it.hasNext()) {
            var entry = it.next();
            var paramName = entry.getKey();
            var paramValues = entry.getValue();
            if (paramValues) {
                paramValues = paramValues.map(function (param) {
                    return param.getString();
                });
            }
            result[paramName] = paramValues;
        }
        return result;
    }

    /**
     * @constructor
     * @class Properties that contain parts of the request
     * @name RequestPathInfo
     * @param {object} nativePathInfo The native path info object
     */
    function RequestPathInfo(nativePathInfo) {
        /** @private */
        this.nativePathInfo = nativePathInfo;
    }

    Object.defineProperties(RequestPathInfo.prototype, {

        /**
         * The resource path
         * @name RequestPathInfo~resourcePath
         * @type {String}
         * @member
         */
        resourcePath: {
            get: function() {
                return this.nativePathInfo.getResourcePath();
            }
        },

        /**
         * The extension in the path
         * @name RequestPathInfo~extension
         * @type {String}
         * @member
         */
        extension: {
            get: function() {
                return this.nativePathInfo.getExtension();
            }
        },

        /**
         * The selector string segment
         * @name RequestPathInfo~selectorString
         * @type {String}
         * @member
         */
        selectorString: {
            get: function() {
                return this.nativePathInfo.getSelectorString();
            }
        },

        /**
         * The selectors in the request
         * @name RequestPathInfo~selectors
         * @type {Array.<String>}
         * @member
         */
        selectors: {
            get: function() {
                return this.nativePathInfo.getSelectors();
            }
        },

        /**
         * The suffix in the request path
         * @name RequestPathInfo~suffix
         * @type {String}
         * @member
         */
        suffix: {
            get: function() {
                return this.nativePathInfo.getSuffix();
            }
        }
    });

    /**
     * @constructor
     * @name Request
     * @class The request class
     * @param {object} nativeRequest The nativeResource request object
     */
    function Request(nativeRequest) {
        /** @private */
        this.nativeRequest = nativeRequest;

        /**
         * A map of the parameters in this request
         * @name Request~parameters
         * @type {object.<string, string>}
         * @member
         */
        this.parameters = convertParams(nativeRequest.getRequestParameterMap());

        /**
         * The path info associated with this request
         * @name Request~pathInfo
         * @type {RequestPathInfo}
         * @member
         */
        this.pathInfo = new RequestPathInfo(nativeRequest.getRequestPathInfo());
    }

    return Request;
});
