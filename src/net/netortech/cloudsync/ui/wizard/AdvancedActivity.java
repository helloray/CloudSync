package net.netortech.cloudsync.ui.wizard;

import java.util.LinkedList;
import java.util.List;

import net.netortech.cloudsync.R;
import net.netortech.cloudsync.data.TaskInfo;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;

import com.netortech.utilities.ListItem;

public class AdvancedActivity extends WizardStepActivity {
	Spinner ddlWifi, ddlMobile;
	Button btnFinished; 
	
	@Override protected void btnBack_Click() { finish(); }
	@Override protected String getActivityName() { return "AdvancedActivity"; }
	@Override protected int getLayoutId() { return R.layout.wizard_advanced; }
	@Override protected String getStepInstructions() { return "What size files would you like to download over wifi and your mobile data network"; }
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
	
	private void initStep()
	{
		bind();
	}
	private void bind()
	{
		ddlWifi = (Spinner)this.findViewById(R.id.ddlWifiSyncSize);
		ddlMobile = (Spinner)this.findViewById(R.id.ddlMobileSyncSize);
		btnFinished = (Button)this.findViewById(R.id.btnFinished);

		btnFinished.setOnClickListener(new OnClickListener() { public void onClick(View v) { AdvancedActivity.this.finishWizard(WizardStepActivity.FINISH_NO_ADVANCED); } });
		
		List<ListItem> wifiSelections = new LinkedList<ListItem>();
		wifiSelections.add(new ListItem(-1, "Any size files", ""));
		wifiSelections.add(new ListItem(100 * TaskInfo.MEGABYTE, "Large files (< 100 mb)", ""));
		wifiSelections.add(new ListItem(20 * TaskInfo.MEGABYTE, "Medium files (< 20 mb)", ""));
		wifiSelections.add(new ListItem(5 * TaskInfo.MEGABYTE, "Small files (< 5 mb)", ""));
		wifiSelections.add(new ListItem(0, "Do not sync over wifi", ""));
		
		ArrayAdapter<ListItem> adapter = new ArrayAdapter<ListItem>(this, android.R.layout.simple_spinner_item, wifiSelections);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		ddlWifi.setAdapter(adapter);
        ddlWifi.setOnItemSelectedListener(new OnItemSelectedListener() {
		    public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) 
		    { 
		    	ListItem i = (ListItem)ddlWifi.getAdapter().getItem(pos);
		    	task.MaxSize = i.ID;
	    	}
		    public void onNothingSelected(AdapterView<?> parent) { }
		});

		List<ListItem> mobileSelections = new LinkedList<ListItem>();
		mobileSelections.add(new ListItem(-1, "Any size files", ""));
		mobileSelections.add(new ListItem(100 * TaskInfo.MEGABYTE, "Large files (< 100 mb)", ""));
		mobileSelections.add(new ListItem(20 * TaskInfo.MEGABYTE, "Medium files (< 20 mb)", ""));
		mobileSelections.add(new ListItem(5 * TaskInfo.MEGABYTE, "Small files (< 5 mb)", ""));
		mobileSelections.add(new ListItem(0, "Do not sync over mobile network", ""));
		
		adapter = new ArrayAdapter<ListItem>(this, android.R.layout.simple_spinner_item, mobileSelections);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		ddlMobile.setAdapter(adapter);
        ddlMobile.setOnItemSelectedListener(new OnItemSelectedListener() {
		    public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) 
		    { 
		    	ListItem i = (ListItem)ddlMobile.getAdapter().getItem(pos);
		    	task.MobileMaxSize = i.ID;
	    	}
		    public void onNothingSelected(AdapterView<?> parent) { }
		});
        
        for(int index = 0; index < mobileSelections.size(); index++)
    		if(mobileSelections.get(index).ID == task.MobileMaxSize) 
    			ddlMobile.setSelection(((ArrayAdapter<ListItem>)ddlMobile.getAdapter()).getPosition(mobileSelections.get(index)));
        for(int index = 0; index < wifiSelections.size(); index++)
    		if(wifiSelections.get(index).ID == task.MaxSize) 
    			ddlWifi.setSelection(((ArrayAdapter<ListItem>)ddlWifi.getAdapter()).getPosition(wifiSelections.get(index)));
	}
}
