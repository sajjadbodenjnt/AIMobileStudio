cmake_minimum_required(VERSION 3.22.1)
project(droidforge_llama LANGUAGES C CXX)
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(LLAMA_BUILD_TESTS OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_EXAMPLES OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_SERVER OFF CACHE BOOL "" FORCE)
set(GGML_OPENMP OFF CACHE BOOL "" FORCE)

include(FetchContent)
FetchContent_Declare(llama_cpp
    GIT_REPOSITORY https://github.com/ggerganov/llama.cpp.git
    GIT_TAG b4510
    GIT_SHALLOW TRUE
)
FetchContent_MakeAvailable(llama_cpp)

add_library(droidforge_llama SHARED llama_jni.cpp)
target_include_directories(droidforge_llama PRIVATE ${llama_cpp_SOURCE_DIR}/include)
find_library(android-lib android)
find_library(log-lib log)
target_link_libraries(droidforge_llama PRIVATE llama ${android-lib} ${log-lib})
