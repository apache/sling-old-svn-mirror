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
