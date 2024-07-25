RDK_SHA ?= $(shell cd /usr/local/src/rdk && git describe --tags)

.PHONY:
rename-apk: app/build/outputs/apk/debug/app-debug.apk
	cp $< rdk-$(shell date +%y%m%d)-$(RDK_SHA)-$(shell git rev-parse --short HEAD).apk

ADB_IP ?= 10.1.2.129:5555
connect-adb:
	adb connect $(ADB_IP)
