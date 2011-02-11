/*
 * Copyright 2011 Sosuke Masui <esmasui@gmail.com>
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

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;

import net.kazzz.felica.FeliCaException;
import net.kazzz.felica.lib.FeliCaLib;
import net.kazzz.felica.lib.FeliCaLib.CommandPacket;
import net.kazzz.felica.lib.FeliCaLib.IDm;

import android.content.Intent;

import com.felicanetworks.mfc.PushIntentSegment;
import com.felicanetworks.mfc.PushSegment;
import com.felicanetworks.mfc.PushStartAppSegment;
import com.felicanetworks.mfc.PushStartBrowserSegment;
import com.felicanetworks.mfc.PushStartMailerSegment;

import edu.umd.cs.findbugs.annotations.CheckForNull;

public class PushCommand extends CommandPacket {

    public static final byte PUSH = (byte) 0xb0;

    static {
        FeliCaLib.commandMap.put(PUSH, "Push");
    }

    public PushCommand(IDm idm, PushSegment segment) throws FeliCaException {
        super(PUSH, idm, packContent(packSegment(buildData(segment))));
    }

    private static byte[] packContent(byte[] segments) {
        byte[] buffer = new byte[segments.length + 1];
        buffer[0] = (byte) segments.length;
        System.arraycopy(segments, 0, buffer, 1, segments.length);
        return buffer;
    }

    private static byte[] packSegment(byte[]... segments) {

        int bytes = 3; // 個別部数(1byte) + チェックサム(2bytes)
        for (int i = 0; i < segments.length; ++i)
            bytes += segments[i].length;

        ByteBuffer buffer = ByteBuffer.allocate(bytes);

        // 個別部数
        buffer.put((byte) segments.length);

        // 個別部
        for (int i = 0; i < segments.length; ++i)
            buffer.put(segments[i]);

        // チェックサム
        int sum = segments.length;
        for (int i = 0; i < segments.length; ++i) {
            byte[] e = segments[i];
            for (int j = 0; j < e.length; ++j)
                sum += e[j];
        }
        int checksum = -sum & 0xffff;

        putAsBigEndian(checksum, buffer);

        return buffer.array();
    }

    private static byte[][] buildData(PushSegment segment)
            throws FeliCaException {
        if (segment instanceof PushIntentSegment) {
            return buildPushIntentSegment((PushIntentSegment) segment);
        }
        if (segment instanceof PushStartBrowserSegment) {
            return buildPushStartBrowserSegment((PushStartBrowserSegment) segment);
        }
        if (segment instanceof PushStartMailerSegment) {
            return buildPushStartMailerSegment((PushStartMailerSegment) segment);
        }
        if (segment instanceof PushStartAppSegment) {
            return buildPushStartAppSegment((PushStartAppSegment) segment);
        }
        throw new IllegalArgumentException("not supported " + segment);
    }

    private static byte[][] buildPushStartBrowserSegment(
            PushStartBrowserSegment segment) {

        final int type = segment.getType();
        final String url = segment.getURL();
        final String browserStartupParam = segment.getBrowserStartupParam();

        byte[] urlByte = url.getBytes();
        byte[] browserStartupParamByte = browserStartupParam == null ? new byte[0]
                : browserStartupParam.getBytes();

        int capacity = urlByte.length + browserStartupParamByte.length + 5;// type(1byte)
                                                                            // +
                                                                            // paramSize(2bytes)
                                                                            // +
                                                                            // urlLength(2bytes)
        ByteBuffer buffer = ByteBuffer.allocate(capacity);

        // 個別部ヘッダ

        // 起動制御情報
        buffer.put((byte) type);
        // 個別部パラメータサイズ
        int paramSize = urlByte.length + browserStartupParamByte.length + 2; // urlLength(2bytes)
        putAsLittleEndian(paramSize, buffer);

        // 個別部パラメータ

        // URLサイズ
        putAsLittleEndian(urlByte.length, buffer);
        // URL
        buffer.put(urlByte);
        // (ブラウザスタートアップパラメータ)
        buffer.put(browserStartupParamByte);

        return new byte[][] { buffer.array() };
    }

    private static final Charset MAIL_ADDRESS_CHARSET = Charset.forName("iso8859-1");
    private static final Charset MAIL_SUBJECT_CHARSET = Charset.forName("Shift_JIS");
    private static final Charset MAIL_BODY_CHARSET = MAIL_SUBJECT_CHARSET;
    private static final Charset MAIL_STARTUP_PARAM_CHARSET = MAIL_SUBJECT_CHARSET;
    private static final byte[] EMPTY_BYTES = new byte[0];

    private static byte[][] buildPushStartMailerSegment(PushStartMailerSegment segment) {

        final int type = segment.getType();
        @CheckForNull
        final String[] toAddresses = segment.getToAddress();
        @CheckForNull
        final String[] ccAddresses = segment.getCcAddress();
        @CheckForNull
        final String subject = segment.getSubject();
        @CheckForNull
        final String body = segment.getBody();
        @CheckForNull
        final String mailerStartupParam = segment.getMailerStartupParam();

        final byte[] toAddrsBytes = getJoinedBytes(toAddresses, MAIL_ADDRESS_CHARSET, (byte) ',');
        final byte[] ccAddrsBytes = getJoinedBytes(ccAddresses, MAIL_ADDRESS_CHARSET, (byte) ',');
        final byte[] subjectBytes = (subject == null) ? EMPTY_BYTES : subject
                .getBytes(MAIL_SUBJECT_CHARSET);
        final byte[] bodyBytes = (body == null) ? EMPTY_BYTES : body.getBytes(MAIL_BODY_CHARSET);
        final byte[] mailerStartupParamByte = (mailerStartupParam == null) ? EMPTY_BYTES
                : mailerStartupParam.getBytes(MAIL_STARTUP_PARAM_CHARSET);

        final int capacity = toAddrsBytes.length + ccAddrsBytes.length + subjectBytes.length
                + bodyBytes.length + mailerStartupParamByte.length + 11; // type(1byte)
                                                                         // +
                                                                         // paramSize(2bytes)
                                                                         // +
                                                                         // toLength(2bytes)
                                                                         // +
                                                                         // ccLength(2bytes)
                                                                         // +
                                                                         // subjectLength(2bytes)
                                                                         // +
                                                                         // bodyLength(2bytes)
        ByteBuffer buffer = ByteBuffer.allocate(capacity);

        // 個別部ヘッダ

        // 起動制御情報
        buffer.put((byte) type);
        // 個別部パラメータサイズ
        int paramSize = capacity - 3/*type(1byte) + paramSize(2bytes)*/;
        putAsLittleEndian(paramSize, buffer);

        // 個別部パラメータ

        // Toサイズ
        putAsLittleEndian(toAddrsBytes.length, buffer);
        // To
        buffer.put(toAddrsBytes);
        // Ccサイズ
        putAsLittleEndian(ccAddrsBytes.length, buffer);
        // Cc
        buffer.put(ccAddrsBytes);
        // Subject サイズ
        putAsLittleEndian(subjectBytes.length, buffer);
        // Subject
        buffer.put(subjectBytes);
        // Body サイズ
        putAsLittleEndian(bodyBytes.length, buffer);
        // Body
        buffer.put(bodyBytes);
        // メーラスタートアップパラメータ
        buffer.put(mailerStartupParamByte);

        return new byte[][] { buffer.array() };
    }

    // TODO いろいろテストすること
    static byte[] getJoinedBytes(@CheckForNull String[] strs, Charset charset, byte separator) {
        if (strs == null) {
            return EMPTY_BYTES;
        }

        int totalStrByteLength = 0;
        int nullCount = 0;
        final byte[][] strBytes = new byte[strs.length][];
        for (int i = 0; i < strs.length; i++) {
            final String str = strs[i];
            if (str == null) {
                strBytes[i] = null;
                nullCount++;
                continue;
            }
            strBytes[i] = str.getBytes(charset);
            totalStrByteLength += strBytes[i].length;
        }
        totalStrByteLength += Math.max(0, strs.length - nullCount - 1); // separator の分

        final byte[] result = new byte[totalStrByteLength];
        int nextHeadIndex = 0;
        for (int i = 0; i < strBytes.length; i++) {
            final byte[] bytes = strBytes[i];
            if (bytes == null) {
                continue;
            }
            final int length = strBytes[i].length;
            System.arraycopy(strBytes[i], 0, result, nextHeadIndex, length);
            nextHeadIndex += length;
            if (nextHeadIndex != totalStrByteLength) {
                // 最後の要素ではない場合
                result[nextHeadIndex] = separator;
                nextHeadIndex++;
            }
        }
        return result;
    }

    private static byte[][] buildPushIntentSegment(PushIntentSegment segment) {
        final Intent intent = segment.getIntentData();
        final String intentUrl = intent.toUri(Intent.URI_INTENT_SCHEME);

        return buildPushIntentSegment(intentUrl);
    }

    private static byte[][] buildPushIntentSegment(String intentUrl) {
        return buildPushIappliSegment("", "ANDR01", new String[] {intentUrl});
    }

    private static final Charset APP_URL_CHARSET = Charset.forName("iso8859-1");
    private static final Charset APP_ICC_CHARSET = Charset.forName("iso8859-1");
    private static final Charset APP_PARAM_CHARSET = Charset.forName("Shift_JIS");

    private static byte[][] buildPushStartAppSegment(PushStartAppSegment segment) {
        final String url = segment.getAppURL();
        final String icc = segment.getAppIdentificationCode();
        final String[] appStarupParam = segment.getAppStartupParam();
        return buildPushIappliSegment(url, icc, appStarupParam);
    }

    private static byte[][] buildPushIappliSegment(@CheckForNull String url,
            @CheckForNull String icc, @CheckForNull String[] appParam) {
        if (url == null) {
            url = "";
        }
        if (icc == null) {
            icc = "";
        }

        byte[] urlBytes = url.getBytes(APP_URL_CHARSET);
        byte[] iccBytes = icc.getBytes(APP_ICC_CHARSET);
        byte[] appParamBytes = getJoinedBytes(appParam, APP_PARAM_CHARSET, (byte) ' ');

        int capacity = urlBytes.length + iccBytes.length + appParamBytes.length + 7;// type(1byte)
                                                                                    // +
                                                                                    // paramBytesLength(2bytes)
                                                                                    // +
                                                                                    // urlBytesLength(2bytes)
                                                                                    // +
                                                                                    // iccBytesLength(2bytes)

        ByteBuffer buffer = ByteBuffer.allocate(capacity);

        // 個別部ヘッダ

        // 起動制御情報
        buffer.put((byte) 1);
        // 個別部パラメータサイズ
        int paramSize = capacity - 3; // type(1byte)
                                      // +
                                      // paramBytesLength(2)
        putAsLittleEndian(paramSize, buffer);

        // 個別部パラメータ

        // URLサイズ
        putAsLittleEndian(urlBytes.length, buffer);
        // URL
        buffer.put(urlBytes);
        // iccサイズ
        putAsLittleEndian(iccBytes.length, buffer);
        // icc
        buffer.put(iccBytes);
        // (アプリケーション起動パラメータ)
        buffer.put(appParamBytes);

        return new byte[][] {
            buffer.array()
        };
    }

    @SuppressWarnings("unused")
    private static byte[][] buildPushVibrationSegment(byte pattern, byte count, String message) {

        byte[] messageBytes;
        try {
            messageBytes = message.getBytes("Shift_JIS");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
        int capacity = messageBytes.length + 5;// type(1byte)
                                               // +
                                               // paramSize(2bytes)
                                               // +
                                               // pattern(1byte)
                                               // +
                                               // count(1byte)
        ByteBuffer buffer = ByteBuffer.allocate(capacity);

        // 個別部ヘッダ

        // 起動制御情報
        buffer.put((byte) 4);
        // 個別部パラメータサイズ
        int paramSize = messageBytes.length + 2; // pattern(1byte) + count(1byte)
        putAsLittleEndian(paramSize, buffer);

        // 個別部パラメータ

        // pattern
        buffer.put(pattern);
        // count
        buffer.put(count);
        // URL
        buffer.put(messageBytes);

        return new byte[][] {
            buffer.array()
        };
    }

    private static void putAsLittleEndian(int i, ByteBuffer buffer) {
        buffer.put((byte) ((i >> 0) & 0xff));
        buffer.put((byte) ((i >> 8) & 0xff));
    }

    private static void putAsBigEndian(int i, ByteBuffer buffer){
        buffer.put((byte) ((i >> 8) & 0xff));
        buffer.put((byte) ((i >> 0) & 0xff));
    }
}
