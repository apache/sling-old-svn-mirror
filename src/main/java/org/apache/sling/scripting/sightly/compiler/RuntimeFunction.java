/*******************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package org.apache.sling.scripting.sightly.compiler;

import java.util.Locale;

/**
 * <p>
 *     This class documents what runtime functions (abstracted by
 *     {@link org.apache.sling.scripting.sightly.compiler.expression.nodes.RuntimeCall} expression nodes) will need to be available in a
 *     Sightly runtime.
 * </p>
 * <p>
 *     A Sightly runtime can only be defined through a {@link org.apache.sling.scripting.sightly.compiler.backend.BackendCompiler}
 *     that can transpile {@link org.apache.sling.scripting.sightly.compiler.expression.nodes.RuntimeCall}s to specific runtime function
 *     implementations.
 * </p>
 */
public final class RuntimeFunction {

    /**
     * <p>
     *     The name of the {@link org.apache.sling.scripting.sightly.compiler.expression.nodes.RuntimeCall} function that will process string
     *     formatting. The function will receive the following parameters:
     *
     *     <ol>
     *         <li>the format String (e.g. 'Hello {0}, welcome to {1}')</li>
     *         <li>an array of objects that will replace the format placeholders</li>
     *     </ol>
     * </p>
     * <p>
     *     For more details check https://github.com/Adobe-Marketing-Cloud/sightly-spec/blob/1.2/SPECIFICATION.md#122-format.
     * </p>
     */
    public static final String FORMAT = "format";

    /**
     * <p>
     *     The name of the {@link org.apache.sling.scripting.sightly.compiler.expression.nodes.RuntimeCall} function that will process
     *     i18n. The function will receive the following parameters:
     *
     *     <ol>
     *         <li>the String to translate</li>
     *         <li>optional: locale information</li>
     *         <li>optional: hint information</li>
     *         <li>optional (not part of the specification): basename information; for more details see
     *         {@link java.util.ResourceBundle#getBundle(String, Locale)}</li>
     *     </ol>
     * </p>
     * <p>
     *     For more details check https://github.com/Adobe-Marketing-Cloud/sightly-spec/blob/1.2/SPECIFICATION.md#123-i18n.
     * </p>
     */
    public static final String I18N = "i18n";

    /**
     * <p>
     *     The name of the {@link org.apache.sling.scripting.sightly.compiler.expression.nodes.RuntimeCall} function that will process
     *     join operations on arrays. The function will receive the following parameters:
     *
     *     <ol>
     *         <li>the array of objects to join (e.g. [1, 2, 3])</li>
     *         <li>the join string (e.g. ';')</li>
     *     </ol>
     * </p>
     * <p>
     *     For more details check https://github.com/Adobe-Marketing-Cloud/sightly-spec/blob/1.2/SPECIFICATION.md#124-array-join.
     * </p>
     */
    public static final String JOIN = "join";

    /**
     * <p>
     *     The name of the {@link org.apache.sling.scripting.sightly.compiler.expression.nodes.RuntimeCall} function that will provide
     *     URI manipulation support. The function will receive the following parameters:
     *
     *     <ol>
     *         <li>optional: a URI string to process</li>
     *         <li>optional: a Map containing URI manipulation options</li>
     *     </ol>
     * </p>
     * <p>
     *     For more details check https://github.com/Adobe-Marketing-Cloud/sightly-spec/blob/1.2/SPECIFICATION.md#125-uri-manipulation.
     * </p>
     */
    public static final String URI_MANIPULATION = "uriManipulation";

    /**
     * <p>
     *     The name of the {@link org.apache.sling.scripting.sightly.compiler.expression.nodes.RuntimeCall} function that will provide
     *     XSS escaping and filtering support. The function will receive the following parameters:
     *
     *     <ol>
     *         <li>the original string to escape / filter</li>
     *         <li>the context to be applied - see {@link org.apache.sling.scripting.sightly.compiler.expression.MarkupContext}</li>
     *     </ol>
     * </p>
     * <p>
     *     For more details check https://github.com/Adobe-Marketing-Cloud/sightly-spec/blob/1.2/SPECIFICATION.md#121-display-context.
     * </p>
     */
    public static final String XSS = "xss";

    /**
     * <p>
     *     The name of the {@link org.apache.sling.scripting.sightly.compiler.expression.nodes.RuntimeCall} function that will perform
     *     script execution delegation. The function will receive the following parameters:
     *
     *     <ol>
     *         <li>optional: the relative or absolute path of the script to execute</li>
     *         <li>optional: a Map of options to perform script include processing</li>
     *     </ol>
     * </p>
     * <p>
     *     For more details about the supported options check
     *     https://github.com/Adobe-Marketing-Cloud/sightly-spec/blob/1.2/SPECIFICATION.md#228-include.
     * </p>
     */
    public static final String INCLUDE = "include";

    /**
     * <p>
     *     The name of the {@link org.apache.sling.scripting.sightly.compiler.expression.nodes.RuntimeCall} function that will perform
     *     resource inclusion in the rendering process. The function will receive the following parameters:
     *
     *     <ol>
     *         <li>optional: a relative or absolute path of the resource to be included</li>
     *         <li>optional: a Map containing the resource processing options</li>
     *     </ol>
     * </p>
     * <p>
     *     For more details about the supported options check
     *     https://github.com/Adobe-Marketing-Cloud/sightly-spec/blob/1.2/SPECIFICATION.md#229-resource.
     * </p>
     */
    public static final String RESOURCE = "includeResource";

    /**
     * <p>
     *     The name of the {@link org.apache.sling.scripting.sightly.compiler.expression.nodes.RuntimeCall} function that will provide
     *     the support for loading Use-API objects. The function will receive the following parameters:
     *
     *     <ol>
     *         <li>an identifier that allows to discover the Use-API object that needs to be loaded</li>
     *         <li>optional: a Map of the arguments that are passed to the Use-API object for initialisation or to provide context</li>
     *     </ol>
     * </p>
     * <p>
     *     For more details check https://github.com/Adobe-Marketing-Cloud/sightly-spec/blob/1.2/SPECIFICATION.md#221-use.
     * </p>
     */
    public static final String USE = "use";

}
