package net.netortech.cloudsync.data;

import android.content.ContentValues;
import android.database.Cursor;

public interface IElement {
	public ContentValues getValues();
	public String[] getFields();
	public void Bind(Cursor c);
	public Boolean Validate();
}
