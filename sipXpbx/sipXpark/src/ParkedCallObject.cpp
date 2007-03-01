// 
// 
// Copyright (C) 2005 SIPfoundry Inc.
// Licensed by SIPfoundry under the LGPL license.
// 
// Copyright (C) 2005 Pingtel Corp.
// Licensed to SIPfoundry under a Contributor Agreement.
// 
// $$
////////////////////////////////////////////////////////////////////////////

// SYSTEM INCLUDES

// APPLICATION INCLUDES
#include "ParkedCallObject.h"
#include <net/Url.h>
#include <os/OsFS.h>
#include <filereader/OrbitFileReader.h>

// EXTERNAL FUNCTIONS
// EXTERNAL VARIABLES
// CONSTANTS
const UtlContainableType ParkedCallObject::TYPE = "ParkedCallObject";

// STATIC VARIABLE INITIALIZATIONS

int ParkedCallObject::sNextSeqNo = 0;
const int ParkedCallObject::sSeqNoIncrement = 2;
const int ParkedCallObject::sSeqNoMask = 0x3FFFFFFE;

/* //////////////////////////// PUBLIC //////////////////////////////////// */

/* ============================ CREATORS ================================== */

// Constructor
ParkedCallObject::ParkedCallObject(const UtlString& orbit,
                                   CallManager* callManager, 
                                   const UtlString& callId,
                                   const UtlString& address,
                                   const UtlString& playFile,
                                   bool bPickup,
                                   OsMsgQ* listenerQ) :
   mSeqNo(sNextSeqNo),
   mpCallManager(callManager),
   mOriginalCallId(callId),
   mCurrentCallId(callId),
   mOriginalAddress(address),
   mCurrentAddress(address),
   mpPlayer(NULL),
   mFile(playFile),
   mPickupCallId(NULL),
   mOrbit(orbit),
   mbPickup(bPickup),
   mbEstablished(false),
   mTimeoutTimer(listenerQ, mSeqNo + TIMEOUT),
   // Create the OsQueuedEvent to handle DTMF events.
   // This would ordinarily be an allocated object, because
   // removeDtmfEvent will delete it asynchronously.
   // But we do not need to call removeDtmfEvent, as we let call
   // teardown remove the pointer to mDtmfEvent (without attempting to
   // delete it).  We know that call teardown happens first, becausse
   // we do not delete a ParkedCallObject before knowing that it is
   // torn down.
   mDtmfEvent(*listenerQ, mSeqNo + DTMF),
   mKeycode(OrbitData::NO_KEYCODE),
   mTransferInProgress(FALSE)
{
   OsDateTime::getCurTime(mParked);
   // Update sNextSeqNo.
   sNextSeqNo = (sNextSeqNo + sSeqNoIncrement) & sSeqNoMask;
}

ParkedCallObject::~ParkedCallObject()
{
   if (mpPlayer)
   {
      mpCallManager->destroyPlayer(MpPlayer::STREAM_PLAYER, mOriginalCallId,
                                   mpPlayer);
   }
}
   

const char* ParkedCallObject::getOriginalAddress()
{
   return mOriginalAddress.data();
}


void ParkedCallObject::setCurrentAddress(const UtlString& address)
{
   mCurrentAddress = address;
}


const char* ParkedCallObject::getCurrentAddress()
{
   return mCurrentAddress.data();
}


const char* ParkedCallObject::getOriginalCallId()
{
   return mOriginalCallId.data();
}


void ParkedCallObject::setCurrentCallId(const UtlString& callId)
{
   mCurrentCallId = callId;
}


const char* ParkedCallObject::getCurrentCallId()
{
   return mCurrentCallId.data();
}


void ParkedCallObject::setPickupCallId(const UtlString& callId)
{
   mPickupCallId = callId;
}


const char* ParkedCallObject::getPickupCallId()
{
   return mPickupCallId.data();
}


const char* ParkedCallObject::getOrbit()
{
   return mOrbit.data();
}
   

void ParkedCallObject::getTimeParked(OsTime& parked)
{
   parked = mParked;
}


bool ParkedCallObject::isPickupCall()
{
   return mbPickup;
}


OsStatus ParkedCallObject::playAudio()
{
   OsStatus result = OS_SUCCESS;

   OsSysLog::add(FAC_PARK, PRI_DEBUG,
                 "CallId %s is requesting to play the audio file",
                 mOriginalCallId.data());

   // Create an audio player and queue up the audio to be played.
   mpCallManager->createPlayer(MpPlayer::STREAM_PLAYER, mOriginalCallId,
                               mFile.data(),
                               STREAM_SOUND_REMOTE | STREAM_FORMAT_WAV,
                               &mpPlayer) ;

   if (mpPlayer == NULL)
   {
      OsSysLog::add(FAC_PARK, PRI_ERR,
                    "CallId %s: Failed to create player",
                    mOriginalCallId.data());
      return OS_FAILED;
   }

   mpPlayer->setLoopCount(-1);    // Play forever.

   if (mpPlayer->realize(TRUE) != OS_SUCCESS)
   {
      OsSysLog::add(FAC_PARK, PRI_ERR,
                    "ParkedCallObject::playAudio - CallId %s: Failed to realize player",
                    mOriginalCallId.data());
      return OS_FAILED;
   }

   if (mpPlayer->prefetch(TRUE) != OS_SUCCESS)
   {
      OsSysLog::add(FAC_PARK, PRI_ERR,
                    "ParkedCallObject::playAudio - CallId %s: Failed to prefetch player",
                    mOriginalCallId.data());
      return OS_FAILED;
   }

   if (mpPlayer->play(FALSE) != OS_SUCCESS)
   {
      OsSysLog::add(FAC_PARK, PRI_ERR,
                    "ParkedCallObject::playAudio - CallId %s: Failed to play",
                    mOriginalCallId.data());
      return OS_FAILED;
   }
   OsSysLog::add(FAC_PARK, PRI_DEBUG, "ParkedCallObject::playAudio - Successful");

   return result;
}


