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
package org.apache.sling.commons.log.internal.slf4j;

import java.io.File;
import junit.framework.TestCase;

abstract class AbstractSlingLogTest extends TestCase {

    private static int counter;

    /**
     * Returns a base file for testing ensuring the parent path directory
     * hierarchy exists. The file itself is located below the target folder of
     * the current working directory.
     */
    protected File getBaseFile() {
        final File baseFile = new File("target/" + getClass().getSimpleName()
            + "/" + (counter++) + "-" + System.currentTimeMillis() + "/"
            + getClass().getSimpleName());
        baseFile.getParentFile().mkdirs();
        return baseFile;
    }

    /**
     * Returns a base file name for testing ensuring the parent path directory
     * hierarchy exists. The file itself is located below the target folder of
     * the current working directory.
     */
    protected String getBase() {
        return getBaseFile().getAbsolutePath();
    }

}
