/*
 * Copyright (c) 2007, Wirtualna Polska S.A.
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
#pragma once
#include <VideoSupport/Types.h>
#include <memory>

enum VideoProcessorCategory
{
	//! Returned processor implements @c VideoFrameInPlaceProcessor interface.
	videoVerticalFlipper,
	//! Returned processor implements @c VideoScaler interface.
	videoScaler,
};

typedef std::auto_ptr<VideoFrameProcessor> VideoFrameProcessorAutoPtr;
typedef VideoFrameProcessor* (* VideoFrameProcessorConstructor)();

class VideoProcessorFactory
{
public:

	static VideoProcessorFactory* GetInstance();

	static void StaticDispose();

	//! @note This is caller's responsibility to cast returned processor to appropriate
	//! type and perform additional initialization.
	VideoFrameProcessorAutoPtr CreateProcessor(VideoProcessorCategory category, VideoSurface surface);

	static bool RegisterConstructor(VideoProcessorCategory category, VideoSurface surface, VideoFrameProcessorConstructor constructor);

private:

	VideoProcessorFactory();
	~VideoProcessorFactory();
	
	VideoProcessorFactory(const VideoProcessorFactory&);
	VideoProcessorFactory& operator=(const VideoProcessorFactory&);

	struct Impl;
	Impl* impl_;
};