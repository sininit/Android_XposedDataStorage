package top.fols.aapp.xp.tstorage.hok;

import android.content.Context;
import android.os.IBinder;
import android.os.RemoteException;
import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import top.fols.aapp.util.XAppLog;
import top.fols.aapp.xp.tstorage.R;
import top.fols.box.io.XStream;
import top.fols.box.lang.reflect.optdeclared.XReflectAccessibleInherit;

//import android.os.RemoteException;

/**
 * 为了使其它也是用该模块的作者能够稳定运行 请不要修改这个类
 */
public class XposedDataStorageClient {
    //请填写自己的包名, 不要和别人的模块包名一样，为了解决多个模块都用本服务
    public static final String SERVICE_ONLY_KEY = R.class.getPackage().getName();
    public static final long SERVICE_VERSION = 1;//版本号



    public static final String ANDROID_PACKAGE_NAME = "android";
    public static final XAppLog ANDROID_LOG = new XAppLog(ANDROID_PACKAGE_NAME) {{ removeAll(); }};


    public static final int SERVICE_ONTRANSACT_REQUEST_CODE = -Math.abs('G' | 'X' | 'N' | 'X' | 'P' | 'S');
    public static final String SERVICE_NAME;
    public static final String DESCRIPTOR;
    public static final Class STUB_CLASS;
    static{
        try {
            //ServiceManager.addService(String name, IBinder ibinder);




            //android 10 system service list: android.app.SystemServiceRegistry/com.android.server.SystemServer
            SERVICE_NAME = Context.WINDOW_SERVICE;

            //public class Stub extends Binder;
            //public class Binder implements IBinder
            //所有 aidl生成的 Stub类都有 DESCRIPTOR字段
            STUB_CLASS = Class.forName("android.view.IWindowManager$Stub");
            DESCRIPTOR = (String) XReflectAccessibleInherit.getStaticFieldValue(STUB_CLASS, "DESCRIPTOR");
        } catch (Throwable e) {
            ANDROID_LOG.log(e);

            throw new RuntimeException(e);
        }
    }






    /** Tool */
    //反射服务管理器获取IBinder
    public static IBinder getIBinder(String serviceName) throws ClassNotFoundException, NoSuchMethodException, SecurityException, IllegalAccessException, InstantiationException, InvocationTargetException, IllegalArgumentException {
        //ServiceManager.getService(String name);

        Class<?> serviceManager = Class.forName("android.os.ServiceManager");
        Method method = serviceManager.getMethod("getService", String.class);
        IBinder ibinder = (IBinder) method.invoke(serviceManager.newInstance(), serviceName);
        return ibinder;
    }
    /** Tool */


    private static Object sendInvokMethodRequest0(IBinder mRemote, String methodName, Object... param)
            throws RemoteException
            , IOException, ClassNotFoundException {
        //发送数据给服务器 最终到达onTransact 进行处理
        //@1
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        Object _result;
        try {
            _data.writeInterfaceToken(DESCRIPTOR);

            //write key
            _data.writeString(SERVICE_ONLY_KEY);

            //write method
            _data.writeString(methodName);
            _data.writeByteArray(XStream.ObjectTool.toByteArray(param));
            mRemote.transact(SERVICE_ONTRANSACT_REQUEST_CODE, _data, _reply, 0);
            _reply.readException();
            _result = XStream.ObjectTool.toObject(_reply.createByteArray());
        } finally {
            _reply.recycle();
            _data.recycle();
        }
        return _result;
    }


    private IBinder ibinder = null;
    public IBinder getIBinder() throws ClassNotFoundException, NoSuchMethodException, SecurityException, IllegalAccessException, InstantiationException, InvocationTargetException, IllegalArgumentException {
        if (null != this.ibinder) {
            return this.ibinder;
        }
        return this.ibinder = getIBinder(SERVICE_NAME);
    }




    /**
     * 让服务端执行指定方法
     */
    public Object requestInvokMethod(String name, Object... param) throws LocalClientException, RemoteServiceException  {
        IBinder ibinder;
        try {
            ibinder = this.getIBinder();
        } catch (Throwable e) {
            //"get binder exception"
            throw new LocalClientException(e);
        }

        try {
            Object result = sendInvokMethodRequest0(ibinder, name, param);
            if (result instanceof RemoteServiceException) {
                RemoteServiceException remoteException = (RemoteServiceException)result;
                remoteException.addSuppressed(new LocalClientException());
                throw remoteException;
            }
            return result;
        } catch (RemoteServiceException e) {
            throw e;
        } catch (Throwable e) {
            throw new LocalClientException(e);
        }
    }




    //服务端方法将接口序列化 实际上还是通过requestInvokMethodSend
    private ServiceMethodInterface interfaceCache = null;
    public ServiceMethodInterface getMethodsInterface() {
        if (null == interfaceCache) {
            Object result =  Proxy.newProxyInstance(
                    this.getClass().getClassLoader(),
                    new Class[]{ServiceMethodInterface.class},
                    new InvocationHandler(){
                        @Override
                        public Object invoke(Object p1, Method p2, Object[] p3) throws Throwable {
                            String name = p2.getName();
                            Object[] param = p3;
                            return requestInvokMethod(name, param);
                        }
                    });
            interfaceCache = (ServiceMethodInterface) result;
        }
        return interfaceCache;
    }





    public boolean isServiceAvailable() {
        try {
            ServiceMethodInterface serviceMethodInterface = getMethodsInterface();
            long version = serviceMethodInterface.getVersion();
            //如果当前客户端版本大于服务端版本则返回异常
            if (SERVICE_VERSION > version) {
                return false;
            }
            return true;
        } catch (Throwable e) {
            return false;
        }
    }
    public boolean isServiceVersionEquals() {
        try {
            ServiceMethodInterface serviceMethodInterface = getMethodsInterface();
            long version = serviceMethodInterface.getVersion();
            return version == SERVICE_VERSION;
        } catch (Throwable e) {
            return false;
        }
    }
    public long getServiceVersion() {
        try {
            ServiceMethodInterface serviceMethodInterface = getMethodsInterface();
            long version = serviceMethodInterface.getVersion();
            return version;
        } catch (Throwable e) {
            return -1;
        }
    }



    public long getClientVersion() {
        return this.SERVICE_VERSION;
    }



    /**
     * 本地异常
     */
    public static class LocalClientException extends Exception {
        public LocalClientException() { super(); }
        public LocalClientException(java.lang.String message) { super(message); }
        public LocalClientException(java.lang.String message, java.lang.Throwable cause) { super(message, cause); }
        public LocalClientException(java.lang.Throwable cause) { super(cause); }
    }

    /**
     * 远程服务执行出现异常
     */
    public static class RemoteServiceException extends Exception {
        public RemoteServiceException() { super(); }
        public RemoteServiceException(java.lang.String message) { super(message); }
        public RemoteServiceException(java.lang.String message, java.lang.Throwable cause) { super(message, cause); }
        public RemoteServiceException(java.lang.Throwable cause) { super(cause); }
    }



}
