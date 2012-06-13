#!/bin/bash

PREBUILT=/home/dario/android-ndk-r4b/build/prebuilt/linux-x86/arm-eabi-4.4.0
PLATFORM=/home/dario/android-ndk-r4b/build/platforms/android-3/arch-arm

FLAGS="--target-os=linux"
FLAGS="$FLAGS --arch=arm"
FLAGS="$FLAGS --enable-version3"
FLAGS="$FLAGS --enable-gpl"
FLAGS="$FLAGS --enable-small"
#FLAGS="$FLAGS --enable-nonfree"
FLAGS="$FLAGS --disable-armvfp"
FLAGS="$FLAGS --disable-stripping"
FLAGS="$FLAGS --disable-ffmpeg"
FLAGS="$FLAGS --disable-ffplay"
FLAGS="$FLAGS --disable-ffserver"
FLAGS="$FLAGS --disable-ffprobe"
FLAGS="$FLAGS --disable-decoders"
FLAGS="$FLAGS --disable-encoders"
FLAGS="$FLAGS --disable-muxers"
FLAGS="$FLAGS --disable-demuxers"
FLAGS="$FLAGS --disable-devices"
FLAGS="$FLAGS --disable-protocols"
FLAGS="$FLAGS --disable-parsers"
#FLAGS="$FLAGS --enable-protocol=file"
FLAGS="$FLAGS --disable-filters"
FLAGS="$FLAGS --disable-bsfs"
#FLAGS="$FLAGS --enable-avfilter"
FLAGS="$FLAGS --disable-network"
FLAGS="$FLAGS --disable-mpegaudio-hp"
FLAGS="$FLAGS --disable-avdevice"
FLAGS="$FLAGS --enable-cross-compile"
FLAGS="$FLAGS --cc=$PREBUILT/bin/arm-eabi-gcc"
FLAGS="$FLAGS --cross-prefix=$PREBUILT/bin/arm-eabi-"
FLAGS="$FLAGS --nm=$PREBUILT/bin/arm-eabi-nm"
FLAGS="$FLAGS --disable-asm"
#FLAGS="$FLAGS --enable-neon"
FLAGS="$FLAGS --enable-armv5te"
FLAGS="$FLAGS --enable-libmp3lame"
FLAGS="$FLAGS --enable-encoder=libmp3lame"


./configure $FLAGS --extra-cflags="-fPIC -DANDROID -I../ -I${PLATFORM}/usr/include/" --extra-ldflags="-Wl,-T,$PREBUILT/arm-eabi/lib/ldscripts/armelf.x -Wl,-rpath-link=$PLATFORM/usr/lib -L$PLATFORM/usr/lib -L../../objs/local/armeabi -nostdlib $PREBUILT/lib/gcc/arm-eabi/4.4.0/crtbegin.o $PREBUILT/lib/gcc/arm-eabi/4.4.0/crtend.o -lc -lm -ldl"

TEMPFILE=`tempfile`
sed -e "s/restrict restrict/restrict/" config.h > $TEMPFILE
mv $TEMPFILE config.h
