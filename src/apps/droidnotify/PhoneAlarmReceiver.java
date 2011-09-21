package apps.droidnotify;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.telephony.TelephonyManager;
import apps.droidnotify.common.Common;
import apps.droidnotify.common.Constants;
import apps.droidnotify.log.Log;

/**
 * This class listens for scheduled Missed Call notifications that we want to display.
 * 
 * @author Camille S�vigny
 */
public class PhoneAlarmReceiver extends BroadcastReceiver {
	
	//================================================================================
    // Properties
    //================================================================================

	private boolean _debug = false;

	//================================================================================
	// Public Methods
	//================================================================================
	
	/**
	 * This function starts the service that will handle the work or reschedules the work if the phone is in use.
	 * 
	 * @param context - Application Context.
	 * @param intent - Intent object that we are working with.
	 */
	@Override
	public void onReceive(Context context, Intent intent) {
		_debug = Log.getDebug();
		if (_debug) Log.v("PhoneAlarmReceiver.onReceive()");
		//Read preferences and exit if app is disabled.
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
	    if(!preferences.getBoolean(Constants.APP_ENABLED_KEY, true)){
			if (_debug) Log.v("PhoneAlarmReceiver.onReceive() App Disabled. Exiting...");
			return;
		}
		//Read preferences and exit if missed call notifications are disabled.
	    if(!preferences.getBoolean(Constants.MISSED_CALL_NOTIFICATIONS_ENABLED_KEY, true)){
			if (_debug) Log.v("PhoneAlarmReceiver.onReceive() Missed Call Notifications Disabled. Exiting... ");
			return;
		}
	    //Check the state of the users phone.
		TelephonyManager telemanager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
	    boolean rescheduleNotification = false;
	    boolean callStateIdle = telemanager.getCallState() == TelephonyManager.CALL_STATE_IDLE;
	    boolean inMessagingApp = preferences.getBoolean(Constants.USER_IN_MESSAGING_APP, false);
	    boolean messagingAppRunning = Common.isMessagingAppRunning(context);
	    String messagingAppRuningAction = preferences.getString(Constants.MESSAGING_APP_RUNNING_ACTION_PHONE, "0");
	    if(!callStateIdle || inMessagingApp){
	    	rescheduleNotification = true;
	    }else{
	    	//Messaging App is running.
	    	if(messagingAppRunning){
	    		//Reschedule notification based on the users preferences.
			    if(messagingAppRuningAction.equals(Constants.MESSAGING_APP_RUNNING_ACTION_RESCHEDULE)){
					rescheduleNotification = true;
			    }
			    //Ignore notification based on the users preferences.
			    if(messagingAppRuningAction.equals(Constants.MESSAGING_APP_RUNNING_ACTION_IGNORE)){
			    	return;
			    }
	    	}
	    }
	    if(!rescheduleNotification){
			WakefulIntentService.acquireStaticLock(context);
			context.startService(new Intent(context, PhoneReceiverService.class));
	    }else{
	    	// Set alarm to go off x minutes from the current time as defined by the user preferences.
	    	long rescheduleInterval = Long.parseLong(preferences.getString(Constants.RESCHEDULE_NOTIFICATION_TIMEOUT_KEY, "5")) * 60 * 1000;
	    	if(preferences.getBoolean(Constants.RESCHEDULE_NOTIFICATIONS_ENABLED, true)){
	    		if(rescheduleInterval == 0){
	    			SharedPreferences.Editor editor = preferences.edit();
	    			editor.putBoolean(Constants.RESCHEDULE_NOTIFICATIONS_ENABLED, false);
	    			editor.commit();
	    			return;
	    		}
	    		if (_debug) Log.v("PhoneAlarmReceiver.onReceive() Rescheduling notification. Rechedule in " + rescheduleInterval + "minutes.");
				AlarmManager alarmManager = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
				Intent phoneIntent = new Intent(context, PhoneAlarmReceiver.class);
				phoneIntent.setAction("apps.droidnotify.VIEW/PhoneReschedule/" + System.currentTimeMillis());
				PendingIntent phonePendingIntent = PendingIntent.getBroadcast(context, 0, phoneIntent, 0);
				alarmManager.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + rescheduleInterval, phonePendingIntent);
	    	}
	    }
	}

}