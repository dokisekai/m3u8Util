package net.aabg.m3u8util.util;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class FileMerger {
    /**
     * 合并多个文件到一个文件中
     *
     * @param segmentPaths 包含要合并的文件路径的列表
     * @param outputPath 合并后文件的输出路径
     * @throws IOException 如果发生I/O错误
     */
    public static void mergeFiles(List<Path> segmentPaths, Path outputPath) throws IOException, FileNotFoundException {
        try (FileChannel outChannel = new FileOutputStream(outputPath.toFile(), true).getChannel()) {
            for (Path segmentPath : segmentPaths) {
                try (FileChannel inChannel = new FileInputStream(segmentPath.toFile()).getChannel()) {
                    inChannel.transferTo(0, inChannel.size(), outChannel);
                    Files.delete(segmentPath); // 合并后删除临时文件
                }
            }
        }
    }
}
