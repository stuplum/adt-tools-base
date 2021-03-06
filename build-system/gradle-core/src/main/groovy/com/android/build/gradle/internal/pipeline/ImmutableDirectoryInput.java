/*
 * Copyright (C) 2015 The Android Open Source Project
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

package com.android.build.gradle.internal.pipeline;

import com.android.annotations.NonNull;
import com.android.annotations.concurrency.Immutable;
import com.android.build.api.transform.DirectoryInput;
import com.android.build.api.transform.Status;
import com.google.common.base.Joiner;
import com.google.common.base.Objects;
import com.google.common.collect.ImmutableMap;

import java.io.File;
import java.util.Map;
import java.util.Set;

/**
 * Immutable version of {@link DirectoryInput}.
 */
@Immutable
class ImmutableDirectoryInput extends QualifiedContentImpl implements DirectoryInput {

    @NonNull
    private final Map<File, Status> changedFiles;

    ImmutableDirectoryInput(
            @NonNull String name,
            @NonNull File file,
            @NonNull Set<ContentType> contentTypes,
            @NonNull Set<Scope> scopes) {
        super(name, file, contentTypes, scopes);
        this.changedFiles = ImmutableMap.of();
    }

    protected ImmutableDirectoryInput(
            @NonNull String name,
            @NonNull File file,
            @NonNull Set<ContentType> contentTypes,
            @NonNull Set<Scope> scopes,
            @NonNull Map<File, Status> changedFiles) {
        super(name, file, contentTypes, scopes);
        this.changedFiles = ImmutableMap.copyOf(changedFiles);
    }

    @NonNull
    @Override
    public Map<File, Status> getChangedFiles() {
        return changedFiles;
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
                .add("name", getName())
                .add("file", getFile())
                .add("contentTypes", Joiner.on(',').join(getContentTypes()))
                .add("scopes", Joiner.on(',').join(getScopes()))
                .add("changedFiles", changedFiles)
                .toString();
    }
}