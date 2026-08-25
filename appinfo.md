# MyClass - Comprehensive Application Features & Functionality Guide

This document details **every single functionality, action, and user interaction** available within the **MyClass** application. It serves as a master reference for everything the app is capable of doing from an end-user perspective.

---

## 1. Class & Schedule Management (Home & Dashboard)
The core of the application revolves around managing daily classes and schedules.

- **Dashboard View**: Displays today's date and a real-time status of the current, upcoming, and completed classes based on the device's clock.
- **Class Filtering**: Automatically filters the schedule to show only the classes for the current day of the week.
- **Dynamic Status Indicators**: Classes show status tags such as "ACTIVE", "UPCOMING", "COMPLETED", or "SCHEDULED" depending on the current time versus the class start and end times.
- **Class Details**: 
  - Displays Subject Name, Class Section (e.g., "5A"), Room Number, and time duration (e.g., "09:00 - 10:00").
- **Add New Class (Schedule Entry)**: 
  - Dialog to add a new class schedule.
  - Requires Subject, Class Name, Room Number, Day of the Week (Monday-Sunday selection), Start Time (Time picker), and End Time (Time picker).
- **Class Logs & Teaching Journals**: 
  - Teachers can log details for any scheduled class.
  - Track status (e.g., "Completed", "Half completed", "Cancelled", "Disturbed").
  - Add specific notes for what was taught on that particular day.

## 2. Homework & Assignments
A dedicated system for assigning, tracking, and grading homework for specific classes.

- **Create Homework Assignment**:
  - Requires a Task Title and Homework Guidelines (Description).
  - Ability to link an associated "Classwork Note".
  - **Deadlines**: Select pre-defined deadlines ("Next Class", "Tomorrow") or open a Custom Date Picker calendar.
  - **Attachments**: 
    - Open the device **Camera** to snap a photo of the assignment/board.
    - Open the device **Gallery** to select an existing image file.
- **Edit Homework**: Update title, description, deadline, or replace/remove attached images.
- **View Homework**: 
  - View the full description, deadline, and a full-screen or cropped view of the attached image.
- **Submission Tracking (Grading)**:
  - View a list of all students enrolled in the class assigned to the homework.
  - Mark individual student submission statuses with interactive chips: **"Done"**, **"Half Done"**, or **"Not Done"**.
- **Delete Homework**: Swipe or button action to completely delete a homework record.

## 3. Syllabus Management
A robust module for tracking what needs to be taught and what has been taught.

- **Syllabus Creation**: 
  - Add a syllabus for a specific subject and class.
  - **Raw Text Input**: Type or paste the syllabus contents directly.
  - **PDF Attachment**: Select a PDF document from the device storage to serve as the syllabus.
- **Syllabus Viewing**:
  - Displays the syllabus notes in a structured card.
  - If a PDF was attached, provides an "Open PDF Syllabus" button that launches the device's default PDF viewer (using an Intent) directly to the attached file.
- **Topic & Unit Tracking**: 
  - (If parsed) Ability to track individual units and topics.
  - Tap on a topic to mark it as completed (strikethrough text with a checkmark icon).
  - Shows a progress bar calculating the percentage of completed topics versus total topics in a unit.

## 4. Student Management & Roster
A database of all students managed by the teacher.

- **Add Student Profile**: 
  - Input Student Name, Roll Number, Class Name/Section, Contact Number, and Guardian Name.
- **Student List View**:
  - Search or filter students.
  - View list of students grouped or sorted by their classes.
- **Student Activity & Performance Logging**:
  - Add performance notes for individual students.
  - Track behavioral notes, academic warnings, or positive reinforcement remarks.
- **Edit/Delete Student**: Update contact info, change classes, or remove the student from the roster completely.

## 5. Exams & Grading
A module to schedule examinations and track student marks.

- **Create Exam**: 
  - Input Exam Name (e.g., "Midterm"), Date (Date picker), Target Classes (comma-separated), Subject, Full Marks, and Pass Marks.
- **Exam List View**: Shows upcoming and past exams with their scheduled dates.
- **Marks Entry System**:
  - Select an exam and a specific class.
  - Displays a roster of students for that class.
  - Input numerical marks obtained for each student.
  - Add optional text remarks for the student's performance.
- **Status Indicators**: Highlights passing vs. failing grades based on the defined Pass Marks threshold.

## 6. Personal Notes (Teacher Journal)
A private note-taking area for the educator.

- **Create Note**: Title and Content fields.
- **Tagging**: Categorize notes using predefined tags like "Lesson Plan", "Homework Notes", or "General".
- **Notes List**: View all saved notes, sorted chronologically, with their respective tags displayed.

## 7. Holidays & Academic Calendar
Keep track of days off and academic events.

- **Add Holiday**: 
  - Input Holiday Title (e.g., "Winter Break").
  - Select Start Date and End Date using a calendar picker.
- **Holiday List**: Displays all upcoming holidays in a list format, showing the date range.

## 8. Settings & Profile Management
Application-wide configurations and teacher profile settings.

- **Managed Classes (Class Settings)**:
  - Define the global list of active classes (e.g., "5A", "10B").
  - Add new class names.
  - Delete obsolete class names from the system.
- **Teacher Profile**:
  - Manage personal information: Name, Email, Office Location, Subject Specialty, Date of Birth, and School Name.
- **Data Persistence**: 
  - All data across every single module (Classes, Students, Homework, Exams, Notes, Settings) is saved persistently to a local SQLite (Room) database.
  - The app functions 100% offline with zero lag.

---
*End of Functionality Guide.*
