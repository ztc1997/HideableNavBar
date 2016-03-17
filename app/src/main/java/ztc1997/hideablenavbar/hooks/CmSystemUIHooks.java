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

package ztc1997.hideablenavbar.hooks;

import android.view.KeyEvent;
import android.view.View;

import java.lang.reflect.Array;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import ztc1997.hideablenavbar.XposedInit;

public class CmSystemUIHooks {
    public static final String TAG = CmSystemUIHooks.class.getSimpleName() + ": ";

    public static void doHook(ClassLoader loader) {
        final Class<?> CLASS_NAVBAR_EDITOR = XposedHelpers.findClass("com.android.systemui.statusbar.phone.NavbarEditor", loader);
        final Class<?> CLASS_BUTTON_INFO = XposedHelpers.findClass("com.android.systemui.statusbar.phone.NavbarEditor$ButtonInfo", loader);

        final Object NAVBAR_HIDE = XposedHelpers.newInstance(CLASS_BUTTON_INFO, "hide", XposedInit.getStringNavHideId(), XposedInit.getStringNavHideId(),
                -1, XposedInit.getIcHideId(), XposedInit.getIcHideLandIdId(), XposedInit.getIcHideId());
        final Object buttonInfos = XposedHelpers.getStaticObjectField(CLASS_NAVBAR_EDITOR, "ALL_BUTTONS");
        XposedHelpers.setStaticObjectField(CLASS_NAVBAR_EDITOR, "ALL_BUTTONS", addElementToArray(buttonInfos, NAVBAR_HIDE, CLASS_BUTTON_INFO));

        XposedHelpers.findAndHookConstructor(CLASS_NAVBAR_EDITOR, View.class, boolean.class, boolean.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                super.beforeHookedMethod(param);
                XposedHelpers.setIntField(NAVBAR_HIDE, "displayId", XposedInit.getStringNavHideId());
                XposedHelpers.setIntField(NAVBAR_HIDE, "contentDescription", XposedInit.getStringNavHideId());
                XposedHelpers.setIntField(NAVBAR_HIDE, "portResource", XposedInit.getIcHideId());
                XposedHelpers.setIntField(NAVBAR_HIDE, "landResource", XposedInit.getIcHideLandIdId());
                XposedHelpers.setIntField(NAVBAR_HIDE, "sideResource", XposedInit.getIcHideId());
            }
        });

        final Class<?> CLASS_KEY_BUTTON_VIEW = XposedHelpers.findClass("com.android.systemui.statusbar.policy.KeyButtonView", loader);
        XposedHelpers.findAndHookMethod(CLASS_KEY_BUTTON_VIEW, "sendEvent", int.class, int.class, long.class, boolean.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                super.afterHookedMethod(param);
                if (!(XposedHelpers.getIntField(param.thisObject, "mCode") == -1)) return;
                int action = (int) param.args[0];
                int flags = (int) param.args[1];
                if (action == KeyEvent.ACTION_UP && flags == 0) {
                    View view = (View) param.thisObject;
                    PhoneWindowManagerHooks.sendNavBarHideIntent(view.getContext());
                }
                param.setResult(null);
            }
        });
    }

    private static Object addElementToArray(Object origin, Object element, Class<?> type) {
        int length = Array.getLength(origin);
        Object result = Array.newInstance(type, length + 1);
        for (int i = 0; i < length; i++) {
            Array.set(result, i, Array.get(origin, i));
        }
        Array.set(result, length, element);
        return result;
    }
}
