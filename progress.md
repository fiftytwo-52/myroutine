# MyClass Upgrade Progress

## Current status

- [x] Preserve the tutor profile when Android recreates the app after it is removed from recents.
- [x] Show the next upcoming dated event below the Dashboard English date when available.
- [x] Add class-wise filtering to the Students list alongside search.
- [x] Show `Holiday` in the Dashboard class schedule heading for selected marked holiday dates.
- [x] Show highest and lowest graded marks on exam cards.
- [x] Add an exam result dialog with metadata, high/low student names, average, pass/fail counts, and student marks.
- [x] Add Android print-framework PDF export for exam result details.
- [x] Show `Break` in Dashboard period thumbnails for break and lunch entries.
- [x] Make JSON backup import replace existing persisted application data inside a Room transaction.
- [x] Add confirmation warnings before all identified user-facing delete/reset operations, including JSON replacement restore.
- [x] Synchronize Notes and Settings calendar highlighting for persisted holidays and dated events.
- [x] Port the class-window routine service, exact-restart alarm, boot re-arm, and all four home-screen widgets into this `com.example` codebase and verify with `assembleDebug`.
- [x] Redesign the Dashboard Nepali date badge as a true wavy circle (sine-modulated 12-bump Canvas path) with a small Nepali year badge.
- [x] Compact the Dashboard ongoing-class (LIVE) card height.
- [x] Fix exam creation target-class multiselect so all classes are visible/selectable via chips.
- [x] Rename the Settings classes tab header to "Add / Remove Classes" with a clarifying subtitle.
- [x] Fix the "Done" button in the exam detailed-view popup so it dismisses.
- [x] Add the target "Class:" line to the exam PDF export header.
- [x] Add a system-wide Light/Dark theme switcher in Settings (Appearance tab) with DataStore persistence and status-bar icon sync.
- [x] Mark dated events as Completed/Cancelled/Failed from the Dashboard with a status popup.
- [x] Show short class names (e.g. "1A") in schedule period thumbnail circles.
- [x] Add a Settings → Dashboard screen with system-wide statistics and multi-page PDF export via the print framework.
- [x] Verification pass: fixed 6 compile errors left by the previous agent (Paint color shadowing in DashboardPdfGenerator, Modifier.weight outside RowScope in SettingsScreen) and verified `assembleDebug` exits 0.

- [x] Read and reviewed [`upgrade.md`](upgrade.md), including all four upgrade phases.
- [x] Confirmed the project already uses Room persistence for core entities, DAOs, and the database.
- [x] Audit dashboard sorting and remove the three requested schedule-header actions.
- [x] Audit and fix holiday selection state and extend calendar management with upcoming events.
- [x] Complete the light-mode palette, flat list/calendar styling, compact cards, tabs, and bottom navigation migration.
- [x] Implement the required functional and visual adjustments.
- [x] Build and verify the Android project (uses the Gradle-managed JetBrains JDK 21 via `JAVA_HOME`; `:app:compileDebugKotlin` succeeds).

## Notes

- Application startup seeds the singleton teacher profile only when the row is absent, preventing process recreation from overwriting saved profile data.
- The Dashboard selects the nearest event dated today or later and displays it below the English date; saved holiday ranges and recurring weekly holidays are applied to the selected schedule date.
- Exam statistics include only numeric marks; absent and ungraded students remain visible in the detailed list but are excluded from average and pass/fail calculations. Deleting an exam now removes its associated marks first.
- PDF export uses Android's print service, allowing the complete exam summary and student marks table to be saved as a PDF.
- Break-like schedule entries are detected from their class or subject label and display `Break` in the Dashboard circle thumbnail.
- JSON restore validates the backup shape before clearing tables and replaces all Room-managed records atomically.
- Destructive actions are confirmation-gated for notes, events, holidays, schedules, managed classes, homework, exams, syllabus, students, and student performance remarks/resets.
- Notes and Settings calendar views share the same `holiday` table and dated `note.eventEpochDay` records.

