all:
	mvn package -DdescriptorId=jar-with-dependencies

run:
	java -Djna.debug_load=true \
	     -Djna.library.path=/opt/homebrew/opt/sdl2-compat/lib \
	     -XstartOnFirstThread \
	     -jar target/eqs-1.0-SNAPSHOT.jar
