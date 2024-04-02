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

    @Override
    public Void call() throws Exception {
        String m3u8Content = HttpUtil.downloadContent(m3u8Url);
        List<String> mediaUrls = M3u8Parser.parseMediaFileUrls(m3u8Content);
        EncryptionInfo encryptionInfo = M3u8Parser.parseEncryptionInfo(m3u8Content);

        List<Callable<Path>> downloadTasks = new ArrayList<>();
        for (int i = 0; i < mediaUrls.size(); i++) {
            Path tempDir = downloadDir.resolve("temp" + i); // 为每个片段创建临时目录
            Files.createDirectories(tempDir);
            String mediaUrl = mediaUrls.get(i);
            downloadTasks.add(new MediaSegmentDownloader(mediaUrl, tempDir, encryptionInfo, 3)); // 假设最大重试次数为3
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
