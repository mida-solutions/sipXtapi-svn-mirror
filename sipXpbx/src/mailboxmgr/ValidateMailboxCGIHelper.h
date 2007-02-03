// 
// 
// Copyright (C) 2004 SIPfoundry Inc.
// Licensed by SIPfoundry under the LGPL license.
// 
// Copyright (C) 2004 Pingtel Corp.
// Licensed to SIPfoundry under a Contributor Agreement.
// 
// $$
//////////////////////////////////////////////////////////////////////////////

#ifndef VALIDATEMAILBOXCGIHELPER_H
#define VALIDATEMAILBOXCGIHELPER_H

// SYSTEM INCLUDES
//#include <...>

// APPLICATION INCLUDES
#include "mailboxmgr/VXMLCGICommand.h"

// DEFINES
// MACROS
// EXTERNAL FUNCTIONS
// EXTERNAL VARIABLES
// CONSTANTS
// STRUCTS
// TYPEDEFS
// FORWARD DECLARATIONS

/**
 * Mailbox Class
 *
 * @author John P. Coffey
 * @version 1.0
 */
class ValidateMailboxCGIHelper : public VXMLCGICommand
{
public:
    /**
     * Ctor
     */
    ValidateMailboxCGIHelper ( const UtlString& identityOrExtension );

    /**
     * Virtual Destructor
     */
    virtual ~ValidateMailboxCGIHelper();

    /** 
     * This does the work, lazy creates a mailbox if 
     * required and validates the identity/extension 
     */
    virtual OsStatus execute (  UtlString* out = NULL);

    OsStatus validate ( const UtlBoolean& checkPermissions = TRUE ) ;

    /** Getter for the identity */
    void getMailboxIdentity( UtlString& mailboxIdentity ) const;

    /** Getter for the extension */
    void getExtension( UtlString& extension ) const;

    /** updates m_mailboxIdentity & m_extension and verifies validity */
    OsStatus validateIdentityAndGetExtension  ( 
        UtlString& rMailboxIdentity, 
        UtlString& rExtension,
        const UtlBoolean& checkPermissions = TRUE );

    UtlBoolean isNumeric( const UtlString& mailboxIdentity ) const ;

    OsStatus getUserId( const UtlString& mailboxIdentity,
                        UtlString& rUserId ) const ;

protected:

private:
    // this is the identity, never the extension
    const UtlString m_identityOrExtension;

    // mailbox identity, this is a simple sip identity
    UtlString m_mailboxIdentity;

    // mailbox extension, this is the entry that 
    // corresponds to m_mailbox in the AliasDB
    UtlString m_extension;

    // flag indicating whether we need to lazy create/validate 
    bool m_isValidated;
};

#endif //VALIDATEMAILBOXCGIHELPER_H

