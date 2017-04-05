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
package org.apache.sling.scripting.thymeleaf.it.tests;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.greaterThan;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class TemplateEngineIT extends ThymeleafTestSupport {

    @Test
    public void testTemplateEngine() {
        assertNotNull(templateEngine);
    }

    @Test
    public void testTemplateResolvers() {
        final int size = templateEngine.getConfiguration().getTemplateResolvers().size();
        assertThat(size, is(greaterThan(0)));
    }

    @Test
    public void testMessageResolvers() {
        final int size = templateEngine.getConfiguration().getMessageResolvers().size();
        assertThat(size, is(greaterThan(0)));
    }

    @Test
    public void testDialects() {
        final int size = templateEngine.getConfiguration().getDialects().size();
        assertThat(size, is(greaterThan(0)));
    }

}
