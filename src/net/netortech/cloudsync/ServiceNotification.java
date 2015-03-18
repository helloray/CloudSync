package net.netortech.cloudsync;

import net.netortech.cloudsync.clouds.DownloadListener;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.RemoteViews;
import android.widget.TextView;

public class ServiceNotification implements DownloadListener {
	NotificationManager manager = null;
	int notificationId = 0;
	long lastNotification = 0;
	
	Notification notification;
	PendingIntent pendingIntent;
	Context context;
	RemoteViews contentView;
	
	String currentTickerText;
	String currentTitle;
	String currentStatus;
	boolean isDownloading = false;
	
	public ServiceNotification(Context context, String tickerText, String title, PendingIntent pendingIntent)
	{
		this.context = context;
		this.currentTickerText = tickerText;
		this.currentTitle = title;
		this.pendingIntent = pendingIntent;
		
		CreateNotification();
	}
	
	private void CreateNotification()
	{
		ColorManager colors = new ColorManager(context);
		
		contentView = new RemoteViews(context.getPackageName(), R.layout.service_notification);
		contentView.setImageViewResource(R.id.image, R.drawable.icon);
		contentView.setTextViewText(R.id.title, (CharSequence)currentTitle);
		contentView.setTextColor(R.id.title, colors.getNotificationTextColor());
		contentView.setTextViewText(R.id.text, (CharSequence)"");
		contentView.setTextColor(R.id.text, colors.getNotificationTextColor());
		
		notification = new Notification();
		notification.icon = R.drawable.icon;
		notification.tickerText = currentTickerText;
		notification.when = System.currentTimeMillis();

		notification.contentView = contentView;
		notification.contentIntent = pendingIntent;
	}

	public void SetNotificationManager(NotificationManager manager, int ID)
	{
		this.manager = manager;
		this.notificationId = ID;
	}
	
	public void SetTitle(String title)
	{
		currentTitle = title;
		if(!isDownloading)
		{
			contentView.setTextViewText(R.id.title, (CharSequence)currentTitle);
			notifyManager();
		}
	}
	public void SetStatus(String status)
	{
		currentStatus = status;
		if(!isDownloading)
		{
			contentView.setTextViewText(R.id.text, (CharSequence)currentStatus);
			notifyManager();
		}
	}
	public void SetText(String title, String status)
	{
		currentTitle = title;
		currentStatus = status;
		if(!isDownloading)
		{
			contentView.setTextViewText(R.id.title, (CharSequence)currentTitle);
			contentView.setTextViewText(R.id.text, (CharSequence)currentStatus);
			notifyManager();
		}
	}
	
	public Notification getNotification()
	{
		return notification;
	}

	private void notifyManager()
	{
		if(manager != null) manager.notify(notificationId, notification);
	}
	
	@Override
	public void DownloadFile(String fileName)
	{
		Log.v("ServiceNotification", "Downloading file: " + fileName);
		isDownloading = true;
		contentView.setViewVisibility(R.id.llStatus, View.VISIBLE);
		contentView.setTextViewText(R.id.text, (CharSequence)"Downloading " + fileName);
		contentView.setInt(R.id.pgStatus, "setProgress", 0);
		lastNotification = System.currentTimeMillis();
		notifyManager();
	}
	
	@Override
	public void ProgressChanged(int percent) {
		if((System.currentTimeMillis() - lastNotification) > 250) // Avoid flooding the notification manager.
		{
			Log.v("ServiceNotification", "Progress changed. " + percent + "%");
			contentView.setInt(R.id.pgStatus, "setProgress", percent);
			lastNotification = System.currentTimeMillis();
			notifyManager();
		}
	}

	@Override
	public void Finished() {
		Log.v("ServiceNotification", "Finished download.");
		isDownloading = false;
		contentView.setViewVisibility(R.id.llStatus, View.GONE);
		contentView.setTextViewText(R.id.text, (CharSequence)currentStatus);
		notifyManager();
	}
	
	private class ColorManager
	{
		public ColorManager(Context context)
		{
			this.context = context;
		}
		private Context context;
		
		// A clever method for determining default notification text colors
		// regardless of API version.
		// Source: http://stackoverflow.com/a/7320604/52551
		private Integer notification_text_color = null;
		private float notification_text_size = 11;
		private final String COLOR_SEARCH_RECURSE_TIP = "SOME_SAMPLE_TEXT";

		public Integer getNotificationTextColor()
		{
			if(notification_text_color == null) extractColors();
			return notification_text_color;
		}
		
		private boolean recurseGroup(ViewGroup gp)
		{
		    final int count = gp.getChildCount();
		    for (int i = 0; i < count; ++i)
		    {
		        if (gp.getChildAt(i) instanceof TextView)
		        {
		            final TextView text = (TextView) gp.getChildAt(i);
		            final String szText = text.getText().toString();
		            if (COLOR_SEARCH_RECURSE_TIP.equals(szText))
		            {
		                notification_text_color = text.getTextColors().getDefaultColor();
		                notification_text_size = text.getTextSize();
		                DisplayMetrics metrics = new DisplayMetrics();
		                WindowManager systemWM = (WindowManager)context.getSystemService(Context.WINDOW_SERVICE);
		                systemWM.getDefaultDisplay().getMetrics(metrics);
		                notification_text_size /= metrics.scaledDensity;
		                return true;
		            }
		        }
		        else if (gp.getChildAt(i) instanceof ViewGroup)
		            return recurseGroup((ViewGroup) gp.getChildAt(i));
		    }
		    return false;
		}

		private void extractColors()
		{
		    if (notification_text_color != null)
		        return;

		    try
		    {
		        Notification ntf = new Notification();
		        ntf.setLatestEventInfo(context, (CharSequence)COLOR_SEARCH_RECURSE_TIP, (CharSequence)"Utest", null);
		        LinearLayout group = new LinearLayout(context);
		        ViewGroup event = (ViewGroup) ntf.contentView.apply(context, group);
		        recurseGroup(event);
		        group.removeAllViews();
		    }
		    catch (Exception e)
		    {
		        notification_text_color = android.R.color.black;
		    }
		}
	}
}
