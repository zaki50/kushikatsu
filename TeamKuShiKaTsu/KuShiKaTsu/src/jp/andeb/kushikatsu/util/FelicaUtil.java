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

import static com.felicanetworks.mfc.FelicaException.ID_ILLEGAL_STATE_ERROR;
import static com.felicanetworks.mfc.FelicaException.ID_IO_ERROR;
import static com.felicanetworks.mfc.FelicaException.ID_OPEN_ERROR;
import static com.felicanetworks.mfc.FelicaException.ID_UNKNOWN_ERROR;
import static com.felicanetworks.mfc.FelicaException.TYPE_ALREADY_ACTIVATED;
import static com.felicanetworks.mfc.FelicaException.TYPE_CURRENTLY_ACTIVATING;
import static com.felicanetworks.mfc.FelicaException.TYPE_CURRENTLY_ONLINE;
import static com.felicanetworks.mfc.FelicaException.TYPE_FELICA_NOT_AVAILABLE;
import static com.felicanetworks.mfc.FelicaException.TYPE_INVALID_RESPONSE;
import static com.felicanetworks.mfc.FelicaException.TYPE_NOT_ACTIVATED;
import static com.felicanetworks.mfc.FelicaException.TYPE_NOT_CLOSED;
import static com.felicanetworks.mfc.FelicaException.TYPE_NOT_IC_CHIP_FORMATTING;
import static com.felicanetworks.mfc.FelicaException.TYPE_NOT_OPENED;
import static com.felicanetworks.mfc.FelicaException.TYPE_OPEN_FAILED;
import static com.felicanetworks.mfc.FelicaException.TYPE_PUSH_FAILED;
import static com.felicanetworks.mfc.FelicaException.TYPE_REMOTE_ACCESS_FAILED;
import static com.felicanetworks.mfc.FelicaException.TYPE_TIMEOUT_OCCURRED;
import static jp.andeb.kushikatsu.helper.KushikatsuHelper.RESULT_DEVICE_IN_USE;
import static jp.andeb.kushikatsu.helper.KushikatsuHelper.RESULT_DEVICE_LOCKED;
import static jp.andeb.kushikatsu.helper.KushikatsuHelper.RESULT_DEVICE_NOT_FOUND;
import static jp.andeb.kushikatsu.helper.KushikatsuHelper.RESULT_NOT_INITIALIZED;
import static jp.andeb.kushikatsu.helper.KushikatsuHelper.RESULT_UNEXPECTED_ERROR;
import android.util.Log;

import com.felicanetworks.mfc.Felica;
import com.felicanetworks.mfc.FelicaException;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;

/**
 * {@link Felica} クラスを扱う上でのユーティリティクラスです。
 *
 * @author YAMAZAKI Makoto <makoto1975@gmail.com>
 */
@DefaultAnnotation(NonNull.class)
public final class FelicaUtil {

