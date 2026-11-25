package com.timmyquesteditor;

import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.net.Uri;
import android.view.View;
import android.widget.*;
import java.io.*;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import org.json.*;

public class MainActivity extends Activity {
    TextView status; EditText editor; Uri opened; boolean wasGzip=false; byte[] original;
    static final int OPEN=1, SAVE=2, BACKUP=3;
    public void onCreate(Bundle b){super.onCreate(b); buildUI();}
    void buildUI(){
        LinearLayout root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(20,20,20,20);
        TextView title=new TextView(this); title.setText("Timmy Quest Editor"); title.setTextSize(24); root.addView(title);
        status=new TextView(this); status.setText("Select a save file. Android may restrict access to Android/data."); root.addView(status);
        Button open=new Button(this);open.setText("Open save file");open.setOnClickListener(v->{Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.setType("*/*");i.addCategory(Intent.CATEGORY_OPENABLE);startActivityForResult(i,OPEN);});root.addView(open);
        editor=new EditText(this);editor.setGravity( topLeft()); editor.setTypeface(android.graphics.Typeface.MONOSPACE);editor.setMinLines(12);editor.setTextSize(12);root.addView(editor,new LinearLayout.LayoutParams(-1,0,1));
        Button validate=new Button(this);validate.setText("Validate JSON");validate.setOnClickListener(v->validateJson());root.addView(validate);
        Button export=new Button(this);export.setText("Export edited file");export.setOnClickListener(v->{Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT);i.setType("*/*");i.addCategory(Intent.CATEGORY_OPENABLE);i.putExtra(Intent.EXTRA_TITLE, suggestedName());startActivityForResult(i,SAVE);});root.addView(export);
        Button backup=new Button(this);backup.setText("Export original backup");backup.setOnClickListener(v->{if(original==null){status.setText("Open a file first.");return;}Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT);i.setType("*/*");i.addCategory(Intent.CATEGORY_OPENABLE);i.putExtra(Intent.EXTRA_TITLE,"backup_"+suggestedName());startActivityForResult(i,BACKUP);});root.addView(backup);
        setContentView(root);
    }
    int topLeft(){return android.view.Gravity.TOP|android.view.Gravity.LEFT;}
    String suggestedName(){return opened==null?"edited_save.json":opened.getLastPathSegment();}
    byte[] read(Uri u)throws Exception{InputStream in=getContentResolver().openInputStream(u);ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] buf=new byte[8192];int n;while((n=in.read(buf))>0)out.write(buf,0,n);in.close();return out.toByteArray();}
    byte[] ungzip(byte[] data)throws Exception{GZIPInputStream g=new GZIPInputStream(new ByteArrayInputStream(data));ByteArrayOutputStream o=new ByteArrayOutputStream();byte[] b=new byte[8192];int n;while((n=g.read(b))>0)o.write(b,0,n);g.close();return o.toByteArray();}
    byte[] gzip(byte[] data)throws Exception{ByteArrayOutputStream o=new ByteArrayOutputStream();GZIPOutputStream g=new GZIPOutputStream(o);g.write(data);g.close();return o.toByteArray();}
    protected void onActivityResult(int req,int res,Intent data){super.onActivityResult(req,res,data);if(res!=RESULT_OK||data==null)return;Uri u=data.getData();try{
        if(req==OPEN){opened=u;original=read(u);wasGzip=original.length>1&&original[0]==(byte)0x1f&&original[1]==(byte)0x8b;byte[] raw=wasGzip?ungzip(original):original;String s=new String(raw,java.nio.charset.StandardCharsets.UTF_8);editor.setText(s);status.setText("Loaded "+u.getLastPathSegment()+(wasGzip?" (gzip)":"")+" — "+raw.length+" bytes");}
        else if(req==SAVE){String s=editor.getText().toString();validateJsonOrThrow(s);byte[] raw=s.getBytes(java.nio.charset.StandardCharsets.UTF_8);byte[] out=wasGzip?gzip(raw):raw;write(u,out);status.setText("Exported edited file.");}
        else if(req==BACKUP){write(u,original);status.setText("Backup exported unchanged.");}
    }catch(Exception e){status.setText("Error: "+e.getMessage());}}
    void write(Uri u,byte[] b)throws Exception{OutputStream o=getContentResolver().openOutputStream(u);o.write(b);o.close();}
    void validateJson(){try{validateJsonOrThrow(editor.getText().toString());status.setText("JSON is valid.");}catch(Exception e){status.setText("Invalid JSON: "+e.getMessage());}}
    void validateJsonOrThrow(String s)throws Exception{String t=s.trim();if(t.startsWith("{"))new JSONObject(t);else if(t.startsWith("["))new JSONArray(t);else throw new Exception("JSON must begin with { or [");}
}