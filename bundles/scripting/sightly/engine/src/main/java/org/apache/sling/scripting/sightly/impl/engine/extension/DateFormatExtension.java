/*******************************************************************************
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
 ******************************************************************************/

package org.apache.sling.scripting.sightly.impl.engine.extension;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import org.apache.commons.lang.LocaleUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.scripting.sightly.SightlyException;
import org.apache.sling.scripting.sightly.extension.RuntimeExtension;
import org.apache.sling.scripting.sightly.impl.utils.BindingsUtils;
import org.apache.sling.scripting.sightly.render.RenderContext;
import org.apache.sling.scripting.sightly.render.RuntimeObjectModel;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(service = RuntimeExtension.class, property = { RuntimeExtension.NAME + "=" + "dateFormat" })
/**
 * Extension to format a date in HTL, use the following notation: ${ dateValue @ dateFormat}
 * dateFormat used is from SimpleDateFormat
 */
public class DateFormatExtension implements RuntimeExtension {

	private static final Logger LOG = LoggerFactory.getLogger(DateFormatExtension.class);

	private static final String DATE_FORMAT = "dateFormat";
	
	private static final String LOCALE_OPTION = "locale";
	
	private static final String TIMEZONE = "timezone";
	
	@Override
	public Object call(final RenderContext renderContext, Object... arguments) {
		ExtensionUtils.checkArgumentCount(DATE_FORMAT, arguments, 2);
		
        Map<String, Object> options = (Map<String, Object>) arguments[1];
        RuntimeObjectModel runtimeObjectModel = renderContext.getObjectModel();
        Object dateValue = arguments[0];
		String dateFormat = runtimeObjectModel.toString(options.get(DATE_FORMAT));

		String localeOption = null;
		if ( options.containsKey(LOCALE_OPTION)) {
			localeOption = runtimeObjectModel.toString(options.get(LOCALE_OPTION));
		}
		TimeZone timezone = null;
		if ( options.containsKey(TIMEZONE)) {
			timezone = TimeZone.getTimeZone(runtimeObjectModel.toString(options.get(TIMEZONE)));
		} else {
			timezone = TimeZone.getDefault();
		}

		Locale locale = getLocale( renderContext, localeOption);
		
		String returnValue = null;
		
		if (dateValue instanceof Calendar) {

			Calendar cal = (Calendar) dateValue;
			returnValue = format(cal.getTime(), dateFormat, locale, timezone);

		} else if (dateValue instanceof Date) {

			Date dateAsDate = (Date) dateValue;
			returnValue = format(dateAsDate, dateFormat, locale, timezone);

		} else {
			throw new SightlyException("Input value is not of a date type, supported types java.util.Date and java.util.Calendar");
		}
		return returnValue;
		
	}
	
	private Locale getLocale(RenderContext renderContext, String specifiedLocale) {
		
		if ( StringUtils.isNotBlank(specifiedLocale)) {
			Locale l = LocaleUtils.toLocale(specifiedLocale);
			if ( l != null) {
				return l;
			}
		}
		
        final SlingHttpServletRequest request = BindingsUtils.getRequest(renderContext.getBindings());
        return request.getLocale();
		
	}

	private String format(Date date, String format, Locale locale, TimeZone timezone) {
		LOG.trace("Formatting date {0}, with format {1}, locale {2}, timezone {3}", date, format, locale, timezone);

		try {
			SimpleDateFormat formatter = new SimpleDateFormat(format, locale);
			formatter.setTimeZone(timezone);
			return formatter.format(date);
		} catch (Exception e) {
			String error = String.format("Error during formatting of date %s with format %s, locale %s and timezone %s", date, format, locale, timezone);
			throw new SightlyException( error, e);
		}
	}
}
