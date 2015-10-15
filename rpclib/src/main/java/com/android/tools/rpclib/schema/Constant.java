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
package com.android.tools.rpclib.schema;

import org.jetbrains.annotations.NotNull;

import com.android.tools.rpclib.binary.BinaryClass;
import com.android.tools.rpclib.binary.BinaryID;
import com.android.tools.rpclib.binary.BinaryObject;
import com.android.tools.rpclib.binary.Decoder;
import com.android.tools.rpclib.binary.Encoder;
import com.android.tools.rpclib.binary.Namespace;
import com.android.tools.rpclib.any.Box;

import java.io.IOException;

public final class Constant implements BinaryObject {
    //<<<Start:Java.ClassBody:1>>>
    private String mName;
    private Object mValue;

    // Constructs a default-initialized {@link Constant}.
    public Constant() {}


    public String getName() {
        return mName;
    }

    public Constant setName(String v) {
        mName = v;
        return this;
    }

    public Object getValue() {
        return mValue;
    }

    public Constant setValue(Object v) {
        mValue = v;
        return this;
    }

    @Override @NotNull
    public BinaryClass klass() { return Klass.INSTANCE; }

    private static final byte[] IDBytes = {89, -115, -108, -95, 52, -54, -18, -56, -16, 111, -23, -65, -17, 125, 64, 58, 69, 108, 1, -53, };
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
        public BinaryObject create() { return new Constant(); }

        @Override
        public void encode(@NotNull Encoder e, BinaryObject obj) throws IOException {
            Constant o = (Constant)obj;
            e.string(o.mName);
            e.variant(Box.wrap(o.mValue));
        }

        @Override
        public void decode(@NotNull Decoder d, BinaryObject obj) throws IOException {
            Constant o = (Constant)obj;
            o.mName = d.string();
            o.mValue = ((Box)d.variant()).unwrap();
        }
        //<<<End:Java.KlassBody:2>>>
    }
}
