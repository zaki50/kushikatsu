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
package jp.andeb.kushikatsu;

import static jp.andeb.kushikatsu.util.ActivityUtil.setupActivityView;

import java.util.concurrent.TimeUnit;

import jp.andeb.kushikatsu.util.FelicaServiceConnection;
import jp.andeb.kushikatsu.util.FelicaUtil;
import android.app.Activity;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Parcelable;
import android.os.SystemClock;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.felicanetworks.mfc.AppInfo;
import com.felicanetworks.mfc.Felica;
import com.felicanetworks.mfc.FelicaEventListener;
import com.felicanetworks.mfc.FelicaException;
import com.felicanetworks.mfc.PushIntentSegment;

public class SendActivity extends Activity implements FelicaEventListener {

    private static final String INTERNAL_INTENT = "EXTRA_INTENT";

    private TextView view = null;

    private Intent internalIntent = null;

    Felica felica = null;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setupActivityView(this, R.layout.send, R.drawable.icon);

        Log.i(SendActivity.class.getSimpleName(), "onCreate1");

        view = (TextView) findViewById(R.id.send_status);

        internalIntent = getInternalIntent();

        FelicaServiceConnection conn = FelicaServiceConnection.getInstance();
        Log.i(SendActivity.class.getSimpleName(), "onCreate2");
        conn.setContext(getApplicationContext(),
                new FelicaServiceConnection.Listener() {

                    @Override
                    public void connected(final Felica felica) {
                        SendActivity.this.felica = felica;
                        Log.i(SendActivity.class.getSimpleName(),
                                "connected to FeliCa");
                        try {
                            Log.i(SendActivity.class.getSimpleName(),
                                    "activating FeliCa");
                            felica.activateFelica(null, SendActivity.this);
                        } catch (IllegalArgumentException e) {
                            Log.e(SendActivity.class.getSimpleName(), "", e);
                        } catch (FelicaException e) {
                            Log.e(SendActivity.class.getSimpleName(),
                                    FelicaUtil.toString(e), e);
                        }
                    }

                    @Override
                    public void disconnected() {
                        Toast.makeText(getApplicationContext(), "disconnected",
                                Toast.LENGTH_LONG).show();
                    }
                });
        felica = conn.connect();
        if (felica != null) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        felica.activateFelica(null, SendActivity.this);
                    } catch (IllegalArgumentException e) {
                        Log.e(SendActivity.class.getSimpleName(), "", e);
                    } catch (FelicaException e) {
                        Log.e(SendActivity.class.getSimpleName(),
                                FelicaUtil.toString(e), e);
                    }
                }
            }).start();
        }
        Log.i(SendActivity.class.getSimpleName(), "onCreate3");
    }

    private Intent getInternalIntent() {
        final Intent initiator = getIntent();
        if (initiator == null) {
            return null;
        }
        final Parcelable internalIntent;
        internalIntent = initiator.getParcelableExtra(INTERNAL_INTENT);
        if (!(internalIntent instanceof Intent)) {
            return null;
        }
        return (Intent) internalIntent;
    }

    @Override
    public void finished() {
        Log.i(SendActivity.class.getSimpleName(), "FeliCa activated");
        try {
            boolean succeeded = false;

            requestToDisplay("オープンします");
            Log.i(SendActivity.class.getSimpleName(), "open");
            felica.open();
            try {
                requestToDisplay("オープンしました");
                Log.i(SendActivity.class.getSimpleName(), "opened");

                final long PUSH_TIMEOUT = TimeUnit.SECONDS.toMillis(5L);
                final long startTime = SystemClock.uptimeMillis();
                while (SystemClock.uptimeMillis() - startTime < PUSH_TIMEOUT) {

                    try {
                        // Androidインテント実行パラメータの生成
                        PushIntentSegment pushSegment;
                        pushSegment = new PushIntentSegment(internalIntent);

                        Log.i(SendActivity.class.getSimpleName(),
                                "sending FeliCa message");
                        // Push送信
                        felica.push(pushSegment);
                        succeeded = true;

                        MediaPlayer mediaPlayer = MediaPlayer.create(this,
                                R.raw.se9);
                        mediaPlayer.start();

                        Log.i(SendActivity.class.getSimpleName(),
                                "FeliCa message sent");
                        break;
                    } catch (IllegalArgumentException e) {
                        // 不正な引数が指定された場合
                        Log.e(SendActivity.class.getSimpleName(), e.getClass()
                                .getSimpleName(), e);
                        requestToDisplay(e.getMessage());
                    } catch (FelicaException e) {
                        // FelicaExceptionをキャッチした場合
                        Log.e(SendActivity.class.getSimpleName(),
                                FelicaUtil.toString(e), e);
                        requestToDisplay(e.getMessage());
                    } catch (Exception e) {
                        // 予期せぬ例外をキャッチした場合
                        Log.e(SendActivity.class.getSimpleName(), e.getClass()
                                .getSimpleName(), e);
                        requestToDisplay(e.getMessage());
                    }
                }

            } finally {
                try {
                    felica.close();
                } finally {
                    try {
                        felica.inactivateFelica();
                    } finally {
                        setResult(succeeded ? Activity.RESULT_OK
                                : Activity.RESULT_CANCELED);
                        finish();
                    }
                }
            }
        } catch (FelicaException e) {
            Log.e(SendActivity.class.getSimpleName(),
                    "通信失敗: " + FelicaUtil.toString(e), e);
        }
    }

    @Override
    public void errorOccurred(final int arg0, final String arg1,
            final AppInfo arg2) {
        Log.i(SendActivity.class.getSimpleName(), "failed to activate FeliCa");
        Toast.makeText(SendActivity.this, "failed to activate FeliCa",
                Toast.LENGTH_SHORT).show();
    }

    private void requestToDisplay(final String text) {
        // new Handler().post(new Runnable() {
        // @Override
        // public void run() {
        // view.setText(text);
        // }
        // });
    }
}
