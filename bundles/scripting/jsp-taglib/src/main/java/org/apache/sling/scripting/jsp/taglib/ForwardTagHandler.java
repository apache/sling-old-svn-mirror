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
package org.apache.sling.scripting.jsp.taglib;

import java.io.IOException;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.jsp.JspTagException;

/**
 * The <code>ForwardTagHandler</code> implements the
 * <code>&lt;sling:forward&gt;</code> custom tag.
 */
public class ForwardTagHandler extends AbstractDispatcherTagHandler {

    private static final long serialVersionUID = 4516800311593983971L;

    protected void dispatch(RequestDispatcher dispatcher,
            ServletRequest request, ServletResponse response)
            throws IOException, ServletException, JspTagException {
        try {
            dispatcher.forward(request, response);
        } catch (IllegalStateException ise) {
            throw new JspTagException(ise);
        }
    }

}
