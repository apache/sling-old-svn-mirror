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
package org.apache.sling.rewriter.impl.components;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.rewriter.ProcessingComponentConfiguration;
import org.apache.sling.rewriter.ProcessingContext;
import org.apache.sling.rewriter.Serializer;
import org.apache.sling.rewriter.SerializerFactory;

/**
 * This sax serializer serializes xhtml-
 */
@Component
@Service(value=SerializerFactory.class)
@Property(name="pipeline.type",value="xhtml-serializer")
public class XHtmlSerializerFactory implements SerializerFactory {

    /**
     * @see org.apache.sling.rewriter.SerializerFactory#createSerializer()
     */
    public Serializer createSerializer() {
        return new XHTMLSerializer();
    }

    /**
     * <p>A pedantinc XHTML serializer encoding all recognized entities with their
     * proper HTML names.</p>
     *
     * <p>For configuration options of this serializer, please look at the
     * {@link org.apache.cocoon.components.serializers.util.EncodingSerializer},
     * in addition to those, this serializer also support the specification of a
     * default doctype. This default will be used if no document type is received
     * in the SAX events.
     *
     * <p>The value <i>mytype</i> can be one of:</p>
     *
     * <dl>
     *   <dt>"<code>none</code>"</dt>
     *   <dd>Not to emit any dococument type declaration.</dd>
     *   <dt>"<code>strict</code>"</dt>
     *   <dd>The XHTML 1.0 Strict document type.</dd>
     *   <dt>"<code>loose</code>"</dt>
     *   <dd>The XHTML 1.0 Transitional document type.</dd>
     *   <dt>"<code>frameset</code>"</dt>
     *   <dd>The XHTML 1.0 Frameset document type.</dd>
     * </dl>
     *
     */
    public class XHTMLSerializer
        extends org.apache.cocoon.components.serializers.util.XHTMLSerializer
        implements Serializer  {

        /**
         * @see org.apache.sling.rewriter.Serializer#init(org.apache.sling.rewriter.ProcessingContext, org.apache.sling.rewriter.ProcessingComponentConfiguration)
         */
        public void init(ProcessingContext context,
                         ProcessingComponentConfiguration config)
        throws IOException {
            final String encoding = config.getConfiguration().get("encoding", "UTF-8");
            try {
                this.setEncoding(encoding);
            } catch (UnsupportedEncodingException exception) {
                throw new IOException("Encoding not supported: " + encoding);
            }
            setIndentPerLevel(config.getConfiguration().get("indent", 0));
            setOmitXmlDeclaration(config.getConfiguration().get("omit-xml-declaration", "no"));
            setDoctypeDefault(config.getConfiguration().get("doctype-default", String.class));

            this.setup(context.getRequest());
            this.setOutputStream(context.getOutputStream());
        }

        /**
         * @see org.apache.sling.rewriter.Serializer#dispose()
         */
        public void dispose() {
            // nothing to do
        }
    }
}
