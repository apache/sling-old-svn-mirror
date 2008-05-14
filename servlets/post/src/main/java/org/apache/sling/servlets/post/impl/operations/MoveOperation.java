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
package org.apache.sling.servlets.post.impl.operations;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.sling.api.servlets.HtmlResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The <code>MoveOperation</code> class implements the
 * {@link org.apache.sling.servlets.post.SlingPostConstants#OPERATION_MOVE move}
 * operation for the Sling default POST servlet.
 */
public class MoveOperation extends AbstractCopyMoveOperation {

    /**
     * default log
     */
    private static final Logger log = LoggerFactory.getLogger(MoveOperation.class);

    @Override
    protected String getOperationName() {
        return "move";
    }

    @Override
    protected void execute(HtmlResponse response, Session session,
            String source, String dest) throws RepositoryException {

        session.move(source, dest);
        response.onMoved(source, dest);
        log.debug("moved {} to {}", source, dest);
    }
}
