// ----------------------------------------------------------------------------
// Copyright 2007-2014, GeoTelematic Solutions, Inc.
// All rights reserved
// ----------------------------------------------------------------------------
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
// 
// http://www.apache.org/licenses/LICENSE-2.0
// 
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//
// ----------------------------------------------------------------------------
// Change History:
//  2007/03/18  Martin D. Flynn
//     -Initial release
//  2007/06/13  Martin D. Flynn
//     -Moved to "org.opengts.db.tables"
//  2007/09/16  Martin D. Flynn
//     -Integrated DBSelect
//  2010/04/25  Martin D. Flynn
//     -Fix trimming of 'inactive' Devices
//  2012/04/03  Martin D. Flynn
//     -Added command-line option to count/delete old events by group
//  2013/03/01  Martin D. Flynn
//     -Added delete between devices for 'deleteOldEvents' and 'countOldEvents'
//  2014/03/03  Martin D. Flynn
//     -Made some adjustments to display warnings when unable to count InnoDB events.
//  2014/09/16  Martin D. Flynn
//     -Added support for "notifyEmail" and "allowNotify" (see "RuleNotification")
// ----------------------------------------------------------------------------
package org.opengts.db.tables;

import java.lang.*;
import java.util.*;
import java.math.*;
import java.io.*;
import java.sql.*;

import org.opengts.util.*;
import org.opengts.util.JSON.JSONParsingException;
import org.opengts.dbtools.*;
import org.opengts.db.*;
import org.opengts.db.tables.*;

