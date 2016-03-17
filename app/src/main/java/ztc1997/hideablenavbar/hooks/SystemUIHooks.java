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

import android.content.res.Resources;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import ztc1997.hideablenavbar.XposedInit;
import ztc1997.hideablenavbar.view.KeyButtonView;

import static de.robv.android.xposed.XposedBridge.log;
import static ztc1997.hideablenavbar.BuildConfig.DEBUG;

public class SystemUIHooks {
    public static final String TAG = SystemUIHooks.class.getSimpleName() + ": ";

    private static View sNavBar;

    public static void doHook(ClassLoader loader) {
        final Class<?> CLASS_NAVIGATION_BAR_VIEW = XposedHelpers.findClass("com.android.systemui.statusbar.phone.NavigationBarView", loader);
        XposedHelpers.findAndHookMethod(CLASS_NAVIGATION_BAR_VIEW, "onFinishInflate", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                super.afterHookedMethod(param);
                sNavBar = (View) param.thisObject;
                Resources res = sNavBar.getResources();
                View[] mRotatedViews = (View[]) XposedHelpers.getObjectField(sNavBar, "mRotatedViews");

                ViewGroup navBtnsRot0 = (ViewGroup) mRotatedViews[Surface.ROTATION_0]
                        .findViewById(res.getIdentifier("nav_buttons", "id", XposedInit.PACKAGE_SYSTEMUI));
                ViewGroup navBtnsRot90 = (ViewGroup) mRotatedViews[Surface.ROTATION_90]
                        .findViewById(res.getIdentifier("nav_buttons", "id", XposedInit.PACKAGE_SYSTEMUI));
                navBtnsRot0.removeViewAt(0);
                navBtnsRot90.removeViewAt(navBtnsRot90.getChildCount() - 1);

                final float scale = sNavBar.getContext().getResources().getDisplayMetrics().density;
                int navigationSidePadding = (int) (40 * scale);
                int navigationExtraKeyWidth = (int) (36 * scale);

                try {
                    navigationSidePadding = res.getDimensionPixelOffset(res.getIdentifier("navigation_side_padding", "dimen", XposedInit.PACKAGE_SYSTEMUI));
                } catch (Resources.NotFoundException e) {
                    if (DEBUG) log(TAG + e);
                }
                try {
                    navigationExtraKeyWidth = res.getDimensionPixelOffset(res.getIdentifier("navigation_extra_key_width", "dimen", XposedInit.PACKAGE_SYSTEMUI));
                } catch (Resources.NotFoundException e) {
                    if (DEBUG) log(TAG + e);
                }

                FrameLayout hideNavContainer0 = new FrameLayout(sNavBar.getContext());
                hideNavContainer0.setLayoutParams(new LinearLayout.LayoutParams(navigationSidePadding, ViewGroup.LayoutParams.MATCH_PARENT));
                ImageView hideNavBtn0 = new KeyButtonView(sNavBar.getContext());
                hideNavBtn0.setLayoutParams(new ViewGroup.LayoutParams(navigationExtraKeyWidth, ViewGroup.LayoutParams.MATCH_PARENT));
                hideNavBtn0.setImageResource(XposedInit.getIcHideId());
                hideNavBtn0.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
                hideNavBtn0.setOnClickListener(onHideNavBtnClickListener);
                hideNavContainer0.addView(hideNavBtn0);
                navBtnsRot0.addView(hideNavContainer0, 0);

                FrameLayout hideNavContainer90 = new FrameLayout(sNavBar.getContext());
                hideNavContainer90.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, navigationSidePadding));
                ImageView hideNavBtn90 = new KeyButtonView(sNavBar.getContext());
                hideNavBtn90.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, navigationExtraKeyWidth));
                hideNavBtn90.setImageResource(XposedInit.getIcHideLandIdId());
                hideNavBtn90.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
                hideNavBtn90.setOnClickListener(onHideNavBtnClickListener);
                hideNavContainer90.addView(hideNavBtn90);
                navBtnsRot90.addView(hideNavContainer90, navBtnsRot90.getChildCount());
            }
        });
    }

    private static View.OnClickListener onHideNavBtnClickListener = new View.OnClickListener() {

        @Override
        public void onClick(View v) {
            PhoneWindowManagerHooks.sendNavBarHideIntent(sNavBar.getContext());
        }
    };
}
