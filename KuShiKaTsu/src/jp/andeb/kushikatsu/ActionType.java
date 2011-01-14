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
package jp.andeb.kushikatsu;

import static jp.andeb.kushikatsu.helper.KushikatsuHelper.SendIntent.EXTRA_INTENT;
import static jp.andeb.kushikatsu.helper.KushikatsuHelper.StartBrowser.EXTRA_BROWSER_PARAM;
import static jp.andeb.kushikatsu.helper.KushikatsuHelper.StartBrowser.EXTRA_URL;
import static jp.andeb.kushikatsu.helper.KushikatsuHelper.StartMailer.EXTRA_CC;
import static jp.andeb.kushikatsu.helper.KushikatsuHelper.StartMailer.EXTRA_EMAIL;
import static jp.andeb.kushikatsu.helper.KushikatsuHelper.StartMailer.EXTRA_MAIL_PARAM;
import static jp.andeb.kushikatsu.helper.KushikatsuHelper.StartMailer.EXTRA_SUBJECT;
import static jp.andeb.kushikatsu.helper.KushikatsuHelper.StartMailer.EXTRA_TEXT;
import jp.andeb.kushikatsu.helper.KushikatsuHelper.SendIntent;
import jp.andeb.kushikatsu.helper.KushikatsuHelper.StartBrowser;
import jp.andeb.kushikatsu.helper.KushikatsuHelper.StartMailer;
import android.content.Intent;
import android.os.Parcelable;

import com.felicanetworks.mfc.PushIntentSegment;
import com.felicanetworks.mfc.PushSegment;
import com.felicanetworks.mfc.PushStartBrowserSegment;
import com.felicanetworks.mfc.PushStartMailerSegment;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;

/**
 * Push するメッセージの種類を表す {@code enum} です。
 */
@DefaultAnnotation(NonNull.class)
enum ActionType {
    // インテント送信
    FELICA_INTENT(new Extractor() {

        @Override
        public String getActionString() {
            return SendIntent.ACTION;
        }

        @Override
        @CheckForNull
        public PushSegment extract(Intent intent) {
            final Parcelable internalIntent;
            internalIntent = intent.getParcelableExtra(EXTRA_INTENT);
            if (!(internalIntent instanceof Intent)) {
                return null;
            }
            final PushIntentSegment segment;
            segment = new PushIntentSegment((Intent) internalIntent);
            return segment;
        }
    }), //
    // ブラウザ起動
    FELICA_BROWSER(new Extractor() {

        @Override
        public String getActionString() {
            return StartBrowser.ACTION;
        }

        public PushSegment extract(Intent intent) {
            if (Intent.ACTION_SEND.equals(intent.getAction())) {
                final CharSequence uri = intent.getCharSequenceExtra(Intent.EXTRA_TEXT);
                if (uri == null) {
                    return null;
                }
                final PushStartBrowserSegment segment;
                segment = new PushStartBrowserSegment(uri.toString(), null);
                return segment;
            }
            
            final String url = intent.getStringExtra(EXTRA_URL);
            final String browserParam = intent
                    .getStringExtra(EXTRA_BROWSER_PARAM);
            if (url == null || url.length() == 0) {
                return null;
            }

            final PushStartBrowserSegment segment;
            segment = new PushStartBrowserSegment(url, browserParam);
            return segment;
        }
    }), //
    // メーラ起動
    FELICA_MAILER(new Extractor() {

        @Override
        public String getActionString() {
            return StartMailer.ACTION;
        }

        public PushSegment extract(Intent intent) {
            final String[] to = intent.getStringArrayExtra(EXTRA_EMAIL);
            final String[] cc = intent.getStringArrayExtra(EXTRA_CC);
            final String subject = intent.getStringExtra(EXTRA_SUBJECT);
            final String body = intent.getStringExtra(EXTRA_TEXT);
            final String param = intent.getStringExtra(EXTRA_MAIL_PARAM);

            final PushStartMailerSegment segment;
            segment = new PushStartMailerSegment(to, cc, subject, body, param);
            return segment;
        }
    }), // メーラ起動用
    ;

    private static interface Extractor {
        public String getActionString();

        @CheckForNull
        public PushSegment extract(Intent initiatorIntent);
    }

    private final Extractor segmentExtractor_;

    private ActionType(Extractor segmentExtractor) {
        segmentExtractor_ = segmentExtractor;
    }

    /**
     * 対応する {@code ACTION} 文字列を返します。
     * @return
     * {@code ACTION} 文字列。
     */
    public String getActionString() {
        return segmentExtractor_.getActionString();
    }

    @CheckForNull
    public PushSegment extractSegment(Intent initiatorIntent) {
        if (initiatorIntent == null) {
            throw new IllegalArgumentException(
                    "'initiatorIntent' must not be null");
        }
        final PushSegment message = segmentExtractor_.extract(initiatorIntent);
        return message;
    }

    /**
     * 指定された {@code ACTION} 文字列に対応する {@link ActionType} を返します。
     * @param actionString
     * {@code ACTION} 文字列。 {@code null} 禁止。
     * @return
     * 指定された文字列に対応する {@link ActionType} オブジェクト。
     * 対応する {@code ActionType} が存在しない場合は {@code null}。
     */
    @CheckForNull
    public static ActionType of(String actionString) {
        if (Intent.ACTION_SEND.equals(actionString)) {
            // SEND は相手端末ではブラウザを開くようにする
            return FELICA_BROWSER;
        }

        if (FELICA_INTENT.getActionString().equals(actionString)) {
            return FELICA_INTENT;
        }
        if (FELICA_BROWSER.getActionString().equals(actionString)) {
            return FELICA_BROWSER;
        }
        if (FELICA_MAILER.getActionString().equals(actionString)) {
            return FELICA_MAILER;
        }
        return null;
    }
}
