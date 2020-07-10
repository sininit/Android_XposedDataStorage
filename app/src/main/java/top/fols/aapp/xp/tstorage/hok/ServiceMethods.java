package top.fols.aapp.xp.tstorage.hok;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import top.fols.aapp.xp.tstorage.hok.util.ServiceReturnBigData;
import top.fols.box.io.XStream;
import top.fols.box.io.base.XByteArrayInputStream;
import top.fols.box.io.os.XFile;
import top.fols.box.time.XTimeTool;
import java.io.Serializable;

/**
 * 这些方法是由服务端执行并返回给客户端的
 * 由于只有一个实例 所以 static 和非static 没什么区别
 * 参数和返回数据 必须可以序列化
 * 由于安卓的限制 序列化后的数据~不能大于512K 在降低版本上 200k可能都会出错！
 */
public class ServiceMethods implements ServiceMethodInterface {

    private <E extends Serializable> ServiceReturnBigData<E> newServiceReturnBigData(E object) {
        return new ServiceReturnBigData<E>(this, object);
    }



    @Override
    public ServiceReturnBigData<byte[]> returnBigData() throws Throwable {
        return newServiceReturnBigData(new byte[64 * 1024 * 1024]);
    }







    @Override
    public long getVersion() throws Throwable {
        return XposedDataStorageClient.SERVICE_VERSION;
    }




    //自动清理器 (如果长时间不访问)
    long acCheckTime = 10 * XTimeTool.time_1s;//每次检测时间
    long acTime = 10 * XTimeTool.time_1s;//对象超时
    AutoClearThread acThread = new AutoClearThread(){{ start(); }};
    static class AcValue<E extends Object> {
        E content;
        long time;

        public AcValue(E content, long time) {
            this.content = content;
            this.time = time;
        }

        void clear() { this.content = null; }
    }
    class AutoClearThread extends Thread {
        private ServiceMethods s = ServiceMethods.this;

        AutoClearThread() {
            acThread = this;
        }
        long current() {
            long currentt; currentt = System.currentTimeMillis(); currentt = currentt <= 0 ?System.currentTimeMillis(): currentt;
            return currentt;
        }

        /** * @see file_stream_ */
        void clear_file_stream_(long currentt) {
            List<String> closeList = new ArrayList<>();
            for (String tok: this.s.file_stream_map.keySet()) {
                AcValue<RandomAccessFile> ac = this.s.file_stream_map.get(tok);
                long lastt = ac.time;
                if (currentt - lastt >= acTime) {
                    closeList.add(tok);
                }
            }
            for (String tok: closeList) {
                this.s.file_stream_close(tok);
            }
            closeList = null;
        }

        /** * @see bytes_stream_ */
        void clear_bytes_stream_(long currentt) {
            List<String> closeList = new ArrayList<>();
            for (String tok: this.s.bytes_stream_map.keySet()) {
                AcValue<XByteArrayInputStream> ac = this.s.bytes_stream_map.get(tok);
                long lastt = ac.time;
                if (currentt - lastt >= acTime) {
                    closeList.add(tok);
                }
            }
            for (String tok: closeList) {
                this.s.bytes_stream_close(tok);
            }
            closeList = null;
        }


        @Override
        public void run() {
            while (true) {
                try {
                    this.sleep(acCheckTime);
                    long currentt = this.current();

                    try { this.clear_file_stream_(currentt); } catch (Throwable e) { XposedDataStorageClient.ANDROID_LOG.log(e); }
                    try { this.clear_bytes_stream_(currentt); } catch (Throwable e) { XposedDataStorageClient.ANDROID_LOG.log(e); }
                } catch (InterruptedException e) {
                    break;
                } catch (Throwable e) {
                    break;
                }
            }

            new AutoClearThread(){{ start(); }};
        }
    }






