cmake_minimum_required(VERSION 3.22.1)
project(hakim LANGUAGES C CXX)

# ابنِ llama.cpp محسّناً دائماً، حتى بنسخة debug (بدونها التحليل أبطأ بعشر مرات)
set(CMAKE_BUILD_TYPE Release)
set(CMAKE_C_FLAGS_DEBUG "-O3 -DNDEBUG")
set(CMAKE_CXX_FLAGS_DEBUG "-O3 -DNDEBUG")

# تعليمات ARM السريعة (dotprod/fp16) موجودة بكل المعالجات الحديثة
if(ANDROID_ABI STREQUAL "arm64-v8a")
    set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -march=armv8.2-a+dotprod+fp16")
    set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -march=armv8.2-a+dotprod+fp16")
endif()

set(BUILD_SHARED_LIBS     OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_TESTS     OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_EXAMPLES  OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_SERVER    OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_TOOLS     OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_COMMON    OFF CACHE BOOL "" FORCE)
set(LLAMA_CURL            OFF CACHE BOOL "" FORCE)
set(GGML_NATIVE           OFF CACHE BOOL "" FORCE)
set(GGML_OPENMP           OFF CACHE BOOL "" FORCE)

# llama.cpp بيتنزّل تلقائياً وقت البناء.
# لتثبيت نسخة معينة: -DLLAMA_CPP_TAG=b7xxx
# لاستعمال نسخة محلية بدون نت: -DFETCHCONTENT_SOURCE_DIR_LLAMA=/path/to/llama.cpp
set(LLAMA_CPP_TAG "master" CACHE STRING "llama.cpp git tag or commit")

include(FetchContent)
FetchContent_Declare(
    llama
    GIT_REPOSITORY https://github.com/ggml-org/llama.cpp.git
    GIT_TAG        ${LLAMA_CPP_TAG}
    GIT_SHALLOW    TRUE
)
FetchContent_MakeAvailable(llama)

add_library(hakim SHARED hakim_jni.cpp)
target_compile_features(hakim PRIVATE cxx_std_17)
target_link_libraries(hakim PRIVATE llama android log)
