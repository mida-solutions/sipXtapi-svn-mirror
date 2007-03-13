//  
// Copyright (C) 2007 SIPez LLC. 
// Licensed to SIPfoundry under a Contributor Agreement. 
//
// Copyright (C) 2007 SIPfoundry Inc.
// Licensed by SIPfoundry under the LGPL license.
//
// $$
///////////////////////////////////////////////////////////////////////////////

// Author: Dan Petrie <dpetrie AT SIPez DOT com>

// SYSTEM INCLUDES
#include <assert.h>

// APPLICATION INCLUDES
#include <os/OsWriteLock.h>
#include <os/OsReadLock.h>
#include <os/OsDateTime.h>
#include <os/OsSysLog.h>
#include <os/OsTask.h>
#include <mp/MpInputDeviceManager.h>
#include <mp/MpInputDeviceDriver.h>
#include <mp/MpBuf.h>
#include <mp/MpAudioBuf.h>
#include <utl/UtlInt.h>
#ifdef RTL_ENABLED
#   include <rtl_macro.h>
#endif

// EXTERNAL FUNCTIONS
// EXTERNAL VARIABLES
// CONSTANTS
// STATIC VARIABLE INITIALIZATIONS
// PRIVATE CLASSES
/**
*  @brief Private class container for input device buffer and related info
*/
class MpInputDeviceFrameData
{
public:
    MpInputDeviceFrameData()
    : mFrameTime(0)
    {};

    virtual ~MpInputDeviceFrameData()
    {};

    MpAudioBufPtr mFrameBuffer;
    MpFrameTime mFrameTime;
    OsTime mFrameReceivedTime;

private:
      /// Copy constructor (not implemented for this class)
    MpInputDeviceFrameData(const MpInputDeviceFrameData& rMpInputDeviceFrameData);

      /// Assignment operator (not implemented for this class)
    MpInputDeviceFrameData& operator=(const MpInputDeviceFrameData& rhs);
};

/**
*  @brief Private class container for MpInputDeviceDriver pointer and window of buffers
*/
class MpAudioInputConnection : public UtlInt
{
/* //////////////////////////// PUBLIC //////////////////////////////////// */
public:

/* ============================ CREATORS ================================== */
///@name Creators
//@{

     /// Default constructor
   MpAudioInputConnection(MpInputDeviceHandle deviceId,
                          MpInputDeviceDriver& deviceDriver,
                          unsigned int frameBufferLength,
                          unsigned int samplesPerFrame,
                          unsigned int samplesPerSecond,
                          MpBufPool& bufferPool)
   : UtlInt(deviceId)
   , mLastPushedFrame(frameBufferLength - 1)
   , mFrameBufferLength(frameBufferLength)
   , mFrameBuffersUsed(0)
   , mppFrameBufferArray(NULL)
   , mpInputDeviceDriver(&deviceDriver)
   , mSamplesPerFrame(samplesPerFrame)
   , mSamplesPerSecond(samplesPerSecond)
   , mpBufferPool(&bufferPool)
   , mInUse(FALSE)
   {
       assert(mFrameBufferLength > 0);
       assert(mSamplesPerFrame > 0);
       assert(mSamplesPerSecond > 0);
               
       mppFrameBufferArray = new MpInputDeviceFrameData[mFrameBufferLength];
   };


     /// Destructor
   virtual
   ~MpAudioInputConnection()
   {
       OsSysLog::add(FAC_MP, PRI_ERR,"~MpInputDeviceManager start dev: %p id: %d\n",
                     mpInputDeviceDriver, getValue());

       if (mppFrameBufferArray)
       {
           delete[] mppFrameBufferArray;
           mppFrameBufferArray = NULL;
       }
       OsSysLog::add(FAC_MP, PRI_ERR,"~MpInputDeviceManager end\n");
   }

//@}

/* ============================ MANIPULATORS ============================== */
///@name Manipulators
//@{

