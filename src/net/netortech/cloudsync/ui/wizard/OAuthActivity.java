package net.netortech.cloudsync.ui.wizard;

import java.util.Map;

import net.netortech.cloudsync.R;
import net.netortech.cloudsync.clouds.Cloud;
import net.netortech.cloudsync.clouds.ICloud;
import net.netortech.cloudsync.data.SettingsDB;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.netortech.utilities.clouds.CloudException;
import com.netortech.utilities.web.Url;

public class OAuthActivity extends WizardStepActivity {
	private static final String ALTERNATE_CALLBACK1 = "http://android.netortech.com/resources/OAuthCallback.aspx";
	private static final String ALTERNATE_CALLBACK2 = "cloudsync://oauthresponse";
	WebView wvMain;
	
	ICloud cloud;
	ProgressDialog dialog;
	boolean authenticationSuccess = false;
	boolean testingAccess = false;
	String authenticationError = ""; 
	String uid = null;
	String tokenKey = null;
	
	@Override protected void btnBack_Click() { finish(); }
	@Override protected void btnCancel_Click() { finish(); }
	@Override protected String getActivityName() { return "OAuthActivity"; }
	@Override protected int getLayoutId() { return R.layout.wizard_oauth; }
	
	@Override
	public void onWizardStepCreated(Bundle b)
	{
		initStep();
	}
	@Override protected String getStepTitle() 
	{ 
		if(task != null && task.ID == -1)
			return "Creating New Job!";
		else if(task != null)
			return "Editing Job '" + task.Name + "'";
		else 
			return "Editing Account '" + account.Name + "'";
	}
	@Override 
	protected String getStepInstructions() 
	{ 
		return "What credentials do you use to connect to " + engine.Name;
	}

	private void testAccess(String callback)
	{
		authenticationSuccess = false;
		authenticationError = null;
		try
		{
			cloud.setOAuthAuthorizationData(account_data, callback);
			authenticationSuccess = true;
		} 
		catch(CloudException ex)
		{
			authenticationError = ex.getMessage();
		}
	}
	private void beginTestAccess(String callback)
	{
		final String final_callback = callback;
		testingAccess = true;
		Thread thread =  new Thread(null, new Runnable(){ public void run() { testAccess(final_callback); finishTestAccess(); } }, "MagentoBackground");
		thread.start();
		dialog = ProgressDialog.show(this, this.getString(R.string.please_wait), this.getString(R.string.authenticating), true);
	}
	private void finishTestAccess()
	{
		if(authenticationSuccess) account.Failures = 0;
		this.runOnUiThread(new Runnable() {
	        public void run() {
	        	dialog.dismiss();
	        	if(!authenticationSuccess && authenticationError != null)
	        		showOkAlert(String.format(OAuthActivity.this.getString(R.string.account_login_failed), authenticationError));
	        	else if(!authenticationSuccess)
	        		showOkAlert(String.format(OAuthActivity.this.getString(R.string.account_login_failed), "Unknown"));
	        	else
	        		showConfirmCredentials(OAuthActivity.this.getString(R.string.account_login_success) + " " + 
	        				OAuthActivity.this.getString(R.string.continue_question), WizardStepActivity.CONFIRM_NEXT_STEP_CODE);
	        }
    	});
	}
	@Override
	protected void confirmItem(Object item, int code)
	{
		switch(code)
		{
			case WizardStepActivity.CONFIRM_NEXT_STEP_CODE:
				if(task == null)
					finishWizard(FINISH_NO_ADVANCED);
				else if(task.ID == -1)
					startNextStep();
				else
					finishWizard();
		}
	}
	private void startNextStep()
	{
		Intent intent = new Intent(this, AccountNameActivity.class);
		putExtras(intent);
		startActivityForResult(intent, ANONYMOUS_REQUEST_CODE);
	}
	private void initStep()
	{
		// Causes some errors.
    	//this.getApplicationContext().deleteDatabase("webview.db");
    	//this.getApplicationContext().deleteDatabase("webviewCache.db");
		
		bind();
		cloud = Cloud.getCloud(SettingsDB.getEngineInfo(account.EngineID, getDBContext()));
		account_data = SettingsDB.getAllAccountData(account.ID, getDBContext());
		cloud.initializeAccountDataFields(account_data);
		
		wvMain.loadUrl(cloud.getOAuthAuthorization(cloud.getOAuthCallback()).toString());
	}
	private void bind()
	{
		wvMain = (WebView)this.findViewById(R.id.wvMain);

		wvMain.clearCache(true);
		CookieSyncManager.createInstance(this); 
	    CookieManager cookieManager = CookieManager.getInstance();
	    cookieManager.removeAllCookie();
		wvMain.getSettings().setSaveFormData(false);
		wvMain.getSettings().setSavePassword(false);
		
		wvMain.getSettings().setJavaScriptEnabled(true);
		wvMain.setWebViewClient(new WebViewClient() {  
		    @Override  
		    public boolean shouldOverrideUrlLoading(WebView view, String url)  
		    {  
		    	if(!url.startsWith(cloud.getOAuthCallback().toString()) && !url.startsWith(ALTERNATE_CALLBACK1) && !url.startsWith(ALTERNATE_CALLBACK2))
		    	{
		    		view.loadUrl(url); 
		    		return true;
		    	}

		    	beginTestAccess(url);
		    	
		        return true;
		    }
		    @Override  
		    public void onPageFinished(WebView view, String url)  
		    {
		    	if(testingAccess) return;
		    	if(url.startsWith(cloud.getOAuthCallback().toString()) || url.startsWith(ALTERNATE_CALLBACK1) || url.startsWith(ALTERNATE_CALLBACK2))
		    	{
			    	beginTestAccess(url);
		    	}
		    }
		});
	}
    protected void showConfirmCredentials(CharSequence message, int code)
    {
    	final int finalCode = code;
    	AlertDialog.Builder builder = new AlertDialog.Builder(this);
    	builder.setMessage(message)
    	       .setCancelable(false)
    	       .setPositiveButton(OAuthActivity.this.getString(R.string.yes), new DialogInterface.OnClickListener() {
    	           public void onClick(DialogInterface dialog, int id) {
    	        	   OAuthActivity.this.confirmItem(null, finalCode);
    	           }
    	       })
    	       .setNegativeButton(OAuthActivity.this.getString(R.string.no), new DialogInterface.OnClickListener() { 
    	    	   public void onClick(DialogInterface dialog, int id) { 
    	    		   testingAccess = false;
    	    	   } 
	    	   });
    	AlertDialog alert = builder.create();   
    	alert.show();
    }

}
