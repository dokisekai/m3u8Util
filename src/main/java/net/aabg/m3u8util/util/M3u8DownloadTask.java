package net.aabg.m3u8util.util;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

public class M3u8DownloadTask implements Callable<Void> {
    private final String m3u8Url;
    private final ExecutorService executorService; // 用于媒体片段的下载任务
    private final Path downloadDir; // 下载目录

    public M3u8DownloadTask(String m3u8Url, ExecutorService executorService, Path downloadDir) {
        this.m3u8Url = m3u8Url;
        this.executorService = executorService;
        this.downloadDir = downloadDir;
    }

    /**
     * 从给定的URL中提取最后一个斜杠之前的内容
     *
     * @param url 给定的URL
     * @return 最后一个斜杠之前的内容，如果未找到斜杠则返回空字符串
     */
    public static String extractBeforeLastSlash(String url) {
        // 查找最后一个斜杠的索引
        int index = url.lastIndexOf('/');
        if (index != -1) {
            // 如果找到斜杠，则截取出之前的部分
            return url.substring(0, index + 1);
        }
        // 如果未找到斜杠，则返回空字符串
        return "";
    }
    @Override
    public Void call() throws Exception {
        String m3u8Content = HttpUtil.downloadContent(m3u8Url);
        String headLink = extractBeforeLastSlash(m3u8Url);
        List<String> mediaUrls = M3u8Parser.parseMediaFileRelativePaths(m3u8Content);
        EncryptionInfo encryptionInfo = M3u8Parser.parseEncryptionInfo(m3u8Content);

        List<Callable<Path>> downloadTasks = new ArrayList<>();
        Path tempDir = downloadDir.resolve("temp"); // 为每个片段创建临时目录
        Files.createDirectories(tempDir);
        for (String mediaUrl : mediaUrls) {
            downloadTasks.add(new MediaSegmentDownloader(headLink + mediaUrl, tempDir, encryptionInfo, 3)); // 假设最大重试次数为3
        }

        List<Future<Path>> results = executorService.invokeAll(downloadTasks);
        List<Path> downloadedSegments = new ArrayList<>();
        for (Future<Path> result : results) {
            downloadedSegments.add(result.get()); // 获取下载结果，此处简化错误处理
        }

        Path finalOutputPath = downloadDir.resolve("finalOutput.ts");
        FileMerger.mergeFiles(downloadedSegments, finalOutputPath);
        System.out.println("下载和合并完成: " + finalOutputPath);

        return null;
    }
}