    OsStatus pushFrame(unsigned int numSamples,
                       MpAudioSample* samples,
                       MpFrameTime frameTime)
    {
        OsStatus result = OS_FAILED;
        assert(samples);

        printf("pushFrame frameTime: %d\n", frameTime);
        // TODO: could support reframing here.  For now
        // the driver must do the correct framing.
        assert(numSamples == mSamplesPerFrame);

        // Circular buffer of frames
        int thisFrameIndex;
        if(mFrameBuffersUsed)
        {
            thisFrameIndex = (++mLastPushedFrame) % mFrameBufferLength;
        }
        else
        {
            thisFrameIndex = 0;
            mLastPushedFrame = 0;
        }
            
        MpInputDeviceFrameData* thisFrameData = &mppFrameBufferArray[thisFrameIndex];

        // Current time to review device driver jitter
        OsDateTime::getCurTimeSinceBoot(thisFrameData->mFrameReceivedTime);

        // Frame time slot the driver says this is targeted for
        thisFrameData->mFrameTime = frameTime;

        // Make sure we have someplace we can stuff the data
        if (!thisFrameData->mFrameBuffer.isValid())
        {
            thisFrameData->mFrameBuffer = 
                mpBufferPool->getBuffer();
            mFrameBuffersUsed++;
            if(mFrameBuffersUsed > mFrameBufferLength)
            {
                mFrameBuffersUsed = mFrameBufferLength;
            }
        }

        assert(thisFrameData->mFrameBuffer->getSamplesNumber() >= numSamples);

        // Stuff the data in a buffer
        if (thisFrameData->mFrameBuffer.isValid())
        {
            memcpy(thisFrameData->mFrameBuffer->getSamples(), samples, numSamples);
            thisFrameData->mFrameBuffer->setSamplesNumber(numSamples);
            thisFrameData->mFrameBuffer->setSpeechType(MpAudioBuf::MP_SPEECH_UNKNOWN);
            result = OS_SUCCESS;
        }
        else
        {
            assert(0);
        }

        return result;
    };


    OsStatus getFrame(MpFrameTime frameTime,
                      MpBufPtr& buffer,
                      unsigned& numFramesBefore,
                      unsigned& numFramesAfter)
    {
        OsStatus result = OS_INVALID_STATE;

        // Need to look for the frame even if the device is disabled
        // as it may already be queued up
        if (mpInputDeviceDriver && mpInputDeviceDriver->isEnabled())
        {
            result = OS_NOT_FOUND;
        }
        else
        {
            printf("getFrame invalid device: %d\n", getValue());
        }

        unsigned int lastFrame = mLastPushedFrame;
        printf("getFrame lastFrame: %d mFrameBuffersUsed: %d\n", lastFrame, mFrameBuffersUsed);
        numFramesBefore = 0;
        numFramesAfter = 0;
        int framePeriod = 1000 * mSamplesPerFrame / mSamplesPerSecond;

        // When requesting a frame we provide the frame that overlaps the
        // given frame time.  The frame time is for the beginning of a frame.
        // So we provide the frame that begins at or less than the requested
        // time, but not more than one frame period older.
        unsigned int frameIndex;
        for (frameIndex = 0; frameIndex < mFrameBuffersUsed; frameIndex++)
        {
            // Walk backwards from the last inserted frame
            MpInputDeviceFrameData* frameData = 
                &mppFrameBufferArray[(lastFrame - frameIndex) % mFrameBufferLength];

            // The frame whose time range covers the requested time
            if (frameData->mFrameBuffer.isValid() &&
                frameData->mFrameTime <= frameTime &&
                frameData->mFrameTime + framePeriod > frameTime)
            {
                // We have a frame of media for the requested time
                numFramesBefore = frameIndex;
                numFramesAfter = mFrameBuffersUsed - 1 - frameIndex;

                // We always make a copy of the frame as we are typically
                // crossing task boundaries here.
                buffer = frameData->mFrameBuffer.clone();
                result = OS_SUCCESS;
                break;
            }
        }

        if(result == OS_SUCCESS)
        {
            printf("getFrame got frame: %d\n", frameIndex);
        }
        return(result);
    }; 

//@}

/* ============================ ACCESSORS ================================= */
///@name Accessors
//@{

