package net.aabg.m3u8util.util;

import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

@Slf4j
public class M3u8DownloadTask implements Callable<Void> {
    private final String m3u8Url;
    private final String fileName;
    private final ExecutorService executorService; // 用于媒体片段的下载任务
    private final Path downloadDir; // 下载目录

    public M3u8DownloadTask(String m3u8Url, String fileName, ExecutorService executorService, Path downloadDir) {
        this.m3u8Url = m3u8Url;
        this.executorService = executorService;
        this.downloadDir = downloadDir;
        this.fileName=fileName;
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
    public static String extractPathFromUrl(String url) throws URISyntaxException {
        URI uri = new URI(url);
        String path = uri.getPath();
        int lastSlashIndex = path.lastIndexOf('/');
        if (lastSlashIndex != -1) {
            path = path.substring(lastSlashIndex + 1); // 丢弃最后一个斜杠前面的内容
        }
        int dotIndex = path.indexOf('.');
        if (dotIndex != -1) {
            path = path.substring(0, dotIndex); // 丢弃"."后面的内容
        }
        return path;
    }
    @Override
    public Void call() throws Exception {
        String m3u8Content = HttpUtil.downloadContent(m3u8Url);
        String headLink = extractBeforeLastSlash(m3u8Url);
        List<String> mediaUrls = M3u8Parser.parseMediaFileRelativePaths(m3u8Content);
        EncryptionInfo encryptionInfo = M3u8Parser.parseEncryptionInfo(m3u8Content);

        List<Callable<Path>> downloadTasks = new ArrayList<>();

        String tempDirName = extractPathFromUrl(m3u8Url); // 使用URL的路径部分作为临时目录的名称
        String userHomeDir = System.getProperty("user.home");
        Path tempDir=null;
        if (fileName!=null){
            tempDir = Paths.get(userHomeDir, fileName);
        }else {
            tempDir = Paths.get(userHomeDir, tempDirName);
        }

        if (!Files.exists(tempDir)) {
            Files.createDirectories(tempDir);
        }

        int totalSegments = mediaUrls.size(); // 总片段数量
        int successCount = 0; // 成功数量
        int failCount = 0; // 失败数量
        //下载媒体片段
        for (String mediaUrl : mediaUrls) {
            downloadTasks.add(new MediaSegmentDownloader(headLink + mediaUrl, tempDir, encryptionInfo, 3, Path.of(tempDir + "/log.txt"))); // 假设最大重试次数为3
        }

        List<Future<Path>> results = executorService.invokeAll(downloadTasks);
        List<Path> downloadedSegments = new ArrayList<>();
        for (Future<Path> result : results) {
            try {
                downloadedSegments.add(result.get());// 获取下载结果，此处简化错误处理
                successCount++; // 如果成功，增加成功数量
            } catch (Exception e) {
                failCount++; // 如果失败，增加失败数量
            }
        }
        // 打印统计信息
        log.info("总片段数量: " + totalSegments);
        log.info("成功数量: " + successCount);
        log.info("失败数量: " + failCount);
        if (totalSegments==successCount){
            log.info("所有片段下载成功");
            Path finalOutputPath = downloadDir.resolve(tempDir+"/"+fileName+".ts");
            FileMerger.mergeFiles(downloadedSegments, finalOutputPath);
            log.info("下载和合并完成: " + finalOutputPath);
        } else {
            log.info("部分片段下载失败");
        }




        return null;
    }
}
