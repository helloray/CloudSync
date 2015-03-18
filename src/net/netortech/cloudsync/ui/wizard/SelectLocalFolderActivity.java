package net.netortech.cloudsync.ui.wizard;

import java.util.LinkedList;
import java.util.List;

import net.netortech.cloudsync.R;
import net.netortech.cloudsync.data.TaskInfo;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class SelectLocalFolderActivity extends WizardStepActivity {
	Button btn1, btn2, btn3, btn4;
	List<Button> buttons = new LinkedList<Button>();
	
	private static final int INTENT_FIRST_QUESTION = 0;
	private static final int INTENT_QUESTION_WHAT_TYPE_OF_FILES = 1;
	private static final int INTENT_QUESTION_WOULD_YOU_SPECIFY = 2;
	
	private static final int ACTION_SHOW_TYPE_QUESTION = 0;
	private static final int ACTION_SHOW_SPECIFY_QUESTION = 1;
	private static final int ACTION_SPECIFY_LOCATION = 2;
	private static final int ACTION_DEFAULT_MEDIA = 3;
	private static final int ACTION_LEAVE = 4;
	
	int currentQuestions = INTENT_FIRST_QUESTION;
	
	@Override protected void btnBack_Click() { finish(); }
	@Override protected String getActivityName() { return "SelectLocalFolderActivity"; }
	@Override protected int getLayoutId() { return R.layout.wizard_multi_button; }
	
	@Override
	public void onWizardStepCreated(Bundle b)
	{
		init();
		log("Task path currently: " + task.Path);
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
		switch(currentQuestions)
		{
		case INTENT_QUESTION_WHAT_TYPE_OF_FILES:
			return "What kinds of files will this job be syncing";
		case INTENT_QUESTION_WOULD_YOU_SPECIFY:
			return "Would you like to store these files with the rest of your media";
		default:
			return "Would you like to change where this job stores files";
		}
	}
	private void startNextStep(int action)
	{
		Intent intent;
		
		switch(action)
		{
		case ACTION_SHOW_TYPE_QUESTION:
			intent = new Intent(this, SelectLocalFolderActivity.class);
			putExtras(intent);
			intent.putExtra("questions", INTENT_QUESTION_WHAT_TYPE_OF_FILES);
			startActivityForResult(intent, ANONYMOUS_REQUEST_CODE);
			break;
		case ACTION_SHOW_SPECIFY_QUESTION:
			intent = new Intent(this, SelectLocalFolderActivity.class);
			putExtras(intent);
			intent.putExtra("questions", INTENT_QUESTION_WOULD_YOU_SPECIFY);
			startActivityForResult(intent, ANONYMOUS_REQUEST_CODE);
			break;
		case ACTION_SPECIFY_LOCATION:
			task.Path = task.ID != -1 ? task.Path : currentQuestions == INTENT_QUESTION_WHAT_TYPE_OF_FILES ? TaskInfo.DEFAULT_SYNC_FOLDER : TaskInfo.DEFAULT_MEDIA_FOLDER; 
			intent = new Intent(this, LocalFolderActivity.class);
			putExtras(intent);
			startActivityForResult(intent, ANONYMOUS_REQUEST_CODE);
			break;
		case ACTION_DEFAULT_MEDIA:
			task.Path = TaskInfo.DEFAULT_MEDIA_FOLDER;
			log("Set task path to: " + task.Path);
			finishWizard();
			break;
		case ACTION_LEAVE:
			finishWizard();
			break;
		}
	}
	private void init()
	{
		Intent intent = getIntent();
		if(intent.hasExtra("questions")) currentQuestions = intent.getIntExtra("questions", INTENT_FIRST_QUESTION);
		if(currentQuestions == INTENT_FIRST_QUESTION && task.ID == -1) currentQuestions = INTENT_QUESTION_WHAT_TYPE_OF_FILES;
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
		
		switch(currentQuestions)
		{
			case INTENT_QUESTION_WHAT_TYPE_OF_FILES:
				btn1.setText(R.string.media_files);
				btn1.setOnClickListener(chooseAction(SelectLocalFolderActivity.ACTION_SHOW_SPECIFY_QUESTION));
				btn2.setText(R.string.assorted_files);
				btn2.setOnClickListener(chooseAction(SelectLocalFolderActivity.ACTION_SPECIFY_LOCATION));
				break;
			case INTENT_QUESTION_WOULD_YOU_SPECIFY:
				btn1.setText(R.string.store_with_media);
				btn1.setOnClickListener(chooseAction(SelectLocalFolderActivity.ACTION_DEFAULT_MEDIA));
				btn2.setText(R.string.specify_local_location);
				btn2.setOnClickListener(chooseAction(SelectLocalFolderActivity.ACTION_SPECIFY_LOCATION));
				break;
			default:
				btn2.setText(R.string.no_local_location_change);
				btn2.setOnClickListener(chooseAction(SelectLocalFolderActivity.ACTION_LEAVE));
				btn1.setText(R.string.local_location_change);
				btn1.setOnClickListener(chooseAction(SelectLocalFolderActivity.ACTION_SHOW_TYPE_QUESTION));
				break;
		}
	}
	
	private OnClickListener chooseAction(int action)
	{
		final int finalAction = action;
		return new OnClickListener() { public void onClick(View v) { startNextStep(finalAction); }};
	}

}
