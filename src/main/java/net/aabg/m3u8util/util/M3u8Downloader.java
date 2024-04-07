package net.aabg.m3u8util.util;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
@Slf4j
public class M3u8Downloader {

    public static void main(String[] args) {
        // 假设你有一个或多个M3U8文件链接
        List<String> m3u8Urls = List.of(
                "http://example.com/path/to/your/playlist1.m3u8",
                "http://example.com/path/to/your/playlist2.m3u8"
                // 添加更多的M3U8文件链接
        );

        // 创建一个固定大小的线程池来处理下载任务
        ExecutorService executorService = Executors.newFixedThreadPool(10); // 适当选择线程池大小

        // 存储Future对象的列表，以便跟踪任务完成情况
        List<Future<Void>> futures = new ArrayList<>();

        // 循环提交每个M3u8下载任务到线程池
        for (String m3u8Url : m3u8Urls) {
            if (isM3u8UrlAvailable(m3u8Url)){
//                M3u8DownloadTask downloadTask = new M3u8DownloadTask(m3u8Url, executorService, Paths.get("你的文件路径"));
                M3u8DownloadTask downloadTask = new M3u8DownloadTask(m3u8Url, executorService, Paths.get("/Users/snz/Documents"));
                futures.add(executorService.submit(downloadTask));
            }else {
                log.error("所提供的网络路径不可用");
            }
//            encodeURL

        }

        // 等待所有下载任务完成
        futures.forEach(future -> {
            try {
                future.get(); // 这里会阻塞，直到任务完成或者抛出异常
            } catch (Exception e) {
                System.err.println("下载任务执行出错: " + e.getMessage());
            }
        });

        // 所有任务完成后，关闭线程池
        executorService.shutdown();
        System.out.println("所有下载任务完成。");
    }



    public static boolean isM3u8UrlAvailable(String m3u8Url) {
        HttpClient httpClient = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(m3u8Url))
                .method("HEAD", HttpRequest.BodyPublishers.noBody()) // 使用HEAD方法来最小化响应数据量
                .build();

        try {
            HttpResponse<Void> response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
            int statusCode = response.statusCode();
            // 通常，状态码200表示资源可用，但也可能需要检查其他状态码如 302（重定向）
            return statusCode == 200;
        } catch (IOException | InterruptedException e) {
            System.err.println("Error checking M3U8 URL: " + e.getMessage());
            Thread.currentThread().interrupt(); // 处理InterruptedException时重设中断状态
        }

        return false;
    }
}
