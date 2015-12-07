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

package com.android.build.gradle.shrinker;

import static com.google.common.truth.Truth.assertThat;

import com.android.annotations.NonNull;
import com.android.build.api.transform.TransformInput;
import com.android.ide.common.internal.WaitableExecutor;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.io.Files;

import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;

/**
 * Tests for {@link FullRunShrinker}.
 */
public class FullRunShrinkerTest extends AbstractShrinkerTest {

    private FullRunShrinker<String> mShrinker;

    @Before
    public void createShrinker() throws Exception {
        mShrinker = new FullRunShrinker<String>(
                new WaitableExecutor<Void>(),
                buildGraph(),
                getPlatformJars(),
                mShrinkerLogger);
    }

    @NonNull
    protected ShrinkerGraph<String> buildGraph() throws IOException {
        return JavaSerializationShrinkerGraph.empty(mIncrementalDir);
    }

    @Test
    public void simple_oneClass() throws Exception {
        // Given:
        Files.write(TestClasses.SimpleScenario.aaa(), new File(mTestPackageDir, "Aaa.class"));

        // When:
        run("Aaa", "aaa:()V");

        // Then:
        assertMembersLeft("Aaa", "aaa:()V", "bbb:()V");
    }

    @Test
    public void simple_threeClasses() throws Exception {
        // Given:
        Files.write(TestClasses.SimpleScenario.aaa(), new File(mTestPackageDir, "Aaa.class"));
        Files.write(TestClasses.SimpleScenario.bbb(), new File(mTestPackageDir, "Bbb.class"));
        Files.write(TestClasses.SimpleScenario.ccc(), new File(mTestPackageDir, "Ccc.class"));

        // When:
        run("Bbb", "bbb:(Ltest/Aaa;)V");

        // Then:
        assertMembersLeft("Aaa", "aaa:()V", "bbb:()V");
        assertMembersLeft("Bbb", "bbb:(Ltest/Aaa;)V");
        assertClassSkipped("Ccc");
    }

    @Test
    public void virtualCalls_keepEntryPointsSuperclass() throws Exception {
        // Given:
        Files.write(TestClasses.VirtualCalls.abstractClass(), new File(mTestPackageDir, "AbstractClass.class"));
        Files.write(TestClasses.VirtualCalls.impl(1), new File(mTestPackageDir, "Impl1.class"));

        // When:
        run("Impl1", "abstractMethod:()V");

        // Then:
        assertMembersLeft("Impl1", "abstractMethod:()V");
        assertMembersLeft("AbstractClass");
    }

    @Test
    public void virtualCalls_abstractType() throws Exception {
        // Given:
        Files.write(
                TestClasses.VirtualCalls.main_abstractType(), new File(mTestPackageDir, "Main.class"));
        Files.write(TestClasses.VirtualCalls.abstractClass(), new File(mTestPackageDir, "AbstractClass.class"));
        Files.write(TestClasses.VirtualCalls.impl(1), new File(mTestPackageDir, "Impl1.class"));
        Files.write(TestClasses.VirtualCalls.impl(2), new File(mTestPackageDir, "Impl2.class"));
        Files.write(TestClasses.VirtualCalls.impl(3), new File(mTestPackageDir, "Impl3.class"));

        // When:
        run("Main", "main:([Ljava/lang/String;)V");

        // Then:
        assertMembersLeft("Main", "main:([Ljava/lang/String;)V");
        assertClassSkipped("Impl3");
        assertMembersLeft("AbstractClass", "abstractMethod:()V", "<init>:()V");
        assertMembersLeft("Impl1", "abstractMethod:()V", "<init>:()V");
        assertMembersLeft("Impl2", "abstractMethod:()V", "<init>:()V");
    }

    @Test
    public void virtualCalls_concreteType() throws Exception {
        // Given:
        Files.write(
                TestClasses.VirtualCalls.main_concreteType(), new File(mTestPackageDir, "Main.class"));
        Files.write(TestClasses.VirtualCalls.abstractClass(), new File(mTestPackageDir, "AbstractClass.class"));
        Files.write(TestClasses.VirtualCalls.impl(1), new File(mTestPackageDir, "Impl1.class"));
        Files.write(TestClasses.VirtualCalls.impl(2), new File(mTestPackageDir, "Impl2.class"));
        Files.write(TestClasses.VirtualCalls.impl(3), new File(mTestPackageDir, "Impl3.class"));

        // When:
        run("Main", "main:([Ljava/lang/String;)V");

        // Then:
        assertMembersLeft("Main", "main:([Ljava/lang/String;)V");
        assertClassSkipped("Impl3");
        assertMembersLeft("AbstractClass", "<init>:()V");
        assertMembersLeft("Impl1", "abstractMethod:()V", "<init>:()V");
        assertMembersLeft("Impl2", "<init>:()V");
    }

