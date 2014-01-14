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
package org.apache.sling.scripting.javascript.wrapper;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import org.apache.sling.scripting.javascript.SlingWrapper;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.Undefined;
import org.apache.sling.commons.json.jcr.JsonItemWriter;

@SuppressWarnings("serial")
public class ScriptableCalendar extends ScriptableBase implements SlingWrapper {

	public static final String CLASSNAME = "Calendar";
	private SimpleDateFormat calendarFormat;

	/** Calendar is a class, not an interface - so we need to enumerate possible implementations here */
    public static final Class<?> [] WRAPPED_CLASSES = { Calendar.class, GregorianCalendar.class };

    /**
     * The wrapped Calendar. Will be {@code null} if the
     * {@link #jsConstructor(Object)} method is not called, which particularly
     * is the case for the Calendar host object prototype.
     */
	private Calendar calendar;

    public Class<?>[] getWrappedClasses() {
		return WRAPPED_CLASSES;
	}

    public void jsConstructor(Object o) {
        this.calendar = (Calendar) o;
    }

    @Override
    public Object get(String name, Scriptable start) {

        // builtin javascript properties (jsFunction_ etc.) have priority
        final Object fromSuperclass = super.get(name, start);
        if(fromSuperclass != Scriptable.NOT_FOUND) {
            return fromSuperclass;
        }

        if(calendar == null) {
            return Undefined.instance;
        }

        if("date".equals(name)) {
        	return ScriptRuntime.toObject(this, calendar.getTime());
        }

        return getNative(name, start);
    }

	@Override
	protected Class<?> getStaticType() {
		return Calendar.class;
	}

	@Override
	protected Object getWrappedObject() {
		return calendar;
	}

	@Override
	public Class<?> jsGet_javascriptWrapperClass() {
		return getClass();
	}

	@Override
	public String getClassName() {
		return CLASSNAME;
	}

	@Override
	public String toString() {
        if (calendarFormat == null) {
            calendarFormat = new SimpleDateFormat(JsonItemWriter.ECMA_DATE_FORMAT, JsonItemWriter.DATE_FORMAT_LOCALE);
        }
        return calendarFormat.format(calendar.getTime());
	}

    public Object unwrap() {
        return calendar;
    }

    @SuppressWarnings("unchecked")
	@Override
    public Object getDefaultValue(Class typeHint) {
    	return toString();
    }
}
