// 
// 
// Copyright (C) 2005 SIPfoundry Inc.
// Licensed by SIPfoundry under the LGPL license.
// 
// Copyright (C) 2005 Pingtel Corp.
// Licensed to SIPfoundry under a Contributor Agreement.
// 
// $$
//////////////////////////////////////////////////////////////////////////////

// SYSTEM INCLUDES


// APPLICATION INCLUDES
#include <utl/UtlRegex.h>
#include "os/OsDateTime.h"
#include "os/OsSysLog.h"
#include "sipdb/SIPDBManager.h"
#include "sipdb/ResultSet.h"
#include "sipdb/RegistrationDB.h"
#include "SipRedirectorRegDB.h"

// DEFINES
// MACROS
// EXTERNAL FUNCTIONS
// EXTERNAL VARIABLES
// CONSTANTS
// STRUCTS
// TYPEDEFS
// FORWARD DECLARATIONS

// Constructor
SipRedirectorRegDB::SipRedirectorRegDB()
{
}

// Destructor
SipRedirectorRegDB::~SipRedirectorRegDB()
{
}

// Initializer
OsStatus
SipRedirectorRegDB::initialize(const UtlHashMap& configParameters,
                               OsConfigDb& configDb,
                               SipUserAgent* pSipUserAgent,
                               int redirectorNo)
{
   return OS_SUCCESS;
}

// Finalizer
void
SipRedirectorRegDB::finalize()
{
}

SipRedirector::LookUpStatus
SipRedirectorRegDB::lookUp(
   const SipMessage& message,
   const UtlString& requestString,
   const Url& requestUri,
   const UtlString& method,
   SipMessage& response,
   RequestSeqNo requestSeqNo,
   int redirectorNo,
   SipRedirectorPrivateStorage*& privateStorage)
{
   int timeNow = OsDateTime::getSecsSinceEpoch();
   ResultSet registrations;
   // Local copy of requestUri
   Url requestUriCopy = requestUri;

   // Look for any grid parameter and remove it.
   UtlString gridParameter;
   UtlBoolean gridPresent =
      requestUriCopy.getUrlParameter("grid", gridParameter, 0);
   if (gridPresent)
   {
      requestUriCopy.removeUrlParameter("grid");
   }
   if (OsSysLog::willLog(FAC_SIP, PRI_DEBUG))
   {
      UtlString temp;
      requestUriCopy.getUri(temp);
      OsSysLog::add(FAC_SIP, PRI_DEBUG,
                    "SipRedirectorRegDB::lookUp "
                    "gridPresent = %d, gridParameter = '%s', "
                    "requestUriCopy after removing grid = '%s'",
                    gridPresent, gridParameter.data(), temp.data());
   }

   // Note that getUnexpiredContacts will reduce the requestUri to its
   // identity (user/host/port) part before searching in the
   // database.  The requestUri identity is matched against the
   // "identity" column of the database, which is the identity part of
   // the "uri" column which is stored in registration.xml.
   RegistrationDB::getInstance()-> // TODO - change to SipRegistrar::getRegistrationDB (see Scott Lawrence)
      getUnexpiredContacts(requestUriCopy, timeNow, registrations);

   int numUnexpiredContacts = registrations.getSize();

   OsSysLog::add(FAC_SIP, PRI_DEBUG,
                 "SipRedirectorRegDB::lookUp "
                 "got %d unexpired contacts", numUnexpiredContacts);

   for (int i = 0; i < numUnexpiredContacts; i++)
   {
      // Query the Registration DB for the contact, expires and qvalue columns.
      UtlHashMap record;
      registrations.getIndex(i, record);
      UtlString contactKey("contact");
      UtlString expiresKey("expires");
      UtlString qvalueKey("qvalue");
      UtlString contact = *((UtlString*) record.findValue(&contactKey));
      UtlString qvalue  = *((UtlString*) record.findValue(&qvalueKey));
      OsSysLog::add(FAC_SIP, PRI_DEBUG,
                    "SipRedirectorRegDB::lookUp "
                    "contact = '%s', qvalue = '%s'", contact.data(),
                    qvalue.data());
      Url contactUri(contact);

      // If the contact URI is the same as the request URI, ignore it.
      if (!contactUri.isUserHostPortEqual(requestUriCopy))
      {
         // Check if the q-value from the database is valid, and if so,
         // add it into contactUri.
         if (!qvalue.isNull() &&
             qvalue.compareTo(SPECIAL_IMDB_NULL_VALUE) != 0)
         {
            // :TODO: (XPL-3) need a RegEx copy constructor here
            // Check if q value is numeric and between the range 0.0 and 1.0.
            static RegEx qValueValid("^(0(\\.\\d{0,3})?|1(\\.0{0,3})?)$"); 
            if (qValueValid.Search(qvalue.data()))
            {
               contactUri.setFieldParameter(SIP_Q_FIELD, qvalue);
            }
         }

         // Re-apply any grid parameter.
         if (gridPresent)
         {
            contactUri.setUrlParameter("grid", gridParameter);
         }

         // Add the contact.
         addContact(response, requestString, contactUri, "registration");
      }
   }

   return SipRedirector::LOOKUP_SUCCESS;
}