    MpInputDeviceDriver* getDeviceDriver() const{return(mpInputDeviceDriver);};


    unsigned getTimeDerivatives(unsigned nDerivatives, 
                                int*& derivativeBuf)
    {
        unsigned nActualDerivs = 0;
        
        int referenceFramePeriod = 1000 * mSamplesPerFrame / mSamplesPerSecond;
        unsigned int lastFrame = mLastPushedFrame;

        unsigned int t2FrameIdx;
        for(t2FrameIdx = 0; 
            (t2FrameIdx < mFrameBuffersUsed) && (nActualDerivs < nDerivatives); 
            t2FrameIdx++)
        {
            // in indexes here, higher is older, since we're subtracting before
            // modding to get the actual buffer array index.
            unsigned int t1FrameIdx = t2FrameIdx+1;

            MpInputDeviceFrameData* t2FrameData = 
                &mppFrameBufferArray[(lastFrame - t2FrameIdx) 
                                     % mFrameBufferLength];
            MpInputDeviceFrameData* t1FrameData = 
                &mppFrameBufferArray[(lastFrame - t1FrameIdx) 
                                     % mFrameBufferLength];

            // The first time we find an invalid buffer, break out of the loop.
            // This takes care of the case when only a small amount of data has
            // been pushed to the buffer.
            if(!t2FrameData->mFrameBuffer.isValid() ||
                !t1FrameData->mFrameBuffer.isValid())
            {
                break;
            }

            long t2OsMsecs = t2FrameData->mFrameReceivedTime.cvtToMsecs();
            long t1OsMsecs = t1FrameData->mFrameReceivedTime.cvtToMsecs();
            int curDeriv = (t2OsMsecs - t1OsMsecs) / referenceFramePeriod;
            derivativeBuf[t2FrameIdx] = curDeriv;
            nActualDerivs++;
        }

        return nActualDerivs;
    }

    inline void setInUse() { mInUse = TRUE; }
    inline void clearInUse() { mInUse = FALSE; }
    inline UtlBoolean isInUse() { return mInUse; }

//@}

/* ============================ INQUIRY =================================== */
///@name Inquiry
//@{

//@}

/* //////////////////////////// PRIVATE /////////////////////////////////// */
private:
    unsigned int mLastPushedFrame;    ///< Index of last pushed frame in mppFrameBufferArray.
    unsigned int mFrameBufferLength;  ///< Length of mppFrameBufferArray.
    unsigned int mFrameBuffersUsed;   ///< actual number of buffers with data in them
    MpInputDeviceFrameData* mppFrameBufferArray;
    MpInputDeviceDriver* mpInputDeviceDriver;
    unsigned int mSamplesPerFrame;    ///< Number of audio samples in one frame.
    unsigned int mSamplesPerSecond;   ///< Number of audio samples in one second.
    MpBufPool* mpBufferPool;          
    UtlBoolean mInUse;                ///< Use indicator to synchronize disable and remove.

      /// Copy constructor (not implemented for this class)
    MpAudioInputConnection(const MpAudioInputConnection& rMpAudioInputConnection);

      /// Assignment operator (not implemented for this class)
    MpAudioInputConnection& operator=(const MpAudioInputConnection& rhs);
};


//               MpInputDeviceManager implementation

/* //////////////////////////// PUBLIC //////////////////////////////////// */

/* ============================ CREATORS ================================== */

// Constructor
MpInputDeviceManager::MpInputDeviceManager(unsigned defaultSamplesPerFrame, 
                                           unsigned defaultSamplesPerSec,
                                           unsigned defaultNumBufferedFrames,
                                           MpBufPool& bufferPool)
: mRwMutex(OsRWMutex::Q_PRIORITY)
, mLastDeviceId(0)
, mDefaultSamplesPerFrame(defaultSamplesPerFrame)
, mDefaultSamplesPerSecond(defaultSamplesPerSec)
, mDefaultNumBufferedFrames(defaultNumBufferedFrames)
, mpBufferPool(&bufferPool)
{
    assert(defaultSamplesPerFrame > 0);
    assert(defaultSamplesPerSec > 0);
    assert(defaultNumBufferedFrames > 0);

    OsDateTime::getCurTimeSinceBoot(mTimeZero);
}


