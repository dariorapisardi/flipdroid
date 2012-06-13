LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)
LOCAL_STATIC_LIBRARIES := libavformat libavcodec libavutil libpostproc libswscale mp3lame
LOCAL_MODULE := ffmpeg
include $(BUILD_STATIC_LIBRARY)
include $(call all-makefiles-under,$(LOCAL_PATH))
