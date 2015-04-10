package org.apache.sling.scripting.thymeleaf.internal.processor.attr;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.adapter.Adaptable;
import org.apache.sling.api.scripting.SlingBindings;
import org.apache.sling.api.scripting.SlingScriptHelper;
import org.apache.sling.commons.classloader.DynamicClassLoaderManager;
import org.apache.sling.scripting.thymeleaf.internal.SlingWebContext;
import org.thymeleaf.Arguments;
import org.thymeleaf.Configuration;
import org.thymeleaf.dom.Element;
import org.thymeleaf.processor.ProcessorResult;
import org.thymeleaf.processor.attr.AbstractAttrProcessor;
import org.thymeleaf.standard.expression.IStandardExpression;
import org.thymeleaf.standard.expression.IStandardExpressionParser;
import org.thymeleaf.standard.expression.StandardExpressions;
import org.thymeleaf.util.Validate;

public class SlingAdaptToAttrProcessor extends AbstractAttrProcessor {

	public static final int ATTRIBUTE_PRECEDENCE = 100;

	public static final String ATTRIBUTE_NAME = "adaptTo";
	public static final String VAR_ATTRIBUTE_NAME = "data-sling-var";
	public static final String ADAPTABLE_ATTRIBUTE_NAME = "data-sling-adaptable";

	private DynamicClassLoaderManager dynamicClassLoaderManager;


	public SlingAdaptToAttrProcessor(DynamicClassLoaderManager dynamicClassLoaderManager) {
		super(ATTRIBUTE_NAME);
		this.dynamicClassLoaderManager = dynamicClassLoaderManager;
	}

	@Override
	protected ProcessorResult processAttribute(Arguments arguments, Element element, String attributeName) {
		final SlingWebContext context = (SlingWebContext) arguments.getContext();
		final SlingHttpServletRequest request = context.getHttpServletRequest();
		final ClassLoader classLoader = getClassLoader(request);


		try {
			final String adaptTo = element.getAttributeValue(attributeName);
			final String var = element.getAttributeValue(VAR_ATTRIBUTE_NAME);
			final String adaptableValue = element.getAttributeValue(ADAPTABLE_ATTRIBUTE_NAME);

			Validate.notEmpty(var, "var must be set");
			Validate.notEmpty(adaptableValue, "adaptable must be set");

			final Configuration configuration = arguments.getConfiguration();
			final IStandardExpressionParser parser = StandardExpressions.getExpressionParser(configuration);
			final IStandardExpression expression = parser.parseExpression(configuration, arguments, adaptableValue);
			Adaptable adaptable = (Adaptable) expression.execute(configuration, arguments);

			Class<?> adaptToClass = classLoader.loadClass(adaptTo);

			Object adapted = adaptable.adaptTo(adaptToClass);
			if(adapted != null){
				context.getVariables().put(var, adapted);
				element.getParent().removeChild(element);
			}
			else {
				throw new RuntimeException("Could not adapt from " + adaptable.getClass().getName() + " to " + adaptTo);
			}

		} catch (ClassNotFoundException e) {
			throw new RuntimeException("Class not found: " + e.getMessage());
		}

		return ProcessorResult.OK;
	}

	@Override
	public int getPrecedence() {
		return ATTRIBUTE_PRECEDENCE;
	}

	private ClassLoader getClassLoader(SlingHttpServletRequest request){
		if(dynamicClassLoaderManager != null){
			return dynamicClassLoaderManager.getDynamicClassLoader();
		}

		final SlingBindings bindings = (SlingBindings) request.getAttribute(SlingBindings.class.getName());
		final SlingScriptHelper scriptHelper = bindings.getSling();
		final DynamicClassLoaderManager dynamicClassLoaderManager = scriptHelper.getService(DynamicClassLoaderManager.class);
		final ClassLoader classLoader = dynamicClassLoaderManager.getDynamicClassLoader();
		return classLoader;
	}
}
