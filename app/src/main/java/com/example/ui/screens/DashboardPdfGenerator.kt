package com.example.ui.screens

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import com.example.data.repository.ClassScheduleRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.FileOutputStream
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Generates the system-wide "Dashboard" PDF: a multi-page statistical report
 * covering every dataset recorded in the app — students, classes, weekly
 * routine, homework & submissions, exams & marks, behavior logs, calendar
 * events, holidays and syllabi — presented with stat tiles, bar charts, a
 * donut chart and detailed tables. The finished document is handed to the
 * Android print framework so it can be saved or shared as a real PDF without
 * any storage permission.
 */
object DashboardPdfGenerator {

    private const val PAGE_W = 595
    private const val PAGE_H = 842
    private const val ML = 40f   // content margin left
    private const val MR = 555f  // content margin right
    private const val CONTENT_W = MR - ML

    private val INDIGO = Color.parseColor("#3F51B5")
    private val GREEN = Color.parseColor("#2E7D32")
    private val AMBER = Color.parseColor("#F9A825")
    private val RED = Color.parseColor("#C62828")
    private val TEAL = Color.parseColor("#26A69A")
    private val PURPLE = Color.parseColor("#7E57C2")
    private val BLUE = Color.parseColor("#5C6BC0")

    private val POSITIVE_TYPES = setOf("Impressed by Engaging", "Positive Curiosity", "Got Prize")
    private val NEGATIVE_TYPES = setOf(
        "Named by Monitor", "Making Noise", "Name By Monitor",
        "Sent Outside", "Disturbed Class", "Scoldings"
    )

    private fun clip(s: String, max: Int): String =
        if (s.length > max) s.take(max - 1) + "…" else s

    /** One ranked row in the "Top Performers" table. */
    private data class PerfRow(
        val name: String,
        val className: String,
        val hwDone: Int,
        val hwTotal: Int,
        val positive: Int,
        val negative: Int,
        val avgPercent: Double?,
        val score: Double
    )

    /** One row in the per-exam breakdown table. */
    private data class ExamRow(
        val name: String,
        val classes: String,
        val count: Int,
        val average: Double,
        val highest: Double,
        val topScorer: String,
        val passPercent: Int
    )

    /** Manages pages, paints and the vertical cursor for the multi-page report. */
    private class PdfSession(
        private val pdf: PdfDocument,
        private val teacherName: String
    ) {
        var pageNum = 0
            private set
        private var page: PdfDocument.Page? = null
        lateinit var canvas: Canvas
            private set
        var y = 0f
            private set

        val white = Paint().apply { color = Color.WHITE; textSize = 9f; isFakeBoldText = true }
        val whiteBig = Paint().apply { color = Color.WHITE; textSize = 16f; isFakeBoldText = true }
        val sub = Paint().apply { color = Color.WHITE; textSize = 8f }
        val heading = Paint().apply { color = INDIGO; textSize = 13f; isFakeBoldText = true }
        val body = Paint().apply { color = Color.DKGRAY; textSize = 9.5f }
        val bodyBold = Paint().apply { color = Color.BLACK; textSize = 9.5f; isFakeBoldText = true }
        val micro = Paint().apply { color = Color.GRAY; textSize = 8f }
        val headerCell = Paint().apply { color = Color.WHITE; textSize = 8.5f; isFakeBoldText = true }
        val grid = Paint().apply { color = Color.LTGRAY; strokeWidth = 1f; style = Paint.Style.STROKE }
        val altRow = Paint().apply { color = Color.parseColor("#EFEBFF") }

        init { newPage() }

        fun newPage() {
            page?.let { pdf.finishPage(it) }
            pageNum++
            val p = pdf.startPage(PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, pageNum).create())
            page = p
            canvas = p.canvas
            val band = Paint().apply { color = INDIGO; style = Paint.Style.FILL }
            canvas.drawRect(0f, 0f, PAGE_W.toFloat(), 76f, band)
            canvas.drawText("SYSTEM DASHBOARD", ML, 30f, whiteBig)
            canvas.drawText(teacherName.ifBlank { "Teacher" }, ML, 48f, white)
            canvas.drawText(
                "Generated ${SimpleDateFormat("EEE, MMM d, yyyy • h:mm a", Locale.getDefault()).format(Date())}",
                ML, 63f, sub
            )
            canvas.drawText("Page $pageNum", MR - 45f, 48f, white)
            y = 96f
        }

