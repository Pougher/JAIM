ifeq ($(OS),Windows_NT)
	JFLAGS := -jar target/eqs-1.0-SNAPSHOT.jar
else
	UNAME_S := $(shell uname -s)
	ifeq ($(UNAME_S),Darwin)
    	JFLAGS := -Djna.debug_load=true \
	     -Djna.library.path=/opt/homebrew/opt/sdl2-compat/lib \
	     -XstartOnFirstThread \
	     -jar target/eqs-1.0-SNAPSHOT.jar
	endif
endif

all:
	mvn package -DdescriptorId=jar-with-dependencies

run:
	java $(JFLAGS)
