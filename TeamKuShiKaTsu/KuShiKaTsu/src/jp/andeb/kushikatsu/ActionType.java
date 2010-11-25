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
        private static final String INTENT = "EXTRA_INTENT";

        @CheckForNull
        public PushSegment extract(Intent initiatorIntent) {
            final Parcelable internalIntent;
            internalIntent = initiatorIntent.getParcelableExtra(INTENT);
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
        private static final String URL = "EXTRA_URL";
        private static final String BROWSER_PARAM = "EXTRA_BROWSER_PARAM";

        public PushSegment extract(Intent initiatorIntent) {
            final String url = initiatorIntent.getStringExtra(URL);
            final String browserParam = initiatorIntent
                    .getStringExtra(BROWSER_PARAM);
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
        private static final String EMAIL = "EXTRA_EMAIL";
        private static final String CC = "EXTRA_CC";
        private static final String SUBJECT = "EXTRA_SUBJECT";
        private static final String TEXT = "EXTRA_TEXT";
        private static final String MAIL_PARAM = "EXTRA_MAIL_PARAM";

        public PushSegment extract(Intent initiatorIntent) {
            final String[] to = initiatorIntent.getStringArrayExtra(EMAIL);
            final String[] cc = initiatorIntent.getStringArrayExtra(CC);
            final String subject = initiatorIntent.getStringExtra(SUBJECT);
            final String body = initiatorIntent.getStringExtra(TEXT);
            final String param = initiatorIntent.getStringExtra(MAIL_PARAM);

            final PushStartMailerSegment segment;
            segment = new PushStartMailerSegment(to, cc, subject, body, param);
            return segment;
        }
    }), // メーラ起動用
    ;

    private static interface Extractor {
        @CheckForNull
        public PushSegment extract(Intent initiatorIntent);
    }

    // ACTION 文字列
    private final String action_;
    private final Extractor segmentExtractor_;

    private ActionType(Extractor segmentExtractor) {
        action_ = SendActivity.class.getPackage() + "." + name();
        segmentExtractor_ = segmentExtractor;
    }

    /**
     * 対応する {@code ACTION} 文字列を返します。
     * @return
     * {@code ACTION} 文字列。
     */
    public String getActionString() {
        return action_;
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
