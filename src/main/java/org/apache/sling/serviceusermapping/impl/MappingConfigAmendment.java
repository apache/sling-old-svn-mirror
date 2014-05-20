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
package org.apache.sling.serviceusermapping.impl;

import java.util.ArrayList;
import java.util.Map;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.PropertyUnbounded;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.sling.commons.osgi.ServiceUtil;
import org.osgi.framework.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(metatype=true,
        name="org.apache.sling.serviceusermapping.impl.ServiceUserMapperImpl.amended",
        label="Apache Sling Service User Mapper Service Amendment",
        description="An amendment mapping for the user mapping service.",
        configurationFactory=true,
        policy=ConfigurationPolicy.REQUIRE)
@Service(value={MappingConfigAmendment.class})
@Property(name=Constants.SERVICE_RANKING, intValue=0, propertyPrivate=false,
          label="Ranking",
          description="Amendments are processed in order of their ranking, an amendment with a higher ranking has" +
                      " precedence over a mapping with a lower ranking.")
public class MappingConfigAmendment implements Comparable<MappingConfigAmendment> {

    @Property(
            label = "Service Mappings",
            description = "Provides mappings from service name to user names. "
                + "Each entry is of the form 'serviceName [ \":\" subServiceName ] \"=\" userName' "
                + "where serviceName and subServiceName identify the service and userName "
                + "defines the name of the user to provide to the service. Invalid entries are logged and ignored.",
            unbounded = PropertyUnbounded.ARRAY)
    private static final String PROP_SERVICE2USER_MAPPING = "user.mapping";

    private static final String[] PROP_SERVICE2USER_MAPPING_DEFAULT = {};

    /** default logger */
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private Mapping[] serviceUserMappings;

    private Comparable<Object> comparable;

    @Activate
    @Modified
    void configure(final Map<String, Object> config) {
        final String[] props = PropertiesUtil.toStringArray(config.get(PROP_SERVICE2USER_MAPPING),
            PROP_SERVICE2USER_MAPPING_DEFAULT);

        ArrayList<Mapping> mappings = new ArrayList<Mapping>(props.length);
        for (final String prop : props) {
            if (prop != null && prop.trim().length() > 0 ) {
                try {
                    final Mapping mapping = new Mapping(prop.trim());
                    mappings.add(mapping);
                } catch (final IllegalArgumentException iae) {
                    logger.info("configure: Ignoring '{}': {}", prop, iae.getMessage());
                }
            }
        }

        this.serviceUserMappings = mappings.toArray(new Mapping[mappings.size()]);
        this.comparable = ServiceUtil.getComparableForServiceRanking(config);
    }

    public Mapping[] getServiceUserMappings() {
        return this.serviceUserMappings;
    }

    public int compareTo(final MappingConfigAmendment o) {
        return -this.comparable.compareTo(o.comparable);
    }
}
