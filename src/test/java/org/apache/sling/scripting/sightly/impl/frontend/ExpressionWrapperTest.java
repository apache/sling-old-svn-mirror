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
package org.apache.sling.scripting.sightly.impl.frontend;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.sling.scripting.sightly.compiler.RuntimeFunction;
import org.apache.sling.scripting.sightly.compiler.expression.Expression;
import org.apache.sling.scripting.sightly.compiler.expression.ExpressionNode;
import org.apache.sling.scripting.sightly.compiler.expression.MarkupContext;
import org.apache.sling.scripting.sightly.compiler.expression.nodes.ArrayLiteral;
import org.apache.sling.scripting.sightly.compiler.expression.nodes.MapLiteral;
import org.apache.sling.scripting.sightly.compiler.expression.nodes.NullLiteral;
import org.apache.sling.scripting.sightly.compiler.expression.nodes.NumericConstant;
import org.apache.sling.scripting.sightly.compiler.expression.nodes.RuntimeCall;
import org.apache.sling.scripting.sightly.compiler.expression.nodes.StringConstant;
import org.apache.sling.scripting.sightly.impl.compiler.frontend.ExpressionWrapper;
import org.apache.sling.scripting.sightly.impl.compiler.frontend.Interpolation;
import org.apache.sling.scripting.sightly.impl.filter.ExpressionContext;
import org.apache.sling.scripting.sightly.impl.filter.Filter;
import org.apache.sling.scripting.sightly.impl.filter.FormatFilter;
import org.apache.sling.scripting.sightly.impl.filter.I18nFilter;
import org.apache.sling.scripting.sightly.impl.filter.JoinFilter;
import org.apache.sling.scripting.sightly.impl.filter.URIManipulationFilter;
import org.apache.sling.scripting.sightly.impl.filter.XSSFilter;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ExpressionWrapperTest {

    private static List<Filter> filters;

    @BeforeClass
    public static void setUp() {
        filters = new ArrayList<>(5);
        filters.add(I18nFilter.getInstance());
        filters.add(FormatFilter.getInstance());
        filters.add(JoinFilter.getInstance());
        filters.add(URIManipulationFilter.getInstance());
        filters.add(XSSFilter.getInstance());
        Collections.sort(filters);
    }

    @Test
    public void testI18nOptionsRemoval() {
        Interpolation interpolation = new Interpolation();
        Map<String, ExpressionNode> options = new HashMap<>();
        options.put(I18nFilter.HINT_OPTION, new StringConstant("hint"));
        options.put(I18nFilter.LOCALE_OPTION, new StringConstant("de"));
        options.put(I18nFilter.I18N_OPTION, NullLiteral.INSTANCE);
        interpolation.addExpression(new Expression(new StringConstant("hello"), options));
        ExpressionWrapper wrapper = new ExpressionWrapper(filters);
        Expression result = wrapper.transform(interpolation, MarkupContext.TEXT, ExpressionContext.TEXT);
        List<ExpressionNode> xssArguments = runEmptyOptionsAndXSSAssertions(result);
        RuntimeCall i18n = (RuntimeCall) xssArguments.get(0);
        assertEquals("Expected to I18n runtime function call.", RuntimeFunction.I18N, i18n.getFunctionName());
    }

    @Test
    public void testFormatOptionsRemoval() {
        Interpolation interpolation = new Interpolation();
        Map<String, ExpressionNode> options = new HashMap<>();
        List<ExpressionNode> formatArray = new ArrayList<>();
        formatArray.add(new StringConstant("John"));
        formatArray.add(new StringConstant("Doe"));
        options.put(FormatFilter.FORMAT_OPTION, new ArrayLiteral(formatArray));
        interpolation.addExpression(new Expression(new StringConstant("Hello {0} {1}"), options));
        ExpressionWrapper wrapper = new ExpressionWrapper(filters);
        Expression result = wrapper.transform(interpolation, MarkupContext.TEXT, ExpressionContext.TEXT);
        List<ExpressionNode> xssArguments = runEmptyOptionsAndXSSAssertions(result);
        RuntimeCall format = (RuntimeCall) xssArguments.get(0);
        assertEquals(RuntimeFunction.FORMAT, format.getFunctionName());
    }

    @Test
    public void testJoinOptionsRemoval() {
        Interpolation interpolation = new Interpolation();
        Map<String, ExpressionNode> options = new HashMap<>();
        options.put(JoinFilter.JOIN_OPTION, new StringConstant(";"));
        List<ExpressionNode> array = new ArrayList<>();
        array.add(new NumericConstant(0));
        array.add(new NumericConstant(1));
        interpolation.addExpression(new Expression(new ArrayLiteral(array), options));
        ExpressionWrapper wrapper = new ExpressionWrapper(filters);
        Expression result = wrapper.transform(interpolation, MarkupContext.TEXT, ExpressionContext.TEXT);
        List<ExpressionNode> xssArguments = runEmptyOptionsAndXSSAssertions(result);
        RuntimeCall join = (RuntimeCall) xssArguments.get(0);
        assertEquals(RuntimeFunction.JOIN, join.getFunctionName());
    }

    @Test
    public void testURIOptionsRemoval() {
        Interpolation interpolation = new Interpolation();
        Map<String, ExpressionNode> options = new HashMap<>();
        options.put(URIManipulationFilter.SCHEME, new StringConstant("https"));
        options.put(URIManipulationFilter.DOMAIN, new StringConstant("www.example.org"));
        options.put(URIManipulationFilter.PREPEND_PATH, new StringConstant("/before"));
        options.put(URIManipulationFilter.PATH, new StringConstant("/path"));
        options.put(URIManipulationFilter.APPEND_PATH, new StringConstant("/after"));
        List<ExpressionNode> selectors = new ArrayList<>();
        selectors.add(new StringConstant("a"));
        selectors.add(new StringConstant("b"));
        options.put(URIManipulationFilter.SELECTORS, new ArrayLiteral(selectors));
        options.put(URIManipulationFilter.EXTENSION, new StringConstant("html"));
        options.put(URIManipulationFilter.PREPEND_SUFFIX, new StringConstant("/pre"));
        options.put(URIManipulationFilter.APPEND_SUFFIX, new StringConstant("/after"));
        options.put(URIManipulationFilter.FRAGMENT, new StringConstant("rewrite"));
        Map<String, ExpressionNode> query = new HashMap<>();
        query.put("q", new StringConstant("sightly"));
        query.put("array", new ArrayLiteral(new ArrayList<ExpressionNode>() {{
            add(new NumericConstant(1));
            add(new NumericConstant(2));
            add(new NumericConstant(3));
        }}));
        options.put(URIManipulationFilter.QUERY, new MapLiteral(query));
        options.put(URIManipulationFilter.REMOVE_QUERY, new StringConstant("array"));
        interpolation.addExpression(
                new Expression(new StringConstant("http://www.example.com/resource.selector.extension/suffix#fragment?param=value"),
                        options));
        ExpressionWrapper wrapper = new ExpressionWrapper(filters);
        Expression result = wrapper.transform(interpolation, MarkupContext.TEXT, ExpressionContext.TEXT);
        List<ExpressionNode> xssArguments = runEmptyOptionsAndXSSAssertions(result);
        RuntimeCall join = (RuntimeCall) xssArguments.get(0);
        assertEquals(RuntimeFunction.URI_MANIPULATION, join.getFunctionName());
    }

    private List<ExpressionNode> runEmptyOptionsAndXSSAssertions(Expression result) {
        assertTrue("Expected empty options map for expression after processing.", result.getOptions().isEmpty());
        RuntimeCall xss = (RuntimeCall) result.getRoot();
        assertEquals("Expected XSS escaping applied to expression.", RuntimeFunction.XSS, xss.getFunctionName());
        return xss.getArguments();
    }

}
