package com.sajjad.aimobilestudio;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {
    private static final int BG = Color.rgb(7, 17, 27);
    private static final int PANEL = Color.rgb(15, 29, 43);
    private static final int GREEN = Color.rgb(22, 199, 132);
    private static final int MUTED = Color.rgb(155, 174, 190);
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private LinearLayout root, projectList;
    private TextView status, selectedLabel;
    private EditText prompt;
    private Spinner language;
    private ProjectStore store;
    private String selectedProject = "";

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        store = new ProjectStore(this);
        buildUi();
        refreshProjects();
    }

    private int dp(float n) { return (int)(n * getResources().getDisplayMetrics().density + 0.5f); }
    private GradientDrawable shape(int color, int radius, int stroke) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color); d.setCornerRadius(dp(radius));
        if (stroke != 0) d.setStroke(dp(1), stroke);
        return d;
    }
    private TextView text(String value, int size, int color, boolean bold) {
        TextView v = new TextView(this);
        v.setText(value); v.setTextSize(size); v.setTextColor(color);
        if (bold) v.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return v;
    }
    private void gap(LinearLayout parent, int height) {
        View v = new View(this);
        parent.addView(v, new LinearLayout.LayoutParams(1, dp(height)));
    }
    private LinearLayout verticalPanel() {
        LinearLayout p = new LinearLayout(this);
        p.setOrientation(LinearLayout.VERTICAL);
        p.setPadding(dp(16), dp(15), dp(16), dp(15));
        p.setBackground(shape(PANEL, 18, Color.rgb(32, 55, 69)));
        return p;
    }
    private Button button(String label, boolean primary, Runnable action) {
        Button b = new Button(this);
        b.setText(label); b.setAllCaps(false); b.setTextSize(14);
        b.setTextColor(primary ? BG : Color.WHITE);
        b.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        b.setBackground(shape(primary ? GREEN : Color.rgb(29, 47, 62), 13,
                primary ? GREEN : Color.rgb(48, 72, 88)));
        b.setPadding(dp(12), dp(7), dp(12), dp(7));
        b.setOnClickListener(v -> action.run());
        return b;
    }
    private void buildUi() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(22), dp(18), dp(28));
        root.setBackgroundColor(BG);
        scroll.addView(root);
        setContentView(scroll);

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        TextView mark = text("✦", 29, GREEN, true);
        mark.setGravity(Gravity.CENTER);
        mark.setBackground(shape(Color.rgb(16, 54, 53), 16, GREEN));
        LinearLayout.LayoutParams mp = new LinearLayout.LayoutParams(dp(54), dp(54));
        header.addView(mark, mp);
        LinearLayout brandCol = new LinearLayout(this);
        brandCol.setOrientation(LinearLayout.VERTICAL);
        brandCol.setPadding(dp(12), 0, 0, 0);
        brandCol.addView(text("AI MOBILE STUDIO", 20, Color.WHITE, true));
        brandCol.addView(text("CODE • CREATE • BUILD", 10, GREEN, true));
        header.addView(brandCol);
        root.addView(header);
        gap(root, 20);

        LinearLayout hero = verticalPanel();
        hero.setBackground(shape(Color.rgb(12, 43, 48), 20, Color.rgb(22, 111, 91)));
        hero.addView(text("Your idea. Your code. Your Android app.", 21, Color.WHITE, true));
        gap(hero, 7);
        hero.addView(text("AI-assisted workspace for building Android projects with multiple programming languages.", 13, MUTED, false));
        gap(hero, 13);
        hero.addView(text("JAVA   KOTLIN   HTML   CSS   JAVASCRIPT   XML   GRADLE", 11, GREEN, true));
        root.addView(hero);
        gap(root, 16);

        LinearLayout projectCard = verticalPanel();
        projectCard.addView(text("01  PROJECT WORKSPACE", 13, GREEN, true));
        gap(projectCard, 8);
        selectedLabel = text("No project selected", 13, MUTED, false);
        projectCard.addView(selectedLabel);
        gap(projectCard, 10);
        projectCard.addView(button("＋  Create new Android project", true, this::newProject));
        gap(projectCard, 10);
        projectList = new LinearLayout(this);
        projectList.setOrientation(LinearLayout.VERTICAL);
        projectCard.addView(projectList);
        root.addView(projectCard);
        gap(root, 16);

        LinearLayout aiCard = verticalPanel();
        aiCard.addView(text("02  AI CODING AGENT", 13, GREEN, true));
        gap(aiCard, 6);
        aiCard.addView(text("Choose the technology and describe the app or feature you want to build.", 13, MUTED, false));
        gap(aiCard, 12);
        language = new Spinner(this);
        String[] languages = {"Android Kotlin", "Android Java", "HTML + CSS + JavaScript", "Kotlin Multiplatform", "React Native (JavaScript)", "Flutter (Dart)", "Python", "C / C++", "XML + Gradle"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, languages);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        language.setAdapter(adapter);
        aiCard.addView(language);
        gap(aiCard, 10);
        prompt = new EditText(this);
        prompt.setTextColor(Color.WHITE);
        prompt.setHintTextColor(MUTED);
        prompt.setHint("Example: Create a beautiful expense tracker with dashboard, charts and local storage…");
        prompt.setTextSize(14);
        prompt.setGravity(Gravity.TOP);
        prompt.setMinLines(5);
        prompt.setPadding(dp(13), dp(12), dp(13), dp(12));
        prompt.setBackground(shape(Color.rgb(8, 19, 29), 13, Color.rgb(48, 72, 88)));
        aiCard.addView(prompt, new LinearLayout.LayoutParams(-1, -2));
        gap(aiCard, 10);
        aiCard.addView(button("✦  Generate with OpenAI", true, this::generate));
        gap(aiCard, 8);
        aiCard.addView(button("Save OpenAI API Key", false, this::saveApiKey));
        root.addView(aiCard);
        gap(root, 16);

        LinearLayout tools = verticalPanel();
        tools.addView(text("03  BUILD TOOLCHAIN", 13, GREEN, true));
        gap(tools, 8);
        tools.addView(text("Debug APK • Release APK • Android App Bundle (AAB)\nProject files • Build diagnostics • AI repair guidance", 13, MUTED, false));
        gap(tools, 8);
        tools.addView(button("Show selected project location", false, this::showProjectInfo));
        root.addView(tools);
        gap(root, 16);

        status = text("●  Ready — create a project to begin.", 12, GREEN, false);
        status.setPadding(dp(4), dp(8), dp(4), dp(8));
        root.addView(status);
        TextView footer = text("AI MOBILE STUDIO  •  BUILD YOUR NEXT IDEA", 10, MUTED, true);
        footer.setGravity(Gravity.CENTER);
        root.addView(footer);
    }

    private void newProject() {
        EditText name = new EditText(this);
        name.setSingleLine(true); name.setHint("MyAndroidApp");
        new AlertDialog.Builder(this).setTitle("Create Android project").setView(name)
            .setPositiveButton("Create", (d,w) -> {
                try {
                    selectedProject = store.createProject(name.getText().toString());
                    status.setText("●  Project created: " + selectedProject);
                    selectedLabel.setText("Selected: " + selectedProject);
                    refreshProjects();
                } catch (Exception e) { status.setText("Create failed: " + e.getMessage()); }
            }).setNegativeButton("Cancel", null).show();
    }
    private void refreshProjects() {
        projectList.removeAllViews();
        for (String name : store.listProjects()) {
            Button b = button((name.equals(selectedProject) ? "✓  " : "›  ") + name, false,
                () -> {
                    selectedProject = name;
                    selectedLabel.setText("Selected: " + name);
                    status.setText("●  Project selected: " + name);
                    refreshProjects();
                });
            projectList.addView(b, new LinearLayout.LayoutParams(-1, -2));
        }
        if (projectList.getChildCount() == 0)
            projectList.addView(text("Your projects will appear here.", 12, MUTED, false));
    }
    private void saveApiKey() {
        EditText key = new EditText(this);
        key.setSingleLine(true); key.setHint("sk-...");
        key.setInputType(129);
        new AlertDialog.Builder(this).setTitle("OpenAI API key")
            .setMessage("An OpenAI API key is separate from a ChatGPT subscription and may incur API charges.")
            .setView(key).setPositiveButton("Save", (d,w) -> {
                getSharedPreferences("secure_settings", MODE_PRIVATE).edit()
                    .putString("openai_api_key", key.getText().toString().trim()).apply();
                status.setText("●  API key saved in app-private storage.");
            }).setNegativeButton("Cancel", null).show();
    }
    private void generate() {
        if (selectedProject.isEmpty()) { status.setText("Create/select a project first."); return; }
        String apiKey = getSharedPreferences("secure_settings", MODE_PRIVATE).getString("openai_api_key", "");
        if (apiKey.isEmpty()) { status.setText("Save an OpenAI API key first."); return; }
        String request = prompt.getText().toString().trim();
        if (request.isEmpty()) { status.setText("Describe the app you want to create."); return; }
        final String project = selectedProject;
        final String tech = String.valueOf(language.getSelectedItem());
        String fullPrompt = "Act as a senior Android and software engineer. Target technology: " + tech +
            ". Project name: " + project + ". User request: " + request +
            "\nReturn a complete, production-minded implementation. For code, include every required file with its exact relative path and full contents. Explain build steps and dependencies. Never claim files were written or an APK was built.";
        status.setText("●  Generating with OpenAI…");
        worker.execute(() -> {
            try {
                String result = new OpenAIClient(apiKey).generate(fullPrompt);
                store.saveText(project, "AI_RESPONSE.md", result);
                runOnUiThread(() -> new AlertDialog.Builder(this).setTitle("AI output saved")
                    .setMessage("The AI response was saved as AI_RESPONSE.md in " + project +
                        ". Review and apply the generated files before building.")
                    .setPositiveButton("Show response", (d,w) ->
                        new AlertDialog.Builder(this).setTitle("AI response").setMessage(result)
                            .setPositiveButton("Close", null).show())
                    .setNegativeButton("Close", null).show());
                runOnUiThread(() -> status.setText("●  AI response saved. File-apply/build automation is not yet complete."));
            } catch (Exception e) {
                runOnUiThread(() -> status.setText("AI error: " + e.getMessage()));
            }
        });
    }
    private void showProjectInfo() {
        if (selectedProject.isEmpty()) { status.setText("Select a project first."); return; }
        status.setText("Project folder: " + store.projectPath(selectedProject) + " (app-private storage)");
    }
    @Override protected void onDestroy() {
        super.onDestroy();
        worker.shutdownNow();
    }
}