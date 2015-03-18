package net.netortech.cloudsync.ui.wizard;

import net.netortech.cloudsync.R;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;

public class TaskNameActivity extends WizardStepActivity {
	EditText txtTextbox;
	Button btnNext; 
	
	@Override protected void btnBack_Click() { finish(); }
	@Override protected String getActivityName() { return "TaskNameActivity"; }
	@Override protected int getLayoutId() { return R.layout.wizard_textbox; }
	@Override protected String getStepInstructions() { return getText(R.string.what_name_for_task).toString(); }
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
		Intent intent = new Intent(this, AccountActivity.class);
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
				task.Name = s.toString();
			}

			@Override
			public void beforeTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {}
			@Override
			public void onTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {}
        	
        });
		btnNext.setOnClickListener(new OnClickListener() { public void onClick(View v) {
			if(!task.Name.trim().equals(""))
			{
				startNextStep();
				return;
			}
			alert(getString(R.string.task_name_required));
		}});
		
		txtTextbox.setText(task.Name);
	}
}
