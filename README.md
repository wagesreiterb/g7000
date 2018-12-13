# G7000
Emulator of the Philips Videopac G7000 (Magnavox Odyssey²)
(the G7400 isn't considered at all)

!! Attention !!
This emu of the G7000/Odyssey2 Video Game Console still has some bugs and also just works with some selected game-binaries.
Furthermore collision detection isn't implemented at all.


-) clone the project
   git clone http://github.com/wagesreiterb/g7000

-) compile with javac
   javac g7000/*.java
   (don't change the directory)

-) create the directory binaries
   copy the firmware "o2rom.bin" into the directory binaries
   you should be able to find it somewhere in the internet
   $ md5sum o2rom.bin 
   562d5ebf9e030a40d6fabfc2f33139fd  o2rom.bin
   
-) copy new_pong.bin into the directory binaries
   you should be able to find it somewhere in the internet
   $ md5sum new_pong.bin 
   f26d1518a538ec67c2f5b8cad8174d8d  new_pong.bin

-) run with java
   java -cp ./ g7000.Main



Tested on Ubuntu with:
$ java -version
java version "1.7.0_181"
OpenJDK Runtime Environment (IcedTea 2.6.14) (7u181-2.6.14-0ubuntu0.3)
OpenJDK 64-Bit Server VM (build 24.181-b01, mixed mode)
