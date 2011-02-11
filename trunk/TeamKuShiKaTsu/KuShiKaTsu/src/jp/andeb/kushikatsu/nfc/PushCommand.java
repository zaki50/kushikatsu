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
import net.kazzz.felica.lib.FeliCaLib;
import net.kazzz.felica.lib.FeliCaLib.CommandPacket;
import net.kazzz.felica.lib.FeliCaLib.IDm;

import com.felicanetworks.mfc.PushIntentSegment;
import com.felicanetworks.mfc.PushSegment;
import com.felicanetworks.mfc.PushStartAppSegment;
import com.felicanetworks.mfc.PushStartBrowserSegment;
import com.felicanetworks.mfc.PushStartMailerSegment;

import edu.umd.cs.findbugs.annotations.CheckForNull;

/**
 * Push 送信するコマンドのための抽象クラスです。
 */
public abstract class PushCommand extends CommandPacket {

    public static final byte PUSH = (byte) 0xb0;

    static {
        FeliCaLib.commandMap.put(PUSH, "Push");
    }

    /**
     * 渡された {@link PushSegment} から {@link PushCommand} インスタンスを構築します。
     *
     * @param idm
     * 対象の IDm。{@code null} 禁止。
     * @param segment
     * {@link PushSegment} オブジェクト。{@code null} 禁止。
     * @return
     * 構築された {@link PushCommand} オブジェクト。 指定された {@link PushSegment} に応じて
     * 適切なサブクラスのインスタンスが構築されます。
     * @throws FeliCaException
     * 指定された情報から {@link PushStartMailerCommand} が構築できない場合。
     * @throws IllegalArgumentException
     * {@code null} 禁止の引き数に {@code null} を渡した場合。
     */
    public static PushCommand create(IDm idm, PushSegment segment) throws FeliCaException {
        if (idm == null) {
            throw new IllegalArgumentException("'idm' must not be null.");
        }
        if (segment == null) {
            throw new IllegalArgumentException("'segment' must not be null.");
        }
        if (segment instanceof PushIntentSegment) {
            return new PushStartAppCommand(idm, (PushIntentSegment) segment);
        }
        if (segment instanceof PushStartAppSegment) {
            return new PushStartAppCommand(idm, (PushStartAppSegment) segment);
        }
        if (segment instanceof PushStartBrowserSegment) {
            return new PushStartBrowserCommand(idm, (PushStartBrowserSegment) segment);
        }
        if (segment instanceof PushStartMailerSegment) {
            return new PushStartMailerCommand(idm, (PushStartMailerSegment) segment);
        }
        throw new FeliCaException("unsupported push segment: " + segment.getClass().getSimpleName());
    }

    protected PushCommand(IDm idm, byte[][] segments) throws FeliCaException {
        super(PUSH, idm, packContent(packSegments(segments)));
    }

    private static byte[] packContent(byte[] segments) {
        byte[] buffer = new byte[segments.length + 1];
        buffer[0] = (byte) segments.length;
        System.arraycopy(segments, 0, buffer, 1, segments.length);
        return buffer;
    }

    private static byte[] packSegments(byte[]... segments) {
        int bytes = 3; // 個別部数(1byte) + チェックサム(2bytes)
        for (int i = 0; i < segments.length; ++i) {
            bytes += segments[i].length;
        }

        final ByteBuffer buffer = ByteBuffer.allocate(bytes);

        // 個別部数
        buffer.put((byte) segments.length);

        // 個別部
        for (int i = 0; i < segments.length; ++i) {
            buffer.put(segments[i]);
        }

        // チェックサム
        int sum = segments.length;
        for (int i = 0; i < segments.length; ++i) {
            byte[] e = segments[i];
            for (int j = 0; j < e.length; ++j) {
                sum += e[j];
            }
        }
        final int checksum = -sum & 0xffff;
        putShortAsBigEndian(checksum, buffer);

        return buffer.array();
    }

    protected static final byte[] EMPTY_BYTES = new byte[0];

    protected static byte[] getBytes(@CheckForNull String str, Charset charset) {
        if (str == null) {
            return EMPTY_BYTES;
        }
        return str.getBytes(charset);
    }

    // TODO いろいろテストすること
    protected static byte[] getJoinedBytes(@CheckForNull String[] strs, Charset charset, byte separator) {
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

    protected static void putShortAsLittleEndian(int i, ByteBuffer buffer) {
        buffer.put((byte) ((i >> 0) & 0xff));
        buffer.put((byte) ((i >> 8) & 0xff));
    }

    protected static void putShortAsBigEndian(int i, ByteBuffer buffer) {
        buffer.put((byte) ((i >> 8) & 0xff));
        buffer.put((byte) ((i >> 0) & 0xff));
    }
}
