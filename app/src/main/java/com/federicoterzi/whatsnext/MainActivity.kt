package com.federicoterzi.whatsnext

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.federicoterzi.whatsnext.model.Lesson
import kotlinx.android.synthetic.main.activity_main.*
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.*

const val REQUEST_URL = "https://corsi.unibo.it/magistrale/ingegneriainformatica/orario-lezioni/@@orario_reale_json?"

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    override fun onStart() {
        super.onStart()

        requestSchedule()
    }

    private fun requestSchedule() {
        doAsync {
            val client = OkHttpClient()
            val request = Request.Builder().url(REQUEST_URL).build()
            val response = client.newCall(request).execute()
            val jsonResponse = JSONObject(response.body()?.string())
            val lessons = extractLessons(jsonResponse)

            val currentLesson = findCurrentLesson(lessons)
            val nextLesson = findNextLesson(lessons)
            Log.d("Lesson", nextLesson.toString())

            // Populate the fields
            uiThread {
                roomTextView.text = nextLesson?.room
                lessonTextView.text = nextLesson?.title
                teacherTextView.text = nextLesson?.teacher
                val format = SimpleDateFormat("HH:mm")
                val finalDate = "${format.format(nextLesson?.start)} - ${format.format(nextLesson?.end)}"
                dateTextView.text = finalDate

                val currentLessonText = if (currentLesson != null) {
                    "La lezione di ${currentLesson?.title} finirà alle: ${format.format(currentLesson?.end)}"
                }else{
                    "Non hai lezioni adesso, yey!"
                }
                currentTextView.text = currentLessonText
            }
        }
    }

    private fun extractLessons(response: JSONObject) : List<Lesson>{
        val output = mutableListOf<Lesson>()

        val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss")
        val events = response.getJSONArray("events")
        for (i in 0 until events.length()){
            val event = events.getJSONObject(i)
            val title = event.getString("title")
            val startDate = format.parse(event.getString("start"))
            val endDate = format.parse(event.getString("end"))
            val teacher = event.getString("docente")
            val roomsJson = event.getJSONArray("aule")
            val firstRoomJson = roomsJson.getJSONObject(0)
            val room = firstRoomJson.getString("des_risorsa")

            val lesson = Lesson(title, teacher, room, startDate, endDate)
            output.add(lesson)
        }

        return output
    }

    private fun findCurrentLesson(lessons: List<Lesson>): Lesson? {
        val nowDate = Calendar.getInstance().time

        for (lesson in lessons) {
            if (nowDate.after(lesson.start) && nowDate.before(lesson.end) ) {
                return lesson
            }
        }

        return null
    }

    private fun findNextLesson(lessons: List<Lesson>): Lesson? {
        val nowDate = Calendar.getInstance().time

        for (lesson in lessons) {
            if (nowDate.before(lesson.start) && lesson.start.time < ( nowDate.time + 1000 * 60* 60 * 8)) {
                return lesson
            }
        }

        return null
    }
}
