/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.build.gradle.integration.common.utils;

import static com.android.build.gradle.integration.common.truth.TruthHelper.assertThat;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.google.common.base.Predicate;
import com.google.common.collect.Lists;
import com.google.common.io.Files;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.List;

/**
 * Helper to help verify content of a file.
 */
public class TestFileUtils {

    /**
     * Return a list of relative path of the files in a directory.
     */
    public static List<String> listFiles(@NonNull File base) {
        assertThat(base).isDirectory();

        List<String> fileList = Lists.newArrayList();
        for (File file : Files.fileTreeTraverser().preOrderTraversal(base).filter(
                new Predicate<File>() {
                    @Override
                    public boolean apply(@Nullable File file) {
                        // we want to skip directories and symlinks, so isFile is the best check.
                        return file != null && file.isFile();
                    }
                })) {
            assertThat(file.toString()).startsWith(base.toString());
            String fileName = file.toString().substring(base.toString().length());
            fileList.add(fileName);

        }
        return fileList;
    }

    public static void checkContent(File file, String expectedContent) throws IOException {
        checkContent(file, Collections.singleton(expectedContent));
    }

    public static void checkContent(File file, Iterable<String> expectedContents) throws IOException {
        assertThat(file).isFile();

        String contents = Files.toString(file, Charsets.UTF_8);
        for (String expectedContent : expectedContents) {
            assertTrue("File '" + file.getAbsolutePath() + "' does not contain: " + expectedContent,
                    contents.contains(expectedContent));
        }
    }

    public static void searchAndReplace(
            @NonNull File file,
            @NonNull String search,
            @NonNull String replace) throws IOException {
        String content = Files.toString(file, Charset.defaultCharset());
        String newContent = content.replaceAll(search, replace);
        assertNotEquals("No match in file.", content, newContent);
        Files.write(newContent, file, Charset.defaultCharset());
    }

    /**
     * Replace a line from a file with another line.
     * @param file file to change
     * @param lineNumber the line number, starting at 1
     * @param line the line to replace with
     * @throws IOException
     */
    public static void replaceLine(
            @NonNull  File file,
            int lineNumber,
            @NonNull String line) throws IOException {
        List<String> lines = Files.readLines(file, Charsets.UTF_8);

        lines.add(lineNumber, line);
        lines.remove(lineNumber - 1);

        Files.write(
                Joiner.on(System.getProperty("line.separator")).join(lines),
                file,
                Charsets.UTF_8);
    }

    public static void addMethod(
            @NonNull File javaFile,
            @NonNull String methodCode) throws IOException {
        // Put the method code before the last closing brace.
        searchAndReplace(javaFile, "\n}\\s+$", methodCode + "\n\n}");
    }

    public static void appendToFile(@NonNull File file, @NonNull String content) throws IOException {
        Files.append(content, file, Charset.defaultCharset());
    }
}
