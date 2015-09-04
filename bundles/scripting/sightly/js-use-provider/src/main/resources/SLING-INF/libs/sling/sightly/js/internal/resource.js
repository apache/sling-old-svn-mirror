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
use(['helper.js'], function(helper) {
       
    function getParentPath(path) {
        var index = path.lastIndexOf('/');
        if (index == -1) {
            return null;
        }
        return path.substring(0, index);
    }


    function getProperties(nativeResource) {
        var valueMap = nativeResource.adaptTo(Packages.org.apache.sling.api.resource.ValueMap);
        return (valueMap) ? helper.mapToObject(valueMap) : {};
    }

    /**
     * @name Resource
     * @constructor
     * @class The Resource class
     * @param {object} nativeResource The nativeResource resource object
     */
    function Resource(nativeResource, promise) {
        /**
         * The absolute path for this resource
         * @name Resource~path
         * @member
         * @type {string}
         */
        this.path = nativeResource.getPath();

        /**
         * The map of properties for this object
         * @name Resource~properties
         * @member
         * @type {object.<string, object>}
         */
        this.properties = getProperties(nativeResource);

        /** @private */
        this.nativeResource = nativeResource;

        if (!promise) {
            throw new Error('No promise library provided');
        }
        this._promise = promise;
    }

    Resource.prototype = /** @lends Resource.prototype */ {
        constructor: Resource,

        /**
         * Get the parent resource
         * @return {promise.<Resource>} a promise with the parent of this resource, or null if
         * the resource has no parent
         */
        getParent: function() {
            var parentPath = getParentPath(this.path);
            if (!parentPath) {
                return null;
            }
            var resolver = this.nativeResource.getResourceResolver();
            var parent = resolver.resolve(parentPath);
            return this._promise.success(new Resource(parent, this._promise));
        },

        /**
         * Get the children of this resource
         * @return {promise.<array.<Resource>>} a promise with the array of children resource
         */
        getChildren: function() {
            var resolver = this.nativeResource.getResourceResolver();
            var children = [];
            var it = resolver.listChildren(this.nativeResource);
            var promise = this._promise;
            while (it.hasNext()) {
                var childNativeResource = it.next();
                children.push(new Resource(childNativeResource, promise));
            }
            return this._promise.success(children);
        },

        /**
         * Returns the name of this resource. The name of a resource is the last segment of the path.
         * @returns {string} the name of this resource
         */
        getName: function () {
            var index = this.path.lastIndexOf('/');
            if (index == -1) {
                return this.path;
            }
            return this.path.substring(index + 1);
        },

        /**
         * The resource type is meant to point to rendering/processing scripts, editing dialogs, etc.
         * @return {string} the resource type of this resource
         */
        getResourceType: function () {
            return this.nativeResource.resourceType;
        },

        /**
         * Resolve a path to a resource. The path may be relative
         * to this path
         * @param  {string} path the requested path
         * @return {promise.<Resource>} the promise of a resource. If the resource
         * does not exist, the promise will fail
         */
        resolve: function(path) {
            var resolver = this.nativeResource.getResourceResolver();
            var res = resolver.getResource(this.nativeResource, path);
            if (res == null) {
                return this._promise.failure(new Error('No resource found at path: ' + path));
            }
            return this._promise.success(new Resource(res, this._promise));
        }
    };

    Object.defineProperties(Resource.prototype, {
        /**
         * The name of the resource
         * @name Resource~name
         * @member
         * @type {string}
         */
        name: {
            get: function() {
                return this.getName();
            }
        },

        /**
         * The resource type
         * @name Resource~resourceType
         * @member
         * @type {string}
         */
        resourceType: {
            get: function() {
                return this.getResourceType();
            }
        }
    });

    return Resource;

});
