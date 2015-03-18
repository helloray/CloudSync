package net.netortech.cloudsync.data;

public interface IUniqueElement extends IElement {

	public long getID();
	public void setID(long id);
	public String getIDField();
	public String getTable();
	
}
