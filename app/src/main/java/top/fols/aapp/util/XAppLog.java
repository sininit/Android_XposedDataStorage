package top.fols.aapp.util;

import android.util.Log;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;

/**
 * log file addres : AppDataDir/packageName/log/*
 */
public class XAppLog {
    private static final String TIME_FORMAT = new SimpleDateFormat("yyyy-MM-dd_HH.mm.ss.ms").format(System.currentTimeMillis());
    private File file;
    private String packageName;

    public XAppLog(String thisAppPackageName) {
        this.packageName = null == thisAppPackageName ?"null": thisAppPackageName;
    }

    private String getFileName() {
        return String.format("%s.txt", XAppLog.TIME_FORMAT);
    }


    private File getDir(){
        File dir = new File("/data/data/" + this.packageName + "/log/");
        return dir;
    }

    public File getFile() {
        if (null == this.file) {
            this.file = new File(this.getDir(), this.getFileName());
        }
        return this.file;
    }

    public void removeAll() {
        File dir = getDir();
        File[] list = dir.listFiles();
        if (null != list) {
            for (File f:list) {
                f.delete();
            }
        }
        dir.delete();
    }



    public void log(Object log) {
        try {
            String fContent = this.fString(log);

            File file = this.getFile();
            if (!file.exists()) {
                file.getParentFile().mkdirs();
                file.createNewFile();
            }

            StringBuilder sb = new StringBuilder();
            sb.append(String.format("%s: %s\n", time(), fContent));

            FileWriter out = new FileWriter(file, true);
            out.write(sb.toString());
            out.close();
        } catch (IOException e) {
            Log.e(this.getClass().getSimpleName(), fString(e));
        }
    }





    public static String time() {
        SimpleDateFormat dateFm = new SimpleDateFormat("[yyyy-MM-dd HH.mm.ss.ms]");
        String dateTime = dateFm.format(new java.util.Date());
        return dateTime;
    }


    public static String fString(Object content) {
        if (null == content) {
            return "" + null;
        }
        if (content instanceof Throwable) {
            Throwable e = (Throwable)content;
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw); pw.flush();
            content = sw.toString();
        }
        return content.toString();
    }






}
