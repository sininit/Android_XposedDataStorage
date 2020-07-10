package top.fols.aapp.xp.tstorage.hok.util;

import android.os.RemoteException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import top.fols.aapp.xp.tstorage.hok.ServiceMethodInterface;
import top.fols.aapp.xp.tstorage.hok.XposedDataStorageClient;
import top.fols.box.io.XStream;
import top.fols.box.io.base.XByteArrayInputStream;
import top.fols.box.io.os.XFile;
import top.fols.box.statics.XStaticFixedValue;

public class XposedDataStorage {


    /**
     * 如果出现异常则判断是否被清理了或者没有打开，如果是则重新打开再执行一次
     */
    public static class RandomAccessFileInputStream extends top.fols.box.io.abstracts.XAbstractRandomAccessInputStream {
        String token;
        String path; 
        String mode = XStaticFixedValue.FileOptMode.r();
        XposedDataStorageClient client;
        ServiceMethodInterface smi;

        private long index;
        private long length;
        private long reOff = 0;

        int parcel = 64 * 1024;//max send buffer length



//        private byte[] file_stream_read(String token, int len) throws IOException {
//            try {
//                return (byte[])client.requestInvokMethod("file_stream_read", token, len);
//            } catch (Throwable e) {
//                throw new IOException(e);
//            } 
//        }

        private byte[] file_stream_read0(int len) throws IOException {
            try {
                return this.smi.file_stream_read(this.token, Math.min(parcel, len));
            } catch (Throwable e) {
                if (this.smi.file_stream_contains(this.token) == false) {
                    this.reopen0();
                    return this.smi.file_stream_read(this.token, Math.min(parcel, len));
                }
                if (e instanceof IOException) {
                    throw (IOException)e;
                } else {
                    throw new IOException(e);
                }
            } 
        }
        private void file_stream_seek0(long index)throws IOException {
            try {
                this.smi.file_stream_seek(this.token, index);
            } catch (Throwable e) {
                if (this.smi.file_stream_contains(this.token) == false) {
                    this.reopen0();
                    this.smi.file_stream_seek(this.token, index);
                    return;
                }
                if (e instanceof IOException) {
                    throw (IOException)e;
                } else {
                    throw new IOException(e);
                }
            }
        }

        private long file_stream_length0()throws IOException {
            try {
                return this.smi.file_stream_length(this.token);
            } catch (Throwable e) {
                if (this.smi.file_stream_contains(this.token) == false) {
                    this.reopen0();
                    return this.smi.file_stream_length(this.token);
                }
                if (e instanceof IOException) {
                    throw (IOException)e;
                } else {
                    throw new IOException(e);
                }
            }
        }
        private void file_stream_close0() {
            this.smi.file_stream_close(this.token);
        }
        private void reopen0() throws IOException {
            this.token = this.smi.file_stream_open(this.path, this.mode);
            this.length = this.smi.file_stream_length(this.token);
            this.smi.file_stream_seek(this.token, this.index);
        }







        public RandomAccessFileInputStream(XposedDataStorageClient client, String path) throws FileNotFoundException, IOException, NoSuchMethodException, InstantiationException, SecurityException, RemoteException, ClassNotFoundException, IllegalArgumentException, InvocationTargetException, IllegalAccessException, Throwable {
            this.client = client;
            this.smi = this.client.getMethodsInterface();

            this.path = path;
            this.index = 0;
            this.reopen0();
        }

        public String getToken() {
            return this.token;
        }

        /**
         * Reads the next byte of data from this input stream. The value
         * byte is returned as an <code>int</code> in the range
         * <code>0</code> to <code>255</code>. If no byte is available
         * because the end of the stream has been reached, the value
         * <code>-1</code> is returned.
         * <p>
         * This <code>read</code> method
         * cannot block.
         *
         * @return  the next byte of data, or <code>-1</code> if the end of the
         *          stream has been reached.
         */
        @Override
        public int read() throws java.io.IOException {
            byte[] one = new byte[1];
            if (this.read(one) == -1) {
                return -1;
            }
            return one[0] & 0xff;//
        }

        @Override
        public int read(byte[] b) throws IOException {
            return this.read(b, 0, b.length);
        }

