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
 *
 * $Id$
 */
package jp.andeb.kushikatsu.util;

import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.util.Log;

/**
 * {@link MediaPlayer} を使用する際のユーティリティクラスです.
 */
public final class MediaPlayerUtil {

    /**
     * 再生完了時に、自動的に {@link MediaPlayer} を解放する {@link OnCompletionListener}
     * です。
     */
    public static OnCompletionListener RELEASE_PLAYER_LISTENER;
    static {
        RELEASE_PLAYER_LISTENER = new OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                mp.release();
                Log.d("MediaPlayerUtil.OnCompletionListener",
                        "released media player");
            }
        };
    }

    private MediaPlayerUtil() {
        throw new AssertionError("instantiation prohibited.");
    }
}
