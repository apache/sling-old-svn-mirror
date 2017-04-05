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

/**
 * Extends the {@link org.apache.sling.commons.mime.MimeTypeService}
 * service used by client to resolve MIME type information based on content as well as the
 * {@link org.apache.sling.commons.contentdetection.FileNameExtractor} service
 * for extracting the file name from a url or path
 *
 * @version 1.0.0
 */
@Version("1.0.0")
@Export(optional = "provide:=true")
package org.apache.sling.commons.contentdetection;

import aQute.bnd.annotation.Export;
import aQute.bnd.annotation.Version;

