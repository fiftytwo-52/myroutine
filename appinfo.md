# MyClass Application - Development Task List

This document outlines every feature, screen, and functionality of the MyClass application structured as implementation tasks. This format is designed for developer agents to methodically build or replicate the application.

## 1. Core Architecture & Database Setup
- [x] **Task 1.1:** Initialize Android project with Kotlin, Jetpack Compose (Material 3), and Room Database.
- [x] **Task 1.2:** Implement Room Entities for `ClassSchedule`, `ClassLog`, `Exam`, `ExamMark`, `Holiday`, `Homework`, `HomeworkSubmission`, `ManagedClass`, `StudentActivity`, `Student`, `Syllabus`, `Note`, and `TeacherProfile`.
- [x] **Task 1.3:** Setup Room DAOs and the main `AppDatabase` configuration.
- [x] **Task 1.4:** Implement ViewModel to handle StateFlow and UI state management for all screens.
- [x] **Task 1.5:** Implement persistent Liquid Glass Floating Bottom Navigation Bar routing to Home, Syllabus, Students, Exams, Notes, and Settings.
- [x] **Task 1.6:** Implement `NepaliCalendarUtils` helper class containing logic for Bikram Sambat (BS) Nepali Calendar conversion, translating Gregorian dates to Nepali dates (days, months, digits).

## 2. Home Screen & Dashboard
- [x] **Task 2.1:** Build the dynamic header displaying the live current time (without seconds, `hh:mm a`), today's Gregorian date (bigger font), and current day of the week.
- [x] **Task 2.2:** Integrate the **Nepali Date (Bikram Sambat/BS)** display on the Home Screen header formatted in a clean transparent pill container (`BS: Poush 17, 2082 BS`).
- [x] **Task 2.3:** Implement a Nepali Date Offset adjustment dialog (launched by clicking the date pill) allowing users to adjust the BS calendar date forward/backward by a number of days to fix leap year inaccuracies.
- [x] **Task 2.4:** Build the **Pill-Shaped Weekly Calendar Row (Mon - Sun)** directly above the schedule section, allowing users to filter and inspect any day's routine with one tap.
- [x] **Task 2.5:** Create the **Dedicated Ongoing Class Card** positioned right below the header:
  - Displays ongoing period (e.g. `6B`) with active indicator badge.
  - Calculates & displays next class period for the same class (e.g. *"Next 6B class: Thursday at 13:05 - 13:45 (Computer)"*).
- [x] **Task 2.6:** Implement **School Holiday Override Logic**: Check if today is a custom school holiday or weekly off (Saturday). If so, display a prominent **"SCHOOL HOLIDAY"** banner and disable active session tracking.
- [x] **Task 2.7:** Create Class Item Cards displaying Subject, Target Class/Section inside a **circular background badge** (e.g., `6A`, `8A`), Room Number, and bigger Start/End Times.
- [x] **Task 2.8:** Implement real-time status indicators logic on Class Cards (`ACTIVE`, `UPCOMING`, `COMPLETED`, `SCHEDULED`) based on device clock comparison.
- [x] **Task 2.9:** Create the **Class Log Dialog** (launched from a class card) with status options (Completed, Cancelled, Half completed, Disturbed) and a text field for teaching notes.
- [x] **Task 2.10:** Create the **Homework Management Dialog** with input fields for Task Title, Homework Guidelines, and Associated Classwork Note.
- [x] **Task 2.11:** Add interactive Deadline Chips to the Homework Dialog ("Next Class", "Tomorrow", "Custom Date Picker").
- [x] **Task 2.12:** Integrate Android Camera (`TakePicture`) and Gallery (`GetContent`) intents to allow users to attach images to homework assignments.
- [x] **Task 2.13:** Implement a cropped image preview for attachments with a removal button.
- [x] **Task 2.14:** Build the Homework Submission Tracking UI inside the dialog, allowing the teacher to toggle student statuses ("Done", "Half Done", "Not Done").

## 3. Syllabus Management
- [x] **Task 3.1:** Build the Syllabus Screen with a horizontally scrollable Tab Row to switch between different managed classes.
- [x] **Task 3.2:** Implement the Add/Edit Syllabus Dialog (FAB) with fields for Subject and Class Name.
- [x] **Task 3.3:** Add raw text input capabilities for manual syllabus entry.
- [x] **Task 3.4:** Add a **PDF Picker integration** (`ActivityResultContracts.OpenDocument`) to allow attaching PDF syllabus files.
- [x] **Task 3.5:** Implement the "Open PDF Syllabus" button that fires an `ACTION_VIEW` intent to open the attached PDF in a native system viewer.
- [x] **Task 3.6:** Build the Unit & Topic Tracking UI, displaying units as cards and a linear progress bar for completion percentages.
- [x] **Task 3.7:** Implement interactive topic checkboxes that apply strikethrough text formatting when marked as completed.

