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

import javax.inject.Inject;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.SyntheticResource;
import org.apache.sling.scripting.thymeleaf.TemplateModeProvider;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.thymeleaf.templatemode.TemplateMode;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class PatternTemplateModeProviderIT extends ThymeleafTestSupport {

    @Inject
    protected TemplateModeProvider templateModeProvider;

    private static Resource mockResource(final String path) {
        return new SyntheticResource(null, path, null);
    }

    @Test
    public void provideTemplateMode_HTML() throws Exception {
        final Resource resource = mockResource("/apps/thymeleaf/page/foo.html");
        final TemplateMode templateMode = templateModeProvider.provideTemplateMode(resource);
        assertThat(templateMode, is(TemplateMode.HTML));
    }

    @Test
    public void provideTemplateMode_XML() throws Exception {
        final Resource resource = mockResource("/apps/thymeleaf/page/foo.xml");
        final TemplateMode templateMode = templateModeProvider.provideTemplateMode(resource);
        assertThat(templateMode, is(TemplateMode.XML));
    }

    @Test
    public void provideTemplateMode_TEXT() throws Exception {
        final Resource resource = mockResource("/apps/thymeleaf/text/foo.txt");
        final TemplateMode templateMode = templateModeProvider.provideTemplateMode(resource);
        assertThat(templateMode, is(TemplateMode.TEXT));
    }

    @Test
    public void provideTemplateMode_JAVASCRIPT() throws Exception {
        final Resource resource = mockResource("/apps/thymeleaf/assets/foo.js");
        final TemplateMode templateMode = templateModeProvider.provideTemplateMode(resource);
        assertThat(templateMode, is(TemplateMode.JAVASCRIPT));
    }

    @Test
    public void provideTemplateMode_CSS() throws Exception {
        final Resource resource = mockResource("/apps/thymeleaf/assets/foo.css");
        final TemplateMode templateMode = templateModeProvider.provideTemplateMode(resource);
        assertThat(templateMode, is(TemplateMode.CSS));
    }

    @Test
    public void provideTemplateMode_fall_through() throws Exception {
        final Resource resource = mockResource("foohtml");
        final TemplateMode templateMode = templateModeProvider.provideTemplateMode(resource);
        assertThat(templateMode, is(TemplateMode.RAW));
    }

}
