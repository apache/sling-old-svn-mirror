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
package org.apache.sling.scripting.xproc.xpl;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

import org.apache.sling.scripting.xproc.xpl.api.Step;
import org.custommonkey.xmlunit.XMLAssert;
import org.custommonkey.xmlunit.XMLUnit;

import junit.framework.TestCase;

public class XplBuilderTest extends TestCase {
	
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		XMLUnit.setIgnoreComments(true);
		XMLUnit.setIgnoreWhitespace(true);
	}
	
	public void testBuild() throws Exception {		
		String xplPath = "/xpl/html.xpl";		
		XplBuilder builder = new XplBuilder();
		Step pipeline = builder.build(getReaderFromPath(xplPath));
		String strControl = toString(getClass().getResourceAsStream(xplPath));
		XMLAssert.assertXMLEqual(strControl, pipeline.toString());
	}
	
	private Reader getReaderFromPath(String path) throws Exception {
		InputStream is = getClass().getResourceAsStream(path);
		Reader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
		return reader;
	}
	
	private String toString(InputStream ins) throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buf = new byte[1024];
        int rd;
        while ( (rd = ins.read(buf)) >= 0) {
            bos.write(buf, 0, rd);
        }
        bos.close();

        return new String(bos.toByteArray(), "UTF-8");
    }
	
}
