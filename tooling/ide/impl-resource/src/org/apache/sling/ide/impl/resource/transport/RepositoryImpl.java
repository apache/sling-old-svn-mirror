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

import java.util.Map;

import org.apache.commons.httpclient.HttpClient;
import org.apache.sling.ide.impl.resource.util.Tracer;
import org.apache.sling.ide.transport.Command;
import org.apache.sling.ide.transport.FileInfo;
import org.apache.sling.ide.transport.ResourceProxy;
import org.osgi.service.event.EventAdmin;

public class RepositoryImpl extends AbstractRepository{
	
    private final HttpClient httpClient = new HttpClient();
    private Tracer tracer;
    private EventAdmin eventAdmin;

	@Override
	public Command<Void> newAddNodeCommand(final FileInfo fileInfo) {
        return wrap(new AddNodeCommand(fileInfo, repositoryInfo, httpClient));
	}

    private <T> Command<T> wrap(AbstractCommand<T> command) {

        return new TracingCommand<T>(command, tracer, eventAdmin);
    }

	@Override
	public Command<Void> newDeleteNodeCommand(final FileInfo fileInfo) {
        return wrap(new DeleteNodeCommand(fileInfo, repositoryInfo, httpClient));
	}
	
	@Override
    public Command<ResourceProxy> newListChildrenNodeCommand(final String path) {
        return wrap(new ListChildrenCommand(repositoryInfo, httpClient, path + ".1.json"));
	}

	@Override
	public Command<byte[]> newGetNodeCommand(final String path) {
		
        return wrap(new GetNodeCommand(repositoryInfo, httpClient, path));
	}
	
	@Override
    public Command<Map<String, Object>> newGetNodeContentCommand(final String path) {
        return wrap(new GetNodeContentCommand(repositoryInfo, httpClient, path + ".json"));
	}
	
	@Override
    public Command<Void> newUpdateContentNodeCommand(final FileInfo fileInfo, final Map<String, Object> properties) {
		
        return wrap(new UpdateContentCommand(repositoryInfo, httpClient, fileInfo.getRelativeLocation(), properties, fileInfo));
	}

    public void bindTracer(Tracer tracer) {

        this.tracer = tracer;
    }

    public void unbindTracer(Tracer tracer) {

        this.tracer = null;
    }

    public void bindEventAdmin(EventAdmin eventAdmin) {

        this.eventAdmin = eventAdmin;
    }

    public void unbindEventAdmin(EventAdmin eventAdmin) {

        this.eventAdmin = null;
    }
}
