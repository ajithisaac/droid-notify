package apps.droidnotify.twitter;

import java.util.Calendar;

import twitter4j.DirectMessage;
import twitter4j.IDs;
import twitter4j.ResponseList;
import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterFactory;
import twitter4j.User;
import twitter4j.auth.AccessToken;
import twitter4j.conf.Configuration;
import twitter4j.conf.ConfigurationBuilder;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.widget.Toast;

import apps.droidnotify.Notification;
import apps.droidnotify.NotificationActivity;
import apps.droidnotify.QuickReplyActivity;
import apps.droidnotify.R;
import apps.droidnotify.common.Common;
import apps.droidnotify.common.Constants;
import apps.droidnotify.receivers.TwitterAlarmReceiver;
import apps.droidnotify.log.Log;

/**
 * This class is a collection of Twitter methods.
 * 
 * @author Camille S�vigny
 */
public class TwitterCommon {

	//================================================================================
    // Properties
    //================================================================================
	
	private static boolean _debug = false; 
	private static Context _context = null;
	
	//================================================================================
	// Public Methods
	//================================================================================
		
	/**
	 * Get Twitter Direct Messages. Read account and notify as needed.
	 * 
	 * @param context - The application context.
	 * 
	 * @return ArrayList<String> - Returns an ArrayList of Strings that contain the Twitter information.
	 */
	public static Bundle getTwitterDirectMessages(Context context, Twitter twitter){
		_debug = Log.getDebug();
		if (_debug) Log.v("TwitterCommon.getTwitterDirectMessages()");
		try{
			SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
			Bundle twitterDirectMessageNotificationBundle = new Bundle();
        	int bundleCount = 0;
			//Retrieve the date filter.
			Calendar today = Calendar.getInstance();
			today.set(Calendar.MILLISECOND, 0);
			today.set(Calendar.SECOND, 0);
			today.set(Calendar.MINUTE, 0);
			today.set(Calendar.HOUR_OF_DAY, 0);
			long currentDateTime = today.getTimeInMillis();
			long dateFilter = preferences.getLong(Constants.TWITTER_DIRECT_MESSAGE_DATE_FILTER_KEY, currentDateTime);		
			long maxDateTime = 0;
		    ResponseList <DirectMessage> messages = twitter.getDirectMessages();		    
			for(DirectMessage message: messages){
				long timeStamp = message.getCreatedAt().getTime();
				if(timeStamp > maxDateTime){
					maxDateTime = timeStamp;
				}
				if(timeStamp > dateFilter){
	        		Bundle twitterDirectMessageNotificationBundleSingle = new Bundle();
	        		bundleCount++;
					String messageBody = message.getText();
					long messageID = message.getId();					
			    	String sentFromAddress = message.getSenderScreenName();
			    	long twitterID = message.getSenderId();
		    		Bundle twitterContactInfoBundle = getContactInfoByTwitterID(context, twitterID);
		    		if(twitterContactInfoBundle == null){
						//Basic Notification Information.
		    			twitterDirectMessageNotificationBundleSingle.putString(Constants.BUNDLE_SENT_FROM_ADDRESS, sentFromAddress);
		    			twitterDirectMessageNotificationBundleSingle.putLong(Constants.BUNDLE_SENT_FROM_ID, twitterID);
		    			twitterDirectMessageNotificationBundleSingle.putString(Constants.BUNDLE_MESSAGE_BODY, messageBody.replace("\n", "<br/>"));
		    			twitterDirectMessageNotificationBundleSingle.putLong(Constants.BUNDLE_MESSAGE_ID, messageID);
		    			twitterDirectMessageNotificationBundleSingle.putString(Constants.BUNDLE_LINK_URL, "http://mobile.twitter.com/" + sentFromAddress + "/messages");
		    			twitterDirectMessageNotificationBundleSingle.putLong(Constants.BUNDLE_TIMESTAMP, timeStamp);	    			
		    			twitterDirectMessageNotificationBundleSingle.putInt(Constants.BUNDLE_NOTIFICATION_TYPE, Constants.NOTIFICATION_TYPE_TWITTER);
		    			twitterDirectMessageNotificationBundleSingle.putInt(Constants.BUNDLE_NOTIFICATION_SUB_TYPE, Constants.NOTIFICATION_TYPE_TWITTER_DIRECT_MESSAGE);
					}else{
						//Basic Notification Information.
		    			twitterDirectMessageNotificationBundleSingle.putString(Constants.BUNDLE_SENT_FROM_ADDRESS, sentFromAddress);
		    			twitterDirectMessageNotificationBundleSingle.putLong(Constants.BUNDLE_SENT_FROM_ID, twitterID);
		    			twitterDirectMessageNotificationBundleSingle.putString(Constants.BUNDLE_MESSAGE_BODY, messageBody.replace("\n", "<br/>"));
		    			twitterDirectMessageNotificationBundleSingle.putLong(Constants.BUNDLE_MESSAGE_ID, messageID);
		    			twitterDirectMessageNotificationBundleSingle.putString(Constants.BUNDLE_LINK_URL, "http://mobile.twitter.com/" + sentFromAddress + "/messages");
		    			twitterDirectMessageNotificationBundleSingle.putLong(Constants.BUNDLE_TIMESTAMP, timeStamp);	    			
		    			twitterDirectMessageNotificationBundleSingle.putInt(Constants.BUNDLE_NOTIFICATION_TYPE, Constants.NOTIFICATION_TYPE_TWITTER);
		    			twitterDirectMessageNotificationBundleSingle.putInt(Constants.BUNDLE_NOTIFICATION_SUB_TYPE, Constants.NOTIFICATION_TYPE_TWITTER_DIRECT_MESSAGE);
		    			//Contact Information.
						twitterDirectMessageNotificationBundleSingle.putLong(Constants.BUNDLE_CONTACT_ID, twitterContactInfoBundle.getLong(Constants.BUNDLE_CONTACT_ID, -1));
						twitterDirectMessageNotificationBundleSingle.putString(Constants.BUNDLE_CONTACT_NAME, twitterContactInfoBundle.getString(Constants.BUNDLE_CONTACT_NAME));
						twitterDirectMessageNotificationBundleSingle.putLong(Constants.BUNDLE_PHOTO_ID, twitterContactInfoBundle.getLong(Constants.BUNDLE_PHOTO_ID, -1));
						twitterDirectMessageNotificationBundleSingle.putString(Constants.BUNDLE_LOOKUP_KEY, twitterContactInfoBundle.getString(Constants.BUNDLE_LOOKUP_KEY));
					}
		    		twitterDirectMessageNotificationBundle.putBundle(Constants.BUNDLE_NOTIFICATION_BUNDLE_NAME + "_" + String.valueOf(bundleCount), twitterDirectMessageNotificationBundleSingle);
				}
			}
			if(bundleCount <= 0){
				if (_debug) Log.v("TwitterCommon.getTwitterDirectMessages() No Twitter Direct Messages Found. Exiting...");
				return null;
			}
			twitterDirectMessageNotificationBundle.putInt(Constants.BUNDLE_NOTIFICATION_BUNDLE_COUNT, bundleCount);
			//Store the max date in the preferences.
			//Don't load any messages that are older than this date next time around.
			SharedPreferences.Editor editor = preferences.edit();
			editor.putLong(Constants.TWITTER_DIRECT_MESSAGE_DATE_FILTER_KEY, maxDateTime);
			editor.commit();
			//Return array.
			return twitterDirectMessageNotificationBundle;
		}catch(Exception ex){
			Log.e("TwitterCommon.getTwitterDirectMessages() ERROR: " + ex.toString());
			return null;
		}
	}

