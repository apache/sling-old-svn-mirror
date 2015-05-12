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
use(['resource.js', 'request.js', 'promise.js'], function(Resource, Request, promiseFactory) {

    return function(bindings, Q) {
        var promiseLib = promiseFactory(Q);
        var slyResource;
        var slyProperties;
        var slyRequest;
        if (bindings.containsKey('resource')) {
            slyResource = new Resource(bindings.get('resource'), promiseLib);
            slyProperties = slyResource.properties;
        }
        if (bindings.containsKey('request')) {
            slyRequest = new Request(bindings.get('request'));
        }

        /**
         * @namespace sly
         */
        return /** @lends sly */ {

            /**
             * The current resource of the request
             * @type {Resource}
             */
            resource: slyResource,

            /**
             * The properties of the current resource
             * @type {Object.<string, Object>}
             */
            properties: slyResource.properties,

            /**
             * The request object
             * @type {Request}
             */
            request: slyRequest
        };
    }

});
