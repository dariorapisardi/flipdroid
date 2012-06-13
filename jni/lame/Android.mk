LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE := mp3lame

LOCAL_SRC_FILES := VbrTag.c \
   bitstream.c \
   encoder.c \
   fft.c \
   gain_analysis.c \
   id3tag.c \
   lame.c \
   newmdct.c \
   presets.c \
   psymodel.c \
   quantize.c \
   quantize_pvt.c \
   reservoir.c \
   set_get.c \
   tables.c \
   takehiro.c \
   util.c \
   vbrquantize.c \
   version.c \
   mpglib_interface.c

LOCAL_LDLIBS := -ldl -lc -lz -lm

include $(BUILD_STATIC_LIBRARY)
#include $(BUILD_SHARED_LIBRARY)
