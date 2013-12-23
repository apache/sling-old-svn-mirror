package org.apache.sling.mailarchiveserver.util;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Scanner;

import javax.jcr.query.Query;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;

/**
 * Helper REPL for querying JCR with SQL2 or XPath
 * 
 * @author bogomolo
 */

@Component
public class SearchSandbox {

	@Reference
	private	ResourceResolverFactory resourceResolverFactory;
	ResourceResolver resolver = null;

	@Activate
	public void test() throws PersistenceException, LoginException {
		System.out.println("*** Search service sandbox");

		if (resolver == null) {
			resolver = resourceResolverFactory.getAdministrativeResourceResolver(null);
		}

		Resource testRoot;

		final Resource root = resolver.getResource("/");
		final String path = getClass().getSimpleName();
		testRoot = resolver.getResource(root, path);
		if (testRoot == null) {
			testRoot = resolver.create(root, path, null);
			resolver.commit();
		}
		
		for (int i = 0; i < 3; i++) {
			final Props props = new Props("title", "HELLO", "jcr:text", "world");
			resolver.create(testRoot, "child_" + System.currentTimeMillis(), props).getPath();
			resolver.commit();
		}

		Scanner sc = new Scanner(System.in);
		System.out.println("Type \"quit\" to continue loading MAS.");
		System.out.print("*** New query: >");
		String query = sc.nextLine();
		while (!query.equalsIgnoreCase("quit")) {
			try {
				System.out.println("*** sql");
				Iterator<Resource> resSQL = resolver.findResources(query, Query.JCR_SQL2);
				while (resSQL.hasNext()) {
					Resource resource = (Resource) resSQL.next();
					System.out.println(resource.toString());
				}
				
//				System.out.println("*** xpath");
//				Iterator<Resource> resXPath = resolver.findResources(query, Query.XPATH);
//				while (resXPath.hasNext()) {
//					Resource resource = (Resource) resXPath.next();
//					System.out.println(resource.toString());
//				}

			} catch (Exception e) {
				e.printStackTrace();
			}
			
			System.out.print("*** New query: >");
			query = sc.nextLine();
		}
	}

	@SuppressWarnings("serial")
	private static class Props extends HashMap<String, Object> {
		Props(String ... keyValue) {
			for(int i=0 ; i< keyValue.length; i+=2) {
				put(keyValue[i], keyValue[i+1]);
			}
		}
	}

}