	/**
	 * Get Twitter Mentions. Read account and notify as needed.
	 * 
	 * @param context - The application context.
	 * 
	 * @return ArrayList<String> - Returns an ArrayList of Strings that contain the Twitter information.
	 */
	public static Bundle getTwitterMentions(Context context, Twitter twitter){
		_debug = Log.getDebug();
		if (_debug) Log.v("TwitterCommon.getTwitterMentions()");
		try{
			SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
			Bundle twitterMentionNotificationBundle = new Bundle();
        	int bundleCount = 0;
			//Retrieve the date filter.
			Calendar today = Calendar.getInstance();
			today.set(Calendar.MILLISECOND, -1);
			today.set(Calendar.SECOND, -1);
			today.set(Calendar.MINUTE, -1);
			today.set(Calendar.HOUR_OF_DAY, -1);
			long currentDateTime = today.getTimeInMillis();
			long dateFilter = preferences.getLong(Constants.TWITTER_MENTION_DATE_FILTER_KEY, currentDateTime);		
			long maxDateTime = 0;
			ResponseList<Status> mentions = twitter.getMentions();
			for(Status mention: mentions){
				long timeStamp = mention.getCreatedAt().getTime();
				if(timeStamp > maxDateTime){
					maxDateTime = timeStamp;
				}
				if(timeStamp > dateFilter){
	        		Bundle twitterMentionNotificationBundleSingle = new Bundle();
	        		bundleCount++;
					String mentionText = mention.getText();
					long mentionID = mention.getId();
			    	User twitterUser = mention.getUser();
		    		Bundle twitterContactInfoBundle = getContactInfoByTwitterUser(context, twitterUser);
		    		if(twitterContactInfoBundle == null){
						String sentFromAddress = twitterUser.getScreenName();
						//Basic Notification Information.
		    			twitterMentionNotificationBundleSingle.putString(Constants.BUNDLE_SENT_FROM_ADDRESS, sentFromAddress);
		    			twitterMentionNotificationBundleSingle.putLong(Constants.BUNDLE_SENT_FROM_ID, twitterUser.getId());
		    			twitterMentionNotificationBundleSingle.putString(Constants.BUNDLE_MESSAGE_BODY, mentionText.replace("\n", "<br/>"));
		    			twitterMentionNotificationBundleSingle.putLong(Constants.BUNDLE_MESSAGE_ID, mentionID);
		    			twitterMentionNotificationBundleSingle.putString(Constants.BUNDLE_LINK_URL, "http://mobile.twitter.com/replies");
		    			twitterMentionNotificationBundleSingle.putLong(Constants.BUNDLE_TIMESTAMP, timeStamp);	    			
		    			twitterMentionNotificationBundleSingle.putInt(Constants.BUNDLE_NOTIFICATION_TYPE, Constants.NOTIFICATION_TYPE_TWITTER);
		    			twitterMentionNotificationBundleSingle.putInt(Constants.BUNDLE_NOTIFICATION_SUB_TYPE, Constants.NOTIFICATION_TYPE_TWITTER_MENTION);
		    		}else{
						String sentFromAddress = twitterUser.getScreenName();
						//Basic Notification Information.
		    			twitterMentionNotificationBundleSingle.putString(Constants.BUNDLE_SENT_FROM_ADDRESS, sentFromAddress);
		    			twitterMentionNotificationBundleSingle.putLong(Constants.BUNDLE_SENT_FROM_ID, twitterUser.getId());
		    			twitterMentionNotificationBundleSingle.putString(Constants.BUNDLE_MESSAGE_BODY, mentionText.replace("\n", "<br/>"));
		    			twitterMentionNotificationBundleSingle.putLong(Constants.BUNDLE_MESSAGE_ID, mentionID);
		    			twitterMentionNotificationBundleSingle.putString(Constants.BUNDLE_LINK_URL, "http://mobile.twitter.com/replies");
		    			twitterMentionNotificationBundleSingle.putLong(Constants.BUNDLE_TIMESTAMP, timeStamp);	    			
		    			twitterMentionNotificationBundleSingle.putInt(Constants.BUNDLE_NOTIFICATION_TYPE, Constants.NOTIFICATION_TYPE_TWITTER);
		    			twitterMentionNotificationBundleSingle.putInt(Constants.BUNDLE_NOTIFICATION_SUB_TYPE, Constants.NOTIFICATION_TYPE_TWITTER_MENTION);
		    			//Contact Information.
						twitterMentionNotificationBundleSingle.putLong(Constants.BUNDLE_CONTACT_ID, twitterContactInfoBundle.getLong(Constants.BUNDLE_CONTACT_ID, -1));
						twitterMentionNotificationBundleSingle.putString(Constants.BUNDLE_CONTACT_NAME, twitterContactInfoBundle.getString(Constants.BUNDLE_CONTACT_NAME));
						twitterMentionNotificationBundleSingle.putLong(Constants.BUNDLE_PHOTO_ID, twitterContactInfoBundle.getLong(Constants.BUNDLE_PHOTO_ID, -1));
						twitterMentionNotificationBundleSingle.putString(Constants.BUNDLE_LOOKUP_KEY, twitterContactInfoBundle.getString(Constants.BUNDLE_LOOKUP_KEY));
					}
		    		twitterMentionNotificationBundle.putBundle(Constants.BUNDLE_NOTIFICATION_BUNDLE_NAME + "_" + String.valueOf(bundleCount), twitterMentionNotificationBundleSingle);
				}
			}
			if(bundleCount <= 0){
				if (_debug) Log.v("TwitterCommon.getTwitterMentions() No Twitter Mentions Found. Exiting...");
				return null;
			}
			twitterMentionNotificationBundle.putInt(Constants.BUNDLE_NOTIFICATION_BUNDLE_COUNT, bundleCount);
			//Store the max date in the preferences.
			//Don't load any messages that are older than this date next time around.
			SharedPreferences.Editor editor = preferences.edit();
			editor.putLong(Constants.TWITTER_MENTION_DATE_FILTER_KEY, maxDateTime);
			editor.commit();
			//Return array.
			return twitterMentionNotificationBundle;
		}catch(Exception ex){
			Log.e("TwitterCommon.getTwitterMentions() ERROR: " + ex.toString());
			return null;
		}
	}
	
