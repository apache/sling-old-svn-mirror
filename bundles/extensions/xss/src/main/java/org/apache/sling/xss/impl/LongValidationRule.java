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

/**
 * OWASP Enterprise Security API (ESAPI)
 *
 * This file is part of the Open Web Application Security Project (OWASP)
 * Enterprise Security API (ESAPI) project. For details, please see
 * <a href="http://www.owasp.org/index.php/ESAPI">http://www.owasp.org/index.php/ESAPI</a>.
 *
 * Copyright (c) 2007 - The OWASP Foundation
 *
 * The ESAPI is published by OWASP under the BSD license. You should read and accept the
 * LICENSE before you use, modify, and/or redistribute this software.
 *
 * @author Jeff Williams <a href="http://www.aspectsecurity.com">Aspect Security</a>
 * @created 2007
 */
package org.apache.sling.xss.impl;

import org.apache.sling.xss.XSSAPI;
import org.owasp.esapi.Encoder;
import org.owasp.esapi.StringUtilities;
import org.owasp.esapi.errors.ValidationException;
import org.owasp.esapi.reference.validation.BaseValidationRule;


/**
 * A validator performs syntax and possibly semantic validation of a single
 * piece of data from an untrusted source.
 * <p>
 * This class is derived from the OWASP ESAPI {@code LongValidationRule}
 * class to support validation of {@code long} values.
 *
 * @see XSSAPI#getValidLong(String, long)
 * @see org.owasp.esapi.Validator
 * @see org.owasp.esapi.reference.validation.IntegerValidationRule
 */
class LongValidationRule extends BaseValidationRule {

    private final long minValue;
    private final long maxValue;

    LongValidationRule( String typeName, Encoder encoder, long minValue, long maxValue ) {
        super( typeName, encoder );
        this.minValue = minValue;
        this.maxValue = maxValue;
    }

    public Long getValid( String context, String input ) throws ValidationException {
        return safelyParse(context, input);
    }

    private Long safelyParse(String context, String input) throws ValidationException {
        // do not allow empty Strings such as "   " - so trim to ensure
        // isEmpty catches "    "
        if (input != null) input = input.trim();

        if ( StringUtilities.isEmpty(input) ) {
            if (allowNull) {
                return null;
            }
            throw new ValidationException( context + ": Input number required", "Input number required: context=" + context + ", input=" + input, context );
        }

        // canonicalize
        String canonical = encoder.canonicalize( input );

        if (minValue > maxValue) {
            throw new ValidationException( context + ": Invalid number input: context", "Validation parameter error for number: maxValue ( " + maxValue + ") must be greater than minValue ( " + minValue + ") for " + context, context );
        }

        // validate min and max
        try {
            long i = Long.parseLong(canonical);
            if (i < minValue) {
                throw new ValidationException( "Invalid number input must be between " + minValue + " and " + maxValue + ": context=" + context, "Invalid number input must be between " + minValue + " and " + maxValue + ": context=" + context + ", input=" + input, context );
            }
            if (i > maxValue) {
                throw new ValidationException( "Invalid number input must be between " + minValue + " and " + maxValue + ": context=" + context, "Invalid number input must be between " + minValue + " and " + maxValue + ": context=" + context + ", input=" + input, context );
            }
            return i;
        } catch (NumberFormatException e) {
            throw new ValidationException( context + ": Invalid number input", "Invalid number input format: context=" + context + ", input=" + input, e, context);
        }
    }

    @Override
    public Long sanitize( String context, String input ) {
        Long toReturn = Long.valueOf( 0 );
        try {
            toReturn = safelyParse(context, input);
        } catch (ValidationException e ) {
            // do nothing
        }
        return toReturn;
    }
}