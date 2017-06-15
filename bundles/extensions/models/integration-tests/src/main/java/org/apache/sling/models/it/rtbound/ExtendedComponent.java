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
package org.apache.sling.models.it.rtbound;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.Model;

@Model(adaptables = { Resource.class }, resourceType = "sling/rt/extended")
public class ExtendedComponent extends BaseComponent {

    private final Date d = new Date();

    public ExtendedComponent(Resource resource) {
        super(resource);
    }

    public Calendar getDateByCalendar() {
        Calendar cal = new GregorianCalendar();
        cal.setTime(d);
        return cal;
    }

    public Date getDate() {
        return d;
    }
}