    /** file_stream_ */
    Object file_stream_sync = new Object();
    long file_stream_id = 0;
    Map<String, AcValue<RandomAccessFile>> file_stream_map = new ConcurrentHashMap<>();
    void file_stream_access_top(String token) {
        AcValue<RandomAccessFile> ac = this.file_stream_map.get(token);
        if (null != ac) {
            ac.time = System.currentTimeMillis();
            //XposedDataStorageClient.LOG.log("access: "+token);
        }
    }
    /**
     * @return token
     */
    @Override
    public String file_stream_open(String path, String mode) throws FileNotFoundException {
        synchronized (this.file_stream_sync) {
            String token = null;
            while (null == token || this.file_stream_map.containsKey(token)) {
                token = new StringBuilder().
                    append(path).
                    append("_").
                    append(new Object().hashCode()).//random memory addres
                    append("_").
                    append(file_stream_id++).//stream id
                    toString();
            }
            RandomAccessFile raf = new RandomAccessFile(path, mode);
            this.file_stream_map.put(token, new AcValue<RandomAccessFile>(raf, System.currentTimeMillis()));
            //XposedDataStorageClient.LOG.log("open: "+token);
            return token;
        }
    }
    @Override
    public boolean file_stream_contains(String token) {
        synchronized (this.file_stream_sync) {
            boolean contains = this.file_stream_map.containsKey(token);
            this.file_stream_access_top(token);
            return contains;
        }
    }
    @Override
    public boolean file_stream_close(String token) {
        synchronized (this.file_stream_sync) {
            boolean result = false;
            AcValue<RandomAccessFile> ac = this.file_stream_map.get(token);
            RandomAccessFile raf = ac.content;
            if (null != raf) {
                XStream.tryClose(raf);
                result = true;
            }
            if (null != ac) {
                ac.clear();
            }
            this.file_stream_map.remove(token);
            //XposedDataStorageClient.LOG.log("close: "+token);
            return result;
        }
    }




    RandomAccessFile file_stream_getraf0(String token) throws IOException {
        AcValue<RandomAccessFile> ac = this.file_stream_map.get(token);
        if (null == ac) {
            throw new IOException("closed or no open");
        }
        RandomAccessFile raf = null == ac ?null: ac.content;
        this.file_stream_access_top(token);
        return raf;
    }


    @Override
    public byte[] file_stream_read(String token, int len) throws IOException {
        RandomAccessFile raf = this.file_stream_getraf0(token);
        byte[] buf = new byte[len];
        int read = raf.read(buf);
        if (read == -1 || read == 0) {
            return null;
        } else {
            byte[] readBytes = Arrays.copyOf(buf, read); buf = null;
            return readBytes;
        }
    }
    @Override
    public void file_stream_write(String token, byte[] bytes) throws IOException {
        RandomAccessFile raf = this.file_stream_getraf0(token);
        raf.write(bytes);
    }
    @Override
    public long file_stream_length(String token) throws IOException {
        RandomAccessFile raf = this.file_stream_getraf0(token);
        return raf.length();
    }
    @Override
    public long file_stream_setlength(String token, long length) throws IOException {
        RandomAccessFile raf = this.file_stream_getraf0(token);
        raf.setLength(length);
        return raf.length();
    }
    @Override
    public void file_stream_seek(String token, long index) throws IOException {
        RandomAccessFile raf = this.file_stream_getraf0(token);
        raf.seek(index);
    }
    /** file_stream_ */














    /**
     * 自动清理 并且读完自动清理
     */
    /** bytes_stream_ */
    private Object bytes_stream_sync = new Object();
    private long bytes_stream_id = 0;
    private Map<String, AcValue<XByteArrayInputStream>> bytes_stream_map = new ConcurrentHashMap<>();
    private void bytes_stream_access_top(String token) {
        AcValue<XByteArrayInputStream> ac = this.bytes_stream_map.get(token);
        if (null != ac) {
            ac.time = System.currentTimeMillis();
            //XposedDataStorageClient.LOG.log("access: "+token);
        }
    }
    /**
     * @return token
     */
    @Override
    public String bytes_stream_open(byte[] bytes)  {
        synchronized (this.bytes_stream_sync) {
            String token = null;
            while (null == token || this.bytes_stream_map.containsKey(token)) {
                token = new StringBuilder().
                    append(bytes.hashCode()).
                    append("-").
                    append(new Object().hashCode()).//random memory addres
                    append("_").
                    append(bytes_stream_id++).//stream id
                    toString();
            }
            XByteArrayInputStream raf = new XByteArrayInputStream(bytes);
            this.bytes_stream_map.put(token, new AcValue<XByteArrayInputStream>(raf, System.currentTimeMillis()));
            //XposedDataStorageClient.LOG.log("open: "+token);
            return token;
        }
    }

    @Override
    public boolean bytes_stream_contains(String token) {
        synchronized (this.bytes_stream_sync) {
            boolean contains = this.bytes_stream_map.containsKey(token);
            this.bytes_stream_access_top(token);
            return contains;
        }
    }

