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
import java.util.GregorianCalendar;

import org.apache.sling.scripting.sightly.extension.RuntimeExtension;
import org.apache.sling.scripting.sightly.render.RenderContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.osgi.service.component.annotations.Component;

@Component(service = RuntimeExtension.class, property = { RuntimeExtension.NAME + "=" + "dateFormat" })
public class DateFormatExtension implements RuntimeExtension {

	private static final Logger LOG = LoggerFactory.getLogger(DateFormatExtension.class);

	@Override
	public Object call(final RenderContext renderContext, Object... arguments) {
		ExtensionUtils.checkArgumentCount("dateFormat", arguments, 2);
		Object dateValue = arguments[0];

		String dateFormat = String.valueOf(arguments[1]);

		if (dateValue instanceof GregorianCalendar) {

			GregorianCalendar gregorianCal = (GregorianCalendar) dateValue;
			SimpleDateFormat formatter = new SimpleDateFormat(dateFormat);
			return formatter.format(gregorianCal.getTime());

		} else if (dateValue instanceof Calendar) {

			Calendar cal = (Calendar) dateValue;
			String value = format(cal, dateFormat);
			return value;

		} else if (dateValue instanceof Date) {

			SimpleDateFormat formatter = new SimpleDateFormat(dateFormat);
			Date dateAsDate = (Date) dateValue;
			return formatter.format(dateAsDate.getTime());

		} else {
			return dateValue;
		}
	}

	private String format(Calendar cal, String format) {
		SimpleDateFormat formatter = new SimpleDateFormat(format);
		return formatter.format(cal);
	}
}