        fun finish() {
            page?.let { pdf.finishPage(it) }
        }

        fun ensureSpace(needed: Float) {
            if (y + needed > 800f) newPage()
        }

        fun section(title: String, subTitle: String? = null) {
            ensureSpace(34f)
            canvas.drawText(title.uppercase(), ML, y + 10f, heading)
            if (subTitle != null) {
                val right = Paint(sub).apply { textAlign = Paint.Align.RIGHT }
                canvas.drawText(subTitle, MR - 10f, y + 10f, right)
            }
            canvas.drawLine(ML, y + 16f, MR, y + 16f, grid)
            y += 26f
        }

        fun statTile(x: Float, tY: Float, w: Float, h: Float, value: String, label: String, color: Int) {
            val bg = Paint().apply { this.color = color; alpha = 26; style = Paint.Style.FILL }
            canvas.drawRoundRect(RectF(x, tY, x + w, tY + h), 10f, 10f, bg)
            val vp = Paint().apply { this.color = color; textSize = 20f; isFakeBoldText = true; textAlign = Paint.Align.CENTER }
            canvas.drawText(value, x + w / 2f, tY + h * 0.55f, vp)
            val lp = Paint().apply { this.color = Color.DKGRAY; textSize = 8f; textAlign = Paint.Align.CENTER }
            canvas.drawText(label, x + w / 2f, tY + h * 0.86f, lp)
        }

        fun statRow(yRow: Float, h: Float, tiles: List<Triple<String, String, Int>>) {
            val gap = 10f
            val count = tiles.size
            val w = (CONTENT_W - gap * (count - 1)) / count
            var x = ML
            tiles.forEach { (value, label, color) ->
                statTile(x, yRow, w, h, value, label, color)
                x += w + gap
            }
            y = yRow + h + 14f
        }

        fun advance(by: Float) {
            y += by
        }
    fun barChart(items: List<Triple<String, Float, Int>>) {
            if (items.isEmpty()) {
                emptyHint()
                return
            }
            val rowH = 22f
            val labelW = 170f
            val barMaxW = CONTENT_W - labelW - 40f
            val lp = Paint(body).apply { textAlign = Paint.Align.LEFT }
            items.forEach { (label, frac, color) ->
                ensureSpace(rowH)
                canvas.drawText(clip(label, 22), ML, y + 13f, lp)
                val barColor = Paint().apply { this.color = color; style = Paint.Style.FILL }
                val bw = (barMaxW * frac.coerceIn(0f, 1f)).coerceAtLeast(6f)
                canvas.drawRoundRect(RectF(ML + labelW, y + 3f, ML + labelW + bw, y + 17f), 5f, 5f, barColor)
                canvas.drawText("${(frac * 100).toInt()}%", ML + labelW + barMaxW + 6f, y + 13f, Paint(micro).apply { textAlign = Paint.Align.LEFT })
                y += rowH
            }
        }

        fun donut(cx: Float, cy: Float, radius: Float, parts: List<Triple<String, Float, Int>>, centerValue: String, centerLabel: String) {
            val total = parts.map { it.second }.sum()
            if (total <= 0f) return
            var start = -90f
            val stroke = Paint().apply { style = Paint.Style.STROKE; strokeWidth = 24f; isAntiAlias = true }
            parts.forEach { (_, frac, color) ->
                val sweep = frac / total * 360f
                if (sweep > 0f) {
                    stroke.color = color
                    canvas.drawArc(RectF(cx - radius, cy - radius, cx + radius, cy + radius), start, sweep - 0.8f, false, stroke)
                    start += sweep
                }
            }
            val cvp = Paint().apply { color = Color.BLACK; textSize = 17f; isFakeBoldText = true; textAlign = Paint.Align.CENTER }
            canvas.drawText(centerValue, cx, cy - 2f, cvp)
            val clp = Paint().apply { color = Color.DKGRAY; textSize = 7.5f; textAlign = Paint.Align.CENTER }
            canvas.drawText(centerLabel, cx, cy + 12f, clp)
        }

