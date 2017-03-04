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
package org.apache.sling.scripting.freemarker.it.tests;

import java.io.IOException;

import javax.inject.Inject;

import org.apache.sling.resource.presence.ResourcePresence;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.ops4j.pax.exam.util.Filter;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.ops4j.pax.exam.cm.ConfigurationAdminOptions.factoryConfiguration;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class AdaptToModelIT extends FreemarkerTestSupport {

    private Document document;

    @Inject
    @Filter(value = "(path=/apps/freemarker/page/adaptto/html.ftl)")
    private ResourcePresence resourcePresence;

    @Configuration
    public Option[] configuration() {
        return new Option[]{
            baseConfiguration(),
            factoryConfiguration("org.apache.sling.resource.presence.internal.ResourcePresenter")
                .put("path", "/apps/freemarker/page/adaptto/html.ftl")
                .asOption(),
        };
    }

    @Before
    public void setup() throws IOException {
        final String url = String.format("http://localhost:%s/freemarker/adaptto.html", httpPort());
        document = Jsoup.connect(url).get();
    }

    @Test
    public void testTitle() {
        assertThat(document.title(), is("Sling Models adaptTo()"));
    }

    @Test
    public void testPageName() {
        final Element name = document.getElementById("name");
        assertThat(name.text(), is("adaptto"));
    }

}
