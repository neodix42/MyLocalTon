some CPUs do not support AVX2 instructions and Windows binaries fail.
In order to fix this please apply the following patch:
third-party\rocksdb\CMakeLists.txt
if(MSVC)
-    set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} /arch:AVX2")
+    set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} /arch:AVX")