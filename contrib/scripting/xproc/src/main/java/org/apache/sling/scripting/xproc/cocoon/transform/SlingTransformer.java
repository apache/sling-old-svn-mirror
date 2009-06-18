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
package org.apache.sling.scripting.xproc.cocoon.transform;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TemplatesHandler;
import javax.xml.transform.sax.TransformerHandler;

import org.apache.cocoon.pipeline.component.sax.AbstractTransformer;
import org.apache.cocoon.pipeline.component.sax.XMLConsumer;
import org.apache.cocoon.pipeline.component.sax.XMLConsumerAdapter;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.scripting.SlingScriptHelper;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

/**
 * A Cocoon transformer that executes XSLT
 * transformations. The templates are Sling 
 * resources supporting to be adapted to 
 * {@link InputStream}.
 */
public class SlingTransformer extends AbstractTransformer {
	
	private SlingHttpServletRequest request;
	private Map<String, Object> parameters;
    private String srcAbsPath;

    public SlingTransformer() {
        super();
    }

    public SlingTransformer(SlingScriptHelper sling, String srcAbsPath) {
        this(sling, srcAbsPath, null);
    }

    public SlingTransformer(SlingScriptHelper sling, String srcAbsPath, Map<String, Object> parameters) {
        super();
        if (srcAbsPath == null) {
            throw new IllegalArgumentException("The parameter 'source' mustn't be null.");
        }
        
        this.request = sling.getRequest();
        this.parameters = parameters;
        this.srcAbsPath = srcAbsPath;
    }

    /**
     * Test if the name is a valid parameter name for XSLT
     */
    private static boolean isValidXSLTParameterName(String name) {
        return name.matches("[a-zA-Z_][\\w\\-\\.]*");
    }

    @Override
    public void setConfiguration(Map<String, ? extends Object> configuration) {
        this.parameters = new HashMap<String, Object>(configuration);
    }

    @Override
    protected void setXMLConsumer(XMLConsumer consumer) {
        TransformerHandler transformerHandler;
        try {
            transformerHandler = this.createTransformerHandler();
        } catch (Exception ex) {
            throw new RuntimeException("Could not initialize transformer handler.", ex);
        }

        final Map<String, Object> map = this.getLogicSheetParameters();
        if (map != null) {
            final Transformer transformer = transformerHandler.getTransformer();

            for (Entry<String, Object> entry : map.entrySet()) {
                transformer.setParameter(entry.getKey(), entry.getValue());
            }
        }

        final SAXResult result = new SAXResult();
        result.setHandler(consumer);
        // According to TrAX specs, all TransformerHandlers are LexicalHandlers
        result.setLexicalHandler(consumer);
        transformerHandler.setResult(result);

        super.setXMLConsumer(new XMLConsumerAdapter(transformerHandler, transformerHandler));
    }

    private TransformerHandler createTransformerHandler() throws Exception {
        SAXTransformerFactory transformerFactory = (SAXTransformerFactory) TransformerFactory.newInstance();
        TemplatesHandler templatesHandler = transformerFactory.newTemplatesHandler();

        XMLReader xmlReader = XMLReaderFactory.createXMLReader();
        xmlReader.setContentHandler(templatesHandler);
        InputSource inputSource = new InputSource(getXsltSource());
        xmlReader.parse(inputSource);

        // Create transformer handler
        final TransformerHandler handler = transformerFactory.newTransformerHandler(templatesHandler.getTemplates());

        return handler;
    }

    private Map<String, Object> getLogicSheetParameters() {
        if (this.parameters == null) {
            return null;
        }

        Map<String, Object> result = new HashMap<String, Object>();

        for (Entry<String, Object> entry : this.parameters.entrySet()) {
            String name = entry.getKey();

            if (isValidXSLTParameterName(name)) {
                result.put(name, entry.getValue());
            }
        }

        return result;
    }
	
    /**
     * Get the XSLT source. For the time being, the 
     * path must be absolute.
     */
    private InputStream getXsltSource() throws Exception {
		// The source is a xml file
		Resource xmlResource = this.request.getResourceResolver().resolve(srcAbsPath);
		InputStream xmlSourceFile = xmlResource.adaptTo(InputStream.class);
		if (xmlSourceFile != null) 
			return xmlSourceFile;
		return null;
			
	}
    
}
