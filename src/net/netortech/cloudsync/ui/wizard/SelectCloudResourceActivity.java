package net.netortech.cloudsync.ui.wizard;

import java.util.LinkedList;
import java.util.List;

import net.netortech.cloudsync.R;
import net.netortech.cloudsync.clouds.Cloud;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class SelectCloudResourceActivity extends WizardStepActivity {
	Button btn1, btn2, btn3, btn4;
	List<Button> buttons = new LinkedList<Button>();
	
	private static final int ACTION_SPECIFY_LOCATION = 1;
	private static final int ACTION_SELECT_ROOT = 2;
	private static final int ACTION_LEAVE = 3;
	
	@Override protected void btnBack_Click() { finish(); }
	@Override protected String getActivityName() { return "SelectCloudFolderActivity"; }
	@Override protected int getLayoutId() { return R.layout.wizard_multi_button; }
	
	@Override
	public void onWizardStepCreated(Bundle b)
	{
		init();
	}
	@Override protected String getStepTitle() 
	{ 
		if(task.ID == -1)
			return "Creating New Job!";
		else
			return "Editing Job '" + task.Name + "'";
	}
	@Override protected String getStepInstructions() 
	{
		return "Would you like to specify which cloud folder to sync from"; 
	}
	private void startNextStep(int action)
	{
		Intent intent;

    	boolean hasContainers = Cloud.getCloud(engine).hasContainers();
		
    	switch(action)
		{
		case ACTION_SPECIFY_LOCATION:
			intent = new Intent(this, hasContainers ? CloudContainerActivity.class : CloudFolderActivity.class);
			putExtras(intent);
			startActivityForResult(intent, ANONYMOUS_REQUEST_CODE);
			break;
		case ACTION_SELECT_ROOT:
			intent = new Intent(this, SelectLocalFolderActivity.class);
			putExtras(intent);
			startActivityForResult(intent, ANONYMOUS_REQUEST_CODE);
			break;
		case ACTION_LEAVE:
			intent = new Intent(this, SelectLocalFolderActivity.class);
			putExtras(intent);
			startActivityForResult(intent, ANONYMOUS_REQUEST_CODE);
			break;
		}
	}
	private void init()
	{
		bind();
	}
	private void bind()
	{
		btn1 = (Button)this.findViewById(R.id.btn1);
		btn2 = (Button)this.findViewById(R.id.btn2);
		btn3 = (Button)this.findViewById(R.id.btn3);
		btn4 = (Button)this.findViewById(R.id.btn4);
		buttons.add(btn1);
		buttons.add(btn2);
		buttons.add(btn3);
		buttons.add(btn4);
		
		btn1.setVisibility(View.VISIBLE);
		btn2.setVisibility(View.VISIBLE);
		
		btn1.setText(R.string.specify_cloud_location);
		btn1.setOnClickListener(chooseAction(SelectCloudResourceActivity.ACTION_SPECIFY_LOCATION));
		if(task.ID == -1)
		{
			btn2.setText(R.string.cloud_default_location);
			btn2.setOnClickListener(chooseAction(SelectCloudResourceActivity.ACTION_SELECT_ROOT));
		} else {
			btn2.setText(R.string.no_cloud_location_change);
			btn2.setOnClickListener(chooseAction(SelectCloudResourceActivity.ACTION_LEAVE));
		}
	}
	
	private OnClickListener chooseAction(int action)
	{
		final int finalAction = action;
		return new OnClickListener() { public void onClick(View v) { startNextStep(finalAction); }};
	}

}
