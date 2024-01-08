package com.xbin.elasticsearch.controller;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.http.util.EntityUtils;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/star")
public class StarController {

    private RestHighLevelClient elasticsearchClient;

    public StarController(RestHighLevelClient elasticsearchClient) {
        this.elasticsearchClient = elasticsearchClient;
    }

    @GetMapping
    public Object autoComplete(String term) throws IOException {
        Request request = new Request("GET", "stars/_search");

        request.setJsonEntity(String.format("{" +
                "  \"_source\": false," +
                "  \"suggest\": {" +
                "    \"star_name_suggest\": {" +
                "      \"prefix\": \"%s\"," +
                "      \"completion\": {" +
                "        \"field\": \"name\"," +
                "        \"size\": 10," +
                "        \"skip_duplicates\": true" +
                "      }" +
                "    }" +
                "  }" +
                "}", term));

        Response response = elasticsearchClient.getLowLevelClient().performRequest(request);

        String jsonString = EntityUtils.toString(response.getEntity()); // "{\"age\": 10}"  {"age": 10}

        JSONObject jsonObject = JSONObject.parseObject(jsonString);

        JSONArray suggests = jsonObject.getJSONObject("suggest").getJSONArray("star_name_suggest");

        JSONArray options = suggests.getJSONObject(0).getJSONArray("options");

        List<String> results = new ArrayList<>();
        for(int i = 0; i < options.size(); i++) {
            JSONObject opt = options.getJSONObject(i);
            results.add(opt.getString("text"));
        }

        return results;
    }
}
