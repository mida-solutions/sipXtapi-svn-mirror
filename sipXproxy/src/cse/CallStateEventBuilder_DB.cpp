//
// Copyright (C) 2006 SIPfoundry Inc.
// License by SIPfoundry under the LGPL license.
// 
// Copyright (C) 2006 Pingtel Corp.
// Licensed to SIPfoundry under a Contributor Agreement.
//
//////////////////////////////////////////////////////////////////////////////

// SYSTEM INCLUDES
#include <assert.h>
#include "os/OsSysLog.h"
#include "os/OsDateTime.h"

// APPLICATION INCLUDES
#include "CallStateEventBuilder_DB.h"

// DEFINES
// MACROS
// EXTERNAL FUNCTIONS
// EXTERNAL VARIABLES
// CONSTANTS

static const char CallRequestType = 'R';
static const char CallSetupType = 'S';
static const char CallEndType = 'E';
static const char CallFailureType = 'F';
static const char CallTransferType = 'T';

static const char* ModuleName = 
  "CallStateEventBuilder_DB";

static const char* ObserverEventTable = 
  "observer_state_events";
  
static const char* CallEventTable =
  "call_state_events";

static const char* CallEvent_Start =
  "INSERT INTO %s VALUES (DEFAULT,\'%s\',%d,"
  "timestamp \'";

static const char* CallEvent_NoFailure =
  "0,\'\'";
  
static const char* CallEvent_DefaultElement =
  "\'\',";
  
static const char* CallEvent_DefaultReferElement =
  "\'\',\'\',";  

// STRUCTS
// TYPEDEFS
// FORWARD DECLARATIONS

/* //////////////////////////// PUBLIC //////////////////////////////////// */

/* ============================ CREATORS ================================== */

/// Instantiate an event builder and set the observer name for its events
CallStateEventBuilder_DB::CallStateEventBuilder_DB(const char* observerDnsName ///< the DNS name to be recorded in all events
                                                     ) :
   CallStateEventBuilder(observerDnsName)
{
}


/// Destructor
CallStateEventBuilder_DB::~CallStateEventBuilder_DB()
{
}


/**
 * Generate a metadata event.
 * This method generates a complete event - it does not require that the callEventComplete method be called.
 */
void CallStateEventBuilder_DB::observerEvent(int sequenceNumber, ///< for ObserverReset, this should be zero
                                             const OsTime& timestamp,      ///< obtained using getCurTime(OsTime)
                                             ObserverEvent eventCode,
                                             const char* eventMsg ///< for human consumption
                                             )
{
   BuilderMethod eventMethod;
 
   switch (eventCode)
   {
   case ObserverReset:
      reset(); // because this event is ok any time, clear out any partial event.
      eventMethod = BuilderReset;
      break;
   default:
      assert(false);
      OsSysLog::add(FAC_SIP, PRI_ERR, "observerEvent: invalid eventCode %d", eventCode);
      eventMethod = InvalidEvent;
      break;
   }

   if (builderStateIsOk(eventMethod))
   {
      newEvent(sequenceNumber, timestamp, ObserverEventTable);
   
      char buffer[256];
      snprintf(buffer, 256, "%d,\'%s\'", 
               eventCode, eventMsg);
      mCurrentEvent.append(buffer);

      mCallInfo.remove(0);
      mReferElement.remove(0);      
      mContactElement.remove(0);
      mReferElement.remove(0);
      mFailureElement.remove(0);

      mEventComplete = true;
   }
   else
   {
      assert(false);
      OsSysLog::add(FAC_SIP, PRI_ERR, "observerEvent: %d not allowed.", eventCode);
   }
}

/// Begin a Call Request Event - an INVITE without a to tag has been observed
/**
 * Requires:
 *   - callRequestEvent
 *   - addCallData (the toTag in the addCallRequest will be a null string)
 *   - addEventVia (at least for via index zero)
 *   - completeCallEvent
 */
void CallStateEventBuilder_DB::callRequestEvent(int sequenceNumber,
                                                 const OsTime& timestamp,      ///< obtain using getCurTime(OsTime)
                                                 const UtlString& contact
                                                 )
{
   if (builderStateIsOk(CallRequestEvent))
   {
      newEvent(sequenceNumber, timestamp, CallEventTable, CallRequestType);

      // Translate singe quotes
      if (contact.index('\'') != UTL_NOT_FOUND)
      {
         UtlString ncontact(contact);
         ncontact.replace('\'', '"');
         mContactElement = "\'" + ncontact + "\',";         
      }
      else
      {
         mContactElement = "\'" + contact + "\',";
      }
   }
   else
   {
      assert(false);
      OsSysLog::add(FAC_SIP, PRI_ERR, 
                    "%s::callRequestEvent not allowed.",
                    ModuleName);
   }
}


