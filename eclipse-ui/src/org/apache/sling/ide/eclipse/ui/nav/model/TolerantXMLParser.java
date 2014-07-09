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

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;

import de.pdark.decentxml.Document;
import de.pdark.decentxml.XMLParser;
import de.pdark.decentxml.XMLSource;
import de.pdark.decentxml.XMLStringSource;
import de.pdark.decentxml.XMLTokenizer;

public class TolerantXMLParser {

    public static Document parse(final InputStream in, final String originDetails) throws IOException {
        String xml = IOUtils.toString(in);
        return parse (xml, originDetails);   
    }
    
    public static Document parse(final String xml, final String originDetails) throws IOException {
        return parse (new XMLStringSource (xml), originDetails);   
    }
    
    public static Document parse(final XMLSource xmlSource, final String originDetails) throws IOException {
        XMLParser parser = new XMLParser () {
            @Override
            protected XMLTokenizer createTokenizer(XMLSource source) {
                XMLTokenizer tolerantTokenizerIgnoringEntities = new TolerantXMLTokenizer(source, originDetails);
                tolerantTokenizerIgnoringEntities.setTreatEntitiesAsText (this.isTreatEntitiesAsText());
                return tolerantTokenizerIgnoringEntities;
            }
        };
        return parser.parse (xmlSource);   
    }
    
}
