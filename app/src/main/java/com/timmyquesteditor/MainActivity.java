package com.timmyquesteditor;

import android.app.Activity;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.content.Intent;
import android.net.Uri;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import java.io.*;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import org.json.*;
import androidx.documentfile.provider.DocumentFile;
import android.content.SharedPreferences;

public class MainActivity extends Activity {
    TextView status; EditText editor; Uri opened; boolean wasGzip=false; byte[] original;
    static final int OPEN=1, SAVE=2, BACKUP=3, TREE=4;
    Uri treeUri; DocumentFile treeRoot; SharedPreferences prefs; boolean backupExported=false; java.util.ArrayList<LinearLayout> fieldPanels=new java.util.ArrayList<>();
    final int BG=Color.rgb(12,16,24), PANEL=Color.rgb(23,30,42), ACCENT=Color.rgb(65,190,220), TEXT=Color.rgb(235,242,250), MUTED=Color.rgb(155,170,188);
    public void onCreate(Bundle b){super.onCreate(b);prefs=getSharedPreferences("folder_connection",MODE_PRIVATE);getWindow().setStatusBarColor(BG);getWindow().setNavigationBarColor(BG);buildUI();restoreFolder();}
    void requestBroadFileAccess(){
        if(android.os.Build.VERSION.SDK_INT>=30){
            if(Environment.isExternalStorageManager()){status.setText("Broad file access is already granted. Protected app folders may remain restricted.");return;}
            try{Intent i=new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);i.setData(Uri.parse("package:"+getPackageName()));startActivity(i);}
            catch(Exception e){try{startActivity(new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION));}catch(Exception ex){status.setText("Could not open storage permission settings: "+ex.getMessage());}}
        }else{status.setText("Broad file access settings are not supported on this Android version.");}
    }
    void buildUI(){
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setBackgroundColor(BG);
        LinearLayout header=new LinearLayout(this);header.setPadding(20,18,20,16);header.setOrientation(LinearLayout.VERTICAL);header.setBackgroundColor(PANEL);
        header.addView(label("TIMMY QUEST EDITOR",12,ACCENT,true));header.addView(label("Save Editor",28,TEXT,true));header.addView(label("Simple mode • H3VR2 Quest",14,MUTED,false));root.addView(header);
        ScrollView scroll=new ScrollView(this);LinearLayout body=new LinearLayout(this);body.setPadding(16,16,16,24);body.setOrientation(LinearLayout.VERTICAL);scroll.addView(body);
        TextView loadTitle=label("1. LOAD YOUR SAVE",17,TEXT,true);body.addView(loadTitle);
        Button open=button("📂  Choose save file");open.setOnClickListener(v->{Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.setType("*/*");i.addCategory(Intent.CATEGORY_OPENABLE);startActivityForResult(i,OPEN);});body.addView(open);
        Button connect=button("Find saves in a folder (optional)");connect.setOnClickListener(v->{Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);startActivityForResult(i,TREE);});body.addView(connect);
        status=label("No save loaded. Choose your save file to begin.",14,MUTED,false);status.setPadding(14,14,14,14);status.setBackground(panel());body.addView(status);
        LinearLayout tabs=new LinearLayout(this);tabs.setOrientation(LinearLayout.HORIZONTAL);tabs.setPadding(0,8,0,8);ScrollView tabScroll=new ScrollView(this);tabScroll.setHorizontalScrollBarEnabled(true);tabScroll.addView(tabs);body.addView(tabScroll);
        FrameLayout workspace=new FrameLayout(this);body.addView(workspace,new LinearLayout.LayoutParams(-1,-2));
        LinearLayout quickPanel=spawnMenuPanel();
        LinearLayout savePanel=new LinearLayout(this);savePanel.setOrientation(LinearLayout.VERTICAL);savePanel.setPadding(12,12,12,12);savePanel.setBackground(panel());savePanel.addView(label("SAVE & BACKUP",19,TEXT,true));
        Button backup=button("1. Export original backup");backup.setOnClickListener(v->{if(original==null){status.setText("Load a save first.");return;}Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT);i.setType("*/*");i.addCategory(Intent.CATEGORY_OPENABLE);i.putExtra(Intent.EXTRA_TITLE,"backup_"+suggestedName());startActivityForResult(i,BACKUP);});savePanel.addView(backup);
        Button export=button("2. Export edited save");export.setOnClickListener(v->{if(editor.getText().toString().trim().isEmpty()){status.setText("Load a save first.");return;}Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT);i.setType("*/*");i.addCategory(Intent.CATEGORY_OPENABLE);i.putExtra(Intent.EXTRA_TITLE,suggestedName());startActivityForResult(i,SAVE);});savePanel.addView(export);
        Button directSave=button("Overwrite original (advanced)");directSave.setOnClickListener(v->saveBackToOpened());savePanel.addView(directSave);
        LinearLayout advanced=new LinearLayout(this);advanced.setOrientation(LinearLayout.VERTICAL);advanced.setPadding(12,12,12,12);advanced.setBackground(panel());advanced.addView(label("ADVANCED TOOLS",19,TEXT,true));
        LinearLayout storePanel=fieldPanel("STORE FIELDS",false);LinearLayout inventoryPanel=fieldPanel("INVENTORY FIELDS",false);LinearLayout treePanel=fieldPanel("ALL FIELDS",false);LinearLayout rawPanel=new LinearLayout(this);rawPanel.setOrientation(LinearLayout.VERTICAL);rawPanel.addView(label("RAW JSON",16,TEXT,true));editor=new EditText(this);editor.setGravity(Gravity.TOP|Gravity.LEFT);editor.setTypeface(Typeface.MONOSPACE);editor.setMinLines(10);editor.setTextSize(13);editor.setTextColor(TEXT);editor.setHintTextColor(MUTED);editor.setHint("Raw save data appears here after loading.");editor.setPadding(14,14,14,14);editor.setBackground(panel());rawPanel.addView(editor,new LinearLayout.LayoutParams(-1,-2));Button validate=button("Validate JSON");validate.setOnClickListener(v->validateJson());rawPanel.addView(validate);advanced.addView(storePanel);advanced.addView(inventoryPanel);advanced.addView(treePanel);advanced.addView(rawPanel);
        LinearLayout[] pages={quickPanel,savePanel,advanced};String[] names={"✎  Edit","💾  Save","⚙  Advanced"};for(int i=0;i<pages.length;i++){final int ix=i;Button tb=button(names[i]);tb.setLayoutParams(new LinearLayout.LayoutParams(-2,-2));tb.setOnClickListener(v->{workspace.removeAllViews();workspace.addView(pages[ix]);});tabs.addView(tb);}
        workspace.addView(quickPanel);TextView tip=label("Start with Quick Edit. Always export a backup before changing your save.",13,MUTED,false);tip.setPadding(12,14,12,14);body.addView(tip);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));setContentView(root);
    }
    LinearLayout spawnMenuPanel(){
        LinearLayout p=new LinearLayout(this);p.setOrientation(LinearLayout.VERTICAL);p.setPadding(12,12,12,12);p.setBackground(panel());
        p.addView(label("ITEM MENU",20,TEXT,true));p.addView(label("Search and browse item references",13,MUTED,false));
        EditText search=new EditText(this);search.setSingleLine(true);search.setHint("Search items...");search.setTextColor(TEXT);search.setHintTextColor(MUTED);search.setBackground(panel());p.addView(search);
        LinearLayout cats=new LinearLayout(this);cats.setOrientation(LinearLayout.HORIZONTAL);p.addView(cats);
        ScrollView listScroll=new ScrollView(this);LinearLayout list=new LinearLayout(this);list.setOrientation(LinearLayout.VERTICAL);listScroll.addView(list);p.addView(listScroll,new LinearLayout.LayoutParams(-1,500));
        String[] category={"All","Weapons","Ammo","Magazines","Explosives","Equipment","Power-ups","Misc"};
        String[] ids={"extref://footlong.base/Grenade_TechCryo","extref://footlong.base/Round_9x19mm_FMJ_115gr","extref://footlong.base/Round_45ACP_FMJTracer_M1","extref://footlong.base/Grenade_RGO"};
        String[] names={"Tech Cryo Grenade","9x19mm FMJ 115gr","45 ACP FMJ Tracer M1","RGO Grenade"};
        String[] types={"Explosives","Ammo","Ammo","Explosives"};final String[] selected={"All"};
        Runnable refresh=()->{list.removeAllViews();int count=0;String q=search.getText().toString().trim().toLowerCase(java.util.Locale.ROOT);for(int i=0;i<names.length;i++){if(!selected[0].equals("All")&&!selected[0].equals(types[i]))continue;if(!names[i].toLowerCase(java.util.Locale.ROOT).contains(q)&&!ids[i].toLowerCase(java.util.Locale.ROOT).contains(q))continue;Button item=button(names[i]);item.setOnClickListener(v->new android.app.AlertDialog.Builder(MainActivity.this).setTitle(names[i]).setMessage("Reference ID:\\n"+ids[i]+"\\n\\nThis reference was found in your uploaded VendOS save. Adding/spawning it requires verified save structure.").setPositiveButton("OK",null).show());list.addView(item);count++;}if(count==0)list.addView(label("No matching items in the currently verified item references.",14,MUTED,false));};
        for(String c:category){Button b=button(c);cats.addView(b);b.setOnClickListener(v->{selected[0]=c.equals("All")?"All":c;refresh.run();});}
        search.addTextChangedListener(new android.text.TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int c,int a){}public void onTextChanged(CharSequence s,int st,int before,int count){refresh.run();}public void afterTextChanged(android.text.Editable e){}});refresh.run();
        p.addView(label("Items shown are verified references found in the provided save files. This is a catalogue view; item injection is not yet validated.",12,MUTED,false));return p;
    }
    void requestAllFilesAccess(){ if(android.os.Build.VERSION.SDK_INT>=30){try{Intent i=new Intent(android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,Uri.parse("package:"+getPackageName()));startActivity(i);}catch(Exception e){Intent i=new Intent(android.provider.Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);startActivity(i);}}else{status.setText("Storage access is handled through Android file selection.");}}
    void saveBackToOpened(){if(opened==null){status.setText("Select a save file first.");return;}if(original==null){status.setText("Original file data unavailable; reload before saving.");return;}if(!backupExported){new android.app.AlertDialog.Builder(this).setTitle("Backup required").setMessage("Export an unchanged backup of the original save before overwriting it. Use the Export original backup button, then try saving again.").setPositiveButton("OK",null).show();status.setText("Save blocked: export an original backup first.");return;}new android.app.AlertDialog.Builder(this).setTitle("Overwrite selected save?").setMessage("This will overwrite the selected file. A backup was exported during this session.").setPositiveButton("Overwrite",(d,w)->{try{validateJsonOrThrow(editor.getText().toString());byte[] raw=editor.getText().toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);write(opened,wasGzip?gzip(raw):raw);status.setText("Saved changes to selected file.");backupExported=false;}catch(Exception e){status.setText("Save failed: "+e.getMessage());}}).setNegativeButton("Cancel",null).show();}
    LinearLayout fieldPanel(String title,boolean quick){LinearLayout p=new LinearLayout(this);fieldPanels.add(p);p.setOrientation(LinearLayout.VERTICAL);p.setPadding(12,12,12,12);p.setBackground(panel());p.addView(label(title,19,TEXT,true));EditText filter=new EditText(this);filter.setSingleLine(true);filter.setTextColor(TEXT);filter.setHintTextColor(MUTED);filter.setHint("Filter fields…");filter.setTextSize(14);filter.setPadding(12,10,12,10);filter.setBackground(panel());p.addView(filter);filter.addTextChangedListener(new android.text.TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int c,int a){}public void onTextChanged(CharSequence s,int st,int before,int count){populateFields(p,title.equals("QUICK EDIT"));}public void afterTextChanged(android.text.Editable e){}});populateFields(p,quick);return p;}
    Object parseRoot()throws Exception{String t=editor.getText().toString().trim();if(t.startsWith("{"))return new JSONObject(t);if(t.startsWith("["))return new JSONArray(t);throw new Exception("JSON root must be an object or array");}
    void populateFields(LinearLayout p,boolean quick){
        while(p.getChildCount()>2)p.removeViewAt(2);
        try{
            if(editor.getText().toString().trim().isEmpty()){p.addView(label("Load a save file first.",13,MUTED,false));return;}
            Object root=parseRoot();java.util.ArrayList<String> paths=new java.util.ArrayList<>();collectPaths(root,"",paths);
            String panelTitle=((TextView)p.getChildAt(0)).getText().toString().toLowerCase(java.util.Locale.ROOT);
            if(quick||panelTitle.contains("store")){
                java.util.ArrayList<String> filtered=new java.util.ArrayList<>();
                String[] terms=quick?new String[]{"money","cash","currency","xp","experience","health","ammo","resource","score","stat","level","credit","gold","meat","metal","mustard"}:new String[]{"store","box","sectionid","price","purchased","quantity","money","meat","metal","mustard","experience"};
                for(String path:paths){String low=path.toLowerCase(java.util.Locale.ROOT);boolean match=false;for(String term:terms)if(low.contains(term)){match=true;break;}if(match)filtered.add(path);}
                paths=filtered;
            }else if(panelTitle.contains("inventory")){
                java.util.ArrayList<String> filtered=new java.util.ArrayList<>();
                String[] terms={"inventorypages","dumpqueue","wearables","pockets","holdinginhandleft","holdinginhandright","gridsize","items","contents","objects","itemdef","affixments","instanceseed","rectmin","rectsize","position","rotation","transform"};
                for(String path:paths){String low=path.toLowerCase(java.util.Locale.ROOT);boolean match=false;for(String term:terms)if(low.contains(term)){match=true;break;}if(match)filtered.add(path);}
                paths=filtered;
            }
            EditText filter=(EditText)p.getChildAt(1);String query=filter==null?"":filter.getText().toString().trim().toLowerCase(java.util.Locale.ROOT);
            if(!query.isEmpty()){java.util.ArrayList<String> matching=new java.util.ArrayList<>();for(String path:paths)if(path.toLowerCase(java.util.Locale.ROOT).contains(query))matching.add(path);paths=matching;}
            java.util.Collections.sort(paths,String.CASE_INSENSITIVE_ORDER);int total=paths.size();if(paths.size()>250)paths=new java.util.ArrayList<>(paths.subList(0,250));
            p.addView(label(total+" editable leaf fields found",12,MUTED,false));
            for(String path:paths){Button b=button(path+"  ✎");b.setTextSize(12);b.setOnClickListener(v->editPath(path));p.addView(b);}
            if(paths.isEmpty())p.addView(label(quick?"No matching quick-edit fields.":"No matching fields in this save file.",13,MUTED,false));
            else if(total>250)p.addView(label("Showing 250 of "+total+" fields. Filter to narrow the list.",12,MUTED,false));
        }catch(Exception e){p.addView(label("Load valid JSON to populate this field list.",13,MUTED,false));}
    }
    void collectPaths(Object node,String prefix,java.util.ArrayList<String> out)throws Exception{
        if(node instanceof JSONObject){JSONObject o=(JSONObject)node;java.util.Iterator<String> it=o.keys();while(it.hasNext()){String k=it.next();String path=prefix.isEmpty()?k:prefix+"."+k;collectPaths(o.get(k),path,out);}}
        else if(node instanceof JSONArray){JSONArray a=(JSONArray)node;for(int i=0;i<a.length();i++)collectPaths(a.get(i),prefix+"["+i+"]",out);}
        else if(!prefix.isEmpty())out.add(prefix);
    }
    java.util.ArrayList<String> pathTokens(String path){java.util.ArrayList<String> tokens=new java.util.ArrayList<>();java.util.regex.Matcher m=java.util.regex.Pattern.compile("([^\\.\\[\\]]+)|\\[(\\d+)\\]").matcher(path);while(m.find())tokens.add(m.group(1)!=null?m.group(1):m.group(2));return tokens;}
    Object getPath(String path)throws Exception{Object cur=parseRoot();for(String token:pathTokens(path)){if(cur instanceof JSONObject)cur=((JSONObject)cur).get(token);else if(cur instanceof JSONArray)cur=((JSONArray)cur).get(Integer.parseInt(token));else throw new Exception("Path is not a container");}return cur;}
    void editPath(String path){try{Object val=getPath(path);final EditText input=new EditText(this);final Switch boolInput=new Switch(this);View dialogView;if(val instanceof Boolean){boolInput.setText("Value");boolInput.setChecked((Boolean)val);dialogView=boolInput;}else{input.setText(String.valueOf(val));if(val instanceof Number)input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER|android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL|android.text.InputType.TYPE_NUMBER_FLAG_SIGNED);dialogView=input;}new android.app.AlertDialog.Builder(this).setTitle(path).setView(dialogView).setPositiveButton("Apply",(d,w)->{try{Object root=parseRoot();setPath(root,path,val instanceof Boolean?(boolInput.isChecked()?"true":"false"):input.getText().toString(),val);editor.setText(root instanceof JSONObject?((JSONObject)root).toString(2):((JSONArray)root).toString(2));status.setText("Field updated in editor. Save/export to write it.");refreshFieldPanels();}catch(Exception e){status.setText("Field update failed: "+e.getMessage());}}).setNegativeButton("Cancel",null).show();}catch(Exception e){status.setText("Could not open field: "+e.getMessage());}}
    void setPath(Object root,String path,String text,Object old)throws Exception{java.util.ArrayList<String> parts=pathTokens(path);if(parts.isEmpty())throw new Exception("Empty field path");Object cur=root;for(int i=0;i<parts.size()-1;i++){String t=parts.get(i);cur=cur instanceof JSONObject?((JSONObject)cur).get(t):((JSONArray)cur).get(Integer.parseInt(t));}String key=parts.get(parts.size()-1);Object value;if(old instanceof Number){java.math.BigDecimal n;try{n=new java.math.BigDecimal(text.trim());}catch(Exception e){throw new Exception("Enter a valid number");}if(old instanceof Integer){try{value=n.intValueExact();}catch(Exception e){throw new Exception("Whole number required (integer range)");}}else if(old instanceof Long){try{value=n.longValueExact();}catch(Exception e){throw new Exception("Whole number required (long range)");}}else if(old instanceof Float)value=n.floatValue();else value=n.doubleValue();}else if(old instanceof Boolean){if(!text.equalsIgnoreCase("true")&&!text.equalsIgnoreCase("false"))throw new Exception("Enter true or false");value=Boolean.parseBoolean(text);}else if(old==JSONObject.NULL){String t=text.trim();if(t.equalsIgnoreCase("null"))value=JSONObject.NULL;else value=text;}else value=text;if(cur instanceof JSONObject)((JSONObject)cur).put(key,value);else ((JSONArray)cur).put(Integer.parseInt(key),value);}
    void refreshFieldPanels(){for(LinearLayout p:fieldPanels){if(p.getChildCount()==0)continue;Object h=p.getChildAt(0);boolean quick=h instanceof TextView && ((TextView)h).getText().toString().equals("QUICK EDIT");populateFields(p,quick);}}
    LinearLayout categoryPanel(String title,String description){LinearLayout p=new LinearLayout(this);p.setOrientation(LinearLayout.VERTICAL);p.setPadding(12,12,12,12);p.setBackground(panel());TextView h=label(title,19,TEXT,true);p.addView(h);TextView d=label(description,14,MUTED,false);d.setPadding(0,12,0,12);p.addView(d);TextView wait=label("Save data fields and actions will appear here when the file schema is connected.",13,MUTED,false);p.addView(wait);return p;}
    TextView label(String s,int size,int color,boolean bold){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);if(bold)t.setTypeface(null,Typeface.BOLD);return t;}
    android.graphics.drawable.Drawable panel(){android.graphics.drawable.GradientDrawable d=new android.graphics.drawable.GradientDrawable();d.setColor(PANEL);d.setCornerRadius(18);d.setStroke(1,Color.rgb(40,52,68));return d;}
    Button button(String s){Button b=new Button(this);b.setText(s);b.setTextColor(TEXT);b.setTextSize(15);b.setAllCaps(false);b.setGravity(Gravity.CENTER);b.setBackground(panel());LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.setMargins(0,10,0,10);b.setLayoutParams(lp);return b;}
    String suggestedName(){return opened==null?"edited_save.json":opened.getLastPathSegment();}
    byte[] read(Uri u)throws Exception{InputStream in=getContentResolver().openInputStream(u);ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] buf=new byte[8192];int n;while((n=in.read(buf))>0)out.write(buf,0,n);in.close();return out.toByteArray();}
    byte[] ungzip(byte[] data)throws Exception{GZIPInputStream g=new GZIPInputStream(new ByteArrayInputStream(data));ByteArrayOutputStream o=new ByteArrayOutputStream();byte[] b=new byte[8192];int n;while((n=g.read(b))>0)o.write(b,0,n);g.close();return o.toByteArray();}
    byte[] gzip(byte[] data)throws Exception{ByteArrayOutputStream o=new ByteArrayOutputStream();GZIPOutputStream g=new GZIPOutputStream(o);g.write(data);g.close();return o.toByteArray();}
    protected void onActivityResult(int req,int res,Intent data){super.onActivityResult(req,res,data);if(res!=RESULT_OK||data==null)return;Uri u=data.getData();try{
        if(req==TREE){getContentResolver().takePersistableUriPermission(u,Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION);treeUri=u;treeRoot=DocumentFile.fromTreeUri(this,u);prefs.edit().putString("tree_uri",u.toString()).apply();status.setText("Folder connected. Scanning subfolders…");findSaveCandidates(treeRoot);return;}
        if(req==OPEN){backupExported=false;opened=u;original=read(u);wasGzip=original.length>1&&original[0]==(byte)0x1f&&original[1]==(byte)0x8b;byte[] raw=wasGzip?ungzip(original):original;String s=new String(raw,java.nio.charset.StandardCharsets.UTF_8);editor.setText(s);refreshFieldPanels();status.setText("Loaded: "+u.getLastPathSegment()+"\n"+(wasGzip?"GZIP compressed":"Uncompressed")+" • "+raw.length+" bytes");}
        else if(req==SAVE){String s=editor.getText().toString();validateJsonOrThrow(s);byte[] raw=s.getBytes(java.nio.charset.StandardCharsets.UTF_8);write(u,wasGzip?gzip(raw):raw);status.setText("Exported edited file successfully.");}
        else if(req==BACKUP){write(u,original);backupExported=true;status.setText("Original backup exported unchanged. Direct overwrite is now unlocked for this session.");}
    }catch(Exception e){status.setText("Error: "+e.getMessage());}}

    void restoreFolder(){String saved=prefs.getString("tree_uri",null);if(saved!=null){try{treeUri=Uri.parse(saved);treeRoot=DocumentFile.fromTreeUri(this,treeUri);status.setText("Previously connected folder restored. Tap Scan connected folder again to search.");}catch(Exception e){status.setText("Saved folder connection could not be restored. Connect it again.");}}}
    void findSaveCandidates(DocumentFile root){ java.util.ArrayList<DocumentFile> found=new java.util.ArrayList<>(); scan(root,found,0); if(found.isEmpty()){status.setText("Folder scanned, but no likely save files found. Try selecting the profile/save folder itself.");return;} String[] names=new String[found.size()];for(int i=0;i<found.size();i++){DocumentFile f=found.get(i);names[i]=(f.getName()==null?"(unnamed)":f.getName())+" • "+(f.length()/1024)+" KB";}new android.app.AlertDialog.Builder(this).setTitle("Save files found ("+found.size()+")").setItems(names,(d,w)->{try{DocumentFile f=found.get(w);backupExported=false;opened=f.getUri();original=read(opened);wasGzip=original.length>1&&original[0]==(byte)0x1f&&original[1]==(byte)0x8b;byte[] raw=wasGzip?ungzip(original):original;editor.setText(new String(raw,java.nio.charset.StandardCharsets.UTF_8));refreshFieldPanels();status.setText("Loaded: "+f.getName()+"\\n"+(wasGzip?"GZIP compressed":"Uncompressed")+" • "+raw.length+" bytes");}catch(Exception e){status.setText("Could not read file: "+e.getMessage());}}).show();status.setText("Scan complete: "+found.size()+" candidate files.");}
    void scan(DocumentFile dir,java.util.ArrayList<DocumentFile> found,int depth){if(dir==null||depth>5)return;DocumentFile[] children=dir.listFiles();if(children==null)return;for(DocumentFile f:children){if(f==null)continue;if(f.isDirectory())scan(f,found,depth+1);else if(f.isFile()){String n=f.getName()==null?"":f.getName().toLowerCase(java.util.Locale.ROOT);boolean named=n.contains("save")||n.contains("profile");boolean dataExt=n.endsWith(".json")||n.endsWith(".dat")||n.endsWith(".sav")||n.endsWith(".gz");if(named||dataExt)found.add(f);}}}
    void write(Uri u,byte[] b)throws Exception{if(u==null)throw new Exception("No destination selected");OutputStream o=getContentResolver().openOutputStream(u,"wt");if(o==null)throw new Exception("Storage provider did not open a write stream");try{o.write(b);o.flush();}finally{o.close();}}
    void validateJson(){try{validateJsonOrThrow(editor.getText().toString());status.setText("JSON is valid ✓");}catch(Exception e){status.setText("Invalid JSON: "+e.getMessage());}}
    void validateJsonOrThrow(String s)throws Exception{String t=s.trim();if(t.startsWith("{"))new JSONObject(t);else if(t.startsWith("["))new JSONArray(t);else throw new Exception("JSON must begin with { or [");}
}