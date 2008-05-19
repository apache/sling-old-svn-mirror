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
package org.apache.sling.servlets.post;

/**
 * The <code>SlingPostConstants</code> interface provides constants for well
 * known parameters of the core SlingPostServlet. Extensions of the servlet
 * through implementations of the {@link SlingPostOperation} interface may
 * extend this constants.
 */
public interface SlingPostConstants {

    /**
     * Prefix for parameter names which control this POST (RP_ stands for
     * "request param") (value is ":"). This prefix must be used on all request
     * parameters which have significance to POST request processing. Such
     * parameters will not be used to denote properties to be written to the
     * repository.
     */
    public static final String RP_PREFIX = ":";

    /**
     * The name of the parameter containing the operation to execute (value is
     * ":operation"). If this parameter is missing or empty, the request is
     * assumed to be a request to create new content or to modify existing
     * content.
     */
    public static final String RP_OPERATION = RP_PREFIX + "operation";

    /**
     * The suffix to the resource path used to indicate to automatically
     * generate the name of the new item to create during a content creation
     * request (value is "/").
     */
    public static final String DEFAULT_CREATE_SUFFIX = "/";

    /**
     * An alternative suffix to the resource path used to indicate to
     * automatically generate the name of the new item to create during a
     * content creation request (value is "/*").
     */
    public static final String STAR_CREATE_SUFFIX = "/*";

    /**
     * Name of the predefined delete operation (value is "delete").
     * <p>
     * The delete operation requires no further request parameters and just
     * deletes the content addressed by the request.
     */
    public static final String OPERATION_DELETE = "delete";

    /**
     * Name of the predefined copy operation (value is "copy").
     * <p>
     * The copy operation requires the {@link #RP_DEST} request parameter
     * denoting the path to copy the content to. In addition the
     * {@link #RP_ORDER} parameter may be defined to specificy to relative node
     * order of the destination node. Finally the {@link #RP_REPLACE} parameter
     * may be set to indicate whether an existing item at the destination should
     * be replaced or not.
     */
    public static final String OPERATION_COPY = "copy";

    /**
     * Name of the predefined move operation (value is "move")
     * <p>
     * The move operation requires the {@link #RP_DEST} request parameter
     * denoting the path to move the content to. In addition the
     * {@link #RP_ORDER} parameter may be defined to specificy to relative node
     * order of the destination node. Finally the {@link #RP_REPLACE} parameter
     * may be set to indicate whether an existing item at the destination should
     * be replaced or not.
     */
    public static final String OPERATION_MOVE = "move";

    /**
     * Name of the request parameter used to indicate the destination for the
     * copy and move operations (value is "dest"). This request parameter is
     * required by the copy and move operations.
     */
    public static final String RP_DEST = RP_PREFIX + "dest";

    /**
     * Name of the request parameter indicating whether the destination for a
     * copy or move operation is to be replaced if existing (value is
     * "replace"). Copy or move is only possible if the destination exists if
     * the replace parameter is set to the case-insignificant value true.
     */
    public static final String RP_REPLACE = RP_PREFIX + "replace";

    /**
     * Optional request parameter indicating the order of newly created nodes in
     * creation, copy and move operation requests (value is ":order").
     * <p>
     * The value of this parameter may be {@link #ORDER_FIRST},
     * {@link #ORDER_BEFORE}, {@link #ORDER_AFTER}, {@link #ORDER_LAST} or a
     * numberic value indicating the absolute position in the child list of the
     * parent node.
     */
    public static final String RP_ORDER = RP_PREFIX + "order";

    /**
     * Possible value of the {@link #RP_ORDER} parameter indicating that the
     * node by moved to the first position amongst its sibblings (value is
     * "first").
     */
    public static final String ORDER_FIRST = "first";

    /**
     * Possible value of the {@link #RP_ORDER} parameter indicating that the
     * node by moved immediately before the sibbling whose name is contained in
     * the {@link #RP_ORDER} parameter (value is "before ").
     */
    public static final String ORDER_BEFORE = "before ";

    /**
     * Possible value of the {@link #RP_ORDER} parameter indicating that the
     * node by moved immediately after the sibbling whose name is contained in
     * the {@link #RP_ORDER} parameter (value is "after ").
     */
    public static final String ORDER_AFTER = "after ";

