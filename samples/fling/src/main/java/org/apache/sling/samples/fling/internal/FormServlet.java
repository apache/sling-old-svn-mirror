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
package org.apache.sling.samples.fling.internal;

import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;
import javax.servlet.RequestDispatcher;
import javax.servlet.Servlet;
import javax.servlet.ServletException;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestDispatcherOptions;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.scripting.SlingBindings;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.api.wrappers.SlingHttpServletRequestWrapper;
import org.apache.sling.commons.messaging.MessageService;
import org.apache.sling.commons.messaging.Result;
import org.apache.sling.samples.fling.form.Form;
import org.apache.sling.samples.fling.form.FormFactory;
import org.apache.sling.validation.ValidationResult;
import org.apache.sling.validation.ValidationService;
import org.apache.sling.validation.model.ValidationModel;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.thymeleaf.ITemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.context.IContext;

@Component(
    service = Servlet.class,
    property = {
        Constants.SERVICE_DESCRIPTION + "=Apache Sling Fling Sample “Form Servlet”",
        Constants.SERVICE_VENDOR + "=The Apache Software Foundation",
        "sling.servlet.resourceTypes=fling/page/form",
        "sling.servlet.methods=POST"
    }
)
public class FormServlet extends SlingAllMethodsServlet {

    @Reference(
        policy = ReferencePolicy.DYNAMIC,
        policyOption = ReferencePolicyOption.GREEDY
    )
    private volatile ValidationService validationService;

    @Reference(
        policy = ReferencePolicy.DYNAMIC,
        policyOption = ReferencePolicyOption.GREEDY
    )
    private volatile ITemplateEngine templateEngine;

    @Reference(
        policy = ReferencePolicy.DYNAMIC,
        policyOption = ReferencePolicyOption.GREEDY
    )
    private volatile MessageService messageService;

    private final String recipient = "recipient@example.net"; // TODO

    // see FormPage.form
    private static final String FORM_REQUEST_ATTRIBUTE_NAME = "form";

    private static final String SUCCESS_SELECTOR = "success";

    private final Logger logger = LoggerFactory.getLogger(FormServlet.class);

    public FormServlet() {
    }

    @Override
    protected void doPost(@Nonnull SlingHttpServletRequest request, @Nonnull SlingHttpServletResponse response) throws ServletException, IOException {
        final ValueMap parameters = request.adaptTo(ValueMap.class);
        logger.debug("parameters: {}", parameters);

        final String formType = parameters.get("formType", String.class);
        logger.debug("form type is '{}'", formType);

        final Form form = FormFactory.build(formType, parameters);
        if (form == null) {
            fail(null, 400, request, response);
            return;
        }

        final String resourcePath = request.getRequestPathInfo().getResourcePath();
        final ValidationModel validationModel = validationService.getValidationModel(form.getResourceType(), resourcePath, false);
        if (validationModel == null) {
            logger.error("no validation model found");
            fail(form, 500, request, response);
            return;
        }

        final ValidationResult validationResult = validationService.validate(parameters, validationModel);
        form.setValidationResult(validationResult);

        if (!validationResult.isValid()) {
            logger.debug("validation result not valid");
            fail(form, 400, request, response);
            return;
        }

        // render form with message template
        final String template = "/etc/messaging/form/comment.txt"; // TODO
        final Map<String, Object> variables = new HashMap<>();
        variables.put(SlingBindings.RESOLVER, request.getResourceResolver()); // TODO service resource resolver?
        variables.put("form", form);
        final IContext context = new Context(Locale.ENGLISH, variables);
        logger.debug("rendering message template '{}' with variables: {}", template, variables);
        final String message = templateEngine.process(template, context);
        logger.debug("message: '{}'", message);

        try {
            final CompletableFuture<Result> future = messageService.send(message, recipient);
            future.get(1, TimeUnit.SECONDS);
            logger.debug("comment [{}] form sent to {}", message, recipient);
        } catch (Exception e) {
            logger.error("sending message failed: {}", e.getMessage(), e);
            fail(form, 500, request, response);
            return;
        }

        succeed(form, request, response);
    }

    private void fail(final Form form, final int status, final SlingHttpServletRequest request, final SlingHttpServletResponse response) throws ServletException, IOException {
        response.setStatus(status);
        forward(form, request, response, null);
    }

    private void succeed(final Form form, final SlingHttpServletRequest request, final SlingHttpServletResponse response) throws ServletException, IOException {
        final RequestDispatcherOptions options = new RequestDispatcherOptions();
        options.setAddSelectors(SUCCESS_SELECTOR);
        forward(form, request, response, options);
    }

    private void forward(final Form form, final SlingHttpServletRequest request, final SlingHttpServletResponse response, final RequestDispatcherOptions options) throws ServletException, IOException {
        final FormValidationHttpServletRequestWrapper requestWrapper = new FormValidationHttpServletRequestWrapper(request, form);
        final RequestDispatcher dispatcher = request.getRequestDispatcher(request.getResource(), options);
        dispatcher.forward(requestWrapper, response);
    }

    private class FormValidationHttpServletRequestWrapper extends SlingHttpServletRequestWrapper {

        FormValidationHttpServletRequestWrapper(final SlingHttpServletRequest request, final Form form) {
            super(request);
            setAttribute(FORM_REQUEST_ATTRIBUTE_NAME, form);
        }

        @Override
        public String getMethod() {
            return HttpConstants.METHOD_GET;
        }

    }

}
