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
package org.apache.sling.pipes.internal;

import static org.apache.sling.query.SlingQuery.$;

import java.util.Iterator;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.pipes.BasePipe;
import org.apache.sling.pipes.Plumber;
import org.apache.sling.query.SlingQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * this pipe uses SlingQuery to filters children (filter defined in expr property) of
 * a resource (defined in the path property)
 */
public class SlingQueryPipe extends BasePipe {
    private static Logger logger = LoggerFactory.getLogger(SlingQueryPipe.class);

    public final static String RESOURCE_TYPE = RT_PREFIX + "slingQuery";

    public SlingQueryPipe(Plumber plumber, Resource resource) throws Exception {
        super(plumber, resource);
    }

    @Override
    public boolean modifiesContent() {
        return false;
    }

    public Iterator<Resource> getOutput() {
        Resource resource = getInput();
        if (resource != null) {
            String queryExpression = getExpr();
            SlingQuery query = $(resource).children(getExpr());
            logger.info("[sling query]: executing $({}).children({})", resource.getPath(), queryExpression);
            return query.iterator();
        }
        return EMPTY_ITERATOR;
    }
}
