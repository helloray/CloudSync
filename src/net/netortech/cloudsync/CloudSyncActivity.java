package net.netortech.cloudsync;

import net.netortech.cloudsync.data.SettingsDB;
import net.netortech.cloudsync.ui.advanced.AccountsActivity;
import net.netortech.cloudsync.ui.advanced.TasksActivity;
import net.netortech.cloudsync.ui.wizard.TaskActivity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

import com.netortech.utilities.hardware.Power;
import com.netortech.utilities.hardware.Wifi;

public class CloudSyncActivity extends BaseActivity {
	public static SettingsDB db;
	Intent service;
	public static CloudSyncActivity instance;
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    setContentView(R.layout.main);
	
	    log("Cloud Sync application created.", true);
	    Init();
	}
	@Override
	public void onDestroy()
	{
		instance = null;
		super.onDestroy();
	}
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
	    MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.main_menu, menu);
	    return true;
	}
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    // Handle item selection
	    switch (item.getItemId()) {
	    case R.id.mnuAccounts:
	        startActivity(new Intent(this, AccountsActivity.class));
	        return true;
	    case R.id.mnuTasks:
	        startActivity(new Intent(this, TasksActivity.class));
	        return true;
	    default:
	        return super.onOptionsItemSelected(item);
	    }
	}
	private void Init()
	{
		Wifi.setContext(this.getApplicationContext());
		// Wifi.setToSimulation();
		// Wifi.setIsConnected(true);
		Power.setContext(this.getApplicationContext());
		
	    instance = this;
	    service = new Intent(this, CloudSyncService.class);
	    if(CloudSyncService.isRunning())
	    {
    		TextView t = (TextView) instance.findViewById(R.id.lblServiceStatus);
    		t.setText(R.string.service_running);
	    }
	    Bind();
	}
	private void Bind()
	{
	    final Button btnStartService = (Button) findViewById(R.id.btnStartService);
	    btnStartService.setOnClickListener(new OnClickListener(){ public void onClick(View v) {
	    	startService();
	    }});
	    final Button btnStopService = (Button) findViewById(R.id.btnStopService);
	    btnStopService.setOnClickListener(new OnClickListener(){ public void onClick(View v) {
	    	stopService();
	    }});
	    
	    final Button btnWizard = (Button) findViewById(R.id.btnWizard);
	    btnWizard.setOnClickListener(new OnClickListener(){ public void onClick(View v) {
	    	startActivity(new Intent(CloudSyncActivity.this, TaskActivity.class));
	    }});

	    /*
	    final Button btnAccounts = (Button) findViewById(R.id.btnAccounts);
	    btnAccounts.setOnClickListener(new OnClickListener(){ public void onClick(View v) {
	        startActivity(new Intent(CloudSyncActivity.this, AccountsActivity.class));
	    }});
	    final Button btnTasks = (Button) findViewById(R.id.btnTasks);
	    btnTasks.setOnClickListener(new OnClickListener(){ public void onClick(View v) {
	        startActivity(new Intent(CloudSyncActivity.this, TasksActivity.class));
	    }});
	    */
	}
	private void stopService()
	{
	    if(CloudSyncService.isRunning()) stopService(service); 
	}
	private void startService()
	{
		if(!CloudSyncService.isRunning()) startService(service);
	}
	public static void signalServiceStarted()
	{
		if(instance == null) return;
		instance.runOnUiThread(new Runnable() {
	        public void run() {
	    		TextView t = (TextView) instance.findViewById(R.id.lblServiceStatus);
	    		t.setText(R.string.service_running);
	        }
		});
	}
	public static void signalServiceStopped()
	{
		if(instance == null) return;
		instance.runOnUiThread(new Runnable() {
	        public void run() {
	    		TextView t = (TextView) instance.findViewById(R.id.lblServiceStatus);
	    		t.setText(R.string.service_not_running);
	        }
		});
	}
	public static void signalServiceStopping()
	{
		if(instance == null) return;
		instance.runOnUiThread(new Runnable() {
	        public void run() {
	    		TextView t = (TextView) instance.findViewById(R.id.lblServiceStatus);
	    		t.setText(R.string.service_shutting_down);
	        }
		});
	}
    protected static void log(String msg)
    {
    	log(msg, false);
    }
    protected static void log(String msg, Boolean quiet)
    {
    	if(!quiet) return;
    	Log.v("CloudSyncActivity", msg);
    }
}