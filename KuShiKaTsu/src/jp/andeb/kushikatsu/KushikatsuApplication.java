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

package jp.andeb.kushikatsu;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import android.app.Application;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import com.felicanetworks.mfc.PushSegment;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;

/**
 * 串かつアプリケーショングルーバルな情報を保持できるようにした {@link Application} です。
 */
@DefaultAnnotation(NonNull.class)
public final class KushikatsuApplication extends Application {

    /**
     * NFC タグを検出した際に送信される {@link PushSegment}。
     */
    @CheckForNull
    private PushSegment pushSegment_ = null;

    /**
     * 登録された Push セグメント無効化するためのタイマーです。タイマーがセットされている間だけ
     * {@link Timer} インスタンスがセットされます。
     */
    @CheckForNull
    private Timer timer_ = null;

    /**
     * {@link PushSegment} が登録されてから無効なるまでの秒数のデフォルト値。
     */
    public static final int DEFAULT_PUSH_SEGMENT_VALID_PERIOD_SEC = 30;

    /**
     * {@link PushSegment} を登録します。
     *
     * <p>
     * 登録した {@link PushSegment} は
     * {@link #DEFAULT_PUSH_SEGMENT_VALID_PERIOD_SEC}{@code (}{@link #DEFAULT_PUSH_SEGMENT_VALID_PERIOD_SEC}{@code )}
     * 秒間有効です。
     * </p>
     *
     * @param pushSegment
     * 登録する {@link PushSegment}。 {@code null} を渡した場合は現在保持している
     * {@link PushSegment}をクリアします。
     * @see #setPushSegment(PushSegment, int)
     * @see #getPushSegment()
     */
    public void setPushSegment(@CheckForNull PushSegment pushSegment) {
        setPushSegment(pushSegment, DEFAULT_PUSH_SEGMENT_VALID_PERIOD_SEC);
    }

    /**
     * {@link PushSegment} を登録します。
     *
     * @param pushSegment
     * 登録する {@link PushSegment}。 {@code null} 不可。
     * @param validPeriodSec
     * 登録した {@link PushSegment} の有効期間を秒で指定します。正数のみを指定可能です。
     * @throws IllegalArgumentException
     * 引き数の制約に違反した場合。
     * @see #setPushSegment(PushSegment)
     * @see #getPushSegment()
     */
    public synchronized void setPushSegment(PushSegment pushSegment, int validPeriodSec) {
       if (pushSegment == null) {
           throw new IllegalArgumentException("'pushSegment' must not be null.");
       }
        if (validPeriodSec <= 0L) {
            throw new IllegalArgumentException("validPeriodSec must be positive: " + validPeriodSec);
        }
        cancelTimer();
        pushSegment_ = pushSegment;
        showPushNotification();
        startTimer(validPeriodSec);
    }

    /**
     * 現在有効な {@link PushSegment} を返します。
     *
     * @return
     * 現在有効な {@link PushSegment}。有効なものが登録されていない場合は {@code null} を返します。
     */
    @CheckForNull
    public synchronized PushSegment getPushSegment() {
        final PushSegment pushSegment = pushSegment_;
        return pushSegment;
    }

    /**
     * 登録されている Push セグメントをクリアします。
     */
    public synchronized void clearPushSegment() {
        pushSegment_ = null;
        hidePushNotification();
        cancelTimer();
    }

    /**
     * 登録されている Push メッセージを無効化するためのタイマーを開始します。
     *
     * <p>
     * すでに Push メッセージ無効化タイマーが動作中の場合は例外がスローされます。
     * </p>
     *
     * @param validPeriodSec
     * Push メッセージを無効化するまでの時間(秒)。
     * @throws IllegalStateException
     * すでに Push メッセージ無効化タイマーが動作中の場合。
     */
    private void startTimer(int validPeriodSec) {
        if (timer_ != null) {
            throw new IllegalStateException("");
        }
        timer_ = new Timer("registered push timer", true);
        timer_.schedule(new TimerTask() {
            @Override
            public void run() {
                cancelTimer();
                clearPushSegment();
                hidePushNotification();
            }
        }, TimeUnit.SECONDS.toMillis(validPeriodSec));
    }

    /**
     * Push メッセージ無効化タイマーが開始されている場合は、タイマーをキャンセルします。
     * 開始されていない場合はなにもしません。
     */
    private void cancelTimer() {
        final Timer t = timer_;
        if (t == null) {
            return;
        }
        t.cancel();
        timer_ = null;
    }


    /**
     * notification の識別子
     */
    private static final int PUSH_NOTIFICATION_ID = 1;

    /**
     * 有効な NFC Push が登録されていることを示す Notification を表示します。
     */
    private void showPushNotification() {
        // Notification を表示
        final Context appContext = getApplicationContext();
        final NotificationManager notificationMgr = getNotificationManager();
        final PendingIntent pi = createPendingIntentForNotification(appContext);

        final Notification notify = new Notification();
        notify.icon = R.drawable.icon;
        notify.tickerText = getText(R.string.nfc_notification_ticker);
        notify.flags = Notification.FLAG_ONGOING_EVENT;
        notify.setLatestEventInfo(appContext, getText(R.string.nfc_notification_title),
                getText(R.string.nfc_notification_text), pi);

        notificationMgr.notify(PUSH_NOTIFICATION_ID, notify);
    }

    /**
     * 有効な NFC Push が登録されていることを示す Notification を非表示にします。
     * 表示されていない場合はなにもしません。
     */
    private void hidePushNotification() {
        final NotificationManager notificationMgr = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationMgr.cancel(PUSH_NOTIFICATION_ID);
    }

    /**
     * {@link NotificationManager} を返します。
     * @return
     * {@link NotificationManager}。
     */
    private NotificationManager getNotificationManager() {
        final NotificationManager nm;
        nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        assert nm != null;
        return nm;
    }

    /**
     * ユーザにより Notification アイテムが選択された際に飛ばす {@link PendingIntent} を構築します。
     * @param appContext
     * アプリケーションコンテキスト。{@code null} 禁止。
     * @return
     * 構築された {@link PendingIntent}。
     */
    private PendingIntent createPendingIntentForNotification(final Context appContext) {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_DEFAULT);
        intent.setClass(this, PrefActivity.class);
        final PendingIntent pi;
        pi = PendingIntent.getActivity(appContext, -1/* not used */, intent,
                Intent.FLAG_ACTIVITY_NEW_TASK);
        return pi;
    }

}
