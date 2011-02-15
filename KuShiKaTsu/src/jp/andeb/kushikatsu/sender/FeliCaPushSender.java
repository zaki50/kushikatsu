/*
 * Copyright 2011 Android DEvelopers' cluB
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

package jp.andeb.kushikatsu.sender;

import static jp.andeb.kushikatsu.helper.KushikatsuHelper.RESULT_UNEXPECTED_ERROR;
import static jp.andeb.kushikatsu.util.FelicaUtil.closeQuietly;
import static jp.andeb.kushikatsu.util.FelicaUtil.inactivateQuietly;
import jp.andeb.kushikatsu.SendActivity;
import jp.andeb.kushikatsu.util.FelicaUtil;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;

import com.felicanetworks.mfc.Felica;
import com.felicanetworks.mfc.FelicaException;
import com.felicanetworks.mfc.PushSegment;

/**
 * FeliCa ライブラリ(MFC) を使用して、Push 送信を行うクラスです。
 */
public class FeliCaPushSender implements PushSender {
    private static final String TAG = FeliCaPushSender.class.getSimpleName();

    private final SendActivity act_;

    private Felica felica_ = null;

    private volatile boolean canceled_ = false;

    public FeliCaPushSender(SendActivity act) {
        super();
        act_ = act;
    }

    /**
     * サービスへのバインドを開始し、{@link Felica} インスタンス獲得処理を開始します。
     *
     * @return
     * サービスのバインドを開始した場合は {@code true}、開始できなかった場合は {@code false}
     * を返します。 {@code true} を返した場合は、 {@link #service_} の
     * {@code onServiceConnected(ComponentName, IBinder)} が呼び出されます。
     */
    @Override
    public boolean connect() {
        if (felica_ != null) {
            throw new IllegalStateException("already connected to FeliCa.");
        }
        final Intent intent = new Intent();
        intent.setClass(act_, Felica.class);
        final boolean result = act_.bindService(intent, service_,
                Context.BIND_AUTO_CREATE);
        return result;
    }

    @Override
    public void disconnect() {
        closeQuietly(felica_, TAG);
        inactivateQuietly(felica_, TAG);
        unbindServiceQuietly();
    }

    @Override
    public void open() throws FelicaException {
        felica_.open();
    }

    @Override
    public void push(PushSegment segment) throws FelicaException {
        felica_.push(segment);
    }

    @Override
    public void cancel() {
        canceled_ = true;
    }

    @Override
    public boolean isCanceled() {
        return canceled_;
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
                act_.setResultWithLog(RESULT_UNEXPECTED_ERROR);
                act_.finish();
                return;
            }

            Log.i(TAG, "connected to FeliCa service");
            try {
                Log.i(TAG, "activating FeliCa");
                canceled_ = false;
                felica.activateFelica(null, act_);
            } catch (IllegalArgumentException e) {
                Log.e(TAG, e.getClass().getSimpleName()
                        + " thrown on activateFelica()", e);
                act_.setResultWithLog(RESULT_UNEXPECTED_ERROR);
                act_.finish();
            } catch (FelicaException e) {
                final int resultCode = FelicaUtil.logAndGetResultCode(TAG, e,
                        "activateFelica()");
                act_.setResultWithLog(resultCode);
                act_.finish();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            final Felica felica = felica_;
            if (felica != null) {
                act_.setResultWithLog(RESULT_UNEXPECTED_ERROR);
                act_.finish();
            }
            felica_ = null;
        }
    };

    private void unbindServiceQuietly() {
        try {
            act_.unbindService(service_);
        } catch (Exception e) {
            Log.e(TAG, "failed to unbind service.", e);
        }
    }


}
