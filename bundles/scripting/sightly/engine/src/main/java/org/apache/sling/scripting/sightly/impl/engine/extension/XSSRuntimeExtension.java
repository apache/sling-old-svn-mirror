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
package org.apache.sling.scripting.sightly.impl.engine.extension;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.sling.scripting.sightly.SightlyException;
import org.apache.sling.scripting.sightly.compiler.RuntimeFunction;
import org.apache.sling.scripting.sightly.compiler.expression.MarkupContext;
import org.apache.sling.scripting.sightly.extension.RuntimeExtension;
import org.apache.sling.scripting.sightly.render.RenderContext;
import org.apache.sling.xss.XSSAPI;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Runtime support for XSS filtering
 */
@Component(
        service = RuntimeExtension.class,
        property = {
                RuntimeExtension.NAME + "=" + RuntimeFunction.XSS
        }
)
public class XSSRuntimeExtension implements RuntimeExtension {

    @Reference(policyOption = ReferencePolicyOption.GREEDY)
    private XSSAPI xssApi;

    private static final Set<String> elementNameWhiteList = new HashSet<>();
    private static final Logger LOG = LoggerFactory.getLogger(XSSRuntimeExtension.class);
    private static final Pattern VALID_ATTRIBUTE = Pattern.compile("^[a-zA-Z_:][\\-a-zA-Z0-9_:\\.]*$");
    private static final Pattern ATTRIBUTE_BLACKLIST = Pattern.compile("^(style|(on.*))$", Pattern.CASE_INSENSITIVE);

    @Override
    public Object call(final RenderContext renderContext, Object... arguments) {
        if (arguments.length < 2) {
            throw new SightlyException(
                    String.format("Extension %s requires at least %d arguments", RuntimeFunction.XSS, 2));
        }
        Object original = arguments[0];
        Object option = arguments[1];
        Object hint = null;
        if (arguments.length >= 3) {
            hint = arguments[2];
        }
        MarkupContext markupContext = null;
        if (option != null && option instanceof String) {
            String name = (String) option;
            markupContext = MarkupContext.lookup(name);
        }
        if (markupContext == MarkupContext.UNSAFE) {
            return original;
        }
        if (markupContext == null) {
            LOG.warn("Expression context {} is invalid, expression will be replaced by the empty string", option);
            return "";
        }
        String text = renderContext.getObjectModel().toString(original);
        return applyXSSFilter(text, hint, markupContext);
    }

    private String applyXSSFilter(String text, Object hint, MarkupContext xssContext) {
        if (xssContext.equals(MarkupContext.ATTRIBUTE) && hint instanceof String) {
            String attributeName = (String) hint;
            MarkupContext attrMarkupContext = getAttributeMarkupContext(attributeName);
            return applyXSSFilter(text, attrMarkupContext);
        }
        return applyXSSFilter(text, xssContext);
    }

    private String applyXSSFilter(String text, MarkupContext xssContext) {
        switch (xssContext) {
            case ATTRIBUTE:
                return xssApi.encodeForHTMLAttr(text);
            case COMMENT:
            case TEXT:
                return xssApi.encodeForHTML(text);
            case ATTRIBUTE_NAME:
                return escapeAttributeName(text);
            case NUMBER:
                Long result = xssApi.getValidLong(text, 0);
                if (result != null) {
                    return result.toString();
                }
            case URI:
                return xssApi.getValidHref(text);
            case SCRIPT_TOKEN:
                return xssApi.getValidJSToken(text, "");
            case STYLE_TOKEN:
                return xssApi.getValidStyleToken(text, "");
            case SCRIPT_STRING:
                return xssApi.encodeForJSString(text);
            case STYLE_STRING:
                return xssApi.encodeForCSSString(text);
            case SCRIPT_COMMENT:
            case STYLE_COMMENT:
                return xssApi.getValidMultiLineComment(text, "");
            case ELEMENT_NAME:
                return escapeElementName(text);
            case HTML:
                return xssApi.filterHTML(text);
        }
        return text; //todo: apply the rest of XSS filters
    }

