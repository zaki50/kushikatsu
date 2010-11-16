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

import jp.andeb.kushikatsu.SendActivity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;

import com.felicanetworks.mfc.Felica;

/**
 * {@link Felica} との接続( ServiceConnection)を管理するクラス。
 *
 * <p>
 * シングルトンなので、 {@link #getInstance()} を使用してインスタンスを取得してください。
 * </p>
 */
public class FelicaServiceConnection implements ServiceConnection {

    private static final FelicaServiceConnection instance;
    static {
        instance = new FelicaServiceConnection();
    }

    private Context context = null;
    private Listener listener = null;

    public interface Listener {

        public void connected(Felica felica);

        public void disconnected();
    }

    /**
     * {@link Felica} インスタンス。
     *
     * <p>
     * {@code null} でない場合は、FeliCa が有効化されていることを意味します。
     * </p>
     */
    private Felica felica = null;

    private FelicaServiceConnection() {
        // nothing to do
        assert true;
    };

    /**
     * {@link FelicaServiceConnection} のシングルトンインスタンスを返します。
     *
     * @return
     * {@link FelicaServiceConnection} インスタンス。
     */
    public static FelicaServiceConnection getInstance() {
        return instance;
    }

    public void setContext(Context context, Listener listener) {
        this.context = context;
        this.listener = listener;
    }

    /**
     * サービスへのバインドを開始し、{@link Felica} インスタンス獲得処理を開始します。
     *
     * @return
     * 既に {@link Felica} インスタンス獲得済みの場合は、そのインスタンを返します。
     * その場合はリスナの {@link Listener#connected(Felica) #connected(Felica)}
     * の呼び出しも行われません。インスタンスが獲得されていない場合は {@code null} を返します。
     */
    public Felica connect() {
        if (context == null) {
            throw new RuntimeException("connect error:Context is not set.");
        }

        if (felica != null) {
            Log.i(SendActivity.class.getSimpleName(), "already connected");
            // 接続済み
            // FIXME 返り値として返すのではなく、未獲得とおなように callback 経由で返すことを検討する
            return felica;
        }

        final Intent intent = new Intent();
        intent.setClass(context, Felica.class);
        if (!context.bindService(intent, this, Context.BIND_AUTO_CREATE)) {
            throw new RuntimeException(
                    "connect error:Context#bindService() failed.");
        }
        // 接続状態変更はonServiceConnected()が呼び出されたタイミングで実施
        return null;
    }

    public void disconnect() throws Exception {
        if (felica == null) {
            // FIXME context が null でない場合は接続途中なので対処する
            return;
        }

        if (context == null) {
            throw new Exception("connect error:Context is not set.");
        }

        context.unbindService(this);

        // 接続状態を解除
        felica = null;
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {

        // Felicaとの接続が確立されたので、Felicaインスタンスを取得する
        Felica felica = ((Felica.LocalBinder) service).getInstance();
        if (listener != null) {
            listener.connected(felica);
        }
        this.felica = felica;
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {

        // Felicaの設定解除
        if (listener != null) {
            listener.disconnected();
        }
        felica = null;
        listener = null;
    }

}
