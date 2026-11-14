// محرك حكيم: جسر JNI بين التطبيق و llama.cpp
// يشتغل بالكامل على الجهاز بدون إنترنت.

#include <jni.h>
#include <android/log.h>

#include <algorithm>
#include <atomic>
#include <mutex>
#include <string>
#include <vector>

#include "llama.h"

#define TAG "HakimNative"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

namespace {

struct Engine {
    llama_model *model = nullptr;
    const llama_vocab *vocab = nullptr;
    std::string chat_template;
};

std::atomic<bool> g_stop{false};
std::once_flag g_backend_once;

// أطول بادئة تنتهي بحرف UTF-8 كامل (الحروف العربية بايتين، وممكن التوكن يقطعها)
size_t utf8_complete_prefix(const std::string &s) {
    const size_t n = s.size();
    size_t i = n;
    int back = 0;
    while (i > 0 && back < 4) {
        const auto c = static_cast<unsigned char>(s[i - 1]);
        if ((c & 0xC0) != 0x80) {
            const size_t lead = i - 1;
            size_t need = 1;
            if (c >= 0x80) {
                if ((c & 0xE0) == 0xC0) need = 2;
                else if ((c & 0xF0) == 0xE0) need = 3;
                else if ((c & 0xF8) == 0xF0) need = 4;
            }
            return (n - lead >= need) ? n : lead;
        }
        --i;
        ++back;
    }
    return n;
}

std::string bytes_to_string(JNIEnv *env, jbyteArray arr) {
    if (arr == nullptr) return {};
    const jsize len = env->GetArrayLength(arr);
    std::string out(static_cast<size_t>(len), '\0');
    if (len > 0) env->GetByteArrayRegion(arr, 0, len, reinterpret_cast<jbyte *>(&out[0]));
    return out;
}

std::string token_to_piece(const llama_vocab *vocab, llama_token tok) {
    char buf[256];
    const int n = llama_token_to_piece(vocab, tok, buf, sizeof(buf), 0, false);
    if (n >= 0) return std::string(buf, static_cast<size_t>(n));
    std::string big(static_cast<size_t>(-n), '\0');
    const int m = llama_token_to_piece(vocab, tok, &big[0], static_cast<int32_t>(big.size()), 0, false);
    if (m < 0) return {};
    big.resize(static_cast<size_t>(m));
    return big;
}

std::vector<llama_token> tokenize(const llama_vocab *vocab, const std::string &text) {
    const int needed = -llama_tokenize(vocab, text.c_str(), static_cast<int32_t>(text.size()),
                                       nullptr, 0, true, true);
    if (needed <= 0) return {};
    std::vector<llama_token> toks(static_cast<size_t>(needed));
    const int m = llama_tokenize(vocab, text.c_str(), static_cast<int32_t>(text.size()),
                                 toks.data(), static_cast<int32_t>(toks.size()), true, true);
    if (m < 0) return {};
    toks.resize(static_cast<size_t>(m));
    return toks;
}

std::string apply_template(const Engine &e, const std::string &system, const std::string &user) {
    std::vector<llama_chat_message> msgs;
    if (!system.empty()) msgs.push_back({"system", system.c_str()});
    msgs.push_back({"user", user.c_str()});

    const char *tmpl = e.chat_template.empty() ? nullptr : e.chat_template.c_str();
    std::vector<char> buf(4096 + (system.size() + user.size()) * 2);
    int n = llama_chat_apply_template(tmpl, msgs.data(), msgs.size(), true,
                                      buf.data(), static_cast<int32_t>(buf.size()));
    if (n > static_cast<int>(buf.size())) {
        buf.resize(static_cast<size_t>(n) + 1);
        n = llama_chat_apply_template(tmpl, msgs.data(), msgs.size(), true,
                                      buf.data(), static_cast<int32_t>(buf.size()));
    }
    if (n < 0) {
        // قالب غير معروف: نستعمل صيغة Gemma (MedGemma مبني على Gemma)
        LOGI("unknown chat template, falling back to gemma format");
        return "<start_of_turn>user\n" + system + "\n\n" + user +
               "<end_of_turn>\n<start_of_turn>model\n";
    }
    return std::string(buf.data(), static_cast<size_t>(n));
}

void emit_text(JNIEnv *env, jobject cb, jmethodID on_text, const std::string &s) {
    if (s.empty()) return;
    jbyteArray arr = env->NewByteArray(static_cast<jsize>(s.size()));
    if (arr == nullptr) return;
    env->SetByteArrayRegion(arr, 0, static_cast<jsize>(s.size()),
                            reinterpret_cast<const jbyte *>(s.data()));
    env->CallVoidMethod(cb, on_text, arr);
    env->DeleteLocalRef(arr);
}

bool callback_failed(JNIEnv *env) {
    if (env->ExceptionCheck()) {
        env->ExceptionDescribe();
        env->ExceptionClear();
        return true;
    }
    return false;
}

} // namespace