        @Override
        public int read(byte[] p1, int off, int len) throws java.io.IOException {
            byte[] buf;
            int need = len;

            buf = this.file_stream_read0(Math.min(parcel, need));
            if (null == buf || buf.length == 0) {
                return -1;
            }
            this.index += buf.length;
            System.arraycopy(buf, 0, p1, off, buf.length);
            off += buf.length;
            need -= buf.length;

            if (need <= 0) {
                return len;
            }

            int lj = buf.length;
            while (need > 0) {
                buf = this.file_stream_read0(Math.min(parcel, need));
                if (null == buf || buf.length == 0) {
                    return lj;
                }
                this.index += buf.length;
                System.arraycopy(buf, 0, p1, off, buf.length);
                off += buf.length;
                need -= buf.length;
                lj += buf.length;
            }
            return lj;
        }



        public int available() {
            long available =  this.length - this.index;
            if (available > Integer.MAX_VALUE) {
                return Integer.MAX_VALUE;
            }
            return (int)available;
        }

        public void close() {
            this.file_stream_close0();
        }

        public long skip(long n) throws java.io.IOException {
            if (n == 0) {
                return 0;
            }
            if (n < 0) {
                throw new IOException("skip bytes lengths error:" + n);
            }
            long length = this.length;
            if (n + this.index > length) {
                n = length - this.index;
            }
            this.index += n;
            this.seekIndex(this.index);
            return n;
        }

        public synchronized void mark(int p1) {
            this.reOff = this.index;
        }

        public void reset() throws java.io.IOException {
            this.seekIndex(this.reOff);
        }

        public boolean markSupported() {
            return true;
        }

        public long getIndex() {
            return this.index;
        }

        public long length() {
            return this.length;
        }

        public void seekIndex(long p1) throws java.io.IOException {
            this.file_stream_seek0(p1);
            this.index = p1;
        }

        public void reloadMessage() throws IOException, Throwable {
            this.length = this.file_stream_length0();
        }
    }

    /**
     * 如果出现异常则判断是否被清理了或者没有打开，如果是则重新打开再执行一次
     */
    public static class RandomAccessFileOutputStream extends top.fols.box.io.abstracts.XAbstractRandomAccessOutputStream {
        String token;
        String path; 
        String mode = XStaticFixedValue.FileOptMode.rw();
        XposedDataStorageClient client;
        ServiceMethodInterface smi;




        private long index;
        private long length;

        int parcel = 64 * 1024;////max send buffer length

//        private void file_stream_write(String token, byte[] buf) throws IOException {
//            try {
//                client.requestInvokMethod("file_stream_write", token, buf);
//            } catch (Throwable e) {
//                throw new IOException(e);
//            }
//        }

        private void file_stream_write0(byte[] bytes)throws IOException {
            try {
                this.smi.file_stream_write(this.token, bytes);
            } catch (Throwable e) {
                if (this.smi.file_stream_contains(this.token) == false) {
                    this.reopen0();
                    this.smi.file_stream_write(this.token, bytes);
                    return;
                }
                if (e instanceof IOException) {
                    throw (IOException)e;
                } else {
                    throw new IOException(e);
                }
            }
        }
        private void file_stream_seek0(long index)throws IOException {
            try {
                this.smi.file_stream_seek(this.token, index);
            } catch (Throwable e) {
                if (this.smi.file_stream_contains(this.token) == false) {
                    this.reopen0();
                    this.smi.file_stream_seek(this.token, index);
                    return;
                }
                if (e instanceof IOException) {
                    throw (IOException)e;
                } else {
                    throw new IOException(e);
                }
            }
        }

        private long file_stream_length0()throws IOException {
            try {
                return this.smi.file_stream_length(this.token);
            } catch (Throwable e) {
                if (this.smi.file_stream_contains(this.token) == false) {
                    this.reopen0();
                    return this.smi.file_stream_length(this.token);
                }
                if (e instanceof IOException) {
                    throw (IOException)e;
                } else {
                    throw new IOException(e);
                }
            }
        }
        private void file_stream_setlength0(long length)throws IOException {
            try {
                this.smi.file_stream_setlength(this.token, length);
            } catch (Throwable e) {
                if (this.smi.file_stream_contains(this.token) == false) {
                    this.reopen0();
                    this.smi.file_stream_setlength(this.token, length);
                    return;
                }
                if (e instanceof IOException) {
                    throw (IOException)e;
                } else {
                    throw new IOException(e);
                }
            }
        }
        private void file_stream_close0() {
            this.smi.file_stream_close(token);
        }
        private void reopen0() throws IOException {
            this.token = this.smi.file_stream_open(this.path, this.mode);
            this.length = this.smi.file_stream_length(this.token);
            this.smi.file_stream_seek(this.token, this.index);
        }