    private String escapeElementName(String original) {
        original = original.trim();
        if (elementNameWhiteList.contains(original.toLowerCase())) {
            return original;
        }
        return "";
    }

    private MarkupContext getAttributeMarkupContext(String attributeName) {
        if ("src".equalsIgnoreCase(attributeName) || "href".equalsIgnoreCase(attributeName)) {
            return MarkupContext.URI;
        }
        return MarkupContext.ATTRIBUTE;
    }

    private String escapeAttributeName(String attributeName) {
        if (attributeName == null) {
            return null;
        }
        attributeName = attributeName.trim();
        if (matchPattern(VALID_ATTRIBUTE, attributeName) && !isSensitiveAttribute(attributeName)) {
            return attributeName;
        }
        return null;
    }

    private boolean matchPattern(Pattern pattern, String str) {
        return pattern.matcher(str).matches();
    }

    static {
        elementNameWhiteList.add("section");
        elementNameWhiteList.add("nav");
        elementNameWhiteList.add("article");
        elementNameWhiteList.add("aside");
        elementNameWhiteList.add("h1");
        elementNameWhiteList.add("h2");
        elementNameWhiteList.add("h3");
        elementNameWhiteList.add("h4");
        elementNameWhiteList.add("h5");
        elementNameWhiteList.add("h6");
        elementNameWhiteList.add("header");
        elementNameWhiteList.add("footer");
        elementNameWhiteList.add("address");
        elementNameWhiteList.add("main");
        elementNameWhiteList.add("p");
        elementNameWhiteList.add("pre");
        elementNameWhiteList.add("blockquote");
        elementNameWhiteList.add("ul");
        elementNameWhiteList.add("ol");
        elementNameWhiteList.add("li");
        elementNameWhiteList.add("dl");
        elementNameWhiteList.add("dt");
        elementNameWhiteList.add("dd");
        elementNameWhiteList.add("figure");
        elementNameWhiteList.add("figcaption");
        elementNameWhiteList.add("div");
        elementNameWhiteList.add("a");
        elementNameWhiteList.add("em");
        elementNameWhiteList.add("strong");
        elementNameWhiteList.add("small");
        elementNameWhiteList.add("s");
        elementNameWhiteList.add("cite");
        elementNameWhiteList.add("q");
        elementNameWhiteList.add("dfn");
        elementNameWhiteList.add("abbbr");
        elementNameWhiteList.add("data");
        elementNameWhiteList.add("time");
        elementNameWhiteList.add("code");
        elementNameWhiteList.add("var");
        elementNameWhiteList.add("samp");
        elementNameWhiteList.add("kbd");
        elementNameWhiteList.add("sub");
        elementNameWhiteList.add("sup");
        elementNameWhiteList.add("i");
        elementNameWhiteList.add("b");
        elementNameWhiteList.add("u");
        elementNameWhiteList.add("mark");
        elementNameWhiteList.add("ruby");
        elementNameWhiteList.add("rt");
        elementNameWhiteList.add("rp");
        elementNameWhiteList.add("bdi");
        elementNameWhiteList.add("bdo");
        elementNameWhiteList.add("span");
        elementNameWhiteList.add("br");
        elementNameWhiteList.add("wbr");
        elementNameWhiteList.add("ins");
        elementNameWhiteList.add("del");
        elementNameWhiteList.add("table");
        elementNameWhiteList.add("caption");
        elementNameWhiteList.add("colgroup");
        elementNameWhiteList.add("col");
        elementNameWhiteList.add("tbody");
        elementNameWhiteList.add("thead");
        elementNameWhiteList.add("tfoot");
        elementNameWhiteList.add("tr");
        elementNameWhiteList.add("td");
        elementNameWhiteList.add("th");
    }

    private boolean isSensitiveAttribute(String name) {
        return ATTRIBUTE_BLACKLIST.matcher(name).matches();
    }
}