        fun legend(x: Float, tY: Float, parts: List<Triple<String, Float, Int>>) {
            var yy = tY
            parts.forEach { (label, frac, color) ->
                val sw = Paint().apply { this.color = color; style = Paint.Style.FILL }
                canvas.drawRoundRect(RectF(x, yy, x + 10f, yy + 10f), 3f, 3f, sw)
                val claim = if (frac > 0f) "  •  ${java.lang.String.format(Locale.US, "%.0f", frac)}" else "  •  0"
                canvas.drawText("$label$claim", x + 14f, yy + 8f, body)
                yy += 18f
            }
        }

        fun emptyHint() {
            ensureSpace(24f)
            canvas.drawText("— No data recorded yet —", ML, y + 12f, micro)
            y += 22f
        }

        fun table(headers: List<String>, rows: List<List<String>>, weights: List<Float>, columnColors: Map<Int, Int> = emptyMap()) {
            if (rows.isEmpty()) {
                emptyHint()
                return
            }
            val widths = weights.map { CONTENT_W * it }
            ensureSpace(22f)
            val hbg = Paint().apply { color = INDIGO; style = Paint.Style.FILL }
            canvas.drawRoundRect(RectF(ML, y, MR, y + 20f), 6f, 6f, hbg)
            var x = ML
            headers.forEachIndexed { i, h ->
                canvas.drawText(clip(h, 24), x + 5f, y + 13f, Paint(headerCell).apply { textAlign = Paint.Align.LEFT })
                x += widths[i]
            }
            y += 24f
            var rowIdx = 0
            rows.forEach { row ->
                ensureSpace(21f)
                if (rowIdx % 2 == 1) canvas.drawRect(ML, y - 2f, MR, y + 16f, altRow)
                x = ML
                row.forEachIndexed { i, cell ->
                    val p = if (i == 0) {
                        Paint(bodyBold).apply { textAlign = Paint.Align.LEFT }
                    } else {
                        Paint(body).apply { textAlign = Paint.Align.LEFT; color = columnColors[i] ?: body.color }
                    }
                    canvas.drawText(clip(cell, 26), x + 5f, y + 11f, p)
                    x += widths[i]
                }
                canvas.drawLine(ML, y + 17f, MR, y + 17f, grid)
                y += 21f
                rowIdx++
            }
            y += 6f
        }
    }

    suspend fun generate(
        repository: ClassScheduleRepository,
        context: Context,
        teacherName: String,
        weeklyHolidayCount: Int = 0,
        onSuccess: (String) -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        try {
            // ---------- 1. Collect every dataset the system owns ----------
            val students = repository.allStudents.first()
            val submissions = repository.homeworkSubmissionDao.getAllSubmissions().first()
            val activities = repository.studentActivityDao.getAllActivities().first()
            val homework = repository.allHomework.first()
            val exams = repository.allExams.first()
            val marks = repository.examDao.getAllMarks().first()
            val events = repository.allDatedEvents.first()
            val holidays = repository.allHolidays.first()
            val routine = repository.allClasses.first()
            val managed = repository.allManagedClasses.first()
            val syllabi = repository.allSyllabuses.first()
            val dec = DecimalFormat("#.#")

            // ---------- 2. Derived numbers ----------
            val studentsByClass = students.groupBy { it.className }
            val classNames = managed.map { it.name }.ifEmpty { studentsByClass.keys.sorted() }
            val hwTotal = homework.size
            val hwDoneCount = homework.count { it.isCompleted }
            val subDone = submissions.count { it.status.lowercase() == "done" }
            val subHalf = submissions.count { it.status.lowercase() == "half done" }
            val subNot = submissions.count { it.status.lowercase() == "not done" }
            val subTotal = submissions.size
            val completedEvents = events.count { it.eventStatus == "COMPLETED" }
            val cancelledEvents = events.count { it.eventStatus == "CANCELLED" }
            val failedEvents = events.count { it.eventStatus == "FAILED" }
            val pendingEvents = events.size - completedEvents - cancelledEvents - failedEvents
            val behaviorPositive = activities.count { it.activityType in POSITIVE_TYPES }
            val behaviorNegative = activities.count { it.activityType in NEGATIVE_TYPES }
            val behaviorByType = activities.groupingBy { it.activityType }.eachCount().toSortedMap()

            val marksForStudent = marks.groupBy { it.studentId }
            val studentPerf = students.map { s ->
                val subs = submissions.filter { it.studentId == s.id }
                val done = subs.count { it.status.lowercase() == "done" }
                val acts = activities.filter { it.studentId == s.id }
                val pos = acts.count { it.activityType in POSITIVE_TYPES }
                val neg = acts.count { it.activityType in NEGATIVE_TYPES }
                val pcts = marksForStudent[s.id].orEmpty().mapNotNull { m ->
                    val ex = exams.find { it.id == m.examId } ?: return@mapNotNull null
                    val full = ex.fullMarks.toDoubleOrNull()
                    val got = m.marksObtained.toDoubleOrNull()
                    if (full != null && full > 0 && got != null) got / full * 100.0 else null
                }
                PerfRow(s.name, s.className, done, subs.size, pos, neg, if (pcts.isEmpty()) null else pcts.average(), 0.0)
            }.map { p ->
                val hwRate = if (p.hwTotal > 0) p.hwDone.toDouble() / p.hwTotal else 1.0
                val beh = (p.positive - 2 * p.negative).coerceIn(-5, 10)
                val behScore = ((beh + 5) / 15f) * 20
                val examScore = (p.avgPercent ?: 0.0) * 0.4
                p.copy(score = hwRate * 40 + behScore + examScore)
            }.sortedByDescending { it.score }

            val examRows = exams.map { ex ->
                val exMarks = marks.filter { it.examId == ex.id }
                val nums = exMarks.mapNotNull { it.marksObtained.toDoubleOrNull() }
                val passInt = ex.passMarks.toIntOrNull() ?: 40
                val passCount = nums.count { it >= passInt }
                val maxV = nums.maxOrNull()
                val topScorer = if (maxV == null) "—" else exMarks.firstOrNull { it.marksObtained.toDoubleOrNull() == maxV }
                    ?.let { st -> students.find { s -> s.id == st.studentId }?.name } ?: "—"
                ExamRow(
                    ex.name,
                    ex.targetClassNames?.replace(",", ", ")?.ifBlank { "All Classes" } ?: "All Classes",
                    exMarks.size,
                    if (nums.isEmpty()) 0.0 else nums.average(),
                    maxV ?: 0.0,
                    topScorer,
                    if (nums.isEmpty()) 0 else (passCount * 100 / nums.size)
                )
            }.sortedByDescending { it.average }

            val pdf = PdfDocument()
            val s = PdfSession(pdf, teacherName)
    // ---- Page 1: At a Glance + chart sections ----
            s.section("At a Glance")
            s.statRow(s.y, 54f, listOf(
                Triple(students.size.toString(), "Students", INDIGO),
                Triple(managed.size.toString(), "Classes", TEAL),
                Triple(routine.size.toString(), "Routine Periods", BLUE),
                Triple(hwTotal.toString(), "Homeworks", GREEN),
                Triple(exams.size.toString(), "Exams", PURPLE),
                Triple(events.size.toString(), "Events", AMBER)
            ))
            s.statRow(s.y, 54f, listOf(
                Triple(if (hwTotal > 0) "${hwDoneCount * 100 / hwTotal}%" else "0%", "HW Completed", GREEN),
                Triple(subTotal.toString(), "HW Evaluations", TEAL),
                Triple(activities.size.toString(), "Behavior Logs", PURPLE),
                Triple(holidays.size.toString(), "Holidays", AMBER),
                Triple(weeklyHolidayCount.toString(), "Weekly Offs", RED),
                Triple(syllabi.size.toString(), "Syllabi", BLUE)
            ))

            s.section("Homework Status")
            s.donut(ML + 90f, s.y + 70f, 52f, listOf(
                Triple("Done", subDone.toFloat(), GREEN),
                Triple("Half Done", subHalf.toFloat(), AMBER),
                Triple("Not Done", subNot.toFloat(), RED)
            ), subTotal.toString(), "Evaluations")
            s.legend(ML + 220f, s.y + 30f, listOf(
                Triple("Done", subDone.toFloat(), GREEN),
                Triple("Half Done", subHalf.toFloat(), AMBER),
                Triple("Not Done", subNot.toFloat(), RED)
            ))
            s.advance(120f)

            s.section("Event Outcomes")
            s.donut(ML + 90f, s.y + 70f, 52f, listOf(
                Triple("Completed", completedEvents.toFloat(), GREEN),
                Triple("Cancelled", cancelledEvents.toFloat(), AMBER),
                Triple("Failed", failedEvents.toFloat(), RED),
                Triple("Pending", pendingEvents.toFloat(), BLUE)
            ), events.size.toString(), "Events")
            s.legend(ML + 220f, s.y + 30f, listOf(
                Triple("Completed", completedEvents.toFloat(), GREEN),
                Triple("Cancelled", cancelledEvents.toFloat(), AMBER),
                Triple("Failed", failedEvents.toFloat(), RED),
                Triple("Pending", pendingEvents.toFloat(), BLUE)
            ))
            s.advance(120f)

            // ---- Page 2: Class details + Top performers ----
            s.section("Class Details")
            val classRows = classNames.map { cn ->
                val classStudents = students.filter { it.className == cn }
                val classHw = homework.count { hw ->
                    routine.find { it.id == hw.classId }?.name == cn
                }
                val classSubs = submissions.filter { sub ->
                    classStudents.any { it.id == sub.studentId }
                }
                val done = classSubs.count { it.status.lowercase() == "done" }
                val pos = activities.count { a -> classStudents.any { it.id == a.studentId } && a.activityType in POSITIVE_TYPES }
                val neg = activities.count { a -> classStudents.any { it.id == a.studentId } && a.activityType in NEGATIVE_TYPES }
                val percents = classStudents.flatMap { st ->
                    marksForStudent[st.id].orEmpty().mapNotNull { m ->
                        val ex = exams.find { it.id == m.examId } ?: return@mapNotNull null
                        val full = ex.fullMarks.toDoubleOrNull()
                        val got = m.marksObtained.toDoubleOrNull()
                        if (full != null && full > 0 && got != null) got / full * 100.0 else null
                    }
                }
                listOf(
                    cn,
                    classStudents.size.toString(),
                    classHw.toString(),
                    done.toString(),
                    if (classSubs.isEmpty()) "—" else "${done * 100 / classSubs.size}%",
                    pos.toString(),
                    neg.toString(),
                    if (percents.isEmpty()) "—" else dec.format(percents.average()) + "%"
                )
            }
            s.table(
                listOf("Class", "Students", "HW Given", "HW Done", "HW Rate", "Pos+", "Neg–", "Avg Exam"),
                classRows,
                listOf(0.16f, 0.11f, 0.11f, 0.10f, 0.10f, 0.08f, 0.08f, 0.12f)
            )

            s.section("Top Performers (all-time score)")
            val perfRows = studentPerf.take(15).mapIndexed { i, p ->
                listOf(
                    (i + 1).toString(),
                    p.name,
                    p.className,
                    if (p.hwTotal > 0) "${p.hwDone * 100 / p.hwTotal}%" else "—",
                    p.positive.toString(),
                    p.negative.toString(),
                    p.avgPercent?.let { dec.format(it) + "%" } ?: "—",
                    dec.format(p.score)
                )
            }
            s.table(
                listOf("#", "Student", "Class", "HW%", "Pos+", "Neg–", "Avg Exam", "Score"),
                perfRows,
                listOf(0.05f, 0.22f, 0.11f, 0.09f, 0.08f, 0.08f, 0.14f, 0.12f)
            )
    // ---- Exams ----
            s.section("Exam Analysis")
            val exTable = examRows.map { r ->
                listOf(r.name, r.classes, r.count.toString(), dec.format(r.average), dec.format(r.highest), r.topScorer, "${r.passPercent}%")
            }
            s.table(
                listOf("Exam", "Classes", "Students", "Avg", "Highest", "Top Scorer", "Pass%"),
                exTable,
                listOf(0.20f, 0.22f, 0.11f, 0.10f, 0.10f, 0.18f, 0.10f)
            )
            if (examRows.isNotEmpty()) {
                s.section("Average Marks Chart")
                val maxAvg = examRows.maxOf { it.average }.coerceAtLeast(1.0)
                s.barChart(
                    examRows.map { Triple(it.name, (it.average / (maxAvg * 1.15)).toFloat(), INDIGO) }
                )
            }

            // ---- Behavior + Holidays + Syllabus + Events ----
            s.section("Behavior Logs")
            val behRows = behaviorByType.entries.map { listOf(it.key, it.value.toString()) }
            s.table(
                listOf("Behavior Type", "Count"),
                behRows,
                listOf(0.6f, 0.4f),
                columnColors = mapOf(1 to TEAL)
            )
            s.statRow(s.y, 40f, listOf(
                Triple(behaviorPositive.toString(), "Positive Logs", GREEN),
                Triple(behaviorNegative.toString(), "Negative Logs", RED),
                Triple(activities.size.toString(), "Total Logs", PURPLE)
            ))

            s.section("Holidays")
            s.statRow(s.y, 40f, listOf(
                Triple(weeklyHolidayCount.toString(), "Weekly Off Days", AMBER),
                Triple(holidays.size.toString(), "Saved Ranges", BLUE)
            ))
            if (holidays.isEmpty() && weeklyHolidayCount == 0) s.emptyHint()

            s.section("Syllabus Overview")
            val sylRows = syllabi.map { listOf(it.subject, it.className) }
            s.table(
                listOf("Subject", "Class"),
                sylRows,
                listOf(0.6f, 0.4f)
            )

            s.section("Registered Events")
            val evRows = events.sortedBy { it.eventEpochDay ?: 0L }.take(25).map { ev ->
                val d = ev.eventEpochDay?.let {
                    try {
                        SimpleDateFormat("MMM d", Locale.US).format(java.util.Date(it * 86400000L))
                    } catch (e: Exception) { "" }
                } ?: ""
                listOf(ev.title, d, ev.eventStatus.ifBlank { "Pending" })
            }
            s.table(
                listOf("Event", "Date", "Status"),
                evRows,
                listOf(0.5f, 0.2f, 0.3f),
                columnColors = mapOf(2 to PURPLE)
            )

            s.finish()

            // Hand the finished document to the Android print framework so the
            // user can save / share it as a real PDF (no storage permission).
            withContext(Dispatchers.Main) {
                val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
                printManager?.print(
                    "System_Dashboard",
                    object : PrintDocumentAdapter() {
                        override fun onLayout(
                            oldAttributes: PrintAttributes?,
                            newAttributes: PrintAttributes?,
                            cancellationSignal: CancellationSignal?,
                            callback: PrintDocumentAdapter.LayoutResultCallback?,
                            extras: Bundle?
                        ) {
                            if (cancellationSignal?.isCanceled == true) {
                                callback?.onLayoutCancelled()
                                return
                            }
                            callback?.onLayoutFinished(
                                PrintDocumentInfo.Builder("System_Dashboard")
                                    .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                                    .build(),
                                true
                            )
                        }

                        override fun onWrite(pages: Array<out PageRange>?, destination: ParcelFileDescriptor?, cancellationSignal: CancellationSignal?, callback: PrintDocumentAdapter.WriteResultCallback?) {
                            try {
                                pdf.writeTo(FileOutputStream(destination?.fileDescriptor))
                                callback?.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
                            } catch (e: Exception) {
                                callback?.onWriteFailed(e.message)
                            } finally {
                                pdf.close()
                            }
                        }

                        override fun onFinish() {
                            pdf.close()
                        }
                    },
                    PrintAttributes.Builder()
                        .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                        .setColorMode(PrintAttributes.COLOR_MODE_COLOR)
                        .build()
                )
                onSuccess("Dashboard PDF ready")
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                android.widget.Toast.makeText(context, "Dashboard PDF failed: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                onError(e.message ?: "Failed to generate dashboard")
            }
        }
    }
}