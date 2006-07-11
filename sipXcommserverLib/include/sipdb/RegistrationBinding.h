// 
// 
// Copyright (C) 2005-2006 SIPez LLC.
// Licensed to SIPfoundry under a Contributor Agreement.
// 
// Copyright (C) 2004 SIPfoundry Inc.
// Licensed by SIPfoundry under the LGPL license.
// 
// Copyright (C) 2004 Pingtel Corp.
// Licensed to SIPfoundry under a Contributor Agreement.
// 
// $$
//////////////////////////////////////////////////////////////////////////////

#ifndef REGISTRATION_H
#define REGISTRATION_H
// SYSTEM INCLUDES

// APPLICATION INCLUDES
#include "utl/UtlContainable.h"

// DEFINES
// MACROS
// EXTERNAL FUNCTIONS
// EXTERNAL VARIABLES
// CONSTANTS
// STRUCTS
// TYPEDEFS
// FORWARD DECLARATIONS
class Url;
class UtlHashMap;
class UtlString;

// :TODO: Consider wrapping the RegistrationBinding around an underlying hash set
// for efficiency, rather than having a bunch of invidual data members, to eliminate
// conversions from/to hash sets.

/**
 * The Registration class represents a SIP registration binding.
 * Let's treat bindings as well-defined objects rather than unstructured hash sets.
 */
class RegistrationBinding : public UtlContainable
{
public:
   /// Default constructor
   RegistrationBinding();

   /// Constructor that builds a RegistrationBinding from a HashMap
   RegistrationBinding(const UtlHashMap& regData);

   /// Destructor
   virtual ~RegistrationBinding() {}

   /// Copy the binding data to a hash map
   void copy(UtlHashMap& map) const;
   
   const UtlString* getIdentity() const;
   void       setIdentity(const UtlString& identity);
   
   const Url* getUri() const;
   void       setUri(const Url& uri);
   void       setUri(const UtlString& uri);
   
   const UtlString* getCallId() const;
   void       setCallId(const UtlString& callId);
   
   const UtlString* getContact() const;
   void       setContact(const UtlString& contact);
   
   const UtlString* getQvalue() const;
   void       setQvalue(const UtlString& qvalue);
   
   const UtlString* getInstanceId() const;
   void       setInstanceId(const UtlString& instanceId);
   
   const UtlString* getGruu() const;
   void       setGruu(const UtlString& gruu);
   
   int        getCseq() const;
   void       setCseq(int cseq);
   void       setCseq(const UtlString& cseq);
   
   int        getExpires() const;
   void       setExpires(int expires);
   void       setExpires(const UtlString& expires);
   
   const UtlString* getPrimary() const;
   void       setPrimary(const UtlString& primary);
   
   INT64      getUpdateNumber() const;
   void       setUpdateNumber(INT64 updateNumber);
   void       setUpdateNumber(const UtlString& updateNumber);

/* ========================= UtlContainable interface======================== */

    /// Calculate a hash code for this object.
    virtual unsigned hash() const;

    /// Compare this object to another object. 
    virtual int compareTo(UtlContainable const *) const;    

    /**
     * Get the ContainableType for the RegistrationBinding as a contained object.
     */
    virtual UtlContainableType getContainableType() const;

    static const UtlContainableType TYPE; ///< constant for class type comparison.

/* ========================================================================== */

private:
   // registration data
   UtlString* mIdentity;
   Url*       mUri;
   UtlString* mCallId;
   UtlString* mContact;
   UtlString* mQvalue;
   UtlString* mInstanceId;
   UtlString* mGruu;
   int        mCseq;
   int        mExpires;
   UtlString* mPrimary;
   INT64      mUpdateNumber;

   /// no copy constructor
   RegistrationBinding(const RegistrationBinding& nocopy);

   /// no assignment operator
   RegistrationBinding& operator=(const RegistrationBinding& noassignment);
};

#endif //REGISTRATION_H
