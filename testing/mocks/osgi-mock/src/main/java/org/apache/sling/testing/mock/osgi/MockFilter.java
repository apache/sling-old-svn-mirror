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
package org.apache.sling.testing.mock.osgi;

import java.util.Dictionary;
import java.util.Map;

import org.osgi.framework.Filter;
import org.osgi.framework.ServiceReference;

/**
 * Mock {@link Filter} implementation.
 */
class MockFilter implements Filter {

    @Override
    public boolean match(final ServiceReference reference) {
        return false;
    }

    @Override
    public boolean match(final Dictionary dictionary) {
        return false;
    }

    @Override
    public boolean matchCase(final Dictionary dictionary) {
        return false;
    }

    // this is part of org.osgi.core 6.0.0
    public boolean matches(Map<String, ?> map) {
        return false;
    }

}