- [`upgrade.md`](upgrade.md) is now populated with the implementation requirements.
- The dashboard now groups completed classes after active, upcoming, and scheduled classes while preserving chronological order within each group.
- The schedule header no longer shows Add Event, Monthly Calendar View, or Homework Assignment actions.
- Notes and calendar flows now create their shared state outside direct composition flow operators, and note timestamps use the observable app locale.
- Calendar Management now supports persisted upcoming event create, edit, and delete actions through dated notes.
- Holiday range selection normalizes reversed date clicks, and calendar cells use flat pastel indicators without tile borders.
- The app is forced to light mode with an off-white background, muted lilac/mint/peach accents, high-contrast dark text, compact 10 dp cards, and a flat white bottom bar with a top divider and pastel active indicator.
- Dated calendar events are excluded from the Teacher Journal while remaining available on the calendar and dashboard, and obsolete Dashboard event-creation code has been removed.
- Build verification was attempted, but this machine has no executable Java runtime and no configured `JAVA_HOME`; Java 17 is required by the project.
- This file will be updated whenever a goal is completed.
- The marks entry dialog now holds typed marks/remarks as per-student drafts and applies them in bulk with a single Save button in the dialog action row, placed to the left of Close; the Absent checkbox still saves immediately.
- Saving marks requires no completeness — even a single student's entered mark is accepted, and untouched students are simply skipped.
- The marks field rejects values above the exam's full marks (and negatives), and now opens a decimal keyboard.
- Four home-screen widgets are available: "MyClass Classes" — a compact single-row pill showing the ongoing class (left circle, red border) and the upcoming class (right circle, green border) with the class name (e.g. "10A") and its start time in large bold text inside each circle; "MyClass Events" — next three calendar events with dates; "MyClass Holidays" — next three saved holiday ranges; and "MyClass Exams" — next three exams with dates. All refresh every 30 s from the routine alert service loop, show empty-state messages, and open the app on tap. The classes widget respects saved and weekly holidays.
- Status bar icons are now always dark: since the app is permanently light-themed, `enableEdgeToEdge()` used to follow the system dark mode and render white clock/battery icons on the light background, making the status bar invisible in system dark mode. The activity now forces `SystemBarStyle.light` for both bars.
- Weekly-holiday schedules are hidden everywhere: if a weekday (e.g. Friday) is marked as a recurring weekly holiday in Settings → Calendar, or a date falls in a saved holiday range, its class periods are not shown — the dashboard day view shows the "Holiday" header with an explanatory message, the Settings routine section shows "Weekly Holiday" instead of the period list (Add Period is hidden there too, with a note explaining how to unhide for editing), and the classes widget shows its holiday state. Saving/changing recurring weekly holiday days now also refreshes all home-screen widgets immediately. Class reminders were already skipped on those days.
- The bottom navigation bar is a floating pill: 32 dp rounded corners, semi-transparent (88% alpha) surface, horizontal screen margins, and a subtle outline border replacing the full-width bar with top divider.
- The persistent notification now exists only during class hours: the service computes each day's class window (first period start minus the 2-minute reminder lead, to the last period end, e.g. 9:27–15:30 for 9:30–15:30 classes) and posts "Classes 9:30 AM – 3:30 PM" while it runs. Outside the window it removes the notification, stops itself, and schedules an exact AlarmManager alarm to restart at the next class window (holidays and weekly holidays skipped, scanning up to 30 days ahead). The alarm is re-armed on app open, on schedule/holiday/widget-refresh changes, and after reboot via BootCompletedReceiver; "2 minutes left" reminders still fire inside the window.
- Newly saved or deleted holidays and dated events now appear on the home-screen widgets immediately: every save/delete in Settings (Calendar Management), the Holidays screen, and the Notes calendar triggers `WidgetRefresh.refreshAll()`, so the "MyClass Holidays"/"MyClass Events" lists no longer show stale or missing names until the next class window.
- Widgets stay fresh even while the reminder service is stopped: a new `WidgetAlarmReceiver` repeating inexact alarm (every 30 minutes, registered in the manifest, no notification) redraws all four widgets; the service arms it when it stops outside class hours and cancels it while its own 30-second loop is running. `syncNow` also arms it when no class window exists at all.
- The bottom navigation is now a true pill: a fully rounded (50%) floating container with a subtle border, one evenly weighted item per destination, each drawn as a 34 dp circular chip (primaryContainer when selected) with a 19 dp icon and an always-visible label beneath — replacing the Material `NavigationBar` whose internally fixed item sizing fought the pill shape.
- The doubled status-bar gap at the top was removed: the outer app Scaffold now uses `contentWindowInsets = WindowInsets(0, 0, 0, 0)` since every screen's own Scaffold/TopAppBar already applies the status-bar inset, reclaiming that vertical space.
- Contrast was increased app-wide: the light palette's text tones (`onSurface`, `onSurfaceVariant`, `onBackground`), outlines, and accent/status colors were deepened one tonal step, and the shared widget-list secondary text changed from `#5F6368` to `#41454C`.
- The Notes → Holiday Calendar tab now has a pill-shaped search bar at the top: typing a name case-insensitively filters both the "Upcoming & Scheduled Holidays" and "Upcoming & Scheduled Events" lists below the calendar, with per-section no-match messages and a clear (×) button; the month grid itself stays unfiltered.
- The Dashboard's top greeting/hero card corner radius was increased from 10 dp to 24 dp, distinguishing it from the compact 10 dp content cards.
- Fixed the root cause of "holiday name not showing": the Settings → Calendar save handler cleared `holidayTitle` (and selection state) synchronously while the `scope.launch` coroutine only ran afterwards, so the Holiday was persisted with a blank title. The trimmed title is now captured into a local val before launching. The Settings "Configured Holidays" and Holidays screen lists also render a "Holiday" fallback for previously saved blank titles.
- The Holiday Calendar tab now has a Holidays/Events toggle (two side-by-side FilterChips with live counts) below the calendar legend. Only the selected list is rendered, so the holidays list no longer pushes the events list below it; the choice survives process death via `rememberSaveable`. While a search query is active, both sections are shown (filtered) so a name can be found in either list.
- The bottom pill navigation bar was enlarged so it no longer looks thin: icon circles 34→40 dp, icons 19→22 dp, labels upgraded from labelSmall to labelMedium, 2 dp spacing between icon and label, and inner/vertical paddings increased (8 dp outer, 4 dp per item).
- The Teaching Journal (Notes) screen — view-only Holiday Calendar, searchable/tag-filtered note list, and add/edit dialogs — was re-linked into the app: a `notes` route was added to the NavHost and a sixth "Notes" item (StickyNote2 filled/outlined icons) to the floating pill bottom bar in `MainActivity`, restoring access to the previously orphaned screen so the app matches the documented six-destination navigation.
- The class-window service is now implemented in this tree as `ClassFlowService`: it computes each day's window (first start − 2 min … last end), runs a 30-second loop inside it that posts the persistent "Classes 9:30 AM – 3:30 PM" notification with NOW/NEXT detail, fires the 2-minute-before reminders plus start/end alerts, and refreshes all widgets each tick. Outside the window it stops itself and schedules a single exact AlarmManager alarm (via `BootReceiver.ACTION_START_SERVICE`) for the next window, scanning up to 30 days ahead while skipping saved and weekly holidays; the same alarm is re-armed on app open/schedule changes and after reboot.
- The four home-screen widgets are now implemented in this tree under `widget/`: Classes (ongoing class in a red-bordered circle + upcoming class in a green-bordered circle, holiday-aware), Events, Holidays, and Exams (next three items each with dates, empty states, tap-to-open). `WidgetRefresh.refreshAll()` redraws them from widget onUpdate, the service loop, `ScheduleSync`, and every holiday/event save/delete; `WidgetAlarmReceiver` keeps a manifest-registered 30-minute repeating refresh armed while the service is stopped. All four receivers plus the alarm receiver are declared in the manifest.
- Workspace note: the project tree holds the `com.example` (AI Studio / recovered) codebase. The service, widget, boot-restart, and widget-alarm features described above were re-implemented here (previously they existed only in the earlier `com/myclass/app` tree), and `./gradlew assembleDebug` completes successfully using the Gradle-managed JetBrains JDK 21 via `JAVA_HOME=/home/fiftytwo/.gradle/jdks/jetbrains_s_r_o_-21-amd64-linux.2`.
