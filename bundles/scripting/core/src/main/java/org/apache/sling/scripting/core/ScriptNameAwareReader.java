/*******************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 ******************************************************************************/

package org.apache.sling.scripting.core;


import java.io.FilterReader;
import java.io.Reader;

import org.apache.sling.scripting.api.ScriptNameAware;

/**
 * The {@code ScriptNameAwareReader} is a {@link FilterReader} marked with the {@link ScriptNameAware} interface. This reader allows
 * retrieving the contained script's name.
 */
public final class ScriptNameAwareReader extends FilterReader implements ScriptNameAware {

    private String scriptName;

    /**
     * Creates a {@code ScriptNameAwareReader} based on another {@link Reader}.
     *
     * @param in         the base {@link Reader}
     * @param scriptName the script's name
     */
    public ScriptNameAwareReader(Reader in, String scriptName) {
        super(in);
        this.scriptName = scriptName;
    }

    @Override
    public String getScriptName() {
        return scriptName;
    }


}
