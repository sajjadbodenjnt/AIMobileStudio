#include <jni.h>
#include <string>
#include <vector>
#include <mutex>
#include <algorithm>
#include <cstring>
#include <android/log.h>
#include "llama.h"

#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, "DroidForge", __VA_ARGS__)

static llama_model* g_model = nullptr;
static llama_context* g_ctx = nullptr;
static std::mutex g_mutex;

extern "C" JNIEXPORT jboolean JNICALL
Java_com_droidforge_ai_LlamaNative_initModel(JNIEnv* env, jobject, jstring model_path) {
    std::lock_guard<std::mutex> lock(g_mutex);
    if (g_ctx) { llama_free(g_ctx); g_ctx = nullptr; }
    if (g_model) { llama_model_free(g_model); g_model = nullptr; }
    const char* path = env->GetStringUTFChars(model_path, nullptr);
    llama_backend_init();
    auto mp = llama_model_default_params();
    mp.n_gpu_layers = 0;
    g_model = llama_model_load_from_file(path, mp);
    env->ReleaseStringUTFChars(model_path, path);
    if (!g_model) { LOGE("Model load failed"); return JNI_FALSE; }
    auto cp = llama_context_default_params();
    cp.n_ctx = 2048;
    cp.n_batch = 512;
    cp.n_threads = 4;
    cp.n_threads_batch = 4;
    g_ctx = llama_init_from_model(g_model, cp);
    if (!g_ctx) { llama_model_free(g_model); g_model = nullptr; return JNI_FALSE; }
    return JNI_TRUE;
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_droidforge_ai_LlamaNative_generateCode(JNIEnv* env, jobject, jstring prompt, jint max_tokens) {
    std::lock_guard<std::mutex> lock(g_mutex);
    if (!g_model || !g_ctx) return env->NewStringUTF("ERROR: Model is not initialized.");
    const char* input = env->GetStringUTFChars(prompt, nullptr);
    const llama_vocab* vocab = llama_model_get_vocab(g_model);
    const int needed = -llama_tokenize(vocab, input, (int)strlen(input), nullptr, 0, true, true);
    if (needed <= 0 || needed > 8192) {
        env->ReleaseStringUTFChars(prompt, input);
        return env->NewStringUTF("ERROR: Prompt tokenization failed or prompt is too long.");
    }
    std::vector<llama_token> tokens((size_t)needed);
    int n = llama_tokenize(vocab, input, (int)strlen(input), tokens.data(), (int)tokens.size(), true, true);
    env->ReleaseStringUTFChars(prompt, input);
    if (n < 0) return env->NewStringUTF("ERROR: Tokenization failed.");
    tokens.resize((size_t)n);
    llama_kv_cache_clear(g_ctx);
    llama_batch batch = llama_batch_get_one(tokens.data(), (int)tokens.size());
    if (llama_decode(g_ctx, batch) != 0) return env->NewStringUTF("ERROR: Prompt evaluation failed.");
    auto* sampler = llama_sampler_chain_init(llama_sampler_chain_default_params());
    llama_sampler_chain_add(sampler, llama_sampler_init_temp(0.25f));
    llama_sampler_chain_add(sampler, llama_sampler_init_dist(LLAMA_DEFAULT_SEED));
    std::string output;
    std::vector<char> piece(8192);
    const int limit = std::max(1, std::min((int)max_tokens, 4096));
    for (int i = 0; i < limit; ++i) {
        llama_token tok = llama_sampler_sample(sampler, g_ctx, -1);
        if (llama_vocab_is_eog(vocab, tok)) break;
        llama_sampler_accept(sampler, tok);
        int len = llama_token_to_piece(vocab, tok, piece.data(), (int)piece.size(), 0, true);
        if (len > 0) output.append(piece.data(), (size_t)len);
        batch = llama_batch_get_one(&tok, 1);
        if (llama_decode(g_ctx, batch) != 0) break;
    }
    llama_sampler_free(sampler);
    return env->NewStringUTF(output.c_str());
}

extern "C" JNIEXPORT void JNICALL
Java_com_droidforge_ai_LlamaNative_release(JNIEnv*, jobject) {
    std::lock_guard<std::mutex> lock(g_mutex);
    if (g_ctx) { llama_free(g_ctx); g_ctx = nullptr; }
    if (g_model) { llama_model_free(g_model); g_model = nullptr; }
    llama_backend_free();
}
