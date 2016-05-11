/*******************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package org.apache.sling.scripting.sightly.impl.frontend;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.sling.scripting.sightly.impl.compiler.expression.Expression;
import org.apache.sling.scripting.sightly.impl.compiler.expression.ExpressionNode;
import org.apache.sling.scripting.sightly.impl.compiler.expression.node.ArrayLiteral;
import org.apache.sling.scripting.sightly.impl.compiler.expression.node.MapLiteral;
import org.apache.sling.scripting.sightly.impl.compiler.expression.node.NullLiteral;
import org.apache.sling.scripting.sightly.impl.compiler.expression.node.NumericConstant;
import org.apache.sling.scripting.sightly.impl.compiler.expression.node.RuntimeCall;
import org.apache.sling.scripting.sightly.impl.compiler.expression.node.StringConstant;
import org.apache.sling.scripting.sightly.impl.compiler.frontend.ExpressionWrapper;
import org.apache.sling.scripting.sightly.impl.compiler.frontend.Interpolation;
import org.apache.sling.scripting.sightly.impl.filter.ExpressionContext;
import org.apache.sling.scripting.sightly.impl.filter.Filter;
import org.apache.sling.scripting.sightly.impl.filter.FormatFilter;
import org.apache.sling.scripting.sightly.impl.filter.I18nFilter;
import org.apache.sling.scripting.sightly.impl.filter.JoinFilter;
import org.apache.sling.scripting.sightly.impl.filter.URIManipulationFilter;
import org.apache.sling.scripting.sightly.impl.filter.XSSFilter;
import org.apache.sling.scripting.sightly.impl.plugin.MarkupContext;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ExpressionWrapperTest {

    private static Collection<Filter> filters;

    @BeforeClass
    public static void setUp() {
        filters = new ArrayList<Filter>(5);
        filters.add(new I18nFilter());
        filters.add(new FormatFilter());
        filters.add(new JoinFilter());
        filters.add(new URIManipulationFilter());
        filters.add(new XSSFilter());
    }

    @Test
    public void testI18nOptionsRemoval() {
        Interpolation interpolation = new Interpolation();
        Map<String, ExpressionNode> options = new HashMap<String, ExpressionNode>();
        options.put(I18nFilter.HINT_OPTION, new StringConstant("hint"));
        options.put(I18nFilter.LOCALE_OPTION, new StringConstant("de"));
        options.put(I18nFilter.I18N_OPTION, NullLiteral.INSTANCE);
        interpolation.addExpression(new Expression(new StringConstant("hello"), options));
        ExpressionWrapper wrapper = new ExpressionWrapper(filters);
        Expression result = wrapper.transform(interpolation, MarkupContext.TEXT, ExpressionContext.TEXT);
        List<ExpressionNode> xssArguments = runEmptyOptionsAndXSSAssertions(result);
        RuntimeCall i18n = (RuntimeCall) xssArguments.get(0);
        assertEquals("Expected to I18n runtime function call.", I18nFilter.FUNCTION, i18n.getFunctionName());
    }

    @Test
    public void testFormatOptionsRemoval() {
        Interpolation interpolation = new Interpolation();
        Map<String, ExpressionNode> options = new HashMap<String, ExpressionNode>();
        List<ExpressionNode> formatArray = new ArrayList<ExpressionNode>();
        formatArray.add(new StringConstant("John"));
        formatArray.add(new StringConstant("Doe"));
        options.put(FormatFilter.FORMAT_OPTION, new ArrayLiteral(formatArray));
        interpolation.addExpression(new Expression(new StringConstant("Hello {0} {1}"), options));
        ExpressionWrapper wrapper = new ExpressionWrapper(filters);
        Expression result = wrapper.transform(interpolation, MarkupContext.TEXT, ExpressionContext.TEXT);
        List<ExpressionNode> xssArguments = runEmptyOptionsAndXSSAssertions(result);
        RuntimeCall format = (RuntimeCall) xssArguments.get(0);
        assertEquals(FormatFilter.FORMAT_FUNCTION, format.getFunctionName());
    }

    @Test
    public void testJoinOptionsRemoval() {
        Interpolation interpolation = new Interpolation();
        Map<String, ExpressionNode> options = new HashMap<String, ExpressionNode>();
        options.put(JoinFilter.JOIN_OPTION, new StringConstant(";"));
        List<ExpressionNode> array = new ArrayList<ExpressionNode>();
        array.add(new NumericConstant(0));
        array.add(new NumericConstant(1));
        interpolation.addExpression(new Expression(new ArrayLiteral(array), options));
        ExpressionWrapper wrapper = new ExpressionWrapper(filters);
        Expression result = wrapper.transform(interpolation, MarkupContext.TEXT, ExpressionContext.TEXT);
        List<ExpressionNode> xssArguments = runEmptyOptionsAndXSSAssertions(result);
        RuntimeCall join = (RuntimeCall) xssArguments.get(0);
        assertEquals(JoinFilter.JOIN_FUNCTION, join.getFunctionName());
    }

    @Test
    public void testURIOptionsRemoval() {
        Interpolation interpolation = new Interpolation();
        Map<String, ExpressionNode> options = new HashMap<String, ExpressionNode>();
        options.put(URIManipulationFilter.SCHEME, new StringConstant("https"));
        options.put(URIManipulationFilter.DOMAIN, new StringConstant("www.example.org"));
        options.put(URIManipulationFilter.PREPEND_PATH, new StringConstant("/before"));
        options.put(URIManipulationFilter.PATH, new StringConstant("/path"));
        options.put(URIManipulationFilter.APPEND_PATH, new StringConstant("/after"));
        List<ExpressionNode> selectors = new ArrayList<ExpressionNode>();
        selectors.add(new StringConstant("a"));
        selectors.add(new StringConstant("b"));
        options.put(URIManipulationFilter.SELECTORS, new ArrayLiteral(selectors));
        options.put(URIManipulationFilter.EXTENSION, new StringConstant("html"));
        options.put(URIManipulationFilter.PREPEND_SUFFIX, new StringConstant("/pre"));
        options.put(URIManipulationFilter.APPEND_SUFFIX, new StringConstant("/after"));
        options.put(URIManipulationFilter.FRAGMENT, new StringConstant("rewrite"));
        Map<String, ExpressionNode> query = new HashMap<String, ExpressionNode>();
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
        assertEquals(URIManipulationFilter.URI_MANIPULATION_FUNCTION, join.getFunctionName());
    }

    private List<ExpressionNode> runEmptyOptionsAndXSSAssertions(Expression result) {
        assertTrue("Expected empty options map for expression after processing.", result.getOptions().isEmpty());
        RuntimeCall xss = (RuntimeCall) result.getRoot();
        assertEquals("Expected XSS escaping applied to expression.", XSSFilter.FUNCTION_NAME, xss.getFunctionName());
        return xss.getArguments();
    }

}
