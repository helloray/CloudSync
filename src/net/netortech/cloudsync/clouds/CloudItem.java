package net.netortech.cloudsync.clouds;

import java.util.Date;

public class CloudItem {
	public CloudItem(String name, String path, String key, long Size, Date cloudModified)
	{
		this.Name = name == null ? "" : name;
		this.Path = path == null ? "" : path;
		if(this.Path.startsWith("/")) this.Path = this.Path.substring(1);
		this.Key = key == null ? "" : key;
		if(this.Key.startsWith("/")) this.Key = this.Key.substring(1);
		this.Size = Size;
		this.CloudModified = cloudModified;
	}
	private String Name;
	private String Path;
	private String Key;
	private long Size;
	private Date CloudModified;
	
	public String getName() { return Name; }
	public String getPath() { return Path; }
	public String getKey() { return Key; }
	public long getSize() { return Size; }
	public Date getCloudModified() { return CloudModified; }
	
	@Override
	public String toString()
	{
		return "{ Name: " + Name + ", Path: " + Path + ", Key: " + Key + ", CloudModified: " + CloudModified.toLocaleString() + " }";
	}
}
