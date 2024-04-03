package net.aabg.m3u8util.util;
// HttpUtil.java: HTTP请求工具类，用于下载文本内容和字节数据。
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

public class HttpUtil {

    // HttpClient实例应该被重用来提高性能
    private static final HttpClient httpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_2) // 使用HTTP/2版本，如果服务器支持的话
            .build();

    /**
     * 下载文本内容
     *
     * @param url 要下载内容的URL
     * @return 下载的文本内容
     * @throws IOException          如果发生I/O错误
     * @throws InterruptedException 如果操作被中断
     */
    public static String downloadContent(String url) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET() // 显式地指定使用GET方法
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        return response.body();
    }

    /**
     * 下载字节数据
     *
     * @param url 要下载数据的URL
     * @return 下载的字节数据
     * @throws IOException          如果发生I/O错误
     * @throws InterruptedException 如果操作被中断
     */
    public static byte[] downloadBytes(String url) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();

        HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());

        return response.body();
    }
    public static String encodeURL(String url) {
        try {
            // 假设你的URL被分成两部分：前缀和需要编码的部分
            String prefix = url.substring(0, 54); // 修改为实际的分隔位置
            String partToEncode = url.substring(55); // 修改为实际的分隔位置
            String encodedPart = URLEncoder.encode(partToEncode, StandardCharsets.UTF_8.toString());
            return prefix + encodedPart;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
