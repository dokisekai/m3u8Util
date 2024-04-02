package net.aabg.m3u8util.util;

// M3u8Parser.java: M3U8解析类，用于解析M3U8文件内容，提取媒体文件URLs和加密信息。
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class M3u8Parser {

    // 用于匹配媒体片段URL的简单正则表达式
    private static final Pattern MEDIA_SEGMENT_PATTERN = Pattern.compile("https?://.*");

    // 用于匹配EXT-X-KEY标签的正则表达式，提取加密方法和密钥URL
    private static final Pattern ENCRYPTION_PATTERN = Pattern.compile("#EXT-X-KEY:METHOD=([^,]+),URI=\"([^\"]+)\"");

    /**
     * 解析m3u8内容，提取媒体文件URLs
     *
     * @param m3u8Content m3u8文件的文本内容
     * @return 包含所有媒体文件URL的列表
     */
    public static List<String> parseMediaFileUrls(String m3u8Content) {
        List<String> mediaUrls = new ArrayList<>();
        Matcher matcher = MEDIA_SEGMENT_PATTERN.matcher(m3u8Content);
        while (matcher.find()) {
            mediaUrls.add(matcher.group());
        }
        return mediaUrls;
    }

    /**
     * 解析m3u8内容，提取加密信息
     *
     * @param m3u8Content m3u8文件的文本内容
     * @return 加密信息，如果存在的话
     */
    public static EncryptionInfo parseEncryptionInfo(String m3u8Content) {
        Matcher matcher = ENCRYPTION_PATTERN.matcher(m3u8Content);
        if (matcher.find()) {
            EncryptionInfo encryptionInfo = new EncryptionInfo();
            encryptionInfo.isEncrypted = true;
            encryptionInfo.method = matcher.group(1);
            encryptionInfo.keyUri = matcher.group(2);
            // 这里简化处理，没有提取IV，实际使用中可能需要根据实际情况进行扩展
            return encryptionInfo;
        }
        return null; // 表示内容未加密
    }
}

