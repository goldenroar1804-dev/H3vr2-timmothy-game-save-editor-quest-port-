package com.timmyquesteditor;

import android.app.Activity;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.os.Environment;
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
    Uri treeUri; DocumentFile treeRoot;
    final int BG=Color.rgb(12,16,24), PANEL=Color.rgb(23,30,42), ACCENT=Color.rgb(65,190,220), TEXT=Color.rgb(235,242,250), MUTED=Color.rgb(155,170,188);
    public void onCreate(Bundle b){super.onCreate(b);getWindow().setStatusBarColor(BG);getWindow().setNavigationBarColor(BG);buildUI();}
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
        TextView brand=label("TIMMY  /  SAVE TOOLS",12,ACCENT,true);header.addView(brand);
        TextView title=label("Save Editor",28,TEXT,true);header.addView(title);
        TextView subtitle=label("H3VR2 • Quest companion",14,MUTED,false);header.addView(subtitle);root.addView(header);
        ScrollView scroll=new ScrollView(this);LinearLayout body=new LinearLayout(this);body.setPadding(16,16,16,24);body.setOrientation(LinearLayout.VERTICAL);scroll.addView(body);
        TextView section=label("SAVE FILE",16,TEXT,true);body.addView(section);
        Button access=button("Grant broad file access");access.setOnClickListener(v->requestBroadFileAccess());body.addView(access);
        TextView accessInfo=label("Opens Android settings to allow shared-storage access. Protected app folders may remain restricted.",12,MUTED,false);body.addView(accessInfo);
        Button access=button("🔓   Grant broad storage access");access.setOnClickListener(v->requestAllFilesAccess());body.addView(access);
        Button connect=button("⛓   Connect save folder");connect.setOnClickListener(v->{Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);startActivityForResult(i,TREE);});body.addView(connect);
        Button open=button("＋   Select save file");open.setOnClickListener(v->{Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.setType("*/*");i.addCategory(Intent.CATEGORY_OPENABLE);startActivityForResult(i,OPEN);});body.addView(open);
        status=label("No save loaded\nBroad access may help, but Android still restricts Android/data.",14,MUTED,false);status.setPadding(14,14,14,14);status.setBackground(panel());body.addView(status);
        LinearLayout tabs=new LinearLayout(this);tabs.setOrientation(LinearLayout.HORIZONTAL);tabs.setPadding(0,8,0,8);
        ScrollView tabScroll=new ScrollView(this);tabScroll.setHorizontalScrollBarEnabled(true);tabScroll.addView(tabs);body.addView(tabScroll);
        FrameLayout workspace=new FrameLayout(this);body.addView(workspace,new LinearLayout.LayoutParams(-1,-2));
        editor=new EditText(this);editor.setGravity(Gravity.TOP|Gravity.LEFT);editor.setTypeface(Typeface.MONOSPACE);editor.setMinLines(14);editor.setTextSize(13);editor.setTextColor(TEXT);editor.setHintTextColor(MUTED);editor.setHint("Load a save to edit its raw JSON");editor.setPadding(14,14,14,14);editor.setBackground(panel());
        LinearLayout rawPanel=new LinearLayout(this);rawPanel.setOrientation(LinearLayout.VERTICAL);rawPanel.addView(label("RAW JSON • edit the selected save file",15,TEXT,true));rawPanel.addView(editor,new LinearLayout.LayoutParams(-1,-2));
        Button validate=button("✓   Validate JSON");validate.setOnClickListener(v->validateJson());rawPanel.addView(validate);
        Button export=button("⇧   Export edited save");export.setOnClickListener(v->{Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT);i.setType("*/*");i.addCategory(Intent.CATEGORY_OPENABLE);i.putExtra(Intent.EXTRA_TITLE,suggestedName());startActivityForResult(i,SAVE);});rawPanel.addView(export);
        Button backup=button("⤓   Export original backup");backup.setOnClickListener(v->{if(original==null){status.setText("Open a file first.");return;}Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT);i.setType("*/*");i.addCategory(Intent.CATEGORY_OPENABLE);i.putExtra(Intent.EXTRA_TITLE,"backup_"+suggestedName());startActivityForResult(i,BACKUP);});rawPanel.addView(backup);
        LinearLayout quickPanel=categoryPanel("QUICK EDIT","Quick access to save values such as currency, resources, XP, health and statistics. The real field mapping will populate after the save structure is identified.");
        LinearLayout storePanel=categoryPanel("STORE EDIT","Browse store slots, replace items, edit prices and sections, and apply bulk pricing.");
        LinearLayout inventoryPanel=categoryPanel("INVENTORY / STORAGE","Browse inventory pages and slots, change items, duplicate or remove slots, and add items.");
        LinearLayout favoritesPanel=categoryPanel("FAVORITES & PRESETS","Manage favorite item references and reusable store or inventory presets.");
        LinearLayout treePanel=categoryPanel("ALL PARAMETERS","Search and edit the complete save-data field tree.");
        LinearLayout descPanel=categoryPanel("FIELD GUIDE","Descriptions of fields detected in the selected save file.");
        LinearLayout[] pages={quickPanel,storePanel,inventoryPanel,favoritesPanel,treePanel,rawPanel,descPanel};
        String[] titles={"Quick Edit","Store","Inventory","★ Favorites","All Fields","Raw JSON","Field Guide"};
        for(int i=0;i<pages.length;i++){final int index=i;Button tb=button(titles[i]);tb.setLayoutParams(new LinearLayout.LayoutParams(-2,-2));tb.setOnClickListener(v->{workspace.removeAllViews();workspace.addView(pages[index]);});tabs.addView(tb);}
        workspace.addView(quickPanel);
        TextView note=label("Interface structure adapted from the original H3VR2 Save Editor. Editing actions are being connected to the Quest save format; not all tabs are functional yet.",13,MUTED,false);note.setPadding(14,16,14,16);note.setBackground(panel());body.addView(note);
        root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));setContentView(root);
    }
    void requestAllFilesAccess(){ if(android.os.Build.VERSION.SDK_INT>=30){try{Intent i=new Intent(android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,Uri.parse("package:"+getPackageName()));startActivity(i);}catch(Exception e){Intent i=new Intent(android.provider.Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);startActivity(i);}}else{status.setText("Storage access is handled through Android file selection.");}}
    LinearLayout categoryPanel(String title,String description){LinearLayout p=new LinearLayout(this);p.setOrientation(LinearLayout.VERTICAL);p.setPadding(12,12,12,12);p.setBackground(panel());TextView h=label(title,19,TEXT,true);p.addView(h);TextView d=label(description,14,MUTED,false);d.setPadding(0,12,0,12);p.addView(d);TextView wait=label("Save data fields and actions will appear here when the file schema is connected.",13,MUTED,false);p.addView(wait);return p;}
    TextView label(String s,int size,int color,boolean bold){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);if(bold)t.setTypeface(null,Typeface.BOLD);return t;}
    android.graphics.drawable.Drawable panel(){android.graphics.drawable.GradientDrawable d=new android.graphics.drawable.GradientDrawable();d.setColor(PANEL);d.setCornerRadius(18);d.setStroke(1,Color.rgb(40,52,68));return d;}
    Button button(String s){Button b=new Button(this);b.setText(s);b.setTextColor(TEXT);b.setTextSize(15);b.setAllCaps(false);b.setGravity(Gravity.CENTER);b.setBackground(panel());LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.setMargins(0,10,0,10);b.setLayoutParams(lp);return b;}
    String suggestedName(){return opened==null?"edited_save.json":opened.getLastPathSegment();}
    byte[] read(Uri u)throws Exception{InputStream in=getContentResolver().openInputStream(u);ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] buf=new byte[8192];int n;while((n=in.read(buf))>0)out.write(buf,0,n);in.close();return out.toByteArray();}
    byte[] ungzip(byte[] data)throws Exception{GZIPInputStream g=new GZIPInputStream(new ByteArrayInputStream(data));ByteArrayOutputStream o=new ByteArrayOutputStream();byte[] b=new byte[8192];int n;while((n=g.read(b))>0)o.write(b,0,n);g.close();return o.toByteArray();}
    byte[] gzip(byte[] data)throws Exception{ByteArrayOutputStream o=new ByteArrayOutputStream();GZIPOutputStream g=new GZIPOutputStream(o);g.write(data);g.close();return o.toByteArray();}
    protected void onActivityResult(int req,int res,Intent data){super.onActivityResult(req,res,data);if(res!=RESULT_OK||data==null)return;Uri u=data.getData();try{
        if(req==TREE){getContentResolver().takePersistableUriPermission(u,Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION);treeUri=u;treeRoot=DocumentFile.fromTreeUri(this,u);findSaveCandidates(treeRoot);return;}
        if(req==OPEN){opened=u;original=read(u);wasGzip=original.length>1&&original[0]==(byte)0x1f&&original[1]==(byte)0x8b;byte[] raw=wasGzip?ungzip(original):original;String s=new String(raw,java.nio.charset.StandardCharsets.UTF_8);editor.setText(s);status.setText("Loaded: "+u.getLastPathSegment()+"\n"+(wasGzip?"GZIP compressed":"Uncompressed")+" • "+raw.length+" bytes");}
        else if(req==SAVE){String s=editor.getText().toString();validateJsonOrThrow(s);byte[] raw=s.getBytes(java.nio.charset.StandardCharsets.UTF_8);write(u,wasGzip?gzip(raw):raw);status.setText("Exported edited file successfully.");}
        else if(req==BACKUP){write(u,original);status.setText("Original backup exported unchanged.");}
    }catch(Exception e){status.setText("Error: "+e.getMessage());}}
    void findSaveCandidates(DocumentFile root){ java.util.ArrayList<DocumentFile> found=new java.util.ArrayList<>(); scan(root,found,0); if(found.isEmpty()){status.setText("Folder connected, but no likely save files found. Try selecting the specific profile folder.");return;} String[] names=new String[found.size()];for(int i=0;i<found.size();i++)names[i]=found.get(i).getName();new android.app.AlertDialog.Builder(this).setTitle("Choose save file").setItems(names,(d,w)->{try{DocumentFile f=found.get(w);opened=f.getUri();original=read(opened);wasGzip=original.length>1&&original[0]==(byte)0x1f&&original[1]==(byte)0x8b;byte[] raw=wasGzip?ungzip(original):original;editor.setText(new String(raw,java.nio.charset.StandardCharsets.UTF_8));status.setText("Connected: "+f.getName()+"\n"+(wasGzip?"GZIP compressed":"Uncompressed"));}catch(Exception e){status.setText("Could not read file: "+e.getMessage());}}).show();}
    void scan(DocumentFile dir,java.util.ArrayList<DocumentFile> found,int depth){if(depth>5)return;for(DocumentFile f:dir.listFiles()){if(f.isDirectory())scan(f,found,depth+1);else{String n=f.getName()==null?"":f.getName().toLowerCase();if(n.contains("save")||n.contains("profile")||n.endsWith(".json")||n.endsWith(".dat"))found.add(f);}}}
    void write(Uri u,byte[] b)throws Exception{OutputStream o=getContentResolver().openOutputStream(u);o.write(b);o.close();}
    void validateJson(){try{validateJsonOrThrow(editor.getText().toString());status.setText("JSON is valid ✓");}catch(Exception e){status.setText("Invalid JSON: "+e.getMessage());}}
    void validateJsonOrThrow(String s)throws Exception{String t=s.trim();if(t.startsWith("{"))new JSONObject(t);else if(t.startsWith("["))new JSONArray(t);else throw new Exception("JSON must begin with { or [");}
}