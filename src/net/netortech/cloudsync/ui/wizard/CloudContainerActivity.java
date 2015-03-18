package net.netortech.cloudsync.ui.wizard;

import java.util.LinkedList;
import java.util.List;

import net.netortech.cloudsync.R;
import net.netortech.cloudsync.clouds.Cloud;
import net.netortech.cloudsync.clouds.ICloud;
import net.netortech.cloudsync.data.TaskData;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.netortech.utilities.web.JSONObject;

public class CloudContainerActivity extends WizardStepActivity {
	ListView lstCloudContainers;
	Button btnRefresh;
	
	ProgressDialog dialog;
	List<String> containers;
	String currentContainer = "";
	
	@Override protected void btnBack_Click() { setResult(RESULT_CANCELED, null); finish(); }
	@Override protected String getActivityName() { return "TaskActivity"; }
	@Override protected int getLayoutId() { return R.layout.wizard_cloud_containers; }
	@Override protected String getStepInstructions() { return "Which container would you like to sync from"; }
	
	@Override
	public void onWizardStepCreated(Bundle b)
	{
		initStep();
	}
	@Override protected String getStepTitle() 
	{ 
		if(task.ID == -1)
			return "Creating New Job!";
		else
			return "Editing Job '" + task.Name + "'";
	}
	@Override
	protected void confirmItem(Object item, int code)
	{
		switch(code)
		{
			case WizardStepActivity.CONFIRM_GO_BACK_CODE:
				setResult(WizardStepActivity.WIZARD_ACCOUNT_FAILED_RESULT_CODE, null); 
				break;
			case WizardStepActivity.CONFIRM_CONTINUE_CODE:
				beginGetContainers();
				break;
		}
	}
	private void containerSelected(String container)
	{
		Cloud.getCloud(engine).setCloudContainer(container, task_data);
		startNextStep();
	}
    protected void getContainers()
    {
    	account.Failures = 0;
    	ICloud cloud = Cloud.getCloud(account, account_data, getDBContext(), true);
    	if(account.Failures == 0) 
    	{
    		containers = cloud.getContainers(new LinkedList<TaskData>(task_data));
    		runOnUiThread(new Runnable(){ public void run() { bindContainers(); } });
    	}
    }
    protected void beginGetContainers()
    {
	     Thread thread =  new Thread(null, new Runnable(){ public void run() { getContainers(); finishGetContainers(); } }, "MagentoBackground");
         thread.start();
         dialog = ProgressDialog.show(this, this.getString(R.string.please_wait), this.getString(R.string.retrieving_data), true);
    }
    protected void finishGetContainers()
    {
    	this.runOnUiThread(new Runnable() {
	        public void run() {
	        	dialog.dismiss();
	        	if(account.Failures > 0)
	        		showConfirm(CloudContainerActivity.this.getString(R.string.account_login_failed_retry), null, 
	        				WizardStepActivity.CONFIRM_CONTINUE_CODE, WizardStepActivity.CONFIRM_GO_BACK_CODE);
	        }
    	});
    }
    private void startNextStep()
	{
		Intent intent;
		JSONObject obj;
		
    	boolean hasFolders = Cloud.getCloud(engine).hasFolders();
    	
		intent = new Intent(this, hasFolders ? CloudFolderActivity.class : SelectLocalFolderActivity.class);
		
		putExtras(intent);
		
		startActivityForResult(intent, ANONYMOUS_REQUEST_CODE);
	}
    private void initStep()
    {
    	currentContainer = Cloud.getCloud(engine).getCloudContainer(task_data);
    	bind();
    	log("Current Container Name: " + currentContainer);
    	
    	beginGetContainers();
    }
    private void bindContainers()
    {
    	ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, R.layout.wizard_edit_item, containers) {
	        @Override
	        public View getView(int position, View convertView, ViewGroup parent) {
                LayoutInflater vi = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);

                final String item = getItem(position);
                final View v = vi.inflate(R.layout.wizard_edit_item, null);
                final Button btnSelect = (Button)v.findViewById(R.id.btnSelect);
                final Button btnDelete = (Button)v.findViewById(R.id.btnDelete);
                final TextView t = (TextView) v.findViewById(R.id.lblItemTitle);
                final TextView d = (TextView) v.findViewById(R.id.lblItemDescription);
                
            	btnDelete.setVisibility(View.GONE);
            	d.setVisibility(View.GONE);
            	btnSelect.setText(R.string.select);

            	OnClickListener selectListener = new OnClickListener(){ public void onClick(View v) { containerSelected(item); }};
                v.setOnClickListener(selectListener);
                btnSelect.setOnClickListener(selectListener);

                if(currentContainer.equals(item)) v.setBackgroundColor(Color.parseColor(CloudContainerActivity.this.getString(R.color.selected)));
                
                t.setText(item);  
                
                return v;
	        }
		};
		lstCloudContainers.setAdapter(adapter);
    }
    private void bind()
    {
    	lstCloudContainers = (ListView)this.findViewById(R.id.lstCloudContainers);
    }
}
