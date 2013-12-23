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
package org.apache.sling.mailarchiveserver.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.mailarchiveserver.api.SearchQueryParser;

@Component
@Service(SearchQueryParser.class)
public class SearchQueryParserImpl implements SearchQueryParser {


	private static final String SPACE_SUBSTITUTION = ".-.";

	@Override
	public Map<String, List<String>> parse(String phrase) {
		Map<String, List<String>> res = new HashMap<String, List<String>>();
		phrase = phrase.trim();
		if (phrase == "") {
			return null;
		}

		phrase = parseQuotes(phrase);

		String[] lexemes = phrase.split(" ");
		for (String lexeme : lexemes) {
			String[] token = lexeme.split(":");
			if (token.length == 1) {
				insertTokenIntoMap(postprocessQuotedPhrase(token[0].trim()), SearchParameter.NONE, res);
			} else if (token.length > 1) {
				String searchParam = getSearchParameter(token[0]);
				if (searchParam != null) {
					insertTokenIntoMap(postprocessQuotedPhrase(token[1].trim()), searchParam, res);
				}
			}
		}
		return res;
	}

	private String postprocessQuotedPhrase(String phrase) {
		return phrase.replace(SPACE_SUBSTITUTION, " ");
	}

	private String parseQuotes(String phrase) {
		String[] quotes = phrase.split("\"");
		for (int i = 1; i < quotes.length; i += 2) {
			quotes[i] = quotes[i].replace(" ", SPACE_SUBSTITUTION);
		}
		String res = "";
		for (String s : quotes) {
			res += s;
		}
		return res;
	}

	private static String getSearchParameter(String s) {
		s = s.trim().toLowerCase();
		if (SEARCH_PARAMETERES.contains(s)) {
			return s;
		} else {
			return null;
		}
	}

	private static void insertTokenIntoMap(String tokenString, String tokenClass, Map<String, List<String>> map) {
		List<String> ls = map.get(tokenClass);
		if (ls == null) {
			ls = new ArrayList<String>();
		}
		ls.add(tokenString);
		map.put(tokenClass, ls);
	}

	public static final Set<String> SEARCH_PARAMETERES = new HashSet<String>();
	public static final Set<String> MESSAGE_FIELDS = new HashSet<String>();
	public static final Map<String, String> SEARCH_PARAMETER_TO_MESSAGE_FIELD_MAP = new HashMap<String, String>();

	static {
		SEARCH_PARAMETERES.add(SearchParameter.FROM);
		SEARCH_PARAMETERES.add(SearchParameter.SUBJ);
		SEARCH_PARAMETERES.add(SearchParameter.LIST);

		MESSAGE_FIELDS.add(SearchableMessageField.FROM);
		MESSAGE_FIELDS.add(SearchableMessageField.SUBJ);
		MESSAGE_FIELDS.add(SearchableMessageField.LIST);
		MESSAGE_FIELDS.add(SearchableMessageField.BODY);

		SEARCH_PARAMETER_TO_MESSAGE_FIELD_MAP.put(SearchParameter.FROM, SearchableMessageField.FROM);
		SEARCH_PARAMETER_TO_MESSAGE_FIELD_MAP.put(SearchParameter.SUBJ, SearchableMessageField.SUBJ);
		SEARCH_PARAMETER_TO_MESSAGE_FIELD_MAP.put(SearchParameter.LIST, SearchableMessageField.LIST);
	}

	public static class SearchParameter {
		public static final String NONE = ""; // not in SEARCH_PARAMETERES !
		public static final String FROM = "from";
		public static final String SUBJ = "subject";
		public static final String LIST = "list";
	}

	public static class SearchableMessageField {
		public static final String FROM = "From";
		public static final String SUBJ = "Subject";
		public static final String LIST = "'List-Id'";
		public static final String BODY = "Body";
		//		public static final String DATE = "";
		//		public static final String FROM = "";
		//		public static final String TO = "";


	}

}