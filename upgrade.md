### Agent Task List & Implementation Guide

**Context for Agent:**
You are tasked with fixing core architecture bugs, updating UI layout logic, and migrating a high-density, dark-mode utility app into a specific light-mode design system. Do not alter the data density or padding drastically; adhere strictly to the design constraints below.

#### Phase 1: Architecture & Data Persistence

* **Implement Persistent Storage:** Resolve the data loss issue upon app termination. Migrate Teacher Profile, Student Data, and Settings from volatile state memory to a persistent local storage solution (e.g., SQLite, Room, or secure Key-Value storage) or ensure reliable backend synchronization.

#### Phase 2: Functional & Logic Updates

* **Homepage Sorting Logic:** Modify the "Today's Schedule" list logic. Move completed classes to the bottom of the list. *(Note: Ensure the "Completed" state is clearly visually distinct so users understand why the chronological order is broken).*
* **Homepage UI Cleanup:** Locate the "Today's Schedule" header section. Remove the following UI elements from the right side:
* "Add Event" button
* "Monthly Calendar View" button
* "Homework Assignment" button


* **Holiday & Calendar Management:**
* Debug and fix the state-selection issue preventing days from being marked as holidays in the calendar view.
* Rename the "Holidays" section in Settings to "Calendar Management."
* Expand the "Calendar Management" data model and UI to support creating, editing, and deleting general "Upcoming Events" alongside holidays.



#### Phase 3: Global Design System Migration

* **Theme & Palette:**
* Migrate the entire application to Light Mode.
* Set the global app background to an off-white/very light gray (e.g., `#F8F9FA`).
* Use muted pastels (peach, mint green, lilac) strictly for accent colors, active states, tags, and progress rings.


* **Typography & Contrast:**
* Implement a clean, rounded geometric sans-serif font globally (e.g., Poppins, Quicksand, or Nunito).
* Enforce high contrast: All primary text (names, times, critical data) must be a highly legible dark gray/near-black (`#1A1A1A`). Do not use faint gray text for vital information.



#### Phase 4: Component-Specific Styling

* **High-Density Lists (Students, Routine, Notes):**
* Convert current dark mode list rows into flat, white, rectangular containers.
* Apply a subtle 1px border or a very faint drop shadow to separate rows from the background.
* Set the corner radius for list items to `8px` - `12px` maximum (do not use extreme pill-shapes for rows).
* Group utility action icons (Print, Menu, Edit, Delete) to the right side, using a minimal, dark gray color.


* **Grids & Calendars (Classes, Holidays):**
* **Class Sections (64 items):** Render active class sections as compact, flat pill shapes with a light pastel background and dark text. Minimize padding to ensure grid scannability.
* **Calendars:** Remove borders from calendar grids to create a flat table layout. Highlight active dates (holidays/events) using solid pastel circles or squares positioned *behind* the date number.


* **Navigation:**
* **Bottom Bar:** Update to a flat white bar with a subtle top border. The active tab must be represented by a soft pastel pill shape placed behind the icon.
* **Top Tabs:** Indicate the active top tab (Profile, Routine, Classes, Holidays) using a clean underline or a floating pastel pill.