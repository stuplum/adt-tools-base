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

package com.android.build.gradle.integration.dependencies;

import static com.android.build.gradle.integration.common.utils.TestFileUtils.appendToFile;
import static com.android.build.gradle.integration.common.truth.TruthHelper.assertThat;
import static com.android.build.gradle.integration.common.truth.TruthHelper.assertThatApk;

import com.android.build.gradle.integration.common.fixture.GradleTestProject;
import com.android.build.gradle.integration.common.utils.ModelHelper;
import com.android.builder.model.AndroidLibrary;
import com.android.builder.model.AndroidProject;
import com.android.builder.model.Dependencies;
import com.android.builder.model.Variant;
import com.android.ide.common.process.ProcessException;
import com.google.common.collect.Iterables;
import com.google.common.truth.Truth;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;

/**
 * test for compile library in app
 */
public class AppWithCompileLibTest {

    @ClassRule
    public static GradleTestProject project = GradleTestProject.builder()
            .fromTestProject("projectWithModules")
            .create();
    static Map<String, AndroidProject> models;

    @BeforeClass
    public static void setUp() throws IOException {
        appendToFile(project.getSubproject("app").getBuildFile(),
                "\n" +
                "dependencies {\n" +
                "    compile project(\":library\")\n" +
                "}\n");
        models = project.executeAndReturnMultiModel("clean", ":app:assembleDebug");
    }

    @AfterClass
    public static void cleanUp() {
        project = null;
        models = null;
    }

    @Test
    public void checkCompiledLibraryIsPackaged() throws IOException, ProcessException {
        File apk = project.getSubproject("app").getApk("debug");

        assertThatApk(apk).containsClass("Lcom/example/android/multiproject/library/PersonView;");
    }

    @Test
    public void checkCompiledLibraryIsInTheModel() {
        Variant variant = ModelHelper.getVariant(models.get(":app").getVariants(), "debug");
        Truth.assertThat(variant).isNotNull();

        Dependencies deps = variant.getMainArtifact().getDependencies();
        Collection<AndroidLibrary> libraries = deps.getLibraries();
        assertThat(libraries).hasSize(1);
        assertThat(Iterables.getOnlyElement(libraries).getProject()).isEqualTo(":library");
    }

    @Test
    public void checkCompiledLibraryIsInTheAndroidTestDependency() {
        // TODO
    }

    @Test
    public void checkCompiledLibraryIsInTheUnitTestDependency() {
        // TODO
    }
}