        public RandomAccessFileOutputStream(XposedDataStorageClient client, String path) throws FileNotFoundException, IOException, Throwable {
            this.client = client;
            this.smi = client.getMethodsInterface();

            this.path = path;
            this.index = 0;
            this.reopen0();
        }

        public String getToken() {
            return this.token;
        }

        public void write(int p1) throws java.io.IOException {
            this.write(new byte[]{(byte)p1});
        }

        @Override
        public void write(byte[] b) throws IOException {
            this.write(b, 0, b.length);
        }

        public void write(byte[] b, int off, int len) throws java.io.IOException {
            if (b == null) {
                throw new NullPointerException();
            } else if (off < 0 || len < 0 || len > b.length - off) {
                throw new IndexOutOfBoundsException(String.format("bytes.length=%s, off=%s, %s=len", b.length, off, len));
            }

            XByteArrayInputStream input = new XByteArrayInputStream(b, off, len);
            byte[] buf = new byte[this.parcel];
            int read;
            while ((read = input.read(buf)) != -1) {
                if (buf.length != read) {
                    buf = Arrays.copyOf(buf, read);
                }
                try {
                    this.file_stream_write0(buf);
                } catch (Throwable e) {
                    throw new IOException(e);
                }
            }
            this.index += len;

            buf = null;
            input.close();
        }

        public void flush() {
            return;
        }

        public void close() {
            this.file_stream_close0();
        }

        public long getIndex() {
            return this.index;
        }

        public void seekIndex(long p1) throws java.io.IOException {
            this.file_stream_seek0(p1);
            this.index = p1;
        }

        public long length() {
            return this.length;
        }

        public void setLength(long p1) throws java.io.IOException {
            this.file_stream_setlength0(p1);
            this.length = p1;
            if (this.index > p1) {
                this.seekIndex(p1);
            }
        }

        public void reloadMessage() throws IOException, Throwable {
            this.length = this.file_stream_length0();
        }
    }









    private static final String ROOT_DIR = "/data/system/xposed_storage";

    private String dataDirPath;
    private String packageDataDirPath;

    public static final String DIR_SHARED_PREFS = "shared_prefs";
    public static final String DIR_FILES = "files";

    private String packageName;
    private XposedDataStorageClient client;
    private ServiceMethodInterface smi;
    public XposedDataStorage(XposedDataStorageClient client , String packageName) throws Throwable {
        this.dataDirPath = ROOT_DIR;
        this.packageName = packageName;
        this.packageDataDirPath = new File(this.dataDirPath, this.packageName).getPath();

        this.client = client;
        this.smi = client.getMethodsInterface();
        this.mkdirsAbsolutePath0(this.ROOT_DIR);
        this.mkdirsAbsolutePath0(this.packageDataDirPath);
        this.mkdirsPackageOtherDir();
    }

    private void mkdirsPackageOtherDir() throws Throwable {
        this.mkdirsAbsolutePath0(this.getSubFileAbsolutePath0(DIR_SHARED_PREFS));
        this.mkdirsAbsolutePath0(this.getSubFileAbsolutePath0(DIR_FILES));
    }


    private void mkdirsAbsolutePath0(String dirpath) throws Throwable {
        smi.java_io_file_mkdirs_and_setpermission(dirpath);
    }




    public XposedDataStorageClient getClient() {
        return this.client;
    }





    public boolean existsAbsoluteFile(File file) {
        try {
            return this.smi.java_io_file_exists(file.getAbsolutePath());
        } catch (Throwable e) {
            return false;
        }
    }
    public boolean canReadAbsoluteFile(File file) {
        try {
            return this.smi.java_io_file_canRead(file.getAbsolutePath());
        } catch (Throwable e) {
            return false;
        }
    }
    public boolean canWriteAbsoluteFile(File file) {
        try {
            return this.smi.java_io_file_canWrite(file.getAbsolutePath());
        } catch (Throwable e) {
            return false;
        }
    }
    public boolean canExecuteAbsoluteFile(File file) {
        try {
            return this.smi.java_io_file_canExecute(file.getAbsolutePath());
        } catch (Throwable e) {
            return false;
        }
    }
    public File getAbsoluteFile(String subFile) {
        String absolutelyPath = this.getSubFileAbsolutePath0(subFile);
        return new File(absolutelyPath);
    }









