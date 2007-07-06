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
//#include <...>

// APPLICATION INCLUDES
#include "mailboxmgr/VXMLDefs.h"
#include "mailboxmgr/MailboxManager.h"
#include "mailboxmgr/SpecialAAMenuCGI.h"


// DEFINES
// MACROS
// EXTERNAL FUNCTIONS
// EXTERNAL VARIABLES
// CONSTANTS
// STRUCTS
// TYPEDEFS
// FORWARD DECLARATIONS

/* ============================ CREATORS ================================== */

SpecialAAMenuCGI::SpecialAAMenuCGI( const UtlString& option ) :
    mOption ( option )
{}

SpecialAAMenuCGI::~SpecialAAMenuCGI()
{}

OsStatus
SpecialAAMenuCGI::execute(UtlString* out)
{
    OsStatus result = OS_SUCCESS;

    // Instantiate the mailbox manager
    MailboxManager* pMailboxManager = MailboxManager::getInstance();
   
    if (mOption.compareTo("true", UtlString::ignoreCase) == 0)
    {
        result = pMailboxManager->setSystemOverallAAMenu(true);
    }
    else
    {
        result = pMailboxManager->setSystemOverallAAMenu(false);
    }    

    UtlString dynamicVxml (VXML_BODY_BEGIN);

    if( result == OS_SUCCESS )
    {
        // Enable or disable special AA menu successfully
        dynamicVxml += VXML_SUCCESS_SNIPPET;
    }
    else
    {
        // Unable to save the password
        dynamicVxml += VXML_FAILURE_SNIPPET;
    }

    dynamicVxml += VXML_END;

    // Write out the dynamic VXML script to be processed by OpenVXI
    if (out)
    {
       out->remove(0);
       UtlString responseHeaders;
       MailboxManager::getResponseHeaders(dynamicVxml.length(), responseHeaders);
      
       out->append(responseHeaders.data());
       out->append(dynamicVxml.data());
    }

    return OS_SUCCESS ;
}