    @Test
    public void virtualCalls_methodFromParent() throws Exception {
        // Given:
        Files.write(
                TestClasses.VirtualCalls.main_parentChild(), new File(mTestPackageDir, "Main.class"));
        Files.write(TestClasses.VirtualCalls.parent(), new File(mTestPackageDir, "Parent.class"));
        Files.write(TestClasses.VirtualCalls.child(), new File(mTestPackageDir, "Child.class"));

        // When:
        run("Main", "main:()V");

        // Then:
        assertMembersLeft("Main", "main:()V");
        assertMembersLeft("Parent", "<init>:()V", "onlyInParent:()V");
        assertMembersLeft("Child", "<init>:()V");
    }

    @Test
    public void sdkTypes_methodsFromJavaClasses() throws Exception {
        // Given:
        Files.write(TestClasses.SdkTypes.main(), new File(mTestPackageDir, "Main.class"));
        Files.write(
                TestClasses.SdkTypes.myException(), new File(mTestPackageDir, "MyException.class"));

        // When:
        run("Main", "main:([Ljava/lang/String;)V");

        // Then:
        assertMembersLeft("Main", "main:([Ljava/lang/String;)V");
        assertMembersLeft(
                "MyException",
                "<init>:()V",
                "hashCode:()I",
                "getMessage:()Ljava/lang/String;");
    }

    @Test
    public void interfaces_sdkInterface_classUsed_abstractType() throws Exception {
        // Given:
        Files.write(TestClasses.Interfaces.main(), new File(mTestPackageDir, "Main.class"));
        Files.write(TestClasses.Interfaces.myCharSequence(), new File(mTestPackageDir, "MyCharSequence.class"));
        Files.write(
                TestClasses.Interfaces.myInterface(), new File(mTestPackageDir, "MyInterface.class"));
        Files.write(TestClasses.Interfaces.myImpl(), new File(mTestPackageDir, "MyImpl.class"));
        Files.write(TestClasses.Interfaces.doesSomething(), new File(mTestPackageDir, "DoesSomething.class"));
        Files.write(
                TestClasses.Interfaces.implementationFromSuperclass(),
                new File(mTestPackageDir, "ImplementationFromSuperclass.class"));

        // When:
        run(
                "Main",
                "buildMyCharSequence:()Ltest/MyCharSequence;",
                "callCharSequence:(Ljava/lang/CharSequence;)V");

        // Then:
        assertMembersLeft(
                "Main",
                "buildMyCharSequence:()Ltest/MyCharSequence;",
                "callCharSequence:(Ljava/lang/CharSequence;)V");
        assertMembersLeft(
                "MyCharSequence",
                "subSequence:(II)Ljava/lang/CharSequence;",
                "charAt:(I)C",
                "length:()I",
                "<init>:()V");
        assertClassSkipped("MyInterface");
        assertClassSkipped("MyImpl");

        assertImplements("MyCharSequence", "java/lang/CharSequence");
    }

    @Test
    public void interfaces_sdkInterface_classUsed_concreteType() throws Exception {
        // Given:
        Files.write(TestClasses.Interfaces.main(), new File(mTestPackageDir, "Main.class"));
        Files.write(TestClasses.Interfaces.myCharSequence(), new File(mTestPackageDir, "MyCharSequence.class"));
        Files.write(
                TestClasses.Interfaces.myInterface(), new File(mTestPackageDir, "MyInterface.class"));
        Files.write(TestClasses.Interfaces.myImpl(), new File(mTestPackageDir, "MyImpl.class"));
        Files.write(TestClasses.Interfaces.doesSomething(), new File(mTestPackageDir, "DoesSomething.class"));
        Files.write(
                TestClasses.Interfaces.implementationFromSuperclass(),
                new File(mTestPackageDir, "ImplementationFromSuperclass.class"));

        // When:
        run(
                "Main",
                "buildMyCharSequence:()Ltest/MyCharSequence;",
                "callMyCharSequence:(Ltest/MyCharSequence;)V");

        // Then:
        assertMembersLeft(
                "Main",
                "buildMyCharSequence:()Ltest/MyCharSequence;",
                "callMyCharSequence:(Ltest/MyCharSequence;)V");
        assertMembersLeft(
                "MyCharSequence",
                "subSequence:(II)Ljava/lang/CharSequence;",
                "charAt:(I)C",
                "length:()I",
                "<init>:()V");
        assertClassSkipped("MyInterface");
        assertClassSkipped("MyImpl");

        assertImplements("MyCharSequence", "java/lang/CharSequence");
    }

