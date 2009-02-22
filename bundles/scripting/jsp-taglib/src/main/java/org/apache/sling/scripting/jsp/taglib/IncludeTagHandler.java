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
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.tagext.BodyContent;

/**
 * The <code>IncludeTagHandler</code> implements the
 * <code>&lt;sling:include&gt;</code> custom tag.
 */
public class IncludeTagHandler extends AbstractDispatcherTagHandler {

    /** flush argument */
    private boolean flush = false;

    protected void dispatch(RequestDispatcher dispatcher,
            ServletRequest request, ServletResponse response)
            throws IOException, ServletException {

        // optionally flush
        if (flush && !(pageContext.getOut() instanceof BodyContent)) {
            // might throw an IOException of course
            pageContext.getOut().flush();
        }

        dispatcher.include(request, response);
    }

    public void setPageContext(PageContext pageContext) {
        super.setPageContext(pageContext);

        // init local fields, since tag might be reused
        flush = false;
    }

    public void setFlush(boolean flush) {
        this.flush = flush;
    }

}
