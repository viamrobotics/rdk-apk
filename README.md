# foreground service demo

## instructions

1. install android studio
2. open this project
3. in the droid-apk branch of my RDK fork, [here](https://github.com/abe-winter/rdk/tree/droid-apk), run `make droid-rdk.aar`. you may need to install gomobile, you may need to be on go 1.19
4. go to app/build.gradle.kts in this repo and find the implementation() line with droid-rdk.aar -- change it to the correct path on your system
5. hamburger menu -> run -> run
6. once the app is on your emulator AVD, go to its settings and grant all permissions

## avd setup

You need a rooted device to install modules. To set up your emulator:

- in android studio device manager, create a new AVD. Pick an android 10 image for your laptop's architecture (x86 for intel, ARM for apple silicon probably). Make sure to pick one that **doesn't** have google play services. You'll probably have to switch tabs in the UI.
- run `adb root`, `adb shell`, `su`, `whoami` to make sure you really can get root