// Destructor
MpInputDeviceManager::~MpInputDeviceManager()
{
}

/* ============================ MANIPULATORS ============================== */
int MpInputDeviceManager::addDevice(MpInputDeviceDriver& newDevice)
{
    OsWriteLock lock(mRwMutex);

    MpInputDeviceHandle newDeviceId = ++mLastDeviceId;
    // Tell the device what id to use when pushing frames to this

    newDevice.setDeviceId(newDeviceId);

    // Create a connection to contain the device and its buffered frames
    MpAudioInputConnection* connection = 
        new MpAudioInputConnection(newDeviceId,
                                   newDevice, 
                                   mDefaultNumBufferedFrames,
                                   mDefaultSamplesPerFrame,
                                   mDefaultSamplesPerSecond,
                                   *mpBufferPool);

    // Map by device name string
    UtlInt* idValue = new UtlInt(newDeviceId);
    OsSysLog::add(FAC_MP, PRI_ERR,"MpInputDeviceManager::addDevice dev: %p value: %p id: %d\n", newDevice, idValue, newDeviceId);
    mConnectionsByDeviceName.insertKeyAndValue(&newDevice, idValue);

    // Map by device ID
    mConnectionsByDeviceId.insert(connection);

    return(newDeviceId);
}


MpInputDeviceDriver* MpInputDeviceManager::removeDevice(MpInputDeviceHandle deviceId)
{
    // We need the manager lock while we're indicating the connection is in use.
    mRwMutex.acquireWrite();

    MpAudioInputConnection* connectionFound = NULL;
    UtlInt deviceKey(deviceId);
    MpInputDeviceDriver* deviceDriver = NULL;

    int checkInUseTries = 10;
    for(; checkInUseTries > 0; checkInUseTries--)
    {
        connectionFound =
            (MpAudioInputConnection*) mConnectionsByDeviceId.find(&deviceKey);
        if(connectionFound && connectionFound->isInUse())
        {
            // If the device is in use, release the manager lock,
            // wait a small bit, and get the lock again to give the manager a
            // chance to finish what it was doing  with the connection.
            mRwMutex.releaseWrite();
            OsTask::delay(1);
            mRwMutex.acquireWrite();
        }
        else if(connectionFound)
        {
            break;
        }
    }

    if(connectionFound && !connectionFound->isInUse())
    {
        connectionFound->setInUse();

        // Remove from the id indexed container
        mConnectionsByDeviceId.remove(connectionFound);

        deviceDriver = connectionFound->getDeviceDriver();
        assert(deviceDriver);

        // Get the int value mapped in the hash so we can clean up
        UtlInt* deviceIdInt =
            (UtlInt*) mConnectionsByDeviceName.findValue(deviceDriver);

        // Remove from the name indexed hash
        mConnectionsByDeviceName.remove(deviceDriver);
        if (deviceIdInt)
        {
            OsSysLog::add(FAC_MP, PRI_ERR,"MpInputDeviceManager::addDevice dev: %p int: %p id: %d\n", deviceDriver, deviceIdInt, deviceIdInt->getValue());
            delete deviceIdInt;
            deviceIdInt = NULL;
        }

        delete connectionFound;
        connectionFound = NULL;
    }

    // Note: we specifically keep the manager lock for the entire duration 
    //       of removal (with exception of waiting while it's in use).
    mRwMutex.releaseWrite();

    // deviceDriver of NULL is returned if:
    //    * The connection is not found.
    //    * The connection is found, but the connection was already in use.
    return(deviceDriver);
}


