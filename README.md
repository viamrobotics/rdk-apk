# foreground service demo

## instructions

1. install android studio
2. open this project
3. in the droid-apk branch of my RDK fork, [here](https://github.com/abe-winter/rdk/tree/droid-apk), run `make droid-rdk.aar`. you may need to install gomobile, you may need to be on go 1.19
4. go to app/build.gradle.kts in this repo and find the implementation() line with droid-rdk.aar -- change it to the correct path on your system
5. hamburger menu -> run -> run
6. once the app is on your emulator AVD, go to its settings and grant all permissions

## avd setup

### setup with root

Root is not strictly required but you may want it so you can write + inspect app private storage. If so:

- in android studio device manager, create a new AVD. Pick an android 10 image for your laptop's architecture (x86 for intel, ARM for apple silicon probably). Make sure to pick one that **doesn't** have google play services. You'll probably have to switch tabs in the UI.
- run `adb root`, `adb shell`, `su`, `whoami` to make sure you really can get root

### setup without root

We're targeting SDK 28 so we can execute downloaded files (see 'termux hack' section below).

To set up local modules, copy binaries into /sdcard/Download like:

```
adb push binary-name /sdcard/Download
```

And set up your module to use /sdcard/Download/binary-name as the `executable_path`. Via an ugly and semi-reliable hack, the RDK will then copy from Download to its private cache directory before running.

## termux hack

Nutshell: newer android doesn't want to execute files you download. ([They think](https://developer.android.com/about/versions/10/behavior-changes-10#execute-permission) this is a W^X violation, i.e. nobody other than the package manager should be able to write to a location that is then executed).

We're copying termux, an android shell for linux. They target android sdk 28 to get the old behavior. This means we can't distribute via play store, which we're okay with.

More links:
- https://viam.atlassian.net/browse/RSDK-6558 for history of the issue for us
- https://github.com/termux/termux-app/discussions/3372 for summary of termux approach
- https://android.googlesource.com/platform/system/sepolicy/+/master/private/untrusted_app_27.te#24 droid selinux policy which controls this for SDK <= 28