## 4. Student Management & Roster
- [x] **Task 4.1:** Build the Students Screen list view grouped/sorted by class (fixed list recomposition to prevent blinking).
- [x] **Task 4.2:** Implement a top Search Bar to filter students by Name or Roll Number.
- [x] **Task 4.3:** Create the Add/Edit Student Dialog with inputs for Name, Roll No, Class, Contact, Guardian, and Behavioral Notes.
- [x] **Task 4.4:** Add a "Reset Behavior" button/action within the Student edit profile.
- [x] **Task 4.5:** Build the Student Detailed Report Dialog (triggered via print/info icon) that summarizes a student's profile and recent activities.
- [x] **Task 4.6:** Implement the "Print/Save PDF" functionality to generate a PDF report card via `PrintManager`.
- [x] **Task 4.7:** Build the Bulk Import Dialog with CSV format warnings.

## 5. Exams & Grading
- [x] **Task 5.1:** Build the Exams Screen list view showing cards for upcoming and past exams.
- [x] **Task 5.2:** Create the Add Exam Dialog (FAB) with **Target Class Selection Chips** (populated from registered setting classes), Subject, Date (Picker), Full Marks, and Pass Marks.
- [x] **Task 5.3:** Build the Marks Entry Dialog containing a scrollable roster of students for the selected exam/class.
- [x] **Task 5.4:** Implement individual inputs for "Marks Obtained" and "Remarks" for each student row.
- [x] **Task 5.5:** Implement an **"Absent" checkbox toggle** for each student in the marks entry dialog, which automatically fills the mark value as "Absent" and disables the text input.
- [x] **Task 5.6:** Add dynamic visual validation (e.g., highlighting row/text in red) if the entered marks are below the defined Pass Marks threshold, and skip validation if marked Absent.
- [x] **Task 5.7:** Implement batch-save logic to commit all exam marks simultaneously to the Room database.

## 6. Notes & Holiday Calendar
- [x] **Task 6.1:** Build an interactive month-view Calendar Grid (View-Only format for the Notes tab).
- [x] **Task 6.2:** Integrate highlighting for specific holiday date ranges and weekly recurring holidays in the calendar view, showing tooltips or dialogs with the holiday title when tapped.
- [x] **Task 6.3:** Build the Teacher Journal list view displaying chronologically sorted note cards.
- [x] **Task 6.4:** Create the Add/Edit Note Dialog with fields for Title and Content.
- [x] **Task 6.5:** Implement categorized **Tag Chips** ("All", "Lesson Plan", "Homework Idea", "General", "Observation") as a horizontally scrollable LazyRow to filter and save specific types of notes with distinct colors.

## 7. Settings & Configurations
- [x] **Task 7.1:** Build the Settings Screen divided into categorical tabs/groups.
- [x] **Task 7.2 - Profile & Automation:** Create the Teacher Profile editor (Name, Email, Office, Specialty, Date of Birth picker).
- [x] **Task 7.3 - Profile & Automation:** Implement age calculation logic (`calculateAgeInt`) that parses the entered Date of Birth to dynamically display the teacher's current age.
- [x] **Task 7.4 - Profile & Automation:** Add a toggle switch for "Phone Silencer" to automate DND during class hours.
- [x] **Task 7.5 - Routine Setup:** Build the master schedule editor with a Day-of-Week selector tab row.
- [x] **Task 7.6 - Routine Setup:** Create the Class Setup Dialog featuring Start Time and End Time clock pickers, with validation ensuring end time is after start time.
- [x] **Task 7.7 - Subjects/Managed Classes:** Create a UI to add, edit, or delete the master list of active classes/sections (e.g., "5A", "10B").
- [x] **Task 7.8 - Holidays Configuration:** Implement **Calendar Date Range Picker** in Settings to allow users to select start date and end date ranges (e.g., "Winter Break", "Dashain Vacation") and persist them to the database.
- [x] **Task 7.9 - Data Backup:** Implement database JSON Export (save data to JSON file) and Import (restore data from JSON file) functionalities.
- [x] **Task 7.10:** Add an "About / Developer Info" card.
