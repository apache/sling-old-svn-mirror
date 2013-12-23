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
package org.apache.sling.mailarchiveserver.impl;

import java.util.Iterator;

import javax.jcr.query.Query;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.mailarchiveserver.api.SearchQueryParser;
import org.apache.sling.mailarchiveserver.api.SearchService;

@Component
@Service(SearchService.class)
public class SearchServiceImpl implements SearchService {

	@Reference
	private	ResourceResolverFactory resourceResolverFactory;
	ResourceResolver resolver = null;
	@Reference 
	private SearchQueryParser parser;
	private QueryBuilderImpl queryBuilder = new QueryBuilderImpl();

	@Activate
	public void activate() throws LoginException {
		if (resolver == null) {
			resolver = resourceResolverFactory.getAdministrativeResourceResolver(null);
		}
	}

	@Override
	public Iterator<Resource> find(String phrase) {
		String query = queryBuilder.buildQuery(parser.parse(phrase), QueryBuilderImpl.SQL2);
		Iterator<Resource> res = resolver.findResources(query, Query.JCR_SQL2);
		return res;
	}
}
