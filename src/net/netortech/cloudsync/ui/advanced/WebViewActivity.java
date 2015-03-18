package net.netortech.cloudsync.ui.advanced;

import net.netortech.cloudsync.R;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.webkit.WebView;

public class WebViewActivity extends Activity {
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
        setContentView(R.layout.webview);

		Intent i = this.getIntent();
		if(!i.hasExtra("Url"))
		{
			log("Url extra not found.");
			return;
		}
		Uri uri = Uri.parse(i.getStringExtra("Url"));
		log("Url extra: " + uri.toString());
		WebView wv = (WebView)this.findViewById(R.id.wvMain);
		wv.loadUrl(uri.toString());
	}
	protected static void log(String message)
	{
		log(message, false);
	}
	protected static void log(Exception ex)
	{
		log(ex, false);
	}
	protected static void log(Exception ex, Boolean quiet)
	{
		String trace = "";
		for(StackTraceElement e : ex.getStackTrace()) trace += e.toString() + " ";
		log(ex.toString() + " trace: " + trace, true);
	}
	protected static void log(String message, Boolean quiet)
	{
		Log.v("TestActivity", message);
	}
}
