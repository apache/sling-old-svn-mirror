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
package org.apache.sling.jcr.base.util;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.apache.sling.testing.mock.jcr.MockJcr;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

public class AccessControlUtilTest {

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Test
    public void testGetAccessControlManager() throws RepositoryException {
        Session s = MockJcr.newSession();

        /* Here we check that AccessControlUtil calls getAccessControlManager method of MockSession object.
         * getAccessControlManager implementation of MockSession throws UnsupportedOperationException so we except it below.
         * If AccessControlUtil.getAccessControlManager method will call method which even
         * doesn't exist RepositoryException will be thrown
         */
        exception.expect(UnsupportedOperationException.class);
        AccessControlUtil.getAccessControlManager(s);
    }
}
