package top.fols.aapp.xp.tstorage.hok.util.preference;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.preference.PreferenceFragment;

import java.io.File;

import top.fols.aapp.xp.tstorage.hok.util.XposedDataStorage;
import top.fols.box.lang.reflect.optdeclared.XReflectAccessibleInherit;

/**
 * if targetSdkVersion >= 28, if selinux open accessing /data/data/ directory of other applications will throw an exception
 */
public class XDXSharedPreferencesUtils {

    public static File getFile(XposedDataStorage xdx, String preferencesName) throws RuntimeException {
        File dir = xdx.getAbsoluteFile(XposedDataStorage.DIR_SHARED_PREFS);
        File file = new File(dir, preferencesName);
        return file;
    }


    public static XDXSharedPreferencesImpl getSharedPreferences(XposedDataStorage xdx, String preferencesName) throws RuntimeException {
        File file = XDXSharedPreferencesUtils.getFile(xdx, preferencesName);
        return new XDXSharedPreferencesImpl(xdx, file);
    }





    @SuppressLint(value = "PrivateApi")
    public static void setPreferenceFragmentSharedPreferences(PreferenceFragment pf, SharedPreferences sp) {
        try {
            SharedPreferences mSharedPreferences = sp;
            XReflectAccessibleInherit.setFieldValue(pf.getPreferenceManager(), "mSharedPreferences", mSharedPreferences);
            XReflectAccessibleInherit.setFieldValue(pf.getPreferenceManager(), "mEditor", mSharedPreferences.edit());
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }






//  /data/user_de/0
    /**
     * /data/user_de/ Permission Denied 
     */
//      file = XSharedPreferencesUtil.getSharedPreferencesFileUserDe0(packageName, preferencesName);
//      XFile.openFile(file, false);

    /**
     * /data/user_de/ Permission Denied 
     */
//  public static File getSharedPreferencesFileUserDe0(String packageName, String preferencesName) {
//        File prefsDir = new File(Environment.getDataDirectory() , "user_de/0/" + packageName + "/shared_prefs/");
//      File prefsDirFile = new File(prefsDir, preferencesName + ".xml");
//      return prefsDirFile;
//    }


}