    @Test
    public void interfaces_implementationFromSuperclass() throws Exception {
        // Given:
        Files.write(TestClasses.Interfaces.main(), new File(mTestPackageDir, "Main.class"));
        Files.write(
                TestClasses.Interfaces.myInterface(), new File(mTestPackageDir, "MyInterface.class"));
        Files.write(TestClasses.Interfaces.myCharSequence(), new File(mTestPackageDir, "MyCharSequence.class"));
        Files.write(TestClasses.Interfaces.myImpl(), new File(mTestPackageDir, "MyImpl.class"));
        Files.write(TestClasses.Interfaces.doesSomething(), new File(mTestPackageDir, "DoesSomething.class"));
        Files.write(
                TestClasses.Interfaces.implementationFromSuperclass(),
                new File(mTestPackageDir, "ImplementationFromSuperclass.class"));

        // When:
        run(
                "Main",
                "useImplementationFromSuperclass:(Ltest/ImplementationFromSuperclass;)V",
                "useMyInterface:(Ltest/MyInterface;)V");

        // Then:
        assertMembersLeft(
                "Main",
                "useImplementationFromSuperclass:(Ltest/ImplementationFromSuperclass;)V",
                "useMyInterface:(Ltest/MyInterface;)V");
        assertMembersLeft("ImplementationFromSuperclass");
        assertMembersLeft(
                "MyInterface",
                "doSomething:(Ljava/lang/Object;)V");
        assertClassSkipped("MyImpl");
        assertClassSkipped("MyCharSequence");

        // This is the tricky part: this method should be kept, because a subclass is using it to
        // implement an interface.
        assertMembersLeft(
                "DoesSomething",
                "doSomething:(Ljava/lang/Object;)V");

        assertImplements("ImplementationFromSuperclass", "test/MyInterface");
    }

    @Test
    public void interfaces_sdkInterface_classNotUsed() throws Exception {
        // Given:
        Files.write(TestClasses.Interfaces.main(), new File(mTestPackageDir, "Main.class"));
        Files.write(TestClasses.Interfaces.myCharSequence(), new File(mTestPackageDir, "MyCharSequence.class"));
        Files.write(
                TestClasses.Interfaces.myInterface(), new File(mTestPackageDir, "MyInterface.class"));
        Files.write(TestClasses.Interfaces.myImpl(), new File(mTestPackageDir, "MyImpl.class"));
        Files.write(TestClasses.Interfaces.doesSomething(), new File(mTestPackageDir, "DoesSomething.class"));
        Files.write(
                TestClasses.Interfaces.implementationFromSuperclass(),
                new File(mTestPackageDir, "ImplementationFromSuperclass.class"));

        // When:
        run(
                "Main",
                "callCharSequence:(Ljava/lang/CharSequence;)V");

        // Then:
        assertMembersLeft(
                "Main",
                "callCharSequence:(Ljava/lang/CharSequence;)V");
        assertClassSkipped("MyCharSequence");
        assertClassSkipped("MyInterface");
        assertClassSkipped("MyImpl");
    }

    @Test
    public void interfaces_appInterface_abstractType() throws Exception {
        // Given:
        Files.write(TestClasses.Interfaces.main(), new File(mTestPackageDir, "Main.class"));
        Files.write(TestClasses.Interfaces.myCharSequence(), new File(mTestPackageDir, "MyCharSequence.class"));
        Files.write(
                TestClasses.Interfaces.myInterface(), new File(mTestPackageDir, "MyInterface.class"));
        Files.write(TestClasses.Interfaces.myImpl(), new File(mTestPackageDir, "MyImpl.class"));
        Files.write(TestClasses.Interfaces.doesSomething(), new File(mTestPackageDir, "DoesSomething.class"));
        Files.write(
                TestClasses.Interfaces.implementationFromSuperclass(),
                new File(mTestPackageDir, "ImplementationFromSuperclass.class"));

        // When:
        run(
                "Main",
                "buildMyImpl:()Ltest/MyImpl;",
                "useMyInterface:(Ltest/MyInterface;)V");

        // Then:
        assertMembersLeft(
                "Main",
                "buildMyImpl:()Ltest/MyImpl;",
                "useMyInterface:(Ltest/MyInterface;)V");
        assertMembersLeft(
                "MyInterface",
                "doSomething:(Ljava/lang/Object;)V");
        assertMembersLeft(
                "MyImpl",
                "<init>:()V",
                "doSomething:(Ljava/lang/Object;)V");
        assertClassSkipped("MyCharSequence");

        assertImplements("MyImpl", "test/MyInterface");
    }

    @Test
    public void interfaces_appInterface_concreteType() throws Exception {
        // Given:
        Files.write(TestClasses.Interfaces.main(), new File(mTestPackageDir, "Main.class"));
        Files.write(TestClasses.Interfaces.myCharSequence(), new File(mTestPackageDir, "MyCharSequence.class"));
        Files.write(
                TestClasses.Interfaces.myInterface(), new File(mTestPackageDir, "MyInterface.class"));
        Files.write(TestClasses.Interfaces.myImpl(), new File(mTestPackageDir, "MyImpl.class"));
        Files.write(TestClasses.Interfaces.doesSomething(), new File(mTestPackageDir, "DoesSomething.class"));
        Files.write(
                TestClasses.Interfaces.implementationFromSuperclass(),
                new File(mTestPackageDir, "ImplementationFromSuperclass.class"));

        // When:
        run("Main", "useMyImpl_interfaceMethod:(Ltest/MyImpl;)V");

        // Then:
        assertMembersLeft(
                "Main",
                "useMyImpl_interfaceMethod:(Ltest/MyImpl;)V");
        assertClassSkipped("MyInterface");
        assertMembersLeft("MyImpl", "doSomething:(Ljava/lang/Object;)V");
        assertClassSkipped("MyCharSequence");

        assertDoesntImplement("MyImpl", "test/MyInterface");
    }

