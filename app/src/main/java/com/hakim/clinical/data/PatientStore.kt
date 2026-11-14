package com.hakim.clinical.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/** حفظ ملفات المرضى مشفّرة داخل التطبيق. */
class PatientStore(context: Context) {

    private val file = File(context.filesDir, "patients.enc")

    class LoadResult(val patients: List<Patient>, val error: String?)

    @Synchronized
    fun load(): LoadResult {
        if (!file.exists()) return LoadResult(emptyList(), null)
        return try {
            val json = String(Crypto.decrypt(file.readBytes()), Charsets.UTF_8)
            LoadResult(patientsFromJson(JSONArray(json)), null)
        } catch (e: Exception) {
            // ما منمسح الملف أبداً: منحتفظ فيه بنسخة جانبية
            runCatching { file.copyTo(File(file.parentFile, "patients.enc.unreadable-${System.currentTimeMillis()}")) }
            LoadResult(emptyList(), "ما قدرت افتح ملف المرضى المحفوظ. انحفظت نسخة منه، وفيك تسترجع من نسخة احتياطية.")
        }
    }

    @Synchronized
    fun save(patients: List<Patient>) {
        val bytes = Crypto.encrypt(patientsToJson(patients).toString().toByteArray(Charsets.UTF_8))
        val tmp = File(file.parentFile, "patients.enc.tmp")
        tmp.writeBytes(bytes)
        if (!tmp.renameTo(file)) {
            file.writeBytes(bytes)
            tmp.delete()
        }
    }

    companion object {

        fun patientsToJson(list: List<Patient>): JSONArray = JSONArray().apply { list.forEach { put(patientToJson(it)) } }

        fun patientsFromJson(arr: JSONArray): List<Patient> =
            List(arr.length()) { i -> patientFromJson(arr.getJSONObject(i)) }

        fun patientToJson(p: Patient) = JSONObject().apply {
            put("id", p.id)
            put("name", p.name)
            put("fileNo", p.fileNo)
            put("birthYear", p.birthYear ?: JSONObject.NULL)
            put("sex", p.sex)
            put("phone", p.phone)
            put("history", p.history)
            put("meds", p.meds)
            put("allergies", p.allergies)
            put("surgeries", p.surgeries)
            put("notes", p.notes)
            put("createdAt", p.createdAt)
            put("updatedAt", p.updatedAt)
            put("visits", JSONArray().apply { p.visits.forEach { put(visitToJson(it)) } })
        }

        fun patientFromJson(o: JSONObject): Patient {
            val va = o.optJSONArray("visits")
            return Patient(
                id = o.optString("id"),
                name = o.optString("name"),
                fileNo = o.optString("fileNo"),
                birthYear = if (o.isNull("birthYear")) null else o.optInt("birthYear"),
                sex = o.optString("sex"),
                phone = o.optString("phone"),
                history = o.optString("history"),
                meds = o.optString("meds"),
                allergies = o.optString("allergies"),
                surgeries = o.optString("surgeries"),
                notes = o.optString("notes"),
                createdAt = o.optLong("createdAt"),
                updatedAt = o.optLong("updatedAt"),
                visits = if (va == null) emptyList() else List(va.length()) { visitFromJson(va.getJSONObject(it)) },
            )
        }

        fun visitToJson(v: Visit) = JSONObject().apply {
            put("id", v.id)
            put("date", v.date)
            put("complaint", v.complaint)
            put("exam", v.exam)
            put("vitals", vitalsToJson(v.vitals))
            put("labs", v.labs)
            put("diagnosis", v.diagnosis)
            put("plan", v.plan)
            put("analysis", v.analysis)
            put("updatedAt", v.updatedAt)
        }

        fun visitFromJson(o: JSONObject) = Visit(
            id = o.optString("id"),
            date = o.optLong("date"),
            complaint = o.optString("complaint"),
            exam = o.optString("exam"),
            vitals = o.optJSONObject("vitals")?.let { vitalsFromJson(it) } ?: Vitals(),
            labs = o.optString("labs"),
            diagnosis = o.optString("diagnosis"),
            plan = o.optString("plan"),
            analysis = o.optString("analysis"),
            updatedAt = o.optLong("updatedAt"),
        )

        fun vitalsToJson(v: Vitals) = JSONObject().apply {
            put("bp", v.bp)
            put("hr", v.hr)
            put("temp", v.temp)
            put("spo2", v.spo2)
            put("rr", v.rr)
            put("glucose", v.glucose)
            put("weight", v.weight)
        }

        fun vitalsFromJson(o: JSONObject) = Vitals(
            bp = o.optString("bp"),
            hr = o.optString("hr"),
            temp = o.optString("temp"),
            spo2 = o.optString("spo2"),
            rr = o.optString("rr"),
            glucose = o.optString("glucose"),
            weight = o.optString("weight"),
        )
    }
}

/** النسخة الاحتياطية: المرضى + سجل التحليلات، مشفّرة بكلمة سر. */
object Backup {

    fun export(patients: List<Patient>, history: List<HistoryItem>, password: CharArray): ByteArray {
        val root = JSONObject().apply {
            put("app", "DrAli")
            put("version", 1)
            put("createdAt", System.currentTimeMillis())
            put("patients", PatientStore.patientsToJson(patients))
            put("history", HistoryStore.historyToJson(history))
        }
        return Crypto.encryptWithPassword(root.toString().toByteArray(Charsets.UTF_8), password)
    }

    class Imported(val patients: List<Patient>, val history: List<HistoryItem>)

    fun restore(data: ByteArray, password: CharArray): Imported {
        val root = JSONObject(String(Crypto.decryptWithPassword(data, password), Charsets.UTF_8))
        val pa = root.optJSONArray("patients") ?: JSONArray()
        val ha = root.optJSONArray("history") ?: JSONArray()
        return Imported(PatientStore.patientsFromJson(pa), HistoryStore.historyFromJson(ha))
    }
}
