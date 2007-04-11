//  
// Copyright (C) 2007 SIPez LLC. 
// Licensed to SIPfoundry under a Contributor Agreement. 
//
// Copyright (C) 2007 SIPfoundry Inc.
// Licensed by SIPfoundry under the LGPL license.
//
// $$
///////////////////////////////////////////////////////////////////////////////

// Author: Alexander Chemeris <Alexander DOT Chemeris AT SIPez DOT com>

#ifndef _MpOutputDeviceDriver_h_
#define _MpOutputDeviceDriver_h_

// SYSTEM INCLUDES
//#include <utl/UtlDefs.h>
#include <os/OsStatus.h>
#include <utl/UtlString.h>

// APPLICATION INCLUDES
#include "mp/MpTypes.h"

// DEFINES
// MACROS
// EXTERNAL FUNCTIONS
// EXTERNAL VARIABLES
// CONSTANTS
// STRUCTS
// TYPEDEFS
// FORWARD DECLARATIONS
class MpOutputDeviceManager;

/**
*  @brief Container for device specific output driver.
*
*  The MpOutputDeviceDriver is the abstract base class for the implementations
*  of output media drivers.  An instance of MpOutputDeviceDriver is created for
*  every physical or logical input device (e.g. speaker).  A driver is
*  instantiated and then added to the MpOutputDeviceManager.  The driver must
*  be enabled via the MpOutputDeviceManager to begin consuming frames.
*
*  Each audio output driver may be used in two modes: direct write mode and
*  non direct write (mixer) mode. In direct write mode data is pushed to device
*  as soon as it become available. In mixer mode data from several sources will
*  be buffered and mixed inside connection and pushed to device only when
*  mixer buffer become full. Direct write mode have less latency, but could be
*  fed by one source only. If two or more sources will try to push data, only
*  first will succeed. In opposite to direct write mode, mixer mode is supposed
*  to accept several sreams and mix them. In this mode device should provide
*  timer which will notify MpAudioOutputConnection when device is ready to
*  accept next frame of data.
*
*  MpOutputDeviceDriver has a text name which is defined upon construction.
*  This name will typically be the same as the OS defined name for the
*  input device.  The name of the MpOutputDeviceDriver is accessed via the
*  data() method inherited from UtlString.  This allows MpOutputDeviceDriver
*  to be contained and accessed by name.
*/
class MpOutputDeviceDriver : public UtlString
{
/* //////////////////////////// PUBLIC //////////////////////////////////// */
public:

/* ============================ CREATORS ================================== */
///@name Creators
//@{

     /// Default constructor.
   explicit
   MpOutputDeviceDriver(const UtlString& name);
     /**<
     *  @param name - (in) unique device driver name (e.g. "/dev/dsp", 
     *         "YAMAHA AC-XG WDM Audio", etc.)
     */

     /// Destructor.
   virtual
   ~MpOutputDeviceDriver();

//@}

/* ============================ MANIPULATORS ============================== */
///@name Manipulators
//@{

     /// Initialize device driver and state.
   virtual OsStatus enableDevice(unsigned samplesPerFrame, 
                                 unsigned samplesPerSec,
                                 MpFrameTime currentFrameTime,
                                 UtlBoolean enableDirectWriteMode) = 0;
     /**<
     *  This method enables the device driver.
     *
     *  @NOTE this SHOULD NOT be used to mute/unmute a device. Disabling and
     *  enabling a device results in state and buffer queues being cleared.
     *
     *  @param samplesPerFrame - (in) the number of samples in a frame of media
     *  @param samplesPerSec - (in) sample rate for media frame in samples per second
     *  @param currentFrameTime - (in) time in milliseconds for beginning of frame
     *         relative to the MpOutputDeviceManager reference time
     *  @param enableDirectWriteMode - (in) pass TRUE to enable direct write
     *         mode. In this mode data is pushed to device as soon as it become
     *         available. In non direct (mixer) write mode data will be pulled
     *         by device itself.
     *
     *  @returns OS_INVALID_STATE if device already enabled.
     *
     *  @NOTE This method is supposed to be used from MpAudioOutputConnection only.
     *        If you want enable device, use MpOutputDeviceManager or
     *        MpAudioOutputConnection methods.
     */

