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
 *
 * THIS FILE WAS GENERATED BY codergen. EDIT WITH CARE.
 */
package com.android.tools.rpclib.binary;

import org.jetbrains.annotations.NotNull;

import com.android.tools.rpclib.binary.BinaryClass;
import com.android.tools.rpclib.binary.BinaryID;
import com.android.tools.rpclib.binary.BinaryObject;
import com.android.tools.rpclib.binary.Decoder;
import com.android.tools.rpclib.binary.Encoder;
import com.android.tools.rpclib.binary.Namespace;

import java.io.IOException;

public final class TypeB implements BinaryObject {
    //<<<Start:Java.ClassBody:1>>>
    private String mData;

    // Constructs a default-initialized {@link TypeB}.
    public TypeB() {}


    public String getData() {
        return mData;
    }

    public TypeB setData(String v) {
        mData = v;
        return this;
    }

    @Override @NotNull
    public BinaryClass klass() { return Klass.INSTANCE; }

    private static final byte[] IDBytes = {4, -124, -33, 125, -120, 37, -17, 67, -9, 113, 60, 92, 3, 43, -32, -3, 66, 31, 110, -121, };
    public static final BinaryID ID = new BinaryID(IDBytes);

    static {
        Namespace.register(ID, Klass.INSTANCE);
    }
    public static void register() {}
    //<<<End:Java.ClassBody:1>>>
    public enum Klass implements BinaryClass {
        //<<<Start:Java.KlassBody:2>>>
        INSTANCE;

        @Override @NotNull
        public BinaryID id() { return ID; }

        @Override @NotNull
        public BinaryObject create() { return new TypeB(); }

        @Override
        public void encode(@NotNull Encoder e, BinaryObject obj) throws IOException {
            TypeB o = (TypeB)obj;
            e.string(o.mData);
        }

        @Override
        public void decode(@NotNull Decoder d, BinaryObject obj) throws IOException {
            TypeB o = (TypeB)obj;
            o.mData = d.string();
        }
        //<<<End:Java.KlassBody:2>>>
    }
}
