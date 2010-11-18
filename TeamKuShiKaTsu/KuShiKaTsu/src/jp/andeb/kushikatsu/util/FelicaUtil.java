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

import static com.felicanetworks.mfc.FelicaException.ID_ILLEGAL_STATE_ERROR;
import static com.felicanetworks.mfc.FelicaException.ID_UNKNOWN_ERROR;
import static com.felicanetworks.mfc.FelicaException.TYPE_ALREADY_ACTIVATED;
import static com.felicanetworks.mfc.FelicaException.TYPE_CURRENTLY_ACTIVATING;
import static com.felicanetworks.mfc.FelicaException.TYPE_REMOTE_ACCESS_FAILED;

import com.felicanetworks.mfc.FelicaException;

public final class FelicaUtil {

    /**
     * 渡された {@link FelicaException} が、 MFC プロセスとの通信に失敗したことを意味して
     * いるかどかを返します。
     *
     * <p>
     * 主に以下の場合にスローされます。
     * </p>
     * <ul>
     *  <li>FeliCa デバイスを搭載していない端末で実行した場合</li>
     * </ul>
     *
     * @param e
     * チェック対象の例外。
     * @return
     * MFC プロセスとの通信失敗を意味している場合は {@code true}、そうでない場合は
     * {@code false} を返します。
     */
    public static boolean isMissingMfc(FelicaException e) {
        if (e == null) {
            return false;
        }
        final boolean result = e.getID() == ID_UNKNOWN_ERROR
                && e.getType() == TYPE_REMOTE_ACCESS_FAILED;
        return result;
    }

    /**
     * 渡された {@link FelicaException} が、 既にアクティベート済みのためアクティベート要求に
     * 失敗したことを意味しているかどかを返します。
     *
     * @param e
     * チェック対象の例外。
     * @return
     * アクティベート済みによる失敗を意味している場合は {@code true}、そうでない場合は
     * {@code false} を返します。
     */
    public static boolean isAlreadyActivated(FelicaException e) {
        if (e == null) {
            return false;
        }
        final boolean result = e.getID() == ID_ILLEGAL_STATE_ERROR
                && e.getType() == TYPE_ALREADY_ACTIVATED;
        return result;
    }

    /**
     * 渡された {@link FelicaException} が、 現在アクティベート中のためアクティベート要求に
     * 失敗したことを意味しているかどかを返します。
     *
     * @param e
     * チェック対象の例外。
     * @return
     * 現在アクティベート中のための失敗を意味している場合は {@code true}、そうでない場合は
     * {@code false} を返します。
     */
    public static boolean isCurrentlyActivating(FelicaException e) {
        if (e == null) {
            return false;
        }
        final boolean result = e.getID() == ID_ILLEGAL_STATE_ERROR
                && e.getType() == TYPE_CURRENTLY_ACTIVATING;
        return result;
    }

    public static String toString(FelicaException e) {
        if (e == null) {
            return "";
        }

        final StringBuilder sb = new StringBuilder();
        sb.append(e.getClass().getName());
        sb.append("[id=").append(e.getID());
        sb.append(",type=").append(e.getType());
        sb.append(",statusFlag1=" + e.getStatusFlag1());
        sb.append(",statusFlag2=").append(e.getStatusFlag2());
        sb.append("]");
        return sb.toString();
    }

    private FelicaUtil() {
        throw new AssertionError("instantiation prohibited.");
    }
}