/// Begin a Call Setup Event - a 2xx response to an INVITE has been observed
/**
 * Requires:
 *   - callSetupEvent
 *   - addCallData
 *   - addEventVia (at least for via index zero)
 *   - completeCallEvent
 */
void CallStateEventBuilder_DB::callSetupEvent(int sequenceNumber,
                                               const OsTime& timestamp,      ///< obtain using getCurTime(OsTime)
                                               const UtlString& contact
                                               )
{
   if (builderStateIsOk(CallSetupEvent))
   {
      newEvent(sequenceNumber, timestamp, CallEventTable, CallSetupType);

      if (contact.index('\'') != UTL_NOT_FOUND)
      {
         UtlString ncontact(contact);
         ncontact.replace('\'', '"');
         mContactElement = "\'" + ncontact + "\',";         
      }
      else
      {
         mContactElement = "\'" + contact + "\',";
      }
   }
   else
   {
      assert(false);
      OsSysLog::add(FAC_SIP, PRI_ERR, 
                    "%s::callSetupEvent not allowed.", ModuleName);
   }
}


/// Begin a Call Failure Event - an error response to an INVITE has been observed
/**
 * Requires:
 *   - callFailureEvent
 *   - addCallData
 *   - addEventVia (at least for via index zero)
 *   - completeCallEvent
 */
void CallStateEventBuilder_DB::callFailureEvent(int sequenceNumber,
                                                 const OsTime& timestamp,      ///< obtain using getCurTime(OsTime)
                                                 int statusCode,
                                                 const UtlString& statusMsg
                                                 )
{
   if (builderStateIsOk(CallFailureEvent))
   {
      newEvent(sequenceNumber, timestamp, CallEventTable, CallFailureType);

      char buffer[256];
      snprintf(buffer, 256, "%d,\'%s\'", statusCode, statusMsg.data());
      mFailureElement = buffer;
   }
   else
   {
      assert(false);
      OsSysLog::add(FAC_SIP, PRI_ERR, 
                    "%s::callFailureEvent not allowed.", ModuleName);
   }
}


/// Begin a Call End Event - a BYE request has been observed
/**
 * Requires:
 *   - callEndEvent
 *   - addCallData
 *   - addEventVia (at least for via index zero)
 *   - completeCallEvent
 */
void CallStateEventBuilder_DB::callEndEvent(const int sequenceNumber,
                                             const OsTime& timestamp      ///< obtain using getCurTime(OsTime)
                                             )
{
   if (builderStateIsOk(CallEndEvent))
   {
      newEvent(sequenceNumber, timestamp, CallEventTable, CallEndType);
      mFailureElement = CallEvent_NoFailure;
   }
   else
   {
      assert(false);
      OsSysLog::add(FAC_SIP, PRI_ERR, 
                    "%s::callEndEvent not allowed.", ModuleName);
   }
}


/// Begin a Call Transfer Event - a REFER request has been observed
/**
 * Requires:
 *   - callTransferEvent
 *   - addCallData
 *   - completeCallEvent
 */
void CallStateEventBuilder_DB::callTransferEvent(int sequenceNumber, 
                                                  const OsTime& timeStamp, 
                                                  const UtlString& contact,
                                                  const UtlString& refer_to,
                                                  const UtlString& referred_by) 
{
   if (builderStateIsOk(CallTransferEvent))
   {
      newEvent(sequenceNumber, timeStamp, CallEventTable, CallTransferType);
      
      if (contact.index('\'') != UTL_NOT_FOUND)
      {
         UtlString ncontact(contact);
         ncontact.replace('\'', '"');
         mContactElement = "\'" + ncontact + "\',";         
      }
      else
      {
         mContactElement = "\'" + contact + "\',";           
      }
      
      if (refer_to.index('\'') != UTL_NOT_FOUND)
      {
         UtlString nrefer_to(refer_to);
         nrefer_to.replace('\'', '"');
         mReferElement = "\'" + nrefer_to + "\',";         
      }
      else
      {
         mReferElement = "\'" + refer_to + "\',";           
      }
      
      if (referred_by.index('\'') != UTL_NOT_FOUND)
      {
         UtlString nreferred_by(referred_by);
         nreferred_by.replace('\'', '"');
         mReferElement += "\'" + nreferred_by + "\',";         
      }
      else
      {
         mReferElement += "\'" + referred_by + "\',";           
      }                                          
    
   }
   else
   {
      assert(false);
      OsSysLog::add(FAC_SIP, PRI_ERR, 
                    "%s::callEndEvent not allowed.", ModuleName);
   }   
}


