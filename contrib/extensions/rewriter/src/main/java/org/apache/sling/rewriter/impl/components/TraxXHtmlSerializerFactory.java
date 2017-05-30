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

import org.apache.sling.rewriter.SerializerFactory;
import org.osgi.service.component.annotations.Component;


/**
 * This sax serializer serializes xhtml-
 */
@Component(service = SerializerFactory.class,
    property = {
            "pipeline.type=trax-xhtml-serializer"
    })
public class TraxXHtmlSerializerFactory extends AbstractTraxSerializerFactory {

    @Override
    protected String getOutputFormat() {
        return "xhtml";
    }

    protected String getDoctypePublic() {
        return "-//W3C//DTD XHTML 1.0 Strict//EN";
    }

    protected String getDoctypeSystem() {
        return "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd";
    }
}
