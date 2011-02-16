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

import com.felicanetworks.mfc.FelicaException;
import com.felicanetworks.mfc.PushSegment;

/**
 * Push 送信処理のためのインテフェースです。
 *
 * @author zaki
 */
public interface PushSender {

    /**
     * Push送信デバイスの占有を開始します。
     * @return
     * 成功した場合は {@code true}、失敗あいた場合は {@code false}。
     */
    public boolean connect();

    /**
     * Push送信デバイスの占有を終了します。占有していない場合は何もしません。
     */
    public void disconnect();

    /**
     * Push送信デバイスの使用を開始します。
     * @throws FelicaException
     * 使用開始に失敗した場合。
     */
    public void open() throws FelicaException;

    /**
     * Push送信を行います。
     * @param segment
     * 送信する Push メッセージ情報。{@code null} 禁止。
     * @throws FelicaException
     * 送信に失敗した場合。
     */
    public void push(PushSegment segment) throws FelicaException;

    /**
     * 送信をキャンセルします。
     */
    public void cancel();

    /**
     * 送信がキャンセルされたかどうかを返します。
     * @return
     * 送信がキャンセルされていれば {@code true}、されてなければ {@code false} を返します。
     */
    public boolean isCanceled();
}
