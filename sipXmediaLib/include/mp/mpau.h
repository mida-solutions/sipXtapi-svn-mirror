//  
// Copyright (C) 2006 SIPez LLC. 
// Licensed to SIPfoundry under a Contributor Agreement. 
//
// Copyright (C) 2004-2006 SIPfoundry Inc.
// Licensed by SIPfoundry under the LGPL license.
//
// Copyright (C) 2004-2006 Pingtel Corp.  All rights reserved.
// Licensed to SIPfoundry under a Contributor Agreement.
//
// $$
///////////////////////////////////////////////////////////////////////////////

#ifndef AU_H_INCLUDED
#define AU_H_INCLUDED
#include "mp/MpAudioAbstract.h"
#include "mp/MpAudioFileDecompress.h"
#include <iostream>

bool isAuFile(istream &file);

class MpAuRead: public MpAudioAbstract {

private:
   istream & mStream;
   int m_CompressionType;
   AbstractDecompressor *_decoder;
public:
   typedef enum {
      DeG711MuLaw = 1,
      DePcm8Unsigned,
      DePcm16MsbSigned
   } AuDecompressionType;

   MpAuRead(istream & s, int raw = 0);

   ~MpAuRead() {
      if (_decoder) delete _decoder;
   }
   size_t getSamples(AudioSample *buffer, size_t numSamples) {
      return _decoder->getSamples(buffer,numSamples);
   }
private:
   size_t _dataLength;
public:
   size_t readBytes(AudioByte *buffer, size_t length);
   int getDecompressionType() {return m_CompressionType; }
   size_t getBytesSize();

private:
   bool _headerRead;    ///< true if header has already been read
   int _headerChannels; ///< channels from header
   int _headerRate;     ///< sampling rate from header
   void ReadHeader(void);
protected:
   void minMaxSamplingRate(long *min, long *max, long *preferred) {
      ReadHeader();
      *min = *max = *preferred = _headerRate;
   }
   void minMaxChannels(int *min, int *max, int *preferred) {
      ReadHeader();
      *min = *max = *preferred = _headerChannels;
   }
};

#endif
