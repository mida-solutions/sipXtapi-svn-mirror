// 
// Copyright (C) 2006 SIPfoundry Inc.
// Licensed by SIPfoundry under the LGPL license.
// 
// Copyright (C) 2006 Pingtel Corp.
// Licensed to SIPfoundry under a Contributor Agreement.
// 
// $$
//////////////////////////////////////////////////////////////////////////////
#ifndef CALLERALIASDB_H
#define CALLERALIASDB_H

// SYSTEM INCLUDES

// APPLICATION INCLUDES
#include "os/OsMutex.h"
#include "utl/UtlString.h"

// DEFINES

// MACROS
// EXTERNAL FUNCTIONS
// EXTERNAL VARIABLES
// CONSTANTS
// STRUCTS
// TYPEDEFS
// FORWARD DECLARATIONS
template<class T> class dbCursor;
class dbDatabase;
class dbFieldDescriptor;
class dbQuery;
class RegistrationRow;
class Url;

/// Database of all information acquired through REGISTER messages
class CallerAliasDB
{
  public:
   static const UtlString DbName;

   // Singleton Accessor
   static CallerAliasDB* getInstance( const UtlString& name = DbName );

   /// releaseInstance - cleans up the singleton (for use at exit)
   static void releaseInstance();

   /// Add a single mapping to the database.
   void insertRow(const UtlString identity, ///< identity of caller in 'user@domain' form (no scheme) 
                  const UtlString domain,   /**< domain and optional port for target
                                             *  ( 'example.com' or 'example.com:5099' ) */
                  const UtlString alias     /// returned alias
                  );
   /**<
    * The identity value may be the null string; this is a wildcard entry that matches
    * any caller to the given domain.
    */
   
   /// Get the caller alias for this combination of caller identity and target domain.
   bool getCallerAlias (
      const UtlString& identity, ///< identity of caller in 'user@domain' form (no scheme)
      const UtlString& domain,   /**< domain and optional port for target
                                  *  ( 'example.com' or 'example.com:5099' ) */
      UtlString& callerAlias     /// returned alias
                        ) const;
   /**<
    * @returns true if an alias was found for this caller, false if not
    *
    * This first looks in the database for an exact match of identity and domain;
    *   if this match is found, the resulting alias is returned in callerAlias.
    * If no exact match is found, the database is then checked for a row containing
    *   a null (empty string) identity and the domain; this is a domain wildcard entry
    *   and it is returned in callerAlias.
    * If neither match is found, callerAlias is set to the null string.
    */

   // Clear out all rows in the database.
   void removeAllRows ();

   // Write the current rows to the persistent store.
   OsStatus store();

  protected:
   // Fast DB instance
   static dbDatabase* spDBInstance;

   // Fast DB instance
   dbDatabase* mpFastDB;

   // There is only one singleton in this design
   static CallerAliasDB* spInstance;

   // Singleton and Serialization mutex
   static OsMutex sLockMutex;

   // The persistent filename for loading/saving
   static const UtlString DatabaseName;

   // Implicitly called when constructed - reads data from persistent store.
   OsStatus load();

   /// Utility method for dumping all rows
   void getAllRows ( ResultSet& rResultSet ) const;

   // Singleton Constructor is private
   CallerAliasDB(const UtlString& name);

   static const UtlString IdentityKey;
   static const UtlString DomainKey;
   static const UtlString AliasKey;

  private:
   /// No destructor, no no no
   ~CallerAliasDB();

   static UtlString nullString;
};

#endif //CALLERALIASDB_H
