package top.fols.aapp.xp.tstorage;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.widget.Toast;
import java.util.Arrays;
import top.fols.aapp.util.XAppLog;
import top.fols.aapp.xp.tstorage.hok.ServiceMethodInterface;
import top.fols.aapp.xp.tstorage.hok.XposedDataStorageClient;
import top.fols.aapp.xp.tstorage.hok.util.ServiceReturnBigData;
import top.fols.aapp.xp.tstorage.hok.util.XposedDataStorage;
import top.fols.box.lang.XString;
import top.fols.box.time.XTiming;
import top.fols.box.util.XExceptionTool;

public class MainActivity extends Activity {


    public static final XAppLog LOG = new XAppLog("top.fols.aapp.xp.tstorage");

    public void toastInUiThread(final Object value) {
        final Activity activity = this;
        toastInUiThread(activity, value, Toast.LENGTH_SHORT);
    }
    public static void toastInUiThread(final Activity activity, final Object value) {
        toastInUiThread(activity, value, Toast.LENGTH_SHORT);
    }
    public static void toastInUiThread(final Activity activity, final Object value, final int duration) {
        activity.runOnUiThread(new Runnable(){
                @Override
                public void run() {
                    String content = "" + value;
                    Toast.makeText(activity, content, duration).show();
                }
            });
    }



    public void toast(Object value) {
        Context context = this;
        toast(context, value, Toast.LENGTH_SHORT);
    }
    public static void toast(Context context, Object value) {
        toast(context, value, Toast.LENGTH_SHORT);
    }
    public static void toast(Context context, Object value, int duration) {
        String content = "" + value;
        Toast.makeText(context, content, duration).show();
    }





    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        try {
            XposedDataStorageClient client = new XposedDataStorageClient();
            if (!client.isServiceAvailable()) {
                Toast.makeText(this,
                    "[" + "服务不可用" + "]",
                    Toast.LENGTH_SHORT).show();
            }
            if (!client.isServiceVersionEquals()) {
                Toast.makeText(this,
                    "[" + "服务版本不相同" + "]",
                    Toast.LENGTH_SHORT).show();

            }



            final ServiceMethodInterface asi = client.getMethodsInterface();
            long version = asi.getVersion();

            XposedDataStorage storage = new XposedDataStorage(client, this.getApplicationContext().getApplicationInfo().packageName);

            String[] list = storage.list("/");

            XTiming xtiming = XTiming.newAndStart();
            storage.writeToString("test.txt", XString.repeat("0", 64 * 1024));
            storage.readToString("test.txt");


            Toast.makeText(this,
                "[list: " + Arrays.toString(list) + "]\n" +
                "[list-time: " + xtiming.endAndGetEndLessStart() + "]\n" +
                "[version: " + version + "]",
                Toast.LENGTH_SHORT).show();
                
                
            Toast.makeText(this,
                "~",
                Toast.LENGTH_SHORT).show();
                
                
            //sleep异常说明自动清理服务正常
            final ServiceReturnBigData<byte[]> bigdata = asi.returnBigData();
            new Thread(){
                @Override public void run() {
                    try {
                        sleep(20 * 1000);
                        toastInUiThread( "[自动清理字节流-sleep：" + bigdata.get(asi).length + "]");
                    } catch (Throwable e) {
                        LOG.log(e);
                        toastInUiThread(XExceptionTool.StackTraceToString(e));
                    }
                }
            }.start();
            Toast.makeText(this,
                "[自动清理字节流：" + bigdata.get(asi).length + "]",
                Toast.LENGTH_SHORT).show();
                
                
                
            //token改变了说明自动清理服务正常
            final XposedDataStorage.RandomAccessFileOutputStream rafo = storage.getSubFileRandomOutputStream("a");
            new Thread(){
                @Override public void run() {
                    try {
                        sleep(20 * 1000);
                        rafo.seekIndex(0);
                        toastInUiThread("[自动清理文件流-sleep：" + rafo.getToken() + "]");
                    } catch (Throwable e) {
                        LOG.log(e);
                        toastInUiThread(XExceptionTool.StackTraceToString(e));
                    }
                }
            }.start();
            Toast.makeText(this,
                "[自动清理文件流：" + rafo.getToken() + "]",
                Toast.LENGTH_SHORT).show();




                
                
                
                
            storage.mkdirs("1/2/3/");
            storage.createNewFile("1/2/3/45");
//          storage.deleteAll();
            storage.getSubFileRandomOutputStream("1/2/3/4/5");
        } catch (Throwable e) {
            LOG.log(e);
            
            Toast.makeText(this, XExceptionTool.StackTraceToString(e), Toast.LENGTH_SHORT).show();
        }

    }
}
