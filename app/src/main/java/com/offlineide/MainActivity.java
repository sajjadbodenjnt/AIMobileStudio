package com.offlineide;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.widget.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

public class MainActivity extends Activity {
    private LinearLayout body;
    private TextView status;
    private EditText editor;
    private File workspace;
    private int dp(float n) { return (int)(n * getResources().getDisplayMetrics().density); }

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        workspace = new File(getFilesDir(), "workspace");
        if (!workspace.exists()) workspace.mkdirs();
        home();
    }

    private static void writeText(File file, String value) throws Exception {
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) parent.mkdirs();
        try (FileOutputStream out = new FileOutputStream(file, false)) {
            out.write(value.getBytes(StandardCharsets.UTF_8));
            out.flush();
        }
    }

    private static String readText(File file) throws Exception {
        try (FileInputStream in = new FileInputStream(file);
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int count;
            while ((count = in.read(buffer)) != -1) out.write(buffer, 0, count);
            return out.toString("UTF-8");
        }
    }

    private TextView label(String s, int size) {
        TextView t = new TextView(this);
        t.setText(s); t.setTextSize(size); t.setTextColor(Color.WHITE);
        return t;
    }

    private Button action(String s, Runnable r) {
        Button b = new Button(this); b.setText(s); b.setOnClickListener(v -> r.run());
        return b;
    }

    private void shell(String title) {
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(1); page.setPadding(dp(12), dp(10), dp(12), dp(8));
        page.setBackgroundColor(Color.rgb(10, 18, 15));
        TextView heading = label(title, 20);
        heading.setTypeface(null, Typeface.BOLD); heading.setPadding(0, dp(10), 0, dp(14));
        page.addView(heading);
        ScrollView scroll = new ScrollView(this);
        body = new LinearLayout(this); body.setOrientation(1); scroll.addView(body);
        page.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        status = label("READY • LOCAL WORKSPACE", 12);
        status.setTextColor(0xff6fe0a0); status.setPadding(0, dp(10), 0, 0);
        page.addView(status); setContentView(page);
    }

    private void home() {
        shell("OFFLINE ANDROID IDE");
        body.addView(label("Create native Android projects on your phone.", 16));
        body.addView(label("Workspace: " + workspace.getAbsolutePath(), 11));
        body.addView(action("New Android App Project", this::newProject));
        body.addView(action("Open Workspace", this::files));
        body.addView(action("Local AI Assistant", this::ai));
        body.addView(action("Build Toolchain Status", this::buildInfo));
        body.addView(label("\nPhase 1: project workspace, source editor, local AI connector and build setup screen. Full on-device compiler integration is the next milestone.", 13));
    }

    private void newProject() {
        EditText input = new EditText(this);
        input.setHint("App name (letters/numbers)");
        new android.app.AlertDialog.Builder(this).setTitle("Create Android app project").setView(input)
            .setNegativeButton("Cancel", null).setPositiveButton("Create", (d, w) -> {
                String name = input.getText().toString().trim().replaceAll("[^A-Za-z0-9_]", "");
                if (name.isEmpty()) name = "MyApp";
                try {
                    File entry = ProjectGenerator.create(workspace, name);
                    status.setText("ANDROID PROJECT CREATED: " + name);
                    edit(entry);
                } catch (Exception e) {
                    status.setText("PROJECT ERROR: " + e.getMessage());
                }
            }).show();
    }

    private void edit(File file) {
        shell("CODE EDITOR • " + file.getName());
        editor = new EditText(this); editor.setGravity(Gravity.TOP | Gravity.LEFT);
        editor.setTextSize(13); editor.setTypeface(Typeface.MONOSPACE);
        editor.setTextColor(0xffe3f3e9); editor.setBackgroundColor(0xff14211b);
        try { editor.setText(readText(file)); }
        catch (Exception e) { status.setText("OPEN ERROR: " + e.getMessage()); }
        body.addView(editor, new LinearLayout.LayoutParams(-1, 0, 1));
        body.addView(action("Save", () -> {
            try {
                writeText(file, editor.getText().toString());
                status.setText("SAVED: " + file.getName());
            } catch (Exception e) { status.setText("SAVE ERROR: " + e.getMessage()); }
        }));
        body.addView(action("Back", this::files));
    }

    private void files() {
        shell("PROJECT WORKSPACE");
        File[] all = workspace.listFiles();
        if (all == null || all.length == 0) body.addView(label("No projects yet. Tap New Java Project.", 14));
        else for (File f : all) {
            if (f.isDirectory()) {
                body.addView(label("📁 " + f.getName(), 15));
                File[] children = f.listFiles();
                if (children != null) for (File c : children) if (c.isFile())
                    body.addView(action("    " + c.getName(), () -> edit(c)));
            } else body.addView(action(f.getName(), () -> edit(f)));
        }
        body.addView(action("Home", this::home));
    }

    private void ai() {
        shell("LOCAL AI ASSISTANT");
        body.addView(label("Local endpoint: 127.0.0.1:11434 (Ollama)", 13));
        EditText prompt = new EditText(this); prompt.setHint("Describe the code you want...");
        prompt.setTextColor(Color.WHITE); prompt.setMinLines(4); body.addView(prompt);
        body.addView(action("Ask local model", () -> {
            String q = prompt.getText().toString().trim(); if (q.isEmpty()) return;
            status.setText("Connecting to local model...");
            new Thread(() -> {
                String answer = LocalAI.generate("qwen2.5-coder:3b", q);
                runOnUiThread(() -> new android.app.AlertDialog.Builder(this)
                    .setTitle("AI result").setMessage(answer).setPositiveButton("Close", null).show());
            }).start();
        }));
        body.addView(label("No cloud API is used. A local inference server and downloaded model are required.", 12));
    }

    private void buildInfo() {
        shell("OFFLINE BUILD TOOLCHAIN");
        body.addView(label("Required locally: Java 17, Gradle, Android SDK Platform 35, build-tools, AAPT2, D8/R8, apksigner, cached Android Gradle Plugin and dependencies.", 14));
        body.addView(label("The initial repository is the IDE foundation; it does not yet bundle or execute the full compiler. The build runner, logs, signing and install pipeline are Phase 2.", 13));
        body.addView(action("Home", this::home));
    }
}
