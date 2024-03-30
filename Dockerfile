# base image for APK builds
FROM ubuntu:latest

RUN --mount=type=cache,target=/var/cache/apt apt-get update
# sdkmanager requires 21 jre, we build on old jdk because we target old droid. (not sure old jdk is necessary though)
# make...python3 for tflite build
# nasm for x264
# golang for rdk
RUN --mount=type=cache,target=/var/cache/apt apt-get install -qy \
    zip \
    openjdk-11-jdk-headless openjdk-21-jre-headless \
    make curl patch cmake git python3 \
    nasm \
    golang-1.21-go

ENV ANDROID_HOME /droid
ENV ANDROID_SDK_ROOT /droid

WORKDIR ${ANDROID_SDK_ROOT}

# https://developer.android.com/studio#command-line-tools-only
ARG CLI_TOOLS=commandlinetools-linux-11076708_latest.zip
RUN curl --fail --silent --show-error -o ${CLI_TOOLS} https://dl.google.com/android/repository/${CLI_TOOLS} && unzip -q ${CLI_TOOLS} && rm ${CLI_TOOLS}

ENV PATH ${PATH}:/droid/cmdline-tools/bin
ARG NDK_VERSION=26.2.11394342
ENV NDK_ROOT ${ANDROID_SDK_ROOT}/ndk/${NDK_VERSION}

# note: we target android 28, but compileSdk is 34 in our gradle files; check if both are needed
RUN yes | sdkmanager --sdk_root=$(realpath .) --install "platforms;android-28" "platforms;android-34" "build-tools;26.0.3" "ndk;${NDK_VERSION}"

ENV PATH ${PATH}:/usr/lib/go-1.21/bin:/root/go/bin
RUN go install golang.org/x/mobile/cmd/gomobile@latest
RUN gomobile init

ENV SDKMAN_DIR /usr/local/sdkman
# this is the sdk manager script from https://get.sdkman.io/
# we have a copy checked-in to get a consistent version
# todo: look at getting openjdk from here as well
RUN --mount=type=bind,source=etc/get-sdkman.sh,target=get-sdkman.sh ./get-sdkman.sh
# requires bash because it uses 'source' keyword internally
RUN bash -c "source $SDKMAN_DIR/bin/sdkman-init.sh && sdk install gradle 8.2"
