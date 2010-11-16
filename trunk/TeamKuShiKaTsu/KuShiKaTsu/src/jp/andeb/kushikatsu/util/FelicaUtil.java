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

import com.felicanetworks.mfc.FelicaException;

public final class FelicaUtil {

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
