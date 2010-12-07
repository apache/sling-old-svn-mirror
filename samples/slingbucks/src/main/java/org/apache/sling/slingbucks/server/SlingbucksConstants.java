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
package org.apache.sling.slingbucks.server;

public class SlingbucksConstants {
    public static final String CONTENT_PATH = "/content/slingbucks"; 
    public static final String PUBLIC_PATH = CONTENT_PATH + "/public"; 
    public static final String PRIVATE_PATH = CONTENT_PATH + "/private"; 
    public static final String ORDERS_PATH = PUBLIC_PATH + "/orders"; 
    public static final String CONFIRMED_ORDERS_PATH = PRIVATE_PATH + "/confirmed";
    
    public static final String CONFIRMED_ORDER_PROPERTY_NAME = "orderConfirmed";
    public static final String LAST_MODIFIED_PROPERTY_NAME = "lastModified";
}