    @Test
    public void interfaces_appInterface_interfaceNotUsed() throws Exception {
        // Given:
        Files.write(TestClasses.Interfaces.main(), new File(mTestPackageDir, "Main.class"));
        Files.write(TestClasses.Interfaces.myCharSequence(), new File(mTestPackageDir, "MyCharSequence.class"));
        Files.write(
                TestClasses.Interfaces.myInterface(), new File(mTestPackageDir, "MyInterface.class"));
        Files.write(TestClasses.Interfaces.myImpl(), new File(mTestPackageDir, "MyImpl.class"));
        Files.write(TestClasses.Interfaces.doesSomething(), new File(mTestPackageDir, "DoesSomething.class"));
        Files.write(
                TestClasses.Interfaces.implementationFromSuperclass(),
                new File(mTestPackageDir, "ImplementationFromSuperclass.class"));

        // When:
        run("Main", "useMyImpl_otherMethod:(Ltest/MyImpl;)V");

        // Then:
        assertMembersLeft(
                "Main",
                "useMyImpl_otherMethod:(Ltest/MyImpl;)V");
        assertClassSkipped("MyInterface");
        assertMembersLeft(
                "MyImpl",
                "someOtherMethod:()V");
        assertClassSkipped("MyCharSequence");

        assertDoesntImplement("MyImpl", "test/MyInterface");
    }

    @Test
    public void fields() throws Exception {
        // Given:
        Files.write(TestClasses.Fields.main(), new File(mTestPackageDir, "Main.class"));
        Files.write(TestClasses.Fields.myFields(), new File(mTestPackageDir, "MyFields.class"));
        Files.write(TestClasses.Fields.myFieldsSubclass(), new File(mTestPackageDir, "MyFieldsSubclass.class"));
        Files.write(TestClasses.emptyClass("MyFieldType"), new File(mTestPackageDir, "MyFieldType.class"));

        // When:
        run("Main", "main:()I");

        // Then:
        assertMembersLeft(
                "Main",
                "main:()I");
        assertMembersLeft(
                "MyFields",
                "<init>:()V",
                "<clinit>:()V",
                "readField:()I",
                "f1:I",
                "f2:I",
                "f4:Ltest/MyFieldType;",
                "sString:Ljava/lang/String;");
        assertMembersLeft("MyFieldType");
    }

    @Test
    public void fields_fromSuperclass() throws Exception {
        // Given:
        Files.write(TestClasses.Fields.main(), new File(mTestPackageDir, "Main.class"));
        Files.write(TestClasses.Fields.myFields(), new File(mTestPackageDir, "MyFields.class"));
        Files.write(TestClasses.Fields.myFieldsSubclass(), new File(mTestPackageDir, "MyFieldsSubclass.class"));
        Files.write(TestClasses.emptyClass("MyFieldType"), new File(mTestPackageDir, "MyFieldType.class"));

        // When:
        run("Main", "main_subclass:()I");

        // Then:
        assertMembersLeft(
                "Main",
                "main_subclass:()I");
        assertMembersLeft(
                "MyFields",
                "<init>:()V",
                "<clinit>:()V",
                "f1:I",
                "f2:I",
                "sString:Ljava/lang/String;");
        assertMembersLeft(
                "MyFieldsSubclass",
                "<init>:()V");
        assertClassSkipped("MyFieldType");
    }

    @Test
    public void overrides_methodNotUsed() throws Exception {
        // Given:
        Files.write(
                TestClasses.MultipleOverriddenMethods.main(), new File(mTestPackageDir, "Main.class"));
        Files.write(TestClasses.MultipleOverriddenMethods.interfaceOne(), new File(mTestPackageDir, "InterfaceOne.class"));
        Files.write(TestClasses.MultipleOverriddenMethods.interfaceTwo(), new File(mTestPackageDir, "InterfaceTwo.class"));
        Files.write(TestClasses.MultipleOverriddenMethods.implementation(), new File(mTestPackageDir, "Implementation.class"));

        // When:
        run("Main", "buildImplementation:()V");

        // Then:
        assertMembersLeft(
                "Main",
                "buildImplementation:()V");
        assertClassSkipped("InterfaceOne");
        assertClassSkipped("InterfaceTwo");
        assertMembersLeft("Implementation", "<init>:()V");
    }

    @Test
    public void overrides_classNotUsed() throws Exception {
        // Given:
        Files.write(
                TestClasses.MultipleOverriddenMethods.main(), new File(mTestPackageDir, "Main.class"));
        Files.write(TestClasses.MultipleOverriddenMethods.interfaceOne(), new File(mTestPackageDir, "InterfaceOne.class"));
        Files.write(TestClasses.MultipleOverriddenMethods.interfaceTwo(), new File(mTestPackageDir, "InterfaceTwo.class"));
        Files.write(TestClasses.MultipleOverriddenMethods.implementation(), new File(mTestPackageDir, "Implementation.class"));

        // When:
        run(
                "Main",
                "useInterfaceOne:(Ltest/InterfaceOne;)V",
                "useInterfaceTwo:(Ltest/InterfaceTwo;)V");

        // Then:
        assertMembersLeft(
                "Main",
                "useInterfaceOne:(Ltest/InterfaceOne;)V",
                "useInterfaceTwo:(Ltest/InterfaceTwo;)V");
        assertMembersLeft("InterfaceOne", "m:()V");
        assertMembersLeft("InterfaceTwo", "m:()V");
        assertClassSkipped("MyImplementation");
    }

