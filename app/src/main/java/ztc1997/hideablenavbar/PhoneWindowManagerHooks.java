/*
 * Copyright 2015 Alex Zhang aka. ztc1997
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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;

import static de.robv.android.xposed.XposedBridge.log;

public class PhoneWindowManagerHooks {
    public static final String TAG = PhoneWindowManagerHooks.class.getSimpleName() + ": ";
    public static final String ACTION_HIDE_NAV_BAR = "ztc1997.hideablenavbar.PhoneWindowManagerHooks.action.HIDE_NAV_BAR";

    public static GesturesListener sGesturesListener;

    private static int sNavBarWp, sNavBarHp,sNavBarHl;
    private static Object sPhoneWindowManager;
    private static Context sContext;

    private static BroadcastReceiver sHideNavBarReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(ACTION_HIDE_NAV_BAR)) {
                hideNavBar();
            }
        }
    };

    public static void doHook() {
        final String CLASS_PHONE_WINDOW_MANAGER = "com.android.internal.policy.impl.PhoneWindowManager";
        final String CLASS_IWINDOW_MANAGER = "android.view.IWindowManager";
        final String CLASS_WINDOW_MANAGER_FUNCS = "android.view.WindowManagerPolicy.WindowManagerFuncs";

        XposedHelpers.findAndHookMethod(CLASS_PHONE_WINDOW_MANAGER, null, "init",
                Context.class, CLASS_IWINDOW_MANAGER, CLASS_WINDOW_MANAGER_FUNCS, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        super.afterHookedMethod(param);
                        log(TAG + XposedHelpers.getObjectField(param.thisObject, "mWindowManagerFuncs").getClass().getName());
                        sPhoneWindowManager = param.thisObject;
                        sContext = (Context) XposedHelpers.getObjectField(sPhoneWindowManager, "mContext");
                        Resources res = sContext.getResources();
                        int resWidthId = res.getIdentifier(
                                "navigation_bar_width", "dimen", "android");
                        int resHeightId = res.getIdentifier(
                                "navigation_bar_height", "dimen", "android");
                        int resHeightLandscapeId = res.getIdentifier(
                                "navigation_bar_height_landscape", "dimen", "android");
                        sNavBarWp = res.getDimensionPixelSize(resWidthId);
                        sNavBarHp = res.getDimensionPixelSize(resHeightId);
                        sNavBarHl = res.getDimensionPixelSize(resHeightLandscapeId);

                        sContext.registerReceiver(sHideNavBarReceiver, new IntentFilter(ACTION_HIDE_NAV_BAR));

                        sGesturesListener = new GesturesListener(sContext, new GesturesListener.Callbacks() {
                            @Override
                            public void onSwipeFromTop() {

                            }

                            @Override
                            public void onSwipeFromBottom() {
                                if (XposedHelpers.getBooleanField(sPhoneWindowManager, "mNavigationBarOnBottom"))
                                    showNavBar();
                            }

                            @Override
                            public void onSwipeFromRight() {
                                if (!XposedHelpers.getBooleanField(sPhoneWindowManager, "mNavigationBarOnBottom"))
                                    showNavBar();
                            }

                            @Override
                            public void onDebug() {

                            }
                        });
                    }
                }
        );

        final String CLASS_WINDOW_STATE = "android.view.WindowManagerPolicy$WindowState";
        XposedHelpers.findAndHookMethod(CLASS_PHONE_WINDOW_MANAGER, null, "layoutWindowLw", CLASS_WINDOW_STATE, CLASS_WINDOW_STATE, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                super.afterHookedMethod(param);
                sGesturesListener.screenWidth = XposedHelpers.getIntField(param.thisObject, "mUnrestrictedScreenWidth");
                sGesturesListener.screenHeight = XposedHelpers.getIntField(param.thisObject, "mUnrestrictedScreenHeight");
            }
        });
    }

    public static void showNavBar() {
        setNavBarDimensions(sNavBarWp, sNavBarHp, sNavBarHl);
    }

    public static void hideNavBar() {
        setNavBarDimensions(0, 0, 0);
    }

    private static void setNavBarDimensions(int wp, int hp, int hl) {
        int[] navigationBarWidthForRotation = (int[]) XposedHelpers.getObjectField(
                sPhoneWindowManager, "mNavigationBarWidthForRotation");
        int[] navigationBarHeightForRotation = (int[]) XposedHelpers.getObjectField(
                sPhoneWindowManager, "mNavigationBarHeightForRotation");
        final int portraitRotation = XposedHelpers.getIntField(sPhoneWindowManager, "mPortraitRotation");
        final int upsideDownRotation = XposedHelpers.getIntField(sPhoneWindowManager, "mUpsideDownRotation");
        final int landscapeRotation = XposedHelpers.getIntField(sPhoneWindowManager, "mLandscapeRotation");
        final int seascapeRotation = XposedHelpers.getIntField(sPhoneWindowManager, "mSeascapeRotation");
        if (navigationBarHeightForRotation[portraitRotation] == hp && navigationBarHeightForRotation[landscapeRotation] == hl
                && navigationBarWidthForRotation[portraitRotation] == wp && navigationBarWidthForRotation[landscapeRotation] == wp)
            return;

        navigationBarHeightForRotation[portraitRotation] =
                navigationBarHeightForRotation[upsideDownRotation] =
                        hp;
        navigationBarHeightForRotation[landscapeRotation] =
                navigationBarHeightForRotation[seascapeRotation] =
                        hl;

        navigationBarWidthForRotation[portraitRotation] =
                navigationBarWidthForRotation[upsideDownRotation] =
                        navigationBarWidthForRotation[landscapeRotation] =
                                navigationBarWidthForRotation[seascapeRotation] =
                                        wp;
        XposedHelpers.callMethod(sPhoneWindowManager, "updateRotation", false);
    }

    public static void sendNavBarHideIntent(Context context) {
        Intent intent = new Intent(PhoneWindowManagerHooks.ACTION_HIDE_NAV_BAR);
        context.sendBroadcast(intent);
    }
}
