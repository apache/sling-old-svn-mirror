package org.apache.sling.auth.core.impl;

import org.osgi.framework.ServiceReference;

import junit.framework.TestCase;

public class PathBasedHolderTest extends TestCase {

	
	private static final String CONTENT_PRIVATE = "/content/private";



	public void testRoot(){
		
		PathBasedHolder holder= new TestHolder("/",null);
		assertTrue(holder.isWithin("/content"));
		
	}
	
	public void tetSamePath(){
		
		PathBasedHolder holder= new TestHolder(CONTENT_PRIVATE,null);
		assertTrue(holder.isWithin(CONTENT_PRIVATE));
		
	}
	
	public void tetBelowPath(){
		
		PathBasedHolder holder= new TestHolder(CONTENT_PRIVATE,null);
		assertTrue(holder.isWithin("/content/private/page"));
		
	}
	
	public void tetWithExtensionPath(){
		
		PathBasedHolder holder= new TestHolder(CONTENT_PRIVATE,null);
		assertTrue(holder.isWithin("/content/private.html"));
		
	}
	
	public void testSamePrefix(){
		PathBasedHolder holder= new TestHolder(CONTENT_PRIVATE,null);
		assertFalse(holder.isWithin("/content/private2"));
	}
	
	
	
	class TestHolder extends PathBasedHolder{

		protected TestHolder(String url, ServiceReference serviceReference) {
			super(url, serviceReference);
		}
		
		
	}
}
