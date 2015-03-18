package net.netortech.cloudsync;

import android.app.Activity;
import android.content.Context;

abstract public class BaseActivity extends Activity {
	private Context dbContext = null;
	
	protected Context getDBContext()
	{
		if(dbContext == null) dbContext = this.getApplication();
		return dbContext;
	}
}
