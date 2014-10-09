package org.apache.sling.testing.resourceresolver;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;

import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.commons.json.JSONException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class CreateViaSlingJSONTest {
	private ResourceResolver mockResolver;
	private String basePath = "/any/place/i/want/to/put/it/subTree";

	@Before
	public void setup() {

	}

	@Test
	public void testSubtreeRootNode() throws FileNotFoundException,
			IOException, JSONException, URISyntaxException, LoginException {
		mockResolver = new MockResourceResolverBuilder()
				.addResourcesToLoad(
						basePath,
						CreateViaSlingJSONTest.class
								.getResource("/CreateViaSlingJSONTest/rootSubTreeNode.infinity.json"))
				.load();
		Resource resource = mockResolver.getResource(basePath);
		Assert.assertNotNull("Root resource should never be null.", resource);
		ValueMap vm = resource.adaptTo(ValueMap.class);
		Assert.assertNotNull("ValueMap for root resource shoudl not be null",
				vm);
		Assert.assertEquals("Subtree root node value map not correct",
				"nt:unstructured", vm.get("jcr:primaryType", String.class));
	}

	@Test
	public void testOverlappingTrees() throws FileNotFoundException,
			IOException, JSONException, URISyntaxException, LoginException {
		mockResolver = new MockResourceResolverBuilder()
				.addResourcesToLoad(
						basePath,
						CreateViaSlingJSONTest.class
								.getResource("/CreateViaSlingJSONTest/rootSubTreeNode.infinity.json"))
				.addResourcesToLoad(
						"/any/place/i/also/want/to/put/it/subTree",
						CreateViaSlingJSONTest.class
								.getResource("/CreateViaSlingJSONTest/rootSubTreeNode.infinity.json"))
				.load();

		Resource resource = mockResolver.getResource(basePath);
		Assert.assertNotNull("Root resource should never be null.", resource);
		ValueMap vm = resource.adaptTo(ValueMap.class);
		Assert.assertNotNull("ValueMap for root resource shoudl not be null",
				vm);
		Assert.assertEquals("Subtree root node value map not correct",
				"nt:unstructured", vm.get("jcr:primaryType", String.class));
	}

	@Test(expected = FileNotFoundException.class)
	public void testFileNotFoundExceptions() throws FileNotFoundException,
			IOException, JSONException, URISyntaxException, LoginException {
		mockResolver = new MockResourceResolverBuilder()
				.addResourcesToLoad(
						basePath,
						new URL("file:///nothere.json"))
				.load();

	}

	@Test(expected = FileNotFoundException.class)
	public void testNullCheck() throws FileNotFoundException, IOException,
			JSONException, URISyntaxException, LoginException {
		mockResolver = new MockResourceResolverBuilder().addResourcesToLoad(
				basePath, null).load();

	}
}
