/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.scripting.scala;

import javax.script.ScriptEngineFactory;

import junit.framework.TestCase;

import org.apache.sling.scripting.scala.engine.ScalaScriptEngineFactory;

public class ScalaScriptEngineFactoryTest extends TestCase {

    public void testScriptEngineFactoryInit() {
        ScriptEngineFactory scalaEngineFactory = new ScalaScriptEngineFactory();
        assertNotNull(scalaEngineFactory);
    }

    public void testScriptEngineFactoryEngine() {
        try {
            new ScalaScriptEngineFactory().getScriptEngine();
            assertTrue("Expecting IllegalStateException", false);
        }
        catch (IllegalStateException e) {
            // expected
        }
    }

    public void testScriptEngineFactoryLanguage() {
        String language = new ScalaScriptEngineFactory().getLanguageName();
        assertEquals("scala", language);
    }

    public void testScriptEngineFactoryLanguageVersion() {
        String version = new ScalaScriptEngineFactory().getLanguageVersion();
        assertEquals("2.7.3", version);
    }

}
