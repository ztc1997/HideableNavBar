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
import android.view.MotionEvent;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;

public class PhoneWindowManagerHooks {
    public static final String ACTION_HIDE_NAV_BAR = "ztc1997.hideablenavbar.PhoneWindowManagerHooks.action.HIDE_NAV_BAR";

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
                    }
                }
        );

        final String CLASS_SYSTEM_GESTURES_POINTER_EVENT_LISTENER = "com.android.internal.policy.impl.SystemGesturesPointerEventListener";
        XposedHelpers.findAndHookMethod(CLASS_SYSTEM_GESTURES_POINTER_EVENT_LISTENER, null, "onPointerEvent", MotionEvent.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                super.afterHookedMethod(param);
                final MotionEvent event = (MotionEvent) param.args[0];
                if (event.getAction() == MotionEvent.ACTION_MOVE) {
                    switch ((int) XposedHelpers.callMethod(param.thisObject, "detectSwipe", event)) {
                        case 2:
                            if (XposedHelpers.getBooleanField(sPhoneWindowManager, "mNavigationBarOnBottom"))
                                showNavBar();
                            break;
                        case 3:
                            if (!XposedHelpers.getBooleanField(sPhoneWindowManager, "mNavigationBarOnBottom"))
                                showNavBar();
                            break;
                    }
                }
            }
        });
    }

    private static void showNavBar() {
        setNavBarDimensions(sNavBarWp, sNavBarHp, sNavBarHl);
    }

    private static void hideNavBar() {
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
}
