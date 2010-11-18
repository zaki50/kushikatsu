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
 */
package jp.andeb.kushikatsu.util;

import static com.felicanetworks.mfc.FelicaException.ID_ILLEGAL_STATE_ERROR;
import static com.felicanetworks.mfc.FelicaException.ID_UNKNOWN_ERROR;
import static com.felicanetworks.mfc.FelicaException.TYPE_ALREADY_ACTIVATED;
import static com.felicanetworks.mfc.FelicaException.TYPE_CURRENTLY_ACTIVATING;
import static com.felicanetworks.mfc.FelicaException.TYPE_NOT_ACTIVATED;
import static com.felicanetworks.mfc.FelicaException.TYPE_NOT_CLOSED;
import static com.felicanetworks.mfc.FelicaException.TYPE_REMOTE_ACCESS_FAILED;
import android.util.Log;

import com.felicanetworks.mfc.Felica;
import com.felicanetworks.mfc.FelicaException;

/**
 * {@link Felica} クラスを扱う上でのユーティリティクラスです。
 *
 * @author zaki
 */
public final class FelicaUtil {

    /**
     * 渡された {@link FelicaException} が、 MFC プロセスとの通信に失敗したことを意味して
     * いるかどかを返します。
     *
     * <p>
     * 主に以下の場合にスローされます。
     * </p>
     * <ul>
     *  <li>FeliCa デバイスを搭載していない端末で実行した場合</li>
     *  <li>MFC プロセスが起動できない場合</li>
     *  <li>MFC プロセスが kill された場合</li>
     * </ul>
     *
     * @param e
     * チェック対象の例外。
     * @return
     * MFC プロセスとの通信失敗を意味している場合は {@code true}、そうでない場合は
     * {@code false} を返します。
     */
    public static boolean isMissingMfc(FelicaException e) {
        if (e == null) {
            return false;
        }
        final boolean result = e.getID() == ID_UNKNOWN_ERROR
                && e.getType() == TYPE_REMOTE_ACCESS_FAILED;
        return result;
    }

    /**
     * 渡された {@link FelicaException} が、 {@link Felica} がアクティベートされていない
     * ためにスローされたかどうかを返します。
     *
     * @param e
     * チェック対象の例外。
     * @return
     * 未アクティベートによる失敗を意味している場合は {@code true}、そうでない場合は
     * {@code false} を返します。
     */
    public static boolean isNotActivated(FelicaException e) {
        if (e == null) {
            return false;
        }
        final boolean result = e.getID() == ID_ILLEGAL_STATE_ERROR
                && e.getType() == TYPE_NOT_ACTIVATED;
        return result;
    }

    /**
     * 渡された {@link FelicaException} が、 {@link Felica} が既にアクティベート済みの
     * ためにスローされたかどうかを返します。
     *
     * @param e
     * チェック対象の例外。
     * @return
     * アクティベート済みによる失敗を意味している場合は {@code true}、そうでない場合は
     * {@code false} を返します。
     */
    public static boolean isAlreadyActivated(FelicaException e) {
        if (e == null) {
            return false;
        }
        final boolean result = e.getID() == ID_ILLEGAL_STATE_ERROR
                && e.getType() == TYPE_ALREADY_ACTIVATED;
        return result;
    }

    /**
     * 渡された {@link FelicaException} が、 {@code Felica} が現在アクティベート中の
     * ためにスローされたかどうかを返します。
     *
     * @param e
     * チェック対象の例外。
     * @return
     * 現在アクティベート中のための失敗を意味している場合は {@code true}、そうでない場合は
     * {@code false} を返します。
     */
    public static boolean isCurrentlyActivating(FelicaException e) {
        if (e == null) {
            return false;
        }
        final boolean result = e.getID() == ID_ILLEGAL_STATE_ERROR
                && e.getType() == TYPE_CURRENTLY_ACTIVATING;
        return result;
    }

