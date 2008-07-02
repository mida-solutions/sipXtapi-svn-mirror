//  
// Copyright (C) 2006-2008 SIPez LLC. 
// Licensed to SIPfoundry under a Contributor Agreement. 
//
// Copyright (C) 2004-2008 SIPfoundry Inc.
// Licensed by SIPfoundry under the LGPL license.
//
// Copyright (C) 2004-2006 Pingtel Corp.  All rights reserved.
// Licensed to SIPfoundry under a Contributor Agreement.
//
// $$
///////////////////////////////////////////////////////////////////////////////

#ifndef _MprDejitter_h_
#define _MprDejitter_h_

// SYSTEM INCLUDES
// APPLICATION INCLUDES
#include "os/OsStatus.h"
#include "os/OsBSem.h"
#include "mp/MpRtpBuf.h"

// DEFINES
// MACROS
// EXTERNAL FUNCTIONS
// EXTERNAL VARIABLES
// CONSTANTS
// STRUCTS
// TYPEDEFS
// FORWARD DECLARATIONS

/// The "Dejitter" media processing resource
class MprDejitter
{
/* //////////////////////////// PUBLIC //////////////////////////////////// */
public:

   enum {
      MAX_RTP_PACKETS = 512   ///< Could be any value, power of 2 is desired.
   };

/* ============================ CREATORS ================================== */
///@name Creators
//@{

     /// Constructor
   MprDejitter();

     /// Destructor
   virtual
   ~MprDejitter();

//@}

/* ============================ MANIPULATORS ============================== */
///@name Manipulators
//@{

     /// Add an incoming RTP packet to the dejitter pool
   OsStatus pushPacket(const MpRtpBufPtr &pRtp);
     /**<
     *  This method places the packet to the pool depending the modulo division
     *  value.
     *
     *  @return OS_SUCCESS on success
     *  @return OS_LIMIT_REACHED if too many codecs used in incoming RTP packets
     */

     /// Get next RTP packet, or NULL if none is available.
   MpRtpBufPtr pullPacket();
     /**<
     *  This buffer is the primary dejitter/reorder buffer for the internal
     *  codecs. Some codecs may do their own dejitter stuff too. But we can't
     *  eliminate this buffer because then out-of-order packets would just be
     *  dumped on the ground.
     *
     *  This buffer does NOT substitute silence packets. That is done in
     *  MpJitterBuffer called from MprDecode.
     *
     *  If packets arrive out of order, and the newer packet has already been
     *  pulled due to the size of the jitter buffer set by the codec, this
     *  buffer will NOT discard the out-of-order packet, but send it along
     *  anyway it is up to the codec to discard the packets it cannot use. This
     *  allows this JB to be a no-op buffer for when the commercial library is
     *  used.
     *
     *  If pulled packet belong to signaling codec (e.g. RFC2833 DTMF), then
     *  set isSignaling to true. Else packet will be hold for undefined
     *  amount of time, possible forever.
     */

     /// Get next RTP packet with given timestamp, or NULL if none is available.
   MpRtpBufPtr pullPacket(RtpTimestamp timestamp,
                          UtlBoolean *nextFrameAvailable = NULL,
                          bool lockTimestamp=true);
     /**<
     *  This version of pullPacket() works exactly the same as above version
     *  of pullPacket() with one exception: if (lockTimestamp == true) it checks 
     *  every found packet's timestamp. And return NULL if there are no packets
     *  with timestamp less or equal then passed timestamp.
     *
     *  If pulled packet belong to signaling codec (e.g. RFC2833 DTMF), then
     *  set isSignaling to true. Else packet will be hold for undefined
     *  amount of time, possible forever.
     */

//@}

/* ============================ ACCESSORS ================================= */
///@name Accessors
//@{

      /// Get number of packets in buffer, arrived in time.
   inline int getNumPackets() const;

      /// Get number of late packets in buffer.
   inline int getNumLatePackets() const;

//@}

/* ============================ INQUIRY =================================== */
///@name Inquiry
//@{

//@}

/* //////////////////////////// PROTECTED ///////////////////////////////// */
protected:

/* //////////////////////////// PRIVATE /////////////////////////////////// */
private:

   /// Storage for all stream related data.
   /**
   *  We're able to handle several RTP streams with one instance of dejitter.
   *  This structure encapsulate all data, specific to stream as opposite to
   *  global dejitter data.
   */
   struct StreamData
   {
       /// Constructor, initialize data to meaningful initial state.
      StreamData()
      : mNumPackets(0)
      , mNumLatePackets(0)
      , mNumDiscarded(0)
      , mLastPushed(0)
      , mIsFirstPulledPacket(TRUE)
      {
      }

                     /// Buffer for incoming RTP packets
      MpRtpBufPtr   mpPackets[MAX_RTP_PACKETS];
                     /// Number of packets in buffer, arrived in time.
      int           mNumPackets;
                     /// Number of packets in buffer, arrived late.
      int           mNumLatePackets;
                     /// Number of packets overwritten with newly came packets.
      int           mNumDiscarded;
                     /// Index of the last inserted packet.
      int           mLastPushed;
                     /// Have we returned first RTP packet or not?
      UtlBoolean    mIsFirstPulledPacket;
                     /// Keep track of the last sequence number returned, so that
                     /// we can distinguish out-of-order packets.
      RtpSeq        mMaxPulledSeqNo;
   };

                  /// Timestamp of frame we expect next
   RtpTimestamp  mNextPullTimerCount;
                  /**<
                  *  This is kept global, because we should keep all streams
                  *  in sync.
                  */

                  /// Storage for stream related data
   StreamData    mStreamData;

     /// Copy constructor (not implemented for this class)
   MprDejitter(const MprDejitter& rMprDejitter);

     /// Assignment operator (not implemented for this class)
   MprDejitter& operator=(const MprDejitter& rhs);

};

/* ============================ INLINE METHODS ============================ */

int MprDejitter::getNumPackets() const
{
   return mStreamData.mNumPackets;
}

int MprDejitter::getNumLatePackets() const
{
   return mStreamData.mNumLatePackets;
}

#endif  // _MprDejitter_h_
