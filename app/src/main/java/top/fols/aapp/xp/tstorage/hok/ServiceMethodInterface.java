package top.fols.aapp.xp.tstorage.hok;

import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import top.fols.aapp.xp.tstorage.hok.util.ServiceReturnBigData;

public interface ServiceMethodInterface {
    //获取服务版本号
    public long getVersion() throws Throwable;

    public ServiceReturnBigData<byte[]> returnBigData() throws Throwable;




    //数据流的实现
    public String file_stream_open(String path, String mode) throws IOException;
    public boolean file_stream_contains(String token);
    public boolean file_stream_close(String token);
    public byte[] file_stream_read(String token, int len) throws IOException;
    public void file_stream_write(String token, byte[] bytes) throws IOException; 
    public long file_stream_length(String token) throws IOException; 
    public long file_stream_setlength(String token, long length) throws IOException; 
    public void file_stream_seek(String token, long index) throws IOException;
//  public boolean file_stream_isclose(String token) throws IOException;




    public String bytes_stream_open(byte[] bytes);
    public boolean bytes_stream_contains(String token);
    public boolean bytes_stream_close(String token);
    public byte[] bytes_stream_read(String token, int len) throws IOException;
    public int bytes_stream_length(String token) throws IOException;
    /** bytes_stream_ */





    //File 方法
    public boolean java_io_file_canExecute(String p0) throws Throwable;
    public boolean java_io_file_canRead(String p0) throws Throwable;
    public boolean java_io_file_canWrite(String p0) throws Throwable;
    public int java_io_file_compareTo(String p0, File p1) throws Throwable;
    public boolean java_io_file_createNewFile(String p0) throws Throwable;
    public File java_io_file_createTempFile(String p0, String p1, String p2) throws Throwable;
    public File java_io_file_createTempFile(String p0, String p1, String p2, File p3) throws Throwable;
    public boolean java_io_file_delete(String p0) throws Throwable;
    public void java_io_file_deleteOnExit(String p0) throws Throwable;
    public boolean java_io_file_equals(String p0, java.lang.Object p1) throws Throwable;
    public boolean java_io_file_exists(String p0) throws Throwable;
    public File java_io_file_getAbsoluteFile(String p0) throws Throwable;
    public String java_io_file_getAbsolutePath(String p0) throws Throwable;
    public File java_io_file_getCanonicalFile(String p0) throws Throwable;
    public String java_io_file_getCanonicalPath(String p0) throws Throwable;
    public java.lang.Class java_io_file_getClass(String p0) throws Throwable;
    public long java_io_file_getFreeSpace(String p0) throws Throwable;
    public String java_io_file_getName(String p0) throws Throwable;
    public String java_io_file_getParent(String p0) throws Throwable;
    public File java_io_file_getParentFile(String p0) throws Throwable;
    public String java_io_file_getPath(String p0) throws Throwable;
    public long java_io_file_getTotalSpace(String p0) throws Throwable;
    public long java_io_file_getUsableSpace(String p0) throws Throwable;
    public int java_io_file_hashCode(String p0) throws Throwable;
    public boolean java_io_file_isAbsolute(String p0) throws Throwable;
    public boolean java_io_file_isDirectory(String p0) throws Throwable;
    public boolean java_io_file_isFile(String p0) throws Throwable;
    public boolean java_io_file_isHidden(String p0) throws Throwable;
    public long java_io_file_lastModified(String p0) throws Throwable;
    public long java_io_file_length(String p0) throws Throwable;
    public String[] java_io_file_list(String p0) throws Throwable;
    public String[] java_io_file_list(String p0, FilenameFilter p1) throws Throwable;
    public File[] java_io_file_listFiles(String p0) throws Throwable;
    public File[] java_io_file_listFiles(String p0, FileFilter p1) throws Throwable;
    public File[] java_io_file_listFiles(String p0, FilenameFilter p1) throws Throwable;
    public File[] java_io_file_listRoots(String p0) throws Throwable;
    public boolean java_io_file_mkdir(String p0) throws Throwable;
    public boolean java_io_file_mkdirs(String p0) throws Throwable;
    public void java_io_file_notify(String p0) throws Throwable;
    public void java_io_file_notifyAll(String p0) throws Throwable;
    public boolean java_io_file_renameTo(String p0, File p1) throws Throwable;
    public boolean java_io_file_setExecutable(String p0, boolean p1) throws Throwable;
    public boolean java_io_file_setExecutable(String p0, boolean p1, boolean p2) throws Throwable;
    public boolean java_io_file_setLastModified(String p0, long p1) throws Throwable;
    public boolean java_io_file_setReadOnly(String p0) throws Throwable;
    public boolean java_io_file_setReadable(String p0, boolean p1) throws Throwable;
    public boolean java_io_file_setReadable(String p0, boolean p1, boolean p2) throws Throwable;
    public boolean java_io_file_setWritable(String p0, boolean p1) throws Throwable;
    public boolean java_io_file_setWritable(String p0, boolean p1, boolean p2) throws Throwable;
    public String java_io_file_toString(String p0) throws Throwable;
    public URI java_io_file_toURI(String p0) throws Throwable;
    public URL java_io_file_toURL(String p0) throws Throwable;
    public void java_io_file_wait(String p0) throws Throwable;
    public void java_io_file_wait(String p0, long p1) throws Throwable;
    public void java_io_file_wait(String p0, long p1, int p2) throws Throwable;



    public boolean java_io_file_mkdirs_and_setpermission(String p0) throws Throwable;

    //服务端抛出异常测试
    public void throwExceptionTest() throws Throwable;
}
