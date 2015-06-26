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
/*
 * Emits for each document the direct parent path - allowing to fetch direct children by path.
 */
function(doc, meta) {
  
  // handle only sling resource documents with a valid path
  if (!(meta.id.indexOf("sling-resource:")==0 && doc.path && doc.data)) {
    return;
  }
  var pathParts = doc.path.split("/");
  if (pathParts.length < 3) {
    return;
  }
  
  // remove last element to get parent path
  pathParts.pop();
  var parentPath = pathParts.join("/");
  emit(parentPath, null);
}