    @Test
    public void overrides_interfaceOneUsed_classUsed() throws Exception {
        // Given:
        Files.write(
                TestClasses.MultipleOverriddenMethods.main(), new File(mTestPackageDir, "Main.class"));
        Files.write(TestClasses.MultipleOverriddenMethods.interfaceOne(), new File(mTestPackageDir, "InterfaceOne.class"));
        Files.write(TestClasses.MultipleOverriddenMethods.interfaceTwo(), new File(mTestPackageDir, "InterfaceTwo.class"));
        Files.write(TestClasses.MultipleOverriddenMethods.implementation(), new File(mTestPackageDir, "Implementation.class"));

        // When:
        run(
                "Main",
                "useInterfaceOne:(Ltest/InterfaceOne;)V",
                "buildImplementation:()V");

        // Then:
        assertMembersLeft(
                "Main",
                "useInterfaceOne:(Ltest/InterfaceOne;)V",
                "buildImplementation:()V");
        assertMembersLeft("InterfaceOne", "m:()V");
        assertClassSkipped("InterfaceTwo");
        assertMembersLeft("Implementation", "<init>:()V", "m:()V");
    }

    @Test
    public void overrides_interfaceTwoUsed_classUsed() throws Exception {
        // Given:
        Files.write(
                TestClasses.MultipleOverriddenMethods.main(), new File(mTestPackageDir, "Main.class"));
        Files.write(TestClasses.MultipleOverriddenMethods.interfaceOne(), new File(mTestPackageDir, "InterfaceOne.class"));
        Files.write(TestClasses.MultipleOverriddenMethods.interfaceTwo(), new File(mTestPackageDir, "InterfaceTwo.class"));
        Files.write(TestClasses.MultipleOverriddenMethods.implementation(), new File(mTestPackageDir, "Implementation.class"));

        // When:
        run(
                "Main",
                "useInterfaceTwo:(Ltest/InterfaceTwo;)V",
                "buildImplementation:()V");

        // Then:
        assertMembersLeft(
                "Main",
                "useInterfaceTwo:(Ltest/InterfaceTwo;)V",
                "buildImplementation:()V");
        assertMembersLeft("InterfaceTwo", "m:()V");
        assertClassSkipped("InterfaceOne");
        assertMembersLeft("Implementation", "<init>:()V", "m:()V");
    }

    @Test
    public void overrides_twoInterfacesUsed_classUsed() throws Exception {
        // Given:
        Files.write(
                TestClasses.MultipleOverriddenMethods.main(), new File(mTestPackageDir, "Main.class"));
        Files.write(TestClasses.MultipleOverriddenMethods.interfaceOne(), new File(mTestPackageDir, "InterfaceOne.class"));
        Files.write(TestClasses.MultipleOverriddenMethods.interfaceTwo(), new File(mTestPackageDir, "InterfaceTwo.class"));
        Files.write(TestClasses.MultipleOverriddenMethods.implementation(), new File(mTestPackageDir, "Implementation.class"));

        // When:
        run(
                "Main",
                "useInterfaceOne:(Ltest/InterfaceOne;)V",
                "useInterfaceTwo:(Ltest/InterfaceTwo;)V",
                "buildImplementation:()V");

        // Then:
        assertMembersLeft(
                "Main",
                "useInterfaceOne:(Ltest/InterfaceOne;)V",
                "useInterfaceTwo:(Ltest/InterfaceTwo;)V",
                "buildImplementation:()V");
        assertMembersLeft("InterfaceOne", "m:()V");
        assertMembersLeft("InterfaceTwo", "m:()V");
        assertMembersLeft("Implementation", "<init>:()V", "m:()V");
    }

    @Test
    public void overrides_noInterfacesUsed_classUsed() throws Exception {
        // Given:
        Files.write(
                TestClasses.MultipleOverriddenMethods.main(), new File(mTestPackageDir, "Main.class"));
        Files.write(TestClasses.MultipleOverriddenMethods.interfaceOne(), new File(mTestPackageDir, "InterfaceOne.class"));
        Files.write(TestClasses.MultipleOverriddenMethods.interfaceTwo(), new File(mTestPackageDir, "InterfaceTwo.class"));
        Files.write(TestClasses.MultipleOverriddenMethods.implementation(), new File(mTestPackageDir, "Implementation.class"));

        // When:
        run(
                "Main",
                "useImplementation:(Ltest/Implementation;)V",
                "buildImplementation:()V");

        // Then:
        assertMembersLeft(
                "Main",
                "useImplementation:(Ltest/Implementation;)V",
                "buildImplementation:()V");
        assertClassSkipped("InterfaceOne");
        assertClassSkipped("InterfaceTwo");
        assertMembersLeft("Implementation", "<init>:()V", "m:()V");
    }

