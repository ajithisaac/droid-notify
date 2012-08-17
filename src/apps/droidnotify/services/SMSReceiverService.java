package apps.droidnotify.services;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.telephony.TelephonyManager;

import apps.droidnotify.common.Common;
import apps.droidnotify.common.Constants;
import apps.droidnotify.log.Log;
import apps.droidnotify.sms.SMSCommon;

/**
 * This class does the work of the BroadcastReceiver.
 * 
 * @author Camille S�vigny
 */
public class SMSReceiverService extends WakefulIntentService {
	
	//================================================================================
    // Properties
    //================================================================================
	
	boolean _debug = false;

	//================================================================================
	// Public Methods
	//================================================================================
	
	/**
	 * Class Constructor.
	 */
	public SMSReceiverService() {
		super("SMSBroadcastReceiverService");
		_debug = Log.getDebug();
		if (_debug) Log.v("SMSBroadcastReceiverService.SMSBroadcastReceiverService()");
	}

	//================================================================================
	// Protected Methods
	//================================================================================
	
	/**
	 * Do the work for the service inside this function.
	 * 
	 * @param intent - Intent object that we are working with.
	 */
	@Override
	protected void doWakefulWork(Intent intent) {
		if (_debug) Log.v("SMSBroadcastReceiverService.doWakefulWork()");
		try{
			Context context = getApplicationContext();
			SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
			//Block the notification if it's quiet time.
			if(Common.isQuietTime(context)){
				if (_debug) Log.v("SMSBroadcastReceiverService.doWakefulWork() Quiet Time. Exiting...");
				return;
			}
			//Check for a blacklist entry before doing anything else.
    		Bundle bundle = intent.getExtras();
    		Bundle smsNotificationBundle = SMSCommon.getSMSMessagesFromIntent(context, bundle);	
    		Bundle smsNotificationBundleSingle = null;
    		if(smsNotificationBundle != null){
    			smsNotificationBundleSingle = smsNotificationBundle.getBundle(Constants.BUNDLE_NOTIFICATION_BUNDLE_NAME + "_1");
    		}
		    //Check the state of the users phone.
		    TelephonyManager telemanager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
		    boolean notificationIsBlocked = false;
		    boolean rescheduleNotificationInCall = true;
		    boolean rescheduleNotificationInQuickReply = true;
		    boolean callStateIdle = telemanager.getCallState() == TelephonyManager.CALL_STATE_IDLE;
		    //Reschedule notification based on the users preferences.
		    if(!callStateIdle){
		    	notificationIsBlocked = true;		    	
		    	rescheduleNotificationInCall = preferences.getBoolean(Constants.IN_CALL_RESCHEDULING_ENABLED_KEY, false);
		    }else{
		    	notificationIsBlocked = Common.isNotificationBlocked(context);
		    }
		    if(!notificationIsBlocked){
				Intent smsIntent = new Intent(context, SMSService.class);
				smsIntent.putExtras(intent.getExtras());
				WakefulIntentService.sendWakefulWork(context, smsIntent);
		    }else{		    		
		    	//Display the Status Bar Notification even though the popup is blocked based on the user preferences.
		    	if(preferences.getBoolean(Constants.SMS_STATUS_BAR_NOTIFICATIONS_SHOW_WHEN_BLOCKED_ENABLED_KEY, true)){
	    			if(smsNotificationBundleSingle != null){
						//Display Status Bar Notification
					    Common.setStatusBarNotification(context, 1, Constants.NOTIFICATION_TYPE_SMS, 0, callStateIdle, smsNotificationBundleSingle.getString(Constants.BUNDLE_CONTACT_NAME), smsNotificationBundleSingle.getLong(Constants.BUNDLE_CONTACT_ID, -1), smsNotificationBundleSingle.getString(Constants.BUNDLE_SENT_FROM_ADDRESS), smsNotificationBundleSingle.getString(Constants.BUNDLE_MESSAGE_BODY), null, null, smsNotificationBundleSingle.getLong(Constants.BUNDLE_THREAD_ID, -1), false, Common.getStatusBarNotificationBundle(context, Constants.NOTIFICATION_TYPE_SMS));
	    			}
			    }					
		    	if(smsNotificationBundle != null) Common.rescheduleBlockedNotification(context, rescheduleNotificationInCall, rescheduleNotificationInQuickReply, Constants.NOTIFICATION_TYPE_SMS, smsNotificationBundle);
		    }
		}catch(Exception ex){
			Log.e("SMSBroadcastReceiverService.doWakefulWork() ERROR: " + ex.toString());
		}
	}
		
}