OsStatus MpInputDeviceManager::enableDevice(MpInputDeviceHandle deviceId)
{
    OsStatus status = OS_NOT_FOUND;
    OsWriteLock lock(mRwMutex);

    MpAudioInputConnection* connectionFound = NULL;
    UtlInt deviceKey(deviceId);
    connectionFound =
        (MpAudioInputConnection*) mConnectionsByDeviceId.find(&deviceKey);
    MpInputDeviceDriver* deviceDriver = NULL;

    if (connectionFound)
    {
        deviceDriver = connectionFound->getDeviceDriver();
        assert(deviceDriver);
        if (deviceDriver)
        {
            status = 
                deviceDriver->enableDevice(mDefaultSamplesPerFrame, 
                                           mDefaultSamplesPerSecond,
                                           getCurrentFrameTime());
        }
    }
    return(status);
}


OsStatus MpInputDeviceManager::disableDevice(MpInputDeviceHandle deviceId)
{
    // We need the manager lock while we're indicating the connection is in use.
    mRwMutex.acquireWrite();

    OsStatus status = OS_NOT_FOUND;
    // We haven't determined if it's ok to disable the device yet.
    UtlBoolean okToDisable = FALSE;  
    MpAudioInputConnection* connectionFound = NULL;
    UtlInt deviceKey(deviceId);
    MpInputDeviceDriver* deviceDriver = NULL;

    int checkInUseTries = 10;
    int i;
    for(i = 0; i < checkInUseTries; i--)
    {
        connectionFound =
            (MpAudioInputConnection*) mConnectionsByDeviceId.find(&deviceKey);
        if(connectionFound && connectionFound->isInUse())
        {
            // If the device is in use, release the manager lock,
            // wait a small bit, and get the lock again to give the manager a
            // chance to finish what it was doing  with the connection.
            mRwMutex.releaseWrite();
            OsTask::delay(1);
            mRwMutex.acquireWrite();
        }
        else if(connectionFound)
        {
            break;
        }
    }
    if(connectionFound)
    {
        if(connectionFound->isInUse())
        {
            // If the connection is in use by someone else,
            // then we indicate that the connection is busy.
            status = OS_BUSY;
        }
        else
        {
            // It's ok to disable now,
            // Set the connection in use.
            okToDisable = TRUE;
            connectionFound->setInUse();
        }
    }

    // Now we release the mutex and go on to actually disable the device.
    mRwMutex.releaseWrite();

    if (okToDisable)
    {
        deviceDriver = connectionFound->getDeviceDriver();
        assert(deviceDriver);
        if (deviceDriver)
        {
            status = 
                deviceDriver->disableDevice();
        }

        mRwMutex.acquireWrite();
        connectionFound->clearInUse();
        mRwMutex.releaseWrite();
    }

    return(status);
}


OsStatus MpInputDeviceManager::pushFrame(MpInputDeviceHandle deviceId,
                                         unsigned numSamples,
                                         MpAudioSample* samples,
                                         MpFrameTime frameTime)
{
    OsStatus status = OS_NOT_FOUND;
#ifdef RTL_ENABLED
    RTL_EVENT("MpInputDeviceManager.pushFrame", deviceId);
#endif

    OsWriteLock lock((OsRWMutex&)mRwMutex);

    MpAudioInputConnection* connectionFound = NULL;
    UtlInt deviceKey(deviceId);
    connectionFound =
        (MpAudioInputConnection*) mConnectionsByDeviceId.find(&deviceKey);

    if (connectionFound)
    {
        status = 
            connectionFound->pushFrame(numSamples, samples, frameTime);
    }

#ifdef RTL_ENABLED
    RTL_EVENT("MpInputDeviceManager.pushFrame", 0);
#endif
    return(status);
}


OsStatus MpInputDeviceManager::getFrame(MpInputDeviceHandle deviceId,
                                        MpFrameTime frameTime,
                                        MpBufPtr& buffer,
                                        unsigned& numFramesBefore,
                                        unsigned& numFramesAfter)
{
    OsStatus status = OS_INVALID_ARGUMENT;
#ifdef RTL_ENABLED
    RTL_EVENT("MpInputDeviceManager.getFrame", deviceId);
#endif

    OsWriteLock lock((OsRWMutex&)mRwMutex);

    MpAudioInputConnection* connectionFound = NULL;
    UtlInt deviceKey(deviceId);
    connectionFound =
        (MpAudioInputConnection*) mConnectionsByDeviceId.find(&deviceKey);

    if (connectionFound)
    {
        status = 
            connectionFound->getFrame(frameTime,
            buffer,
            numFramesBefore,
            numFramesAfter);
    }

#ifdef RTL_ENABLED
    RTL_EVENT("MpInputDeviceManager.getFrame", 0);
#endif
    return(status);
}

