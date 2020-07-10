package top.fols.aapp.xp.tstorage.hok;

import android.app.AndroidAppHelper;
import android.content.Context;
import android.os.Parcel;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import java.lang.reflect.Method;
import top.fols.box.io.XStream;
import top.fols.box.lang.reflect.XReflectMatcher;
import top.fols.box.lang.reflect.optdeclared.XReflectAccessibleInherit;
import top.fols.box.statics.XStaticFixedValue;
import top.fols.box.util.XExceptionTool;
import top.fols.box.util.XObjects;

import static top.fols.aapp.xp.tstorage.hok.XposedDataStorageClient.*;

/**
 * 实现原理: hook aidl生成的Stub类中的 onTransact 方法
 * onTransact方法是服务端接收并且处理数据返回给客户
 *
 * 为了使其它也是用该模块的作者能够稳定运行 请不要修改这个类
 */
public class XposedDataStorageService {

    private static Context mContext;
    public static Context getContext() {
        return null == mContext ?mContext = AndroidAppHelper.currentApplication(): mContext;
    }




    //ICustomService.Stub stub = null;//Stub实现
    public static void hook(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        try {
            if (ANDROID_PACKAGE_NAME.equals(lpparam.packageName)) {
                Class StubClass = XposedDataStorageClient.STUB_CLASS;
                //boolean onTransact(int code, android.os.Parcel data, android.os.Parcel reply, int flags)
                Method onTransactMethod = XReflectAccessibleInherit.getMethod(StubClass,  "onTransact", int.class, android.os.Parcel.class, android.os.Parcel.class, int.class);
                XposedBridge.hookMethod(onTransactMethod, new XC_MethodHook(){
                    protected void afterHookedMethod(XC_MethodHook.MethodHookParam param) throws java.lang.Throwable {
                        try {

                            //code list
                            int code = (int) param.args[0];
                            Parcel data = (android.os.Parcel) param.args[1];
                            Parcel reply = (android.os.Parcel) param.args[2];
                            int flags = (int) param.args[3];

                            //execute error return false;
                            boolean result = ((Boolean)param.getResult()).booleanValue();
                            if (!result) {
                                if (code == XposedDataStorageClient.SERVICE_ONTRANSACT_REQUEST_CODE) {
                                    String KEY_ALREADYDEAL = "already.deal";
                                    String KEY_ONLYKEY = "only.key";

                                    Boolean alreadyDeal = (Boolean) param.getObjectExtra(KEY_ALREADYDEAL);
                                    //已经执行完成不进行任何操作
                                    if (null != alreadyDeal && alreadyDeal.booleanValue()) {
                                        return;
                                    }
                                    String dealKey = (String) param.getObjectExtra(KEY_ONLYKEY);
                                    if (null == dealKey) {
                                        data.enforceInterface(XposedDataStorageClient.DESCRIPTOR);
                                        dealKey = data.readString();
                                        param.setObjectExtra(KEY_ONLYKEY, dealKey);
                                    }

                                    //不是这个模块调用的
                                    if (!XObjects.isEquals(dealKey, XposedDataStorageClient.SERVICE_ONLY_KEY)) {
                                        param.setResult(false);
                                        return;
                                    }

                                    dealInvokMethodRequest0(data, reply);

                                    param.setResult(true);
                                    param.setObjectExtra(KEY_ONLYKEY, null);
                                    param.setObjectExtra(KEY_ALREADYDEAL, true);
                                }
                            }
                        } catch (Throwable e) {
                            ANDROID_LOG.log(e);
                        }
                    }
                });
            }
        } catch (Throwable e) {
            ANDROID_LOG.log(e);
            XposedBridge.log(XExceptionTool.StackTraceToString(e));
        }
    }


    //不要有错误
    private static void dealInvokMethodRequest0(Parcel data, Parcel reply) {
        try {
            String _methodName = data.readString();
            Object[] _arg = (Object[]) XStream.ObjectTool.toObject(data.createByteArray());
            Object _result = invokMethodRequest(_methodName, null == _arg ?XStaticFixedValue.nullObjectArray: _arg);
            reply.writeNoException();
            reply.writeByteArray(XStream.ObjectTool.toByteArray(_result));
        } catch (Throwable e) {
            /* 几乎没有可能出现异常 */
            ANDROID_LOG.log(e);
        }
    }


    /**
     * 客户端要求运行 Method
     */
    private static Object invokMethodRequest(String methodName, Object[] paramObjects) {
        Object result = null;
        try {
            //执行Method
            Object staticMethodClassObject = ServiceMethods.DEFAULT_INSTANCE;
            Method method = XReflectMatcher.defaultInstance.getMethod(staticMethodClassObject.getClass(), methodName, paramObjects);
            result = method.invoke(staticMethodClassObject, paramObjects);
        } catch (Throwable e) {
            //执行Method异常
            result = new RemoteServiceException(e);

            ANDROID_LOG.log(e);
        }
        return result;
    }
}
