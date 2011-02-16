/*
 * Copyright 2011 Sosuke Masui <esmasui@gmail.com>,
 *                Makoto Yamazaki <makoto1975@gmail.com>
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

package jp.andeb.kushikatsu.nfc;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

import net.kazzz.felica.FeliCaException;
import net.kazzz.felica.lib.FeliCaLib.IDm;

import android.content.Intent;

import com.felicanetworks.mfc.PushIntentSegment;
import com.felicanetworks.mfc.PushStartAppSegment;

import edu.umd.cs.findbugs.annotations.CheckForNull;

/**
 * アプリ起動や Intent を Push 送信するコマンドのためのクラスです。
 */
public final class PushStartAppCommand extends PushCommand {

    /**
     * 指定された情報に従ってアプリ起動用の {@link PushStartAppCommand} を構築します。
     *
     * @param idm
     * 対象の IDm。{@code null} 禁止。
     * @param segment
     * 起動するアプリの情報を保持した {@link PushStartAppSegment} オブジェクト。{@code null} 禁止。
     * @throws FeliCaException
     * 指定された情報から {@link PushStartAppCommand} が構築できない場合。
     */
    public PushStartAppCommand(IDm idm, PushStartAppSegment segment) throws FeliCaException {
        this(idm, segment.getAppURL(), segment.getAppIdentificationCode(), segment
                .getAppStartupParam());
    }

    /**
     * 指定された情報に従って {@link Intent} 送信用の {@link PushStartAppCommand} を構築します。
     *
     * @param idm
     * 対象の IDm。{@code null} 禁止。
     * @param segment
     * 起動する {@link Intent} 情報を保持した {@link PushStartAppSegment} オブジェクト。{@code null} 禁止。
     * @throws FeliCaException
     * 指定された情報から {@link PushStartAppCommand} が構築できない場合。
     */
    public PushStartAppCommand(IDm idm, PushIntentSegment segment) throws FeliCaException {
        this(idm, null, "ANDR01", toAppParam(segment));
    }

    /**
     * 指定された情報に従ってアプリ起動用の {@link PushStartAppCommand} を構築します。
     *
     * @param idm
     * 対象の IDm。{@code null} 禁止。
     * @param url
     * アプリの URL。 {@code null} は空文字として扱います。
     * @param icc
     * アプリの起動チェックコード。 {@code null} は空文字として扱います。
     * @param appParam
     * アプリの起動パラメータ配列。 {@code null} は空配列として扱います。
     * @throws FeliCaException
     */
    public PushStartAppCommand(IDm idm, @CheckForNull String url, @CheckForNull String icc,
            @CheckForNull String[] appParam) throws FeliCaException {
        super(idm, buildPushAppSegment(url, icc, appParam));
    }

    private static String[] toAppParam(PushIntentSegment segment) {
        if (segment == null) {
            return null;
        }
        final Intent intent = segment.getIntentData();
        if (intent == null) {
            return null;
        }
        return new String[] {
            intent.toUri(Intent.URI_INTENT_SCHEME)
        };
    }

    private static final byte TYPE = 1;

    private static final Charset URL_CHARSET = Charset.forName("iso8859-1");

    private static final Charset ICC_CHARSET = Charset.forName("iso8859-1");

    private static final Charset STARTUP_PARAM_CHARSET = Charset.forName("Shift_JIS");

    private static byte[][] buildPushAppSegment(@CheckForNull String url,
            @CheckForNull String icc, @CheckForNull String[] appParam) {
        final byte[] urlBytes = PushCommand.getBytes(url, URL_CHARSET);
        final byte[] iccBytes = PushCommand.getBytes(icc, ICC_CHARSET);
        final byte[] appParamBytes = getJoinedBytes(appParam, " ", STARTUP_PARAM_CHARSET);

        final int capacity = urlBytes.length + iccBytes.length + appParamBytes.length//
                + 7;// type(1byte)
                    // +
                    // paramBytesLength(2bytes)
                    // +
                    // urlBytesLength(2bytes)
                    // +
                    // iccBytesLength(2bytes)

        final ByteBuffer buffer = ByteBuffer.allocate(capacity);

        // 個別部ヘッダ

        // 起動制御情報
        buffer.put(TYPE);
        // 個別部パラメータサイズ
        final int paramSize = capacity - 3; // type(1byte) + paramBytesLength(2)
        PushCommand.putShortAsLittleEndian(paramSize, buffer);

        // 個別部パラメータ

        // URLサイズ
        PushCommand.putShortAsLittleEndian(urlBytes.length, buffer);
        // URL
        buffer.put(urlBytes);
        // iccサイズ
        PushCommand.putShortAsLittleEndian(iccBytes.length, buffer);
        // icc
        buffer.put(iccBytes);
        // (アプリケーション起動パラメータ)
        buffer.put(appParamBytes);

        return new byte[][] {
            buffer.array()
        };
    }
}
