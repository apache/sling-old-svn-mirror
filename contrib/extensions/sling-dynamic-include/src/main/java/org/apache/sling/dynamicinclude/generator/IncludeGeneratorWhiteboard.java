/*-
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

package org.apache.sling.dynamicinclude.generator;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.Service;

/**
 * Service that provides include generator of given type.
 * 
 * @author tomasz.rekawek
 */

@Component
@Service(IncludeGeneratorWhiteboard.class)
public class IncludeGeneratorWhiteboard {

    @Reference(referenceInterface = IncludeGenerator.class, cardinality = ReferenceCardinality.MANDATORY_MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    private Set<IncludeGenerator> generators = new CopyOnWriteArraySet<IncludeGenerator>();

    public IncludeGenerator getGenerator(String type) {
        for (IncludeGenerator generator : generators) {
            if (type.equals(generator.getType())) {
                return generator;
            }
        }
        return null;
    }

    void bindGenerators(IncludeGenerator generator) {
        generators.add(generator);
    }

    void unbindGenerators(IncludeGenerator generator) {
        generators.remove(generator);
    }

}
