package net.aabg.m3u8util.util;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Callable;

public class MediaSegmentDownloader implements Callable<Path> {
    private final String mediaUrl;
    private final Path downloadDir;
    private final EncryptionInfo encryptionInfo;
    private final int retryAttempts;

    public MediaSegmentDownloader(String mediaUrl, Path downloadDir, EncryptionInfo encryptionInfo, int retryAttempts) {
        this.mediaUrl = mediaUrl;
        this.downloadDir = downloadDir;
        this.encryptionInfo = encryptionInfo;
        this.retryAttempts = retryAttempts;
    }

    @Override
    public Path call() throws Exception {
        Path outputPath = downloadDir.resolve(URI.create(mediaUrl).getPath().replaceAll(".*/", ""));
        for (int attempt = 1; attempt <= retryAttempts; attempt++) {
            try {
                byte[] mediaData = HttpUtil.downloadBytes(mediaUrl);

                if (encryptionInfo != null && encryptionInfo.isEncrypted) {
                    // 假设EncryptUtil类已实现
                    byte[] key = HttpUtil.downloadBytes(encryptionInfo.keyUri);
                    // 这里简化处理，假设IV以某种方式提供或不需要
                    byte[] decryptedData = EncryptUtil.decryptAES(mediaData, key, new byte[16]);
                    Files.write(outputPath, decryptedData);
                } else {
                    Files.write(outputPath, mediaData);
                }

                return outputPath; // 成功下载并处理（如果需要解密，则解密），返回保存的文件路径
            } catch (IOException e) {
                System.out.println("下载失败，尝试次数: " + attempt + "，URL: " + mediaUrl);
                if (attempt == retryAttempts) throw e; // 在最后一次重试仍然失败时抛出异常
            }
        }

        throw new IOException("下载失败，已达到最大重试次数: " + mediaUrl);
    }
}