	/**
	 * Get Twitter Followers. Read account and notify as needed.
	 * 
	 * @param context - The application context.
	 * 
	 * @return ArrayList<String> - Returns an ArrayList of Strings that contain the Twitter information.
	 */
	public static Bundle getTwitterFollowerRequests(Context context, Twitter twitter){
		_debug = Log.getDebug();
		if (_debug) Log.v("TwitterCommon.getTwitterFollowers()");
		try{
			Bundle twitterFollowerRequestNotificationBundle = new Bundle();
        	int bundleCount = 0;
			IDs followerRequests = twitter.getIncomingFriendships(-1);
		    long[] followerIDs = followerRequests.getIDs();
			for(long followerID: followerIDs){
        		Bundle twitterFollowerRequestNotificationBundleSingle = new Bundle();
        		bundleCount++;
				long timeStamp = 0;
				long followerRequestID = 0;
		    	User twitterUser = twitter.showUser(followerID);
		    	String twitterScreenName = twitterUser.getScreenName();
		    	String twitterName = twitterUser.getName();
				String followerMessage = context.getString(R.string.twitter_following_request, twitterName, twitterScreenName);
	    		Bundle twitterContactInfoBundle = getContactInfoByTwitterUser(context, twitterUser);
	    		if(twitterContactInfoBundle == null){
					//Basic Notification Information.
	    			twitterFollowerRequestNotificationBundleSingle.putString(Constants.BUNDLE_SENT_FROM_ADDRESS, twitterScreenName);
	    			twitterFollowerRequestNotificationBundleSingle.putLong(Constants.BUNDLE_SENT_FROM_ID, followerID);
	    			twitterFollowerRequestNotificationBundleSingle.putString(Constants.BUNDLE_MESSAGE_BODY, followerMessage);
	    			twitterFollowerRequestNotificationBundleSingle.putLong(Constants.BUNDLE_MESSAGE_ID, followerRequestID);
	    			twitterFollowerRequestNotificationBundleSingle.putString(Constants.BUNDLE_LINK_URL, "http://mobile.twitter.com");
	    			twitterFollowerRequestNotificationBundleSingle.putLong(Constants.BUNDLE_TIMESTAMP, timeStamp);	    			
	    			twitterFollowerRequestNotificationBundleSingle.putInt(Constants.BUNDLE_NOTIFICATION_TYPE, Constants.NOTIFICATION_TYPE_TWITTER);
	    			twitterFollowerRequestNotificationBundleSingle.putInt(Constants.BUNDLE_NOTIFICATION_SUB_TYPE, Constants.NOTIFICATION_TYPE_TWITTER_FOLLOWER_REQUEST);
				}else{
					//Basic Notification Information.
	    			twitterFollowerRequestNotificationBundleSingle.putString(Constants.BUNDLE_SENT_FROM_ADDRESS, twitterScreenName);
	    			twitterFollowerRequestNotificationBundleSingle.putLong(Constants.BUNDLE_SENT_FROM_ID, followerID);
	    			twitterFollowerRequestNotificationBundleSingle.putString(Constants.BUNDLE_MESSAGE_BODY, followerMessage);
	    			twitterFollowerRequestNotificationBundleSingle.putLong(Constants.BUNDLE_MESSAGE_ID, followerRequestID);
	    			twitterFollowerRequestNotificationBundleSingle.putString(Constants.BUNDLE_LINK_URL, "http://mobile.twitter.com");
	    			twitterFollowerRequestNotificationBundleSingle.putLong(Constants.BUNDLE_TIMESTAMP, timeStamp);	    			
	    			twitterFollowerRequestNotificationBundleSingle.putInt(Constants.BUNDLE_NOTIFICATION_TYPE, Constants.NOTIFICATION_TYPE_TWITTER);
	    			twitterFollowerRequestNotificationBundleSingle.putInt(Constants.BUNDLE_NOTIFICATION_SUB_TYPE, Constants.NOTIFICATION_TYPE_TWITTER_FOLLOWER_REQUEST);
	    			//Contact Information.
					twitterFollowerRequestNotificationBundleSingle.putLong(Constants.BUNDLE_CONTACT_ID, twitterContactInfoBundle.getLong(Constants.BUNDLE_CONTACT_ID, 0));
					twitterFollowerRequestNotificationBundleSingle.putString(Constants.BUNDLE_CONTACT_NAME, twitterContactInfoBundle.getString(Constants.BUNDLE_CONTACT_NAME));
					twitterFollowerRequestNotificationBundleSingle.putLong(Constants.BUNDLE_PHOTO_ID, twitterContactInfoBundle.getLong(Constants.BUNDLE_PHOTO_ID, 0));
					twitterFollowerRequestNotificationBundleSingle.putString(Constants.BUNDLE_LOOKUP_KEY, twitterContactInfoBundle.getString(Constants.BUNDLE_LOOKUP_KEY));
				}
	    		twitterFollowerRequestNotificationBundle.putBundle(Constants.BUNDLE_NOTIFICATION_BUNDLE_NAME + "_" + String.valueOf(bundleCount), twitterFollowerRequestNotificationBundleSingle);
			}
			if(bundleCount <= 0){
				if (_debug) Log.v("TwitterCommon.getTwitterFollowers() No Twitter Follower Requests Found. Exiting...");
				return null;
			}
			twitterFollowerRequestNotificationBundle.putInt(Constants.BUNDLE_NOTIFICATION_BUNDLE_COUNT, bundleCount);
			//Return array.
			return twitterFollowerRequestNotificationBundle;
		}catch(Exception ex){
			Log.e("TwitterCommon.getTwitterFollowers() ERROR: " + ex.toString());
			return null;
		}
	}
	
