package com.sajjad.aimobilestudio;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.graphics.Color;
import android.graphics.Typeface;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private LinearLayout root, projectList;
    private TextView status;
    private EditText prompt;
    private ProjectStore store;
    private String selectedProject = "";

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        store = new ProjectStore(this);
        buildUi();
        refreshProjects();
    }

    private int dp(float n) { return (int)(n * getResources().getDisplayMetrics().density + 0.5f); }
    private TextView text(String value, int size, boolean bold) {
        TextView v = new TextView(this);
        v.setText(value); v.setTextSize(size); v.setTextColor(Color.WHITE);
        if (bold) v.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return v;
    }
    private Button button(String label, Runnable action) {
        Button b = new Button(this); b.setText(label);
        b.setOnClickListener(v -> action.run()); return b;
    }
    private void buildUi() {
        ScrollView scroll = new ScrollView(this);
        root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(22), dp(18), dp(24)); root.setBackgroundColor(Color.rgb(12,18,25));
        scroll.addView(root); setContentView(scroll);
        TextView brand = text("AI MOBILE STUDIO", 23, true);
        brand.setTextColor(Color.rgb(75,220,155)); root.addView(brand);
        root.addView(text("Android project workspace • v2.0", 13, false));
        status = text("Ready — create a project to begin.", 13, false);
        status.setPadding(0,dp(12),0,dp(12)); root.addView(status);
        root.addView(button("＋  New Android Project", this::newProject));
        root.addView(text("PROJECT EXPLORER", 14, true));
        projectList = new LinearLayout(this); projectList.setOrientation(LinearLayout.VERTICAL);
        root.addView(projectList);
        root.addView(text("AI CODING AGENT", 14, true));
        prompt = new EditText(this); prompt.setTextColor(Color.WHITE);
        prompt.setHintTextColor(Color.LTGRAY); prompt.setHint("Describe the Android app or code change...");
        prompt.setMinLines(3); prompt.setGravity(Gravity.TOP); root.addView(prompt);
        root.addView(button("Save OpenAI API Key", this::saveApiKey));
        root.addView(button("Generate with OpenAI", this::generate));
        root.addView(button("Open selected project folder", this::showProjectInfo));
        root.addView(text("Builds are run through the Termux workflow. See README for setup.", 12, false));
    }
    private void newProject() {
        EditText name = new EditText(this); name.setSingleLine(true); name.setHint("MyAndroidApp");
        new AlertDialog.Builder(this).setTitle("New Android project").setView(name)
            .setPositiveButton("Create", (d,w) -> {
                try {
                    selectedProject = store.createProject(name.getText().toString());
                    status.setText("Created: " + selectedProject); refreshProjects();
                } catch (Exception e) { status.setText("Create failed: " + e.getMessage()); }
            }).setNegativeButton("Cancel", null).show();
    }
    private void refreshProjects() {
        projectList.removeAllViews();
        for (String name : store.listProjects()) {
            Button b = button((name.equals(selectedProject) ? "✓ " : "") + name,
                () -> { selectedProject = name; status.setText("Selected project: " + name); refreshProjects(); });
            projectList.addView(b);
        }
    }
    private void saveApiKey() {
        EditText key = new EditText(this); key.setSingleLine(true); key.setHint("sk-...");
        key.setInputType(129);
        new AlertDialog.Builder(this).setTitle("OpenAI API key").setMessage("API billing is separate from a ChatGPT subscription.")
            .setView(key).setPositiveButton("Save", (d,w) -> {
                getSharedPreferences("secure_settings", MODE_PRIVATE).edit()
                    .putString("openai_api_key", key.getText().toString().trim()).apply();
                status.setText("API key saved in app-private storage.");
            }).setNegativeButton("Cancel", null).show();
    }
    private void generate() {
        if (selectedProject.isEmpty()) { status.setText("Create/select a project first."); return; }
        String apiKey = getSharedPreferences("secure_settings", MODE_PRIVATE).getString("openai_api_key", "");
        if (apiKey.isEmpty()) { status.setText("Save an OpenAI API key first."); return; }
        String request = prompt.getText().toString().trim();
        if (request.isEmpty()) { status.setText("Write an app instruction first."); return; }
        status.setText("Generating…");
        worker.execute(() -> {
            try {
                String result = new OpenAIClient(apiKey).generate(request);
                runOnUiThread(() -> new AlertDialog.Builder(this).setTitle("AI response")
                    .setMessage(result).setPositiveButton("OK", (d,w) -> {
                        try { store.saveText(selectedProject, "AI_RESPONSE.md", result); status.setText("Saved AI_RESPONSE.md in " + selectedProject); }
                        catch (Exception e) { status.setText("Save failed: " + e.getMessage()); }
                    }).show());
            } catch (Exception e) { runOnUiThread(() -> status.setText("AI error: " + e.getMessage())); }
        });
    }
    private void showProjectInfo() {
        if (selectedProject.isEmpty()) { status.setText("Select a project first."); return; }
        status.setText("Project folder: " + store.projectPath(selectedProject) + " (app-private storage)");
    }
    @Override protected void onDestroy() { super.onDestroy(); worker.shutdownNow(); }
}
