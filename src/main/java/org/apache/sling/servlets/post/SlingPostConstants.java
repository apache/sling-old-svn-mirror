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
     * Name of the predefined modify operation (value is "modify").
     * <p>
     * The modify operation uses the remaining request parameters to indicate
     * nodes and properties to create.
     * <p>
     * The modify operation is actually chosen by the Sling POST Servlet if the
     * request has no {@link #RP_OPERATION} request parameter.
     *
     * @since 2.0.6 (Bundle version 2.0.6)
     */
    public static final String OPERATION_MODIFY = "modify";

    /**
     * Name of the predefined delete operation (value is "delete").
     * <p>
     * The delete operation requires no further request parameters and just
     * deletes the content addressed by the request.
     * <p>
     * If the {@link #RP_APPLY_TO} parameter is set the resources listed in that
     * parameter are deleted instead of the request resource.
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
     * <p>
     * If the {@link #RP_APPLY_TO} parameter is set the resources listed in that
     * parameter are copied instead of the request resource.
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
     * <p>
     * If the {@link #RP_APPLY_TO} parameter is set the resources listed in that
     * parameter are moved instead of the request resource.
     */
    public static final String OPERATION_MOVE = "move";

    /**
     * Name of the predefined null operation (value is "nop").
     * <p>
     * The null operation is a pseudo operation, which has no effects
     * whatsoever except setting the response status. The null operation may
     * be accompanied with the {@link #RP_NOP_STATUS} parameter to indicate
     * the actual response status to set and the {@link #RP_STATUS} parameter
     * to indicate how to send the actual response status.
     */
    public static final String OPERATION_NOP = "nop";

    /**
     * Name of the predefined checkin operation (value is "checkin").
     * <p>
     * The checkin operation requires no further request parameters and just
     * checks in the content addressed by the request.
     * <p>
     * If the {@link #RP_APPLY_TO} parameter is set the resources listed in that
     * parameter are checked in instead of the request resource.
     */
    public static final String OPERATION_CHECKIN = "checkin";

    /**
     * Name of the predefined checkout operation (value is "checkout").
     * <p>
     * The checkout operation requires no further request parameters and just
     * checks out the content addressed by the request.
     * <p>
     * If the {@link #RP_APPLY_TO} parameter is set the resources listed in that
     * parameter are checked out instead of the request resource.
     */
    public static final String OPERATION_CHECKOUT = "checkout";

    /**
     * Name of the predefined import operation (value is "import").
     *
     * <p>
     * The import operation requires either the {@link #RP_CONTENT} and {@link #RP_CONTENT_TYPE}
     * request parameters or the {@link #RP_CONTENT_FILE} request parameter.
     * Finally the {@link #RP_REPLACE} parameter may be set to indicate whether
     * an existing item at the destination should be overwritten or not.
     */
    public static final String OPERATION_IMPORT = "import";

    /**
     * Name of the request parameter used to indicate the resource to apply the
     * operation to (value is ":applyTo").
     * <p>
     * This property is used by certain opertaions - namely
     * {@link #OPERATION_COPY}, {@link #OPERATION_DELETE} and
     * {@link #OPERATION_MOVE} - to apply the operation to multiple resources
     * instead of the request resource.
     */
    public static final String RP_APPLY_TO = RP_PREFIX + "applyTo";

    /**
     * Name of the request parameter used to indicate the destination for the
     * copy and move operations (value is ":dest"). This request parameter is
     * required by the copy and move operations.
     */
    public static final String RP_DEST = RP_PREFIX + "dest";

    /**
     * Name of the request parameter indicating whether the destination for a
     * copy or move operation is to be replaced if existing (value is
     * ":replace"). Copy or move is only possible if the destination exists if
     * the replace parameter is set to the case-insignificant value true.
     *
     * This request parameter is also used to indicate whether the destination node
     * for an import operation is to be replaced if existing. The parameter value is
     * checked to see if it matches the case-insignificant value true.
     */
    public static final String RP_REPLACE = RP_PREFIX + "replace";

    /**
     * Name of the request parameter indicating whether the destination for a
     * property change during an import operation is to be replaced if existing.
     * The parameter value is checked to see if it matches the case-insignificant
     * value true.
     */
    public static final String RP_REPLACE_PROPERTIES = RP_PREFIX + "replaceProperties";

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
     * Optional request parameter: defines if to enable the error handling 
     * also for POST request. 
     * The parameter value is checked to see if it matches the case-insensitive
     * value true. 
     * 
     * @since 2.2.0 (Bundle version 2.3.0)
     */
    public static final String RP_SEND_ERROR = RP_PREFIX + "sendError";

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
     * Optional request parameter to indicate the actual response status to
     * send back as a result of calling the #OPERATION_NOP (value is ":nopstatus").
     * <p>
     * This parameter is expected to be single-valued and by an integer being a
     * valid HTTP status code. If this parameter is missing or the parameter
     * value cannot be converted to a HTTP status code (integer in the range
     * [100..999]), the default status code 200/OK is returned.
     *
     * @see #OPERATION_NOP
     * @see #RP_STATUS
     */
    public static final String RP_NOP_STATUS = RP_PREFIX + "nopstatus";

    /**
     * The default response status sent back by a {@link #OPERATION_NOP} if the
     * {@link #RP_NOP_STATUS} parameter is not provided or the parameter value
     * cannot be converted into a valid response status code (value is 200).
     *
     * @see #RP_NOP_STATUS
     */
    public static final int NOPSTATUS_VALUE_DEFAULT = 200;

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

    /**
     * Suffix indicating that the named item is to be set from an item whose
     * absolute or relative path is given in the parameter's value (value is
     * "@MoveFrom").
     * <p>
     * This suffix is similar to the {@link #VALUE_FROM_SUFFIX} in that the
     * value for the item is not taken from the request parameter itself but
     * from somewhere else. In this case the value is set by moving another
     * repository item (in the same workspace) to the location addressed by the
     * parameter.
     */
    public static final String SUFFIX_MOVE_FROM = "@MoveFrom";

    /**
     * Suffix indicating that the named item is to be set from an item whose
     * absolute or relative path is given in the parameter's value (value is
     * "@CopyFrom").
     * <p>
     * This suffix is similar to the {@link #VALUE_FROM_SUFFIX} in that the
     * value for the item is not taken from the request parameter itself but
     * from somewhere else. In this case the value is set by copying another
     * repository item (in the same workspace) to the location addressed by the
     * parameter.
     */
    public static final String SUFFIX_COPY_FROM = "@CopyFrom";

    /**
     * Suffix indicating that blank value or values for this property will be
     * ignored.
     */
    public static final String SUFFIX_IGNORE_BLANKS = "@IgnoreBlanks";

    /**
     * Suffix indicating that the default value should be used when the property
     * is not defined. By default the default value is only used when the property
     * is defined, but blank (i.e. an empty form field). With this suffix, the
     * default value will also be used if the property isn't provided at all. This is
     * useful for HTML checkboxes.
     */
    public static final String SUFFIX_USE_DEFAULT_WHEN_MISSING = "@UseDefaultWhenMissing";

    /**
     * Suffix indicating that a multi-value property is to be handled as an
     * ordered set and the sent values start with either "+" or "-" to indicate
     * wether a value should be added to or removed from the set.
     * <p>
     * If a property is marked to be patched with this suffix only properties
     * whose value start with {@link #PATCH_ADD +} or {@link #PATCH_REMOVE -}
     * are considered. Other values are ignored.
     *
     * @see #PATCH_ADD
     * @see #PATCH_REMOVE
     */
    public static final String SUFFIX_PATCH = "@Patch";

    /**
     * Indicates a value to be added to the named multi-value property if the
     * property is being #{@link #SUFFIX_PATCH patched}.
     * <p>
     * If the given value
     * already exists amongst the values of the multi-value properties it is
     * not added.
     */
    public static final char PATCH_ADD = '+';

    /**
     * Indicates a value to be removed from the named multi-value property if
     * the property is being #{@link #SUFFIX_PATCH patched}.
     * <p>
     * If the given value exists multiple times amongst the values of the
     * multi-value properties all occurrences are removed.
     */
    public static final char PATCH_REMOVE = '-';

    /**
     * Name of the request parameter containing the content to be imported
     * by the 'import' operation.
     */
    public static final String RP_CONTENT = RP_PREFIX + "content";

    /**
     * Name of the request parameter containing the content type of the content
     * to be imported by the 'import' operation.
     */
    public static final String RP_CONTENT_TYPE = RP_PREFIX + "contentType";

    /**
     * Name of the request parameter containing the file to be imported
     * by the 'import' operation.
     */
    public static final String RP_CONTENT_FILE = RP_PREFIX + "contentFile";

    /**
     * Name of the request parameter indicating whether versionable nodes should
     * be checked in during an {@link SlingPostConstants#OPERATION_IMPORT} operation.
     */
    public static final String RP_CHECKIN = RP_PREFIX + "checkin";

    /**
     * Name of the request parameter indicating whether versionable nodes should
     * be checked in during an {@link SlingPostConstants#OPERATION_IMPORT} operation.
     * 
     * @since 2.1.2
     */
    public static final String RP_AUTO_CHECKOUT = RP_PREFIX + "autoCheckout";

    /**
     * Name of the request attribute (not parameter) indicating that a post operation
     * should not invoke session.save() upon completion.
     * 
     * @since 2.1.2
     */
    public static final String ATTR_SKIP_SESSION_HANDLING = "skip-session-handling";

    /**
     * Name of the request parameter indicating offset of the chunk in request.
     * @since 2.3.4
     */
    public static final String SUFFIX_OFFSET = "@Offset";

    /**
     * Name of the request parameter indicating length of complete file.
     * @since 2.3.4
     */
    public static final String SUFFIX_LENGTH = "@Length";

    /**
     * Name of the request parameter indicating request contains last chunk
     * and as a result upload should be finished. It is useful in scenarios
     * like file streaming where file size is not known in advance.
     * @since 2.3.4
     */
    public static final String SUFFIX_COMPLETED = "@Completed";

    /**
     * Name of the request parameter indicating request operation is applicable
     * to chunks.
     * @since 2.3.4
     */
    public static final String RP_APPLY_TO_CHUNKS =  RP_PREFIX + "applyToChunks";

    /**
     * Constant for the sling:chunks mixin. Used to identify that node
     * contains chunks.
     * @since 2.3.4
     */
    public static final String NT_SLING_CHUNK_MIXIN = "sling:chunks";

    /**
     * Constant for the sling:fileLength property. The property stores file
     * length.
     * @since 2.3.4
     */
    public static final String NT_SLING_FILE_LENGTH = "sling:fileLength";

    /**
     * Constant for the sling:length property. The property stores
     * cumulative length of all uploaded chunks.
     * @since 2.3.4
     */
    public static final String NT_SLING_CHUNKS_LENGTH = "sling:length";

    /**
     * Constant for the sling:chunk node type. The node type is used
     * to store chunk.
     * @since 2.3.4
     */
    public static final String NT_SLING_CHUNK_NODETYPE = "sling:chunk";

    /**
     * Constant for the sling:offset property. The property stores start
     * offset of chunk.
     * @since 2.3.4
     */
    public static final String NT_SLING_CHUNK_OFFSET = "sling:offset";
    
    /**
     * Constant for prefix for sling:chunk node name.
     * @since 2.3.4
     */
    public static final String CHUNK_NODE_NAME = "chunk";

}
