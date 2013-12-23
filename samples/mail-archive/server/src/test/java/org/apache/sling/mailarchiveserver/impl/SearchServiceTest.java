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

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class SearchServiceTest {

	private static SearchQueryParserImpl parser = new SearchQueryParserImpl();
	private static QueryBuilderImpl builder = new QueryBuilderImpl();

	private String searchPhrase;
	private String expectedQuery;

	@Parameters(name="{0}")
	public static Collection<Object[]> data() {
		List<Object[]> params = new ArrayList<Object[]>();
		params.add(new Object[] {"blank search field", "", QueryBuilderImpl.BASE } );

		params.add(new Object[] {"one word", "word", QueryBuilderImpl.BASE + " AND "
				+ "(LOWER(Body) LIKE '%word%' "
				+ "OR LOWER(Subject) LIKE '%word%' "
				+ "OR LOWER('List-Id') LIKE '%word%' "
				+ "OR LOWER(From) LIKE '%word%')" 
		} );	
		
		// TODO logically this is true, practically order by score because
		params.add(new Object[] {"two words", "hello word", QueryBuilderImpl.BASE + " AND " 
				+ "(LOWER(Body) LIKE '%hello%' "
				+ "OR LOWER(Body) LIKE '%word%' "
				+ "OR LOWER(Subject) LIKE '%hello%' "
				+ "OR LOWER(Subject) LIKE '%word%' "
				+ "OR LOWER('List-Id') LIKE '%hello%' "
				+ "OR LOWER('List-Id') LIKE '%word%' "
				+ "OR LOWER(From) LIKE '%hello%' " 
				+ "OR LOWER(From) LIKE '%word%')" 
		} );	

		params.add(new Object[] {"field search", "hello from:world", QueryBuilderImpl.BASE + " AND "
				+ "(LOWER(From) LIKE '%world%') "
				+ "AND (LOWER(Body) LIKE '%hello%' "
				+ "OR LOWER(Subject) LIKE '%hello%' "
				+ "OR LOWER('List-Id') LIKE '%hello%' "
				+ "OR LOWER(From) LIKE '%hello%')" 
		} );

		params.add(new Object[] {"caps", "SuBjecT:HeRE THeRe", QueryBuilderImpl.BASE + " AND "
				+ "(LOWER(Subject) LIKE '%here%') " 
				+ "AND (LOWER(Body) LIKE '%there%' "
				+ "OR LOWER(Subject) LIKE '%there%' "
				+ "OR LOWER('List-Id') LIKE '%there%' "
				+ "OR LOWER(From) LIKE '%there%')" 
		} );

		params.add(new Object[] {"non-existent field", "FROM:me list:public about:stuff", QueryBuilderImpl.BASE + " AND " 
				+ "(LOWER('List-Id') LIKE '%public%') "
				+ "AND (LOWER(From) LIKE '%me%')" 
		} );

		params.add(new Object[] {"just non-existent field", "frome:e", QueryBuilderImpl.DUMMY } );

		params.add(new Object[] {"two spaces (parsing)", "a  b", QueryBuilderImpl.BASE + " AND "
				+ "(LOWER(Body) LIKE '%a%' "
				+ "OR LOWER(Body) LIKE '%b%' "
				+ "OR LOWER(Subject) LIKE '%a%' "
				+ "OR LOWER(Subject) LIKE '%b%' "
				+ "OR LOWER('List-Id') LIKE '%a%' "
				+ "OR LOWER('List-Id') LIKE '%b%' "
				+ "OR LOWER(From) LIKE '%a%' " 
				+ "OR LOWER(From) LIKE '%b%')" 
		} );	

		params.add(new Object[] {"quoted text", "\"hel  wrd\"", QueryBuilderImpl.BASE + " AND "
				+ "(LOWER(Body) LIKE '%hel  wrd%' "
				+ "OR LOWER(Subject) LIKE '%hel  wrd%' "
				+ "OR LOWER('List-Id') LIKE '%hel  wrd%' "
				+ "OR LOWER(From) LIKE '%hel  wrd%')" 
		} );	
		
		params.add(new Object[] {"quoted field", "from:\"w r d\" hello ", QueryBuilderImpl.BASE + " AND "
				+ "(LOWER(From) LIKE '%w r d%') "
				+ "AND (LOWER(Body) LIKE '%hello%' "
				+ "OR LOWER(Subject) LIKE '%hello%' "
				+ "OR LOWER('List-Id') LIKE '%hello%' "
				+ "OR LOWER(From) LIKE '%hello%')" 
		} );

		//        params.add(new Object[] {"name", "", QueryBuilderImpl.BASE} );
		return params;
	}

	public SearchServiceTest(String description, String one, String two) {
		searchPhrase = one;
		expectedQuery = two;
	}

	@Test
	public void testQueryPipeline() {
		String query = builder.buildQuery(parser.parse(searchPhrase), QueryBuilderImpl.SQL2);
		assertTrue(String.format("\nExpected: %s\n   Output: %s", expectedQuery, query), expectedQuery.equals(query));
	}

}
