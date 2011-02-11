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

import com.felicanetworks.mfc.PushStartBrowserSegment;

import edu.umd.cs.findbugs.annotations.CheckForNull;

public final class PushStartBrowserCommand extends PushCommand {

    public PushStartBrowserCommand(IDm idm, PushStartBrowserSegment segment) throws FeliCaException {
        this(idm, segment.getURL(), segment.getBrowserStartupParam());
    }

    public PushStartBrowserCommand(IDm idm, @CheckForNull String url,
            @CheckForNull String browserStartupParam) throws FeliCaException {
        super(idm, buildPushStartBrowserSegment(url, browserStartupParam));
    }

    private static final byte TYPE = (byte) 2;

    private static final Charset URL_CHARSET = Charset.forName("iso8859-1");

    private static final Charset STARTUP_PARAM_CHARSET = Charset.forName("iso8859-1");

    private static byte[][] buildPushStartBrowserSegment(@CheckForNull String url,
            @CheckForNull String browserStartupParam) {
        final byte[] urlByte = (url == null) ? PushCommand.EMPTY_BYTES : url.getBytes(URL_CHARSET);
        final byte[] browserStartupParamByte = (browserStartupParam == null) ? PushCommand.EMPTY_BYTES
                : browserStartupParam.getBytes(STARTUP_PARAM_CHARSET);

        final int capacity = urlByte.length + browserStartupParamByte.length //
                + 5;// type(1byte) + paramSize(2bytes) + urlLength(2bytes)

        final ByteBuffer buffer = ByteBuffer.allocate(capacity);

        // 個別部ヘッダ

        // 起動制御情報
        buffer.put(TYPE);
        // 個別部パラメータサイズ
        int paramSize = urlByte.length + browserStartupParamByte.length + 2; // urlLength(2bytes)
        putShortAsLittleEndian(paramSize, buffer);

        // 個別部パラメータ

        // URLサイズ
        putShortAsLittleEndian(urlByte.length, buffer);
        // URL
        buffer.put(urlByte);
        // (ブラウザスタートアップパラメータ)
        buffer.put(browserStartupParamByte);

        return new byte[][] {
            buffer.array()
        };
    }
}
