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

import com.intellij.ui.SimpleColoredComponent;
import com.intellij.ui.SimpleTextAttributes;
import org.jetbrains.annotations.NotNull;

import com.android.tools.rpclib.binary.BinaryClass;
import com.android.tools.rpclib.binary.BinaryID;
import com.android.tools.rpclib.binary.BinaryObject;
import com.android.tools.rpclib.binary.Decoder;
import com.android.tools.rpclib.binary.Encoder;
import com.android.tools.rpclib.binary.Namespace;

import java.io.IOException;

public final class Struct extends Type {
    @Override
    public void encodeValue(@NotNull Encoder e, Object value) throws IOException {
        assert(value instanceof BinaryObject);
        e.value((BinaryObject)value);
    }

    @Override
    public Object decodeValue(@NotNull Decoder d) throws IOException {
        BinaryClass klass = Namespace.lookup(mID);
        if (klass == null) {
            throw new IOException("Unknown type id: " + mID);
        }
        BinaryObject obj = klass.create();
        klass.decode(d, obj);
        return obj;
    }

    @Override
    public void render(@NotNull Object value, @NotNull SimpleColoredComponent component) {
        renderObject(value, component);
    }

    //<<<Start:Java.ClassBody:1>>>
    private String mName;
    private BinaryID mID;

    // Constructs a default-initialized {@link Struct}.
    public Struct() {}


    public String getName() {
        return mName;
    }

    public Struct setName(String v) {
        mName = v;
        return this;
    }

    public BinaryID getID() {
        return mID;
    }

    public Struct setID(BinaryID v) {
        mID = v;
        return this;
    }

    @Override @NotNull
    public BinaryClass klass() { return Klass.INSTANCE; }

    private static final byte[] IDBytes = {-33, -29, -98, -14, -114, -100, -22, -101, -109, 73, 71, 118, -17, -99, -30, 70, -39, 104, -110, -108, };
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
        public BinaryObject create() { return new Struct(); }

        @Override
        public void encode(@NotNull Encoder e, BinaryObject obj) throws IOException {
            Struct o = (Struct)obj;
            e.string(o.mName);
            e.id(o.mID);
        }

        @Override
        public void decode(@NotNull Decoder d, BinaryObject obj) throws IOException {
            Struct o = (Struct)obj;
            o.mName = d.string();
            o.mID = d.id();
        }
        //<<<End:Java.KlassBody:2>>>
    }
}
