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

package com.android.repository.api;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.repository.impl.manager.LocalRepoLoader;

import java.io.File;

/**
 * An implementation of a local repository parser to use to try to identify a package if the normal
 * mechanism doesn't. If one is provided to RepoManager,
 * {@link #parseLegacyLocalPackage(File, ProgressIndicator)} will be run on every repository
 * directory that doesn't contain a package recognized by {@link LocalRepoLoader}
 * (or a child of such a directory). {@link LocalRepoLoader} will then use the {@link LocalPackage}
 * generated by this to write out a package.xml in the normal format.
 */
public interface FallbackLocalRepoLoader {

    /**
     * Try to find a package at the given location. If found, return a {@link LocalPackage} with the
     * package's information. Otherwise return {@code null}.
     */
    @Nullable
    LocalPackage parseLegacyLocalPackage(@NonNull File f, @NonNull ProgressIndicator progress);

    /**
     * Refreshes the loader's internal state if necessary. This should probably be done (by the repo
     * manager) whenever a new local repo load is started.
     */
    void refresh();
}