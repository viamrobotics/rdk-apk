RDK_SHA ?= $(shell cd /usr/local/src/rdk && git describe --tags)

.PHONY:
rename-apk: app/build/outputs/apk/debug/app-debug.apk
	cp $< rdk-$(shell date +%y%m%d)-$(RDK_SHA)-$(shell git rev-parse --short HEAD).apk