    private String getSubFileAbsolutePath0(String subpath) {
        if (null == subpath) {
            return this.packageDataDirPath;
        } else {
            String subFile = XFile.getCanonicalPath(subpath);
            File absFile = new File(this.packageDataDirPath, subFile);
            return absFile.getPath();
        }
    }



    public String[] list() throws Throwable {
        return this.list(File.separator);
    }

    public String[] list(String subFile) throws Throwable {
        return this.smi.java_io_file_list(this.getSubFileAbsolutePath0(subFile));
    }

    public void mkdirs(String subFile) throws Throwable {
        this.smi.java_io_file_mkdirs(this.getSubFileAbsolutePath0(subFile));
    }

    public boolean exists(String subFile) throws Throwable {
        return this.smi.java_io_file_exists(this.getSubFileAbsolutePath0(subFile));
    }

    public void delete(String subFile) throws Throwable {
        this.smi.java_io_file_delete(this.getSubFileAbsolutePath0(subFile));
    }

    public long length(String subFile) throws Throwable {
        return this.smi.java_io_file_length(this.getSubFileAbsolutePath0(subFile));
    }

    public long lastModified(String subFile) throws Throwable {
        return this.smi.java_io_file_lastModified(this.getSubFileAbsolutePath0(subFile));
    }

    public boolean createNewFile(String subFile) throws Throwable {
        return this.smi.java_io_file_createNewFile(this.getSubFileAbsolutePath0(subFile));
    }

    public boolean isFile(String subFile) throws Throwable {
        return this.smi.java_io_file_isFile(this.getSubFileAbsolutePath0(subFile));
    }

    public boolean renameTo(String subFile, String newFile) throws Throwable {
        return this.smi.java_io_file_renameTo(this.getSubFileAbsolutePath0(subFile), new File(this.getSubFileAbsolutePath0(subFile)));
    }

    public void deleteAll() throws Throwable {
        this.deletes(File.separator);
    }
    public void deletes(String subFile) throws Throwable {
        if (null == subFile) {
            subFile = File.separator;
        }
        String[] names = this.list(subFile);
        if (null != names) {
            for (String fn: names) {
                String p = XFile.getCanonicalPath(subFile, fn);
                if (isFile(p)) {
                    delete(p);
                } else {
                    deletes(p);
                }
            }
        }
        delete(subFile);
    }



    public String readToString(String subFile) throws Throwable {
        RandomAccessFileInputStream input = this.getSubFileRandomInputStream(subFile);  //命令服务器打开数据流
        try {
            long fileLength = input.length();
            if (fileLength > Integer.MAX_VALUE - 8) {
                throw new OutOfMemoryError("fileSize=" + fileLength);
            }
            return XStream.InputStreamTool.toString(input);
        } finally {
            input.close();
        }
    }

    public void writeToString(String subFile, String content) throws Throwable {
        RandomAccessFileOutputStream output = this.getSubFileRandomOutputStream(subFile);
        try {
            byte[] bytes = content.getBytes();
            output.write(bytes);
            output.flush();
            output.setLength(bytes.length);
        } finally {
            output.close();
        }
    }




    public RandomAccessFileInputStream getSubFileRandomInputStream(String subFile) throws IOException, Throwable {
        String absolutelyPath = this.getSubFileAbsolutePath0(subFile);
        return new RandomAccessFileInputStream(this.client, absolutelyPath);
    }
    public RandomAccessFileOutputStream getSubFileRandomOutputStream(String subFile) throws Throwable {
        String absolutelyPath = this.getSubFileAbsolutePath0(subFile);
        return new RandomAccessFileOutputStream(this.client, absolutelyPath);
    }


    public RandomAccessFileInputStream getAbsolutePathFileRandomInputStream(File absolutelyPath) throws IOException, Throwable {
        return new RandomAccessFileInputStream(this.client, absolutelyPath.getPath());
    }
    public RandomAccessFileOutputStream getAbsolutePathFileRandomOutputStream(File absolutelyPath) throws Throwable {
        return new RandomAccessFileOutputStream(this.client, absolutelyPath.getPath());
    }







}
