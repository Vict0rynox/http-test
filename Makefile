.PHONY: dev build uberjar deps check-formatting clean test

LEIN ?= lein
EDITOR ?= vim

all: build

dev:
	rlwrap $(LEIN) trampoline run

build: uberjar
	$(LEIN) native

uberjar:
	$(LEIN) uberjar

deps:
	$(LEIN) deps

check-formatting:
	$(LEIN) cljfmt check

clean:
	rm -rf target

test:
	$(LEIN) trampoline test
