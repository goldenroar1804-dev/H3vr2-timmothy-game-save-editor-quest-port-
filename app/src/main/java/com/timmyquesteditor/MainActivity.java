package com.timmyquesteditor;

import android.app.Activity;
import android.os.Bundle;
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

public class MainActivity extends Activity {
    TextView status; EditText editor; Uri opened; boolean wasGzip=false; byte[] original;
    static final int OPEN=1, SAVE=2, BACKUP=3;
    final int BG=Color.rgb(12,16,24), PANEL=Color.rgb(23,30,42), ACCENT=Color.rgb(65,190,220), TEXT=Color.rgb(235,242,250), MUTED=Color.rgb(155,170,188);
    public void onCreate(Bundle b){super.onCreate(b);getWindow().setStatusBarColor(BG);getWindow().setNavigationBarColor(BG);buildUI();}
    void buildUI(){
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setBackgroundColor(BG);
        LinearLayout header=new LinearLayout(this);header.setPadding(20,18,20,16);header.setOrientation(LinearLayout.VERTICAL);header.setBackgroundColor(PANEL);
        TextView brand=label("TIMMY  /  SAVE TOOLS",12,ACCENT,true);header.addView(brand);
        TextView title=label("Save Editor",28,TEXT,true);header.addView(title);
        TextView subtitle=label("H3VR2 • Quest companion",14,MUTED,false);header.addView(subtitle);root.addView(header);
        ScrollView scroll=new ScrollView(this);LinearLayout body=new LinearLayout(this);body.setPadding(16,16,16,24);body.setOrientation(LinearLayout.VERTICAL);scroll.addView(body);
        TextView section=label("SAVE FILE",16,TEXT,true);body.addView(section);
        Button open=button("＋   Select save file");open.setOnClickListener(v->{Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.setType("*/*");i.addCategory(Intent.CATEGORY_OPENABLE);startActivityForResult(i,OPEN);});body.addView(open);
        status=label("No save loaded\nChoose a file to begin. Android may restrict access to Android/data.",14,MUTED,false);status.setPadding(14,14,14,14);status.setBackground(panel());body.addView(status);
        TextView editTitle=label("RAW DATA WORKSPACE",16,TEXT,true);editTitle.setPadding(0,24,0,10);body.addView(editTitle);
        editor=new EditText(this);editor.setGravity(Gravity.TOP|Gravity.LEFT);editor.setTypeface(Typeface.MONOSPACE);editor.setMinLines(14);editor.setTextSize(13);editor.setTextColor(TEXT);editor.setHintTextColor(MUTED);editor.setHint("File contents will appear here…");editor.setPadding(14,14,14,14);editor.setBackground(panel());body.addView(editor,new LinearLayout.LayoutParams(-1,-2));
        Button validate=button("✓   Validate JSON");validate.setOnClickListener(v->validateJson());body.addView(validate);
        Button export=button("⇧   Export edited save");export.setOnClickListener(v->{Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT);i.setType("*/*");i.addCategory(Intent.CATEGORY_OPENABLE);i.putExtra(Intent.EXTRA_TITLE,suggestedName());startActivityForResult(i,SAVE);});body.addView(export);
        Button backup=button("⤓   Export original backup");backup.setOnClickListener(v->{if(original==null){status.setText("Open a file first.");return;}Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT);i.setType("*/*");i.addCategory(Intent.CATEGORY_OPENABLE);i.putExtra(Intent.EXTRA_TITLE,"backup_"+suggestedName());startActivityForResult(i,BACKUP);});body.addView(backup);
        TextView note=label("CUSTOM EDITING CATEGORIES\nPlayer, inventory, unlocks and other controls will be added after the save format is identified.",13,MUTED,false);note.setPadding(14,16,14,16);note.setBackground(panel());body.addView(note);
        root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));setContentView(root);
    }
    TextView label(String s,int size,int color,boolean bold){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);if(bold)t.setTypeface(null,Typeface.BOLD);return t;}
    android.graphics.drawable.Drawable panel(){android.graphics.drawable.GradientDrawable d=new android.graphics.drawable.GradientDrawable();d.setColor(PANEL);d.setCornerRadius(18);d.setStroke(1,Color.rgb(40,52,68));return d;}
    Button button(String s){Button b=new Button(this);b.setText(s);b.setTextColor(TEXT);b.setTextSize(15);b.setAllCaps(false);b.setGravity(Gravity.CENTER);b.setBackground(panel());LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.setMargins(0,10,0,10);b.setLayoutParams(lp);return b;}
    String suggestedName(){return opened==null?"edited_save.json":opened.getLastPathSegment();}
    byte[] read(Uri u)throws Exception{InputStream in=getContentResolver().openInputStream(u);ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] buf=new byte[8192];int n;while((n=in.read(buf))>0)out.write(buf,0,n);in.close();return out.toByteArray();}
    byte[] ungzip(byte[] data)throws Exception{GZIPInputStream g=new GZIPInputStream(new ByteArrayInputStream(data));ByteArrayOutputStream o=new ByteArrayOutputStream();byte[] b=new byte[8192];int n;while((n=g.read(b))>0)o.write(b,0,n);g.close();return o.toByteArray();}
    byte[] gzip(byte[] data)throws Exception{ByteArrayOutputStream o=new ByteArrayOutputStream();GZIPOutputStream g=new GZIPOutputStream(o);g.write(data);g.close();return o.toByteArray();}
    protected void onActivityResult(int req,int res,Intent data){super.onActivityResult(req,res,data);if(res!=RESULT_OK||data==null)return;Uri u=data.getData();try{
        if(req==OPEN){opened=u;original=read(u);wasGzip=original.length>1&&original[0]==(byte)0x1f&&original[1]==(byte)0x8b;byte[] raw=wasGzip?ungzip(original):original;String s=new String(raw,java.nio.charset.StandardCharsets.UTF_8);editor.setText(s);status.setText("Loaded: "+u.getLastPathSegment()+"\n"+(wasGzip?"GZIP compressed":"Uncompressed")+" • "+raw.length+" bytes");}
        else if(req==SAVE){String s=editor.getText().toString();validateJsonOrThrow(s);byte[] raw=s.getBytes(java.nio.charset.StandardCharsets.UTF_8);write(u,wasGzip?gzip(raw):raw);status.setText("Exported edited file successfully.");}
        else if(req==BACKUP){write(u,original);status.setText("Original backup exported unchanged.");}
    }catch(Exception e){status.setText("Error: "+e.getMessage());}}
    void write(Uri u,byte[] b)throws Exception{OutputStream o=getContentResolver().openOutputStream(u);o.write(b);o.close();}
    void validateJson(){try{validateJsonOrThrow(editor.getText().toString());status.setText("JSON is valid ✓");}catch(Exception e){status.setText("Invalid JSON: "+e.getMessage());}}
    void validateJsonOrThrow(String s)throws Exception{String t=s.trim();if(t.startsWith("{"))new JSONObject(t);else if(t.startsWith("["))new JSONArray(t);else throw new Exception("JSON must begin with { or [");}
}