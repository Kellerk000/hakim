#!/usr/bin/env python3
"""
تجهيز بيانات تدريب "حكيم" من ملف clinical_cases_parallel_ar.jsonl

ماذا يفعل:
1) يحوّل كل جواب إلى التنسيق الموحّد الذي يفهمه التطبيق (نفس العناوين دائماً).
2) يضيف حالات "عدة احتمالات": تُحذف العلامة المميزة من الحالة، فيصبح الجواب
   قائمة احتمالات + ما يجب سؤاله أو فحصه للتفريق بينها (مشتق من الملف نفسه).
3) يأخذ عينة متوازنة لكل تشخيص لتقليل حفظ القوالب.
4) يبني المكتبة المرجعية (184 تشخيص) التي يستعملها التطبيق للبحث بدون إنترنت.

الاستعمال:
    python3 prepare_dataset.py path/to/clinical_cases_parallel_ar.jsonl
"""
import json
import random
import re
import sys
import unicodedata
from collections import defaultdict
from pathlib import Path

random.seed(20261114)

HERE = Path(__file__).resolve().parent
OUT_DATA = HERE / "data"
OUT_ASSET = HERE.parent / "app" / "src" / "main" / "assets" / "reference_library.json"

# يجب أن يطابق هذا النص حرفياً PromptBuilder.SYSTEM_PROMPT في التطبيق
SYSTEM_PROMPT = (
    "أنت \"حكيم\"، مساعد تشخيصي يدعم الطبيب ولا يحلّ محلّه. يكتب الطبيب الحالة بالفصحى أو بالعامية السورية.\n"
    "حلّل الحالة وأجب بهذا التنسيق فقط:\n"
    "**نوع النتيجة:** تشخيص مرجّح، أو عدة احتمالات\n"
    "**التشخيص الأرجح:** (مع تشخيص مرجّح) أو **الاحتمالات الأقرب:** (مع عدة احتمالات، مرتبة)\n"
    "**القرائن:**\n"
    "**كيف تأكد التشخيص:**\n"
    "**التشخيصات التفريقية:**\n"
    "**الخطوة التالية:**\n"
    "**الاستعجال:**\n"
    "**علامات الإنذار:**\n"
    "اختر \"تشخيص مرجّح\" فقط إذا كانت القرائن مميزة بوضوح. وإلا اختر \"عدة احتمالات\" واكتب في الخطوة التالية "
    "ما يجب سؤاله أو فحصه للتفريق بينها. لا تعطِ جرعات أدوية ولا خطوات جراحية، ونبّه فوراً إذا كانت الحالة إسعافية."
)

ACUITY_RANK = {"emergency": 3, "urgent": 2, "acute": 1, "chronic": 0}

TAIL = {
    "fusha": "القرار النهائي للطبيب بعد الفحص والنتائج.",
    "syrian": "القرار الأخير إلك بعد الفحص والنتائج.",
}
AMBIG_CLUE = {
    "fusha": "الشكوى: {c}، دون علامة مميزة كافية للحسم.",
    "syrian": "الشكوى: {c}، بس ما في علامة مميزة كافية لنحسم.",
}
AMBIG_STEP_HEAD = {
    "fusha": "اسأل وافحص عن العلامات التالية للتفريق:",
    "syrian": "اسأل وافحص عن هالعلامات لتفرّق:",
}
AMBIG_ARROW = {"fusha": "يرجّح", "syrian": "بيرجّح"}
AMBIG_NOCLUE = {
    "fusha": "إذا لم تظهر علامة مميزة: ابدأ بالفحوص الأساسية المذكورة وأعد التقييم.",
    "syrian": "إذا ما بيّنت علامة مميزة: ابدأ بالفحوص الأساسية وارجع قيّم.",
}


def norm(s: str) -> str:
    s = unicodedata.normalize("NFKC", s)
    s = re.sub(r"[ً-ْـ]", "", s)
    s = re.sub("[أإآ]", "ا", s).replace("ى", "ي").replace("ة", "ه")
    s = re.sub(r"\bال", "", s)
    return re.sub(r"\s+", " ", s).strip()


def parse_answer(text: str, lang: str) -> dict:
    lines = [l for l in text.split("\n") if l.strip()]
    star = [l for l in lines if l.startswith("**")]
    assert len(star) == 7, text

    def body(l):
        return l.split("**", 2)[2].strip()

    clues = body(star[1])
    sep = "ومن القصة: " if lang == "fusha" else "ومعها: "
    complaint, detail = clues.split(sep, 1)
    complaint = complaint.replace("الشكوى: ", "").strip().rstrip(".")
    return {
        "clues": clues.rstrip("."),
        "complaint": complaint,
        "detail": detail.strip().rstrip("."),
        "tests": body(star[3]).rstrip("."),
        "action": body(star[4]).rstrip("."),
        "urgency": body(star[5]).rstrip("."),
        "warnings": body(star[6]).rstrip("."),
    }


