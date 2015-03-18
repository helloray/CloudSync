package net.netortech.cloudsync.clouds;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Date;

import net.netortech.cloudsync.data.TaskItem;

import android.util.Log;


public class SyncFile {
	public String Path;
	public String Name;
	private File Directory;
	private File File;
	
	public SyncFile(String path, TaskItem item)
	{
		this.Path = path;
		if(!item.Path.equals("")) this.Path += File.separator + item.Path;
		this.Name = item.Name;
		log("Path: " + Path + ", Name: " + Name);
		init();		
	}
	public SyncFile(String path, CloudItem item)
	{
		this.Path = path;
		if(!item.getPath().equals("")) this.Path += File.separator + item.getPath();
		this.Name = item.getName();
		log("Path: " + Path + ", Name: " + Name);
		init();
	}

	private void init()
	{
		try
		{
			File = new File(Path, Name);
			Directory = new File(Path);
		} catch(Exception ex)
		{
			log("SyncFile('" + Path + "', '" + Name + "'): Error in java.util.File constructor: " + ex);
		}
	}
	
	public Boolean Exists()
	{
		return File.exists();
	}
	public void Delete()
	{
		File.delete();
	}
	public long GetSize()
	{
		return File.length();
	}
	public Boolean DirectoryExists()
	{
		return Directory.exists();
	}
	public Boolean DirectoryIOAccess()
	{
		return Directory.canRead() & Directory.canWrite();
	}
	public Boolean mkdir()
	{
		Boolean ret = Directory.mkdir();
		if(ret) return ret;
		
		// Try to figure out what's wrong. Let's break down the directory branch.
		String[] segments = Path.split(File.separator);
		File dir;
		String arm = "";
		log("Could not create directory. Splitting '" + Path + "' into arms.");
		for(int index = 1; index < segments.length; index++)
		{
			if(segments[index].equals("") && index == segments.length - 1) break; // Trailing separator.
			arm += File.separator + segments[index];
			log("Checking arm '" + arm + "'...");
			dir = new File(arm);
			if(!dir.exists())
			{
				log("Arm did not exist. Creating...");
				ret = dir.mkdir();
				if(!ret)
				{
					log("... failed. Giving up.");
					break;
				}
			} else if (dir.isFile()) {
				log("Arm conflicts with a file name.");
				throw new RuntimeException("Directory '" + Path + "' could not be created because a file with a conflicting name exists at '" + arm + "'. Move, delete or rename this file to resolve the issue.");
			}
		}

		return ret; 
	}
	public FileOutputStream getOutputStream()
	{
		try
		{
			log("Creating FileOutputStream with parameters ('" + Path + "', '" + Name + "')");
			return new FileOutputStream(new File(Path, Name));
		} 
		catch (Exception ex)
		{
			if(RuntimeException.class.isAssignableFrom(ex.getClass())) throw (RuntimeException)ex;
			log("getOutputStream() failed: " + ex);
			return null;
		}
	}
	
	public Date lastModified()
	{
		File file = new File(Path, Name);
		return new Date(file.lastModified());
	}
	
	@Override 
	public String toString()
	{
		return Path + File.separator + Name;
	}
	
	private void log(String msg)
	{
		log(msg, false);
	}
	private void log(String msg, Boolean quiet)
	{
		if(!quiet) return;
		Log.v("SyncFile", msg);
	}
}
