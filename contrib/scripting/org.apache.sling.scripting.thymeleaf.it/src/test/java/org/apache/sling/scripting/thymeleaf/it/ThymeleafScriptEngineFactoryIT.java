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
package org.apache.sling.scripting.thymeleaf.it;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class ThymeleafScriptEngineFactoryIT extends ThymeleafTestSupport {

    @Test
    public void testScriptEngineFactory() {
        assertNotNull(scriptEngineFactory);
    }

    @Test
    public void testScriptEngineFactoryEngineName() {
        assertThat("Apache Sling Scripting Thymeleaf", is(scriptEngineFactory.getEngineName()));
    }

    @Test
    public void testScriptEngineFactoryLanguageName() {
        assertThat("Thymeleaf", is(scriptEngineFactory.getLanguageName()));
    }

    @Test
    public void testScriptEngineFactoryLanguageVersion() {
        assertThat(scriptEngineFactory.getLanguageVersion(), startsWith("3.0"));
    }

    @Test
    public void testScriptEngineFactoryNames() {
        assertThat(scriptEngineFactory.getNames(), hasItem("thymeleaf"));
    }

}
