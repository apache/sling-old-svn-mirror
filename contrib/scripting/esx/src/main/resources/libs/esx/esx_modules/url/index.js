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
/**
 * URL Module
 * only Sling specific functions are implemented from:
 * org.apache.sling.api.request Interface RequestPathInfo
 *
 * needs to be extended to support full: https://nodejs.org/api/url.html
 * This Function should return an annotated URL element including
 * sling specific informatin like suffix, resourcePath, selectors, selector string
 * @param link (location.href for instance or resoruce path)
 * @returns {URL}
 * not implemented yet.
 * adopt: https://github.com/labertasch/slingui/blob/master/stream/src/main/resources/SLING-INF/libs/slingui/client/src/utils/URL.js
 */

 function getAbsoluteParent(path, level){
     var idx = 0;
     var len = path.length;
     while (level >= 0 && idx < len) {
            idx = path.indexOf('/', idx + 1);
            if (idx < 0) {
                  idx = len;
             }
            level--;
      }
     return level >= 0 ? "" : path.substring(0, idx);
 }


 function getRelativeParent(path, level) {
     var idx = path.length;
     while (level > 0) {
             idx = path.lastIndexOf('/', idx - 1);
            if (idx < 0) {
                     return "";
                 }
             level--;
         }
     return (idx == 0) ? "/" : path.substring(0, idx);
 }

 function getRelativeParent(path, level) {
     var idx = path.length;
     while (level > 0) {
             idx = path.lastIndexOf('/', idx - 1);
            if (idx < 0) {
                     return "";
                 }
             level--;
         }
     return (idx == 0) ? "/" : path.substring(0, idx);
 }

exports.getAbsoluteParent = getAbsoluteParent;
exports.getRelativeParent = getRelativeParent;
