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
package jp.andeb.kushikatsu;

import jp.andeb.kushikatsu.helper.KushikatsuHelper;
import jp.andeb.kushikatsu.nfc.INfcAdapter;
import jp.andeb.kushikatsu.nfc.IRawTagConnection;
import jp.andeb.kushikatsu.nfc.PushCommand;
import jp.andeb.kushikatsu.nfc.utils.DelegateFactory;
import net.kazzz.felica.FeliCaException;
import net.kazzz.felica.lib.FeliCaLib.IDm;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.nfc.NfcAdapter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Parcelable;

import com.felicanetworks.mfc.PushSegment;

import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;

/**
 * 送信予約されている {@link PushSegment} を、 gingerbread から導入された NFC の
 * 機能で Push 送信する {@link Activity} です。
 *
 * @author YAMAZAKI Makoto &lt;<a href="mailto:makoto1975@gmail.com" >makoto1975@gmail.com</a>&gt;
 */
@DefaultAnnotation(NonNull.class)
public class NfcPushActivity extends Activity {

    private byte[] idm_ = null;
    private Parcelable tag_ = null;

    private PushSegment segment_ = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.send);
    }

    @Override
    protected void onStart() {
        super.onStart();

        final KushikatsuApplication app = (KushikatsuApplication) getApplication();

        final PushSegment segment = app.getPushSegment();
        if (segment == null) {
            setResult(RESULT_CANCELED);
            finish();
            return;
        }
        segment_ = segment;

        final Intent intent = getIntent();
        if (intent == null) {
            setResult(RESULT_CANCELED);
            finish();
            return;
        }
        idm_ = intent.getByteArrayExtra(NfcAdapter.EXTRA_ID);
        assert idm_ != null;
        tag_ = intent.getParcelableExtra(INfcAdapter.EXTRA_TAG);
        assert tag_ != null;
    }

    @Override
    protected void onResume() {
        super.onResume();
        final NfcAdapter nfcAdapter = NfcAdapter.getDefaultAdapter();
        if (nfcAdapter == null) {
            setResult(KushikatsuHelper.RESULT_DEVICE_NOT_FOUND);
            finish();
            return;
        }

        final ProgressDialog progress = new ProgressDialog(this);
        progress.setTitle(R.string.progress_title);
        progress.setCancelable(false);
        progress.show();

        new AsyncTask<Void, Void, Boolean>() {
            @Override
            protected Boolean doInBackground(Void... params) {
                try {
                    INfcAdapter iNfcAdapter = DelegateFactory.create(INfcAdapter.class,
                            nfcAdapter);

                    Object rawTagConnection = iNfcAdapter.createRawTagConnection(tag_);
                    IRawTagConnection iRawTagConnection = DelegateFactory.create(
                            IRawTagConnection.class, rawTagConnection);

                    final PushCommand pushCommand = PushCommand.create(new IDm(idm_), segment_);
                    final byte[] pushData = pushCommand.getBytes();
                    try {
                        iRawTagConnection.transceive(pushData);
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        iRawTagConnection.close();
                    }
                    return null;
                } catch (FeliCaException e) {
                    return null;
                }
            }

            @Override
            protected void onPostExecute(Boolean result) {
                super.onPostExecute(result);

                progress.dismiss();
                setResult(RESULT_OK);
                finish();
            }

            @Override
            protected void onCancelled() {
                super.onCancelled();

                progress.dismiss();
                setResult(RESULT_CANCELED);
                finish();
            }
        }.execute();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }
}
