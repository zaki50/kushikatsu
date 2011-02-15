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

import edu.umd.cs.findbugs.annotations.CheckForNull;

import net.kazzz.felica.FeliCaException;
import net.kazzz.felica.lib.FeliCaLib.IDm;

/**
 * バイブレータ振動を Push 送信するコマンドのためのクラスです。
 */
public final class PushVibrationCommand extends PushCommand {

    /**
     * 振動パターンのインタフェースです。
     */
    public interface IPattern {
        /**
         * 振動パターンを表す {@code byte} 値を返します。
         * @return
         * 振動パターン。
         */
        public byte patternValue();
    }

    public enum Pattern implements IPattern {
        /**
         * 連続振動(指定不可)
         */
        ON((byte) 0), //

        /**
         * 0.5秒振動、0.5秒停止
         */
        ON_05_OFF_05((byte) 1), //

        /**
         * 1秒振動、1秒停止
         */
        ON_1_OFF_1((byte) 2), //

        /**
         * 2秒振動、1秒停止
         */
        ON_2_OFF_1((byte) 3), //

        /**
         * 3秒振動、1秒停止
         */
        ON_3_OFF_1((byte) 4);

        /**
         * 振動パターン値。
         */
        private final byte patternValue_;

        /**
         * 指定された振動パターン値でインスタンスを構築します。
         *
         * @param patternValue
         * 振動パターン値。
         */
        private Pattern(byte patternValue) {
            patternValue_ = patternValue;
        }

        @Override
        public byte patternValue() {
            return patternValue_;
        }
    };

    /**
     * 指定された情報に従って {@link PushVibrationCommand} を構築します。
     *
     * @param idm
     * 対象の IDm。{@code null} 禁止。
     * @param pattern
     * 振動パターン。通常は {@link Pattern} の要素のいずれかを指定してください。
     * {@link Pattern} の要素に存在しないものを指定する必要がある場合は、 {@link IPattern}
     * を実装するクラスを作成してください。
     * @param count
     * 振動回数。 {@code 1} 以上 {@code 3} 以下を指定してください。
     * この範囲外の値を指定した場合であってもエラーとはせず、 {@code byte} 型にキャストした値を
     * 使用します。
     * @param message
     * push するメッセージを指定します。{@code null} が指定された場合は空文字として扱います。
     * @throws FeliCaException
     */
    public PushVibrationCommand(IDm idm, IPattern pattern, int count, @CheckForNull String message)
            throws FeliCaException {
        super(idm, buildPushVibrationSegment(pattern, count, message));
    }

    private static final byte TYPE = (byte) 4;
    private static final Charset MESSAGE_CHARSET = Charset.forName("Shift_JIS");

    private static byte[][] buildPushVibrationSegment(IPattern pattern, int count,
            @CheckForNull String message) {

        final byte[] messageBytes = (message == null) ? PushCommand.EMPTY_BYTES : message
                .getBytes(MESSAGE_CHARSET);
        final int capacity = messageBytes.length + 5;// type(1byte)
                                                     // +
                                                     // paramSize(2bytes)
                                                     // +
                                                     // pattern(1byte)
                                                     // +
                                                     // count(1byte)
        final ByteBuffer buffer = ByteBuffer.allocate(capacity);

        // 個別部ヘッダ

        // 起動制御情報
        buffer.put(TYPE);
        // 個別部パラメータサイズ
        final int paramSize = messageBytes.length + 2; // pattern(1byte) + count(1byte)
        PushCommand.putShortAsLittleEndian(paramSize, buffer);

        // 個別部パラメータ

        // pattern
        buffer.put(pattern.patternValue());
        // count
        buffer.put((byte) count);
        // URL
        buffer.put(messageBytes);

        return new byte[][] {
            buffer.array()
        };
    }
}
