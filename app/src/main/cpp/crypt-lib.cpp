#include <jni.h>
#include <string>

#include "base64.h"
#include "sha512.h"
#include "crypt.h"

extern "C" JNIEXPORT jstring JNICALL
Java_com_hawerner_passmanager_Password_nativedecrypt(JNIEnv* env,jobject, jstring data, jstring salt, jstring key) {
    const char *returnString;
    const char *fN = env->GetStringUTFChars(data, NULL);
    const char *lN = env->GetStringUTFChars(salt, NULL);
    const char *k = env->GetStringUTFChars(key, NULL);

    std::string dataC = base64_decode(fN);
    std::string saltC(lN);
    std::string keyC(k);

    // strcpy(returnString,fN); // copy string one into the result.
    // strcat(returnString,lN); // append string two to the result.

    env->ReleaseStringUTFChars(data, fN);
    env->ReleaseStringUTFChars(salt, lN);
    env->ReleaseStringUTFChars(key, k);

    crypt(dataC, keyC, saltC);

    returnString = dataC.data();

    return env->NewStringUTF(returnString);
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_hawerner_passmanager_Password_nativecrypt(JNIEnv* env,jobject, jstring data, jstring salt, jstring key) {
    const char *returnString;
    const char *fN = env->GetStringUTFChars(data, NULL);
    const char *lN = env->GetStringUTFChars(salt, NULL);
    const char *k = env->GetStringUTFChars(key, NULL);

    std::string dataC(fN);
    std::string saltC(lN);
    std::string keyC(k);

    // strcpy(returnString,fN); // copy string one into the result.
    // strcat(returnString,lN); // append string two to the result.

    crypt(dataC, keyC, saltC);
    dataC = base64_encode(reinterpret_cast<const unsigned char*>(dataC.data()), dataC.length());

    returnString = dataC.data();

    env->ReleaseStringUTFChars(data, fN);
    env->ReleaseStringUTFChars(salt, lN);
    env->ReleaseStringUTFChars(key, k);

    return env->NewStringUTF(returnString);
}
