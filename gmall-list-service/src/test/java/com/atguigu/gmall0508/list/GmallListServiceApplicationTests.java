package com.atguigu.gmall0508.list;

import io.searchbox.client.JestClient;
import io.searchbox.core.Search;
import io.searchbox.core.SearchResult;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;

@RunWith(SpringRunner.class)
@SpringBootTest
public class GmallListServiceApplicationTests {

	@Autowired
	private JestClient jestClient;

	@Test
	public void contextLoads() {
	}

	@Test
	public void testEs() throws IOException {
		String query="{\n" +
				"  \"query\": {\n" +
				"    \"match\": {\n" +
				"      \"actorList.name\": \"zheng jian\"\n" +
				"    }\n" +
				"  }\n" +
				"}";

		//查询
		Search search = new Search.Builder(query).addIndex("movie_index").addType("movie").build();


		SearchResult searchResult = jestClient.execute(search);
		List<SearchResult.Hit<HashMap, Void>> hits = searchResult.getHits(HashMap.class);
		for (SearchResult.Hit<HashMap, Void> hit : hits) {
			HashMap source = hit.source;
			Object name = source.get("name");
			System.out.println("==========="+name+"============");
		}
	}

}
