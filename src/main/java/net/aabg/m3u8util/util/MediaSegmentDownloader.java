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
                System.out.println("开始下载，尝试次数: " + attempt + "，URL: " + mediaUrl); // 添加打印语句
                byte[] mediaData = HttpUtil.downloadBytes(mediaUrl);

                if (encryptionInfo != null && encryptionInfo.isEncrypted) {
                    byte[] key = HttpUtil.downloadBytes(encryptionInfo.keyUri);
                    byte[] decryptedData = EncryptUtil.decryptAES(mediaData, key, new byte[16]);
                    Files.write(outputPath, decryptedData);
                } else {
                    Files.write(outputPath, mediaData);
                }

                System.out.println("成功下载，尝试次数: " + attempt + "，URL: " + mediaUrl); // 添加打印语句
                return outputPath;
            } catch (IOException e) {
                System.out.println("下载失败，尝试次数: " + attempt + "，URL: " + mediaUrl);
                if (attempt == retryAttempts) throw e;
            }
        }

        throw new IOException("下载失败，已达到最大重试次数: " + mediaUrl);
    }
}