/* ============================ ACCESSORS ================================= */

OsStatus MpInputDeviceManager::getDeviceName(MpInputDeviceHandle deviceId,
                                             UtlString& deviceName) const
{
    OsStatus status = OS_NOT_FOUND;

    OsReadLock lock((OsRWMutex&)mRwMutex);

    MpAudioInputConnection* connectionFound = NULL;
    UtlInt deviceKey(deviceId);
    connectionFound =
        (MpAudioInputConnection*) mConnectionsByDeviceId.find(&deviceKey);
    MpInputDeviceDriver* deviceDriver = NULL;

    if (connectionFound)
    {
        deviceDriver = connectionFound->getDeviceDriver();
        assert(deviceDriver);
        if (deviceDriver)
        {
            status = OS_SUCCESS;
            deviceName = *deviceDriver;
        }
    }
    return(status);
}


MpInputDeviceHandle MpInputDeviceManager::getDeviceId(const char* deviceName) const
{
    OsStatus status = OS_NOT_FOUND;


    OsWriteLock lock((OsRWMutex&)mRwMutex);

    UtlString deviceString(deviceName);
    UtlInt* deviceId
        = (UtlInt*) mConnectionsByDeviceName.find(&deviceString);

    return(deviceId ? deviceId->getValue() : -1);
}


MpFrameTime MpInputDeviceManager::getCurrentFrameTime() const
{
    OsTime now;
    OsDateTime::getCurTimeSinceBoot(now);

    now -= mTimeZero;


    return(now.seconds() * 1000 + now.usecs() / 1000);
}

OsStatus MpInputDeviceManager::getTimeDerivatives(MpInputDeviceHandle deviceId,
                                                  unsigned& nDerivatives,
                                                  int*& derivativeBuf) const
{
    OsStatus stat = OS_INVALID_ARGUMENT;
    unsigned nActualDerivs = 0;
    OsReadLock lock((OsRWMutex&)mRwMutex);

    MpAudioInputConnection* connectionFound = NULL;
    UtlInt deviceKey(deviceId);
    connectionFound =
        (MpAudioInputConnection*) mConnectionsByDeviceId.find(&deviceKey);

    if (connectionFound)
    {
        stat = OS_SUCCESS;
        nActualDerivs = connectionFound->getTimeDerivatives(nDerivatives, 
                                                            derivativeBuf);
    }
    else
    {
        printf("MpInputDeviceManager::pushFrame device(%d) not found\n", deviceId);
    }
    nDerivatives = nActualDerivs;
    return(stat);
}

/* ============================ INQUIRY =================================== */

UtlBoolean MpInputDeviceManager::isDeviceEnabled(MpInputDeviceHandle deviceId)
{
    OsStatus status = OS_NOT_FOUND;
    UtlBoolean enabledState = FALSE;
    OsReadLock lock(mRwMutex);

    MpAudioInputConnection* connectionFound = NULL;
    UtlInt deviceKey(deviceId);
    connectionFound =
        (MpAudioInputConnection*) mConnectionsByDeviceId.find(&deviceKey);
    MpInputDeviceDriver* deviceDriver = NULL;

    if (connectionFound)
    {
        deviceDriver = connectionFound->getDeviceDriver();
        assert(deviceDriver);
        if (deviceDriver)
        {
            enabledState = 
                deviceDriver->isEnabled();
        }
    }
    return(enabledState);
}

/* //////////////////////////// PROTECTED ///////////////////////////////// */

/* //////////////////////////// PRIVATE /////////////////////////////////// */

/* ============================ FUNCTIONS ================================= */