	/**
	 * Get various contact info for a given Twitter ID.
	 * 
	 * @param context - Application Context.
	 * @param twitterID - The Twitter ID of the person we are searching for.
	 * 
	 * @return Bundle - Returns a Bundle of the contact information.
	 */ 
	public static Bundle getContactInfoByTwitterID(Context context, long twitterID){
		_debug = Log.getDebug();;
		if (_debug) Log.v("TwitterCommon.getContactInfoByTwitterID()");
		if (twitterID == 0) {
			if (_debug) Log.v("TwitterCommon.getContactInfoByTwitterID() Twitter ID provided is 0. Exiting...");
			return null;
		}
		Twitter twitter = getTwitter(context);
		if(twitter == null){
			if (_debug) Log.v("TwitterCommon.getContactInfoByTwitterID() Twitter object is null. Exiting...");
			return null;
		}
		try{
			User twitterUser = twitter.showUser(twitterID);
			return getContactInfoByTwitterUser(context, twitterUser);
		}catch(Exception ex){
			Log.e("TwitterCommon.getContactInfoByTwitterID() ERROR: " + ex.toString());
			return null;
		}
	}

	/**
	 * Get various contact info for a given Twitter User object.
	 * 
	 * @param context - Application Context.
	 * @param twitterUser - The Twitter User object of the person we are searching for.
	 * 
	 * @return Bundle - Returns a Bundle of the contact information.
	 */ 
	public static Bundle getContactInfoByTwitterUser(Context context, User twitterUser){
		_debug = Log.getDebug();;
		if (_debug) Log.v("TwitterCommon.getContactInfoByTwitterUser()");
		if (twitterUser == null) {
			if (_debug) Log.v("TwitterCommon.getContactInfoByTwitterUser() Twitter User provided is null. Exiting...");
			return null;
		}
		try{
			String twitterName = twitterUser.getName();
			return Common.getContactsInfoByName(context, twitterName);
		}catch(Exception ex){
			Log.e("TwitterCommon.getContactInfoByTwitterUser() ERROR: " + ex.toString());
			return null;
		}
	}
	