    @Override
    public boolean bytes_stream_close(String token) {
        synchronized (this.bytes_stream_sync) {
            boolean result = false;
            AcValue<XByteArrayInputStream> ac = this.bytes_stream_map.get(token);
            XByteArrayInputStream raf = ac.content;
            if (null != raf) {
                XStream.tryClose(raf);
                raf.releaseBuffer();
                result = true;
            }
            if (null != ac) {
                ac.clear();
            }
            this.bytes_stream_map.remove(token);
            //XposedDataStorageClient.LOG.log("close: "+token);
            return result;
        }
    }

    XByteArrayInputStream bytes_stream_getraf0(String token) {
        AcValue<XByteArrayInputStream> ac = this.bytes_stream_map.get(token);
        if (null == ac) {
            return null;
        }
        XByteArrayInputStream raf = null == ac ?null: ac.content;
        this.bytes_stream_access_top(token);
        return raf;
    }

    @Override
    public byte[] bytes_stream_read(String token, int len) throws IOException {
        XByteArrayInputStream raf = this.bytes_stream_getraf0(token);
        if (null == raf) {
            return null;
        }
        byte[] buf = new byte[len];
        int read = raf.read(buf);
        if (read == -1 || read == 0) {
            return null;
        } else {
            byte[] readBytes = Arrays.copyOf(buf, read); buf = null;
            if (raf.getIndex() >= raf.size()) {
                this.bytes_stream_close(token);
            }
            return readBytes;
        }
    }

    @Override
    public int bytes_stream_length(String token) throws IOException {
        XByteArrayInputStream raf = this.bytes_stream_getraf0(token);
        if (null == raf) {
            throw new IOException("closed or no open");
        }
        return raf.size();
    }
    /** bytes_stream_ */
















