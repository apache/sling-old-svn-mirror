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

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Designate(factory=true, ocd=MappingConfigAmendment.Config.class)
@Component(name = "org.apache.sling.serviceusermapping.impl.ServiceUserMapperImpl.amended",
           configurationPolicy=ConfigurationPolicy.REQUIRE,
           service={MappingConfigAmendment.class},
           property= {
                   "webconsole.configurationFactory.nameHint=Mapping: {user.mapping}",
           })
public class MappingConfigAmendment implements Comparable<MappingConfigAmendment> {

    @ObjectClassDefinition(name ="Apache Sling Service User Mapper Service Amendment",
            description="An amendment mapping for the user mapping service.")
    public @interface Config {

        @AttributeDefinition(name = "Ranking",
              description="Amendments are processed in order of their ranking, an amendment with a higher ranking has" +
                          " precedence over a mapping with a lower ranking.")
        int service_ranking() default 0;

        @AttributeDefinition(name = "Service Mappings",
            description = "Provides mappings from service name to user names. "
                + "Each entry is of the form 'bundleId [ \":\" subServiceName ] \"=\" userName' "
                + "where bundleId and subServiceName identify the service and userName "
                + "defines the name of the user to provide to the service. Invalid entries are logged and ignored.")
        String[] user_mapping() default {};
    }

    /** default logger */
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private Mapping[] serviceUserMappings;

    private int serviceRanking;

    @Activate
    @Modified
    void configure(final Config config) {
        final String[] props = config.user_mapping();

        if ( props != null ) {
            final ArrayList<Mapping> mappings = new ArrayList<Mapping>(props.length);
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
        } else {
            this.serviceUserMappings = new Mapping[0];
        }
        this.serviceRanking = config.service_ranking();
    }

    public Mapping[] getServiceUserMappings() {
        return this.serviceUserMappings;
    }

    @Override
    public int compareTo(final MappingConfigAmendment o) {
        // Sort by rank in descending order.
        if ( this.serviceRanking > o.serviceRanking ) {
            return -1; // lower rank
        } else if (this.serviceRanking < o.serviceRanking) {
            return 1; // higher rank
        }

        // If ranks are equal, then sort by hash code
        return this.hashCode() < o.hashCode() ? -1 : 1;
    }
}