public class DeviceGroup
    extends GroupRecord<DeviceGroup>
{

    // ------------------------------------------------------------------------

    /* reserved name for the group containing "ALL" authorized devices */
    public static final String DEVICE_GROUP_ALL             = "all";
    public static final String DEVICE_GROUP_NONE            = "none";

    public static final OrderedSet<String> GROUP_LIST_ALL   = new OrderedSet<String>(new String[] { DEVICE_GROUP_ALL });
    public static final OrderedSet<String> GROUP_LIST_EMPTY = new OrderedSet<String>();

    // ------------------------------------------------------------------------

    /* optional columns */
    public static final String  OPTCOLS_WorkOrderInfo       = "startupInit.DeviceGroup.WorkOrderInfo";
    public static final String  OPTCOLS_RuleNotification    = "startupInit.DeviceGroup.RuleNotification";

    // ------------------------------------------------------------------------

    /* "DeviceGroup" title (ie. "Group", "Fleet", etc) */
    public static String[] GetTitles(Locale loc) 
    {
        I18N i18n = I18N.getI18N(DeviceGroup.class, loc);
        return new String[] {
            i18n.getString("DeviceGroup.title.singular", "Group"),
            i18n.getString("DeviceGroup.title.plural"  , "Groups"),
        };
    }

    /* Group "All" description */
    public static String GetDeviceGroupAllTitle(Account account, Locale loc)
    {
        I18N i18n = I18N.getI18N(DeviceGroup.class, loc);
        String devTitles[] = (account != null)?
            account.getDeviceTitles(loc) :
            Device.GetTitles(loc);
        return i18n.getString("DeviceGroup.allDescription", "All {1}", devTitles);
    }
        
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // SQL table definition below

    /* table name */
    public static final String _TABLE_NAME              = "DeviceGroup";
    public static String TABLE_NAME() { return DBProvider._translateTableName(_TABLE_NAME); }

    /* field definition */
    public static final String FLD_groupType            = "groupType";
    private static DBField FieldInfo[] = {
        // Group fields
        newField_accountID(true),
        newField_groupID(true),
      //new DBField(FLD_groupType   , Integer.TYPE  , DBField.TYPE_UINT16      , I18N.getString(DeviceGroup.class,"DeviceGroup.fld.groupType"  , "Device Group Type"         ), "edit=2"),
        // Home GeozoneID?
        // Group PushpinID?
        // Common fields
        newField_displayName(),
        newField_description(),
        newField_notes(),
        newField_lastUpdateTime(),
        newField_lastUpdateAccount(true),
        newField_lastUpdateUser(true),
        newField_creationTime(),
    };

    // DeviceGroup notify fields
    // startupInit.DeviceGroup.RuleNotification=true
    public static final String FLD_allowNotify          = "allowNotify";           // allow notification
    public static final String FLD_notifyEmail          = "notifyEmail";           // notification email address
    public static final DBField RuleNotification[]         = {
        new DBField(FLD_allowNotify , Boolean.TYPE  , DBField.TYPE_BOOLEAN     , I18N.getString(DeviceGroup.class,"DeviceGroup.fld.allowNotify", "Allow Notification"        ), "edit=2"),
        new DBField(FLD_notifyEmail , String.class  , DBField.TYPE_EMAIL_LIST(), I18N.getString(DeviceGroup.class,"DeviceGroup.fld.notifyEmail", "Notification EMail Address"), "edit=2"),
    };

    // WorkOrder fields
    // startupInit.DeviceGroup.WorkOrderInfo=true
    public static final String FLD_workOrderID          = "workOrderID";           // WorkOrder ID (may not be used)
    public static final DBField WorkOrderInfo[]         = {
        new DBField(FLD_workOrderID , String.class , DBField.TYPE_STRING(512) , "Work Order IDs" , "edit=2"),
    };

    /* key class */
    public static class Key
        extends GroupKey<DeviceGroup>
    {
        public Key() {
            super();
        }
        public Key(String accountId, String groupId) {
            super.setKeyValue(FLD_accountID, ((accountId != null)? accountId.toLowerCase() : ""));
            super.setKeyValue(FLD_groupID  , ((groupId   != null)? groupId  .toLowerCase() : ""));
        }
        public DBFactory<DeviceGroup> getFactory() {
            return DeviceGroup.getFactory();
        }
    }
    
    /* factory constructor */
    private static DBFactory<DeviceGroup> factory = null;
    public static DBFactory<DeviceGroup> getFactory()
    {
        if (factory == null) {
            factory = DBFactory.createDBFactory(
                DeviceGroup.TABLE_NAME(), 
                DeviceGroup.FieldInfo, 
                DBFactory.KeyType.PRIMARY,
                DeviceGroup.class, 
                DeviceGroup.Key.class,
                true/*editable*/, true/*viewable*/);
            factory.addParentTable(Account.TABLE_NAME());
        }
        return factory;
    }

    /* Bean instance */
    public DeviceGroup()
    {
        super();
    }

    /* database record */
    public DeviceGroup(DeviceGroup.Key key)
    {
        super(key);
    }
    
    // ------------------------------------------------------------------------

    /* table description */
    public static String getTableDescription(Locale loc)
    {
        I18N i18n = I18N.getI18N(DeviceGroup.class, loc);
        return i18n.getString("DeviceGroup.description", 
            "This table defines " +
            "Account specific Device Groups (normal and universal)."
            );
    }

    // SQL table definition above
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // Bean access fields below

    /* return the group type */
    public int getGroupType()
    {
        Integer v = (Integer)this.getFieldValue(FLD_groupType);
        return (v != null)? v.intValue() : 0;
    }

    /* set the group type */
    public void setGroupType(int v)
    {
        this.setFieldValue(FLD_groupType, v);
    }

    // ------------------------------------------------------------------------

    private static final boolean CHECK_ACCOUNT_ALLOWNOTIFY = false;

    /**
    *** Returns true if this DeviceGroup record supports the "allowNotify" field
    *** @return True if this DeviceGroup record supports the "allowNotify" field
    **/
    public static boolean supportsNotification()
    {
        // -- RuleNotification
        return Device.getFactory().hasField(FLD_allowNotify);
      //return Device.getFactory().hasField(FLD_notifyEmail);
    }

    /**
    *** Returns true if this DeviceGroup allows notifications
    *** @return True if this DeviceGroup allows notifications
    **/
    public boolean getAllowNotify()
    {
        // -- RuleNotification
        Boolean v = (Boolean)this.getOptionalFieldValue(FLD_allowNotify);
        return (v != null)? v.booleanValue() : false;
    }

    /**
    *** Sets the "Allow Notification" state for this DeviceGroup
    *** @param v The "Allow Notification" state for this DeviceGroup
    **/
    public void setAllowNotify(boolean v)
    {
        // -- RuleNotification
        this.setOptionalFieldValue(FLD_allowNotify, v);
    }

    /**
    *** Returns true if this DeviceGroup allows notifications
    *** @param checkAccount True to also check Account
    *** @return True if this DeviceGroup allows notifications
    **/
    public boolean getAllowNotify(boolean checkAccount)
    {
        // -- RuleNotification

        /* without regard to account setting? */
        if (!checkAccount) {
            // -- explicit, do not check account
            return this.getAllowNotify();
        } else
        if (!RTConfig.getBoolean(DBConfig.PROP_DeviceGroup_checkAccountAllowNotify,CHECK_ACCOUNT_ALLOWNOTIFY)) {
            // -- property says to not check account
            return this.getAllowNotify();
        }

        /* check account */
        Account acct = this.getAccount();
        return (acct != null)? acct.getAllowNotify() : false;

    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the Notification Email address
    *** @return The Notification Email Address
    **/
    public String getNotifyEmail()
    {
        // -- RuleNotification
        String v = (String)this.getOptionalFieldValue(FLD_notifyEmail);
        return StringTools.trim(v);
    }

    /**
    *** Sets the Notification Email address
    *** @param v The Notification Email Address
    **/
    public void setNotifyEmail(String v)
    {
        // -- RuleNotification
        this.setOptionalFieldValue(FLD_notifyEmail, StringTools.trim(v));
    }

    // ------------------------------------------------------------------------

    /* get WorkOrder ID */
    public String getWorkOrderID()
    {
        String v = (String)this.getOptionalFieldValue(FLD_workOrderID);
        return StringTools.trim(v);
    }

    /* set WorkOrder ID */
    public void setWorkOrderID(String v)
    {
        this.setOptionalFieldValue(FLD_workOrderID, StringTools.trim(v));
    }

    // ------------------------------------------------------------------------

    public void setMapLegend(String legend)
    {
        //
    }

    public String getMapLegend()
    {
        return "";
    }

    // Bean access fields above
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
 
    /* return string representation of instance */
    public String toString()
    {
        return this.getAccountID() + "/" + this.getGroupID();
    }
    
    // ------------------------------------------------------------------------

    /* overridden to set default values */
    public void setCreationDefaultValues()
    {
        this.setDescription("");
        //super.setRuntimeDefaultValues();
    }
    
    // ------------------------------------------------------------------------

    /* return true if the specified device exists in the normal group of the account*/
    public boolean isDeviceInDeviceGroup(String deviceID)
    {
        if (deviceID != null) {
            String accountID = this.getAccountID();
            String groupID   = this.getGroupID();
            return DeviceGroup.isDeviceInDeviceGroup(accountID, deviceID, groupID);
        } else {
            return false;
        }
    }

    /* return true if the specified device exists in the universal group of the account*/
    public boolean isDeviceInDeviceGroup(String devaccID, String deviceID)
    {
        if( (deviceID != null) || (devaccID != null) ) {
            String accountID = this.getAccountID();
            String groupID   = this.getGroupID();
            return DeviceGroup.isDeviceInDeviceGroup(accountID, groupID, devaccID, deviceID);
        } else {
            return false;
        }
    }

    /* return true if the specified device exists in normal or universal group of the account */
    public boolean isDeviceInDeviceGroup(Device device)
    {
        if (device != null) {
        	String devaccID = device.getAccountID();
        	String deviceID = device.getDeviceID();
        	String accountID = this.getAccountID();
        	String groupID   = this.getGroupID();
            return DeviceGroup.isDeviceInDeviceGroup(accountID, groupID, devaccID, deviceID);
        } else {
            return false;
        }
    }

    /* return true if the specified device exists in the normal group of the account */
    public static boolean isDeviceInDeviceGroup(String acctID, String groupID, String deviceID)
    {
        if ((acctID == null) || (groupID == null) || (deviceID == null)) {
            return false;
        } else
        if (groupID.equalsIgnoreCase(DeviceGroup.DEVICE_GROUP_ALL)) {
            return true;
        } else {
            try {
                return DeviceGroup.exists(acctID, groupID, deviceID);
            } catch (DBException dbe) {
                return false;
            }
        }
    }
    
    /* return true if the specified device exists in the universal group of the account */
    public static boolean isDeviceInDeviceGroup(String acctID, String groupID, String devaccID, String deviceID)
    {
        if ((acctID == null) || (groupID == null) || (devaccID == null) || (deviceID == null)) {
            return false;
        } else
        if (groupID.equalsIgnoreCase(DeviceGroup.DEVICE_GROUP_ALL)) {
            return true;
        } else {
            try {
                return DeviceGroup.exists(acctID, groupID, devaccID, deviceID);
            } catch (DBException dbe) {
                return false;
            }
        }
    }
    
    /* add device to this normal group */
    public void addDeviceToDeviceGroup(String deviceID)
        throws DBException
    {
        if (deviceID != null) {
            String accountID = this.getAccountID();
            String groupID   = this.getGroupID();
            DeviceGroup.addDeviceToDeviceGroup(accountID, groupID, deviceID);
        }
    }

    /* add device to this universal group */
    public void addDeviceToDeviceGroup(String devaccID, String deviceID)
        throws DBException
    {
    	if (deviceID != null) {
            String accountID = this.getAccountID();
            String groupID   = this.getGroupID();
        	if (devaccID == null) { 
        		devaccID = accountID; 
        	}
            DeviceGroup.addDeviceToDeviceGroup(accountID, groupID, devaccID, deviceID);
        }
    }

    /* add device to this universal group */
    public void addDeviceToDeviceGroup(Device device)
        throws DBException
    {
        if (device != null) {
        	String devaccID = device.getAccountID();
        	String deviceID = device.getDeviceID();
        	String accountID = this.getAccountID();
        	String groupID   = this.getGroupID();       	
        	DeviceGroup.addDeviceToDeviceGroup(accountID, groupID, devaccID, deviceID);
        }
    }

    /* add device to normal device group */
    public static void addDeviceToDeviceGroup(String accountID, String groupID, String deviceID)
        throws DBException
    {

        /* device exists? */
        if (!Device.exists(accountID,deviceID)) {
            throw new DBException("Device does not exist: " + accountID + "/" + deviceID);
        }

        /* group exists? */
        if (!DeviceGroup.exists(accountID,groupID)) {
            throw new DBException("DeviceGroup does not exist: " + accountID + "/" + groupID);
        }

        /* create/save record */
        DeviceList.Key devListKey = new DeviceList.Key(accountID, groupID, deviceID);
        if (devListKey.exists()) {
            // already exists
        } else {
            DeviceList devListEntry = devListKey.getDBRecord();
            // no other data fields/columns required
            devListEntry.save();
        }

    }

    /* add device to universal device group */
    public static void addDeviceToDeviceGroup(String accountID, String groupID, String devaccID, String deviceID)
        throws DBException
    {

        /* device exists? */
        if (!Device.exists(devaccID,deviceID)) {
            throw new DBException("Device does not exist: " + devaccID + "/" + deviceID);
        }

        /* group exists? */
        if (!DeviceGroup.exists(accountID,groupID)) {
            throw new DBException("DeviceGroup does not exist: " + accountID + "/" + groupID);
        }

        /* create/save record */
        DeviceUList.Key devUListKey = new DeviceUList.Key(accountID, groupID, devaccID, deviceID);
        if (devUListKey.exists()) {
            // already exists
        } else {
            DeviceUList devUListEntry = devUListKey.getDBRecord();
            // no other data fields/columns required
            devUListEntry.save();
        }

    }

    // ------------------------------------------------------------------------
    
    /* remove device from this normal group */
    public void removeDeviceFromDeviceGroup(String deviceID)
        throws DBException
    {
        if (deviceID != null) {
            String accountID = this.getAccountID();
            String groupID   = this.getGroupID();
            DeviceGroup.removeDeviceFromDeviceGroup(accountID, groupID, deviceID);
        }
    }
    
    /* remove device from this universal group */
    public void removeDeviceFromDeviceGroup(String devaccID, String deviceID)
        throws DBException
    {
        if (deviceID != null) {
            String accountID = this.getAccountID();
            String groupID   = this.getGroupID();
            DeviceGroup.removeDeviceFromDeviceGroup(accountID, groupID, devaccID, deviceID);
        }
    }
    
    /* remove device from this normal/universal group */
    public void removeDeviceFromDeviceGroup(Device device)
        throws DBException
    {
        if (device != null) {
        	String devaccID = device.getAccountID();
        	String deviceID = device.getDeviceID();
        	String accountID = this.getAccountID();
        	String groupID   = this.getGroupID();       	        	
        	DeviceGroup.removeDeviceFromDeviceGroup(accountID, groupID, devaccID, deviceID);
        }
    }

    /* remove device from normal device group */
    public static void removeDeviceFromDeviceGroup(String accountID, String groupID, String deviceID)
        throws DBException
    {

        /* device exists? */
        if (!Device.exists(accountID,deviceID)) {
            throw new DBException("Device does not exist: " + accountID + "/" + deviceID);
        }

        /* delete record */
        DeviceList.Key devListKey = new DeviceList.Key(accountID, groupID, deviceID);
        devListKey.delete(false); // no dependencies
        
    }

    /* remove device from universal device group */
    public static void removeDeviceFromDeviceGroup(String accountID, String groupID, String devaccID, String deviceID)
        throws DBException
    {

        /* device exists? */
        if (!Device.exists(devaccID,deviceID)) {
            throw new DBException("Device does not exist: " + accountID + "/" + deviceID);
        }

        /* delete record */
        DeviceUList.Key devUListKey = new DeviceUList.Key(accountID, groupID, devaccID, deviceID);
        devUListKey.delete(false); // no dependencies
        
    }

    /* removes all members from the universal group */
    public void clear()
    		throws DBException
    	{
	    	OrderedSet<String[]> devList = getAllDevicesForGroup(
	    	        this.getAccountID(), this.getGroupID(), (User)null, true, -1L);
	    	Iterator<String[]> dli = devList.iterator();
	    	while(dli.hasNext()) {
	    		String[] dev = dli.next();
	    		this.removeDeviceFromDeviceGroup(dev[0],dev[1]);
	    	}    	
    	}
    
    /* removes all members from the normal group */
    public void clearDevices()
    		throws DBException
    	{
	    	String accountID = this.getAccountID();
	    	String groupID = this.getGroupID();
	    	
    		OrderedSet<String> devList = getDeviceIDsForGroup(
    				accountID, groupID, (User)null, true, -1L);
    	        
	    	Iterator<String> dli = devList.iterator();
	    	while(dli.hasNext()) {
	    		String dev = dli.next();
	    		removeDeviceFromDeviceGroup(accountID, groupID, dev);
	    	}    	
    	}
    
    /* replaces current members of the normal group with the new ones */
    /* devList is a list of Strings deviceId  */
    /* ATTENTION, this function removes ALL members from the group if the input does not contain usable data */
    public void setMembersID(OrderedSet<String[]> devList)
            throws DBException
        {
            if (devList != null) {
            	clearDevices();
    	    	Iterator<String[]> dli = devList.iterator();
    	    	while(dli.hasNext()) {
    	    		String[] dev = dli.next();
					this.addDeviceToDeviceGroup(dev[1]);
    	    	}
            }
            return;
        }
    
    /* replaces current members of the universal group with the new ones */
    /* devList is a list of String arrays { accountId, deviceId } */
    /* ATTENTION, this function removes ALL members from the group if the input does not contain usable data */
    public void setMembers(OrderedSet<String[]> devList)
            throws DBException
        {
            if (devList != null) {
            	clear();
    	    	Iterator<String[]> dli = devList.iterator();
    	    	while(dli.hasNext()) {
    	    		String[] dev = dli.next();
					this.addDeviceToDeviceGroup(dev[0],dev[1]);
    	    	}
            }
            return;
        }
    
   // ------------------------------------------------------------------------
   // ------------------------------------------------------------------------

    /** 
    *** Returns count of events in this DeviceGroup.<br>
    *** Note: Will return -1 if EventData table is InnoDB.
    **/
    public long countOldEvents(long oldTimeSec, boolean log)
        throws DBException
    {
        Account account = this.getAccount();
        String  groupID = this.getGroupID();
        return DeviceGroup.countOldEvents(account, groupID, oldTimeSec, log); // -1 for InnoDB?
    }

    /** 
    *** Returns count of events in this DeviceGroup.<br>
    *** Note: Will return -1 if EventData table is InnoDB.
    **/
    // ******************** TODO: universal groups support
    public static long countOldEvents(Account account, String groupID, long oldTimeSec, boolean log)
        throws DBException
    {

        /* Account */
        if (account == null) {
            if (log) {
                Print.sysPrintln("  Account is null");
            }
            return 0L; 
        }
        String acctID = account.getAccountID();

        /* logging */
        if (log) {
            StringBuffer sb = new StringBuffer();
            sb.append("Counting old events for group ").append(acctID+"/"+groupID);
            sb.append(" prior to ").append((new DateTime(oldTimeSec)).toString());
            Print.sysPrintln(sb.toString());
        }

        /* get device list */
        OrderedSet<String> devList = null;
        devList = DeviceGroup.getDeviceIDsForGroup(acctID, groupID, null, true/*inclInactv*/, -1L);;
        int devCount = ListTools.size(devList);
        if (devCount <= 0) {
            // may also mean that account/group does not exist
            if (log) {
                Print.sysPrintln("  No Devices Found: "+acctID+"/"+groupID);
            }
            return 0L;
        }

        /* count events */
        boolean isInnoDB = false;
        long totalCount = 0L;
        for (String devID : devList) {

            /* count old events */
            long startMS = DateTime.getCurrentTimeMillis();
            long count   = EventData.getRecordCount(acctID, devID, -1L, oldTimeSec); // -1 for InnoDB
            long endMS   = DateTime.getCurrentTimeMillis();
            long deltaMS = endMS - startMS; // amount of time it took to count the events
            if (count > 0L) {
                totalCount += count;
            } else
            if (count < 0L) {
                isInnoDB = true;
            }
            devCount--;

            /* logging */
            if (log) {
                if (count >= 0L) {
                    StringBuffer sb = new StringBuffer();
                    sb.append("  Device: ").append(StringTools.leftAlign(acctID+"/"+devID,25));
                    sb.append(" - counted ").append(StringTools.rightAlign(String.valueOf(count),5));
                    sb.append(" [").append(deltaMS).append("]");
                    Print.sysPrintln(sb.toString());
                } else
                if (count < 0L) { // InnoDB
                    StringBuffer sb = new StringBuffer();
                    sb.append("  Device: ").append(StringTools.leftAlign(acctID+"/"+devID,25));
                    sb.append(" - unable to count (InnoDB?)");
                    sb.append(" [").append(deltaMS).append("]");
                    Print.sysPrintln(sb.toString());
                }
            } else

            /* short sleep before deleting next set of events */
            if (devCount > 0) {
                long sleepMS = 500L;
                try { Thread.sleep(sleepMS); } catch (Throwable th) {/*ignore*/}
            }

        }

        /* logging */
        if (log) {
            if (totalCount > 0L) {
                StringBuffer sb = new StringBuffer();
                sb.append("  Total : ").append(StringTools.leftAlign(acctID+"/"+groupID,25));
                sb.append(" - counted ").append(StringTools.rightAlign(String.valueOf(totalCount),5));
                Print.sysPrintln(sb.toString());
            } else
            if (isInnoDB) {
                Print.sysPrintln("  Unable to determine event counts (InnoDB?): "+acctID+"/"+groupID);
            } else {
                Print.sysPrintln("  No Devices with counts greater than zero: "+acctID+"/"+groupID);
            }
        }

        /* return total count */
        if ((totalCount <= 0L) && isInnoDB) {
            return -1L; // InnoDB
        } else {
            return totalCount;
        }

    }
    
    // ------------------------------------------------------------------------

    /**
    *** Delete events prior to the specified time.<br>
    *** Note: Will return -1 if EventData table is InnoDB.  
    ***       Old events will still be deleted, however it will still go through the
    ***       motions of attempting to delete events, event if the range is empty.
    *** @param oldTimeSec  The timestamp before which events will be deleted
    *** @param log         True to display log
    *** @return The number of events deleted
    **/
    // ******************** TODO: universal groups support
    public long deleteOldEvents(
        long oldTimeSec, 
        boolean log)
        throws DBException
    {
        Account account = this.getAccount();
        String  groupID = this.getGroupID();
        return DeviceGroup.deleteOldEvents(account, groupID, oldTimeSec, log); // -1 for InnoDB
    }

    /**
    *** Delete events prior to the specified time.<br>
    *** Note: Will return -1 if EventData table is InnoDB.  
    ***       Old events will still be deleted, however it will still go through the
    ***       motions of attempting to delete events, event if the range is empty.
    *** @param account     The account
    *** @param groupID     The group-id
    *** @param oldTimeSec  The timestamp before which events will be deleted
    *** @param log         True to display log
    *** @return The number of events deleted
    **/
    // ******************** TODO: universal groups support    
    public static long deleteOldEvents(
        Account account, String groupID, 
        long oldTimeSec, 
        boolean log)
        throws DBException
    {

        /* Account */
        if (account == null) {
            if (log) {
                Print.sysPrintln("  Account is null");
            }
            return 0L; 
        }
        String acctID = account.getAccountID();

        /* compare oldTimeSec against retained age */
        boolean usingRetainedDate = false;
        long delOldTimeSec = account.adjustRetainedEventTime(oldTimeSec);
        if (delOldTimeSec != oldTimeSec) {
            oldTimeSec = delOldTimeSec;
            usingRetainedDate = true;
        }

        /* "oldTimeSec" must not be less than "1" */
        if (oldTimeSec < 1L) {
            oldTimeSec = 1L;
        }

        /* logging */
        if (log) {
            StringBuffer sb = new StringBuffer();
            sb.append("Deleting old events for group ").append(acctID+"/"+groupID);
            sb.append(" prior to ").append((new DateTime(oldTimeSec)).toString());
            if (usingRetainedDate) {
                sb.append(" (retained-date)");
            }
            Print.sysPrintln(sb.toString());
        }

        /* get device list */
        OrderedSet<String> devList = null;
        devList = DeviceGroup.getDeviceIDsForGroup(acctID, groupID, null, true/*inclInactv*/, -1L);;
        int devCount = ListTools.size(devList);
        if (devCount <= 0) {
            // may also mean that account/group does not exist
            if (log) {
                Print.sysPrintln("  No Devices Found: "+acctID+"/"+groupID);
            }
            return 0L; 
        }

        /* delete events */
        StringBuffer msg = new StringBuffer();
        boolean isInnoDB = false;
        long totalCount = 0L;
        for (String devID : devList) {
            msg.setLength(0);

            /* get Device */
            Device device = null;
            try {
                device = Device.getDevice(account, devID); // null if non-existent
                if (device == null) {
                    // -- (unlikely) skip this device
                    continue;
                }
            } catch (DBException dbe) {
                Print.logException("Unable to read device: "+acctID+"/"+devID, dbe);
                return totalCount;
            }

            /* delete old events */
            long startMS = DateTime.getCurrentTimeMillis();
            long count   = EventData.deleteOldEvents(device, oldTimeSec, msg); // -1 for InnoDB
            long endMS   = DateTime.getCurrentTimeMillis();
            long deltaMS = endMS - startMS; // amount of time it took to delete the events
            if (count > 0L) {
                totalCount += count;
            } else
            if (count < 0L) {
                isInnoDB = true;
            }
            devCount--;

            /* logging */
            if (log) {
                if (count >= 0L) {
                    StringBuffer sb = new StringBuffer();
                    sb.append("  Device: ").append(StringTools.leftAlign(acctID+"/"+devID,25));
                    sb.append(" - deleted ").append(StringTools.rightAlign(String.valueOf(count),5));
                    sb.append(" [").append(deltaMS).append("ms]");
                    if (msg.length() > 0) {
                        sb.append("  ").append(msg);
                    }
                    Print.sysPrintln(sb.toString());
                } else
                if (count < 0L) {
                    StringBuffer sb = new StringBuffer();
                    sb.append("  Device: ").append(StringTools.leftAlign(acctID+"/"+devID,25));
                    sb.append(" - deleted ").append(StringTools.rightAlign("?",5)).append(" (InnoDB?)");
                    sb.append(" [").append(deltaMS).append("ms]");
                    if (msg.length() > 0) {
                        sb.append("  ").append(msg);
                    }
                    Print.sysPrintln(sb.toString());
                }
            }

            /* short sleep before deleting next set of events */
            // the delete appears to be rather CPU intensive
            if (devCount > 0) {
                long sleepMS = ((4500L * deltaMS) / 30000L) + 500L;
                if (sleepMS > 5000L) { sleepMS = 5000L; }
                try { Thread.sleep(sleepMS); } catch (Throwable th) {/*ignore*/}
            }

        } // for (String devID : devList)

        /* logging */
        if (log) {
            StringBuffer sb = new StringBuffer();
            sb.append("  Total : ").append(StringTools.leftAlign(acctID+"/"+groupID,25));
            sb.append(" - deleted ").append(StringTools.rightAlign(String.valueOf(totalCount),5));
            Print.sysPrintln(sb.toString());
        }

        /* return total count */
        if ((totalCount <= 0L) && isInnoDB) {
            return -1L; // InnoDB
        } else {
            return totalCount;
        }

    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /* return true if specified group exists in the account */
    public static boolean exists(String acctID, String groupID)
        throws DBException // if error occurs while testing existence
    {
        if (StringTools.isBlank(acctID)) {
            // invalid account
            return false;
        } else
        if (StringTools.isBlank(groupID)) {
            // invalid group
            return false;
        } else
        if (groupID.equalsIgnoreCase(DeviceGroup.DEVICE_GROUP_ALL)) {
            // 'all' always exists
            return true;
        } else {
            DeviceGroup.Key groupKey = new DeviceGroup.Key(acctID, groupID);
            return groupKey.exists();
        }
    }

    /* return true if specified device exists in the normal group of the account */
    public static boolean exists(String acctID, String groupID, String deviceID)
        throws DBException // if error occurs while testing existence
    {
        if ((acctID != null) && (groupID != null) && (deviceID != null)) {
            DeviceList.Key deviceListKey = new DeviceList.Key(acctID, groupID, deviceID);
            return deviceListKey.exists();
        }
        return false;
    }

    /* return true if specified account/device exists in the universal group of the account*/
    public static boolean exists(String acctID, String groupID, String devaccID, String deviceID)
        throws DBException // if error occurs while testing existence
    {
        if ((acctID != null) && (groupID != null) && (devaccID != null) && (deviceID != null)) {
            DeviceUList.Key deviceUListKey = new DeviceUList.Key(acctID, groupID, devaccID, deviceID);
            return deviceUListKey.exists();
        }
        return false;
    }

    // ------------------------------------------------------------------------

    /* Return specified group */
    public static DeviceGroup getDeviceGroup(Account account, String groupId)
        throws DBException
    {
        if (groupId == null) {
            return null;
        } else {
            return DeviceGroup.getDeviceGroup(account, groupId, false);
        }
    }

    /* Return specified group, create if specified */
    public static DeviceGroup getDeviceGroup(Account account, String groupId, boolean createOK)
        throws DBException
    {
        // does not return null, if 'createOK' is true

        /* account-id specified? */
        if (account == null) {
            throw new DBException("Account not specified.");
        }

        /* group-id specified? */
        if (StringTools.isBlank(groupId)) {
            throw new DBException("Device Group-ID not specified.");
        }

        /* get/create group */
        DeviceGroup.Key groupKey = new DeviceGroup.Key(account.getAccountID(), groupId);
        if (groupKey.exists()) { // may throw DBException
            DeviceGroup group = groupKey.getDBRecord(true);
            group.setAccount(account);
            return group;
        } else
        if (createOK) {
            DeviceGroup group = groupKey.getDBRecord();
            group.setAccount(account);
            group.setCreationDefaultValues();
            return group; // not yet saved!
        } else {
            // record doesn't exist, and caller doesn't want us to create it
            return null;
        }

    }

    /* create device group */
    public static DeviceGroup createNewDeviceGroup(Account account, String groupID)
        throws DBException
    {
        if ((account != null) && (groupID != null) && !groupID.equals("")) {
            DeviceGroup group = DeviceGroup.getDeviceGroup(account, groupID, true); // does not return null
            group.save();
            return group;
        } else {
            throw new DBException("Invalid Account/GroupID specified");
        }
    }

    // ------------------------------------------------------------------------

    /* return the DBSelect statement to search members of the specified normal group of the account */
    protected static DBSelect<DeviceList> _getDeviceListSelect(String acctId, String groupId, long limit)
    {

        /* empty/null account */
        if (StringTools.isBlank(acctId)) {
            return null;
        }

        /* empty/null group */
        if (StringTools.isBlank(groupId)) {
            return null;
        }
        
        /* get select */
        // DBSelect: SELECT * FROM DeviceList WHERE ((accountID='acct') and (groupID='group')) ORDER BY deviceID
        DBSelect<DeviceList> dsel = new DBSelect<DeviceList>(DeviceList.getFactory());
        dsel.setSelectedFields(DeviceList.FLD_deviceID);
        DBWhere dwh = dsel.createDBWhere();
        String wh = dwh.WHERE_(dwh.AND(dwh.EQ(AccountRecord.FLD_accountID,acctId ),dwh.EQ(DeviceList.FLD_groupID  ,groupId))); 
        dsel.setWhere(wh);
        dsel.setOrderByFields(DeviceList.FLD_deviceID);
        dsel.setLimit(limit);
        return dsel;

    }

    /* return the DBSelect statement to search members of the specified universal group of the account */
    protected static DBSelect<DeviceUList> _getDeviceUListSelect(String acctId, String groupId, long limit)
    {

        /* empty/null account */
        if (StringTools.isBlank(acctId)) {
            return null;
        }

        /* empty/null group */
        if (StringTools.isBlank(groupId)) {
            return null;
        }
        
        /* get select */
        // DBSelect: SELECT devaccID,deviceID FROM DeviceUList WHERE ((accountID='acctId') and (groupID='groupId')) ORDER BY devaccID, deviceID
        DBSelect<DeviceUList> dsel = new DBSelect<DeviceUList>(DeviceUList.getFactory());
        dsel.setSelectedFields(DeviceUList.FLD_devaccID,DeviceList.FLD_deviceID);
        DBWhere dwh = dsel.createDBWhere();
        String wh = dwh.WHERE_(dwh.AND(dwh.EQ(AccountRecord.FLD_accountID,acctId),dwh.EQ(DeviceList.FLD_groupID,groupId))); 
        dsel.setWhere(wh);
        dsel.setOrderByFields(DeviceUList.FLD_devaccID,DeviceList.FLD_deviceID);
        dsel.setLimit(limit);
        return dsel;

    }

    /* return the number of devices in this normal/universal group */
    public long getAllDeviceCount()
    {
        
        /* get db selector */
        String acctId  = this.getAccountID();
        String groupId = this.getGroupID();
        DBSelect<DeviceUList> dsel = DeviceGroup._getDeviceUListSelect(acctId, groupId, -1L);
        if (dsel == null) {
            return 0;
        }

        /* return count */
        try {
            //Print.logInfo("Retrieving count: " + dsel);
            return DBRecord.getRecordCount(DeviceUList.getFactory(), dsel.getWhere());
        } catch (DBException dbe) {
            Print.logException("Unable to retrieve DeviceUList count", dbe);
            return 0L;
        }
        
    }

    /* return the number of devices in this normal group (deprecated - getAllDeviceCount should be used) */
    // ******************** Does not support universal groups
    public long getDeviceCount()
    {
        
        /* get db selector */
        String acctId  = this.getAccountID();
        String groupId = this.getGroupID();
        DBSelect<DeviceList> dsel = DeviceGroup._getDeviceListSelect(acctId, groupId, -1L);
        if (dsel == null) {
            return 0;
        }

        /* return count */
        try {
            //Print.logInfo("Retrieving count: " + dsel);
            return DBRecord.getRecordCount(DeviceList.getFactory(), dsel.getWhere());
        } catch (DBException dbe) {
            Print.logException("Unable to retrieve DeviceList count", dbe);
            return 0L;
        }
        
    }

    /* return list of all Devices within the specified DeviceGroup (NOT SCALABLE BEYOND A FEW HUNDRED DEVICES) */
    public OrderedSet<String> getDevices(User userAuth, boolean inclInactv)
        throws DBException
    {
        String acctId  = this.getAccountID();
        String groupId = this.getGroupID();
        return DeviceGroup.getDeviceIDsForGroup(acctId, groupId, userAuth, inclInactv, -1L);
    }

    /* return list of all Devices within the specified normal DeviceGroup (NOT SCALABLE BEYOND A FEW HUNDRED DEVICES) */
    public static OrderedSet<String> getDeviceIDsForGroup(
        String acctId, String groupId, User userAuth, 
        boolean inclInactv)
        throws DBException
    {
        return DeviceGroup.getDeviceIDsForGroup(acctId, groupId, userAuth, inclInactv, -1L);
    }

    /* return list of all Devices within the specified normal DeviceGroup as array (to be compatible with getAllDevicesForGroup) */
    public static OrderedSet<String[]> getArrDeviceIDsForGroup(
        String acctId, String groupId, User userAuth, 
        boolean inclInactv)
        throws DBException
    {
    	Integer devListSize;
    	OrderedSet<String[]> arrDevList = new OrderedSet<String[]>();
    	OrderedSet<String> devList = DeviceGroup.getDeviceIDsForGroup(acctId, groupId, userAuth, inclInactv, -1L);
    	if(devList!=null) {
	    	devListSize = devList.size();
	    	for(int i=0; i<devListSize; i++) {
	    		arrDevList.add( new String[] { acctId, devList.get(i) } );
	    	}
    	}
        return arrDevList;
    }

    /**
    *** return list of all Devices within the specified universal DeviceGroup 
    *** (NOT SCALABLE BEYOND A FEW HUNDRED DEVICES)
    *** @param acctId      The account
    *** @param groupID     The group-id
    *** @param userAuth    Ignored
    *** @param inclInactv  Include inactive devices?
    *** @return Ordered set of String arrays { accountId, deviceId }
    **/
    public static OrderedSet<String[]> getAllDevicesForGroup(
        String acctId, String groupId, User userAuth,/*ignored*/ 
        boolean inclInactv)
        throws DBException
    {
        return DeviceGroup.getAllDevicesForGroup(acctId, groupId, userAuth,/*ignored*/ inclInactv, -1L);
    }

    /* return list of all Devices within the specified normal DeviceGroup (NOT SCALABLE BEYOND A FEW HUNDRED DEVICES) */
    public static OrderedSet<String> getDeviceIDsForGroup(
        String acctId, String groupId, User userAuth, 
        boolean inclInactv, long limit)
        throws DBException
    {

        /* valid accountId/groupId? */
        if (StringTools.isBlank(acctId)) {
            return new OrderedSet<String>();
        } else
        if (StringTools.isBlank(groupId)) {
            return new OrderedSet<String>();
        }

        /* "All"? */
        if (groupId.equalsIgnoreCase(DeviceGroup.DEVICE_GROUP_ALL)) {
            return Device.getDeviceIDsForAccount(acctId, userAuth, inclInactv);
        }

        /* get db selector */
        DBSelect<DeviceList> dsel = DeviceGroup._getDeviceListSelect(acctId, groupId, limit);
        if (dsel == null) {
            return new OrderedSet<String>();
        }

        /* read Account? */
        Account account = null;
        if (!inclInactv) {
            // We need the Account, to read the Devices, to determine if they are active/inactive
            // There is a chance that the User already has a handle to the Account
            account = (userAuth != null)? userAuth.getAccount() : Account.getAccount(acctId);
            if (account == null) {
                // account not found?
                Print.logWarn("Account not found? " + acctId);
                return new OrderedSet<String>();
            }
        }
        
        /* read devices for account */
        OrderedSet<String> devList = new OrderedSet<String>();
        DBConnection dbc = null;
        Statement   stmt = null;
        ResultSet     rs = null;
        try {
            dbc  = DBConnection.getDefaultConnection();
            stmt = dbc.execute(dsel.toString());
            rs   = stmt.getResultSet();
            while (rs.next()) {
                String devId = rs.getString(DeviceList.FLD_deviceID);
                // trim inactive?
                if (!inclInactv) {
                    Device device = (account != null)? 
                        Device._getDevice(account, devId) : 
                        null;
                    if ((device == null) || !device.isActive()) {
                        continue;
                    }
                }
                // trim unauthorized?
                if ((userAuth != null) && !userAuth.isAuthorizedDevice(devId)) {
                    continue;
                }
                // device ok
                devList.add(devId);
            }
        } catch (SQLException sqe) {
            throw new DBException("Get Group DeviceList", sqe);
        } finally {
            if (rs   != null) { try { rs.close();   } catch (Throwable t) {} }
            if (stmt != null) { try { stmt.close(); } catch (Throwable t) {} }
            DBConnection.release(dbc);
        }

        /* return list */
        return devList;

    }

    /**
    *** return list of all Devices within the specified universal DeviceGroup 
    *** (NOT SCALABLE BEYOND A FEW HUNDRED DEVICES)
    *** @param acctId      The account
    *** @param groupID     The group-id
    *** @param userAuth    Ignored
    *** @param inclInactv  Include inactive devices?
    *** @param limit	   Maximum number of devices returned
    *** @return Ordered set of String arrays { accountId, deviceId }
    **/
    public static OrderedSet<String[]> getAllDevicesForGroup(
        String acctId, String groupId, User userAuth,/*ignored*/ 
        boolean inclInactv, long limit)
        throws DBException
    {

        /* valid accountId/groupId? */
        if (StringTools.isBlank(acctId)) {
            return new OrderedSet<String[]>();
        } else
        if (StringTools.isBlank(groupId)) {
            return new OrderedSet<String[]>();
        }

        /* "All"? */
        if (groupId.equalsIgnoreCase(DeviceGroup.DEVICE_GROUP_ALL)) {
//            return Device.getAllDevices(inclInactv);	// We consider that 'all' universal group contains ALL devices of ALL accounts
        	OrderedSet<String> acctDevList=Device.getDeviceIDsForAccount(acctId, userAuth, inclInactv);
        	OrderedSet<String[]> devList = new OrderedSet<String[]>();
        	for(String devId : acctDevList) {
                String[] device = new String[] { acctId, devId };
                devList.add(device);
        	}
        	return devList;
        }

        /* get db selector */
        DBSelect<DeviceUList> dsel = DeviceGroup._getDeviceUListSelect(acctId, groupId, limit);
        if (dsel == null) {
            return new OrderedSet<String[]>();
        }

        Account account = Account.getAccount(acctId);
        if (account == null) {
            // account not found?
            Print.logWarn("Get Group UDeviceList - Account not found? " + acctId);
            return new OrderedSet<String[]>();
        }        
        
        /* read devices for account */
        OrderedSet<String[]> devList = new OrderedSet<String[]>();
        DBConnection dbc = null;
        Statement   stmt = null;
        ResultSet     rs = null;
        try {
            dbc  = DBConnection.getDefaultConnection();
            stmt = dbc.execute(dsel.toString());
            rs   = stmt.getResultSet();
            while (rs.next()) {
                String dId = rs.getString(DeviceList.FLD_deviceID);
                String aId = rs.getString(DeviceUList.FLD_devaccID);
                // trim inactive?
                if (!inclInactv) {
                	Account acc = Account.getAccount(aId);
                    Device dev = (acc != null) ? Device._getDevice(acc, dId) : null;
                    if ( (dev == null) || !dev.isActive()) {
                        continue;
                    }
                }
              String[] device = new String[] { aId, dId };
              devList.add(device);
            }
        } catch (SQLException sqe) {
            throw new DBException("Get Group UDeviceList", sqe);
        } finally {
            if (rs   != null) { try { rs.close();   } catch (Throwable t) {} }
            if (stmt != null) { try { stmt.close(); } catch (Throwable t) {} }
            DBConnection.release(dbc);
        }

        /* return list */
        return devList;

    }
    
    // ------------------------------------------------------------------------

    /* return list of all DeviceGroups owned by the specified Account (NOT SCALABLE) */
    // does not return null
    public static OrderedSet<String> getDeviceGroupsForAccount(String acctId, boolean includeAll)
        throws DBException
    {

        /* select */
        // DBSelect: SELECT * FROM DeviceGroup WHERE (accountID='acct') ORDER BY groupID
        DBSelect<DeviceGroup> dsel = new DBSelect<DeviceGroup>(DeviceGroup.getFactory());
        dsel.setSelectedFields(DeviceGroup.FLD_groupID);
        DBWhere dwh = dsel.createDBWhere();
        dsel.setWhere(
            dwh.WHERE(
                dwh.EQ(DeviceGroup.FLD_accountID,acctId)
            )
        );
        dsel.setOrderByFields(DeviceGroup.FLD_groupID);

        /* return list */
        return DeviceGroup.getDeviceGroups(dsel, includeAll);

    }

    /* return list of all DeviceGroups owned by the specified Account (NOT SCALABLE) */
    // does not return null
    public static OrderedSet<String> getDeviceGroups(DBSelect<DeviceGroup> dsel, boolean includeAll)
        throws DBException
    {

        /* group ID list, always add 'All' */
        OrderedSet<String> groupList = new OrderedSet<String>(true);

        /* include 'All'? */
        if (includeAll) {
            groupList.add(DeviceGroup.DEVICE_GROUP_ALL);
        }

        /* invalid account */
        if (dsel == null) {
            return groupList;
        }

        /* read device groups for account */
        DBConnection dbc = null;
        Statement   stmt = null;
        ResultSet     rs = null;
        try {

            /* get records */
            dbc  = DBConnection.getDefaultConnection();
            stmt = dbc.execute(dsel.toString());
            rs   = stmt.getResultSet();
            while (rs.next()) {
                String groupId = rs.getString(DeviceGroup.FLD_groupID);
                //Print.logInfo("Adding DeviceGroup: " + groupId);
                groupList.add(groupId);
            }

        } catch (SQLException sqe) {
            throw new DBException("Getting Account DeviceGroup List", sqe);
        } finally {
            if (rs   != null) { try { rs.close();   } catch (Throwable t) {} }
            if (stmt != null) { try { stmt.close(); } catch (Throwable t) {} }
            DBConnection.release(dbc);
        }

        /* return list */
        return groupList;

    }

    // ------------------------------------------------------------------------

    /* return list of all DeviceGroups of current account in which the specified device is a member */
    public static Collection<String> getDeviceGroupsForDevice(String acctId, String deviceId)
        throws DBException
    {
        return DeviceGroup.getDeviceGroupsForDevice(acctId, deviceId, true);
    }
    
    /* return list of all DeviceGroups of current account in which the specified device is a member */
    // TODO: add support of universal groups
    public static Collection<String> getDeviceGroupsForDevice(String acctId, String deviceId, boolean inclAll)
        throws DBException
    {

        /* valid Account/Device? */
        try {
            if ((acctId == null) || (deviceId == null) || !Device.exists(acctId,deviceId)) {
                return null;
            }
        } catch (DBException dbe) {
            // error attempting to text device existance
            return null;
        }

        /* group ids */
        java.util.List<String> groupList = new Vector<String>();

        /* include all? */
        if (inclAll) {
            groupList.add(DeviceGroup.DEVICE_GROUP_ALL);
        }

        /* get select */
        // DBSelect: SELECT * FROM DeviceList WHERE ((accountID='acct') and (deviceID='dev')) ORDER BY groupID
        DBSelect<DeviceList> dsel = new DBSelect<DeviceList>(DeviceList.getFactory());
        dsel.setSelectedFields(DeviceList.FLD_groupID);
        DBWhere dwh = dsel.createDBWhere();
        dsel.setWhere(
            dwh.WHERE_(
                dwh.AND(
                    dwh.EQ(DeviceList.FLD_accountID,acctId  ),
                    dwh.EQ(DeviceList.FLD_deviceID ,deviceId)
                )
            )
        );
        dsel.setOrderByFields(DeviceList.FLD_groupID);

        /* read devices for DeviceGroup */
        DBConnection dbc = null;
        Statement   stmt = null;
        ResultSet     rs = null;
        try {
            dbc  = DBConnection.getDefaultConnection();
            stmt = dbc.execute(dsel.toString());
            rs   = stmt.getResultSet();
            while (rs.next()) {
                String devId = rs.getString(DeviceList.FLD_groupID);
                groupList.add(devId);
            }
        } catch (SQLException sqe) {
            throw new DBException("Get Group DeviceList", sqe);
        } finally {
            if (rs   != null) { try { rs.close();   } catch (Throwable t) {} }
            if (stmt != null) { try { stmt.close(); } catch (Throwable t) {} }
            DBConnection.release(dbc);
        }

        /* return list */
        return groupList;

    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private static final String ARG_ACCOUNT[]           = new String[] { "account", "acct" };
    private static final String ARG_GROUP[]             = new String[] { "group"  , "grp"  };
    private static final String ARG_CREATE[]            = new String[] { "create" , "cr"   };
    private static final String ARG_EDIT[]              = new String[] { "edit"   , "ed"   };
    private static final String ARG_DELETE[]            = new String[] { "delete"          };
    private static final String ARG_ADD[]               = new String[] { "add"             };
    private static final String ARG_REMOVE[]            = new String[] { "remove"          };
    private static final String ARG_LIST[]              = new String[] { "list"            };
    private static final String ARG_CNT_OLD_EV[]        = new String[] { "countOldEvents"  };
    private static final String ARG_DEL_OLD_EV[]        = new String[] { "deleteOldEvents" };
    private static final String ARG_CONFIRM[]           = new String[] { "confirm"         };

    private static void usage()
    {
        Print.sysPrintln("Usage:");
        Print.sysPrintln("  java ... " + DeviceGroup.class.getName() + " {options}");
        Print.sysPrintln("Common Options:");
        Print.sysPrintln("  -account=<id>               Acount ID which owns DeviceGroup");
        Print.sysPrintln("  -group=<id>                 Group ID to create/edit");
        Print.sysPrintln("");
        Print.sysPrintln("  -create                     Create a new DeviceGroup");
        Print.sysPrintln("  -edit                       Edit an existing (or newly created) DeviceGroup");
        Print.sysPrintln("  -delete                     Delete specified DeviceGroup");
        Print.sysPrintln("  -add=<deviceID>             Add deviceID to group");
        Print.sysPrintln("  -remove=<deviceID>          Remove deviceID from group");
        Print.sysPrintln("  -list                       List Devices in this Group");
        Print.sysPrintln("");
        Print.sysPrintln("  -countOldEvents=<time>      Count events before specified time (requires '-confirm')");
        Print.sysPrintln("  -deleteOldEvents=<time>     Delete events ibefore specified time (requires '-confirm')");
        Print.sysPrintln("  -confirm                    Confirms countOldEvents/deleteOldEvents");
        System.exit(1);
    }

    public static void main(String argv[])	// TODO: add universal groups management
    {
        DBConfig.cmdLineInit(argv,true);  // main
        String accountID = RTConfig.getString(ARG_ACCOUNT, "");
        String groupID   = RTConfig.getString(ARG_GROUP  , "");
        boolean list     = RTConfig.getBoolean(ARG_LIST, false);

        /* option count */
        int opts = 0;

        /* account-id specified? */
        if ((accountID == null) || accountID.equals("")) {
            Print.logError("Account-ID not specified.");
            usage();
        }

        /* get account */
        Account acct = null;
        try {
            acct = Account.getAccount(accountID); // may throw DBException
            if (acct == null) {
                Print.logError("Account-ID does not exist: " + accountID);
                usage();
            }
        } catch (DBException dbe) {
            Print.logException("Error loading Account: " + accountID, dbe);
            //dbe.printException();
            System.exit(99);
        }

        /* group-id specified? */
        boolean hasGroupID = (groupID != null) && !groupID.equals("");

        /* group exists? */
        boolean groupExists = false;
        if (hasGroupID) {
            try {
                groupExists = DeviceGroup.exists(accountID, groupID);
            } catch (DBException dbe) {
                Print.logError("Error determining if DeviceGroup exists: " + accountID + "," + groupID);
                System.exit(99);
            }
        }

        /* delete */
        if (RTConfig.getBoolean(ARG_DELETE, false)) {
            opts++;
            if (!hasGroupID) {
                Print.logError("Group-ID not specified.");
                usage();
            } else
            if (!groupExists) {
                Print.logWarn("DeviceGroup does not exist: " + accountID + "/" + groupID);
                Print.logWarn("Continuing with delete process ...");
            }
            try {
                DeviceGroup.Key groupKey = new DeviceGroup.Key(accountID, groupID);
                groupKey.delete(true); // also deletes dependencies
                Print.logInfo("DeviceGroup deleted: " + accountID + "/" + groupID);
            } catch (DBException dbe) {
                Print.logError("Error deleting DeviceGroup: " + accountID + "/" + groupID);
                dbe.printException();
                System.exit(99);
            }
            System.exit(0);
        }

        /* create */
        if (RTConfig.getBoolean(ARG_CREATE, false)) {
            opts++;
            if (!hasGroupID) {
                Print.logError("Group-ID not specified.");
                usage();
            } else
            if (groupExists) {
                Print.logWarn("DeviceGroup already exists: " + accountID + "/" + groupID);
            } else {
                try {
                    DeviceGroup.createNewDeviceGroup(acct, groupID);
                    Print.logInfo("Created DeviceGroup: " + accountID + "/" + groupID);
                } catch (DBException dbe) {
                    Print.logError("Error creating DeviceGroup: " + accountID + "/" + groupID);
                    dbe.printException();
                    System.exit(99);
                }
            }
        }

        /* edit */
        if (RTConfig.getBoolean(ARG_EDIT, false)) {
            opts++;
            if (!hasGroupID) {
                Print.logError("Group-ID not specified.");
                usage();
            } else
            if (!groupExists) {
                Print.logError("DeviceGroup does not exist: " + accountID + "/" + groupID);
            } else {
                try {
                    DeviceGroup group = DeviceGroup.getDeviceGroup(acct, groupID, false); // may throw DBException
                    DBEdit editor = new DBEdit(group);
                    editor.edit(); // may throw IOException
                } catch (IOException ioe) {
                    if (ioe instanceof EOFException) {
                        Print.logError("End of input");
                    } else {
                        Print.logError("IO Error");
                    }
                } catch (DBException dbe) {
                    Print.logError("Error editing DeviceGroup: " + accountID + "/" + groupID);
                    dbe.printException();
                    System.exit(99);
                }
            }
            System.exit(0);
        }

        /* add */
        if (RTConfig.hasProperty(ARG_ADD)) {
            opts++;
            String deviceID = RTConfig.getString(ARG_ADD, "");
            if (!hasGroupID) {
                Print.logError("Group-ID not specified.");
                usage();
            } else
            if (!groupExists) {
                Print.logError("DeviceGroup does not exist: " + accountID + "/" + groupID);
            } else {
                try {
                    DeviceGroup.addDeviceToDeviceGroup(accountID, groupID, deviceID);
                    Print.logInfo("DeviceList entry added: " + accountID + "/" + groupID + "/" + deviceID);
                } catch (DBException dbe) {
                    Print.logError("Error creating DeviceList entry: " + accountID + "/" + groupID + "/" + deviceID);
                    dbe.printException();
                    System.exit(99);
                }
            }
            System.exit(0);
        }

        /* remove */
        if (RTConfig.hasProperty(ARG_REMOVE)) {
            opts++;
            if (!hasGroupID) {
                Print.logError("Group-ID not specified.");
                usage();
            } else
            if (!groupExists) {
                Print.logError("DeviceGroup does not exist: " + accountID + "/" + groupID);
            } else {
                String deviceID = RTConfig.getString(ARG_REMOVE, "");
                try {
                    DeviceGroup.removeDeviceFromDeviceGroup(accountID, groupID, deviceID);
                    Print.logInfo("DeviceList entry deleted: " + accountID + "/" + groupID + "/" + deviceID);
                } catch (DBException dbe) {
                    Print.logError("Error creating DeviceList entry: " + accountID + "/" + groupID + "/" + deviceID);
                    dbe.printException();
                    System.exit(99);
                }
            }
            System.exit(0);
        }

        /* list */
        if (RTConfig.hasProperty(ARG_LIST)) {
            opts++;
            if (hasGroupID) {
                if (!groupExists) {
                    Print.logError("DeviceGroup does not exist: " + accountID + "/" + groupID);
                } else {
                    Print.sysPrintln("");
                    Print.sysPrintln("DeviceGroup: " + accountID + "/" + groupID);
                    try {
                        OrderedSet<String> devList = DeviceGroup.getDeviceIDsForGroup(accountID, groupID, null, true/*inclInactv*/, -1L);
                        if (devList.size() <= 0) {
                            Print.sysPrintln("  No Devices");
                        } else {
                            for (int d = 0; d < devList.size(); d++) {
                                Print.sysPrintln("  Device: " + devList.get(d));
                            }
                        }
                    } catch (DBException dbe) {
                        Print.logError("Error listing Devices: " + accountID + "/" + groupID);
                        dbe.printException();
                        System.exit(99);
                    }
                }
            } else {
                try {
                    Print.sysPrintln("");
                    Print.logInfo("Account: " + accountID);
                    OrderedSet<String> groupList = DeviceGroup.getDeviceGroupsForAccount(accountID, true);
                    if (groupList.size() == 0) {
                        Print.sysPrintln("  No DeviceGroups");
                    } else {
                        for (String gid : groupList) {
                            Print.sysPrintln("  DeviceGroup: " + gid);
                        }
                    }
                } catch (DBException dbe) {
                    Print.logError("Error listing DeviceGroups: " + accountID);
                    dbe.printException();
                    System.exit(99);
                }
            }
        }

        /* count/delete old events */
        if (RTConfig.hasProperty(ARG_CNT_OLD_EV) || 
            RTConfig.hasProperty(ARG_DEL_OLD_EV)   ) {
            opts++;
            boolean deleteEvents = RTConfig.hasProperty(ARG_DEL_OLD_EV);
            String actionText = deleteEvents? "Deleting" : "Counting";
            TimeZone acctTMZ = acct.getTimeZone(null);
            String   argTime = deleteEvents?
                RTConfig.getString(ARG_DEL_OLD_EV,"") :
                RTConfig.getString(ARG_CNT_OLD_EV,"");
            // DeviceGroup must exist
            if (!hasGroupID) {
                Print.logError("DeviceGroup ID not specified");
                System.exit(99);
            } else
            if (!groupExists) {
                Print.logError("DeviceGroup does not exist: " + accountID + "/" + groupID);
                System.exit(99);
            }
            // arg time
            DateTime oldTime = null;
            if (StringTools.isBlank(argTime)) {
                Print.logError("Invalid time specification: " + argTime);
                System.exit(98);
            } else
            if (argTime.equalsIgnoreCase("current")) {
                oldTime = new DateTime(acctTMZ);
            } else {
                try {
                    oldTime = DateTime.parseArgumentDate(argTime,acctTMZ,true); // end of day time
                } catch (DateTime.DateParseException dpe) {
                    oldTime = null;
                }
                if (oldTime == null) {
                    Print.sysPrintln("Invalid Time specification: " + argTime);
                    System.exit(98);
                } else
                if (oldTime.getTimeSec() > DateTime.getCurrentTimeSec()) {
                    Print.sysPrintln(actionText + " future events not allowed");
                    System.exit(98);
                }
            }
            // count/delete events
            long oldTimeSec = oldTime.getTimeSec();
            boolean confirm = RTConfig.getBoolean(ARG_CONFIRM,false);
            try {
                if (deleteEvents) {
                    Print.sysPrintln("Deleting events prior to: " + (new DateTime(oldTimeSec)));
                    if (!confirm) {
                        Print.sysPrintln("ERROR: Missing '-confirm', aborting delete ...");
                        System.exit(1);
                    }
                    DeviceGroup.deleteOldEvents(acct, groupID, oldTimeSec, true); // InnoDB?
                } else {
                    Print.sysPrintln("Counting events prior to: " + (new DateTime(oldTimeSec)));
                    if (!confirm) {
                        Print.sysPrintln("ERROR: Missing '-confirm', aborting count ...");
                        System.exit(1);
                    }
                    DeviceGroup.countOldEvents(acct, groupID, oldTimeSec, true); // InnoDB?
                }
                System.exit(0);
            } catch (DBException dbe) {
                Print.logError("Error " + actionText + " old events: " + dbe);
                System.exit(99);
            }
        }
        
        /* no options specified */
        if (opts == 0) {
            Print.logWarn("Missing options ...");
            usage();
        }
        
    }
    
}