// أكواد الرجوع من nativeGenerate:
//  >= 0 عدد التوكنات المولّدة
//  -1 فشل تقطيع النص، -2 فشل إنشاء السياق، -3 النص أطول من السياق، -4 فشل المعالجة
extern "C" {

JNIEXPORT jlong JNICALL
Java_com_hakim_clinical_engine_LlamaEngine_nativeLoad(JNIEnv *env, jobject /*thiz*/, jstring jpath) {
    std::call_once(g_backend_once, [] { llama_backend_init(); });

    const char *path = env->GetStringUTFChars(jpath, nullptr);
    llama_model_params mp = llama_model_default_params();
    mp.n_gpu_layers = 0;
    mp.use_mmap = true;
    llama_model *model = llama_model_load_from_file(path, mp);
    env->ReleaseStringUTFChars(jpath, path);
    if (model == nullptr) {
        LOGE("failed to load model");
        return 0;
    }

    auto *e = new Engine();
    e->model = model;
    e->vocab = llama_model_get_vocab(model);
    const char *tmpl = llama_model_chat_template(model, nullptr);
    if (tmpl != nullptr) e->chat_template = tmpl;
    LOGI("model loaded, template=%s", e->chat_template.empty() ? "(none)" : "(model)");
    return reinterpret_cast<jlong>(e);
}

JNIEXPORT void JNICALL
Java_com_hakim_clinical_engine_LlamaEngine_nativeFree(JNIEnv * /*env*/, jobject /*thiz*/, jlong handle) {
    auto *e = reinterpret_cast<Engine *>(handle);
    if (e == nullptr) return;
    if (e->model != nullptr) llama_model_free(e->model);
    delete e;
}

JNIEXPORT jstring JNICALL
Java_com_hakim_clinical_engine_LlamaEngine_nativeModelDesc(JNIEnv *env, jobject /*thiz*/, jlong handle) {
    auto *e = reinterpret_cast<Engine *>(handle);
    if (e == nullptr || e->model == nullptr) return env->NewStringUTF("");
    char buf[256];
    llama_model_desc(e->model, buf, sizeof(buf));
    return env->NewStringUTF(buf);
}

JNIEXPORT jstring JNICALL
Java_com_hakim_clinical_engine_LlamaEngine_nativeSystemInfo(JNIEnv *env, jobject /*thiz*/) {
    return env->NewStringUTF(llama_print_system_info());
}

JNIEXPORT void JNICALL
Java_com_hakim_clinical_engine_LlamaEngine_nativeStop(JNIEnv * /*env*/, jobject /*thiz*/) {
    g_stop.store(true);
}

JNIEXPORT jint JNICALL
Java_com_hakim_clinical_engine_LlamaEngine_nativeGenerate(
        JNIEnv *env, jobject /*thiz*/, jlong handle,
        jbyteArray jsystem, jbyteArray juser,
        jint nCtx, jint nThreads, jint maxTokens,
        jfloat temperature, jfloat topP, jint topK,
        jobject callback) {

    auto *e = reinterpret_cast<Engine *>(handle);
    if (e == nullptr || e->model == nullptr) return -2;
    g_stop.store(false);

    jclass cb_cls = env->GetObjectClass(callback);
    jmethodID on_text = env->GetMethodID(cb_cls, "onText", "([B)V");
    jmethodID on_progress = env->GetMethodID(cb_cls, "onProgress", "(II)V");
    if (on_text == nullptr || on_progress == nullptr) return -2;

    const std::string system = bytes_to_string(env, jsystem);
    const std::string user = bytes_to_string(env, juser);
    const std::string prompt = apply_template(*e, system, user);

    std::vector<llama_token> tokens = tokenize(e->vocab, prompt);
    if (tokens.empty()) return -1;

    llama_context_params cp = llama_context_default_params();
    cp.n_ctx = static_cast<uint32_t>(std::max(1024, static_cast<int>(nCtx)));
    cp.n_batch = 512;
    cp.n_ubatch = 512;
    cp.n_threads = std::max(1, static_cast<int>(nThreads));
    cp.n_threads_batch = std::max(1, static_cast<int>(nThreads));

    llama_context *ctx = llama_init_from_model(e->model, cp);
    if (ctx == nullptr) return -2;

    const int n_ctx = static_cast<int>(llama_n_ctx(ctx));
    const int n_prompt = static_cast<int>(tokens.size());
    if (n_prompt > n_ctx - 64) {
        llama_free(ctx);
        return -3;
    }
    const int budget = std::min(static_cast<int>(maxTokens), n_ctx - n_prompt - 8);

    // قراءة الحالة (معالجة النص على دفعات مع إظهار التقدم)
    const int n_batch = static_cast<int>(cp.n_batch);
    for (int i = 0; i < n_prompt; i += n_batch) {
        if (g_stop.load()) break;
        const int n = std::min(n_batch, n_prompt - i);
        llama_batch batch = llama_batch_get_one(tokens.data() + i, n);
        if (llama_decode(ctx, batch) != 0) {
            llama_free(ctx);
            return -4;
        }
        env->CallVoidMethod(callback, on_progress, static_cast<jint>(i + n), static_cast<jint>(n_prompt));
        if (callback_failed(env)) {
            g_stop.store(true);
            break;
        }
    }

    llama_sampler *smpl = llama_sampler_chain_init(llama_sampler_chain_default_params());
    llama_sampler_chain_add(smpl, llama_sampler_init_penalties(256, 1.05f, 0.0f, 0.0f));
    if (temperature <= 0.0f) {
        llama_sampler_chain_add(smpl, llama_sampler_init_greedy());
    } else {
        llama_sampler_chain_add(smpl, llama_sampler_init_top_k(std::max(1, static_cast<int>(topK))));
        llama_sampler_chain_add(smpl, llama_sampler_init_top_p(topP, 1));
        llama_sampler_chain_add(smpl, llama_sampler_init_temp(temperature));
        llama_sampler_chain_add(smpl, llama_sampler_init_dist(LLAMA_DEFAULT_SEED));
    }

    // كتابة التحليل توكن بتوكن
    std::string pending;
    int n_gen = 0;
    while (!g_stop.load() && n_gen < budget) {
        llama_token tok = llama_sampler_sample(smpl, ctx, -1);
        if (llama_vocab_is_eog(e->vocab, tok)) break;

        pending += token_to_piece(e->vocab, tok);
        const size_t k = utf8_complete_prefix(pending);
        if (k > 0) {
            emit_text(env, callback, on_text, pending.substr(0, k));
            pending.erase(0, k);
            if (callback_failed(env)) break;
        }

        llama_batch batch = llama_batch_get_one(&tok, 1);
        if (llama_decode(ctx, batch) != 0) break;
        ++n_gen;
    }
    if (!pending.empty()) {
        emit_text(env, callback, on_text, pending);
        callback_failed(env);
    }

    llama_sampler_free(smpl);
    llama_free(ctx);
    return n_gen;
}

} // extern "C"
