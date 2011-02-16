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

import com.felicanetworks.mfc.PushStartMailerSegment;

import edu.umd.cs.findbugs.annotations.CheckForNull;

import net.kazzz.felica.FeliCaException;
import net.kazzz.felica.lib.FeliCaLib.IDm;

/**
 * メーラ起動を Push 送信するコマンドのためのクラスです。
 */
public final class PushStartMailerCommand extends PushCommand {

    /**
     * 指定された情報に従ってアプリ起動用の {@link PushStartMailerCommand} を構築します。
     *
     * @param idm
     * 対象の IDm。{@code null} 禁止。
     * @param segment
     * 起動するメール情報を保持した {@link PushStartMailerCommand} オブジェクト。{@code null} 禁止。
     * @throws FeliCaException
     * 指定された情報から {@link PushStartMailerCommand} が構築できない場合。
     */
    public PushStartMailerCommand(IDm idm, PushStartMailerSegment segment) throws FeliCaException {
        this(idm, segment.getToAddress(), segment.getCcAddress(), segment.getSubject(), segment
                .getBody(), segment.getMailerStartupParam());
    }

    /**
     * 指定された情報に従ってアプリ起動用の {@link PushStartMailerCommand} を構築します。
     *
     * @param idm
     * 対象の IDm。{@code null} 禁止。
     * @param toAddresses
     * To に使用するアドレスの配列。{@code null} 要素は無視します。{@code null} は空配列として扱います。
     * @param ccAddresses
     * Cc に使用するアドレスの配列。{@code null} 要素は無視します。{@code null} は空配列として扱います。
     * @param subject
     * Subjet 文字列。 {@code null} は空文字として扱います。
     * @param body
     * Body 文字列。 {@code null} は空文字として扱います。
     * @param mailerStartupParam
     * メーラの起動パラメータ配列。 {@code null} は空配列として扱います。
     * @throws FeliCaException
     * 指定された情報から {@link PushStartMailerCommand} が構築できない場合。
     */
    public PushStartMailerCommand(IDm idm, @CheckForNull String[] toAddresses,
            @CheckForNull String[] ccAddresses, @CheckForNull String subject,
            @CheckForNull String body, @CheckForNull String mailerStartupParam)
            throws FeliCaException {
        super(idm, buildPushStartMailerSegment(toAddresses, ccAddresses, subject, body,
                mailerStartupParam));
    }

    private static final byte TYPE = (byte) 3;

    private static final Charset MAIL_ADDRESS_CHARSET = Charset.forName("iso8859-1");

    private static final Charset MAIL_SUBJECT_CHARSET = Charset.forName("Shift_JIS");

    private static final Charset MAIL_BODY_CHARSET = Charset.forName("Shift_JIS");

    private static final Charset MAIL_STARTUP_PARAM_CHARSET = Charset.forName("Shift_JIS");

    private static byte[][] buildPushStartMailerSegment(@CheckForNull String[] toAddresses,
            @CheckForNull String[] ccAddresses, @CheckForNull String subject,
            @CheckForNull String body, @CheckForNull String mailerStartupParam) {

        final byte[] toAddrsBytes = getJoinedBytes(toAddresses, ",", MAIL_ADDRESS_CHARSET);
        final byte[] ccAddrsBytes = getJoinedBytes(ccAddresses, ",", MAIL_ADDRESS_CHARSET);
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
        final ByteBuffer buffer = ByteBuffer.allocate(capacity);

        // 個別部ヘッダ

        // 起動制御情報
        buffer.put(TYPE);
        // 個別部パラメータサイズ
        final int paramSize = capacity - 3/*type(1byte) + paramSize(2bytes)*/;
        PushCommand.putShortAsLittleEndian(paramSize, buffer);

        // 個別部パラメータ

        // Toサイズ
        PushCommand.putShortAsLittleEndian(toAddrsBytes.length, buffer);
        // To
        buffer.put(toAddrsBytes);
        // Ccサイズ
        PushCommand.putShortAsLittleEndian(ccAddrsBytes.length, buffer);
        // Cc
        buffer.put(ccAddrsBytes);
        // Subject サイズ
        PushCommand.putShortAsLittleEndian(subjectBytes.length, buffer);
        // Subject
        buffer.put(subjectBytes);
        // Body サイズ
        PushCommand.putShortAsLittleEndian(bodyBytes.length, buffer);
        // Body
        buffer.put(bodyBytes);
        // メーラスタートアップパラメータ
        buffer.put(mailerStartupParamByte);

        return new byte[][] { buffer.array() };
    }
}
