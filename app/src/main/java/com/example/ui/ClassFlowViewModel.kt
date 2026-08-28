package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.withTransaction
import com.example.data.database.AppDatabase
import com.example.data.datastore.PreferencesManager
import com.example.data.entity.ClassEntity
import com.example.data.entity.HomeworkEntity
import com.example.data.entity.TeacherProfileEntity
import com.example.data.entity.StudentEntity
import com.example.data.entity.TeacherNoteEntity
import com.example.data.repository.ClassScheduleRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import java.util.Date
import java.util.Calendar

@OptIn(FlowPreview::class)
class ClassFlowViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val repository = ClassScheduleRepository(
        database.classDao(),
        database.homeworkDao(),
        database.teacherDao(),
        database.studentDao(),
        database.teacherNoteDao(),
        database.holidayDao(),
        database.classLogDao(),
        database.examDao(),
        database.homeworkSubmissionDao(),
        database.managedClassDao(),
        database.studentActivityDao(),
        database.syllabusDao()
    )
    private val preferencesManager = PreferencesManager(application)
    
    // Managed Classes 
    val allManagedClasses = repository.allManagedClasses

    fun addManagedClass(name: String) {
        viewModelScope.launch {
             repository.insertManagedClass(com.example.data.entity.ManagedClassEntity(name = name))
        }
    }

    fun deleteManagedClass(managedClass: com.example.data.entity.ManagedClassEntity) {
        viewModelScope.launch {
            repository.deleteManagedClass(managedClass)
        }
    }

    fun importStudentsFromText(className: String, text: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteStudentsByClass(className)
            val lines = text.lines().filter { it.isNotBlank() }
            val studentsToInsert = lines.map { name ->
                StudentEntity(
                    name = name.trim(),
                    rollNumber = "",
                    className = className
                )
            }
            if (studentsToInsert.isNotEmpty()) {
                repository.studentDao.insertStudents(studentsToInsert)
            }
        }
    }

    // Calendar state: Selected Date
    private val _selectedDateMillis = MutableStateFlow(Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis)
    val selectedDateMillis = _selectedDateMillis.asStateFlow()

    private val _selectedDayOfWeek = MutableStateFlow(getDayOfWeekInt(Date(_selectedDateMillis.value)))
    val selectedDayOfWeek = _selectedDayOfWeek.asStateFlow()

    // Classes, Teachers, Homework & Holidays state flows
    val allClasses = repository.allClasses
    val allTeachers = repository.allTeachers
    val teacherProfile = allTeachers.map { it.firstOrNull() }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    
    val allStudents = repository.allStudents
    val allNotes = repository.allNotes
    val allHomework = repository.allHomework
    val allHolidays = repository.allHolidays

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val classLogsForSelectedDate: StateFlow<List<com.example.data.entity.ClassLogEntity>> = _selectedDateMillis
        .flatMapLatest { dateMillis: Long ->
            getClassLogsForDate(dateMillis)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch(Dispatchers.Default) {
            allClasses
                .debounce(1000L)
                .collect { classes ->
                    com.example.notification.ScheduleSync.sync(application, classes)
                }
        }
    }
    
    val onboardingFinished = preferencesManager.onboardingFinished.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )
    
    val isProfileComplete = teacherProfile.map { profile ->
        profile != null && profile.name.isNotBlank() && profile.dob.isNotBlank() && profile.schoolName.isNotBlank()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val schoolStartTime = preferencesManager.schoolStartTime
    val silencerEnabled = preferencesManager.silencerEnabled
    val firstPeriodTime = preferencesManager.firstPeriodTime
    val secondPeriodTime = preferencesManager.secondPeriodTime
    val nepaliDateOffset = preferencesManager.nepaliDateOffset.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0
    )

    val weeklyHolidays = preferencesManager.weeklyHolidays.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptySet()
    )

    fun toggleWeeklyHoliday(dayName: String) {
        viewModelScope.launch {
            val current = weeklyHolidays.value.toMutableSet()
            if (current.contains(dayName)) {
                current.remove(dayName)
            } else {
                current.add(dayName)
            }
            preferencesManager.saveWeeklyHolidays(current)
        }
    }

    // Selected day's classes
    val classesForSelectedDay: StateFlow<List<ClassEntity>> = combine(
        allClasses,
        _selectedDayOfWeek
    ) { classes, selectedDay ->
        classes.filter { it.dayOfWeek == selectedDay }
            .sortedBy { it.startTimeMinutes }
    }
    .flowOn(Dispatchers.Default)
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Simulated Clock Offset or Real System Clock representation (updates every minute/few secs)
    private val _currentTime = MutableStateFlow(SafeTime.now())
    val currentTime = _currentTime.asStateFlow()

    private val _currentDayOfWeek = MutableStateFlow(getDayOfWeekInt())
    val currentDayOfWeek = _currentDayOfWeek.asStateFlow()

    // Smart homework alerts logic
    val smartHomeworkReminders: StateFlow<List<HomeworkReminderState>> = combine(
        repository.allHomeworkWithClass,
        _currentTime,
        _currentDayOfWeek
    ) { homeworkWithClassList, _, _ ->
        val result = mutableListOf<HomeworkReminderState>()
        
        for (item in homeworkWithClassList) {
            if (item.homework.isCompleted) continue
            val classEntity = item.classEntity ?: continue
            
            // Calculate the NEXT sequential occurrence of this class since the homework was created
            val nextOccurMillis = repository.calculateNextOccurrenceMillis(item.homework, classEntity)
            
            val todayStart = getStartOfTodayMillis()
            val tomorrowEnd = todayStart + (2 * 24 * 60 * 60 * 1000L) // today and tomorrow range
            
            // Urgent helper check
            val isNextOrActive = nextOccurMillis in todayStart..tomorrowEnd
            
            result.add(
                HomeworkReminderState(
                    homework = item.homework,
                    classEntity = classEntity,
                    nextOccurrenceMillis = nextOccurMillis,
                    isUrgent = isNextOrActive
                )
            )
        }
        result.sortedBy { it.nextOccurrenceMillis }
    }
    .flowOn(Dispatchers.Default)
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        // Periodic ticker or trigger to refresh current time
        viewModelScope.launch {
            while (true) {
                _currentTime.value = SafeTime.now()
                _currentDayOfWeek.value = getDayOfWeekInt()
                kotlinx.coroutines.delay(15000) // update every 15 seconds
            }
        }
        
        // Seed dummy initial data if database is empty
        seedDataIfEmpty()
    }

    fun selectDate(dateMillis: Long) {
        _selectedDateMillis.value = dateMillis
        _selectedDayOfWeek.value = getDayOfWeekInt(java.util.Date(dateMillis))
    }

    // Class Operations
    fun saveClass(
        id: Int = 0,
        name: String,
        subject: String,
        roomNumber: String,
        teacherId: Int?,
        dayOfWeek: Int,
        startTime: String,
        endTime: String
    ) {
        viewModelScope.launch {
            val entity = ClassEntity(id, name, subject, roomNumber, teacherId, dayOfWeek, startTime, endTime)
            if (id == 0) {
                repository.insertClass(entity)
            } else {
                repository.updateClass(entity)
            }
        }
    }

    fun deleteClass(classEntity: ClassEntity) {
        viewModelScope.launch {
            repository.deleteClass(classEntity)
        }
    }

    fun prepopulateDayTemplate(day: Int) {
        viewModelScope.launch {
            val teacherId = repository.allTeachers.first().firstOrNull()?.id
            val templates = listOf(
                ClassEntity(name = "Class 4A", subject = "Computer", roomNumber = "Lab A", teacherId = teacherId, dayOfWeek = day, startTime = "08:30", endTime = "09:15"),
                ClassEntity(name = "Send Period Ledger", subject = "Admin", roomNumber = "Office", teacherId = teacherId, dayOfWeek = day, startTime = "09:15", endTime = "10:00"),
                ClassEntity(name = "6A Computer", subject = "Computer", roomNumber = "Lab B", teacherId = teacherId, dayOfWeek = day, startTime = "10:00", endTime = "10:45"),
                ClassEntity(name = "7B Computer", subject = "Computer", roomNumber = "Lab A", teacherId = teacherId, dayOfWeek = day, startTime = "10:45", endTime = "11:30"),
                ClassEntity(name = "Class 8", subject = "General", roomNumber = "Room 102", teacherId = teacherId, dayOfWeek = day, startTime = "11:30", endTime = "12:15"),
                ClassEntity(name = "BREAK", subject = "Break", roomNumber = "Canteen", teacherId = null, dayOfWeek = day, startTime = "12:15", endTime = "12:45")
            )
            for (t in templates) {
                repository.insertClass(t)
            }
        }
    }

    fun copyRoutineToAllDays(sourceDay: Int) {
        viewModelScope.launch {
            val classesForDay = repository.allClasses.first().filter { it.dayOfWeek == sourceDay }
            for (day in 1..7) {
                if (day == sourceDay) continue
                // Delete existing classes for target day
                repository.allClasses.first().filter { it.dayOfWeek == day }.forEach {
                    repository.deleteClass(it)
                }
                // Insert copied classes
                for (c in classesForDay) {
                    repository.insertClass(c.copy(id = 0, dayOfWeek = day))
                }
            }
        }
    }

    // Teacher Operations
    fun saveTeacher(
        id: Int = 0,
        name: String,
        email: String,
        officeLocation: String,
        subjectSpecialty: String,
        dob: String = "",
        schoolName: String = ""
    ) {
        viewModelScope.launch {
            val entity = TeacherProfileEntity(id, name, email, officeLocation, subjectSpecialty, dob, schoolName)
            if (id == 0) {
                repository.insertTeacher(entity)
            } else {
                val exists = repository.getTeacherById(id) != null
                if (exists) {
                    repository.updateTeacher(entity)
                } else {
                    repository.insertTeacher(entity.copy(id = 0))
                }
            }
        }
    }


    // Holiday Operations
    fun saveHoliday(title: String, startDate: String, endDate: String) {
        viewModelScope.launch {
            repository.insertHoliday(com.example.data.entity.HolidayEntity(title = title, startDate = startDate, endDate = endDate))
        }
    }

    fun deleteHoliday(holiday: com.example.data.entity.HolidayEntity) {
        viewModelScope.launch {
            repository.deleteHoliday(holiday)
        }
    }

    // Backup & Restore Engine
    fun exportBackup(onSuccess: (String) -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val json = org.json.JSONObject()

                // Classes
                val classesArray = org.json.JSONArray()
                repository.allClasses.first().forEach {
                    val obj = org.json.JSONObject().apply {
                        put("id", it.id)
                        put("name", it.name)
                        put("subject", it.subject)
                        put("roomNumber", it.roomNumber)
                        put("teacherId", it.teacherId ?: -1)
                        put("dayOfWeek", it.dayOfWeek)
                        put("startTime", it.startTime)
                        put("endTime", it.endTime)
                    }
                    classesArray.put(obj)
                }
                json.put("classes", classesArray)

                // Homework
                val homeworkArray = org.json.JSONArray()
                repository.allHomework.first().forEach {
                    val obj = org.json.JSONObject().apply {
                        put("id", it.id)
                        put("classId", it.classId)
                        put("title", it.title)
                        put("description", it.description)
                        put("checkingDateMillis", it.checkingDateMillis ?: 0L)
                        put("isCompleted", it.isCompleted)
                        put("createdDateMillis", it.createdDateMillis)
                    }
                    homeworkArray.put(obj)
                }
                json.put("homework", homeworkArray)

                // Teachers
                val teachersArray = org.json.JSONArray()
                repository.allTeachers.first().forEach {
                    val obj = org.json.JSONObject().apply {
                        put("id", it.id)
                        put("name", it.name)
                        put("email", it.email)
                        put("officeLocation", it.officeLocation)
                        put("subjectSpecialty", it.subjectSpecialty)
                        put("dob", it.dob)
                    }
                    teachersArray.put(obj)
                }
                json.put("teachers", teachersArray)

                // Students
                val studentsArray = org.json.JSONArray()
                repository.allStudents.first().forEach {
                    val obj = org.json.JSONObject().apply {
                        put("id", it.id)
                        put("name", it.name)
                        put("rollNumber", it.rollNumber)
                        put("className", it.className)
                        put("contactNumber", it.contactNumber)
                        put("guardianName", it.guardianName)
                        put("performanceNotes", it.performanceNotes)
                    }
                    studentsArray.put(obj)
                }
                json.put("students", studentsArray)

                // Notes
                val notesArray = org.json.JSONArray()
                repository.allNotes.first().forEach {
                    val obj = org.json.JSONObject().apply {
                        put("id", it.id)
                        put("title", it.title)
                        put("content", it.content)
                        put("tag", it.tag)
                        put("createdDateMillis", it.createdDateMillis)
                        put("eventEpochDay", it.eventEpochDay ?: org.json.JSONObject.NULL)
                    }
                    notesArray.put(obj)
                }
                json.put("notes", notesArray)

                // Holidays
                val holidaysArray = org.json.JSONArray()
                repository.allHolidays.first().forEach {
                    val obj = org.json.JSONObject().apply {
                        put("id", it.id)
                        put("title", it.title)
                        put("startDate", it.startDate)
                        put("endDate", it.endDate)
                    }
                    holidaysArray.put(obj)
                }
                json.put("holidays", holidaysArray)

                val jsonStr = json.toString(4)
                
                val docsDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOCUMENTS)
                val backupDir = java.io.File(docsDir, "MyClass_Backup")
                if (!backupDir.exists()) {
                    backupDir.mkdirs()
                }
                val file = java.io.File(backupDir, "myclass_backup_${System.currentTimeMillis()}.json")
                file.writeText(jsonStr)
                
                kotlinx.coroutines.withContext(Dispatchers.Main) {
                    onSuccess(file.absolutePath)
                }
            } catch (e: Exception) {
                kotlinx.coroutines.withContext(Dispatchers.Main) {
                    onError(e.message ?: "Unknown Error during Export")
                }
            }
        }
    }

    fun importBackup(jsonStr: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // ---------- Phase 1: shape validation & parse (no DB writes until the whole file proves well-formed) ----------
                val json = org.json.JSONObject(jsonStr)

                fun sectionObjects(name: String): List<org.json.JSONObject> {
                    if (!json.has(name) || json.isNull(name)) return emptyList()
                    val arr = json.getJSONArray(name)
                    return (0 until arr.length()).map { i ->
                        arr.optJSONObject(i)
                            ?: throw IllegalArgumentException("'$name' entry ${i + 1} is not an object")
                    }
                }

                fun reqStr(obj: org.json.JSONObject, key: String, section: String): String {
                    if (!obj.has(key) || obj.isNull(key))
                        throw IllegalArgumentException("'$section' entry is missing required field '$key'")
                    return obj.getString(key)
                }

                fun optStr(obj: org.json.JSONObject, key: String): String =
                    if (obj.has(key) && !obj.isNull(key)) obj.getString(key) else ""

                fun optInt(obj: org.json.JSONObject, key: String): Int =
                    if (obj.has(key) && !obj.isNull(key)) obj.getInt(key) else 0

                fun optLong(obj: org.json.JSONObject, key: String): Long =
                    if (obj.has(key) && !obj.isNull(key)) obj.getLong(key) else 0L

                fun optBool(obj: org.json.JSONObject, key: String): Boolean =
                    if (obj.has(key) && !obj.isNull(key)) obj.getBoolean(key) else false

                val teachers = sectionObjects("teachers").map { obj ->
                    TeacherProfileEntity(
                        id = optInt(obj, "id"),
                        name = reqStr(obj, "name", "teachers"),
                        email = optStr(obj, "email"),
                        officeLocation = optStr(obj, "officeLocation"),
                        subjectSpecialty = optStr(obj, "subjectSpecialty"),
                        dob = optStr(obj, "dob")
                    )
                }

                val classes = sectionObjects("classes").map { obj ->
                    ClassEntity(
                        id = optInt(obj, "id"),
                        name = reqStr(obj, "name", "classes"),
                        subject = optStr(obj, "subject"),
                        roomNumber = optStr(obj, "roomNumber"),
                        teacherId = if (obj.has("teacherId") && !obj.isNull("teacherId")) {
                            val tid = obj.getInt("teacherId")
                            if (tid == -1) null else tid
                        } else null,
                        dayOfWeek = if (obj.has("dayOfWeek") && !obj.isNull("dayOfWeek")) obj.getInt("dayOfWeek") else 1,
                        startTime = reqStr(obj, "startTime", "classes"),
                        endTime = reqStr(obj, "endTime", "classes")
                    )
                }

                val homework = sectionObjects("homework").map { obj ->
                    HomeworkEntity(
                        id = optInt(obj, "id"),
                        classId = optInt(obj, "classId"),
                        title = reqStr(obj, "title", "homework"),
                        description = optStr(obj, "description"),
                        checkingDateMillis = optLong(obj, "checkingDateMillis"),
                        isCompleted = optBool(obj, "isCompleted"),
                        createdDateMillis = optLong(obj, "createdDateMillis")
                    )
                }

                val students = sectionObjects("students").map { obj ->
                    StudentEntity(
                        id = optInt(obj, "id"),
                        name = reqStr(obj, "name", "students"),
                        rollNumber = optStr(obj, "rollNumber"),
                        className = optStr(obj, "className"),
                        contactNumber = optStr(obj, "contactNumber"),
                        guardianName = optStr(obj, "guardianName"),
                        performanceNotes = optStr(obj, "performanceNotes")
                    )
                }

                val notes = sectionObjects("notes").map { obj ->
                    TeacherNoteEntity(
                        id = optInt(obj, "id"),
                        title = reqStr(obj, "title", "notes"),
                        content = reqStr(obj, "content", "notes"),
                        tag = optStr(obj, "tag").ifBlank { "General" },
                        createdDateMillis = optLong(obj, "createdDateMillis"),
                        eventEpochDay = if (obj.has("eventEpochDay") && !obj.isNull("eventEpochDay")) {
                            obj.getLong("eventEpochDay")
                        } else null
                    )
                }

                val holidays = sectionObjects("holidays").map { obj ->
                    com.example.data.entity.HolidayEntity(
                        id = optInt(obj, "id"),
                        title = reqStr(obj, "title", "holidays"),
                        startDate = reqStr(obj, "startDate", "holidays"),
                        endDate = reqStr(obj, "endDate", "holidays")
                    )
                }

                if (teachers.isEmpty() && classes.isEmpty() && homework.isEmpty() &&
                    students.isEmpty() && notes.isEmpty() && holidays.isEmpty()
                ) {
                    throw IllegalArgumentException("Backup file contains no recognisable data sections")
                }

                // ---------- Phase 2: atomic replace inside a single Room transaction ----------
                database.withTransaction {
                    repository.teacherDao.deleteAllTeachers()
                    repository.classDao.deleteAllClasses()
                    repository.homeworkDao.deleteAllHomework()
                    repository.studentDao.deleteAllStudents()
                    repository.teacherNoteDao.deleteAllNotes()
                    repository.holidayDao.deleteAllHolidays()

                    teachers.forEach { repository.insertTeacher(it) }
                    classes.forEach { repository.insertClass(it) }
                    homework.forEach { repository.insertHomework(it) }
                    students.forEach { repository.insertStudent(it) }
                    notes.forEach { repository.insertNote(it) }
                    holidays.forEach { repository.insertHoliday(it) }
                }

                kotlinx.coroutines.withContext(Dispatchers.Main) {
                    onSuccess()
                }
            } catch (e: Exception) {
                kotlinx.coroutines.withContext(Dispatchers.Main) {
                    onError(e.message ?: "Failed to parse or restore the backup file")
                }
            }
        }
    }

    // Homework Operations
    fun getSubmissionsForStudent(studentId: Int) = repository.homeworkSubmissionDao.getAllSubmissions().map { submissions -> submissions.filter { it.studentId == studentId } }
    fun saveHomework(
        id: Int = 0,
        classId: Int,
        title: String,
        description: String,
        isCompleted: Boolean = false,
        notes: String? = null,
        imageUri: String? = null,
        checkingDateMillis: Long? = null,
        classworkNote: String? = null
    ) {
        viewModelScope.launch {
            val entity = HomeworkEntity(
                id = id,
                classId = classId,
                title = title,
                description = description,
                isCompleted = isCompleted,
                notes = notes,
                imageUri = imageUri,
                checkingDateMillis = checkingDateMillis,
                classworkNote = classworkNote
            )
            if (id == 0) {
                repository.insertHomework(entity)
            } else {
                repository.updateHomework(entity)
            }
        }
    }

    fun deleteHomework(homework: HomeworkEntity) {
        viewModelScope.launch {
            repository.deleteHomework(homework)
        }
    }


    // Student Operations
    fun saveStudent(
        id: Int = 0,
        name: String,
        rollNumber: String,
        className: String,
        contactNumber: String = "",
        guardianName: String = "",
        performanceNotes: String = ""
    ) {
        viewModelScope.launch {
            val entity = StudentEntity(
                id = id,
                name = name,
                rollNumber = rollNumber,
                className = className,
                contactNumber = contactNumber,
                guardianName = guardianName,
                performanceNotes = performanceNotes
            )
            if (id == 0) {
                repository.insertStudent(entity)
            } else {
                repository.updateStudent(entity)
            }
        }
    }

    fun deleteStudent(student: StudentEntity) {
        viewModelScope.launch {
            repository.deleteStudent(student)
        }
    }

    // Dated calendar events (stored on teacher notes with eventEpochDay)
    val allDatedEvents: StateFlow<List<TeacherNoteEntity>> = repository.allDatedEvents
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Teacher Note Operations
    fun saveNote(
        id: Int = 0,
        title: String,
        content: String,
        tag: String = "General",
        eventEpochDay: Long? = null
    ) {
        viewModelScope.launch {
            val entity = TeacherNoteEntity(
                id = id,
                title = title,
                content = content,
                tag = tag,
                eventEpochDay = eventEpochDay
            )
            if (id == 0) {
                repository.insertNote(entity)
            } else {
                repository.updateNote(entity)
            }
        }
    }

    /** Create or update a dated calendar event (stored as a tagged teacher note). */
    fun saveDatedEvent(
        id: Int = 0,
        title: String,
        content: String,
        eventEpochDay: Long
    ) {
        saveNote(
            id = id,
            title = title,
            content = content,
            tag = "Event",
            eventEpochDay = eventEpochDay
        )
    }

    fun deleteNote(note: TeacherNoteEntity) {
        viewModelScope.launch {
            repository.deleteNote(note)
        }
    }

    // Datastore Operations
    fun updateSchoolStartTime(time: String) {
        viewModelScope.launch {
            preferencesManager.saveSchoolStartTime(time)
        }
    }

    fun toggleSilencer(enabled: Boolean) {
        viewModelScope.launch {
            preferencesManager.saveSilencerEnabled(enabled)
        }
    }

    fun updateFirstPeriodTime(time: String) {
        viewModelScope.launch {
            preferencesManager.saveFirstPeriodTime(time)
        }
    }

    fun updateSecondPeriodTime(time: String) {
        viewModelScope.launch {
            preferencesManager.saveSecondPeriodTime(time)
        }
    }


    fun completeOnboarding() {
        viewModelScope.launch {
            preferencesManager.saveOnboardingFinished(true)
        }
    }

    fun logClassStatus(classId: Int, dateMillis: Long, status: String) {
        viewModelScope.launch {
            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
            val ds = sdf.format(java.util.Date(dateMillis))
            repository.classLogDao.insertLog(com.example.data.entity.ClassLogEntity(classId = classId, dateString = ds, status = status))
        }
    }

    fun getStudentsForClass(className: String) = repository.getStudentsByClass(className)

    // Student Activity Operations
    val allActivities = repository.allActivities
    fun getActivitiesForStudent(studentId: Int) = repository.getActivitiesForStudent(studentId)

    fun recordStudentActivity(studentId: Int, studentName: String, className: String, activityType: String, note: String = "") {
        viewModelScope.launch {
            repository.insertActivity(
                com.example.data.entity.StudentActivityEntity(
                    studentId = studentId,
                    studentName = studentName,
                    className = className,
                    activityType = activityType,
                    note = note
                )
            )
        }
    }

    // Exam Operations
    val allExams = repository.allExams
    fun getAllMarksForExam(examId: Int) = repository.getAllMarksForExam(examId)

    fun saveExam(id: Int = 0, name: String, dateString: String, targetClassNames: String? = null, fullMarks: String = "100", passMarks: String = "40", subject: String = "") {
        viewModelScope.launch {
            repository.insertExam(com.example.data.entity.ExamEntity(id = id, name = name, dateString = dateString, targetClassNames = targetClassNames, fullMarks = fullMarks, passMarks = passMarks, subject = subject))
        }
    }

    fun deleteExam(examId: Int) {
        viewModelScope.launch {
            repository.deleteExam(examId)
        }
    }

    // Syllabus AI logic
    fun saveSyllabusText(subject: String, className: String, rawText: String, onStatus: (String) -> Unit) {
        viewModelScope.launch {
            try {
                onStatus("Saving syllabus...")
                repository.insertSyllabus(
                    com.example.data.entity.SyllabusEntity(
                        subject = subject,
                        className = className,
                        rawText = rawText
                    )
                )
                onStatus("Success")
            } catch (e: Exception) {
                e.printStackTrace()
                onStatus("Error: ${e.message}")
            }
        }
    }
    
    // UI Helpers for Syllabus Progress
    val allSyllabuses = repository.allSyllabuses
    
    fun addSyllabus(subject: String, className: String, rawText: String) {
        saveSyllabusText(subject, className, rawText) { }
    }

    fun deleteSyllabus(id: Int) {
        viewModelScope.launch {
            repository.deleteSyllabus(id)
        }
    }

    fun saveExamMark(examId: Int, classId: Int, studentId: Int, marks: String, remarks: String) {
        viewModelScope.launch {
            // Delete existing mark if any to avoid duplication
            repository.insertMark(
                com.example.data.entity.ExamMarkEntity(
                    examId = examId,
                    classId = classId,
                    studentId = studentId,
                    marksObtained = marks,
                    remarks = remarks
                )
            )
        }
    }

    suspend fun getExamStats(examId: Int, passMark: Int): ExamStats {
        val marks = repository.getAllMarksForExam(examId).first()
        if (marks.isEmpty()) return ExamStats()
        
        val numericMarks = marks.mapNotNull { it.marksObtained.toDoubleOrNull() }
        if (numericMarks.isEmpty()) return ExamStats()

        val passed = numericMarks.count { it >= passMark }
        val failed = numericMarks.size - passed
        val highest = numericMarks.maxOrNull() ?: 0.0
        val lowest = numericMarks.minOrNull() ?: 0.0
        val average = numericMarks.average()

        // Find students with highest marks
        val topMarks = marks.filter { it.marksObtained == highest.toString() || it.marksObtained.toDoubleOrNull() == highest }
        val topStudentName = if (topMarks.isNotEmpty()) {
            val firstStudentName = repository.studentDao.getStudentById(topMarks.first().studentId)?.name ?: "Unknown"
            if (topMarks.size > 1) {
                "$firstStudentName & +${topMarks.size - 1}"
            } else {
                firstStudentName
            }
        } else null

        // Find students with lowest marks
        val lowestMarks = marks.filter { it.marksObtained == lowest.toString() || it.marksObtained.toDoubleOrNull() == lowest }
        val lowestStudentName = if (lowestMarks.isNotEmpty()) {
            val firstStudentName = repository.studentDao.getStudentById(lowestMarks.first().studentId)?.name ?: "Unknown"
            if (lowestMarks.size > 1) {
                "$firstStudentName & +${lowestMarks.size - 1}"
            } else {
                firstStudentName
            }
        } else null

        return ExamStats(
            totalStudents = marks.size,
            passed = passed,
            failed = failed,
            highestMark = highest,
            highestMarkStudentName = topStudentName,
            lowestMark = lowest,
            lowestMarkStudentName = lowestStudentName,
            averageMark = average
        )
    }

    // Reports Generation
    fun exportStudentReport(context: android.content.Context, classFilter: Set<String>? = null, onSuccess: (String) -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val csv = StringBuilder()
                csv.append("Name,Class,Roll,HW Done,HW Not Done,HW Half Done,Named by Monitor,Impressed by Engaging,Positive Curiosity,SentOut,Disturbed,Scoldings,Prize,Sick\n")
                
                val students = repository.allStudents.first().filter { classFilter == null || classFilter.isEmpty() || classFilter.contains(it.className) }
                val submissions = repository.homeworkSubmissionDao.getAllSubmissions().first()
                val allActivities = repository.studentActivityDao.getAllActivities().first()
                
                for (s in students) {
                    val studentSubmissions = submissions.filter { it.studentId == s.id }
                    val done = studentSubmissions.count { it.status.lowercase() == "done" }
                    val notDone = studentSubmissions.count { it.status.lowercase() == "not done" }
                    val halfDone = studentSubmissions.count { it.status.lowercase() == "half done" }
                    
                    val activities = allActivities.filter { it.studentId == s.id }
                    val noise = activities.count { it.activityType == "Named by Monitor" || it.activityType == "Making Noise" || it.activityType == "Name By Monitor" }
                    val impressed = activities.count { it.activityType == "Impressed by Engaging" }
                    val curiosity = activities.count { it.activityType == "Positive Curiosity" }
                    val sentOut = activities.count { it.activityType == "Sent Outside" }
                    val disturbed = activities.count { it.activityType == "Disturbed Class" }
                    val scoldings = activities.count { it.activityType == "Scoldings" }
                    val prize = activities.count { it.activityType == "Got Prize" }
                    val sick = activities.count { it.activityType == "Was Sick" }
                    
                    csv.append("${s.name},${s.className},${s.rollNumber},$done,$notDone,$halfDone,$noise,$impressed,$curiosity,$sentOut,$disturbed,$scoldings,$prize,$sick\n")
                }

                val docsDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOCUMENTS)
                val fileName = if (classFilter != null) "Student_Report_${classFilter}_${System.currentTimeMillis()}.csv" else "Student_Report_All_${System.currentTimeMillis()}.csv"
                val reportFile = java.io.File(docsDir, fileName)
                reportFile.writeText(csv.toString())

                kotlinx.coroutines.withContext(Dispatchers.Main) {
                    onSuccess(reportFile.absolutePath)
                }
            } catch (e: Exception) {
                kotlinx.coroutines.withContext(Dispatchers.Main) {
                    onError(e.message ?: "Failed to generate report")
                }
            }
        }
    }

    fun generateIndividualPdfReport(context: android.content.Context, student: StudentEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val submissions = repository.homeworkSubmissionDao.getAllSubmissions().first().filter { it.studentId == student.id }
                val activities = repository.studentActivityDao.getActivitiesForStudent(student.id).first()
                val exams = repository.allExams.first()
                val allMarks = repository.getMarksForStudent(student.id).first()

                val pdfDocument = android.graphics.pdf.PdfDocument()
                val pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 size
                val page = pdfDocument.startPage(pageInfo)
                val canvas = page.canvas
                val textPaint = android.graphics.Paint().apply { color = android.graphics.Color.BLACK }
                val linePaint = android.graphics.Paint().apply { color = android.graphics.Color.DKGRAY; strokeWidth = 1f }
                val headerBgPaint = android.graphics.Paint()
                
                var currentY = 60f
                val marginLeft = 50f
                
                // Header
                textPaint.textSize = 28f
                textPaint.isFakeBoldText = true
                textPaint.textAlign = android.graphics.Paint.Align.CENTER
                canvas.drawText("STUDENT PERFORMANCE REPORT", 297f, currentY, textPaint)
                currentY += 40f
                
                textPaint.textSize = 18f
                textPaint.textAlign = android.graphics.Paint.Align.LEFT
                canvas.drawText("Class: ${student.className}", marginLeft, currentY, textPaint)
                canvas.drawText("Student: ${student.name} (Roll: ${student.rollNumber})", 300f, currentY, textPaint)
                currentY += 20f
                
                canvas.drawLine(marginLeft, currentY, 545f, currentY, linePaint)
                currentY += 40f
                
                // 1. Homework Performance
                textPaint.textSize = 16f
                textPaint.isFakeBoldText = true
                
                headerBgPaint.color = android.graphics.Color.parseColor("#E3F2FD")
                canvas.drawRect(marginLeft - 5f, currentY - 22f, 545f, currentY + 8f, headerBgPaint)
                
                textPaint.color = android.graphics.Color.parseColor("#0D47A1")
                canvas.drawText("Homework & Assignments", marginLeft + 5f, currentY, textPaint)
                currentY += 35f
                
                textPaint.textSize = 14f
                textPaint.isFakeBoldText = false
                textPaint.color = android.graphics.Color.BLACK
                val done = submissions.count { it.status.lowercase() == "done" }
                val half = submissions.count { it.status.lowercase() == "half done" }
                val notDone = submissions.count { it.status.lowercase() == "not done" }
                val totalHw = submissions.size
                
                canvas.drawText("Total Tasks: $totalHw", marginLeft + 20f, currentY, textPaint)
                currentY += 25f
                canvas.drawText("• Done: $done", marginLeft + 30f, currentY, textPaint)
                canvas.drawText("• Half Done: $half", marginLeft + 150f, currentY, textPaint)
                canvas.drawText("• Not Done: $notDone", marginLeft + 290f, currentY, textPaint)
                currentY += 40f

                // 2. Class Activities & Behavior
                textPaint.textSize = 16f
                textPaint.isFakeBoldText = true
                
                headerBgPaint.color = android.graphics.Color.parseColor("#E8F5E9")
                canvas.drawRect(marginLeft - 5f, currentY - 22f, 545f, currentY + 8f, headerBgPaint)
                
                textPaint.color = android.graphics.Color.parseColor("#1B5E20")
                canvas.drawText("Class Activities & Behavior", marginLeft + 5f, currentY, textPaint)
                currentY += 35f
                
                textPaint.textSize = 14f
                textPaint.isFakeBoldText = false
                textPaint.color = android.graphics.Color.BLACK
                if (activities.filter { it.activityType != "Disciplined" }.isEmpty()) {
                    canvas.drawText("No specific activities recorded.", marginLeft + 20f, currentY, textPaint)
                    currentY += 25f
                } else {
                    val activitySummary = activities.filter { it.activityType != "Disciplined" }.groupBy { it.activityType }.map { "${it.key}: ${it.value.size}" }
                    activitySummary.forEach {
                        canvas.drawText("• $it", marginLeft + 30f, currentY, textPaint)
                        currentY += 25f
                    }
                }
                currentY += 15f

                // 3. Exam Performances
                textPaint.textSize = 16f
                textPaint.isFakeBoldText = true
                
                headerBgPaint.color = android.graphics.Color.parseColor("#FFF3E0")
                canvas.drawRect(marginLeft - 5f, currentY - 22f, 545f, currentY + 8f, headerBgPaint)
                
                textPaint.color = android.graphics.Color.parseColor("#E65100")
                canvas.drawText("Exam Performances", marginLeft + 5f, currentY, textPaint)
                currentY += 35f

                if (allMarks.isEmpty()) {
                    textPaint.textSize = 14f
                    textPaint.isFakeBoldText = false
                    textPaint.color = android.graphics.Color.BLACK
                    canvas.drawText("No exam marks recorded yet.", marginLeft + 20f, currentY, textPaint)
                    currentY += 25f
                } else {
                    textPaint.textSize = 11f
                    textPaint.color = android.graphics.Color.DKGRAY
                    headerBgPaint.color = android.graphics.Color.parseColor("#F5F5F5")
                    canvas.drawRect(marginLeft, currentY - 15f, 545f, currentY + 5f, headerBgPaint)
                    textPaint.isFakeBoldText = true
                    canvas.drawText("Exam (Subject)", marginLeft + 5f, currentY, textPaint)
                    canvas.drawText("Full", 250f, currentY, textPaint)
                    canvas.drawText("Pass", 300f, currentY, textPaint)
                    canvas.drawText("Obtained", 350f, currentY, textPaint)
                    canvas.drawText("Remark", 420f, currentY, textPaint)
                    currentY += 25f
                    textPaint.isFakeBoldText = false
                    textPaint.textSize = 10f
                    textPaint.color = android.graphics.Color.BLACK
                    
                    allMarks.forEach { mark ->
                        val exam = exams.find { it.id == mark.examId }
                        if (exam != null) {
                            val examSubject = if (exam.subject.isNotEmpty()) " (${exam.subject})" else ""
                            canvas.drawText("${exam.name}$examSubject", marginLeft + 5f, currentY, textPaint)
                            canvas.drawText(exam.fullMarks, 250f, currentY, textPaint)
                            canvas.drawText(exam.passMarks, 300f, currentY, textPaint)
                            canvas.drawText(mark.marksObtained, 350f, currentY, textPaint)
                            val remarkDetails = if (mark.remarks.isNotBlank()) mark.remarks else "-"
                            canvas.drawText(remarkDetails, 420f, currentY, textPaint)
                            currentY += 20f
                        }
                    }
                }

                val teachers = repository.teacherDao.getAllTeachers().first()
                val teacherName = teachers.firstOrNull()?.name ?: "Teacher"
                textPaint.textSize = 12f
                textPaint.color = android.graphics.Color.BLACK
                textPaint.isFakeBoldText = true
                canvas.drawText("Teacher: $teacherName", 400f, currentY + 40f, textPaint)
                
                pdfDocument.finishPage(page)
                
                val docsDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOCUMENTS)
                val reportFile = java.io.File(docsDir, "Student_Report_${student.name.replace(" ", "_")}_${System.currentTimeMillis()}.pdf")
                pdfDocument.writeTo(java.io.FileOutputStream(reportFile))
                pdfDocument.close()

                kotlinx.coroutines.withContext(Dispatchers.Main) {
                    android.widget.Toast.makeText(context, "PDF Report Printed: ${reportFile.name}", android.widget.Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                kotlinx.coroutines.withContext(Dispatchers.Main) {
                    android.widget.Toast.makeText(context, "PDF generation failed: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun generateClassPdfReport(context: android.content.Context, className: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val students = repository.allStudents.first().filter { it.className == className }
                val submissions = repository.homeworkSubmissionDao.getAllSubmissions().first()
                val allActivities = repository.studentActivityDao.getAllActivities().first()
                val allExams = repository.allExams.first()
                val allMarks = repository.examDao.getAllMarks().first()

                val pdfDocument = android.graphics.pdf.PdfDocument()
                var pageNumber = 1
                
                for (student in students) {
                    val pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(595, 842, pageNumber++).create()
                    val page = pdfDocument.startPage(pageInfo)
                    val canvas = page.canvas
                    val textPaint = android.graphics.Paint().apply { color = android.graphics.Color.BLACK }
                    val linePaint = android.graphics.Paint().apply { color = android.graphics.Color.DKGRAY; strokeWidth = 1f }
                    
                    var currentY = 60f
                    val marginLeft = 50f
                    val headerBgPaint = android.graphics.Paint().apply { color = android.graphics.Color.BLACK }
                    
                    // Header
                    textPaint.textSize = 28f
                    textPaint.isFakeBoldText = true
                    textPaint.textAlign = android.graphics.Paint.Align.CENTER
                    canvas.drawText("STUDENT PERFORMANCE REPORT", 297f, currentY, textPaint)
                    currentY += 40f

                    textPaint.textSize = 18f
                    textPaint.textAlign = android.graphics.Paint.Align.LEFT
                    canvas.drawText("Class: $className", marginLeft, currentY, textPaint)
                    canvas.drawText("Student: ${student.name} (Roll: ${student.rollNumber})", 300f, currentY, textPaint)
                    currentY += 20f
                    
                    canvas.drawLine(marginLeft, currentY, 545f, currentY, linePaint)
                    currentY += 40f
                    
                    // 1. Homework Performance
                    textPaint.textSize = 16f
                    textPaint.isFakeBoldText = true
                    
                    // Draw professional blue background highlight
                    headerBgPaint.color = android.graphics.Color.parseColor("#E3F2FD")
                    canvas.drawRect(marginLeft - 5f, currentY - 22f, 545f, currentY + 8f, headerBgPaint)
                    
                    textPaint.color = android.graphics.Color.parseColor("#0D47A1")
                    canvas.drawText("Homework & Assignments", marginLeft + 5f, currentY, textPaint)
                    currentY += 35f
                    
                    textPaint.textSize = 14f
                    textPaint.isFakeBoldText = false
                    textPaint.color = android.graphics.Color.BLACK
                    val sSubmissions = submissions.filter { it.studentId == student.id }
                    val done = sSubmissions.count { it.status.lowercase() == "done" }
                    val half = sSubmissions.count { it.status.lowercase() == "half done" }
                    val notDone = sSubmissions.count { it.status.lowercase() == "not done" }
                    val totalHw = sSubmissions.size
                    
                    canvas.drawText("Total Tasks: $totalHw", marginLeft + 20f, currentY, textPaint)
                    currentY += 25f
                    canvas.drawText("• Done: $done", marginLeft + 30f, currentY, textPaint)
                    canvas.drawText("• Half Done: $half", marginLeft + 150f, currentY, textPaint)
                    canvas.drawText("• Not Done: $notDone", marginLeft + 290f, currentY, textPaint)
                    currentY += 40f

                    // 2. Class Activities & Behavior
                    textPaint.textSize = 16f
                    textPaint.isFakeBoldText = true
                    
                    headerBgPaint.color = android.graphics.Color.parseColor("#E8F5E9")
                    canvas.drawRect(marginLeft - 5f, currentY - 22f, 545f, currentY + 8f, headerBgPaint)
                    
                    textPaint.color = android.graphics.Color.parseColor("#1B5E20")
                    canvas.drawText("Class Activities & Behavior", marginLeft + 5f, currentY, textPaint)
                    currentY += 35f
                    
                    textPaint.textSize = 14f
                    textPaint.isFakeBoldText = false
                    textPaint.color = android.graphics.Color.BLACK
                    val sActivities = allActivities.filter { it.studentId == student.id }
                    if (sActivities.filter { it.activityType != "Disciplined" }.isEmpty()) {
                        canvas.drawText("No specific activities recorded.", marginLeft + 20f, currentY, textPaint)
                        currentY += 25f
                    } else {
                        val activitySummary = sActivities.filter { it.activityType != "Disciplined" }.groupBy { it.activityType }.map { "${it.key}: ${it.value.size}" }
                        activitySummary.forEach {
                            canvas.drawText("• $it", marginLeft + 30f, currentY, textPaint)
                            currentY += 25f
                        }
                    }
                    currentY += 15f
                    
                    // 3. Exam Performances
                    textPaint.textSize = 16f
                    textPaint.isFakeBoldText = true
                    
                    headerBgPaint.color = android.graphics.Color.parseColor("#FFF3E0")
                    canvas.drawRect(marginLeft - 5f, currentY - 22f, 545f, currentY + 8f, headerBgPaint)
                    
                    textPaint.color = android.graphics.Color.parseColor("#E65100")
                    canvas.drawText("Exam Performances", marginLeft + 5f, currentY, textPaint)
                    currentY += 35f

                    val sMarks = allMarks.filter { it.studentId == student.id }
                    if (sMarks.isEmpty()) {
                        textPaint.textSize = 14f
                        textPaint.isFakeBoldText = false
                        textPaint.color = android.graphics.Color.BLACK
                        canvas.drawText("No exam marks recorded yet.", marginLeft + 20f, currentY, textPaint)
                    } else {
                        textPaint.textSize = 11f
                        textPaint.color = android.graphics.Color.DKGRAY
                        headerBgPaint.color = android.graphics.Color.parseColor("#F5F5F5")
                        canvas.drawRect(marginLeft, currentY - 15f, 545f, currentY + 5f, headerBgPaint)
                        textPaint.isFakeBoldText = true
                        canvas.drawText("Exam (Subject)", marginLeft + 5f, currentY, textPaint)
                        canvas.drawText("Full", 250f, currentY, textPaint)
                        canvas.drawText("Pass", 300f, currentY, textPaint)
                        canvas.drawText("Obtained", 350f, currentY, textPaint)
                        canvas.drawText("Remark", 420f, currentY, textPaint)
                        currentY += 25f
                        textPaint.isFakeBoldText = false
                        textPaint.textSize = 10f
                        textPaint.color = android.graphics.Color.BLACK
                        
                        sMarks.forEach { mark ->
                            val exam = allExams.find { it.id == mark.examId }
                            if (exam != null) {
                                val examSubject = if (exam.subject.isNotEmpty()) " (${exam.subject})" else ""
                                canvas.drawText("${exam.name}$examSubject", marginLeft + 5f, currentY, textPaint)
                                canvas.drawText(exam.fullMarks, 250f, currentY, textPaint)
                                canvas.drawText(exam.passMarks, 300f, currentY, textPaint)
                                canvas.drawText(mark.marksObtained, 350f, currentY, textPaint)
                                val remarkDetails = if (mark.remarks.isNotBlank()) mark.remarks else "-"
                                canvas.drawText(remarkDetails, 420f, currentY, textPaint)
                                currentY += 20f
                            }
                        }
                    }

                    val teachers = repository.teacherDao.getAllTeachers().first()
                    val teacherName = teachers.firstOrNull()?.name ?: "Teacher"
                    textPaint.textSize = 12f
                    textPaint.color = android.graphics.Color.BLACK
                    textPaint.isFakeBoldText = true
                    canvas.drawText("Teacher: $teacherName", 400f, currentY + 40f, textPaint)

                    pdfDocument.finishPage(page)
                }

                val docsDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOCUMENTS)
                val reportFile = java.io.File(docsDir, "Class_Report_${className.replace(" ", "_")}_${System.currentTimeMillis()}.pdf")
                pdfDocument.writeTo(java.io.FileOutputStream(reportFile))
                pdfDocument.close()

                kotlinx.coroutines.withContext(Dispatchers.Main) {
                    android.widget.Toast.makeText(context, "Class PDF Printed: ${reportFile.name}", android.widget.Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                kotlinx.coroutines.withContext(Dispatchers.Main) {
                    android.widget.Toast.makeText(context, "Class PDF failed: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun generateExamPdfReport(context: android.content.Context, exam: com.example.data.entity.ExamEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val marks = repository.examDao.getAllMarks().first().filter { it.examId == exam.id }
                val students = repository.allStudents.first()

                val passInt = exam.passMarks.toIntOrNull() ?: 40
                var totalPass = 0
                var totalFail = 0
                var sumMarks = 0.0
                var validMarksCount = 0

                marks.forEach { mark ->
                    val markDouble = mark.marksObtained.toDoubleOrNull()
                    if (mark.marksObtained.lowercase() == "absent") {
                        totalFail++
                    } else if (markDouble != null) {
                        validMarksCount++
                        sumMarks += markDouble
                        if (markDouble >= passInt) {
                            totalPass++
                        } else {
                            totalFail++
                        }
                    } else if (mark.marksObtained.lowercase() == "fail") {
                        totalFail++
                    } else if (mark.marksObtained.lowercase() == "pass") {
                        totalPass++
                    }
                }
                
                val avgMark = if (validMarksCount > 0) sumMarks / validMarksCount else 0.0
                val decFormat = java.text.DecimalFormat("#.##")

                val pdfDocument = android.graphics.pdf.PdfDocument()
                val pageWidth = 595
                val pageHeight = 842
                var pageNum = 1
                var pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create()
                var page = pdfDocument.startPage(pageInfo)
                var canvas = page.canvas
                val textPaint = android.graphics.Paint().apply { color = android.graphics.Color.BLACK }
                val borderPaint = android.graphics.Paint().apply { color = android.graphics.Color.LTGRAY; strokeWidth = 1f; style = android.graphics.Paint.Style.STROKE }

                val marginLeft = 50f
                val marginRight = 545f
                var currentY = 0f

                fun drawHeader() {
                    val bgPaint = android.graphics.Paint().apply { color = android.graphics.Color.parseColor("#E8EAF6"); style = android.graphics.Paint.Style.FILL }
                    canvas.drawRect(0f, 0f, pageWidth.toFloat(), 180f, bgPaint)
                    
                    currentY = 45f
                    textPaint.textSize = 28f
                    textPaint.isFakeBoldText = true
                    textPaint.color = android.graphics.Color.parseColor("#3F51B5")
                    textPaint.textAlign = android.graphics.Paint.Align.CENTER
                    canvas.drawText("EXAM REPORT", pageWidth / 2f, currentY, textPaint)
                    
                    currentY = 85f
                    textPaint.textSize = 14f
                    textPaint.color = android.graphics.Color.DKGRAY
                    textPaint.textAlign = android.graphics.Paint.Align.LEFT
                    canvas.drawText("Exam: ${exam.name} (${exam.subject})", marginLeft, currentY, textPaint)
                    canvas.drawText("Date: ${exam.dateString}", 400f, currentY, textPaint)
                    currentY += 25f

                    textPaint.isFakeBoldText = false
                    canvas.drawText("Full Marks: ${exam.fullMarks}", marginLeft, currentY, textPaint)
                    canvas.drawText("Pass Marks: ${exam.passMarks}", 200f, currentY, textPaint)
                    currentY += 30f
                    
                    textPaint.isFakeBoldText = true
                    textPaint.color = android.graphics.Color.parseColor("#00695C")
                    canvas.drawText("Total Passes: $totalPass", marginLeft, currentY, textPaint)
                    textPaint.color = android.graphics.Color.parseColor("#D32F2F")
                    canvas.drawText("Total Failed: $totalFail", 200f, currentY, textPaint)
                    textPaint.color = android.graphics.Color.parseColor("#E65100")
                    canvas.drawText("Class Average: ${decFormat.format(avgMark)}", 350f, currentY, textPaint)
                    
                    currentY = 205f
                }

                fun drawTableHeader() {
                    val rowHeight = 30f
                    val tableHeaderBg = android.graphics.Paint().apply { color = android.graphics.Color.parseColor("#3F51B5"); style = android.graphics.Paint.Style.FILL }
                    canvas.drawRect(marginLeft, currentY - 20f, marginRight, currentY + 10f, tableHeaderBg)
                    
                    textPaint.color = android.graphics.Color.WHITE
                    textPaint.isFakeBoldText = true
                    textPaint.textSize = 12f
                    canvas.drawText("SN", marginLeft + 5f, currentY, textPaint)
                    canvas.drawText("Student Name", marginLeft + 40f, currentY, textPaint)
                    canvas.drawText("Mark", marginLeft + 230f, currentY, textPaint)
                    canvas.drawText("Result", marginLeft + 300f, currentY, textPaint)
                    canvas.drawText("Remarks", marginLeft + 380f, currentY, textPaint)
                    currentY += rowHeight
                }

                drawHeader()
                drawTableHeader()

                var sn = 1
                marks.forEach { mark ->
                    val student = students.find { it.id == mark.studentId }
                    if (student != null) {
                        if (currentY > 780f) {
                            pdfDocument.finishPage(page)
                            pageNum++
                            pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create()
                            page = pdfDocument.startPage(pageInfo)
                            canvas = page.canvas
                            
                            currentY = 60f
                            drawTableHeader()
                        }

                        val rowHeight = 30f
                        canvas.drawRect(marginLeft, currentY - 20f, marginRight, currentY + 10f, borderPaint)
                        
                        textPaint.color = android.graphics.Color.BLACK
                        textPaint.isFakeBoldText = false
                        textPaint.textSize = 12f
                        canvas.drawText(sn.toString(), marginLeft + 5f, currentY, textPaint)
                        canvas.drawText(student.name, marginLeft + 40f, currentY, textPaint)

                        val markDouble = mark.marksObtained.toDoubleOrNull()
                        val resultStr = if (mark.marksObtained.lowercase() == "absent") {
                            "Absent"
                        } else if (markDouble != null) {
                            if (markDouble >= passInt) "Pass" else "Fail"
                        } else "N/A"

                        canvas.drawText(mark.marksObtained, marginLeft + 230f, currentY, textPaint)
                        
                        if (resultStr == "Pass") {
                            textPaint.color = android.graphics.Color.parseColor("#00695C")
                            textPaint.isFakeBoldText = true
                        } else if (resultStr == "Fail" || resultStr == "Absent") {
                            textPaint.color = android.graphics.Color.parseColor("#D32F2F")
                            textPaint.isFakeBoldText = true
                        }
                        canvas.drawText(resultStr, marginLeft + 300f, currentY, textPaint)
                        textPaint.color = android.graphics.Color.BLACK
                        textPaint.isFakeBoldText = false
                        
                        var safeRemarks = mark.remarks
                        if (safeRemarks.length > 25) safeRemarks = safeRemarks.take(22) + "..."
                        canvas.drawText(safeRemarks, marginLeft + 380f, currentY, textPaint)

                        currentY += rowHeight
                        sn++
                    }
                }

                pdfDocument.finishPage(page)

                // Hand the finished document to the Android print framework so the
                // user can save / share it as a real PDF through the system print
                // dialog (no external-storage permission needed).
                kotlinx.coroutines.withContext(Dispatchers.Main) {
                    val printManager = context.getSystemService(android.content.Context.PRINT_SERVICE) as? android.print.PrintManager
                    val jobName = "Exam_Report_${exam.name.replace(" ", "_")}"
                    printManager?.print(
                        jobName,
                        object : android.print.PrintDocumentAdapter() {
                            override fun onLayout(
                                oldAttributes: android.print.PrintAttributes?,
                                newAttributes: android.print.PrintAttributes?,
                                cancellationSignal: android.os.CancellationSignal?,
                                callback: android.print.PrintDocumentAdapter.LayoutResultCallback?,
                                extras: android.os.Bundle?
                            ) {
                                if (cancellationSignal?.isCanceled == true) {
                                    callback?.onLayoutCancelled()
                                    return
                                }
                                val info = android.print.PrintDocumentInfo.Builder(jobName)
                                    .setContentType(android.print.PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                                    .build()
                                callback?.onLayoutFinished(info, true)
                            }

                            override fun onWrite(
                                pages: Array<out android.print.PageRange>?,
                                destination: android.os.ParcelFileDescriptor?,
                                cancellationSignal: android.os.CancellationSignal?,
                                callback: android.print.PrintDocumentAdapter.WriteResultCallback?
                            ) {
                                try {
                                    pdfDocument.writeTo(java.io.FileOutputStream(destination?.fileDescriptor))
                                    callback?.onWriteFinished(arrayOf(android.print.PageRange.ALL_PAGES))
                                } catch (e: Exception) {
                                    callback?.onWriteFailed(e.message)
                                } finally {
                                    pdfDocument.close()
                                }
                            }

                            override fun onFinish() {
                                pdfDocument.close()
                            }
                        },
                        android.print.PrintAttributes.Builder()
                            .setMediaSize(android.print.PrintAttributes.MediaSize.ISO_A4)
                            .setColorMode(android.print.PrintAttributes.COLOR_MODE_COLOR)
                            .build()
                    )
                }
            } catch (e: Exception) {
                kotlinx.coroutines.withContext(Dispatchers.Main) {
                    android.widget.Toast.makeText(context, "Exam PDF failed: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun resetBehaviorForStudent(studentId: Int) {
        viewModelScope.launch {
            repository.studentActivityDao.deleteAllActivitiesForStudent(studentId)
            repository.homeworkSubmissionDao.deleteAllSubmissionsForStudent(studentId)
        }
    }

    // The "Bomb Everything" Button
    fun bombEverything() {
        viewModelScope.launch {
            try {
                database.clearAllTables()
                preferencesManager.clearAllPreferences()
                // App will effectively reset to fresh state
            } catch (e: Exception) {
                // handle error
            }
        }
    }

    fun getSubmissionsForHomework(homeworkId: Int) = repository.homeworkSubmissionDao.getSubmissionsForHomework(homeworkId)

    suspend fun getPastNotDoneCount(studentId: Int) = repository.homeworkSubmissionDao.getNotDoneSubmissionsForStudent(studentId).size

    fun saveSubmission(homeworkId: Int, studentId: Int, status: String) {
        viewModelScope.launch {
            repository.homeworkSubmissionDao.insertSubmission(
                com.example.data.entity.HomeworkSubmissionEntity(
                    homeworkId = homeworkId,
                    studentId = studentId,
                    status = status
                )
            )
        }
    }

    fun getClassLogsForDate(dateMillis: Long): kotlinx.coroutines.flow.Flow<List<com.example.data.entity.ClassLogEntity>> {
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
        val ds = sdf.format(java.util.Date(dateMillis))
        return repository.classLogDao.getLogsForDate(ds)
    }

    fun adjustNepaliDateOffset(offset: Int) {
        viewModelScope.launch {
            preferencesManager.saveNepaliDateOffset(offset)
        }
    }

    // Seed helpers
    private fun seedDataIfEmpty() {
        // Disabled seeding for onboarding
    }
}

data class HomeworkReminderState(
    val homework: HomeworkEntity,
    val classEntity: ClassEntity,
    val nextOccurrenceMillis: Long,
    val isUrgent: Boolean
)

data class ExamStats(
    val totalStudents: Int = 0,
    val passed: Int = 0,
    val failed: Int = 0,
    val highestMark: Double = 0.0,
    val highestMarkStudentName: String? = null,
    val lowestMark: Double = 0.0,
    val lowestMarkStudentName: String? = null,
    val averageMark: Double = 0.0
)

data class SafeTime(val hour: Int, val minute: Int) {
    companion object {
        fun now(): SafeTime {
            val calendar = java.util.Calendar.getInstance()
            return SafeTime(
                calendar.get(java.util.Calendar.HOUR_OF_DAY),
                calendar.get(java.util.Calendar.MINUTE)
            )
        }
    }
}

fun getDayOfWeekInt(date: java.util.Date? = null): Int {
    val calendar = java.util.Calendar.getInstance()
    if (date != null) {
        calendar.time = date
    }
    val day = calendar.get(java.util.Calendar.DAY_OF_WEEK)
    return when (day) {
        java.util.Calendar.MONDAY -> 1
        java.util.Calendar.TUESDAY -> 2
        java.util.Calendar.WEDNESDAY -> 3
        java.util.Calendar.THURSDAY -> 4
        java.util.Calendar.FRIDAY -> 5
        java.util.Calendar.SATURDAY -> 6
        java.util.Calendar.SUNDAY -> 7
        else -> 1
    }
}

fun getStartOfTodayMillis(): Long {
    val cal = java.util.Calendar.getInstance()
    cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
    cal.set(java.util.Calendar.MINUTE, 0)
    cal.set(java.util.Calendar.SECOND, 0)
    cal.set(java.util.Calendar.MILLISECOND, 0)
    return cal.timeInMillis
}

/** Timezone-safe epoch-day helpers (raw millis / 86400000 is wrong for UTC+5:45). */
fun todayEpochDay(): Long = java.time.LocalDate.now().toEpochDay()

fun localDateToEpochDay(dateString: String): Long? = try {
    java.time.LocalDate.parse(dateString).toEpochDay()
} catch (e: Exception) {
    null
}

fun epochDayToDateString(epochDay: Long): String =
    java.time.LocalDate.ofEpochDay(epochDay).toString()

fun formatEpochDay(epochDay: Long): String =
    java.time.format.DateTimeFormatter.ofPattern("EEE, MMM d yyyy")
        .format(java.time.LocalDate.ofEpochDay(epochDay))
