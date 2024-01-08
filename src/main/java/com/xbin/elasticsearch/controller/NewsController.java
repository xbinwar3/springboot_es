package com.xbin.elasticsearch.controller;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.xbin.elasticsearch.model.News;
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
@RequestMapping("/news")
public class NewsController {
    private RestHighLevelClient elasticsearchClient;

    public NewsController(RestHighLevelClient elasticsearchClient) {
        this.elasticsearchClient = elasticsearchClient;
    }

    @GetMapping("/tips")
    public Object autoComplete(String term) throws IOException {
        Request request = new Request("GET", "news/_search");

        request.setJsonEntity(String.format("{" +
                "  \"_source\": false, " +
                "  \"suggest\": {" +
                "    \"news_tags_suggest\": {" +
                "      \"prefix\": \"%s\"," +
                "      \"completion\": {" +
                "        \"field\": \"tags\"," +
                "        \"size\": 10," +
                "        \"skip_duplicates\": true" +
                "      }" +
                "    }" +
                "  }" +
                "}", term));

        Response response = elasticsearchClient.getLowLevelClient().performRequest(request);

        String jsonString = EntityUtils.toString(response.getEntity()); // "{\"age\": 10}"  {"age": 10}

        JSONObject jsonObject = JSONObject.parseObject(jsonString);

        JSONArray suggests = jsonObject.getJSONObject("suggest").getJSONArray("news_tags_suggest");

        JSONArray options = suggests.getJSONObject(0).getJSONArray("options");

        List<String> results = new ArrayList<>();
        for(int i = 0; i < options.size(); i++) {
            JSONObject opt = options.getJSONObject(i);
            results.add(opt.getString("text"));
        }

        return results;
    }

    @GetMapping("/search")
    public List<News> query(String text) throws Exception{
        /**
         * 1.对于高亮的数据，ES是抽取的一个个片段，然后将这些片段设置到一个数组中。
         * 2.对于有些数据，可能title或者content中没有高亮的字眼，那么我们就需要取原始数据的 title 和 content.
         */
        Request request = new Request("GET", "news/_search");
        request.setJsonEntity(String.format("{" +
                "  \"_source\": [\"url\", \"title\", \"content\"], " +
                "  \"query\": {" +
                "    \"multi_match\": {" +
                "      \"query\": \"%s\"," +
                "      \"fields\": [\"title\", \"content\"]" +
                "    }" +
                "  }," +
                "  \"highlight\": {" +
                "    \"pre_tags\": \"<span class='highLight'>\", " +
                "    \"post_tags\": \"</span>\", " +
                "    \"fields\": {" +
                "      \"title\": {}," +
                "      \"content\": {}" +
                "    }" +
                "  }" +
                "}", text));

        Response response = elasticsearchClient.getLowLevelClient().performRequest(request);

        JSONObject jsonObject = JSONObject.parseObject(EntityUtils.toString(response.getEntity()));

        JSONArray hits = jsonObject.getJSONObject("hits").getJSONArray("hits");

        List<News> results = new ArrayList<>();

        for (int i = 0; i < hits.size(); i++) {
            News news = new News();
            JSONObject hit = hits.getJSONObject(i);
            JSONObject highLight = hit.getJSONObject("highlight");  //获取高亮的数据结果

            JSONObject _source = hit.getJSONObject("_source"); //这个是原始的数据
            news.setUrl(_source.getString("url"));  //设置url

            JSONArray highLightTitle = highLight.getJSONArray("title");  //获取高亮的 title 数组
            JSONArray highLightContent = highLight.getJSONArray("content");

            if(null != highLightTitle) {
                StringBuffer highLightTitleStringBuffer = new StringBuffer();
                for (int j = 0; j < highLightTitle.size(); j++) {
                    String titleSegment = highLightTitle.getString(j);
                    highLightTitleStringBuffer.append(titleSegment);
                }
                news.setTitle(highLightTitleStringBuffer.toString());
            }else {  // 如果不存在高亮的数据，那么就取原始数据
                news.setTitle(_source.getString("title"));
            }

            if(null != highLightContent) {
                StringBuffer highLightContentStringBuffer = new StringBuffer();
                for (int j = 0; j < highLightContent.size(); j++) {
                    String contentSegment = highLightContent.getString(j);
                    highLightContentStringBuffer.append(contentSegment);
                }
                news.setContent(highLightContentStringBuffer.toString());
            }else {  // 如果不存在高亮的数据，那么就取原始数据
                news.setContent(_source.getString("content"));
            }

            results.add(news);
        }
        return results;
    }
}
