//
// Copyright (C) 2004, 2005 Pingtel Corp.
// 
//
// $$
////////////////////////////////////////////////////////////////////////

#include <cppunit/extensions/HelperMacros.h>
#include <cppunit/TestCase.h>

#include <sipxunit/TestUtilities.h>
#include <os/OsTimerTask.h>

class OsTimerTaskTest : public CppUnit::TestCase
{
    CPPUNIT_TEST_SUITE(OsTimerTaskTest);
    CPPUNIT_TEST(testTimerTask);
    CPPUNIT_TEST_SUITE_END();

public:
    void testTimerTask()
    {
        OsTimerTask* pTimerTask;
        pTimerTask = OsTimerTask::getTimerTask();
        CPPUNIT_ASSERT_MESSAGE("Timer task created 1", pTimerTask != NULL);
        OsTask::delay(500);    // wait 1/2 second

        pTimerTask->destroyTimerTask();

        OsTask::delay(500);    // wait 1/2 second

        pTimerTask = OsTimerTask::getTimerTask();
        CPPUNIT_ASSERT_MESSAGE("Timer task created 2", pTimerTask != NULL);
        OsTask::delay(500);    // wait 1/2 second

        pTimerTask->destroyTimerTask();
    }
};

CPPUNIT_TEST_SUITE_REGISTRATION(OsTimerTaskTest);