    public static int logAndGetResultCode(String tag, FelicaException e,
            String methodName) {
        int logLevel = Log.INFO;
        final String message;
        final int resultCode;

        if (FelicaUtil.isMissingMfc(e)) {
            // サービスとの接続に失敗。デバイスが存在しない場合など
            message = "Felica failed to connect MFC service on " + methodName;
            resultCode = RESULT_DEVICE_NOT_FOUND;
        } else if (FelicaUtil.isAlreadyActivated(e)) {
            message = "FeliCa device not found on " + methodName;
            resultCode = RESULT_DEVICE_IN_USE;
        } else if (FelicaUtil.isCurrentlyActivating(e)) {
            message = "FeliCa device not found on " + methodName;
            resultCode = RESULT_DEVICE_IN_USE;
        } else if (FelicaUtil.isNotActivated(e)) {
            message = "Felica not activated exception on " + methodName;
            resultCode = RESULT_UNEXPECTED_ERROR;
        } else if (FelicaUtil.isInvalidResponse(e)) {
            message = "Felica invalid response exception on " + methodName;
            resultCode = RESULT_UNEXPECTED_ERROR;
        } else if (FelicaUtil.isTimeoutOccurred(e)) {
            message = "Felica timeout exception on " + methodName;
            resultCode = RESULT_UNEXPECTED_ERROR;
        } else if (FelicaUtil.isNotIcChipFormatting(e)) {
            message = "Felica not initialized exception on " + methodName;
            resultCode = RESULT_NOT_INITIALIZED;
        } else if (FelicaUtil.isNotAvailable(e)) {
            message = "Felica not available exception on " + methodName;
            resultCode = RESULT_DEVICE_LOCKED;
        } else if (FelicaUtil.isOpenFailed(e)) {
            message = "Felica open failed exception on " + methodName;
            resultCode = RESULT_UNEXPECTED_ERROR;
        } else if (FelicaUtil.isNotOpened(e)) {
            message = "Felica not opened exception " + methodName;
            resultCode = RESULT_UNEXPECTED_ERROR;
        } else if (FelicaUtil.isCurrnetlyOnline(e)) {
            message = "Felica currently online exception " + methodName;
            resultCode = RESULT_UNEXPECTED_ERROR;
        } else if (FelicaUtil.isPushFailed(e)) {
            message = "Felica push failed exception " + methodName;
            resultCode = RESULT_UNEXPECTED_ERROR;
        } else {
            message = "unexpected " + FelicaUtil.toString(e) + " on "
                    + methodName;
            resultCode = RESULT_UNEXPECTED_ERROR;

            // 予期しない組み合わせなので、エラーとして出力する
            logLevel = Log.ERROR;
        }

        // ログを出力
        if (logLevel == Log.INFO) {
            Log.i(tag, message, e);
        } else if (logLevel == Log.ERROR) {
            Log.e(tag, message, e);
        } else {
            assert false;
            Log.e(tag, message, e);
        }
        return resultCode;
    }

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
    public static boolean isMissingMfc(@CheckForNull FelicaException e) {
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
    public static boolean isNotActivated(@CheckForNull FelicaException e) {
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
    public static boolean isAlreadyActivated(@CheckForNull FelicaException e) {
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
    public static boolean isCurrentlyActivating(@CheckForNull FelicaException e) {
        if (e == null) {
            return false;
        }
        final boolean result = e.getID() == ID_ILLEGAL_STATE_ERROR
                && e.getType() == TYPE_CURRENTLY_ACTIVATING;
        return result;
    }

    /**
     * 渡された {@link FelicaException} が、 {@code Felica} がオープンされていない
     * ためにスローされたかどうかを返します。
     *
     * @param e
     * チェック対象の例外。
     * @return
     * オープンされていないための失敗を意味している場合は {@code true}、そうでない場合は
     * {@code false} を返します。
     */
    public static boolean isNotOpened(@CheckForNull FelicaException e) {
        if (e == null) {
            return false;
        }
        final boolean result = e.getID() == ID_ILLEGAL_STATE_ERROR
                && e.getType() == TYPE_NOT_OPENED;
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
    public static boolean isNotClosed(@CheckForNull FelicaException e) {
        if (e == null) {
            return false;
        }
        final boolean result = e.getID() == ID_ILLEGAL_STATE_ERROR
                && e.getType() == TYPE_NOT_CLOSED;
        return result;
    }

    /**
     * 渡された {@link FelicaException} が、 {@link Felica} が既に通信中のためにスロー
     * されたかどうかを返します。
     *
     * @param e
     * チェック対象の例外。
     * @return
     * 既に通信中のための失敗を意味している場合は {@code true}、そうでない場合は
     * {@code false} を返します。
     */
    public static boolean isCurrnetlyOnline(@CheckForNull FelicaException e) {
        if (e == null) {
            return false;
        }
        final boolean result = e.getID() == ID_ILLEGAL_STATE_ERROR
                && e.getType() == TYPE_CURRENTLY_ONLINE;
        return result;
    }

    /**
     * 渡された {@link FelicaException} が、 不正な{@code FeliCa} チップレスポンスを
     * 返したためにスローされたかどうかを返します。
     *
     * @param e
     * チェック対象の例外。
     * @return
     * 不正な {@code FeliCa} チップレスポンス意味している場合は {@code true}、そうでない場合は
     * {@code false} を返します。
     */
    public static boolean isInvalidResponse(@CheckForNull FelicaException e) {
        if (e == null) {
            return false;
        }
        final boolean result = e.getID() == ID_IO_ERROR
                && e.getType() == TYPE_INVALID_RESPONSE;
        return result;
    }

    /**
     * 渡された {@link FelicaException} が、 送信タイムアウトのためにスローされたか
     * どうかを返します。
     *
     * @param e
     * チェック対象の例外。
     * @return
     * タイムアウトのための失敗を意味している場合は {@code true}、そうでない場合は
     * {@code false} を返します。
     */
    public static boolean isTimeoutOccurred(@CheckForNull FelicaException e) {
        if (e == null) {
            return false;
        }
        final boolean result = e.getID() == ID_IO_ERROR
                && e.getType() == TYPE_TIMEOUT_OCCURRED;
        return result;
    }

    /**
     * 渡された {@link FelicaException} が、オープン失敗のためにスローされたかどうかを
     * 返します。
     *
     * @param e
     * チェック対象の例外。
     * @return
     * おサイフケータイのオープン失敗を意味している場合は {@code true}、
     * そうでない場合は{@code false} を返します。
     */
    public static boolean isOpenFailed(@CheckForNull FelicaException e) {
        if (e == null) {
            return false;
        }
        final boolean result = e.getID() == ID_UNKNOWN_ERROR
                && e.getType() == TYPE_OPEN_FAILED;
        return result;
    }

    /**
     * 渡された {@link FelicaException} が、 Push 送信処理が失敗したためにスローされたか
     * どうかを返します。
     *
     * @param e
     * チェック対象の例外。
     * @return
     * Push 送信処理の失敗を意味している場合は {@code true}、そうでない場合は
     * {@code false} を返します。
     */
    public static boolean isPushFailed(@CheckForNull FelicaException e) {
        if (e == null) {
            return false;
        }
        final boolean result = e.getID() == ID_UNKNOWN_ERROR
                && e.getType() == TYPE_PUSH_FAILED;
        return result;
    }

    /**
     * 渡された {@link FelicaException} が、おサイフケータイ初期化が行われていないために
     * スローされたかどうかを返します。
     *
     * @param e
     * チェック対象の例外。
     * @return
     * おサイフケータイ初期化がされていなための失敗を意味している場合は {@code true}、
     * そうでない場合は{@code false} を返します。
     */
    public static boolean isNotIcChipFormatting(@CheckForNull FelicaException e) {
        if (e == null) {
            return false;
        }
        final boolean result = e.getID() == ID_OPEN_ERROR
                && e.getType() == TYPE_NOT_IC_CHIP_FORMATTING;
        return result;
    }

    /**
     * 渡された {@link FelicaException} が、おサイフケータイロックのために
     * スローされたかどうかを返します。
     *
     * @param e
     * チェック対象の例外。
     * @return
     * おサイフケータイロックのための失敗を意味している場合は {@code true}、
     * そうでない場合は{@code false} を返します。
     */
    public static boolean isNotAvailable(@CheckForNull FelicaException e) {
        if (e == null) {
            return false;
        }
        final boolean result = e.getID() == ID_OPEN_ERROR
                && e.getType() == TYPE_FELICA_NOT_AVAILABLE;
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
    public static void closeQuietly(@CheckForNull Felica felica, String tag) {
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
    public static void inactivateQuietly(@CheckForNull Felica felica, String tag) {
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
    public static String toString(@CheckForNull FelicaException e) {
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
