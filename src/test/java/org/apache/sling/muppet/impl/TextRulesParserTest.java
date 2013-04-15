/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The SF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.apache.sling.muppet.impl;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.StringReader;
import java.util.List;

import org.apache.sling.muppet.api.Rule;
import org.apache.sling.muppet.api.RuleBuilder;
import org.junit.Test;

public class TextRulesParserTest {
    
    static class TestBuilder implements RuleBuilder {
        @Override
        public Rule buildRule(final String namespace, final String ruleName,final String qualifier, final String expression) {
            if(!"test".equals(namespace)) {
                return null;
            }
            
            return new Rule(null, null, null) {
                @Override
                public String toString() {
                    return "" + namespace + "_" + ruleName + "_" + qualifier + "_" + expression;
                }
            };
        }
    }
    
    @Test
    public void parse() throws IOException {
        final String rules =
            "test:constant:5:5\n"
            + "test:constant:5:42\n"
            + "# a comment here\n"
            + "an invalid line\n"
            + "othernamespace:constant:5:42\n"
            + "\t\n\n\n\n\n"
            + "test:invert:12:-1\n"
            + "\ttest:invert:21:-21\n"
            + "\ttest:nothing:ABC\n"
            + "\ttest  :\twhitespace : QUAL :\t\t DEF\n"
            + "\ttest  :\tWS2 : :\t\t foo\n"
            + "\ttest  :\tWS3 :\t\t foo\n"
        ;
        
        final TextRulesParser p = new TextRulesParser();
        p.addBuilder(new TestBuilder());
        final List<Rule> result = p.parse(new StringReader(rules));
        assertEquals(8, result.size());
        
        final String [] expect = {
            "test_constant_5_5",
            "test_constant_5_42",
            "test_invert_12_-1",
            "test_invert_21_-21",
            "test_nothing_null_ABC",
            "test_whitespace_QUAL_DEF",
            "test_WS2__foo",
            "test_WS3_null_foo"
        };
        
        for(int i=0; i < expect.length; i++) {
            assertEquals(expect[i], result.get(i++).toString());
        }
        
    }
}
