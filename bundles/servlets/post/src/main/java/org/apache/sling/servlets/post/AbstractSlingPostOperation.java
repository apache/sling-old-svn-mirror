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
package org.apache.sling.servlets.post;

import java.util.List;

import javax.jcr.RepositoryException;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.servlets.HtmlResponse;
import org.apache.sling.servlets.post.impl.helper.HtmlPostResponseProxy;
import org.apache.sling.servlets.post.impl.helper.HtmlResponseProxy;

/**
 * The <code>AbstractSlingPostOperation</code> is the abstract base class
 * implementation of the {@link SlingPostOperation} interface extending the new
 * {@link AbstractPostOperation}.
 * <p>
 * This class exists for backwards compatibility. Existing implementations are
 * advised to migrate to the new {@link AbstractPostOperation}.
 *
 * @deprecated as of 2.0.8 (Bundle version 2.2.0) and replaced by
 *             {@link AbstractPostOperation}.
 */
public abstract class AbstractSlingPostOperation extends AbstractPostOperation
        implements SlingPostOperation {

    /**
     *
     * @param request
     * @param response
     * @param changes
     * @throws RepositoryException
     */
    protected abstract void doRun(SlingHttpServletRequest request,
            HtmlResponse response, List<Modification> changes)
            throws RepositoryException;

    /**
     * Implementation of the
     * {@link AbstractPostOperation#doRun(SlingHttpServletRequest, PostResponse, List)}
     * method calling our own
     * {@link #run(SlingHttpServletRequest, HtmlResponse, SlingPostProcessor[])}
     * meethod with a proxy for the Sling API <code>HtmlResponse</code>.
     */
    protected void doRun(SlingHttpServletRequest request,
            PostResponse response, List<Modification> changes)
            throws RepositoryException {
        final HtmlResponse htmlResponseProxy = (response instanceof HtmlPostResponseProxy)
                ? ((HtmlPostResponseProxy) response).getHtmlResponse()
                : new HtmlResponseProxy(response);
        doRun(request, htmlResponseProxy, changes);
    }

    /**
     * Implementation of the
     * {@link SlingPostOperation#run(SlingHttpServletRequest, HtmlResponse, SlingPostProcessor[])}
     * API method calling the
     * {@link PostOperation#run(SlingHttpServletRequest, PostResponse, SlingPostProcessor[])}
     * with a proxy around the Sling API <code>HtmlResponse</code> provided.
     */
    public void run(SlingHttpServletRequest request, HtmlResponse response,
            SlingPostProcessor[] processors) {
        final PostResponse postResponseProxy = new HtmlPostResponseProxy(
            response);
        run(request, postResponseProxy, processors);
    }

}