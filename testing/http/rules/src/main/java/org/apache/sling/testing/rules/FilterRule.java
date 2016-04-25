/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.sling.testing.rules;

import org.apache.sling.testing.rules.annotation.IgnoreIf;
import org.apache.sling.testing.rules.annotation.IgnoreIfProperties;
import org.apache.sling.testing.rules.annotation.IgnoreIfProperty;
import org.apache.sling.testing.rules.util.IgnoreTestsConfig;
import org.apache.sling.testing.rules.util.Match;
import org.junit.AssumptionViolatedException;
import org.junit.experimental.categories.Category;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.junit.rules.TestRule;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FilterRule implements TestRule {
    private static final Logger LOG = LoggerFactory.getLogger(FilterRule.class);
    public static final String CATEGORY_PROPERTY = "filterCategory";
    public static final String INCLUDE_CATEGORY_PROPERTY = "runOnlyFilteredCategories";
    private List<String> defaultCategories = new ArrayList<String>();

    @Override
    public Statement apply(Statement base, Description description) {

        // get the annotations in order and skip the tests accordingly

        // IgnoreIf
        IgnoreIf ignoreIf = description.getAnnotation(IgnoreIf.class);
        if (null != ignoreIf) {
            for (Class condClass : Arrays.asList(ignoreIf.value())) {
                try {
                    Constructor<? extends Condition> constructor = condClass.getConstructor();
                    Condition cond = constructor.newInstance();
                    if (cond.satisfy()) {
                        return emptyStatement("IgnoreIf condition met to skip: " + cond.description());
                    }
                } catch (Exception e) {
                    LOG.error("Error getting condition object", e);
                }
            }
        }

        // IgnoreIfProperties
        List<IgnoreIfProperty> ignoreIfPropertyList = new ArrayList<IgnoreIfProperty>();
        IgnoreIfProperties ignoreIfProperties = description.getAnnotation(IgnoreIfProperties.class);
        if (null != ignoreIfProperties) {
            ignoreIfPropertyList.addAll(Arrays.asList(ignoreIfProperties.value()));
        }

        // IgnoreIfProperty
        IgnoreIfProperty ignoreIfProperty = description.getAnnotation(IgnoreIfProperty.class);
        if (null != ignoreIfProperty) {
            ignoreIfPropertyList.add(ignoreIfProperty);
        }
        // Process the ignoreIfProperties
        for (IgnoreIfProperty ignoreIfProp : ignoreIfPropertyList) {
            if (null != System.getProperty(ignoreIfProp.name())
                    && System.getProperty(ignoreIfProp.name()).equals(ignoreIfProp.value())) {
                return emptyStatement("IgnoreIfProperty condition met to skip: system property '" + ignoreIfProp.name()
                        + "' is equal to '" + ignoreIfProp.value() + "'.");
            }
        }

        // Filter using IgnoreTestsConfig
        String fqdn = description.getClassName();
        String methodName = description.getMethodName();
        if (null != methodName) {
            fqdn += "#" + methodName;
        }

        Match match = IgnoreTestsConfig.get().match(fqdn);
        if (match.isIgnored()) {
            return emptyStatement(match.getReason());
        }

       // filterCategory processing
        List<String> filteredCategories;
        if (System.getProperty(CATEGORY_PROPERTY) != null) {
            filteredCategories = Arrays.asList(System.getProperty(CATEGORY_PROPERTY).split(","));
        } else {
            filteredCategories = defaultCategories;
        }

        Category testCategory = description.getAnnotation(Category.class);
        Class[] testCategories = new Class[0];
        if (testCategory != null) {
            testCategories = testCategory.value();
        }
        /*
         * Category annotation exists and the -DfilterCategory property is also set. If test category exists
         * in -DfilterCategory list & -DrunOnlyFilteredCategories is NOT set, then the test is skipped If test
         * category exists in -DfilterCategory & -DrunOnlyFilteredCategories is set, then the test is included
         */
        if ((System.getProperty(INCLUDE_CATEGORY_PROPERTY) == null)) {
            // Skip Tests from CATEGORY_PROPERTY
            for (Class<?> category : testCategories) {
                if (filteredCategories.contains(category.getSimpleName())) {
                    return emptyStatement("Excluding category: " + category.getSimpleName());
                }
            }
        } else {
            // Run only Tests from CATEGORY_PROPERTY
            boolean categorySelected = false;
            for (Class<?> category : testCategories) {
                if ((filteredCategories.contains(category.getSimpleName()))) {
                    categorySelected = true;
                }
            }

            if (!categorySelected) {
                // No @Category from Test is in CATEGORY_PROPERTY (which should be executed), so skip
                return emptyStatement("Test has no category in (" + INCLUDE_CATEGORY_PROPERTY + "=true): '" + filteredCategories + "'");
            }
        }

        // No Filter excluded this test, so execute
        return base;
    }

    private Statement emptyStatement(final String reason) {
        return new Statement() {

            @Override
            public void evaluate() throws Throwable {
                throw new AssumptionViolatedException("Test was ignored by FilterRule: " + reason);
            }
        };
    }

    public FilterRule addDefaultIgnoreCategories(Class... ignoredCategories) {
        for (Class c : ignoredCategories) {
            this.defaultCategories.add(c.getSimpleName());
        }
        return this;
    }
}
