package top.fols.aapp.xp.tstorage.hok.util;

import java.io.IOException;
import java.io.Serializable;
import top.fols.aapp.xp.tstorage.hok.ServiceMethodInterface;
import top.fols.aapp.xp.tstorage.hok.ServiceMethods;
import top.fols.box.io.XStream;
import top.fols.box.io.base.XByteArrayOutputStream;

public class ServiceReturnBigData<E extends Serializable> implements Serializable {
    private boolean closed;
    private ServiceReturnBigData requireNoClose() throws IOException {
        if (closed) {
            throw new IOException("closed");
        }
        return this;
    }

    /**
     * 由服务端创建
     * 客户创建的没有屁用
     */
    private String token;
    public ServiceReturnBigData(ServiceMethods sms, E object) throws RuntimeException {
        byte[] bytes = null;

        try {
            bytes = XStream.ObjectTool.toByteArray(object);
        } catch (IOException e) {
            //???
            throw new RuntimeException(e);
        }
        this.token = sms.bytes_stream_open(bytes);
    }



//    private boolean isCache = false;
//    private Object cache = null;
    public E get(ServiceMethodInterface smi) throws IOException {
//        if (this.requireNoClose().isCache) { return (E) cache; }

        byte[] bytes = this.readBytes(smi);
        try {
            Object result = XStream.ObjectTool.toObject(bytes);
//            cache = result;
//            isCache = true;
            return (E) result;
        } catch (ClassNotFoundException e) {
            throw new IOException(e);
        } catch (IOException e) {
            throw e;
        }
    }




    public void close(ServiceMethodInterface smi) {
        this.closed = true;
//        this.isCache = false;
//        this.cache = null;
        smi.bytes_stream_close(this.token);
    }


    private byte[] readBytes(ServiceMethodInterface smi) throws IOException {
        int length = this.bytes_stream_length0(smi, this.token);
        XByteArrayOutputStream bytesout = new XByteArrayOutputStream(length);
        byte[] buf = new byte[parcel];
        int read;
        while ((read = this.read(smi, buf, 0, buf.length)) != -1) {
            bytesout.write(buf, 0, read);
        }
        byte[] bytes = bytesout.getBuff();
        bytesout.releaseBuffer();
        buf = null;
        if (bytes.length != length) {
            throw new IOException(String.format("length=%s, read=%s", length, bytes.length));
        }
        return bytes;
    }
    
    
    
    
    long index;
    int parcel = 64 * 1024;//max send buffer length
//  private byte[] bytes_stream_read0(String token, int len) throws IOException {
//     try {
//                return (byte[])client.requestInvokMethod("bytes_stream_read", token, len);
//            } catch (Throwable e) {
//                throw new IOException(e);
//            } 
//        }
    private byte[] bytes_stream_read0(ServiceMethodInterface smi, String token, int len) throws IOException {
        return smi.bytes_stream_read(this.token, len); 
    }
    private int bytes_stream_length0(ServiceMethodInterface smi, String token) throws IOException {
        return smi.bytes_stream_length(this.token); 
    }
    private int read(ServiceMethodInterface smi, byte[] p1, int off, int len) throws java.io.IOException {
        byte[] buf;
        int need = len;

        buf = this.bytes_stream_read0(smi, this.token,
            Math.min(parcel, need));
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
            buf = this.bytes_stream_read0(smi, this.token,
                Math.min(parcel, need));
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

}
