package com.federicoterzi.whatsnext.model

import java.util.*

data class Lesson(val title: String, val teacher: String, val room: String,
                  val start: Date, val end: Date)