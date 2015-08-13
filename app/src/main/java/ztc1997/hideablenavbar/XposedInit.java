/*
 * Copyright 2015. Alex Zhang aka. ztc1997
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ztc1997.hideablenavbar;

import android.content.res.XModuleResources;

import de.robv.android.xposed.IXposedHookInitPackageResources;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.callbacks.XC_InitPackageResources;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class XposedInit implements IXposedHookLoadPackage, IXposedHookZygoteInit, IXposedHookInitPackageResources {
    public static final String PACKAGE_SYSTEMUI = "com.android.systemui";

    private static String MODULE_PATH = null;
    private static int HIDE_ICON_ID;

    public static int getHideIconId() {
        return HIDE_ICON_ID;
    }
    private static int HIDE_ICON_LAND_ID;

    public static int getHideIconLandIdId() {
        return HIDE_ICON_LAND_ID;
    }

    @Override
    public void initZygote(StartupParam startupParam) throws Throwable {
        PhoneWindowManagerHooks.doHook();
        MODULE_PATH = startupParam.modulePath;
    }

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if (lpparam.packageName.equals(PACKAGE_SYSTEMUI))
                SystemUIHooks.doHook(lpparam.classLoader);
    }

    @Override
    public void handleInitPackageResources(XC_InitPackageResources.InitPackageResourcesParam liparam) throws Throwable {
        if (liparam.packageName.equals(PACKAGE_SYSTEMUI)) {
            XModuleResources modRes = XModuleResources.createInstance(MODULE_PATH, liparam.res);
            HIDE_ICON_ID = liparam.res.addResource(modRes, R.mipmap.ic_sysbar_hide);
            HIDE_ICON_LAND_ID = liparam.res.addResource(modRes, R.mipmap.ic_sysbar_hide_land);
        }
    }
}
