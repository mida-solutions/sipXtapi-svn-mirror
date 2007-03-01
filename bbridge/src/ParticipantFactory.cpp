#include "rutil/Log.hxx"
#include "rutil/Logger.hxx"
#include "resip/dum/ServerInviteSession.hxx"
#include "resip/dum/Handles.hxx"
#include "resip/stack/Aor.hxx"
#include "resip/stack/SdpContents.hxx"
#include "resip/stack/Headers.hxx"
#include "resip/stack/RequestLine.hxx"
#include "resip/stack/MethodTypes.hxx"
#include "rutil/DnsUtil.hxx"
#include "net/SdpCodec.h"

#include "ParticipantFactory.h"
#include "Participant.h"
#include "ConferenceSubscriptionApp.h"
#include "Subsystem.h"

#define RESIPROCATE_SUBSYSTEM bbridge::Subsystem::BBRIDGE

using namespace bbridge;
using namespace std;

resip::AppDialogSet* 
ParticipantFactory::createAppDialogSet(resip::DialogUsageManager& dum,
                                       const resip::SipMessage& msg)
{
   // Depending on the method, create the correct application data object
   // for this dialog.
   if (msg.header(resip::h_RequestLine).getMethod() == resip::SUBSCRIBE)
   {
      InfoLog (<< "Making a conference subscription");
      return new ConferenceSubscriptionApp(dum, msg);
   }
   else
   {
      InfoLog (<< "Making a participant");
      return new Participant(dum, msg);
   }
}

/*
  Copyright (c) 2005, Jason Fischl, Adam Roach
  All rights reserved.

  Redistribution and use in source and binary forms, with or without modification,
  are permitted provided that the following conditions are met:

  * Redistributions of source code must retain the above copyright notice, this
    list of conditions and the following disclaimer.
  * Redistributions in binary form must reproduce the above copyright notice,
    this list of conditions and the following disclaimer in the documentation
    and/or other materials provided with the distribution.
  * Neither the name of PurpleComm, Inc. nor the names of its contributors may
    be used to endorse or promote products derived from this software without
    specific prior written permission.

  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
  AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
  IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
  DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE
  FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
  DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
  SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
  CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
  OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
  OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/