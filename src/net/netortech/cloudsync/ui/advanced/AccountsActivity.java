package net.netortech.cloudsync.ui.advanced;

import net.netortech.cloudsync.BaseListActivity;
import net.netortech.cloudsync.CloudSyncActivity;
import net.netortech.cloudsync.R;
import net.netortech.cloudsync.data.AccountInfo;
import net.netortech.cloudsync.data.EngineInfo;
import net.netortech.cloudsync.data.SettingsDB;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class AccountsActivity extends BaseListActivity {
	SettingsDB db = CloudSyncActivity.db;
	
	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.advanced_accounts);
        Init();
    }
    @Override
    public void onResume()
    {
    	super.onResume();
    	Bind();
    }
    
	private void Init()
	{
        Bind();
	}
    private void Bind()
    {
    	final Button btnAddAccount = (Button)findViewById(R.id.btnAddAccount);
    	btnAddAccount.setOnClickListener(new OnClickListener(){ public void onClick(View v) {
        	Intent i = new Intent(AccountsActivity.this, AccountEditActivity.class);
            startActivity(i);
        }});
    	BindList();
    }
    private void BindList()
    {
    	ArrayAdapter<AccountInfo> adapter = new ArrayAdapter<AccountInfo>(this, R.layout.advanced_account_item, SettingsDB.getAllAccountInfo(getDBContext())) {
	        @Override
	        public View getView(int position, View convertView, ViewGroup parent) {
	                View v = convertView;
	                final AccountInfo a = getItem(position);
	                final EngineInfo e = SettingsDB.getEngineInfo(a.EngineID, getDBContext());
	                if (v == null) {
	                    LayoutInflater vi = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	                    v = vi.inflate(R.layout.advanced_account_item, null);
	                }
	                v.setOnClickListener(new OnClickListener(){ public void onClick(View v) {
	                	Intent i = new Intent(AccountsActivity.this, AccountEditActivity.class);
	                	Bundle b = new Bundle();
	                	b.putLong("AccountID", a.ID);
	                	i.putExtras(b);
			            startActivity(i);
	                }});
	                ImageView i = (ImageView) v.findViewById(R.id.imgAccountItem);
                    TextView t = (TextView) v.findViewById(R.id.lblAccountItemTitle);
                    TextView d = (TextView) v.findViewById(R.id.lblAccountItemDescription);
                    if(a.Failures > AccountInfo.MAX_FAILURES_COUNT) i.setImageResource(R.drawable.emo_im_crying);
                    t.setText(a.Name);  
                    d.setText("Type: " + e.Name);
	                return v;
	        }
		};
		this.setListAdapter(adapter);
	}
    public void alert(String msg)
    {
		Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }
    protected static void log(String msg)
    {
    	log(msg, false);
    }
    protected static void log(String msg, Boolean quiet)
    {
    	if(!quiet) return;
    	Log.v("AccountEditActivity", msg);
    }   
}

