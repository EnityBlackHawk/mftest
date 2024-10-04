package org.utfpr.mf.mftest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

public class WorkloadLoader {

    public static List<String> getSelects(String filepath) {
        String content;
        try {
            content = new String(Files.readAllBytes(Paths.get(filepath)));
        } catch (IOException e) {
            throw new RuntimeException("Error reading file: " + filepath);
        }

        return Arrays.stream(content.split("---")).toList();

    }


}
