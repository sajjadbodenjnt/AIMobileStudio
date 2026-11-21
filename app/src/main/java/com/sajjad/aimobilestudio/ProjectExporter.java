package com.sajjad.aimobilestudio;

import android.content.Context;
import android.net.Uri;
import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public final class ProjectExporter {
    private ProjectExporter(){}
    public static void write(Context context,File project,Uri destination)throws Exception{
        try(OutputStream raw=context.getContentResolver().openOutputStream(destination);
            ZipOutputStream zip=new ZipOutputStream(raw)){
            if(raw==null)throw new IllegalStateException("Cannot open destination");
            addTree(project,project,zip);
        }
    }
    private static void addTree(File root,File current,ZipOutputStream zip)throws Exception{
        File[] children=current.listFiles();if(children==null)return;
        for(File file:children){
            if(file.isDirectory()){addTree(root,file,zip);continue;}
            String path=root.toURI().relativize(file.toURI()).getPath();
            zip.putNextEntry(new ZipEntry(path));
            try(FileInputStream in=new FileInputStream(file)){
                byte[] buffer=new byte[8192];int count;
                while((count=in.read(buffer))!=-1)zip.write(buffer,0,count);
            }
            zip.closeEntry();
        }
    }
}
