package net.aabg.m3u8util.util;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.concurrent.Callable;

import static net.aabg.m3u8util.util.M3u8DownloadTask.extractPathFromUrl;

public class MediaSegmentDownloader implements Callable<Path> {
    private final String mediaUrl;
    private final Path downloadDir;
    private final EncryptionInfo encryptionInfo;
    private final int retryAttempts;
    private final Path recordFile; // 下载记录文件的路径

    public MediaSegmentDownloader(String mediaUrl, Path downloadDir, EncryptionInfo encryptionInfo, int retryAttempts, Path recordFile) {
        this.mediaUrl = mediaUrl;
        this.downloadDir = downloadDir;
        this.encryptionInfo = encryptionInfo;
        this.retryAttempts = retryAttempts;
        this.recordFile = recordFile;
    }

    @Override
    public Path call() throws Exception {
        Path outputPath = downloadDir.resolve(URI.create(mediaUrl).getPath().replaceAll(".*/", ""));
        if (isDownloaded(mediaUrl)) { // 检查是否已经下载过
            System.out.println("已下载，跳过: " + mediaUrl);
            return outputPath;
        }

        for (int attempt = 1; attempt <= retryAttempts; attempt++) {
            try {
                System.out.println("开始下载，尝试次数: " + attempt + "，URL: " + mediaUrl);
                byte[] mediaData = HttpUtil.downloadBytes(mediaUrl);

                if (encryptionInfo != null && encryptionInfo.isEncrypted) {
                    byte[] key = HttpUtil.downloadBytes(encryptionInfo.keyUri);
                    byte[] decryptedData = EncryptUtil.decryptAES(mediaData, key, new byte[16]);
                    Files.write(outputPath, decryptedData);
                } else {
                    Files.write(outputPath, mediaData);
                }

                recordDownload(mediaUrl, outputPath.getFileName().toString()); // 记录下载信息
                System.out.println("成功下载，尝试次数: " + attempt + "，URL: " + mediaUrl);
                return outputPath;
            } catch (IOException e) {
                System.out.println("下载失败，尝试次数: " + attempt + "，URL: " + mediaUrl);
                Files.deleteIfExists(outputPath);
                if (attempt == retryAttempts) throw e;
            }
        }

        throw new IOException("下载失败，已达到最大重试次数: " + mediaUrl);
    }

    // 检查是否已经下载过
    private boolean isDownloaded(String mediaUrl) throws IOException {
        if (!Files.exists(recordFile)) {
            try {
                Files.createFile(recordFile);
                System.out.println("文件已创建: " + recordFile);
            } catch (IOException e) {
                System.err.println("创建文件失败: " + e.getMessage());
            }
        } else {
            System.out.println("文件已存在: " + recordFile);
        }

        List<String> lines = Files.readAllLines(recordFile);
        return lines.stream().anyMatch(line -> {
            try {
                return line.contains(extractPathFromUrl(mediaUrl));
            } catch (URISyntaxException e) {
                throw new RuntimeException(e);
            }
        });
    }

    // 记录下载信息
    private void recordDownload(String mediaUrl, String fileName) throws IOException {
        String record = "文件名: " + fileName + ", URL: " + mediaUrl + "\n";
        Files.write(recordFile, record.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }
}