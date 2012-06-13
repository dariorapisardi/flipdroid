/**
 * avcodec-iface.c
 * helper library that encodes RAW PCM data using libavcodec
 * Dario Rapisardi <dario@flipzu.com> November 2010
 * Licensed under the LGPLv2.1 - see /usr/share/common-licenses/LGPL-2.1 for details.
 */
#include <jni.h>
#include <android/log.h>
//#include <libavcodec/avcodec.h>
#include "../ffmpeg/libavcodec/avcodec.h"

#define INBUF_SIZE 4096
#define TAG "Flipzu_Encoder_Iface" 

AVCodec *codec;
AVCodecContext *c= NULL;

/* do proper encoding */
/* Returns the lenght of the encoded chunk */
/* in_size should be frame_size long, as returned by encoder_init() */
jint 
Java_com_flipzu_flipzu_Encoder_audioEncode( JNIEnv *env, jobject this, jbyteArray pcm_buf, jint pcm_len, jbyteArray enc_buf ) 
{
    //__android_log_print(ANDROID_LOG_DEBUG, TAG, "NDK: audioEncode(): called");
    jint enc_len = 0;
    int outbuf_size = 10000;
    char in_buf[pcm_len]; 
    char out_buf[outbuf_size];

    /* get pcm data from java */
    (*env)->GetByteArrayRegion(env, pcm_buf, 0, pcm_len, (jbyte*)in_buf);

    /* do proper encoding */
    enc_len = avcodec_encode_audio(c, (uint8_t*)out_buf, outbuf_size, (short*)in_buf);

    /* copy encoded data to java */
    (*env)->SetByteArrayRegion(env, enc_buf, 0, enc_len, (jbyte*)out_buf);

    //__android_log_print(ANDROID_LOG_DEBUG, TAG, "NDK: audioEncode(): encoded %d bytes", (int)enc_len);

    return enc_len;
}

/* run first - returns the frame size. Use read(frame_size) and give */
/* that chunk of data to audio_encode() */
jint 
Java_com_flipzu_flipzu_Encoder_encoderInit( JNIEnv *env, jobject this, jint bit_rate, jint sample_rate, jint channels ) {

    jint frame_size;
    avcodec_init();
    avcodec_register_all();

    codec = avcodec_find_encoder(CODEC_ID_MP3);
    if (!codec) {
        __android_log_print(ANDROID_LOG_ERROR, TAG, "NDK: encoderInit(): can't find codec");
        return -1;
    }

    c= avcodec_alloc_context();
    
    /* put sample parameters */
    c->bit_rate = bit_rate;
    c->sample_rate = sample_rate;
    c->channels = channels;

    /* open it */
    if (avcodec_open(c, codec) < 0) {
        __android_log_print(ANDROID_LOG_ERROR, TAG, "NDK: encoderInit(): can't open codec");
        return -1;
    }

    frame_size = c->frame_size*2*c->channels;

    return frame_size;

}

/* cleanup */
void 
Java_com_flipzu_flipzu_Encoder_encoderClose( JNIEnv *env, jobject this )
{
    __android_log_print(ANDROID_LOG_DEBUG, TAG, "NDK: encoderClose(): closing codecs.");
    avcodec_close(c);
    free(c);
}

