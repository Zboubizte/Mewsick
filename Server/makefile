OPTIONS=-std=c++11 -I. -DICE_CPP11_MAPPING

all: bin/Server.exe

#SLICE

reslice: slice slicemv

#CREATION DES NOUVELLES SOURCES

slice: sliceServ sliceCli

sliceServ:
	slice2cpp Streaming.ice

sliceCli:
	slice2java Streaming.ice

#DEPLACEMENT DES NOUVELLES SOURCES

slicemv: slicemvServ slicemvCli

slicemvServ:
	mv -f Streaming.h include/Streaming.h && mv -f Streaming.cpp src/Streaming.cpp

slicemvCli:
	mv -f Middleware/* ../app/src/main/java/com/example/mewsick/Middleware

#SERVER

build/%.o: src/%.cpp
	g++ $(OPTIONS) -o $@ -c $<

bin/Server.exe: build/Server.o build/Streaming.o build/StreamingI.o build/MorceauI.o
	g++ -pthread -o $@ $^ -lIce++11

clean:
	rm build/*.o

cleanexec:
	rm bin/Server.exe

mrproper: clean cleanexec