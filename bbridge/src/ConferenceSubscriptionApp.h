#ifndef __CONFERENCE_SUBSCRIPTION_HANDLER_H
#define __CONFERENCE_SUBSCRIPTION_HANDLER_H

#include "resip/dum/SubscriptionHandler.hxx"
#include "resip/dum/Handles.hxx"
#include "Conference.h"

namespace bbridge
{

// DialogSet application information object for subscriptions to the conference
// event package.
class ConferenceSubscriptionApp : public resip::AppDialogSet
{
   public:
      ConferenceSubscriptionApp(resip::DialogUsageManager& dum,
                                const resip::SipMessage &msg);
      ~ConferenceSubscriptionApp();

      // Get the owning conference.
      Conference* getConference();
      // Attach the subscription to a conference.
      void attach(Conference*);
      // Detach the subscription from its conference.
      void detach();
      // Record the ServerSubscriptionHandle.
      void setSubscriptionHandle(resip::ServerSubscriptionHandle);
      // Get the ServerSubscriptionHandle.
      resip::ServerSubscriptionHandle getSubscriptionHandle();
      // Terminate this subscription.
      // Includes detach(), so after this it is safe to delete the
      // conference.
      void terminate();
 
      // Support routines for generating NOTIFYs.

      // Generate notice for one subscriber.
      void notifyOne();

  private:
      Conference* mConference;
      resip::ServerSubscriptionHandle mSubscriptionHandle;
};

}

#endif