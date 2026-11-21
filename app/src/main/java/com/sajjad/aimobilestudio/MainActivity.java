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
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {
    private static final int BG=Color.rgb(7,17,27), PANEL=Color.rgb(15,29,43), GREEN=Color.rgb(22,199,132), MUTED=Color.rgb(155,174,190);
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private LinearLayout root, projectList;
    private TextView status, selectedLabel;
    private EditText prompt;
    private Spinner language;
    private ProjectStore store;
    private String selectedProject="";

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        store=new ProjectStore(this);
        buildUi();
        refreshProjects();
    }
    private int dp(float n){return (int)(n*getResources().getDisplayMetrics().density+0.5f);}
    private GradientDrawable shape(int color,int radius,int stroke){
        GradientDrawable d=new GradientDrawable(); d.setColor(color); d.setCornerRadius(dp(radius));
        if(stroke!=0)d.setStroke(dp(1),stroke); return d;
    }
    private TextView text(String value,int size,int color,boolean bold){
        TextView v=new TextView(this); v.setText(value); v.setTextSize(size); v.setTextColor(color);
        if(bold)v.setTypeface(Typeface.DEFAULT,Typeface.BOLD); return v;
    }
    private void gap(LinearLayout p,int h){View v=new View(this);p.addView(v,new LinearLayout.LayoutParams(1,dp(h)));}
    private LinearLayout panel(){
        LinearLayout p=new LinearLayout(this);p.setOrientation(1);p.setPadding(dp(16),dp(15),dp(16),dp(15));
        p.setBackground(shape(PANEL,18,Color.rgb(32,55,69)));return p;
    }
    private Button button(String label,boolean primary,Runnable action){
        Button b=new Button(this);b.setText(label);b.setAllCaps(false);b.setTextSize(14);
        b.setTextColor(primary?BG:Color.WHITE);b.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        b.setBackground(shape(primary?GREEN:Color.rgb(29,47,62),13,primary?GREEN:Color.rgb(48,72,88)));
        b.setPadding(dp(12),dp(7),dp(12),dp(7));b.setOnClickListener(v->action.run());return b;
    }
    private void buildUi(){
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);
        root=new LinearLayout(this);root.setOrientation(1);root.setPadding(dp(18),dp(22),dp(18),dp(28));root.setBackgroundColor(BG);
        scroll.addView(root);setContentView(scroll);
        LinearLayout header=new LinearLayout(this);header.setGravity(Gravity.CENTER_VERTICAL);
        TextView mark=text("✦",29,GREEN,true);mark.setGravity(Gravity.CENTER);mark.setBackground(shape(Color.rgb(16,54,53),16,GREEN));
        header.addView(mark,new LinearLayout.LayoutParams(dp(54),dp(54)));
        LinearLayout brand=new LinearLayout(this);brand.setOrientation(1);brand.setPadding(dp(12),0,0,0);
        brand.addView(text("AI MOBILE STUDIO",20,Color.WHITE,true));brand.addView(text("CODE • CREATE • BUILD",10,GREEN,true));header.addView(brand);root.addView(header);gap(root,20);
        LinearLayout hero=panel();hero.setBackground(shape(Color.rgb(12,43,48),20,Color.rgb(22,111,91)));
        hero.addView(text("Your idea. Your code. Your Android app.",21,Color.WHITE,true));gap(hero,7);
        hero.addView(text("Generate real project source files with AI, save them in a project workspace, then build with Android Gradle tooling.",13,MUTED,false));
        gap(hero,13);hero.addView(text("JAVA   KOTLIN   HTML   CSS   JAVASCRIPT   XML   GRADLE",11,GREEN,true));root.addView(hero);gap(root,16);
        LinearLayout projects=panel();projects.addView(text("01  PROJECT WORKSPACE",13,GREEN,true));gap(projects,8);
        selectedLabel=text("No project selected",13,MUTED,false);projects.addView(selectedLabel);gap(projects,10);
        projects.addView(button("＋  Create new Android project",true,this::newProject));gap(projects,10);
        projectList=new LinearLayout(this);projectList.setOrientation(1);projects.addView(projectList);gap(projects,4);
        projects.addView(button("View generated project files",false,this::showProjectFiles));root.addView(projects);gap(root,16);
        LinearLayout ai=panel();ai.addView(text("02  AI CODING AGENT",13,GREEN,true));gap(ai,6);
        ai.addView(text("AI returns a structured file list. The app validates paths and writes each file into the selected project.",13,MUTED,false));gap(ai,12);
        language=new Spinner(this);
        String[] langs={"Android Java","Android Kotlin","HTML + CSS + JavaScript","XML + Gradle","Flutter (Dart)","React Native (JavaScript)","C / C++","Python"};
        ArrayAdapter<String> adapter=new ArrayAdapter<>(this,android.R.layout.simple_spinner_item,langs);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);language.setAdapter(adapter);ai.addView(language);gap(ai,10);
        prompt=new EditText(this);prompt.setTextColor(Color.WHITE);prompt.setHintTextColor(MUTED);
        prompt.setHint("Describe the complete app you want to create…");prompt.setTextSize(14);prompt.setGravity(Gravity.TOP);
        prompt.setMinLines(5);prompt.setPadding(dp(13),dp(12),dp(13),dp(12));
        prompt.setBackground(shape(Color.rgb(8,19,29),13,Color.rgb(48,72,88)));ai.addView(prompt,new LinearLayout.LayoutParams(-1,-2));gap(ai,10);
        ai.addView(button("✦  Generate and write project files",true,this::generate));gap(ai,8);
        ai.addView(button("Save OpenAI API Key",false,this::saveApiKey));root.addView(ai);gap(root,16);
        LinearLayout build=panel();build.addView(text("03  BUILD & PACKAGE",13,GREEN,true));gap(build,8);
        build.addView(text("Project source generation is implemented. APK/AAB compilation and on-device installation are not yet implemented inside this app; use the included Termux build script or CI workflow for builds.",13,MUTED,false));
        gap(build,8);build.addView(button("Show project build instructions",false,this::showBuildInfo));root.addView(build);gap(root,16);
        status=text("● Ready — create a project to begin.",12,GREEN,false);status.setPadding(dp(4),dp(8),dp(4),dp(8));root.addView(status);
        TextView footer=text("AI MOBILE STUDIO  •  BUILD YOUR NEXT IDEA",10,MUTED,true);footer.setGravity(Gravity.CENTER);root.addView(footer);
    }
    private void newProject(){
        EditText name=new EditText(this);name.setSingleLine(true);name.setHint("MyAndroidApp");
        new AlertDialog.Builder(this).setTitle("Create Android project").setView(name).setPositiveButton("Create",(d,w)->{
            try{selectedProject=store.createProject(name.getText().toString());selectedLabel.setText("Selected: "+selectedProject);
                status.setText("● Android project scaffold created.");refreshProjects();}
            catch(Exception e){status.setText("Create failed: "+e.getMessage());}
        }).setNegativeButton("Cancel",null).show();
    }
    private void refreshProjects(){
        projectList.removeAllViews();
        for(String n:store.listProjects()){
            projectList.addView(button((n.equals(selectedProject)?"✓  ":"›  ")+n,false,()->{
                selectedProject=n;selectedLabel.setText("Selected: "+n);status.setText("● Project selected.");refreshProjects();
            }),new LinearLayout.LayoutParams(-1,-2));
        }
        if(projectList.getChildCount()==0)projectList.addView(text("Your projects will appear here.",12,MUTED,false));
    }
    private void saveApiKey(){
        EditText key=new EditText(this);key.setSingleLine(true);key.setHint("sk-...");key.setInputType(129);
        new AlertDialog.Builder(this).setTitle("OpenAI API key")
            .setMessage("An OpenAI API key is separate from a ChatGPT subscription and may incur API charges.")
            .setView(key).setPositiveButton("Save",(d,w)->{
                getSharedPreferences("secure_settings",MODE_PRIVATE).edit().putString("openai_api_key",key.getText().toString().trim()).apply();
                status.setText("● API key saved in app-private preferences (not encrypted).");
            }).setNegativeButton("Cancel",null).show();
    }
    private void generate(){
        if(selectedProject.isEmpty()){status.setText("Create/select a project first.");return;}
        String key=getSharedPreferences("secure_settings",MODE_PRIVATE).getString("openai_api_key","");
        if(key.isEmpty()){status.setText("Save an OpenAI API key first.");return;}
        String request=prompt.getText().toString().trim();if(request.isEmpty()){status.setText("Describe your app first.");return;}
        final String project=selectedProject, tech=String.valueOf(language.getSelectedItem());
        String instruction="You are a senior software engineer. Generate complete source files for a real project. Target: "+tech+
            ". Project: "+project+". User requirements: "+request+
            "\nReturn ONLY valid JSON, no markdown fences, with this exact schema: {\"files\":[{\"path\":\"app/src/main/...\",\"content\":\"full file content\"}],\"notes\":\"build notes\"}. "+
            "For Android Java/Kotlin include app/build.gradle, app/src/main/AndroidManifest.xml and all source/resource files. "+
            "Use relative paths only, never absolute paths or .. segments. Include full contents, not snippets. Do not claim an APK was built.";
        status.setText("● Generating source files with OpenAI…");
        worker.execute(()->{
            try{
                String raw=new OpenAIClient(key).generate(instruction);
                JSONObject result=new JSONObject(raw.trim());
                JSONArray files=result.optJSONArray("files");
                if(files==null||files.length()==0)throw new Exception("AI response has no files array. Please regenerate.");
                int saved=0;
                for(int i=0;i<files.length();i++){
                    JSONObject f=files.getJSONObject(i);String path=f.getString("path");String content=f.getString("content");
                    store.saveText(project,path,content);saved++;
                }
                final int count=saved;final String notes=result.optString("notes","No extra notes.");
                runOnUiThread(()->{
                    status.setText("● Wrote "+count+" project files. APK is not built yet.");
                    new AlertDialog.Builder(this).setTitle("Project source generated")
                        .setMessage(count+" files were written into "+project+".\n\n"+notes+"\n\nNext: inspect files and build with Gradle.")
                        .setPositiveButton("View files",(d,w)->showProjectFiles()).setNegativeButton("Close",null).show();
                });
            }catch(Exception e){runOnUiThread(()->status.setText("AI/file error: "+e.getMessage()));}
        });
    }
    private void showProjectFiles(){
        if(selectedProject.isEmpty()){status.setText("Select a project first.");return;}
        String[] files=store.listFiles(selectedProject);
        StringBuilder body=new StringBuilder();for(String f:files)body.append("• ").append(f).append("\n");
        new AlertDialog.Builder(this).setTitle(selectedProject+" — project files")
            .setMessage(body.length()==0?"No files yet.":body.toString()).setPositiveButton("Close",null).show();
    }
    private void showBuildInfo(){
        if(selectedProject.isEmpty()){status.setText("Select a project first.");return;}
        new AlertDialog.Builder(this).setTitle("Build status")
            .setMessage("Project folder: "+store.projectPath(selectedProject)+
                "\n\nThis version creates and saves source files. It does not yet contain an embedded Gradle/Android SDK build engine. Build the project with Gradle from Termux/PC or CI.")
            .setPositiveButton("OK",null).show();
    }
    @Override protected void onDestroy(){super.onDestroy();worker.shutdownNow();}
}
