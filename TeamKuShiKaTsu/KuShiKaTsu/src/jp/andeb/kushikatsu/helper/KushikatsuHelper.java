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
package jp.andeb.kushikatsu.helper;

import static android.app.Activity.RESULT_FIRST_USER;

import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;

/**
 * {@code KuShiKaTsu} を呼び出す際に有用な定数とメソッドを提供するクラスです。
 *
 * <p>
 * このコードは、呼び出し側のアプリにコピペされることを想定しています。
 * </p>
 */
public final class KushikatsuHelper {

    /*
     * 独自定義の result code 群。ここに定義されているものに加え、標準の result code である
     * RESULT_OK と RESULT_CANCELED も使用します。
     */

    /**
     * 予期しないエラーで送信が行えなかった
     * 場合({@code =}{@value #RESULT_UNEXPECTED_ERROR})。
     */
    public static final int RESULT_UNEXPECTED_ERROR = RESULT_FIRST_USER + 0;
    /**
     * Activity を起動した {@link Intent} に含まれる追加情報が不正なため送信が行えなかった
     * 場合({@code =}{@value #RESULT_INVALID_EXTRA})。
     */
    public static final int RESULT_INVALID_EXTRA = RESULT_FIRST_USER + 1;
    /**
     * FeliCa デバイスが搭載されていない端末の
     * 場合({@code =}{@value #RESULT_DEVICE_NOT_FOUND})。
     */
    public static final int RESULT_DEVICE_NOT_FOUND = RESULT_FIRST_USER + 2;
    /**
     * デバイスが他のアプリケーションによって占有されているため送信できなかった
     * 場合({@code =}{@value #RESULT_DEVICE_IN_USE})。
     */
    public static final int RESULT_DEVICE_IN_USE = RESULT_FIRST_USER + 3;
    /**
     * パラメータとして渡された {@link Intent} や URL などの情報が、{@code FeliCa Push}
     * 送信機能で送ることのできる上限を越えている場合({@code =}{@value #RESULT_TOO_BIG})。
     */
    public static final int RESULT_TOO_BIG = RESULT_FIRST_USER + 4;
    /**
     * 受信端末が見つからないため送信がタイムアウトした
     * 場合({@code =}{@value #RESULT_TIMEOUT})。
     */
    public static final int RESULT_TIMEOUT = RESULT_FIRST_USER + 5;
    /**
     * 端末のおサイフケータイ初期化が行われていないため、FeliCa デバイスを使用できない
     * 場合({@code =}{@value #RESULT_NOT_INITIALIZED})。
     */
    public static final int RESULT_NOT_INITIALIZED = RESULT_FIRST_USER + 6;
    /**
     * 端末のおサイフケータイロックのため FeliCa デバイスを使用できない
     * 場合({@code =}{@value #RESULT_DEVICE_LOCKED})。
     */
    public static final int RESULT_DEVICE_LOCKED = RESULT_FIRST_USER + 7;

    /**
     * KuShiKaTsu がもサウンドの名前({@value #SOUND_1})の定数です。
     */
    public static final String SOUND_1 = "se1";
    /**
     * KuShiKaTsu がもサウンドの名前({@value #SOUND_2})の定数です。
     */
    public static final String SOUND_2 = "se2";
    /**
     * KuShiKaTsu がもサウンドの名前({@value #SOUND_3})の定数です。
     */
    public static final String SOUND_3 = "se3";

    private KushikatsuHelper() {
        throw new AssertionError("instatiation prohibited.");
    }
    
    /**
     * KuShiKaTsuインストールチェック。
     * 
     * @param context
     * @return インストール済み可否
     */
    public static boolean isKushikatsuInstalled(final Context context) {
        PackageManager pm = context.getPackageManager();

        List<ResolveInfo> resolveInfo = pm.queryIntentActivities(
                new Intent("jp.andeb.kushikatsu.FELICA_INTENT"),
                PackageManager.MATCH_DEFAULT_ONLY);

        if (resolveInfo.size() > 0) {
            return true;
        }

        return false;
    }

    /**
     * KuShiKaTsuのマーケット画面を表示する。
     * 
     * @param context
     */
    public static void startKushikatsuInstall(final Context context) {
        Uri uri = Uri.parse("market://details?id=jp.andeb.kushikatsu");
        Intent i = new Intent(Intent.ACTION_VIEW, uri);
        context.startActivity(i);
    }

    public static void startKushikatsuForResult(final Activity activity, final Intent intent, final int requestCode) {
        Context context = activity.getBaseContext();

        if (isKushikatsuInstalled(activity.getBaseContext())) {
            // インストール済みの場合
            activity.startActivityForResult(intent, requestCode);

        }else{
            // 未インストールの場合
            startKushikatsuInstall(context);
        }
    }

}
