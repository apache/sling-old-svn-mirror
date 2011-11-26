package org.apache.sling.jcr.contentloader.internal;

import java.util.HashMap;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;

import org.apache.sling.jcr.contentloader.ImportOptions;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JMock;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.runner.RunWith;

@RunWith(JMock.class)
public class DefaultContentCreatorTest {
    
    DefaultContentCreator contentCreator;
    
    Mockery mockery = new JUnit4Mockery();
    
    Node parentNode;
    
    Property prop;
    
    @org.junit.Test public void willRewriteUndefinedPropertyType() throws RepositoryException {
        contentCreator = new DefaultContentCreator(null);
        parentNode = mockery.mock(Node.class);
        prop = mockery.mock(Property.class);
        contentCreator.init(new ImportOptions(){

            @Override
            public boolean isCheckin() {
                return false;
            }

			@Override
			public boolean isAutoCheckout() {
				return true;
			}

			@Override
            public boolean isIgnoredImportProvider(String extension) {
                return false;
            }

            @Override
            public boolean isOverwrite() {
                return true;
            }
            
            @Override
			public boolean isPropertyOverwrite() {
				return true;
			} }, new HashMap<String, ImportProvider>(), null, null);
        
        contentCreator.prepareParsing(parentNode, null);
        this.mockery.checking(new Expectations() {{
        	allowing(parentNode).isNodeType("mix:versionable"); will(returnValue(Boolean.FALSE));
        	allowing(parentNode).getParent(); will(returnValue(null));
            oneOf (parentNode).hasProperty("foo"); will(returnValue(Boolean.TRUE));
            oneOf (parentNode).setProperty(with(equal("foo")), with(equal("bar")));
        }});
        contentCreator.createProperty("foo", PropertyType.UNDEFINED, "bar");
    }
    
    @org.junit.Test public void willNotRewriteUndefinedPropertyType() throws RepositoryException {
        contentCreator = new DefaultContentCreator(null);
        parentNode = mockery.mock(Node.class);
        prop = mockery.mock(Property.class);
        contentCreator.init(new ImportOptions(){

            @Override
            public boolean isCheckin() {
                return false;
            }

            @Override
			public boolean isAutoCheckout() {
				return true;
			}

            @Override
            public boolean isIgnoredImportProvider(String extension) {
                return false;
            }

            @Override
            public boolean isOverwrite() {
                return false;
            }

			@Override
			public boolean isPropertyOverwrite() {
				return false;
			} }, new HashMap<String, ImportProvider>(), null, null);
        
        contentCreator.prepareParsing(parentNode, null);
        this.mockery.checking(new Expectations() {{
            oneOf (parentNode).hasProperty("foo"); will(returnValue(Boolean.TRUE));
            oneOf (parentNode).getProperty("foo"); will(returnValue(prop));
            oneOf (prop).isNew(); will(returnValue(Boolean.FALSE));
        }});
        contentCreator.createProperty("foo", PropertyType.UNDEFINED, "bar");
    }

}