def strong_answer(dx: str, dd: list, a: dict, lang: str) -> str:
    return "\n".join([
        "**نوع النتيجة:** تشخيص مرجّح",
        f"**التشخيص الأرجح:** {dx}",
        f"**القرائن:** {a['clues']}.",
        f"**كيف تأكد التشخيص:** {a['tests']}.",
        f"**التشخيصات التفريقية:** {'، '.join(dd)}.",
        f"**الخطوة التالية:** {a['action']}.",
        f"**الاستعجال:** {a['urgency']}.",
        f"**علامات الإنذار:** {a['warnings']}.",
        TAIL[lang],
    ])


def main():
    src = Path(sys.argv[1]) if len(sys.argv) > 1 else HERE / "clinical_cases_parallel_ar.jsonl"
    recs = [json.loads(l) for l in src.open(encoding="utf-8")]
    print(f"records: {len(recs)}")

    # ملف تعريف لكل تشخيص (من أول سجل) بالفصحى والعامية
    profile = {}
    for r in recs:
        dx = r["diagnosis_reference"]
        if dx in profile:
            continue
        profile[dx] = {
            "dx": dx,
            "domain": r["domain"],
            "specialty": r["specialty"],
            "acuity": r["base_acuity"],
            "triage": r["triage"],
            "dd": r["differential_diagnoses"],
            "fusha": parse_answer(r["answer_fusha"], "fusha"),
            "syrian": parse_answer(r["answer_syrian"], "syrian"),
        }
    print(f"diagnoses: {len(profile)}")

    # ربط أسماء التشخيصات التفريقية بتشخيصات موجودة في الملف
    names = list(profile)
    nnames = {n: norm(n) for n in names}

    # روابط راجعتها يدوياً: الربط التلقائي يخطئ فيها (None = لا تربط)
    overrides = {
        "استرواح صدر": "استرواح صدر عفوي",
        "قرحة هضمية": "قرحة معدة أو عفج",
        "فتق إربي": "فتق إربي قابل للرد",
        "كيسة مبيض": "كيسة مبيض بسيطة",
        "حصاة حالب": "مغص كلوي بحصاة حالب",
        "التهاب فيروسي": None,
        "التهاب جلد": None,
        "فقر دم": None,
        "ألم عضلي": None,
        "التهاب معدة": None,
        "التهاب قولون": None,
        "اعتلال جذري": None,
    }

    def match(label: str):
        if label in overrides:
            return overrides[label]
        nl = norm(label)
        best, best_score = None, 0.0
        for n, nn in nnames.items():
            if nl == nn:
                return n
            if len(nl) >= 6 and (nl in nn or nn in nl):
                score = 0.9
            else:
                a, b = set(nl.split()), set(nn.split())
                score = len(a & b) / max(1, len(a | b))
            if score > best_score:
                best, best_score = n, score
        return best if best_score >= 0.6 else None

    dd_map = {}
    for dx, p in profile.items():
        dd_map[dx] = [(c, match(c)) for c in p["dd"]]
    matched = sum(1 for v in dd_map.values() for _, m in v if m)
    total = sum(len(v) for v in dd_map.values())
    print(f"differentials linked to a library diagnosis: {matched}/{total}")

    def ambiguous(r: dict, lang: str):
        dx = r["diagnosis_reference"]
        p = profile[dx]
        a = p[lang]
        case = r[f"case_{lang}"]
        # احذف الجملة التي تحتوي العلامة المميزة
        parts = [s for s in re.split(r"(?<=\.)\s+", case) if s.strip()]
        kept = [s for s in parts if a["detail"] not in s]
        if len(kept) == len(parts):
            return None
        new_case = " ".join(kept)

        cands = [(dx, dx)] + dd_map[dx]
        steps = [f"- {a['detail']} ← {AMBIG_ARROW[lang]} {dx}"]
        tests = [f"{dx}: {a['tests']}"]
        urg_src = p
        warns = [a["warnings"]]
        for label, m in cands[1:]:
            if not m or m == dx:
                continue
            q = profile[m]
            steps.append(f"- {q[lang]['detail']} ← {AMBIG_ARROW[lang]} {label}")
            if len(tests) < 3:
                tests.append(f"{label}: {q[lang]['tests']}")
            if ACUITY_RANK[q["acuity"]] > ACUITY_RANK[urg_src["acuity"]]:
                urg_src = q
        if urg_src is not p:
            warns.append(urg_src[lang]["warnings"])
        if len(steps) < 2:
            steps.append(f"- {AMBIG_NOCLUE[lang]}")

        answer = "\n".join([
            "**نوع النتيجة:** عدة احتمالات",
            f"**الاحتمالات الأقرب:** {'، '.join(c for c, _ in cands)}",
            f"**القرائن:** {AMBIG_CLUE[lang].format(c=a['complaint'])}",
            f"**الخطوة التالية:** {AMBIG_STEP_HEAD[lang]}\n" + "\n".join(steps),
            f"**كيف تأكد التشخيص:** {'؛ '.join(tests)}.",
            f"**الاستعجال:** {urg_src[lang]['urgency']}.",
            f"**علامات الإنذار:** {'؛ '.join(warns)}.",
            TAIL[lang],
        ])
        return new_case, answer

    def user_msg(case: str, q: str) -> str:
        return case if random.random() < 0.5 else f"{case}\n{q}"

    def example(user: str, assistant: str, meta: dict) -> dict:
        return {
            "messages": [
                {"role": "system", "content": SYSTEM_PROMPT},
                {"role": "user", "content": user},
                {"role": "assistant", "content": assistant},
            ],
            "meta": meta,
        }

    by = defaultdict(list)
    for r in recs:
        by[(r["split"], r["diagnosis_reference"])].append(r)

    plan = {"train": (6, 2), "validation": (1, 1), "test": (1, 1)}
    out = {k: [] for k in plan}
    for (split, dx), rs in by.items():
        n_strong, n_amb = plan[split]
        p = profile[dx]
        for lang in ("fusha", "syrian"):
            pool = rs[:]
            random.shuffle(pool)
            # تنويع مراحل الحالة (قصة فقط / مع علامات حيوية / ...)
            pool.sort(key=lambda r: r["case_stage"])
            stages = defaultdict(list)
            for r in pool:
                stages[r["case_stage"]].append(r)
            picked = []
            while len(picked) < n_strong and any(stages.values()):
                for st in list(stages):
                    if stages[st] and len(picked) < n_strong:
                        picked.append(stages[st].pop())
            for r in picked:
                a = parse_answer(r[f"answer_{lang}"], lang)
                out[split].append(example(
                    user_msg(r[f"case_{lang}"], r[f"question_{lang}"]),
                    strong_answer(dx, r["differential_diagnoses"], a, lang),
                    {"id": r["id"], "dx": dx, "lang": lang, "type": "strong"},
                ))
            amb_pool = [r for r in rs if r not in picked] or rs
            random.shuffle(amb_pool)
            made = 0
            for r in amb_pool:
                if made >= n_amb:
                    break
                res = ambiguous(r, lang)
                if not res:
                    continue
                case, ans = res
                out[split].append(example(
                    user_msg(case, r[f"question_{lang}"]), ans,
                    {"id": r["id"] + "-amb", "dx": dx, "lang": lang, "type": "multiple"},
                ))
                made += 1

    OUT_DATA.mkdir(parents=True, exist_ok=True)
    names_out = {"train": "train.jsonl", "validation": "val.jsonl", "test": "test.jsonl"}
    for split, rows in out.items():
        random.shuffle(rows)
        path = OUT_DATA / names_out[split]
        with path.open("w", encoding="utf-8") as f:
            for row in rows:
                f.write(json.dumps(row, ensure_ascii=False) + "\n")
        n_m = sum(1 for r in rows if r["meta"]["type"] == "multiple")
        print(f"{split}: {len(rows)} examples ({n_m} multiple-diagnosis) -> {path}")

    # المكتبة المرجعية للتطبيق
    lib = []
    for dx, p in profile.items():
        lib.append({
            "dx": dx,
            "domain": p["domain"],
            "specialty": p["specialty"],
            "acuity": p["acuity"],
            "triage": p["triage"],
            "dd": p["dd"],
            "complaint": p["fusha"]["complaint"],
            "detail": p["fusha"]["detail"],
            "complaint_s": p["syrian"]["complaint"],
            "detail_s": p["syrian"]["detail"],
            "tests": p["fusha"]["tests"],
            "action": p["fusha"]["action"],
            "warnings": p["fusha"]["warnings"],
        })
    OUT_ASSET.parent.mkdir(parents=True, exist_ok=True)
    OUT_ASSET.write_text(json.dumps(lib, ensure_ascii=False, indent=0), encoding="utf-8")
    print(f"reference library: {len(lib)} entries -> {OUT_ASSET}")


if __name__ == "__main__":
    main()
