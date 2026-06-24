package com.yry.blog.myblogarticle;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.InfoResponse;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.elasticsearch.client.RestClient;
import java.io.IOException;

public class ElasticsearchTest {

    public static void main(String[] args) throws IOException {
        // --- 1. 设置连接信息 ---
        // 替换为你的 Elasticsearch 地址和端口，默认通常是 localhost:9200
        String esHost = "localhost";
        int esPort = 9200; // 通常 REST API 端口是 9200
        String scheme = "http"; // 如果 ES 启用了 HTTPS，请改为 "https"

        // 如果 ES 启用了安全认证，需要提供用户名和密码
        String username = "elastic"; // 替换为你的用户名
        String password = "YOUR_ES_PASSWORD_HERE"; // 必须替换为你的 elastic 用户的实际密码！

        // --- 2. 创建 HTTP 客户端 (使用 Apache HttpComponents) ---
        BasicCredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        credentialsProvider.setCredentials(AuthScope.ANY,
                new UsernamePasswordCredentials(username, password));

        RestClient restClient = RestClient.builder(new HttpHost(esHost, esPort, scheme))
                .setHttpClientConfigCallback(httpClientBuilder -> httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider))
                .build();

        // --- 3. 创建传输层 ---
        ElasticsearchTransport transport = new RestClientTransport(restClient, new JacksonJsonpMapper());

        // --- 4. 创建 API 客户端实例 ---
        ElasticsearchClient client = new ElasticsearchClient(transport);

        try {
            // --- 5. 执行测试操作 ---

            // 示例 1: 获取 ES 服务器信息
            System.out.println("--- 获取 ES 服务器信息 ---");
            InfoResponse infoResponse = client.info();
            System.out.println("Cluster Name: " + infoResponse.clusterName());
            System.out.println("Node Name: " + infoResponse.name());
            System.out.println("Version Number: " + infoResponse.version().number());
            System.out.println("-------------------------\n");

            // 示例 2: 执行一个简单的搜索 (搜索所有索引中的所有文档)
            System.out.println("--- 执行简单搜索 (搜索所有) ---");
            SearchRequest searchRequest = SearchRequest.of(s -> s
                    .query(q -> q
                            .matchAll(ma -> ma) // 匹配所有文档
                    )
                    .size(10) // 返回最多 10 个结果
            );

            SearchResponse<Object> searchResponse = client.search(searchRequest, Object.class);
            System.out.println("Total hits: " + searchResponse.hits().total().value());

            for (Hit<Object> hit : searchResponse.hits().hits()) {
                System.out.println("Index: " + hit.index() + ", ID: " + hit.id());
                // 注意：因为我们反序列化为 Object，所以 _source 不能直接访问具体字段
                // 如果要访问具体字段，需要定义 POJO 类
                System.out.println("Source: " + hit.source()); // 输出原始 JSON 字符串
            }
            System.out.println("-------------------------");

        } catch (IOException e) {
            System.err.println("与 Elasticsearch 通信时发生错误: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // --- 6. 关闭客户端和底层 HTTP 连接 ---
            if (client != null) {
                try {
                    transport.close();
                    restClient.close();
                } catch (IOException e) {
                    System.err.println("关闭客户端时发生错误: " + e.getMessage());
                }
            }
        }
    }
}
