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
 * This sax serializer serializes to xml-
 */
@Component
@Service(value=SerializerFactory.class)
@Property(name="pipeline.type",value="xml-serializer")
public class XMLSerializerFactory implements SerializerFactory {

    /**
     * @see org.apache.sling.rewriter.SerializerFactory#createSerializer()
     */
    public Serializer createSerializer() {
        return new XMLSerializer();
    }

    public class XMLSerializer
        extends org.apache.cocoon.components.serializers.util.XMLSerializer
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
