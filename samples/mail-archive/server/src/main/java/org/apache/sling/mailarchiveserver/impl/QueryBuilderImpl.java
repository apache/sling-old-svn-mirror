package org.apache.sling.mailarchiveserver.impl;

import static org.apache.sling.mailarchiveserver.impl.SearchQueryParserImpl.MESSAGE_FIELDS;
import static org.apache.sling.mailarchiveserver.impl.SearchQueryParserImpl.SEARCH_PARAMETER_TO_MESSAGE_FIELD_MAP;

import java.util.List;
import java.util.Map;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.mailarchiveserver.api.QueryBuilder;
import org.apache.sling.mailarchiveserver.impl.SearchQueryParserImpl.SearchParameter;

@Component
@Service(QueryBuilder.class)
public class QueryBuilderImpl implements QueryBuilder {

	static final String BASE = "SELECT * FROM [nt:unstructured] WHERE [sling:resourceType] = 'mailarchiveserver/message'";
	static final String DUMMY = "SELECT * FROM [nt:frozenNode] WHERE [42] = 0";
	private static final String AND = " AND ";
	private static final String OR = " OR ";

	public static final String SQL2 = "SQL2";

	private String buildSQL2Query(Map<String, List<String>> tokens) {
		if (tokens == null) {
			return BASE; 
		} else if (tokens.size() == 0) {
			return DUMMY;
		}

		String constraints = "";

		// tokens constraints
		for (String tokenClass : tokens.keySet()) {
			if (tokenClass.equals(SearchParameter.NONE)) {
				continue;
			}
			String fieldConstraint =  buildFieldConstraints(tokenClass, tokens);
			constraints += "("+ fieldConstraint +")" + AND;
		}

		// global constraints
		String globalConstraint = "";
		List<String> ls = tokens.get(SearchParameter.NONE);
		if (ls != null) {
			for (String msgField : MESSAGE_FIELDS) {
				for (String value : ls) {
					if (!value.trim().equals("")) {
						globalConstraint += sqlLikeConstraint(sqlLower(msgField), value.toLowerCase()) + OR;
					}
				}
			}
			globalConstraint = globalConstraint.substring(0, globalConstraint.length()-OR.length());
		}
		if (!globalConstraint.equals("")) {
			constraints += "("+ globalConstraint +")" + AND;
		}

		if (constraints.equals("")) {
			return BASE;
		} else {
			return BASE + " AND " + constraints.substring(0, constraints.length()-AND.length());
		}
	}

	private static String buildFieldConstraints(String tokenClass, Map<String, List<String>> tokens) {
		List<String> ls = tokens.get(tokenClass);
		String messageField = SEARCH_PARAMETER_TO_MESSAGE_FIELD_MAP.get(tokenClass);
		String fieldConstraint = "";
		for (String value : ls) {
			if (!value.trim().equals("")) {
				fieldConstraint += sqlLikeConstraint(sqlLower(messageField), value.toLowerCase()) + OR;
			}
		}
		return fieldConstraint.substring(0, fieldConstraint.length()-OR.length());
	}

	private static String sqlLikeConstraint(String messageField, String value) {
		return messageField + " LIKE '%" + value + "%'";
	}

	private static String sqlLower(String messageField) {
		return "LOWER("+ messageField + ")";
	}

	@Override
	public String buildQuery(Map<String, List<String>> tokens, String lang) {
		if (lang.trim().equalsIgnoreCase(SQL2)) {
			return buildSQL2Query(tokens);
		} else {
			throw new IllegalArgumentException("Invalid lang argument!");
		}
	}

}
