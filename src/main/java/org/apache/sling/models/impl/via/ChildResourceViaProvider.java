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
package org.apache.sling.models.impl.via;

import org.apache.commons.lang3.StringUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.ViaProviderType;
import org.apache.sling.models.annotations.via.ChildResource;
import org.apache.sling.models.spi.ViaProvider;

@Component
@Service
public class ChildResourceViaProvider implements ViaProvider {

    @Override
    public Class<? extends ViaProviderType> getType() {
        return ChildResource.class;
    }

    @Override
    public Object getAdaptable(Object original, String value) {
        if (StringUtils.isBlank(value)) {
            return ORIGINAL;
        }
        if (original instanceof Resource) {
            return ((Resource) original).getChild(value);
        } else {
            return null;
        }
    }
}
