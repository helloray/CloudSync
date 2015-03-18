package net.netortech.cloudsync.ui.advanced;

import java.util.List;

import net.netortech.cloudsync.BaseActivity;
import net.netortech.cloudsync.CloudSyncActivity;
import net.netortech.cloudsync.R;
import net.netortech.cloudsync.data.SettingsDB;
import net.netortech.cloudsync.data.TaskInfo;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class TasksActivity extends BaseActivity {
	SettingsDB db = CloudSyncActivity.db;
	
	@Override
	public void onCreate(Bundle b)
	{
		super.onCreate(b);
        setContentView(R.layout.advanced_tasks);
        init();
	}
    @Override
    public void onResume()
    {
    	super.onResume();
    	bind();
    }
    
	private void init()
	{
		bind();
	}
	private void bind()
	{
    	final Button btnAddTask = (Button)findViewById(R.id.btnAddTask);
    	btnAddTask.setOnClickListener(new OnClickListener(){ public void onClick(View v) {
        	Intent i = new Intent(TasksActivity.this, TaskEditActivity.class);
            startActivity(i);
        }});

    	List<TaskInfo> tasks = SettingsDB.getAllTaskInfo(getDBContext());
    	LinearLayout lstTasks = (LinearLayout)findViewById(R.id.lstTasks);
    	lstTasks.removeAllViews();
    	
    	for(TaskInfo task : tasks)
    	{
    		final TaskInfo t = task;
	        // final AccountInfo a = db.getAccountInfo(task.AccountID);
	        LayoutInflater vi = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	        View v = vi.inflate(R.layout.advanced_task_item, null);
	        
	        final TextView lblTaskItemTitle = (TextView) v.findViewById(R.id.lblTaskItemTitle);
	        final TextView lblTaskItemDescription = (TextView) v.findViewById(R.id.lblTaskItemDescription);
	        lblTaskItemTitle.setText(task.Name);
	        lblTaskItemDescription.setText("Path: " + task.Path);
	        
	        v.setOnClickListener(new OnClickListener(){ public void onClick(View v) {
            	Intent i = new Intent(TasksActivity.this, TaskEditActivity.class);
            	Bundle b = new Bundle();
            	b.putLong("TaskID", t.ID);
            	i.putExtras(b);
	            startActivity(i);
            }});
	        
	        lstTasks.addView(v);
    	}
	}
			
}