	/**
	 * Delete a Twitter item.
	 * 
	 * @param context - The current context of this Activity.
	 * @param messageID - The message ID that we want to delete.
	 */
	public static void deleteTwitterItem(Context context, Notification notification){
		_debug = Log.getDebug();
		if (_debug) Log.v("TwitterCommon.deleteTwitterItem()");
		try{
			switch(notification.getNotificationSubType()){
				case Constants.NOTIFICATION_TYPE_TWITTER_DIRECT_MESSAGE:{
					deleteTwitterDirectMessage(context, notification.getMessageID());
					return;
				}
			}
		}catch(Exception ex){
			Log.e("TwitterCommon.deleteTwitterItem() ERROR: " + ex.toString());
		}
	}
	
	/**
	 * Delete a Twitter Direct Message.
	 * 
	 * @param context - The current context of this Activity.
	 * @param messageID - The message ID that we want to delete.
	 */
	public static void deleteTwitterDirectMessage(Context context, long messageID){
		_debug = Log.getDebug();
		if (_debug) Log.v("TwitterCommon.deleteTwitterDirectMessage()");
		try{
			_context = context;
			if(messageID == 0){
				if (_debug) Log.v("TwitterCommon.deleteTwitterDirectMessage() messageID == 0. Exiting...");
				return;
			}
			new deleteTwitterDirectMessageAsyncTask().execute(messageID);
			return;
		}catch(Exception ex){
			Log.e("TwitterCommon.deleteTwitterDirectMessage() ERROR: " + ex.toString());
			return;
		}
	}	
	
	/**
	 * Launch a Twitter application.
	 * 
	 * @param context - Application Context.
	 * @param notificationActivity - A reference to the parent activity.
	 * @param requestCode - The request code we want returned.
	 * 
	 * @return boolean - Returns true if the application can be launched.
	 */
	public static boolean startTwitterAppActivity(Context context, NotificationActivity notificationActivity, int requestCode){
		_debug = Log.getDebug();
		if (_debug) Log.v("TwitterCommon.startTwitterAppActivity()");
		try{
			Intent intent = getTwitterAppActivityIntent(context);
			if(intent == null){
				if (_debug) Log.v("TwitterCommon.startTwitterAppActivity() Application Not Found");
				Toast.makeText(context, context.getString(R.string.twitter_app_not_found_error), Toast.LENGTH_LONG).show();
				Common.setInLinkedAppFlag(context, false);
				return false;
			}
	        notificationActivity.startActivityForResult(intent, requestCode);
	        Common.setInLinkedAppFlag(context, true);
		    return true;
		}catch(Exception ex){
			Log.e("TwitterCommon.startTwitterAppActivity() ERROR: " + ex.toString());
			Toast.makeText(context, context.getString(R.string.twitter_app_error), Toast.LENGTH_LONG).show();
			Common.setInLinkedAppFlag(context, false);
			return false;
		}
	}