    @Test
    public void annotations_annotatedClass() throws Exception {
        // Given:
        Files.write(TestClasses.Annotations.main_annotatedClass(), new File(mTestPackageDir, "Main.class"));
        Files.write(TestClasses.Annotations.myAnnotation(), new File(mTestPackageDir, "MyAnnotation.class"));
        Files.write(TestClasses.Annotations.myEnum(), new File(mTestPackageDir, "MyEnum.class"));
        Files.write(TestClasses.Annotations.nested(), new File(mTestPackageDir, "Nested.class"));
        Files.write(TestClasses.emptyClass("SomeClass"), new File(mTestPackageDir, "SomeClass.class"));
        Files.write(TestClasses.emptyClass("SomeOtherClass"), new File(mTestPackageDir, "SomeOtherClass.class"));

        // When:
        run("Main", "main:()V");

        // Then:
        assertMembersLeft(
                "Main",
                "main:()V");
        assertMembersLeft("SomeClass");
        assertMembersLeft("SomeOtherClass");
        assertMembersLeft(
                "MyAnnotation",
                "nested:()[Ltest/Nested;",
                "f:()I",
                "klass:()Ljava/lang/Class;",
                "myEnum:()Ltest/MyEnum;");
        assertMembersLeft("Nested", "name:()Ljava/lang/String;");
    }

    @Test
    public void annotations_annotatedMethod() throws Exception {
        // Given:
        Files.write(TestClasses.Annotations.main_annotatedMethod(), new File(mTestPackageDir, "Main.class"));
        Files.write(TestClasses.Annotations.myAnnotation(), new File(mTestPackageDir, "MyAnnotation.class"));
        Files.write(TestClasses.Annotations.nested(), new File(mTestPackageDir, "Nested.class"));
        Files.write(TestClasses.Annotations.myEnum(), new File(mTestPackageDir, "MyEnum.class"));
        Files.write(TestClasses.emptyClass("SomeClass"), new File(mTestPackageDir, "SomeClass.class"));
        Files.write(TestClasses.emptyClass("SomeOtherClass"), new File(mTestPackageDir, "SomeOtherClass.class"));

        // When:
        run("Main", "main:()V");

        // Then:
        assertMembersLeft(
                "Main",
                "main:()V");
        assertMembersLeft("SomeClass");
        assertMembersLeft("SomeOtherClass");
        assertMembersLeft(
                "MyAnnotation",
                "nested:()[Ltest/Nested;",
                "f:()I",
                "klass:()Ljava/lang/Class;",
                "myEnum:()Ltest/MyEnum;");
        assertMembersLeft("Nested", "name:()Ljava/lang/String;");
    }

    @Test
    public void annotations_annotationsStripped() throws Exception {
        // Given:
        Files.write(TestClasses.Annotations.main_annotatedMethod(), new File(mTestPackageDir, "Main.class"));
        Files.write(TestClasses.Annotations.myAnnotation(), new File(mTestPackageDir, "MyAnnotation.class"));
        Files.write(TestClasses.Annotations.nested(), new File(mTestPackageDir, "Nested.class"));
        Files.write(TestClasses.Annotations.myEnum(), new File(mTestPackageDir, "MyEnum.class"));
        Files.write(TestClasses.emptyClass("SomeClass"), new File(mTestPackageDir, "SomeClass.class"));
        Files.write(TestClasses.emptyClass("SomeOtherClass"), new File(mTestPackageDir, "SomeOtherClass.class"));

        // When:
        run("Main", "notAnnotated:()V");

        // Then:
        assertMembersLeft(
                "Main",
                "notAnnotated:()V");
        assertClassSkipped("SomeClass");
        assertClassSkipped("SomeOtherClass");
        assertClassSkipped("MyAnnotation");
        assertClassSkipped("Nested");
    }

    @Test
    public void annotations_keepRules() throws Exception {
        Files.write(TestClasses.Annotations.main_annotatedClass(), new File(mTestPackageDir, "Main.class"));
        Files.write(TestClasses.Annotations.myAnnotation(), new File(mTestPackageDir, "MyAnnotation.class"));
        Files.write(TestClasses.Annotations.nested(), new File(mTestPackageDir, "Nested.class"));
        Files.write(TestClasses.Annotations.myEnum(), new File(mTestPackageDir, "MyEnum.class"));
        Files.write(TestClasses.emptyClass("SomeClass"), new File(mTestPackageDir, "SomeClass.class"));
        Files.write(TestClasses.emptyClass("SomeOtherClass"), new File(mTestPackageDir, "SomeOtherClass.class"));

        run(new KeepRules() {
            @Override
            public <T> Map<T, DependencyType> getSymbolsToKeep(T klass, ShrinkerGraph<T> graph) {
                Map<T, DependencyType> result = Maps.newHashMap();
                for (String annotation : graph.getAnnotations(klass)) {
                    if (annotation.equals("test/MyAnnotation")) {
                        result.put(klass, DependencyType.REQUIRED_CLASS_STRUCTURE);
                    }
                }

                return result;
            }
        });

        assertMembersLeft("Main");
    }

