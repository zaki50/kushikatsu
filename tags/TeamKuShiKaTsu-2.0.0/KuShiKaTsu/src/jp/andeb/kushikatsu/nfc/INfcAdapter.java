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

import jp.andeb.kushikatsu.nfc.utils.DelegateFactory.DeclaredIn;

public interface INfcAdapter {

    public static final String EXTRA_TAG = "android.nfc.extra.TAG";

    // public RawTagConnection createRawTagConnection(Tag tag);
    public Object createRawTagConnection(@DeclaredIn("android.nfc.Tag") Object tag);
}