// Activate the escape mechanisms, if the right conditions are present.
// One is the time-out timer, which if it expires, will transfer
// the call back to the user that parked it.
// The other is the escape keycode, which lets the user transfer the call back.
// Neither mechanism is activated if there is no parker URI to transfer back
// to.  Both need appropriate configuration items in the orbits.xml file
// to be activated.
void ParkedCallObject::startEscapeTimer(UtlString& parker,
                                        int timeout,
                                        int keycode)
{
   OsSysLog::add(FAC_PARK, PRI_DEBUG,
                 "ParkedCallObject::startEscapeTimer callId = '%s', "
                 "parker = '%s', timeout = %d, keycode = %d",
                 mOriginalCallId.data(), parker.data(), timeout, keycode);

   // First, check that there is a parker URI.  If not, none of these
   // mechanisms can function.
   if (parker.isNull())
   {
      return;
   }
   // Here, we can insert further validation, such as that the parker URI
   // is local.

   // Save the parker URI.
   mParker = parker;

   if (timeout != OrbitData::NO_TIMEOUT)
   {
      // Set the timeout timer.
      OsTime timeoutOsTime(timeout, 0);
      // Use a periodic timer, so if the transfer generated by one timeout
      // fails, we will try again later.
      mTimeoutTimer.periodicEvery(timeoutOsTime, timeoutOsTime);
   }
   // Remember the keycode for escaping, if any.
   mKeycode = keycode;
   if (mKeycode != OrbitData::NO_KEYCODE)
   {
      // Register the DTMF listener.
      // The "interdigit timeout" time of 1 is just a guess.
      // Enable keyup events, as those are the ones we will act on.
      mpCallManager->enableDtmfEvent(mOriginalCallId.data(), 1,
                                     &mDtmfEvent, false);
   }
}


// Stop the parking escape mechanisms.
void ParkedCallObject::stopEscapeTimer()
{
   OsSysLog::add(FAC_PARK, PRI_DEBUG,
                 "ParkedCallObject::stopEscapeTimer callId = '%s'",
                 mOriginalCallId.data());
   mTimeoutTimer.stop();
   if (mKeycode != OrbitData::NO_KEYCODE)
   {
      // We can't use removeDtmfEvent() here, because it would try to 
      // free mDtmfEvent.
      mpCallManager->disableDtmfEvent(mOriginalCallId.data(),
                                      (int) &mDtmfEvent);
      mKeycode = OrbitData::NO_KEYCODE;
   }
}


// Do a blind transfer of this call to mParker.
void ParkedCallObject::startTransfer()
{
   if (!mTransferInProgress)
   {
      mTransferInProgress = TRUE;
      OsSysLog::add(FAC_PARK, PRI_DEBUG,
                    "ParkedCallObject::startTransfer starting transfer "
                    "callId = '%s', parker = '%s'",
                    mOriginalCallId.data(), mParker.data());
      // Set remoteHoldBeforeTransfer = FALSE, because Polycom phones
      // do not handle re-INVITE well while a DTMF key is down.
      mpCallManager->transfer_blind(mOriginalCallId, mParker, NULL, NULL,
                                    FALSE);
   }
   else
   {
      OsSysLog::add(FAC_PARK, PRI_DEBUG,
                    "ParkedCallObject::startTransfer transfer already in "
                    "progress callId = '%s', parker = '%s'",
                    mOriginalCallId.data(), mParker.data());
   }
}


// Signal that a transfer attempt for a call has ended.
// The transfer may or may not be successful.  (If it was successful,
// one of the UAs will terminate this call soon.)  Re-enable initiating
// transfers.
void ParkedCallObject::clearTransfer()
{
   mTransferInProgress = FALSE;
   OsSysLog::add(FAC_PARK, PRI_DEBUG,
                 "ParkedCallObject::clearTransfer transfer cleared "
                 "callId = '%s'",
                 mOriginalCallId.data());
}


// Process a DTMF keycode for this call.
void ParkedCallObject::keypress(int keycode)
{
   OsSysLog::add(FAC_PARK, PRI_DEBUG,
                 "ParkedCallObject::keypress callId = '%s', parker = '%s', keycode = %d",
                 mOriginalCallId.data(), mParker.data(), keycode);
   // Must test if the keypress is to cause a transfer.
   if (mKeycode != OrbitData::NO_KEYCODE &&
       keycode == mKeycode &&
       !mParker.isNull())
   {
      mpCallManager->transfer_blind(mOriginalCallId, mParker, NULL, NULL);
   }
}


/**
 * Get the ContainableType.
 */
UtlContainableType ParkedCallObject::getContainableType() const
{
   return TYPE;
}