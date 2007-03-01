// 
// 
// Copyright (C) 2005 SIPfoundry Inc.
// Licensed by SIPfoundry under the LGPL license.
// 
// Copyright (C) 2005 SIPez LLC.
// Licensed to SIPfoundry under a Contributor Agreement.
//
// Copyright (C) 2005 Pingtel Corp.
// Licensed to SIPfoundry under a Contributor Agreement.
// 
// $$
//////////////////////////////////////////////////////////////////////////////

#ifndef REDIRECTRESUMEMSG_H
#define REDIRECTRESUMEMSG_H

// SYSTEM INCLUDES

// APPLICATION INCLUDES
#include "os/OsMsg.h"
#include "SipRedirector.h"

// DEFINES
// MACROS
// EXTERNAL FUNCTIONS
// EXTERNAL VARIABLES
// CONSTANTS
// STRUCTS
// TYPEDEFS
// FORWARD DECLARATIONS

// Message to the redirect server to resume processing a redirection request.
class RedirectResumeMsg : public OsMsg
{
public:
   
   /** Message type code.
    */
   static const int REDIRECT_RESTART = USER_START;

   /** Construct a message saying that redirector redirectorNo is willing to
    * reprocess request seqNo.
    */
   RedirectResumeMsg(RequestSeqNo seqNo,
                     int redirectorNo);

   /**
    * Copy this message.
    */
   virtual RedirectResumeMsg* createCopy(void) const;

   /** Get the sequence number
    */
   inline RequestSeqNo getRequestSeqNo() const
   {
      return mSeqNo;
   }

   /** Get the redirector number
    */
   inline RequestSeqNo getRedirectorNo() const
   {
      return mRedirectorNo;
   }

private:

   /** Sequence number of the request
    */
   RequestSeqNo mSeqNo;

   /** Number of the redirector
    */
   int mRedirectorNo;
};

#endif /*  REDIRECTRESUMEMSG_H */