    @Test
    public void signatures_classSignature() throws Exception {
        // Given:
        Files.write(TestClasses.Signatures.main(), new File(mTestPackageDir, "Main.class"));
        Files.write(TestClasses.Signatures.named(), new File(mTestPackageDir, "Named.class"));
        Files.write(TestClasses.Signatures.namedMap(), new File(mTestPackageDir, "NamedMap.class"));
        Files.write(TestClasses.Signatures.hasAge(), new File(mTestPackageDir, "HasAge.class"));

        // When:
        run("Main", "main:(Ltest/NamedMap;)V");

        // Then:
        assertMembersLeft(
                "Main",
                "main:(Ltest/NamedMap;)V");
        assertMembersLeft("NamedMap");
        assertMembersLeft("Named");
        assertClassSkipped("HasAge");
    }

    @Test
    public void signatures_methodSignature() throws Exception {
        // Given:
        Files.write(TestClasses.Signatures.main(), new File(mTestPackageDir, "Main.class"));
        Files.write(TestClasses.Signatures.named(), new File(mTestPackageDir, "Named.class"));
        Files.write(TestClasses.Signatures.namedMap(), new File(mTestPackageDir, "NamedMap.class"));
        Files.write(TestClasses.Signatures.hasAge(), new File(mTestPackageDir, "HasAge.class"));

        // When:
        run("Main", "callMethod:(Ltest/NamedMap;)V");

        // Then:
        assertMembersLeft(
                "Main",
                "callMethod:(Ltest/NamedMap;)V");
        assertMembersLeft("NamedMap", "method:(Ljava/util/Collection;)V");
        assertMembersLeft("Named");
        assertMembersLeft("HasAge");
    }

    @Test
    public void superCalls_directSuperclass() throws Exception {
        // Given:
        Files.write(TestClasses.SuperCalls.aaa(), new File(mTestPackageDir, "Aaa.class"));
        Files.write(TestClasses.SuperCalls.bbb(), new File(mTestPackageDir, "Bbb.class"));
        Files.write(TestClasses.SuperCalls.ccc(), new File(mTestPackageDir, "Ccc.class"));

        // When:
        run("Ccc", "callBbbMethod:()V");

        // Then:
        assertMembersLeft("Aaa");
        assertMembersLeft("Bbb", "onlyInBbb:()V");
        assertMembersLeft("Ccc", "callBbbMethod:()V");
    }

    @Test
    public void superCalls_indirectSuperclass() throws Exception {
        // Given:
        Files.write(TestClasses.SuperCalls.aaa(), new File(mTestPackageDir, "Aaa.class"));
        Files.write(TestClasses.SuperCalls.bbb(), new File(mTestPackageDir, "Bbb.class"));
        Files.write(TestClasses.SuperCalls.ccc(), new File(mTestPackageDir, "Ccc.class"));

        // When:
        run("Ccc", "callAaaMethod:()V");

        // Then:
        assertMembersLeft("Aaa", "onlyInAaa:()V");
        assertMembersLeft("Bbb");
        assertMembersLeft("Ccc", "callAaaMethod:()V");
    }

    @Test
    public void superCalls_both() throws Exception {
        // Given:
        Files.write(TestClasses.SuperCalls.aaa(), new File(mTestPackageDir, "Aaa.class"));
        Files.write(TestClasses.SuperCalls.bbb(), new File(mTestPackageDir, "Bbb.class"));
        Files.write(TestClasses.SuperCalls.ccc(), new File(mTestPackageDir, "Ccc.class"));

        // When:
        run("Ccc", "callOverriddenMethod:()V");

        // Then:
        assertMembersLeft("Aaa");
        assertMembersLeft("Bbb", "overridden:()V");
        assertMembersLeft("Ccc", "callOverriddenMethod:()V");
    }

    @Test
    public void innerClasses() throws Exception {
        // Given:
        Files.write(TestClasses.InnerClasses.main(), new File(mTestPackageDir, "Main.class"));
        Files.write(TestClasses.InnerClasses.hasInnerClass(), new File(mTestPackageDir, "HasInnerClass.class"));
        Files.write(TestClasses.InnerClasses.staticInnerClass(), new File(mTestPackageDir, "HasInnerClass$StaticInnerClass.class"));

        // When:
        run("Main", "main:()V");

        // Then:
        assertMembersLeft("Main", "main:()V");
        assertMembersLeft("HasInnerClass$StaticInnerClass", "method:()V", "<init>:()V");
        assertMembersLeft("HasInnerClass");
    }

    @Test
    public void staticMethods() throws Exception {
        // Given:
        Files.write(TestClasses.StaticMembers.main(), new File(mTestPackageDir, "Main.class"));
        Files.write(TestClasses.StaticMembers.utils(), new File(mTestPackageDir, "Utils.class"));

        // When:
        run("Main", "callStaticMethod:()Ljava/lang/Object;");

        // Then:
        assertMembersLeft("Main", "callStaticMethod:()Ljava/lang/Object;");
        assertMembersLeft("Utils", "staticMethod:()Ljava/lang/Object;");
    }

