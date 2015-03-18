package net.netortech.cloudsync.ui.wizard;

import net.netortech.cloudsync.R;
import net.netortech.cloudsync.data.SettingsDB;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class FinishedActivity extends WizardStepActivity {
	Button btnFinishAndSave;
	Button btnAdvancedOptions;
	
	
	@Override protected void btnBack_Click() { finish(); }
	@Override protected String getActivityName() { return "FinishedActivity"; }
	@Override protected int getLayoutId() { return R.layout.wizard_finished; }
	@Override protected String getStepInstructions() { return this.getString(R.string.finished_wizard); }
	@Override protected String getStepTitle() { return "Finished! Congratulations!"; }
	
	@Override
	public void onWizardStepCreated(Bundle b)
	{
		initStep();
	}

	private void finishAndSave()
	{
		if(account != null) 
			SettingsDB.SaveAccountInfo(account, account_data, getDBContext());
		else
			log("Account is null.");
		if(task != null) 
		{
			if(account != null) task.AccountID = account.ID;
			SettingsDB.SaveTaskInfo(task, task_data, getDBContext());
		}
		else
			log("Task is null.");
		setResult(WizardStepActivity.WIZARD_FINISH_RESULT_CODE, null); 
		finish();
	}
	
	private void startAdvancedStep()
	{
		Intent intent = new Intent(this, AdvancedActivity.class);
		putExtras(intent);
		startActivityForResult(intent, ANONYMOUS_REQUEST_CODE);
	}
	
	private void initStep()
	{
		bind();
		log("Extra has 'FinishCode': " + getIntent().hasExtra("FinishCode"));
		if(getIntent().getIntExtra("FinishCode", FINISH_ANONYMOUS) == FINISH_NO_ADVANCED) btnAdvancedOptions.setVisibility(View.GONE);
	}
	
	private void bind()
	{
		btnFinishAndSave = (Button)this.findViewById(R.id.btnFinishAndSave);
		btnFinishAndSave.setOnClickListener(new OnClickListener() { public void onClick(View v) { finishAndSave(); }});
		btnAdvancedOptions = (Button)this.findViewById(R.id.btnAdvancedOptions);
		btnAdvancedOptions.setOnClickListener(new OnClickListener() { public void onClick(View v) { startAdvancedStep(); }});
	}
}
