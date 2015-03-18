package net.netortech.cloudsync;

import android.app.ListActivity;
import android.content.Context;

abstract public class BaseListActivity extends ListActivity {
	private Context dbContext = null;
	
	protected Context getDBContext()
	{
		if(dbContext == null) dbContext = this.getApplication();
		return dbContext;
	}
}