    @Test
    public void staticFields_uninitialized() throws Exception {
        // Given:
        Files.write(TestClasses.StaticMembers.main(), new File(mTestPackageDir, "Main.class"));
        Files.write(TestClasses.StaticMembers.utils(), new File(mTestPackageDir, "Utils.class"));

        // When:
        run("Main", "getStaticField:()Ljava/lang/Object;");

        // Then:
        assertMembersLeft("Main", "getStaticField:()Ljava/lang/Object;");
        assertMembersLeft("Utils", "staticField:Ljava/lang/Object;");
    }

    @Test
    public void reflection_instanceOf() throws Exception {
        // Given:
        Files.write(TestClasses.Reflection.main_instanceOf(), new File(mTestPackageDir, "Main.class"));
        Files.write(TestClasses.emptyClass("Foo"), new File(mTestPackageDir, "Foo.class"));

        // When:
        run("Main", "main:(Ljava/lang/Object;)Z");

        // Then:
        assertMembersLeft("Main", "main:(Ljava/lang/Object;)Z");
        assertMembersLeft("Foo");
    }

    @Test
    public void reflection_classLiteral() throws Exception {
        // Given:
        Files.write(
                TestClasses.Reflection.main_classLiteral(), new File(mTestPackageDir, "Main.class"));
        Files.write(TestClasses.emptyClass("Foo"), new File(mTestPackageDir, "Foo.class"));

        // When:
        run("Main", "main:()Ljava/lang/Object;");

        // Then:
        assertMembersLeft("Main", "main:()Ljava/lang/Object;");
        assertMembersLeft("Foo");
    }

    @Test
    public void testTryCatch() throws Exception {
        // Given:
        Files.write(TestClasses.TryCatch.main(), new File(mTestPackageDir, "Main.class"));
        Files.write(TestClasses.TryCatch.customException(), new File(mTestPackageDir, "CustomException.class"));

        // When:
        run("Main", "main:()V");

        // Then:
        assertMembersLeft("Main", "main:()V", "helper:()V");
        assertMembersLeft("CustomException");
    }

    @Test
    public void testTryFinally() throws Exception {
        // Given:
        Files.write(TestClasses.TryCatch.main_tryFinally(), new File(mTestPackageDir, "Main.class"));

        // When:
        run("Main", "main:()V");

        // Then:
        assertMembersLeft("Main", "main:()V", "helper:()V");
    }

    @Test
    public void abstractClasses_callToInterfaceMethodInAbstractClass() throws Exception {
        // Given:
        Files.write(TestClasses.AbstractClasses.myInterface(), new File(mTestPackageDir, "MyInterface.class"));
        Files.write(TestClasses.AbstractClasses.abstractImpl(), new File(mTestPackageDir, "AbstractImpl.class"));
        Files.write(
                TestClasses.AbstractClasses.realImpl(), new File(mTestPackageDir, "RealImpl.class"));

        // When:
        run("RealImpl", "main:()V");

        // Then:
        assertMembersLeft("MyInterface", "m:()V");
        assertMembersLeft("RealImpl", "main:()V", "m:()V");
        assertMembersLeft("AbstractImpl", "helper:()V");
    }

    @Test
    public void primitives() throws Exception {
        // Given:
        Files.write(TestClasses.Primitives.main(), new File(mTestPackageDir, "Main.class"));

        // When:
        run("Main", "ldc:()Ljava/lang/Object;", "checkcast:(Ljava/lang/Object;)[I");

        // Then:
        assertMembersLeft("Main", "ldc:()Ljava/lang/Object;", "checkcast:(Ljava/lang/Object;)[I");
    }

    @Test
    public void invalidReferences_sunMiscUnsafe() throws Exception {
        // Given:
        Files.write(TestClasses.InvalidReferences.main_sunMiscUnsafe(), new File(mTestPackageDir, "Main.class"));

        // When:
        run("Main", "main:()V");

        // Then:
        assertMembersLeft("Main", "main:()V");
        assertThat(mShrinkerLogger.getWarningsCount()).isGreaterThan(0);
    }

    @Test
    public void invalidReferences_Instrumentation() throws Exception {
        // Given:
        Files.write(TestClasses.InvalidReferences.main_javaInstrumentation(), new File(mTestPackageDir, "Main.class"));

        // When:
        run("Main", "main:()V");

        // Make sure we kept the method, even though we encountered unrecognized classes.
        assertMembersLeft("Main", "main:()V", "transform:(Ljava/lang/ClassLoader;Ljava/lang/String;Ljava/lang/Class;Ljava/security/ProtectionDomain;[B)[B");
        assertImplements("Main", "java/lang/instrument/ClassFileTransformer");
        assertThat(mShrinkerLogger.getWarningsCount()).isGreaterThan(0);
    }

    private void run(String className, String... methods) throws IOException {
        run(new TestKeepRules(className, methods));
    }

    private void run(KeepRules keepRules) throws IOException {
        mShrinker.run(
                mInputs,
                Collections.<TransformInput>emptyList(),
                mOutput,
                ImmutableMap.<AbstractShrinker.CounterSet, KeepRules>of(
                        AbstractShrinker.CounterSet.SHRINK, keepRules),
                false);
    }
}