	/**
	 * Get the Intent to launch a Twitter application.
	 * 
	 * @param context - Application Context.
	 * @param notificationActivity - A reference to the parent activity.
	 * 
	 * @return Intent - Returns the Intent.
	 */
	public static Intent getTwitterAppActivityIntent(Context context){
		_debug = Log.getDebug();
		if (_debug) Log.v("TwitterCommon.getTwitterAppActivityIntent()");
		try{
			SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
			String packageName = preferences.getString(Constants.TWITTER_PREFERRED_CLIENT_KEY, Constants.TWITTER_PREFERRED_CLIENT_DEFAULT);
			if(packageName.startsWith("http://")){
				Intent browserIntent = new Intent(Intent.ACTION_VIEW);	
				browserIntent.setData(Uri.parse(packageName));
				browserIntent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
				return browserIntent;
			}
			Intent intent = context.getPackageManager().getLaunchIntentForPackage(packageName);
			if(intent == null){
				if (_debug) Log.v("TwitterCommon.getTwitterAppActivityIntent() Package '" + packageName + "' Not Found. Exiting...");
				return null;
			}
			intent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
	        return intent;
		}catch(Exception ex){
			Log.e("TwitterCommon.getTwitterAppActivityIntent() ERROR: " + ex.toString());
			return null;
		}
	}
	
	/**
	 * Start the intent for the Quick Reply activity send a reply.
	 * 
	 * @param context - Application Context.
	 * @param notificationActivity - A reference to the parent activity.
	 * @param phoneNumber - The phone number we want to send a message to.
	 * @param requestCode - The request code we want returned.
	 * 
	 * @return boolean - Returns true if the activity can be started.
	 */
	public static boolean startTwitterQuickReplyActivity(Context context, NotificationActivity notificationActivity, int requestCode, long sendToID, String sendTo, String name, int notificationSubType){
		_debug = Log.getDebug();
		if (_debug) Log.v("TwitterCommon.startTwitterQuickReplyActivity()");
		if(sendToID == 0){
			Toast.makeText(context, context.getString(R.string.app_quick_reply_address_error), Toast.LENGTH_LONG).show();
			return false;
		}
		try{
			Intent intent = new Intent(context, QuickReplyActivity.class);
	        if (_debug) Log.v("NotificationView.replyToMessage() Put bundle in intent");
	        Bundle bundle = new Bundle();
	        bundle.putInt("notificationType", Constants.NOTIFICATION_TYPE_TWITTER);
	        bundle.putInt("notificationSubType", notificationSubType);
	        bundle.putLong("sendToID", sendToID);
	        bundle.putString("sendTo", sendTo);
		    if(name != null && !name.equals(context.getString(android.R.string.unknownName))){
		    	bundle.putString("name", name);
		    }else{
		    	bundle.putString("name", "");
		    }
		    bundle.putString("message", "");
		    intent.putExtras(bundle);
	        intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
	        notificationActivity.startActivityForResult(intent, requestCode);
	        Common.setInLinkedAppFlag(context, true);
	        return true;
		}catch(Exception ex){
			Log.e("TwitterCommon.startTwitterQuickReplyActivity() ERROR: " + ex.toString());
			Toast.makeText(context, context.getString(R.string.app_android_quick_reply_app_error), Toast.LENGTH_LONG).show();
			Common.setInLinkedAppFlag(context, false);
			return false;
		}
	}
	
	/**
	 * Send a Twitter Direct Message to a user.
	 * 
	 * @param context - Application Context.
	 * @param userID - The ID of the user we are sending the message to.
	 * @param message - The message we want to send.
	 * 
	 * @return boolean - Returns true if the message was sent successfully.
	 */
	public static boolean sendTwitterDirectMessage(Context context, long userID, String message){
		_debug = Log.getDebug();
		if (_debug) Log.v("TwitterCommon.sendTwitterDirectMessage()");
		try{
			_context = context;
			new sendTwitterDirectMessageAsyncTask().execute(String.valueOf(userID), message);
			return true;
		}catch(Exception ex){
			if (_debug) Log.v("TwitterCommon.sendTwitterDirectMessage() ERROR: " + ex.toString());
			return false;
		}
	}

	/**
	 * Send a Tweet/Update User Status. 
	 * This can be an update or a reply.
	 * 
	 * @param context - Application Context.
	 * @param userID - The ID of the user we are sending the message to.
	 * @param message - The message we want to send.
	 * @param isReply - Boolean to state whether this is a reply to a user or not.
	 * 
	 * @return boolean - Returns true if the message was sent successfully.
	 */
	public static boolean sendTweet(Context context, long userID, String message, boolean isReply){
		_debug = Log.getDebug();
		if (_debug) Log.v("TwitterCommon.sendTweet()");
		try{
			_context = context;
			new sendTweetAsyncTask().execute(String.valueOf(userID), message, String.valueOf(isReply));
			return true;
		}catch(Exception ex){
			if (_debug) Log.v("TwitterCommon.sendTweet() ERROR: " + ex.toString());
			return false;
		}
	}
	
