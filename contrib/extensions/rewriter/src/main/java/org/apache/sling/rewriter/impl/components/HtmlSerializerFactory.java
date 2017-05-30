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

import org.apache.sling.rewriter.ProcessingComponentConfiguration;
import org.apache.sling.rewriter.ProcessingContext;
import org.apache.sling.rewriter.Serializer;
import org.apache.sling.rewriter.SerializerFactory;
import org.osgi.service.component.annotations.Component;

/**
 * This sax serializer serializes html-
 */
@Component(service = SerializerFactory.class,
    property = {
            "pipeline.type=html-serializer"
    })
public class HtmlSerializerFactory implements SerializerFactory {

    /**
     * @see org.apache.sling.rewriter.SerializerFactory#createSerializer()
     */
    public Serializer createSerializer() {
        return new HTMLSerializer();
    }

    /**
     * <p>A serializer converting XHTML into plain old HTML.</p>
     *
     * <p>For configuration options of this serializer, please look at the
     * {@link XHtmlSerializerFactory} and
     * {@link org.apache.cocoon.components.serializers.util.EncodingSerializer}.</p>
     *
     * <p>Any of the XHTML document type declared or used will be converted into
     * its HTML 4.01 counterpart, and in addition to those a "compatible" doctype
     * can be supported to exploit a couple of shortcuts into MSIE's rendering
     * engine. The values for the <code>doctype-default</code> can then be:</p>
     *
     * <dl>
     *   <dt>"<code>none</code>"</dt>
     *   <dd>Not to emit any dococument type declaration.</dd>
     *   <dt>"<code>compatible</code>"</dt>
     *   <dd>The HTML 4.01 Transitional (exploiting MSIE shortcut).</dd>
     *   <dt>"<code>strict</code>"</dt>
     *   <dd>The HTML 4.01 Strict document type.</dd>
     *   <dt>"<code>loose</code>"</dt>
     *   <dd>The HTML 4.01 Transitional document type.</dd>
     *   <dt>"<code>frameset</code>"</dt>
     *   <dd>The HTML 4.01 Frameset document type.</dd>
     * </dl>
     *
     */
    public class HTMLSerializer
        extends org.apache.cocoon.components.serializers.util.HTMLSerializer
        implements Serializer {

        /**
         * @see org.apache.sling.rewriter.Serializer#init(org.apache.sling.rewriter.ProcessingContext, org.apache.sling.rewriter.ProcessingComponentConfiguration)
         */
        public void init(ProcessingContext context,
                ProcessingComponentConfiguration config)
        throws IOException {
            String encoding = config.getConfiguration().get("encoding", "UTF-8");
            try {
                this.setEncoding(encoding);
            } catch (UnsupportedEncodingException exception) {
                throw new IOException("Encoding not supported: " + encoding);
            }

            this.setIndentPerLevel(config.getConfiguration().get("indent", 0));
            this.setDoctypeDefault(config.getConfiguration().get("doctype-default", String.class));

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
