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
package org.apache.sling.models.testmodels.classes;

import javax.inject.Inject;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.Default;
import org.apache.sling.models.annotations.Model;

@Model(adaptables = Resource.class)
public class DefaultWrappersModel {

    @Inject
    @Default(booleanValues = true)
    private Boolean booleanWrapperProperty;

    @Inject
    @Default(booleanValues = { true, true })
    private Boolean[] booleanWrapperArrayProperty;

    @Inject
    @Default(longValues = 1L)
    private Long longWrapperProperty;

    @Inject
    @Default(longValues = { 1L, 1L })
    private Long[] longWrapperArrayProperty;

    public Boolean getBooleanWrapperProperty() {
        return booleanWrapperProperty;
    }

    public Boolean[] getBooleanWrapperArrayProperty() {
        return booleanWrapperArrayProperty;
    }

    public Long getLongWrapperProperty() {
        return longWrapperProperty;
    }

    public Long[] getLongWrapperArrayProperty() {
        return longWrapperArrayProperty;
    }

}
