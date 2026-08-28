package com.example.data.repository

import com.example.data.dao.*
import com.example.data.entity.*
import kotlinx.coroutines.flow.Flow

class ClassScheduleRepository(
    val classDao: ClassDao,
    val homeworkDao: HomeworkDao,
    val teacherDao: TeacherProfileDao,
    val studentDao: StudentDao,
    val teacherNoteDao: TeacherNoteDao,
    val holidayDao: HolidayDao,
    val classLogDao: ClassLogDao,
    val examDao: ExamDao,
    val homeworkSubmissionDao: HomeworkSubmissionDao,
    val managedClassDao: ManagedClassDao,
    val studentActivityDao: StudentActivityDao,
    val syllabusDao: SyllabusDao
) {
    // Syllabus
    val allSyllabuses: Flow<List<SyllabusEntity>> = syllabusDao.getAllSyllabuses()

    suspend fun insertSyllabus(syllabus: SyllabusEntity): Long = syllabusDao.insertSyllabus(syllabus)
    suspend fun insertUnit(unit: UnitEntity): Long = syllabusDao.insertUnit(unit)
    suspend fun insertUnits(units: List<UnitEntity>): List<Long> = syllabusDao.insertUnits(units)
    suspend fun insertTopic(topic: TopicEntity): Long = syllabusDao.insertTopic(topic)
    suspend fun insertTopics(topics: List<TopicEntity>): List<Long> = syllabusDao.insertTopics(topics)
    suspend fun insertTopicProgress(progress: TopicProgressEntity) = syllabusDao.insertTopicProgress(progress)
    fun getUnitsForSyllabus(syllabusId: Int) = syllabusDao.getUnitsForSyllabus(syllabusId)
    fun getTopicsForUnit(unitId: Int) = syllabusDao.getTopicsForUnit(unitId)
    suspend fun getAllTopicsForSyllabus(syllabusId: Int) = syllabusDao.getAllTopicsForSyllabus(syllabusId)
    fun getTopicProgressForClass(classId: Int) = syllabusDao.getTopicProgressForClass(classId)
    suspend fun getProgress(topicId: Int, classId: Int) = syllabusDao.getProgress(topicId, classId)
    suspend fun deleteSyllabus(syllabusId: Int) = syllabusDao.deleteSyllabus(syllabusId)

    // Managed Classes
    val allManagedClasses: Flow<List<ManagedClassEntity>> = managedClassDao.getAllManagedClasses()
    suspend fun insertManagedClass(managedClass: ManagedClassEntity) = managedClassDao.insertManagedClass(managedClass)
    suspend fun deleteManagedClass(managedClass: ManagedClassEntity) = managedClassDao.deleteManagedClass(managedClass)

    // Student Activities
    val allActivities: Flow<List<StudentActivityEntity>> = studentActivityDao.getAllActivities()
    fun getActivitiesForStudent(studentId: Int): Flow<List<StudentActivityEntity>> = studentActivityDao.getActivitiesForStudent(studentId)
    suspend fun insertActivity(activity: StudentActivityEntity) = studentActivityDao.insertActivity(activity)
    suspend fun deleteActivity(id: Int) = studentActivityDao.deleteActivity(id)

    // Exams & Marks
    val allExams: Flow<List<ExamEntity>> = examDao.getAllExams()
    fun getMarksForExamAndClass(examId: Int, classId: Int): Flow<List<ExamMarkEntity>> = examDao.getMarksForExamAndClass(examId, classId)
    fun getAllMarksForExam(examId: Int): Flow<List<ExamMarkEntity>> = examDao.getAllMarksForExam(examId)
    fun getMarksForStudent(studentId: Int): Flow<List<ExamMarkEntity>> = examDao.getMarksForStudent(studentId)
    
    suspend fun insertExam(exam: ExamEntity): Long = examDao.insertExam(exam)
    suspend fun deleteExam(examId: Int) {
        examDao.deleteMarksForExam(examId)
        examDao.deleteExam(examId)
    }
    suspend fun insertMark(mark: ExamMarkEntity) = examDao.insertMark(mark)

    // Submissions
    suspend fun getNotDoneSubmissionsForStudent(studentId: Int) = homeworkSubmissionDao.getNotDoneSubmissionsForStudent(studentId)
    fun getSubmissionsForHomework(homeworkId: Int) = homeworkSubmissionDao.getSubmissionsForHomework(homeworkId)
    suspend fun insertSubmission(submission: HomeworkSubmissionEntity) = homeworkSubmissionDao.insertSubmission(submission)

    // Holidays
    val allHolidays: Flow<List<HolidayEntity>> = holidayDao.getAllHolidays()

    suspend fun insertHoliday(holiday: HolidayEntity): Long = holidayDao.insertHoliday(holiday)
    suspend fun updateHoliday(holiday: HolidayEntity) = holidayDao.updateHoliday(holiday)
    suspend fun deleteHoliday(holiday: HolidayEntity) = holidayDao.deleteHoliday(holiday)

    // Classes
    val allClasses: Flow<List<ClassEntity>> = classDao.getAllClasses()
    
    fun getClassesForDay(dayOfWeek: Int): Flow<List<ClassEntity>> = 
        classDao.getClassesForDay(dayOfWeek)

    suspend fun getClassById(id: Int): ClassEntity? = classDao.getClassById(id)
    
    suspend fun insertClass(classEntity: ClassEntity): Long = classDao.insertClass(classEntity)
    
    suspend fun updateClass(classEntity: ClassEntity) = classDao.updateClass(classEntity)
    
    suspend fun deleteClass(classEntity: ClassEntity) = classDao.deleteClass(classEntity)

    // Teachers
    val allTeachers: Flow<List<TeacherProfileEntity>> = teacherDao.getAllTeachers()
    
    suspend fun getTeacherById(id: Int): TeacherProfileEntity? = teacherDao.getTeacherById(id)
    
    suspend fun insertTeacher(teacher: TeacherProfileEntity): Long = teacherDao.insertTeacher(teacher)
    
    suspend fun updateTeacher(teacher: TeacherProfileEntity) = teacherDao.updateTeacher(teacher)
    
    suspend fun deleteTeacher(teacher: TeacherProfileEntity) = teacherDao.deleteTeacher(teacher)

    // Homework
    val allHomework: Flow<List<HomeworkEntity>> = homeworkDao.getAllHomework()
    
    val allHomeworkWithClass: Flow<List<HomeworkWithClass>> = homeworkDao.getAllHomeworkWithClass()
    
    fun getHomeworkForClass(classId: Int): Flow<List<HomeworkEntity>> = 
        homeworkDao.getHomeworkForClass(classId)

    fun getHomeworkByCompletionWithClass(isCompleted: Boolean): Flow<List<HomeworkWithClass>> =
        homeworkDao.getHomeworkByCompletionWithClass(isCompleted)

    suspend fun insertHomework(homework: HomeworkEntity): Long = homeworkDao.insertHomework(homework)
    
    suspend fun updateHomework(homework: HomeworkEntity) = homeworkDao.updateHomework(homework)
    
    suspend fun deleteHomework(homework: HomeworkEntity) = homeworkDao.deleteHomework(homework)

    // Students
    val allStudents: Flow<List<StudentEntity>> = studentDao.getAllStudents()
    
    fun getStudentsByClass(className: String): Flow<List<StudentEntity>> = studentDao.getStudentsByClass(className)
    
    suspend fun deleteStudentsByClass(className: String) = studentDao.deleteStudentsByClass(className)

    suspend fun insertStudent(student: StudentEntity): Long = studentDao.insertStudent(student)
    
    suspend fun updateStudent(student: StudentEntity) = studentDao.updateStudent(student)
    
    suspend fun deleteStudent(student: StudentEntity) = studentDao.deleteStudent(student)

    // Teacher Notes
    val allNotes: Flow<List<TeacherNoteEntity>> = teacherNoteDao.getAllNotes()

    // Dated calendar events (notes pinned to a specific date)
    val allDatedEvents: Flow<List<TeacherNoteEntity>> = teacherNoteDao.getDatedEvents()

    fun getEventsForDay(epochDay: Long): Flow<List<TeacherNoteEntity>> =
        teacherNoteDao.getEventsForDay(epochDay)
    
    fun getNotesByTag(tag: String): Flow<List<TeacherNoteEntity>> = teacherNoteDao.getNotesByTag(tag)
    
    suspend fun insertNote(note: TeacherNoteEntity): Long = teacherNoteDao.insertNote(note)
    
    suspend fun updateNote(note: TeacherNoteEntity) = teacherNoteDao.updateNote(note)
    
    suspend fun deleteNote(note: TeacherNoteEntity) = teacherNoteDao.deleteNote(note)


    /**
     * Calculates the exact millisecond of the NEXT sequential occurrence of a class
     * starting after a homework's specific creation date.
     * This allows us to display a prominent reminder during that next sequential run!
     */
    fun calculateNextOccurrenceMillis(homework: HomeworkEntity, classEntity: ClassEntity): Long {
        val createdCal = java.util.Calendar.getInstance().apply {
            timeInMillis = homework.createdDateMillis
        }
        
        val startTimeParts = try {
            val parts = classEntity.startTime.split(":")
            Pair(parts[0].toInt(), parts[1].toInt())
        } catch (e: Exception) {
            Pair(0, 0)
        }
        
        val targetCalendarDay = when (classEntity.dayOfWeek) {
            1 -> java.util.Calendar.MONDAY
            2 -> java.util.Calendar.TUESDAY
            3 -> java.util.Calendar.WEDNESDAY
            4 -> java.util.Calendar.THURSDAY
            5 -> java.util.Calendar.FRIDAY
            6 -> java.util.Calendar.SATURDAY
            7 -> java.util.Calendar.SUNDAY
            else -> java.util.Calendar.MONDAY
        }
        
        val sameDayClassCal = (createdCal.clone() as java.util.Calendar).apply {
            set(java.util.Calendar.HOUR_OF_DAY, startTimeParts.first)
            set(java.util.Calendar.MINUTE, startTimeParts.second)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        
        val currentDay = createdCal.get(java.util.Calendar.DAY_OF_WEEK)
        
        return if (currentDay == targetCalendarDay && createdCal.before(sameDayClassCal)) {
            sameDayClassCal.timeInMillis
        } else {
            val nextCal = (createdCal.clone() as java.util.Calendar).apply {
                set(java.util.Calendar.HOUR_OF_DAY, startTimeParts.first)
                set(java.util.Calendar.MINUTE, startTimeParts.second)
                set(java.util.Calendar.SECOND, 0)
                set(java.util.Calendar.MILLISECOND, 0)
            }
            do {
                nextCal.add(java.util.Calendar.DAY_OF_YEAR, 1)
            } while (nextCal.get(java.util.Calendar.DAY_OF_WEEK) != targetCalendarDay)
            nextCal.timeInMillis
        }
    }
}
