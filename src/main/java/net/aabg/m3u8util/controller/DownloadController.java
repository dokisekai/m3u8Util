package net.aabg.m3u8util.controller;

import lombok.extern.slf4j.Slf4j;
import net.aabg.m3u8util.util.M3u8DownloadTask;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static net.aabg.m3u8util.util.M3u8Downloader.isM3u8UrlAvailable;
@Slf4j
@RestController
public class DownloadController {

//    传入参数http://example.com/path/to/your/playlist1.m3u8,http://example.com/path/to/your/playlist2.m3u8";
    @GetMapping
    public String download(String urls) {
        urls = "http://example.com/path/to/your/playlist1.m3u8," +
                "http://example.com/path/to/your/playlist2.m3u8";
        // 假设你有一个或多个M3U8文件链接
//        List<String> m3u8Urls = List.of(
//                "http://example.com/path/to/your/playlist1.m3u8",
//                "http://example.com/path/to/your/playlist2.m3u8"
//                // 添加更多的M3U8文件链接
//        );
        List<String> m3u8Urls = Arrays.stream(urls.split(",")).toList();

        // 创建一个固定大小的线程池来处理下载任务
        ExecutorService executorService = Executors.newFixedThreadPool(10); // 适当选择线程池大小

        // 存储Future对象的列表，以便跟踪任务完成情况
        List<Future<Void>> futures = new ArrayList<>();

        // 循环提交每个M3u8下载任务到线程池
        for (String m3u8Url : m3u8Urls) {
            if (isM3u8UrlAvailable(m3u8Url)){
//                M3u8DownloadTask downloadTask = new M3u8DownloadTask(m3u8Url, executorService, Paths.get("你的文件路径"));
                M3u8DownloadTask downloadTask = new M3u8DownloadTask(m3u8Url,"fileName", executorService, Paths.get("/Users/snz/Documents"));
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
        return null;
    }
}
