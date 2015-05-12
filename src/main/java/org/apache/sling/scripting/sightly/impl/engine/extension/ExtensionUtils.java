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
package org.apache.sling.scripting.sightly.impl.engine.extension;

import org.apache.sling.scripting.sightly.SightlyException;
import org.apache.sling.scripting.sightly.extension.RuntimeExtension;

/**
 * Helper class for {@link RuntimeExtension} implementations.
 */
public class ExtensionUtils {

    /**
     * Helper method for checking if the number of arguments passed to a {@link RuntimeExtension} are equal to what the extension requires.
     *
     * @param extensionName the name of the extension
     * @param arguments     the arguments array
     * @param count         the expected number or arguments
     * @throws SightlyException if the number of supplied arguments differs from what's expected
     */
    public static void checkArgumentCount(String extensionName, Object[] arguments, int count) {
        if (arguments.length != count) {
            throw new SightlyException(String.format("Extension %s requires %d arguments", extensionName, count));
        }
    }

}
