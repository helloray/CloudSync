package net.netortech.cloudsync.clouds;

public interface DownloadListener {
	public void DownloadFile(String Filename);
	public void ProgressChanged(int percent);
	public void Finished();
}
