package net.netortech.cloudsync.ui.wizard;

import net.netortech.cloudsync.R;
import net.netortech.cloudsync.clouds.Cloud;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;

import com.netortech.utilities.web.JSONObject;

public class AccountNameActivity extends WizardStepActivity {
	EditText txtTextbox;
	Button btnNext; 
	
	@Override protected void btnBack_Click() { finish(); }
	@Override protected String getActivityName() { return "AccountNameActivity"; }
	@Override protected int getLayoutId() { return R.layout.wizard_textbox; }
	@Override protected String getStepInstructions() { return getText(R.string.what_name_for_account).toString(); }
	@Override protected String getStepTitle() 
	{ 
		if(task.ID == -1)
			return "Creating New Job!";
		else
			return "Editing Job '" + task.Name + "'";
	}
	
	@Override
	public void onWizardStepCreated(Bundle b)
	{
		initStep();
	}
	
	private void startNextStep()
	{
		Intent intent;
		JSONObject obj;
		
    	boolean hasContainers = Cloud.getCloud(engine).hasContainers();
    	boolean hasFolders = Cloud.getCloud(engine).hasFolders();
    	
		intent = new Intent(this, hasContainers && task.ID == -1 ? CloudContainerActivity.class : hasContainers || hasFolders ? SelectCloudResourceActivity.class : SelectLocalFolderActivity.class);
		
		putExtras(intent);
		
		startActivityForResult(intent, ANONYMOUS_REQUEST_CODE);
	}
	
	private void initStep()
	{
		bind();
	}
	private void bind()
	{
		txtTextbox = (EditText)this.findViewById(R.id.txtTextbox);
		btnNext = (Button)this.findViewById(R.id.btnNext);
		
		txtTextbox.addTextChangedListener(new TextWatcher(){
			@Override
			public void afterTextChanged(Editable s) {
				account.Name = s.toString();
			}

			@Override
			public void beforeTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {}
			@Override
			public void onTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {}
        	
        });
		btnNext.setOnClickListener(new OnClickListener() { public void onClick(View v) {
			if(!account.Name.trim().equals(""))
			{
				startNextStep();
				return;
			}
			alert(getString(R.string.task_name_required));
		}});
		
		txtTextbox.setText(account.Name);
	}
}