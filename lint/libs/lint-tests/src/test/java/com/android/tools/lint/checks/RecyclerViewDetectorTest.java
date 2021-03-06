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

package com.android.tools.lint.checks;

import com.android.tools.lint.detector.api.Detector;

@SuppressWarnings({"ClassNameDiffersFromFileName", "MethodMayBeStatic", "SpellCheckingInspection"})
public class RecyclerViewDetectorTest extends AbstractCheckTest {
    public void test() throws Exception {
        assertEquals(""
                + "src/test/pkg/RecyclerViewTest.java:69: Warning: Do not treat position as fixed; only use immediately and call holder.getAdapterPosition() to look it up later [RecyclerView]\n"
                + "        public void onBindViewHolder(ViewHolder holder, int position) {\n"
                + "                                                        ~~~~~~~~~~~~\n"
                + "src/test/pkg/RecyclerViewTest.java:82: Warning: Do not treat position as fixed; only use immediately and call holder.getAdapterPosition() to look it up later [RecyclerView]\n"
                + "        public void onBindViewHolder(ViewHolder holder, final int position) {\n"
                + "                                                        ~~~~~~~~~~~~~~~~~~\n"
                + "src/test/pkg/RecyclerViewTest.java:102: Warning: Do not treat position as fixed; only use immediately and call holder.getAdapterPosition() to look it up later [RecyclerView]\n"
                + "        public void onBindViewHolder(ViewHolder holder, final int position) {\n"
                + "                                                        ~~~~~~~~~~~~~~~~~~\n"
                + "src/test/pkg/RecyclerViewTest.java:111: Warning: Do not treat position as fixed; only use immediately and call holder.getAdapterPosition() to look it up later [RecyclerView]\n"
                + "        public void onBindViewHolder(ViewHolder holder, final int position, List<Object> payloads) {\n"
                + "                                                        ~~~~~~~~~~~~~~~~~~\n"
                + "0 errors, 4 warnings\n",

                lintProject(
                    java("src/test/pkg/RecyclerViewTest.java", ""
                            + "package test.pkg;\n"
                            + "\n"
                            + "import android.support.v7.widget.RecyclerView;\n"
                            + "import android.view.View;\n"
                            + "import android.widget.TextView;\n"
                            + "\n"
                            + "import java.util.List;\n"
                            + "\n"
                            + "@SuppressWarnings({\"ClassNameDiffersFromFileName\", \"unused\"})\n"
                            + "public class RecyclerViewTest {\n"
                            + "    // From https://developer.android.com/training/material/lists-cards.html\n"
                            + "    public static class Test1 extends RecyclerView.Adapter<Test1.ViewHolder> {\n"
                            + "        private String[] mDataset;\n"
                            + "        public static class ViewHolder extends RecyclerView.ViewHolder {\n"
                            + "            public TextView mTextView;\n"
                            + "            public ViewHolder(TextView v) {\n"
                            + "                super(v);\n"
                            + "                mTextView = v;\n"
                            + "            }\n"
                            + "        }\n"
                            + "\n"
                            + "        public Test1(String[] myDataset) {\n"
                            + "            mDataset = myDataset;\n"
                            + "        }\n"
                            + "\n"
                            + "        @Override\n"
                            + "        public void onBindViewHolder(ViewHolder holder, int position) {\n"
                            + "            holder.mTextView.setText(mDataset[position]); // OK\n"
                            + "        }\n"
                            + "    }\n"
                            + "\n"
                            + "    public static class Test2 extends RecyclerView.Adapter<Test2.ViewHolder> {\n"
                            + "        public static class ViewHolder extends RecyclerView.ViewHolder {\n"
                            + "            public ViewHolder(View v) {\n"
                            + "                super(v);\n"
                            + "            }\n"
                            + "        }\n"
                            + "\n"
                            + "        @Override\n"
                            + "        public void onBindViewHolder(ViewHolder holder, int position) {\n"
                            + "            // OK\n"
                            + "        }\n"
                            + "    }\n"
                            + "\n"
                            + "    public static class Test3 extends RecyclerView.Adapter<Test3.ViewHolder> {\n"
                            + "        public static class ViewHolder extends RecyclerView.ViewHolder {\n"
                            + "            public ViewHolder(View v) {\n"
                            + "                super(v);\n"
                            + "            }\n"
                            + "        }\n"
                            + "\n"
                            + "        @Override\n"
                            + "        public void onBindViewHolder(ViewHolder holder, final int position) {\n"
                            + "            // OK - final, but not referenced\n\n"
                            + "        }\n"
                            + "    }\n"
                            + "\n"
                            + "    public static class Test4 extends RecyclerView.Adapter<Test4.ViewHolder> {\n"
                            + "        private int myCachedPosition;\n"
                            + "\n"
                            + "        public static class ViewHolder extends RecyclerView.ViewHolder {\n"
                            + "            public ViewHolder(View v) {\n"
                            + "                super(v);\n"
                            + "            }\n"
                            + "        }\n"
                            + "\n"
                            + "        @Override\n"
                            + "        public void onBindViewHolder(ViewHolder holder, int position) {\n"
                            + "            myCachedPosition = position; // ERROR: escapes\n"
                            + "        }\n"
                            + "    }\n"
                            + "\n"
                            + "    public static class Test5 extends RecyclerView.Adapter<Test5.ViewHolder> {\n"
                            + "        public static class ViewHolder extends RecyclerView.ViewHolder {\n"
                            + "            public ViewHolder(View v) {\n"
                            + "                super(v);\n"
                            + "            }\n"
                            + "        }\n"
                            + "\n"
                            + "        @Override\n"
                            + "        public void onBindViewHolder(ViewHolder holder, final int position) {\n"
                            + "            new Runnable() {\n"
                            + "                @Override public void run() {\n"
                            + "                    System.out.println(position); // ERROR: escapes\n"
                            + "                }\n"
                            + "            }.run();\n"
                            + "        }\n"
                            + "    }\n"
                            + "\n"
                            + "    // https://code.google.com/p/android/issues/detail?id=172335\n"
                            + "    public static class Test6 extends RecyclerView.Adapter<Test6.ViewHolder> {\n"
                            + "        List<String> myData;\n"
                            + "        public static class ViewHolder extends RecyclerView.ViewHolder {\n"
                            + "            private View itemView;\n"
                            + "            public ViewHolder(View v) {\n"
                            + "                super(v);\n"
                            + "            }\n"
                            + "        }\n"
                            + "\n"
                            + "        @Override\n"
                            + "        public void onBindViewHolder(ViewHolder holder, final int position) {\n"
                            + "            holder.itemView.setOnClickListener(new View.OnClickListener() {\n"
                            + "                public void onClick(View view) {\n"
                            + "                    myData.get(position); // ERROR\n"
                            + "                }\n"
                            + "            });\n"
                            + "        }\n"
                            + "\n"
                            + "        @Override\n"
                            + "        public void onBindViewHolder(ViewHolder holder, final int position, List<Object> payloads) {\n"
                            + "            holder.itemView.setOnClickListener(new View.OnClickListener() {\n"
                            + "                public void onClick(View view) {\n"
                            + "                    myData.get(position); // ERROR\n"
                            + "                }\n"
                            + "            });\n"
                            + "        }\n"
                            + "    }\n"
                            + "}\n"),
                        java("src/android/support/v7/widget/RecyclerView.java", ""
                                + "package android.support.v7.widget;\n"
                                + "\n"
                                + "import android.content.Context;\n"
                                + "import android.util.AttributeSet;\n"
                                + "import android.view.View;\n"
                                + "import java.util.List;\n"
                                + "\n"
                                + "// Just a stub for lint unit tests\n"
                                + "public class RecyclerView extends View {\n"
                                + "    public RecyclerView(Context context, AttributeSet attrs) {\n"
                                + "        super(context, attrs);\n"
                                + "    }\n"
                                + "\n"
                                + "    public abstract static class ViewHolder {\n"
                                + "        public ViewHolder(View itemView) {\n"
                                + "        }\n"
                                + "    }\n"
                                + "\n"
                                + "    public abstract static class Adapter<VH extends ViewHolder> {\n"
                                + "        public abstract void onBindViewHolder(VH holder, int position);\n"
                                + "        public void onBindViewHolder(VH holder, int position, List<Object> payloads) {\n"
                                + "        }\n"
                                + "    }\n"
                                + "}\n"))
            );
    }

    @Override
    protected Detector getDetector() {
        return new RecyclerViewDetector();
    }
}