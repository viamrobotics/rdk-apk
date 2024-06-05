# Android APK bundle for RDK

This is a Kotlin wrapper that makes the [Viam RDK](https://github.com/viamrobotics/rdk) usable on Android systems.

## daily builds

We upload daily builds to GCS, linked below.

You probably want the aarch64 build. If you're running an emulator on an intel device, use x86_64.

- [rdk-apk-latest.aarch64.apk](https://storage.googleapis.com/packages.viam.com/apps/rdk-apk/rdk-apk-latest.aarch64.apk)
- [rdk-apk-latest.x86_64.apk](https://storage.googleapis.com/packages.viam.com/apps/rdk-apk/rdk-apk-latest.x86_64.apk)

App setup after installing:
1. Use the GUI to load a viam.json file, either from a folder or by pasting in its contents.
1. In the GUI, there's a card with a list of required permissions. If they're not all checked, add them, either by going to device settings or with the 'add permissions' line in the [runbook](#mini-runbook) below.
1. Once this is done, your device should show up as live in the Viam webapp. If it doesn't try the logcat lines below to debug.

## developer setup

Use a daily build (see above) if possible, i.e. if you're doing module development. Only build this project if you plan to work on this project.

1. install android studio
2. open this project
3. in your local RDK checkout, run `make droid-rdk.aar`. you may need to install gomobile (google for instructions).
4. help this find your RDK. either:
  - create a symlink /usr/local/src/rdk that points to your rdk folder.
  - or edit [app/build.gradle.kts](app/build.gradle.kts). change the droid-rdk.aar line to point to the correct path
5. hamburger menu -> run -> run
6. once the app is on your emulator AVD, go to its settings and grant all permissions

If you run into trouble, check logcat. Some invocations:

```sh
# view logs from the RDK golang thread
adb logcat -d | grep GoLog | less

# view logs for our android package. If something fails to start and `GoLog` doesn't match anything, try this
adb logcat -d | grep fgservice | less
```

For module development instructions, look at the [Android example modules README](https://github.com/viamrobotics/viam-java-sdk/tree/main/android/examples/module) in our Java SDK.

## avd notes

Android 12 introduces a background process killer that has to be disabled in device config. Make sure 

### with root access

Root is not strictly required but you may want it so you can write + inspect app private storage. If so:

- in android studio device manager, create a new AVD. Pick an android 10 image for your laptop's architecture (x86 for intel, ARM for apple silicon probably). Make sure to pick one that **doesn't** have google play services. You'll probably have to switch tabs in the UI.
- run `adb root`, `adb shell`, `su`, `whoami` to make sure you really can get root

### without root

Use any AVD image that works on your architecture, ideally with android version less than 12.

## mini runbook

Useful commands for working with android, adb, emulators. If a command doesn't start with `adb`, it needs to be run inside an `adb shell` session.

uninstall-reinstall:

```sh
adb uninstall com.viam.rdk.fgservice
adb install ./path/to/apk
```

add permissions:

```sh
for perm in CAMERA RECORD_AUDIO READ_EXTERNAL_STORAGE WRITE_EXTERNAL_STORAGE ACCESS_MEDIA_LOCATION; do pm grant com.viam.rdk.fgservice android.permission.$perm; done
# (remove ACCESS_MEDIA_LOCATION if it complains)
```

start app from CLI:

```sh
adb shell am start -n com.viam.rdk.fgservice/.RDKLaunch
```

To force restart the app (sometimes need to manually start after, see 'start app' above):

```sh
# need to `su` first
# ps -A | grep viam
u0_a54        12743    839 13775572 249856 do_epoll_wait      0 S com.viam.rdk.fgservice
# kill -9 12743
```

Find viam's subprocesses (i.e. modules, if running as processes):

```sh
$ ps -A | grep viam
u0_a120       6555  1743 6285104 241764 0                   0 S com.viam.rdk.fgservice
$ ps -A | grep a120
u0_a120       6555  1743 6285104 241764 0                   0 S com.viam.rdk.fgservice
u0_a120       6620  6555   11724   7180 0                   0 S sh
u0_a120       6634  6620 3669544 119780 0                   0 S app_process
```

## W^X workaround

Newer android doesn't want to execute files you download. ([They think](https://developer.android.com/about/versions/10/behavior-changes-10#execute-permission) this is a W^X violation, i.e. nobody other than the package manager should be able to write to a location that is then executed).

We target android sdk 28 to get the old behavior.

More links:
- https://github.com/termux/termux-app/discussions/3372 for summary of termux approach
- https://android.googlesource.com/platform/system/sepolicy/+/master/private/untrusted_app_27.te#24 droid selinux policy which controls this for SDK <= 28