    /**
     * Possible value of the {@link #RP_ORDER} parameter indicating that the
     * node by moved to the last position amongst its sibblings (value is
     * "last").
     */
    public static final String ORDER_LAST = "last";

    /**
     * Optional request paramter specifying a node name for a newly created node
     * (value is ":name").
     */
    public static final String RP_NODE_NAME = RP_PREFIX + "name";

    /**
     * Optional request paramter specifying a node name hint for a newly created
     * node (value is ":nameHint").
     */
    public static final String RP_NODE_NAME_HINT = RP_PREFIX + "nameHint";

    /**
     * Prefix for properties addressing repository items with an absolute path
     * (value is "/").
     * 
     * @see #ITEM_PREFIX_RELATIVE_CURRENT
     */
    public static final String ITEM_PREFIX_ABSOLUTE = "/";

    /**
     * Prefix for properties addressing repository items with a path relative to
     * the current request item (value is "./").
     * <p>
     * When collecting parameters addressing repository items for modification,
     * the parameters are first scanned to see whether there is a parameter with
     * this relative path prefix. If such a parameter exists, the modification
     * operations only assumes parameters whose name is prefixes with this
     * prefix or the {@link #ITEM_PREFIX_ABSOLUTE} or the
     * {@link #ITEM_PREFIX_RELATIVE_PARENT} to be parameters addressing
     * properties to modify. Otherwise, that is if no parameter starts with this
     * prefix, all parameters not starting with the
     * {@link #RP_PREFIX command prefix} are considered addressing properties to
     * modify.
     */
    public static final String ITEM_PREFIX_RELATIVE_CURRENT = "./";

    /**
     * Prefix for properties addressing repository items with a path relative to
     * the parent of the request item (value is "../").
     * 
     * @see #ITEM_PREFIX_RELATIVE_CURRENT
     */
    public static final String ITEM_PREFIX_RELATIVE_PARENT = "../";

    /**
     * Optional request parameter: redirect to the specified URL after POST
     */
    public static final String RP_REDIRECT_TO = RP_PREFIX + "redirect";

    /**
     * Optional request parameter: define how the response is sent back to the
     * client. Supported values for this property are
     * {@link #STATUS_VALUE_BROWSER} and {@link #STATUS_VALUE_STANDARD}. The
     * default is to assume {@link #STATUS_VALUE_STANDARD} if the parameter is
     * not set or set to any other value.
     */
    public static final String RP_STATUS = RP_PREFIX + "status";

    /**
     * The supported value for the {@link #RP_STATUS} request parameter
     * requesting to report success or failure of request processing using
     * standard HTTP status codes. This value is assumed as the default value
     * for the {@link #RP_STATUS} parameter if the parameter is missing or not
     * any of the two supported values.
     * 
     * @see #RP_STATUS
     * @see #STATUS_VALUE_BROWSER
     */
    public static final String STATUS_VALUE_STANDARD = "standard";

    /**
     * The supported value for the {@link #RP_STATUS} request parameter
     * requesting to not report success or failure of request processing using
     * standard HTTP status codes but instead alwas set the status to 200/OK and
     * only report the real success or failure status in the XHTML response.
     * 
     * @see #RP_STATUS
     * @see #STATUS_VALUE_STANDARD
     */
    public static final String STATUS_VALUE_BROWSER = "browser";

    /**
     * Optional request parameter: if provided, added at the end of the computed
     * (or supplied) redirect URL
     */
    public static final String RP_DISPLAY_EXTENSION = RP_PREFIX
        + "displayExtension";

    /**
     * SLING-130, suffix that maps form field names to different JCR property
     * names
     */
    public static final String VALUE_FROM_SUFFIX = "@ValueFrom";

    /**
     * Suffix indicating a type hint for the property (value is "@TypeHint").
     */
    public static final String TYPE_HINT_SUFFIX = "@TypeHint";

    /**
     * Suffix indicating a default value for a property (value is
     * "@DefaultValue").
     */
    public static final String DEFAULT_VALUE_SUFFIX = "@DefaultValue";

    /**
     * Suffix indicating that the named property is to be removed before
     * applying any new content (value is "@Delete").
     */
    public static final String SUFFIX_DELETE = "@Delete";
}
