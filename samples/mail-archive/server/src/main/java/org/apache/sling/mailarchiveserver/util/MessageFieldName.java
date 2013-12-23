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
package org.apache.sling.mailarchiveserver.util;


public class MessageFieldName {
	public static final String LIST_ID = "List-Id"; 
	public static final String IN_REPLY_TO = "In-Reply-To"; 
	public static final String LINK_ID = "linkId";
    public static final String NAME = "jcr:text";
    public static final String CONTENT = "jcr:data";
    public static final String PLAIN_BODY = "Body";
    public static final String HTML_BODY = "htmlBody";
    public static final String LAST_UPDATE = "lastUpdate";
    public static final String X_IMPORT_LOG = "X-mailarchive-import";
    public static final String X_ADDC_PATH = "X-addc-path";
    public static final String X_ORIGINAL_HEADER = "X-original-header";
    public static final String ENCODING = "jcr:encoding";
    
//    public static final String X_ORIGINAL_MESSAGE = "X-original-message";
//    public static final String FILENAME = "filename";
}
