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
package jp.andeb.kushikatsu;

import static jp.andeb.kushikatsu.util.FelicaUtil.closeQuietly;
import static jp.andeb.kushikatsu.util.FelicaUtil.inactivateQuietly;

import java.util.MissingResourceException;
import java.util.concurrent.TimeUnit;

import jp.andeb.kushikatsu.util.FelicaServiceConnection;
import jp.andeb.kushikatsu.util.FelicaUtil;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;

import com.felicanetworks.mfc.AppInfo;
import com.felicanetworks.mfc.Felica;
import com.felicanetworks.mfc.FelicaEventListener;
import com.felicanetworks.mfc.FelicaException;
import com.felicanetworks.mfc.PushSegment;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;

/**
 * 起動時の {@link Intent} に従い {@code FeliCa Push} 送信を行う {@link Activity} です。
 *
 * <p>
 * このアクティビティは、自身を透明にした上で送信中を示すプログレスダイアログを表示します。
 * そのため、あたかも直前にfront だった {@link Activity} が {@code FeliCa Push} 送信を
 * 行っているように見えます。
 * </p>
 * <p>
 * 送信に失敗した場合は、 {@link Activity} のリザルトとして呼び出し元に伝え、自身では
 * エラーメッセージを表示することはありません。呼び出し側で必要に応じてユーザへ伝えてください。
 * このアクティビティは以下の result code を使用します。
 * </p>
 * <ul>
 *   <li>{@link Activity#RESULT_OK}({@code =-1})</li>
 *   <li>{@link Activity#RESULT_CANCELED}({@code =0})</li>
 *   <li>{@link #RESULT_UNEXPECTED_ERROR}({@code =}{@value #RESULT_UNEXPECTED_ERROR})</li>
 *   <li>{@link #RESULT_INVALID_EXTRA}({@code =}{@value #RESULT_INVALID_EXTRA})</li>
 *   <li>{@link #RESULT_DEVICE_NOT_FOUND}({@code =}{@value #RESULT_DEVICE_NOT_FOUND})</li>
 *   <li>{@link #RESULT_DEVICE_IN_USE}({@code =}{@value #RESULT_DEVICE_IN_USE})</li>
 *   <li>{@link #RESULT_TOO_BIG}({@code =}{@value #RESULT_TOO_BIG})</li>
 *   <li>{@link #RESULT_TIMEOUT}({@code =}{@value #RESULT_TIMEOUT})</li>
 *   <li>{@link #RESULT_NOT_INITIALIZED}({@code =}{@value #RESULT_NOT_INITIALIZED})</li>
 *   <li>{@link #RESULT_DEVICE_LOCKED}({@code =}{@value #RESULT_DEVICE_LOCKED})</li>
 * </ul>
 *
 * @author YAMAZAKI Makoto <makoto1975@gmail.com>
 */
@DefaultAnnotation(NonNull.class)
public class SendActivity extends Activity implements FelicaEventListener {

    private static final String TAG = SendActivity.class.getSimpleName();

    @DefaultAnnotation(NonNull.class)
    private static final class CommonParam {
        private static final String SEND_TIMEOUT = "SEND_TIMEOUT";
        private static final String SOUND_ON_SENT = "SOUND_ON_SENT";
    }

    /**
     * Push 送信のリトライ回数の上限です。 {@link Intent} で指定されたパラメータに関わらず
     * ここで指定される回数がリトライの上限です。
     */
    private final static int RETRY_LIMIT = 100;

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

    @CheckForNull
    private Felica felica_ = null;

    @CheckForNull
    private ProgressDialog progress_ = null;

    // Push データ
    @CheckForNull
    private PushSegment segment_ = null;

    /*
     * 共通パラメータ
     */

    /**
     * 送信成功時のサウンドリソースID。 {@code -1} は無音。
     */
    private int soundOnSent_;

    /**
     * タイムアウトまでの時間(ms)。
     */
    private long timeoutOfSending_;

