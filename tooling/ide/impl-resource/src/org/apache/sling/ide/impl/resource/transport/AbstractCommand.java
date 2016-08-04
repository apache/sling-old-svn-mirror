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
package org.apache.sling.ide.impl.resource.transport;

import java.util.Collections;
import java.util.Set;

import org.apache.commons.httpclient.HttpClient;
import org.apache.sling.ide.transport.Command;
import org.apache.sling.ide.transport.Repository.CommandExecutionFlag;
import org.apache.sling.ide.transport.RepositoryException;
import org.apache.sling.ide.transport.RepositoryInfo;
import org.apache.sling.ide.transport.Result;
import org.apache.sling.ide.util.PathUtil;

public abstract class AbstractCommand<T> implements Command<T> {

    protected RepositoryInfo repositoryInfo;
    protected HttpClient httpClient;
    protected String path;

    public AbstractCommand(RepositoryInfo repositoryInfo, HttpClient httpClient, String relativePath) {
        this.repositoryInfo = repositoryInfo;
        this.httpClient = httpClient;
        this.path = createFullPath(relativePath);
    }

    @Override
    public String getPath() {
        return path;
    }

    private String createFullPath(String relativePath) {

        return PathUtil.join(repositoryInfo.getUrl(), relativePath);
    }

    protected Result<T> resultForResponseStatus(int responseStatus) {
        if (isSuccessStatus(responseStatus))
            return AbstractResult.success(null);

        return failureResultForStatusCode(responseStatus);
    }

    protected Result<T> failureResultForStatusCode(int responseStatus) {
        return AbstractResult.failure(new RepositoryException("Repository has returned status code " + responseStatus));
    }

    protected boolean isSuccessStatus(int responseStatus) {

        // TODO - consider all 2xx and possibly 3xx as success?

        return responseStatus == 200 /* OK */|| responseStatus == 201 /* CREATED */;
    }

    @Override
    public Set<CommandExecutionFlag> getFlags() {
        // TODO - this is not supported
        return Collections.emptySet();
    }
    
    @Override
    public Kind getKind() {
        return null;
    }

}