/// Add the dialog and call information for the event being built.
void CallStateEventBuilder_DB::addCallData(const UtlString& callId,
                                            const UtlString& fromTag,  /// may be a null string
                                            const UtlString& toTag,    /// may be a null string
                                            const UtlString& fromField,
                                            const UtlString& toField
                                            )
{
   if (builderStateIsOk(AddCallData))
   {
      if (callId.index('\'') != UTL_NOT_FOUND)
      {
         UtlString ncallId(callId);
         ncallId.replace('\'', '"');
         mCallInfo = "\'" + ncallId + "\',";         
      }
      else
      {
         mCallInfo = "\'" + callId + "\',";           
      }
      
      if (fromTag.index('\'') != UTL_NOT_FOUND)
      {
         UtlString nfromTag(fromTag);
         nfromTag.replace('\'', '"');
         mCallInfo += "\'" + nfromTag + "\',";         
      }
      else
      {
         mCallInfo += "\'" + fromTag + "\',";           
      }      

      if (toTag.index('\'') != UTL_NOT_FOUND)
      {
         UtlString ntoTag(toTag);
         ntoTag.replace('\'', '"');
         mCallInfo += "\'" + ntoTag + "\',";         
      }
      else
      {
         mCallInfo += "\'" + toTag + "\',";           
      }
      
      if (fromField.index('\'') != UTL_NOT_FOUND)
      {
         UtlString nfromField(fromField);
         nfromField.replace('\'', '"');
         mCallInfo += "\'" + nfromField + "\',";         
      }
      else
      {
         mCallInfo += "\'" + fromField + "\',";           
      }  
      
      if (toField.index('\'') != UTL_NOT_FOUND)
      {
         UtlString ntoField(toField);
         ntoField.replace('\'', '"');
         mCallInfo += "\'" + ntoField + "\',";         
      }
      else
      {
         mCallInfo += "\'" + toField + "\',";           
      }          
   }
   else
   {
      assert(false);
      OsSysLog::add(FAC_SIP, PRI_ERR, 
                    "%s::callEndEvent not allowed.", ModuleName);
   }
}

   
/// Add a via element for the event
/**
 * Record a Via from the message for this event 
 * Calls to this routine are in reverse cronological order - the last
 * call for an event should be the via added by the message originator
 */
void CallStateEventBuilder_DB::addEventVia(const UtlString& via
                                            )
{
   if (!builderStateIsOk(AddVia))
   {
      assert(false);
      OsSysLog::add(FAC_SIP, PRI_ERR, 
                    "%s::callEndEvent not allowed.", ModuleName);
   }
}


/// Indicates that all information for the current call event has been added.
void CallStateEventBuilder_DB::completeCallEvent()
{
   if (builderStateIsOk(CompleteCallEvent))
   {
      mEventComplete = true;
   }
   else
   {
      assert(false);
      OsSysLog::add(FAC_SIP, PRI_ERR, 
                    "%s::completeCallEvent not allowed.", ModuleName);
   }
}

/// Clears all the object state
void CallStateEventBuilder_DB::reset()
{
   mCurrentEvent.remove(0);
   mCallInfo = CallEvent_DefaultElement;
   mViaHeader = CallEvent_DefaultElement;
   mLaterElement = CallEvent_DefaultElement;
   mContactElement = CallEvent_DefaultElement;
   mReferElement = CallEvent_DefaultReferElement;
   mFailureElement = CallEvent_NoFailure;
   mEndElement.remove(0);
   mEventComplete = false;
}

void CallStateEventBuilder_DB::newEvent(int sequenceNumber,
                                        const OsTime& timestamp,      ///< obtain using getCurTime(OsTime)
                                        const char* eventTable,
                                        const char eventType
                                        )
{
   char buffer[256];  // size as const int
   
   snprintf(buffer, 256, CallEvent_Start, eventTable, 
            observerName, sequenceNumber);

   mCurrentEvent = buffer;
   
   OsDateTime timeValue(timestamp);
   UtlString timeString;
   timeValue.getSqlTimeStringZ(timeString);
   mCurrentEvent.append(timeString.data()); 
   mCurrentEvent.append("\',");
   if (eventType != '-')
   {   
      mCurrentEvent.append("\'");
      mCurrentEvent.append(eventType);
      mCurrentEvent.append("\',");
   }
}

/// Copies the element into the provided UtlString
bool  CallStateEventBuilder_DB::finishElement(UtlString& event)
/**<
 * @returns
 * - true if the returned element is validly constructed
 * - false if not (a caller error)
 */
{
   bool isComplete = mEventComplete;
   event.remove(0);

   if (isComplete)
   {
      event.append(mCurrentEvent);
      event.append(mCallInfo);
      event.append(mContactElement);
      event.append(mReferElement);
      event.append(mFailureElement);
      event.append(");");

      reset();
   }

   return isComplete;
}

