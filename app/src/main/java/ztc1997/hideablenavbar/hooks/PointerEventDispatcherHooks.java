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

import android.view.InputDevice;
import android.view.InputEvent;
import android.view.MotionEvent;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;

import static de.robv.android.xposed.XposedBridge.log;

public class PointerEventDispatcherHooks {
    public static final String TAG = PointerEventDispatcherHooks.class.getSimpleName() + ": ";

    public static void doHook(ClassLoader loader) {


        final Class<?> CLASS_POINTER_EVENT_DISPATCHER = XposedHelpers.findClass("com.android.server.wm.PointerEventDispatcher", loader);
        XposedHelpers.findAndHookMethod(CLASS_POINTER_EVENT_DISPATCHER, "onInputEvent", InputEvent.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                super.afterHookedMethod(param);
                try {
                    if (param.args[0] instanceof MotionEvent) {
                        MotionEvent event = (MotionEvent) param.args[0];
                        if ((event.getSource() & InputDevice.SOURCE_CLASS_POINTER) != 0) {
                            PhoneWindowManagerHooks.sGesturesListener.onPointerEvent(event);
                        }
                    }
                } catch (Exception e) {
                    log(TAG + e);
                }
            }
        });
    }
}
