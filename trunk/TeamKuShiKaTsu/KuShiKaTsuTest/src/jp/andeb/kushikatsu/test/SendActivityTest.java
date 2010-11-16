package jp.andeb.kushikatsu.test;

import jp.andeb.kushikatsu.SendActivity;
import android.content.Intent;
import android.sax.StartElementListener;
import android.test.ActivityInstrumentationTestCase2;

public class SendActivityTest extends
		ActivityInstrumentationTestCase2<SendActivity> {

	SendActivity sendActivity;

	public SendActivityTest() {
		super("jp.andeb.kushikatsu", SendActivity.class);
	}

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		setActivityInitialTouchMode(false);
		sendActivity = getActivity();
	}

	public void testSample() throws Exception {
		assertNotNull(sendActivity);

		Intent intent = new Intent("com.demo.felica.contacts.RECEIVE");
		intent.putExtra("name", "ほげ");
		intent.putExtra("phone", "000-0000-0000");
		Intent fIntent = new Intent("jp.andeb.kushikatsu.FELICA_SEND");
		fIntent.addCategory(Intent.CATEGORY_DEFAULT);
		fIntent.putExtra("EXTRA_INTENT", intent);
		fIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
		sendActivity.startActivity(fIntent);

	}

}
