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

// SYSTEM INCLUDES


// APPLICATION INCLUDES
#include "utl/UtlHashMapIterator.h"
#include "sipdb/ResultSet.h"

// DEFINES
// MACROS
// EXTERNAL FUNCTIONS
// EXTERNAL VARIABLES
// CONSTANTS
// STRUCTS
// TYPEDEFS
// FORWARD DECLARATIONS

ResultSet::ResultSet() 
{}
 
ResultSet::~ResultSet() 
{
    // make sure we destroy the records 
    // we allocated in the addValue method
    destroyAll();
}

int
ResultSet::getSize() const
{
    return entries();
}

void
ResultSet::destroyAll()
{
    // the pRecord is actually a UtlHashMap
    UtlHashMap* pRecord;
    while (pRecord = dynamic_cast<UtlHashMap*>(get()))
    {
        pRecord->destroyAll();
        delete pRecord;
    }
}

void
ResultSet::clear()
{
   // I think this would actually be a bug if there were ever anything here to clear,
   // so I'm going to assert that the ResultSet is empty, rather than clearing it
   assert(isEmpty());
}

OsStatus 
ResultSet::getIndex(
    const int& index, 
    UtlHashMap& rRecord) const
{
    // The record must be empty.  We can't clear the content because we don't own it.
    assert(rRecord.isEmpty());

    OsStatus result = OS_FAILED;
    UtlHashMap *m;
    if (m = dynamic_cast<UtlHashMap*>(at(index)))
    {
        m->copyInto(rRecord);
        result = OS_SUCCESS;
    }
    return result;
}

void
ResultSet::addValue( const UtlHashMap& record )
{
    UtlHashMap*     pNewRecord = new UtlHashMap() ;
    UtlContainable* pObj ;

    // Proceed with shallow copy
    UtlHashMapIterator itor(const_cast<UtlHashMap&>(record)) ;
    while ((pObj = (UtlContainable*) itor()) != NULL)
    {
        pNewRecord->insertKeyAndValue(itor.key(), itor.value()) ;
    }
    append(pNewRecord) ;
}