    /**
     * 渡された {@link FelicaException} が、 {@code Felica} がクローズされていない
     * ためにスローされたかどうかを返します。
     *
     * @param e
     * チェック対象の例外。
     * @return
     * クローズされていないための失敗を意味している場合は {@code true}、そうでない場合は
     * {@code false} を返します。
     */
    public static boolean isNotClosed(FelicaException e) {
        if (e == null) {
            return false;
        }
        final boolean result = e.getID() == ID_ILLEGAL_STATE_ERROR
                && e.getType() == TYPE_NOT_CLOSED;
        return result;
    }

    private static final String DEFAULT_TAG = FelicaUtil.class.getSimpleName();

    /**
     * {@link Felica} をクローズ({@link Felica#close()})します。
     *
     * <p>
     * 処理において例外がスローされた場合は、指定されたタグで {@code INFO} レベルのログを
     * 出力します。
     * </p>
     *
     * @param felica
     * {@link Felica} インスタンス。 {@code null} の場合は何もしません。
     * @param tag
     * ログを書く際のタグ。
     * {@code null} の場合は このクラスのクラス名({@link Class#getSimpleName()} したもの)
     * を使用します。
     */
    public static void closeQuietly(Felica felica, String tag) {
        if (felica == null) {
            return;
        }
        try {
            felica.close();
        } catch (FelicaException e) {
            if (tag == null) {
                tag = DEFAULT_TAG;
            }
            final String message;
            if (isNotActivated(e)) {
                message = "Felica not activated exception on close()";
            } else if (isMissingMfc(e)) {
                message = "lost connection to MFC exception on close()";
            } else {
                message = "unexpected FelicaException thrown on Felica#close()";
            }
            Log.i(tag, message, e);
        } catch (Throwable e) {
            Log.w(tag, "unexpected " + e.getClass().getSimpleName()
                    + " thrown on " + "Felica#close()", e);
        }
    }

    /**
     * {@link Felica} を無効化({@link Felica#inactivateFelica()})します。
     * また、クローズされていない場合は{@link #closeQuietly(Felica, String)} を用いて
     * クローズします。
     *
     * <p>
     * 処理において例外がスローされた場合は、指定されたタグで {@code INFO} レベルのログを
     * 出力します。
     * </p>
     *
     * @param felica
     * {@link Felica} インスタンス。 {@code null} の場合は何もしません。
     * @param tag
     * ログを書く際のタグ。
     * {@code null} の場合は このクラスのクラス名({@link Class#getSimpleName()} したもの)
     * を使用します。
     */
    public static void inactivateQuietly(Felica felica, String tag) {
        if (felica == null) {
            return;
        }

        closeQuietly(felica, tag);

        try {
            felica.inactivateFelica();
        } catch (FelicaException e) {
            if (tag == null) {
                tag = DEFAULT_TAG;
            }
            final String message;
            if (isNotClosed(e)) {
                message = "Felica not closed exception on inactivateFelica()";
            } else if (isMissingMfc(e)) {
                message = "lost connection to MFC exception on inactivateFelica()";
            } else {
                message = "unexpected FelicaException thrown on Felica#close()";
            }
            Log.i(tag, message, e);
        } catch (Throwable e) {
            Log.w(tag, "unexpected " + e.getClass().getSimpleName()
                    + " thrown on " + "Felica#inactivateFelica()", e);
        }
    }

    /**
     * {@link FelicaException} の文字列表現を返します。
     *
     * @param e
     * 変換対象。 {@code null} の場合は空文字列を返します。
     * @return
     * 例外の文字列表現。
     */
    public static String toString(FelicaException e) {
        if (e == null) {
            return "";
        }

        final StringBuilder sb = new StringBuilder();
        sb.append(e.getClass().getName());
        sb.append("[id=").append(e.getID());
        sb.append(",type=").append(e.getType());
        sb.append(",statusFlag1=" + e.getStatusFlag1());
        sb.append(",statusFlag2=").append(e.getStatusFlag2());
        sb.append("]");
        return sb.toString();
    }

    private FelicaUtil() {
        throw new AssertionError("instantiation prohibited.");
    }
}
