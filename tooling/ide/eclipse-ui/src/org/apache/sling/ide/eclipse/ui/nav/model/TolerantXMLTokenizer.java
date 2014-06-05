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
package org.apache.sling.ide.eclipse.ui.nav.model;

import org.eclipse.core.resources.IFile;

import de.pdark.decentxml.Location;
import de.pdark.decentxml.Token;
import de.pdark.decentxml.XMLParseException;
import de.pdark.decentxml.XMLSource;
import de.pdark.decentxml.XMLTokenizer;

/**
 * In parts inspired by/copied from de.pdark.decentxml.XMLTokenizer.
 * <p>
 * Note: this variant of the XMLTokenizer became necessary since XMLTokenizer incorrectly complains when encountering a
 * '>' inside an xml attribute value. The '>' is valid though.
 * 
 * @see <a href="https://code.google.com/p/decentxml/issues/detail?id=5">decentxml bug report</a>
 */
final class TolerantXMLTokenizer extends XMLTokenizer {
	private final IFile file;

	TolerantXMLTokenizer(XMLSource source, IFile file) {
		super(source);
		this.file = file;
	}

	@Override
	protected void parseAttribute(Token token) {
	    token.setType (Type.ATTRIBUTE);

	    parseName ("attribute");
	    
	    if (pos == token.getStartOffset())
	        throw new XMLParseException ("Expected attribute name", source, pos);
	    
	    skipWhiteSpace ();
	    expect ('=');
	    skipWhiteSpace ();
	    
	    char c = 0;
	    if (pos < source.length ())
	        c = source.charAt (pos);
	    if (c != '\'' && c != '"')
	        throw new XMLParseException ("Expected single or double quotes", source, pos);
	    
	    char endChar = c;
	    
	    while (true)
	    {
	        pos ++;
	        if (pos >= source.length ())
	        {
	            int i = Math.min (20, source.length () - token.getStartOffset ());
	            throw new XMLParseException ("Missing end quote ("+endChar+") of attribute: "
	                    +lookAheadForErrorMessage (null, token.getStartOffset (), i), token);
	        }
	        
	        c = source.charAt (pos);
	        if (c == endChar)
	            break;
	        if (c == '<') {
	        	Location l = new Location(source, pos);
	            System.err.println("Illegal character in attribute value: '"+c+"' in "+file.getFullPath()+" at "+l);
	        }
	    }
	    
	    // Skip end-char
	    pos ++;
	}
}