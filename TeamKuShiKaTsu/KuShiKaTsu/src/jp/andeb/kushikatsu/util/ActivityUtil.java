/*
 * Copyright 2010 Android DEvelopers' cluB
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * $Id$
 */
package jp.andeb.kushikatsu.util;

import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import android.app.Activity;
import android.content.res.Resources.NotFoundException;
import android.view.Window;

/**
 * {@link Activity} を扱う際のユーティリティクラスです。
 *
 */
@DefaultAnnotation(NonNull.class)
public final class ActivityUtil {

    /**
     * 指定された {@code ID} をもつレイアウトでアクティビティを構築します。
     *
     * @param target
     * 構築対象アクティビティ。 {@code null} 禁止。
     * @param layoutResId
     * レイアウトリソースID。
     * @throws NotFoundException
     * 指定された識別子のリソースが存在しない場合。
     * @throws IllegalArgumentException
     * {@code null} 禁止の引き数に {@code null} を指定した場合。
     */
    public static void setupActivityView(Activity target, int layoutResId) {
        if (target == null) {
            throw new IllegalArgumentException("'target' must not be null.");
        }

        setContentView(target, layoutResId);
    }

    /**
     * 指定された {@code ID} をもつレイアウトとタイトルアイコンでアクティビティを構築します。
     *
     * @param target
     * 構築対象アクティビティ。 {@code null} 禁止。
     * @param layoutResId
     * レイアウトリソースID。
     * @param titleIconResId
     * タイトルアイコンのリソースID。
     * @throws NotFoundException
     * 指定された識別子のリソースが存在しない場合。
     * @throws IllegalArgumentException
     * {@code null} 禁止の引き数に {@code null} を指定した場合。
     */
    public static void setupActivityView(Activity target, int layoutResId,
            int titleIconResId) {
        if (target == null) {
            throw new IllegalArgumentException("'target' must not be null.");
        }

        target.requestWindowFeature(Window.FEATURE_LEFT_ICON);
        setContentView(target, layoutResId);
        target.setFeatureDrawableResource(Window.FEATURE_LEFT_ICON,
                titleIconResId);
    }

    /**
     * 指定された {@code ID} をもつレイアウトとタイトルアイコンでアクティビティを構築します。
     *
     * @param target
     * 構築対象アクティビティ。 {@code null} 禁止。
     * @param layoutResId
     * レイアウトリソースID。
     * @param titleIconResId
     * タイトルアイコンのリソースID。
     * @param alpha
     * タイトルアイコンのアルファ値。 {@code 0}(透明) から {@code 255}(不透明) の範囲にクリップされます。
     * @throws NotFoundException
     * 指定された識別子のリソースが存在しない場合。
     * @throws IllegalArgumentException
     * {@code null} 禁止の引き数に {@code null} を指定した場合。
     */
    public static void setupActivityView(Activity target, int layoutResId,
            int titleIconResId, int alpha) {
        setupActivityView(target, layoutResId, titleIconResId);
        final int clippedAlpha = (alpha < 0) ? 0 : (0xff & alpha);
        target.setFeatureDrawableAlpha(Window.FEATURE_LEFT_ICON, clippedAlpha);
    }

    private static void setContentView(Activity target, int layoutResId) {
        target.setContentView(layoutResId);
    }

    private ActivityUtil() {
        throw new AssertionError("instantiation prohibited.");
    }
}
