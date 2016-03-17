/*
 * Copyright 2015-2016 Alex Zhang aka. ztc1997
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package ztc1997.hideablenavbar;

import android.content.res.XModuleResources;

import de.robv.android.xposed.IXposedHookInitPackageResources;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_InitPackageResources;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import ztc1997.hideablenavbar.hooks.CmSystemUIHooks;
import ztc1997.hideablenavbar.hooks.PhoneWindowManagerHooks;
import ztc1997.hideablenavbar.hooks.PointerEventDispatcherHooks;
import ztc1997.hideablenavbar.hooks.SystemUIHooks;

import static de.robv.android.xposed.XposedBridge.log;

public class XposedInit implements IXposedHookLoadPackage, IXposedHookZygoteInit, IXposedHookInitPackageResources {
    public static final String TAG = XposedInit.class.getSimpleName() + ": ";
    public static final String PACKAGE_SYSTEMUI = "com.android.systemui";

    private static String MODULE_PATH;

    private static int IC_HIDE_ID;

    public static int getIcHideId() {
        return IC_HIDE_ID;
    }

    private static int IC_HIDE_LAND_ID;

    public static int getIcHideLandIdId() {
        return IC_HIDE_LAND_ID;
    }

    private static int STRING_NAV_HIDE_ID;

    public static int getStringNavHideId() {
        return STRING_NAV_HIDE_ID;
    }

    @Override
    public void initZygote(StartupParam startupParam) throws Throwable {
        XposedBridge.log("Hideable Nav Bar: Version = " + BuildConfig.VERSION_CODE);
        MODULE_PATH = startupParam.modulePath;
    }

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        switch (lpparam.packageName) {
            case PACKAGE_SYSTEMUI:
                try {
                    SystemUIHooks.doHook(lpparam.classLoader);
                } catch (Exception e) {
                    log(TAG + e);
                }

                try {
                    CmSystemUIHooks.doHook(lpparam.classLoader);
                } catch (Exception e) {
                    log(TAG + e);
                }
                break;
            case "android":
                try {
                    PointerEventDispatcherHooks.doHook(lpparam.classLoader);
                } catch (Exception e) {
                    log(TAG + e);
                }
                try {
                    PhoneWindowManagerHooks.doHook(lpparam.classLoader);
                } catch (Exception e) {
                    log(TAG + e);
                }
                break;

            case BuildConfig.APPLICATION_ID:
                try {
                    XposedHelpers.findAndHookMethod(SettingsActivity.class.getName(), lpparam.classLoader,
                            "activatedModuleVersion", XC_MethodReplacement.returnConstant(BuildConfig.VERSION_CODE));
                } catch (Exception e) {
                    log(TAG + e);
                }
                break;
        }
    }

    @Override
    public void handleInitPackageResources(XC_InitPackageResources.InitPackageResourcesParam liparam) throws Throwable {
        if (liparam.packageName.equals(PACKAGE_SYSTEMUI)) {
            try {
                XModuleResources modRes = XModuleResources.createInstance(MODULE_PATH, liparam.res);
                IC_HIDE_ID = liparam.res.addResource(modRes, R.mipmap.ic_sysbar_hide);
                IC_HIDE_LAND_ID = liparam.res.addResource(modRes, R.mipmap.ic_sysbar_hide_land);
                STRING_NAV_HIDE_ID = liparam.res.addResource(modRes, R.string.navbar_menu_hide);
            } catch (Exception e) {
                log(TAG + e);
            }
        }
    }
}
