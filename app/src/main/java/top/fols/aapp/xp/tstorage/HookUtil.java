package top.fols.aapp.xp.tstorage;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import top.fols.aapp.xp.tstorage.hok.XposedDataStorageService;

public class HookUtil implements IXposedHookLoadPackage {

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam p1) throws Throwable {
        XposedDataStorageService.hook(p1);
    }

}