    @Override
    public boolean java_io_file_canExecute(java.lang.String p0) throws Throwable {    java.io.File ins = new java.io.File(p0);     boolean result = ins.canExecute();     return result; }
    @Override
    public boolean java_io_file_canRead(java.lang.String p0) throws Throwable {    java.io.File ins = new java.io.File(p0);     boolean result = ins.canRead();     return result; }
    @Override
    public boolean java_io_file_canWrite(java.lang.String p0) throws Throwable {    java.io.File ins = new java.io.File(p0);     boolean result = ins.canWrite();     return result; }
    @Override
    public int java_io_file_compareTo(java.lang.String p0, java.io.File p1) throws Throwable {    java.io.File ins = new java.io.File(p0);     int result = ins.compareTo(p1);     return result; }
    @Override
    public boolean java_io_file_createNewFile(java.lang.String p0) throws Throwable {    java.io.File ins = new java.io.File(p0);     boolean result = ins.createNewFile();     return result; }
    @Override
    public java.io.File java_io_file_createTempFile(java.lang.String p0, java.lang.String p1, java.lang.String p2) throws Throwable {    java.io.File ins = new java.io.File(p0);     java.io.File result = ins.createTempFile(p1, p2);     return result; }
    @Override
    public java.io.File java_io_file_createTempFile(java.lang.String p0, java.lang.String p1, java.lang.String p2, java.io.File p3) throws Throwable {    java.io.File ins = new java.io.File(p0);     java.io.File result = ins.createTempFile(p1, p2, p3);     return result; }
    @Override
    public boolean java_io_file_delete(java.lang.String p0) throws Throwable {    java.io.File ins = new java.io.File(p0);     boolean result = ins.delete();     return result; }
    @Override
    public void java_io_file_deleteOnExit(java.lang.String p0) throws Throwable {    java.io.File ins = new java.io.File(p0);     ins.deleteOnExit(); }
    @Override
    public boolean java_io_file_equals(java.lang.String p0, java.lang.Object p1) throws Throwable {    java.io.File ins = new java.io.File(p0);     boolean result = ins.equals(p1);     return result; }
    @Override
    public boolean java_io_file_exists(java.lang.String p0) throws Throwable {    java.io.File ins = new java.io.File(p0);     boolean result = ins.exists();     return result; }
    @Override
    public java.io.File java_io_file_getAbsoluteFile(java.lang.String p0) throws Throwable {    java.io.File ins = new java.io.File(p0);     java.io.File result = ins.getAbsoluteFile();     return result; }
    @Override
    public java.lang.String java_io_file_getAbsolutePath(java.lang.String p0) throws Throwable {    java.io.File ins = new java.io.File(p0);     java.lang.String result = ins.getAbsolutePath();     return result; }
    @Override
    public java.io.File java_io_file_getCanonicalFile(java.lang.String p0) throws Throwable {    java.io.File ins = new java.io.File(p0);     java.io.File result = ins.getCanonicalFile();     return result; }
    @Override
    public java.lang.String java_io_file_getCanonicalPath(java.lang.String p0) throws Throwable {    java.io.File ins = new java.io.File(p0);     java.lang.String result = ins.getCanonicalPath();     return result; }
    @Override
    public java.lang.Class java_io_file_getClass(java.lang.String p0) throws Throwable {    java.io.File ins = new java.io.File(p0);     java.lang.Class result = ins.getClass();     return result; }
    @Override
    public long java_io_file_getFreeSpace(java.lang.String p0) throws Throwable {    java.io.File ins = new java.io.File(p0);     long result = ins.getFreeSpace();     return result; }
    @Override
    public java.lang.String java_io_file_getName(java.lang.String p0) throws Throwable {    java.io.File ins = new java.io.File(p0);     java.lang.String result = ins.getName();     return result; }
    @Override
    public java.lang.String java_io_file_getParent(java.lang.String p0) throws Throwable {    java.io.File ins = new java.io.File(p0);     java.lang.String result = ins.getParent();     return result; }
    @Override
    public java.io.File java_io_file_getParentFile(java.lang.String p0) throws Throwable {    java.io.File ins = new java.io.File(p0);     java.io.File result = ins.getParentFile();     return result; }
    @Override
    public java.lang.String java_io_file_getPath(java.lang.String p0) throws Throwable {    java.io.File ins = new java.io.File(p0);     java.lang.String result = ins.getPath();     return result; }
    @Override
    public long java_io_file_getTotalSpace(java.lang.String p0) throws Throwable {    java.io.File ins = new java.io.File(p0);     long result = ins.getTotalSpace();     return result; }
    @Override
    public long java_io_file_getUsableSpace(java.lang.String p0) throws Throwable {    java.io.File ins = new java.io.File(p0);     long result = ins.getUsableSpace();     return result; }
    @Override
    public int java_io_file_hashCode(java.lang.String p0) throws Throwable {    java.io.File ins = new java.io.File(p0);     int result = ins.hashCode();     return result; }
    @Override
    public boolean java_io_file_isAbsolute(java.lang.String p0) throws Throwable {    java.io.File ins = new java.io.File(p0);     boolean result = ins.isAbsolute();     return result; }
    @Override
    public boolean java_io_file_isDirectory(java.lang.String p0) throws Throwable {    java.io.File ins = new java.io.File(p0);     boolean result = ins.isDirectory();     return result; }
    @Override
    public boolean java_io_file_isFile(java.lang.String p0) throws Throwable {    java.io.File ins = new java.io.File(p0);     boolean result = ins.isFile();     return result; }
    @Override
    public boolean java_io_file_isHidden(java.lang.String p0) throws Throwable {    java.io.File ins = new java.io.File(p0);     boolean result = ins.isHidden();     return result; }
    @Override
    public long java_io_file_lastModified(java.lang.String p0) throws Throwable {    java.io.File ins = new java.io.File(p0);     long result = ins.lastModified();     return result; }
    @Override
    public long java_io_file_length(java.lang.String p0) throws Throwable {    java.io.File ins = new java.io.File(p0);     long result = ins.length();     return result; }
    @Override
    public java.lang.String[] java_io_file_list(java.lang.String p0) throws Throwable {    java.io.File ins = new java.io.File(p0);     java.lang.String[] result = ins.list();     return result; }
    @Override
    public java.lang.String[] java_io_file_list(java.lang.String p0, java.io.FilenameFilter p1) throws Throwable {    java.io.File ins = new java.io.File(p0);     java.lang.String[] result = ins.list(p1);     return result; }
    @Override
    public java.io.File[] java_io_file_listFiles(java.lang.String p0) throws Throwable {    java.io.File ins = new java.io.File(p0);     java.io.File[] result = ins.listFiles();     return result; }
    @Override
    public java.io.File[] java_io_file_listFiles(java.lang.String p0, java.io.FileFilter p1) throws Throwable {    java.io.File ins = new java.io.File(p0);     java.io.File[] result = ins.listFiles(p1);     return result; }
    @Override
    public java.io.File[] java_io_file_listFiles(java.lang.String p0, java.io.FilenameFilter p1) throws Throwable {    java.io.File ins = new java.io.File(p0);     java.io.File[] result = ins.listFiles(p1);     return result; }
    @Override
    public java.io.File[] java_io_file_listRoots(java.lang.String p0) throws Throwable {    java.io.File ins = new java.io.File(p0);     java.io.File[] result = ins.listRoots();     return result; }
    @Override
    public boolean java_io_file_mkdir(java.lang.String p0) throws Throwable {    java.io.File ins = new java.io.File(p0);     boolean result = ins.mkdir();     return result; }
    @Override
    public boolean java_io_file_mkdirs(java.lang.String p0) throws Throwable {    java.io.File ins = new java.io.File(p0);     boolean result = ins.mkdirs();     return result; }
    @Override
    public void java_io_file_notify(java.lang.String p0) throws Throwable {    java.io.File ins = new java.io.File(p0);     ins.notify(); }
    @Override
    public void java_io_file_notifyAll(java.lang.String p0) throws Throwable {    java.io.File ins = new java.io.File(p0);     ins.notifyAll(); }
    @Override
    public boolean java_io_file_renameTo(java.lang.String p0, java.io.File p1) throws Throwable {    java.io.File ins = new java.io.File(p0);     boolean result = ins.renameTo(p1);     return result; }
    @Override
    public boolean java_io_file_setExecutable(java.lang.String p0, boolean p1) throws Throwable {    java.io.File ins = new java.io.File(p0);     boolean result = ins.setExecutable(p1);     return result; }
    @Override
    public boolean java_io_file_setExecutable(java.lang.String p0, boolean p1, boolean p2) throws Throwable {    java.io.File ins = new java.io.File(p0);     boolean result = ins.setExecutable(p1, p2);     return result; }
    @Override
    public boolean java_io_file_setLastModified(java.lang.String p0, long p1) throws Throwable {    java.io.File ins = new java.io.File(p0);     boolean result = ins.setLastModified(p1);     return result; }
    @Override
    public boolean java_io_file_setReadOnly(java.lang.String p0) throws Throwable {    java.io.File ins = new java.io.File(p0);     boolean result = ins.setReadOnly();     return result; }
    @Override
    public boolean java_io_file_setReadable(java.lang.String p0, boolean p1) throws Throwable {    java.io.File ins = new java.io.File(p0);     boolean result = ins.setReadable(p1);     return result; }
    @Override
    public boolean java_io_file_setReadable(java.lang.String p0, boolean p1, boolean p2) throws Throwable {    java.io.File ins = new java.io.File(p0);     boolean result = ins.setReadable(p1, p2);     return result; }
    @Override
    public boolean java_io_file_setWritable(java.lang.String p0, boolean p1) throws Throwable {    java.io.File ins = new java.io.File(p0);     boolean result = ins.setWritable(p1);     return result; }
    @Override
    public boolean java_io_file_setWritable(java.lang.String p0, boolean p1, boolean p2) throws Throwable {    java.io.File ins = new java.io.File(p0);     boolean result = ins.setWritable(p1, p2);     return result; }
    @Override
    public java.lang.String java_io_file_toString(java.lang.String p0) throws Throwable {    java.io.File ins = new java.io.File(p0);     java.lang.String result = ins.toString();     return result; }
    @Override
    public java.net.URI java_io_file_toURI(java.lang.String p0) throws Throwable {    java.io.File ins = new java.io.File(p0);     java.net.URI result = ins.toURI();     return result; }
    @Override
    public java.net.URL java_io_file_toURL(java.lang.String p0) throws Throwable {    java.io.File ins = new java.io.File(p0);     java.net.URL result = ins.toURL();     return result; }
    @Override
    public void java_io_file_wait(java.lang.String p0) throws Throwable {    java.io.File ins = new java.io.File(p0);     ins.wait(); }
    @Override
    public void java_io_file_wait(java.lang.String p0, long p1) throws Throwable {    java.io.File ins = new java.io.File(p0);     ins.wait(p1); }
    @Override
    public void java_io_file_wait(java.lang.String p0, long p1, int p2) throws Throwable {    java.io.File ins = new java.io.File(p0);     ins.wait(p1, p2); }


    @Override
    public boolean java_io_file_mkdirs_and_setpermission(String p0) throws Throwable {
        java.io.File ins = new java.io.File(p0);
        boolean result = true;
        if (!ins.exists()) {
            result = result & ins.mkdirs();
        }
        XFile.openFile(ins, false);
        return result;
    }









    @Override
    public void throwExceptionTest() throws Throwable {
        throw new RuntimeException("？？？");
    }



















    static final ServiceMethods DEFAULT_INSTANCE = new ServiceMethods();
    ServiceMethods() {}
}