	/**
	 * Start the Twitter recurring alarm.
	 *  
	 * @param context - The application context.
	 * @param alarmStartTime - The time to start the alarm.
	 */
	public static void startTwitterAlarmManager(Context context, long alarmStartTime){
		_debug = Log.getDebug();
		if (_debug) Log.v("TwitterCommon.startTwitterAlarmManager()");
		try{
			SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
			AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
			Intent intent = new Intent(context, TwitterAlarmReceiver.class);
			PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, 0);
			long pollingFrequency = Long.parseLong(preferences.getString(Constants.TWITTER_POLLING_FREQUENCY_KEY, Constants.TWITTER_POLLING_FREQUENCY_DEFAULT)) * 60 * 1000;
			alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, alarmStartTime, pollingFrequency, pendingIntent);
		}catch(Exception ex){
			Log.e("TwitterCommon.startTwitterAlarmManager() ERROR: " + ex.toString());
		}
	}
	
	/**
	 * Start a single Twitter alarm.
	 *  
	 * @param context - The application context.
	 * @param alarmStartTime - The time to start the alarm.
	 */
	public static void setTwitterAlarm(Context context, long alarmStartTime){
		_debug = Log.getDebug();
		if (_debug) Log.v("TwitterCommon.setTwitterAlarm()");
		try{
			String intentActionText = "apps.droidnotify.alarm/TwitterAlarmReceiverAlarm/" + String.valueOf(System.currentTimeMillis());
			Common.startAlarm(context, TwitterAlarmReceiver.class, null, intentActionText, alarmStartTime);
		}catch(Exception ex){
			Log.e("TwitterCommon.setTwitterAlarm() ERROR: " + ex.toString());
		}
	}
	
	/**
	 * Cancel the Twitter recurring alarm.
	 *  
	 * @param context - The application context.
	 */
	public static void cancelTwitterAlarmManager(Context context){
		_debug = Log.getDebug();
		if (_debug) Log.v("TwitterCommon.cancelTwitterAlarmManager()");
		try{
			AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
			Intent intent = new Intent(context, TwitterAlarmReceiver.class);
			PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, 0);
			alarmManager.cancel(pendingIntent);
		}catch(Exception ex){
			Log.e("TwitterCommon.cancelTwitterAlarmManager() ERROR: " + ex.toString());
		}
	}
	
	/**
	 * Determine if the user has authenticated their Twitter account. 
	 * 
	 * @param context - The application context.
	 *
	 * @return boolean - Return true if the user preferences have Twitter authentication data.
	 */
	public static boolean isTwitterAuthenticated(Context context) {
		_debug = Log.getDebug();
		if (_debug) Log.v("TwitterCommon.isTwitterAuthenticated()");	
		try {
			SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
			String oauthToken = preferences.getString(Constants.TWITTER_OAUTH_TOKEN, null);
			String oauthTokenSecret = preferences.getString(Constants.TWITTER_OAUTH_TOKEN_SECRET, null);
			if(oauthToken == null || oauthTokenSecret == null){
				if (_debug) Log.v("TwitterCommon.isTwitterAuthenticated() Twitter stored authentication details are null. Exiting...");
				return false;
			}
			return true;
		} catch (Exception ex) {
			Log.e("TwitterCommon.isTwitterAuthenticated() ERROR: " + ex.toString());
			return false;
		}
	}
	
	/**
	 * Initialize and return a Twitter object.
	 * 
	 * @param context - The application context.
	 * 
	 * @return Twitter - The initialized Twitter object or null.
	 */
	public static Twitter getTwitter(Context context){
		_debug = Log.getDebug();
		if (_debug) Log.v("TwitterCommon.getTwitter()");
		try{
			SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
			String oauthToken = preferences.getString(Constants.TWITTER_OAUTH_TOKEN, null);
			String oauthTokenSecret = preferences.getString(Constants.TWITTER_OAUTH_TOKEN_SECRET, null);
			if(oauthToken == null || oauthTokenSecret == null){
				if (_debug) Log.v("TwitterCommon.getTwitter() Oauth values are null. Exiting...");
				return null;
			}
			ConfigurationBuilder configurationBuilder = new ConfigurationBuilder(); 
			configurationBuilder.setOAuthConsumerKey(Constants.TWITTER_CONSUMER_KEY); 
			configurationBuilder.setOAuthConsumerSecret(Constants.TWITTER_CONSUMER_SECRET); 
			Configuration configuration =  configurationBuilder.build();  
			AccessToken accessToken = new AccessToken(oauthToken, oauthTokenSecret);
			TwitterFactory twitterFactory = new TwitterFactory(configuration);
			Twitter twitter = twitterFactory.getInstance(accessToken);
			return twitter;
		}catch(Exception ex){
			Log.e("TwitterCommon.getTwitter() ERROR: " + ex.toString());
			return null;
		}	
	}
	
	/**
	 * Determine if the user has selected the Mobile Webpage as the client or not.
	 * 
	 * @param context - The application context.
	 * 
	 * @return boolean - return true if user has selected the Mobile Webpage as the client.
	 */
	public static boolean isUsingClientWeb(Context context){
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
		String packageName = preferences.getString(Constants.TWITTER_PREFERRED_CLIENT_KEY, Constants.TWITTER_PREFERRED_CLIENT_DEFAULT);
		if(packageName.startsWith("http://")){
			return true;
		}else{
			return false;
		}		
	}

	//================================================================================
	// Private Methods
	//================================================================================
	
	/**
	 * Send a Twitter Direct Message in the background.
	 * 
	 * @author Camille S�vigny
	 */
	private static class sendTwitterDirectMessageAsyncTask extends AsyncTask<String, Void, Boolean> {
	    
	    /**
	     * Do this work in the background.
	     * 
	     * @param params - The User ID & Message to send.
	     */
	    protected Boolean doInBackground(String... params) {
			if (_debug) Log.v("TwitterCommon.sendTwitterDirectMessageAsyncTask.doInBackground()");
			try{
				Twitter twitter = getTwitter(_context);
				if(twitter == null){
					if (_debug) Log.v("TwitterCommon.sendTwitterDirectMessageAsyncTask.doInBackground() Twitter object is null. Exiting...");
					return false;
				}
				twitter.sendDirectMessage(Long.parseLong(params[0]), params[1]);
				return true;
			}catch(Exception ex){
				Log.e("TwitterCommon.sendTwitterDirectMessageAsyncTask.doInBackground() ERROR: " + ex.toString());
				return false;
			}
	    }
	    
	    /**
	     * Display a message if the Twitter Direct Message encountered an error.
	     * 
	     * @param result - Boolean indicating success.
	     */
	    protected void onPostExecute(Boolean result) {
			if (_debug) Log.v("TwitterCommon.sendTwitterDirectMessageAsyncTask.onPostExecute() RESULT: " + result);
			if(result){
				//Do Nothing
			}else{
				Toast.makeText(_context, _context.getString(R.string.twitter_send_direct_mesage_error), Toast.LENGTH_LONG).show();
			}
	    }
	    
	}
	
	/**
	 * Send a Tweet in the background.
	 * 
	 * @author Camille S�vigny
	 */
	private static class sendTweetAsyncTask extends AsyncTask<String, Void, Boolean> {
	    
	    /**
	     * Do this work in the background.
	     * 
	     * @param params - The User ID & Message to send.
	     */
	    protected Boolean doInBackground(String... params) {
			if (_debug) Log.v("TwitterCommon.sendTweetAsyncTask.doInBackground()");
			try{
				Twitter twitter = getTwitter(_context);
				if(twitter == null){
					if (_debug) Log.v("TwitterCommon.sendTweetAsyncTask.doInBackground() Twitter object is null. Exiting...");
					return false;
				}
				if(Boolean.parseBoolean(params[2])){
					String twitterUserScreenName = twitter.showUser(Long.parseLong(params[0])).getScreenName();
					if (_debug) Log.v("TwitterCommon.sendTweetAsyncTask.doInBackground() Message: " + "@" + twitterUserScreenName + " " + params[1]);
					twitter.updateStatus("@" + twitterUserScreenName + " " + params[1]);
				}else{
					if (_debug) Log.v("TwitterCommon.sendTweetAsyncTask.doInBackground() Mesage: " + params[1]);
					twitter.updateStatus(params[1]);
				}
				return true;
			}catch(Exception ex){
				Log.e("TwitterCommon.sendTweetAsyncTask.doInBackground() ERROR: " + ex.toString());
				return false;
			}
	    }
	    
	    /**
	     * Display a message if the Twitter Direct Message encountered an error.
	     * 
	     * @param result - Boolean indicating success.
	     */
	    protected void onPostExecute(Boolean result) {
			if (_debug) Log.v("TwitterCommon.sendTweetAsyncTask.onPostExecute() RESULT: " + result);
			if(result){
				//Do Nothing
			}else{
				Toast.makeText(_context, _context.getString(R.string.twitter_send_tweet_error), Toast.LENGTH_LONG).show();
			}
	    }
	    
	}
	
	/**
	 * Delete a Twitter Direct Message in the background.
	 * 
	 * @author Camille S�vigny
	 */
	private static class deleteTwitterDirectMessageAsyncTask extends AsyncTask<Long, Void, Boolean> {
	    
	    /**
	     * Do this work in the background.
	     * 
	     * @param params - The Message ID to delete.
	     */
	    protected Boolean doInBackground(Long... params) {
			if (_debug) Log.v("TwitterCommon.deleteTwitterDirectMessageAsyncTask.doInBackground()");
			try{
				Twitter twitter = getTwitter(_context);
				if(twitter == null){
					if (_debug) Log.v("TwitterCommon.deleteTwitterDirectMessageAsyncTask.doInBackground() Twitter object is null. Exiting...");
					return false;
				}
				long messageID = params[0];
				twitter.destroyDirectMessage(messageID);
				return true;
			}catch(Exception ex){
				Log.e("TwitterCommon.deleteTwitterDirectMessageAsyncTask.doInBackground() ERROR: " + ex.toString());
				return false;
			}
	    }
	    
	    /**
	     * Display a message if the deleting of the Twitter Direct Message encountered an error.
	     * 
	     * @param result - Boolean indicating success.
	     */
	    protected void onPostExecute(Boolean result) {
			if (_debug) Log.v("TwitterCommon.deleteTwitterDirectMessageAsyncTask.onPostExecute() RESULT: " + result);
			if(result){
				//Do Nothing
			}else{
				//Toast.makeText(_context, _context.getString(R.string.twitter_delete_direct_message_error), Toast.LENGTH_LONG).show();
			}
	    }
	    
	}
	
}