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
package org.apache.sling.launchpad.testservices.servlets;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlValue;

import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;

/**
 * The <tt>JaxbServlet</tt> serializes a basic JAXB-annotated class
 * 
 */
@SlingServlet(paths = "/bin/jaxb", extensions = "xml")
public class JaxbServlet extends SlingAllMethodsServlet {

    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)
            throws ServletException, IOException {

        Entity entity = new Entity();
        entity.setName("entity-name");

        try {
            JAXBContext context = JAXBContext.newInstance(Entity.class);

            response.setHeader("Content-Type", "application/xml");
            context.createMarshaller().marshal(entity, response.getOutputStream());

        } catch (JAXBException e) {
            throw new ServletException(e);
        }
    }

    @XmlRootElement
    public static class Entity {

        private String name;

        public void setName(String name) {
            this.name = name;
        }

        @XmlValue
        public String getName() {
            return name;
        }
    }
}
