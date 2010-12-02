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

import static jp.andeb.kushikatsu.helper.KushikatsuHelper.RESULT_INVALID_EXTRA;
import static jp.andeb.kushikatsu.helper.KushikatsuHelper.RESULT_TIMEOUT;
import static jp.andeb.kushikatsu.helper.KushikatsuHelper.RESULT_TOO_BIG;
import static jp.andeb.kushikatsu.helper.KushikatsuHelper.RESULT_UNEXPECTED_ERROR;
import static jp.andeb.kushikatsu.util.FelicaUtil.closeQuietly;
import static jp.andeb.kushikatsu.util.FelicaUtil.inactivateQuietly;
import static jp.andeb.kushikatsu.util.MediaPlayerUtil.RELEASE_PLAYER_LISTENER;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import jp.andeb.kushikatsu.helper.KushikatsuHelper;
import jp.andeb.kushikatsu.helper.KushikatsuHelper.CommonParam;
import jp.andeb.kushikatsu.util.FelicaUtil;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources.NotFoundException;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.SystemClock;
import android.os.Vibrator;
import android.preference.PreferenceManager;
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
 * 送信に成功/失敗は、 {@link Activity} のリザルトとして呼び出し元に伝え、自身では
 * エラーメッセージを表示することはありません。呼び出し側で必要に応じてユーザへ伝えてください。
 * このアクティビティは以下の result code を使用します。
 * </p>
 * <ul>
 *   <li>{@link Activity#RESULT_OK}({@code =-1})</li>
 *   <li>{@link Activity#RESULT_CANCELED}({@code =0})</li>
 *   <li>{@link KushikatsuHelper#RESULT_UNEXPECTED_ERROR}</li>
 *   <li>{@link KushikatsuHelper#RESULT_INVALID_EXTRA}</li>
 *   <li>{@link KushikatsuHelper#RESULT_DEVICE_NOT_FOUND}</li>
 *   <li>{@link KushikatsuHelper#RESULT_DEVICE_IN_USE}</li>
 *   <li>{@link KushikatsuHelper#RESULT_TOO_BIG}</li>
 *   <li>{@link KushikatsuHelper#RESULT_TIMEOUT}</li>
 *   <li>{@link KushikatsuHelper#RESULT_NOT_INITIALIZED}</li>
 *   <li>{@link KushikatsuHelper#RESULT_DEVICE_LOCKED}</li>
 * </ul>
 *
 * @author YAMAZAKI Makoto &lt;<a href="mailto:makoto1975@gmail.com" >makoto1975@gmail.com</a>&gt;
 */
@DefaultAnnotation(NonNull.class)
public class SendActivity extends Activity implements FelicaEventListener {

    private static final String TAG = SendActivity.class.getSimpleName();

    /**
     * 送信リトライのタイムアウトまでの秒数のデフォルト値。
     */
    private static final int SEND_TIMEOUT_DEFAULT = 10; /* sec */

    /**
     * 送信成功時のサウンドの名前のデフォルト値。
     */
    private static final String SOUND_ON_SENT_DEFAULT = "se1";

    /**
     * Push 送信のリトライ回数の上限です。 {@link Intent} で指定されたパラメータに関わらず
     * ここで指定される回数がリトライの上限です。
     */
    private final static int RETRY_LIMIT = 100;

    /**
     * 振動パターン設定
     */
    private static final long[] VIBRATOR_PATTERN = { 0L, 200L };

    /**
     * サウンドリソースの名前と ID のマップ。
     */
    private static Map<String, Integer> SOUND_ID_MAP;
    static {
        final HashMap<String, Integer> map = new HashMap<String, Integer>();
        for (Field field : R.raw.class.getFields()) {
            if (field.getType() != Integer.TYPE) {
                continue;
            }
            if (!Modifier.isStatic(field.getModifiers())) {
                continue;
            }
            if (!Modifier.isFinal(field.getModifiers())) {
                continue;
            }
            if (!field.getName().startsWith("se")) {
                continue;
            }
            try {
                final Integer resId = (Integer) field.get(null);
                if (resId == null) {
                    // ありえないが念のため
                    continue;
                }
                map.put(field.getName(), resId);
            } catch (IllegalArgumentException e) {
                Log.e(TAG, "failed to get resource id", e);
                continue;
            } catch (IllegalAccessException e) {
                Log.e(TAG, "failed to get resource id", e);
                continue;
            }
        }
        // 音を出さない場合のため
        map.put("", Integer.valueOf(-1));
        SOUND_ID_MAP = Collections.unmodifiableMap(map);
    }

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
     * 送信成功時のサウンド取得のための {@link Context}。
     */
    @CheckForNull
    private Context contextOfSoundOnSent_;

    /**
     * タイムアウトまでの時間(ms)。
     */
    private long timeoutOfSending_;

    /**
     * 共通設定アクセス。
     */
    private SharedPreferences preferences_;

    /**
     * {@link Activity} が初期化される際の処理を実装するメソッドです。
     */
    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        Log.d(TAG, "enter onCreate(): " + hashCode());
        super.onCreate(savedInstanceState);

        setContentView(R.layout.send);
        preferences_ = PreferenceManager.getDefaultSharedPreferences(this);
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
            Log.e(TAG, "initiator intent is null.");
            setResultWithLog(RESULT_INVALID_EXTRA);
            finish();
            return;
        }

        final String action = initiatorIntent.getAction();
        final ActionType type = ActionType.of(action);
        if (type == null) {
            Log.e(TAG, "unsupported initiator action: " + action);
            setResultWithLog(RESULT_INVALID_EXTRA);
            finish();
            return;
        }
        final PushSegment segment = type.extractSegment(initiatorIntent);
        if (segment == null) {
            Log.e(TAG, "failed to create PushSegment.");
            setResultWithLog(RESULT_INVALID_EXTRA);
            finish();
            return;
        }
        segment_ = segment;

        // 完了音を取得する
        int soundResId = initiatorIntent.getIntExtra(
                CommonParam.EXTRA_SOUND_ON_SENT, -1);
        if (0 <= soundResId) {
            // 呼び出し元のサウンドリソースを使用する場合
            @CheckForNull
            final Context callerContext = getCallerContext();
            if (isValidAudioResource(callerContext, soundResId)) {
                assert callerContext != null;
                soundOnSent_ = soundResId;
                contextOfSoundOnSent_ = callerContext;
            } else {
                soundOnSent_ = -1;
                contextOfSoundOnSent_ = null;
            }
        } else {
            // KuShiKaTsu のサウンドリソースを使用する場合
            String soundName = initiatorIntent
                    .getStringExtra(CommonParam.EXTRA_SOUND_ON_SENT);
            if (soundName == null) {
                soundName = preferences_.getString(
                        PrefActivity.KEY_SOUND_PATTERN, SOUND_ON_SENT_DEFAULT);
            }
            soundOnSent_ = getSoundResId(soundName);
            contextOfSoundOnSent_ = this;
        }

        // 送信タイムアウトまでの時間を取得
        int timeoutSec = initiatorIntent.getIntExtra(
                CommonParam.EXTRA_SEND_TIMEOUT, SEND_TIMEOUT_DEFAULT);
        if (timeoutSec < 0) {
            timeoutSec = SEND_TIMEOUT_DEFAULT;
        }
        timeoutOfSending_ = TimeUnit.SECONDS.toMillis(timeoutSec);
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

        startProgress();
        setProgressMessage(getString(R.string.progress_msg_preparing));

        // 擬似デバイスモードかどうか
        final boolean mockEnabled = preferences_.getBoolean(
                PrefActivity.KEY_MOCK_DEVICE_ENABLED, false);
        if (mockEnabled) {
            Log.i(TAG, "mock device enabled.");

            final String mockResultCodeStr;
            mockResultCodeStr = preferences_.getString(
                    PrefActivity.KEY_MOCK_DEVICE_RESULT_CODE, "" + RESULT_OK);
            int mockResultCode;
            try {
                mockResultCode = Integer.parseInt(mockResultCodeStr);
            } catch (NumberFormatException e) {
                mockResultCode = RESULT_UNEXPECTED_ERROR;
            }
            final MockDeviceAsyncTask task = new MockDeviceAsyncTask();
            task.execute(Integer.valueOf(mockResultCode));
            return;
        }

        final boolean connecting = connect();
        if (!connecting) {
            setResultWithLog(RESULT_UNEXPECTED_ERROR);
            dismissProgress();
            finish();
        }
    }

    /**
     * {@link Activity} がフロントではなくなる際の処理を実装するメソッドです。
     */
    @Override
    protected void onPause() {
        Log.d(TAG, "enter onPause(): " + hashCode());
        try {
            super.onPause();

            final Felica felica = felica_;
            felica_ = null;
            if (felica != null) {
                unbindService(service_);
            }
        } finally {
            dismissProgress();
        }
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

    @CheckForNull
    private Context getCallerContext() {
        final String initiatorPackage = getCallingPackage();
        try {
            final Context callerContext = createPackageContext(
                    initiatorPackage, Context.CONTEXT_RESTRICTED);
            return callerContext;
        } catch (NameNotFoundException e) {
            Log.e(TAG, "failed to obtain context of caller. package name: "
                    + initiatorPackage, e);
            return null;
        }
    }

    private boolean isValidAudioResource(@CheckForNull final Context context,
            final int resId) {
        if (context == null) {
            return false;
        }
        try {
            final String resTypeName = context.getResources()
                    .getResourceTypeName(resId);
            return "raw".equals(resTypeName);
        } catch (NotFoundException e) {
            return false;
        }
    }

    private int getSoundResId(final String soundName) {
        final Integer soundResId = SOUND_ID_MAP.get(soundName);
        if (soundResId == null) {
            // 存在しない名前のリソース
            Log.w(TAG, "sound resource not found for '" + soundName + "'.");
            return -1;
        }
        return soundResId.intValue();
    }

    /**
     * サービスへのバインドを開始し、{@link Felica} インスタンス獲得処理を開始します。
     *
     * @return
     * サービスのバインドを開始した場合は {@code true}、開始できなかった場合は {@code false}
     * を返します。 {@code true} を返した場合は、 {@link #service_} の
     * {@code onServiceConnected(ComponentName, IBinder)} が呼び出されます。
     */
    @CheckForNull
    public boolean connect() {
        if (felica_ != null) {
            throw new IllegalStateException("already connected to FeliCa.");
        }
        final Intent intent = new Intent();
        intent.setClass(this, Felica.class);
        final boolean result = bindService(intent, service_,
                Context.BIND_AUTO_CREATE);
        return result;
    }

    /**
     * {@code Felica} サービスとの接続を管理するクラスです。
     */
    private final ServiceConnection service_ = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            final Felica felica = ((Felica.LocalBinder) service).getInstance();
            felica_ = felica;
            if (felica == null) {
                // ありえないけど念のため
                setResultWithLog(RESULT_UNEXPECTED_ERROR);
                finish();
                return;
            }

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
                final int resultCode = FelicaUtil.logAndGetResultCode(TAG, e,
                        "activateFelica()");
                setResultWithLog(resultCode);
                finish();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            final Felica felica = felica_;
            if (felica != null) {
                setResultWithLog(RESULT_UNEXPECTED_ERROR);
                finish();
            }
            felica_ = null;
        }
    };

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

    private final Handler progressMessageHandler_ = new Handler();

    /**
     * プログレスダイアログのメッセージをセットします。
     *
     * <p>
     * プログレスダイアログが表示されていない場合はなにもしません。
     * </p>
     *
     * @param message
     * メッセージ用の文字列。{@code null} を指定した場合はメッセージの変更を行いません。
     */
    private void setProgressMessage(@CheckForNull final String message) {
        final ProgressDialog progress = progress_;
        if (progress == null) {
            return;
        }
        progressMessageHandler_.post(new Runnable() {
            @Override
            public void run() {
                progress.setMessage(message);
            }
        });
    }

    /**
     * プログレスダイアログを破棄します。
     *
     * <p>
     * プログレスダイアログが表示されていない場合はなにもしません。
     * </p>
     */
    private void dismissProgress() {
        final ProgressDialog progress = progress_;
        Log.d(TAG, "dismiss progress dialog called"
                + (progress == null ? "(progress_ == null)" : "") + ": "
                + hashCode());
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
        // FIXME エラーコードを正しくセットする <- activate 失敗以外でも呼ばれそうなので要確認
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
            final int resultCode = FelicaUtil.logAndGetResultCode(TAG, e,
                    "open()");
            return resultCode;
        }

        Log.d(TAG, "sending FeliCa message");
        int retryCount = 0;
        final long startTime = SystemClock.uptimeMillis();
        long now;
        while ((now = SystemClock.uptimeMillis()) - startTime < timeoutOfSending_
                && retryCount < RETRY_LIMIT) {
            try {
                // 送信中メッセージ
                setProgressMessage(getString(
                        R.string.progress_msg_sending_with_remaining_time,
                        Long.valueOf(TimeUnit.MILLISECONDS
                                .toSeconds(timeoutOfSending_
                                        - (now - startTime)))));
                // Push送信
                felica.push(segment);
                Log.i(TAG, "FeliCa message has been sent successfully.");

                // 送信完了メッセージ
                setProgressMessage(getString(R.string.progress_msg_sent));

                notifyBySoundAndVibrator();

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

                final int resultCode = FelicaUtil.logAndGetResultCode(TAG, e,
                        "push()");
                return resultCode;
            }
        }
        return RESULT_TIMEOUT;
    }

    private void notifyBySoundAndVibrator() {
        // sound
        boolean soundMode = preferences_.getBoolean(
                PrefActivity.KEY_SOUND_MODE, true);
        if (soundMode) {
            if (0 < soundOnSent_) {
                assert contextOfSoundOnSent_ != null;
                final MediaPlayer mediaPlayer = MediaPlayer.create(
                        contextOfSoundOnSent_, soundOnSent_);
                mediaPlayer.setOnCompletionListener(RELEASE_PLAYER_LISTENER);
                mediaPlayer.start();
            }
        }
        // 振動
        boolean vibrateMode = preferences_.getBoolean(
                PrefActivity.KEY_VIBRATION_MODE, true);
        if (vibrateMode) {
            Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
            vibrator.vibrate(VIBRATOR_PATTERN, -1);
        }
    }

    /**
     * アクティビティのリザルトコードをセットし、セットした内容をログに出力します。
     * @param resultCode
     * リザルトコード。
     */
    private void setResultWithLog(final int resultCode) {
        Log.d(TAG, "set result code: " + resultCode);
        setResult(resultCode);
    }

    /**
     * 擬似デバイスとして動作するときのための処理を実装したクラスです。
     */
    private final class MockDeviceAsyncTask extends
            AsyncTask<Integer, Void, Void> {

        @Override
        @CheckForNull
        protected Void doInBackground(Integer... args) {
            Log.d(TAG, "enter doInBackground(): " + hashCode());

            final int mockResultCode = args[0].intValue();
            setResultWithLog(mockResultCode);

            SystemClock.sleep(500L);
            if (mockResultCode != RESULT_TIMEOUT) {
                SystemClock.sleep(1000L);
                if (mockResultCode == RESULT_OK) {
                    notifyBySoundAndVibrator();
                }
                return null;
            }

            int retryCount = 0;
            final long startTime = SystemClock.uptimeMillis();
            long now;
            while ((now = SystemClock.uptimeMillis()) - startTime < timeoutOfSending_
                    && retryCount < RETRY_LIMIT) {
                // 送信中メッセージ
                setProgressMessage(getString(
                        R.string.progress_msg_sending_with_remaining_time,
                        Long.valueOf(TimeUnit.MILLISECONDS
                                .toSeconds(timeoutOfSending_
                                        - (now - startTime)))));
                SystemClock.sleep(100L);
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            dismissProgress();
            finish();
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
            dismissProgress();
            finish();
        }

    }

}