     /// Uninitialize device driver.
   virtual OsStatus disableDevice() = 0;
     /**<
     *  This method disables the device driver and should release any
     *  platform device resources so that the device might be used else where.
     *
     *  @NOTE this SHOULD NOT be used to mute/unmute a device. Disabling and
     *        enabling a device results in state and buffer queues being cleared.
     *
     *  @NOTE This method is supposed to be used from MpAudioOutputConnection only.
     *        If you want disable device, use MpOutputDeviceManager or
     *        MpAudioOutputConnection methods.
     */

     /// @brief Send data to output device.
   virtual
   OsStatus pushFrame(unsigned int numSamples,
                      MpAudioSample* samples) = 0;
     /**<
     *  This method is usually called from MpAudioOutputConnection::pushFrame().
     *
     *  @param numSamples - (in) Number of samples in <tt>samples</tt> array.
     *  @param samples - (in) Array of samples to push to device.
     */

     /// Set device ID associated with this device in parent input device manager.
//   virtual OsStatus setDeviceId(MpOutputDeviceHandle deviceId);
     /**<
     *  @NOTE This method is supposed to be used only inside MpAudioOutputConnection.
     *        Normally it used only once in its constructor to associate device
     *        driver to connection.
     */

     /// Clear the device ID associated with this device.
//   virtual OsStatus clearDeviceId();
     /**<
     *  @NOTE This method is supposed to be used only inside MpAudioOutputConnection.
     *        Normally it used only once in its destructor to remove association
     *        of device driver with connection.
     */

//@}

/* ============================ ACCESSORS ================================= */
///@name Accessors
//@{

     /// Get device manager which is used to access to this device driver.
   inline
   MpOutputDeviceManager *getDeviceManager() const;

     /// Get device ID associated with this device in parent input device manager.
//   inline MpOutputDeviceHandle getDeviceId() const;

     /// Calculate the number of milliseconds that a frame occupies in time.
   inline
   MpFrameTime getFramePeriod() const;

     /// Get number of samples in a frame.
   inline
   unsigned getSamplesPerFrame() const;

     /// Get number of samples per second.
   inline
   unsigned getSamplesPerSec() const;


//@}

     /// Calculate the number of milliseconds that a frame occupies in time. 
   static inline
   MpFrameTime getFramePeriod(unsigned samplesPerFrame,
                              unsigned samplesPerSec);

/* ============================ INQUIRY =================================== */
///@name Inquiry
//@{

     /// Inquire if this driver is enabled
   virtual UtlBoolean isEnabled();

//@}

/* //////////////////////////// PROTECTED ///////////////////////////////// */
protected:

   MpOutputDeviceManager* mpDeviceManager;  ///< The object that manages 
                   ///< this device driver.
   UtlBoolean mIsEnabled;         ///< Whether this device driver is enabled or not.
//   MpOutputDeviceHandle mDeviceId; ///< The logical device ID that identifies 
                   ///< this device, as supplied by the InputDeviceManager.
   unsigned mSamplesPerFrame;     ///< Device produce audio frame with this
                   ///< number of samples.
   unsigned mSamplesPerSec;       ///< Device produce audio with this number
                   ///< of samples per second.
   

/* //////////////////////////// PRIVATE /////////////////////////////////// */
private:

     /// Copy constructor (not implemented for this class)
   MpOutputDeviceDriver(const MpOutputDeviceDriver& rMpOutputDeviceDriver);

     /// Assignment operator (not implemented for this class)
   MpOutputDeviceDriver& operator=(const MpOutputDeviceDriver& rhs);
};


/* ============================ INLINE METHODS ============================ */

MpOutputDeviceManager *MpOutputDeviceDriver::getDeviceManager() const
{
   return mpDeviceManager;
}

MpFrameTime MpOutputDeviceDriver::getFramePeriod() const
{
   return getFramePeriod(mSamplesPerFrame, mSamplesPerSec);
}
/*
MpOutputDeviceHandle MpOutputDeviceDriver::getDeviceId() const
{
   return mDeviceId;
}
*/

MpFrameTime MpOutputDeviceDriver::getFramePeriod(unsigned samplesPerFrame,
                                                 unsigned samplesPerSec)
{
   return (1000*samplesPerFrame)/samplesPerSec;
}

unsigned MpOutputDeviceDriver::getSamplesPerFrame() const
{
   return mSamplesPerFrame;
}

unsigned MpOutputDeviceDriver::getSamplesPerSec() const
{
   return mSamplesPerSec;
}

#endif  // _MpOutputDeviceDriver_h_