    /**
     * {@link Activity} が初期化される際の処理を実装するメソッドです。
     */
    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        Log.d(TAG, "enter onCreate(): " + hashCode());

        super.onCreate(savedInstanceState);

        setContentView(R.layout.send);
    }

    /**
     * {@link Activity} が破棄される際の処理を実装するメソッドです。
     */
    @Override
    protected void onDestroy() {
        Log.d(TAG, "enter onDestroy(): " + hashCode());
        super.onDestroy();
        dismissProgress();
    }

    /**
     * {@link Activity} が表示される直前の処理を実装するメソッドです。
     */
    @Override
    protected void onStart() {
        Log.d(TAG, "enter onStart(): " + hashCode());
        super.onStart();

        final Intent initiatorIntent = getIntent();
        if (initiatorIntent == null) {
            setResultWithLog(RESULT_INVALID_EXTRA);
            finish();
            return;
        }

        final ActionType type = ActionType.of(initiatorIntent.getAction());
        final PushSegment segment = type.extractSegment(initiatorIntent);

        if (segment == null) {
            setResultWithLog(RESULT_INVALID_EXTRA);
            finish();
            return;
        }
        segment_ = segment;

        // TODO 起動 Intent の extra から取得する
        soundOnSent_ = R.raw.se1;
        timeoutOfSending_ = TimeUnit.SECONDS.toMillis(10L);
    }

    /**
     * {@link Activity} が表示された直後の処理を実装するメソッドです。
     */
    @Override
    protected void onResume() {
        Log.d(TAG, "enter onResume(): " + hashCode());
        super.onResume();

        /*
         * Set result to "unexpected error". This result code will be overwritten
         * by actual result code in most cases.
         */
        setResult(RESULT_UNEXPECTED_ERROR);

        final FelicaServiceConnection conn;
        conn = FelicaServiceConnection.getInstance();
        final FelicaServiceConnection.Listener listener = new FelicaServiceConnection.Listener() {
            @Override
            public void connected(final Felica felica) {
                SendActivity.this.felica_ = felica;
                Log.i(TAG, "connected to FeliCa service");
                try {
                    Log.i(TAG, "activating FeliCa");
                    felica.activateFelica(null, SendActivity.this);
                } catch (IllegalArgumentException e) {
                    Log.e(TAG, e.getClass().getSimpleName()
                            + " thrown on activateFelica()", e);
                    setResultWithLog(RESULT_UNEXPECTED_ERROR);
                    finish();
                } catch (FelicaException e) {
                    if (FelicaUtil.isMissingMfc(e)) {
                        // サービスとの接続に失敗。デバイスが存在しない場合など
                        Log.i(TAG, "FeliCa device not found");
                        setResultWithLog(RESULT_DEVICE_NOT_FOUND);
                    } else if (FelicaUtil.isAlreadyActivated(e)) {
                        Log.i(TAG, "FeliCa device not found");
                        setResultWithLog(RESULT_DEVICE_IN_USE);
                    } else if (FelicaUtil.isCurrentlyActivating(e)) {
                        Log.i(TAG, "FeliCa device not found");
                        setResultWithLog(RESULT_DEVICE_IN_USE);
                    } else {
                        Log.e(TAG, "unexpected " + FelicaUtil.toString(e), e);
                        setResultWithLog(RESULT_UNEXPECTED_ERROR);
                    }
                    finish();
                }
            }

            @Override
            public void disconnected() {
                Log.i(TAG, "disconnected from Felica Service.");
                finish();
            }
        };
        conn.setContext(this, listener);

        startProgress();
        setProgressMessage(R.string.progress_msg_prepare);
        felica_ = conn.connect();
        if (felica_ != null) {
            final Felica felica = felica_;
            new Thread(new Runnable() {
                @Override
                public void run() {
                    listener.connected(felica);
                }
            }).start();
        }
    }

    /**
     * {@link Activity} がフロントではなくなる際の処理を実装するメソッドです。
     */
    @Override
    protected void onPause() {
        Log.d(TAG, "enter onPause(): " + hashCode());
        super.onPause();

        FelicaServiceConnection.getInstance().disconnect();
        dismissProgress();
    }

    /**
     * プログレスダイアログを表示します。
     *
     * <p>
     * 既に表示されているプログレスダイアログが存在する場合は破棄したうえで新たに作成します。
     * </p>
     */
    private void startProgress() {
        Log.d(TAG, "start progress dialog called: " + hashCode());
        dismissProgress();

        final ProgressDialog progress = new ProgressDialog(this);
        progress.setTitle(R.string.progress_title);
        progress.setCancelable(false);
        // MEMO cancelable に変更するなら cancel 時に Activity を終了させること

        progress.show();

        assert progress_ == null;
        progress_ = progress;
    }

    /**
     * プログレスダイアログのメッセージをセットします。
     *
     * <p>
     * プログレスダイアログが表示されていない場合はなにもしません。
     * </p>
     *
     * @param resId
     * メッセージ用の string リソース識別子。
     * @throws MissingResourceException
     * 指定された識別子のリソースが存在しない場合。
     */
    private void setProgressMessage(final int resId) {
        final ProgressDialog progress = progress_;
        if (progress == null) {
            return;
        }
        progress.setMessage(getString(resId));
    }

    /**
     * プログレスダイアログを破棄します。
     *
     * <p>
     * プログレスダイアログが表示されていない場合はなにもしません。
     * </p>
     */
    private void dismissProgress() {
        Log.d(TAG, "dismiss progress dialog called: " + hashCode());
        final ProgressDialog progress = progress_;
        if (progress == null) {
            return;
        }
        progress_ = null;
        Log.d(TAG, "dismiss progress dialog");
        progress.dismiss();
    }

    /**
     * {@link Felica#activateFelica(String[], FelicaEventListener)} で、デバイスの
     * 有効化に成功した場合に呼び出される {@code callback} です。
     */
    @Override
    public void finished() {
        final Felica felica = felica_;
        if (felica == null) {
            Log.e(TAG, "unexpedted null of delica_");
            setResultWithLog(RESULT_UNEXPECTED_ERROR);
            finish();
            return;
        }
        try {
            final int resultCode = push(felica);
            setResultWithLog(resultCode);
        } finally {
            closeQuietly(felica_, TAG);
            inactivateQuietly(felica_, TAG);
            // 送信の成功/失敗にかかわらず Activity を終了する。
            finish();
        }
    }

    /**
     * {@link Felica#activateFelica(String[], FelicaEventListener)} で、デバイスの
     * 有効化に失敗した場合に呼び出される {@code callback} です。
     *
     * @param id
     * エラー種別。
     * @param msg
     * エラーメッセージ。
     * @param otherAppInfo
     * MFC 利用中のアプリ情報。
     */
    @Override
    public void errorOccurred(final int id, final String msg,
            final AppInfo otherAppInfo) {
        Log.i(TAG, "failed to activate FeliCa");
        // FIXME エラーコードを正しくセットする
        finish();
    }

    /**
     * 実際の送信処理を行います。
     *
     * @param felica
     * {@link Felica} インスタンス。 {@code null} 禁止。
     * @return
     * {@link Activity} としてのリザルトコード。呼び出し側で
     * {@link Activity#setResult(int) setResult()} してください。
     */
    private int push(final Felica felica) {
        Log.i(TAG, "FeliCa activated");
        final PushSegment segment = segment_;
        if (segment == null) {
            Log.d(TAG, "unexpected null of push segment");
            return RESULT_UNEXPECTED_ERROR;
        }

        try {
            Log.d(TAG, "opening felica.");
            felica.open();
            Log.d(TAG, "felica opened.");
        } catch (FelicaException e) {
            Log.e(TAG, "通信失敗: " + FelicaUtil.toString(e), e);
            final String message;
            final int resultCode;
            if (FelicaUtil.isNotActivated(e)) {
                message = "Felica not activated exception on open()";
                resultCode = RESULT_UNEXPECTED_ERROR;
            } else if (FelicaUtil.isInvalidResponse(e)) {
                message = "Felica invalid response exception on open()";
                resultCode = RESULT_UNEXPECTED_ERROR;
            } else if (FelicaUtil.isTimeoutOccurred(e)) {
                message = "Felica timeout exception on open()";
                resultCode = RESULT_UNEXPECTED_ERROR;
            } else if (FelicaUtil.isNotIcChipFormatting(e)) {
                message = "Felica not initialized exception on open()";
                resultCode = RESULT_NOT_INITIALIZED;
            } else if (FelicaUtil.isNotAvailable(e)) {
                message = "Felica not available exception on open()";
                resultCode = RESULT_DEVICE_LOCKED;
            } else if (FelicaUtil.isOpenFailed(e)) {
                message = "Felica open failed exception on open()";
                resultCode = RESULT_UNEXPECTED_ERROR;
            } else if (FelicaUtil.isMissingMfc(e)) {
                message = "Felica failed to connect MFC service on open()";
                resultCode = RESULT_DEVICE_NOT_FOUND;
            } else {
                message = "unexpected " + FelicaUtil.toString(e) + " on open()";
                resultCode = RESULT_UNEXPECTED_ERROR;
            }

            Log.e(TAG, message, e);
            return resultCode;
        }

        Log.d(TAG, "sending FeliCa message");
        int retryCount = 0;
        final long startTime = SystemClock.uptimeMillis();
        while (SystemClock.uptimeMillis() - startTime < timeoutOfSending_
                && retryCount < RETRY_LIMIT) {
            try {
                // Push送信
                felica.push(segment);
                Log.i(TAG, "FeliCa message has been sent successfully.");

                if (0 < soundOnSent_) {
                    // FIXME release() とかちゃんと。
                    final MediaPlayer mediaPlayer = MediaPlayer.create(this,
                            soundOnSent_);
                    mediaPlayer.start();
                }

                return RESULT_OK;
            } catch (IllegalArgumentException e) {
                // Intent が大きすぎるなど、不正な引数が指定された場合
                Log.e(TAG, e.getClass().getSimpleName(), e);
                return RESULT_TOO_BIG;
            } catch (FelicaException e) {
                if (FelicaUtil.isTimeoutOccurred(e)) {
                    retryCount++;
                    Log.d(TAG,
                            "push opration has been timed out. retryCount is "
                                    + retryCount);
                    continue;
                }

                final String message;
                final int resultCode;
                if (FelicaUtil.isNotActivated(e)) {
                    message = "Felica not activated exception on push()";
                } else if (FelicaUtil.isNotOpened(e)) {
                    message = "Felica not opened exception on push()";
                } else if (FelicaUtil.isCurrnetlyOnline(e)) {
                    message = "Felica currently online exception on push()";
                } else if (FelicaUtil.isInvalidResponse(e)) {
                    message = "Felica invalid response exception on push()";
                } else if (FelicaUtil.isPushFailed(e)) {
                    message = "Felica push failed exception on push()";
                } else if (FelicaUtil.isMissingMfc(e)) {
                    message = "Felica failed to connect MFC service on push()";
                } else {
                    message = "unexpected " + FelicaUtil.toString(e)
                            + " on push()";
                }
                resultCode = RESULT_UNEXPECTED_ERROR;

                Log.e(TAG, message, e);
                return resultCode;
            }
        }
        return RESULT_TIMEOUT;
    }

    /**
     * アクティビティのリザルトコードをセットし、セットした内容をログに出力します。
     * @param resultCode
     * リザルトコード。
     */
    private void setResultWithLog(int resultCode) {
        Log.d(TAG, "set result code: " + resultCode);
        setResult(resultCode);
    }
}
