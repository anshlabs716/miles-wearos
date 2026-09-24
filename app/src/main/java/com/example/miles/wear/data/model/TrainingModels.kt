package com.example.miles.wear.data.model

/**
 * Progressive training plans (Couch-to-5K style). Every workout day carries a
 * real [WorkoutPlan] (interval or goal) that drives the tracking service with
 * live sensors — no fake data. Rest days are explicitly marked.
 */
data class PlanDay(
    val id: String,            // "w1d1", "w2d3"...
    val weekday: String,       // "Mon"
    val label: String,         // "8 × 1 min runs"
    val workoutType: WorkoutType,
    val plan: WorkoutPlan,
    val isRest: Boolean = false
)

data class PlanWeek(
    val number: Int,
    val days: List<PlanDay>
)

data class TrainingPlan(
    val key: String,
    val name: String,
    val emoji: String,
    val description: String,
    val weeks: List<PlanWeek>
) {
    val allDays: List<PlanDay> get() = weeks.flatMap { it.days }
    val workoutDays: List<PlanDay> get() = allDays.filter { !it.isRest }
    val totalWorkouts: Int get() = workoutDays.size

    fun dayById(id: String): PlanDay? = allDays.firstOrNull { it.id == id }
}

/** Live progress state for the active training plan. */
data class TrainingPlanState(
    val planKey: String = "",
    val started: Boolean = false,
    val completedDayIds: Set<String> = emptySet(),
    val finished: Boolean = false
)

/** Built-in programs, all with real interval/goal plans. */
object TrainingCatalog {

    private fun w(week: Int, dayRows: List<PlanDay>): PlanWeek = PlanWeek(week, dayRows)

    private fun runDay(week: Int, day: Int, weekday: String, label: String, work: Int, rest: Int, reps: Int): PlanDay =
        PlanDay(
            id = "w${week}d$day",
            weekday = weekday,
            label = label,
            workoutType = WorkoutType.RUN,
            plan = WorkoutPlan(
                mode = WorkoutMode.INTERVAL,
                workSeconds = work,
                recoverySeconds = rest,
                repetitions = reps
            )
        )

    private fun goalDay(week: Int, day: Int, weekday: String, label: String, minutes: Int): PlanDay =
        PlanDay(
            id = "w${week}d$day",
            weekday = weekday,
            label = label,
            workoutType = WorkoutType.RUN,
            plan = WorkoutPlan(
                mode = WorkoutMode.GOAL,
                goalType = GoalType.DURATION,
                goalValue = minutes.toDouble()
            )
        )

    private fun kmDay(week: Int, day: Int, weekday: String, label: String, km: Double): PlanDay =
        PlanDay(
            id = "w${week}d$day",
            weekday = weekday,
            label = label,
            workoutType = WorkoutType.RUN,
            plan = WorkoutPlan(
                mode = WorkoutMode.GOAL,
                goalType = GoalType.DISTANCE,
                goalValue = km
            )
        )

    private fun rest(week: Int, day: Int, weekday: String): PlanDay =
        PlanDay(id = "w${week}d$day", weekday = weekday, label = "Rest", workoutType = WorkoutType.WALK, plan = WorkoutPlan(), isRest = true)

    /** Mon / Wed / Fri run days + rest otherwise. */
    private fun weekWith(week: Int, mon: PlanDay, wed: PlanDay, fri: PlanDay): PlanWeek =
        w(
            week,
            listOf(
                mon,
                rest(week, 2, "Tue"),
                wed,
                rest(week, 4, "Thu"),
                fri,
                rest(week, 6, "Sat"),
                rest(week, 7, "Sun")
            )
        )

    val C25K = TrainingPlan(
        key = "c25k",
        name = "Couch to 5K",
        emoji = "🏃",
        description = "8 weeks: walk + run intervals that build to 5 km",
        weeks = listOf(
            weekWith(1, runDay(1, 1, "Mon", "8 × 1 min runs", 60, 90, 8), runDay(1, 3, "Wed", "8 × 1 min runs", 60, 90, 8), runDay(1, 5, "Fri", "8 × 1 min runs", 60, 90, 8)),
            weekWith(2, runDay(2, 1, "Mon", "6 × 90s runs", 90, 120, 6), runDay(2, 3, "Wed", "6 × 90s runs", 90, 120, 6), runDay(2, 5, "Fri", "6 × 90s runs", 90, 120, 6)),
            weekWith(3, runDay(3, 1, "Mon", "6 × 2 min runs", 120, 120, 6), runDay(3, 3, "Wed", "6 × 2 min runs", 120, 120, 6), runDay(3, 5, "Fri", "6 × 2 min runs", 120, 120, 6)),
            weekWith(4, runDay(4, 1, "Mon", "5 × 3 min runs", 180, 150, 5), runDay(4, 3, "Wed", "5 × 3 min runs", 180, 150, 5), runDay(4, 5, "Fri", "5 × 3 min runs", 180, 150, 5)),
            weekWith(5, runDay(5, 1, "Mon", "5 × 4 min runs", 240, 180, 5), runDay(5, 3, "Wed", "5 × 4 min runs", 240, 180, 5), runDay(5, 5, "Fri", "5 × 4 min runs", 240, 180, 5)),
            weekWith(6, runDay(6, 1, "Mon", "5 × 5 min runs", 300, 180, 5), runDay(6, 3, "Wed", "5 × 5 min runs", 300, 180, 5), runDay(6, 5, "Fri", "5 × 5 min runs", 300, 180, 5)),
            weekWith(7, runDay(7, 1, "Mon", "6 × 5 min runs", 300, 120, 6), runDay(7, 3, "Wed", "6 × 5 min runs", 300, 120, 6), runDay(7, 5, "Fri", "6 × 5 min runs", 300, 120, 6)),
            weekWith(8, runDay(8, 1, "Mon", "6 × 6 min runs", 360, 120, 6), runDay(8, 3, "Wed", "6 × 6 min runs", 360, 120, 6), runDay(8, 5, "Fri", "6 × 6 min runs", 360, 120, 6))
        )
    )

    val FIVEK_IMPROVER = TrainingPlan(
        key = "5k_improver",
        name = "5K Improver",
        emoji = "🎯",
        description = "6 weeks: steady runs to a faster 5 km",
        weeks = listOf(
            weekWith(1, goalDay(1, 1, "Mon", "20 min steady run", 20), goalDay(1, 3, "Wed", "20 min steady run", 20), goalDay(1, 5, "Fri", "20 min steady run", 20)),
            weekWith(2, goalDay(2, 1, "Mon", "25 min steady run", 25), goalDay(2, 3, "Wed", "25 min steady run", 25), goalDay(2, 5, "Fri", "25 min steady run", 25)),
            weekWith(3, goalDay(3, 1, "Mon", "30 min steady run", 30), goalDay(3, 3, "Wed", "30 min steady run", 30), goalDay(3, 5, "Fri", "30 min steady run", 30)),
            weekWith(4, goalDay(4, 1, "Mon", "35 min steady run", 35), goalDay(4, 3, "Wed", "35 min steady run", 35), goalDay(4, 5, "Fri", "35 min steady run", 35)),
            weekWith(5, goalDay(5, 1, "Mon", "40 min steady run", 40), goalDay(5, 3, "Wed", "40 min steady run", 40), goalDay(5, 5, "Fri", "40 min steady run", 40)),
            weekWith(6, kmDay(6, 1, "Mon", "5 km goal run", 5.0), kmDay(6, 3, "Wed", "5 km goal run", 5.0), kmDay(6, 5, "Fri", "5 km goal run", 5.0))
        )
    )

    val TENK_BUILDER = TrainingPlan(
        key = "10k_builder",
        name = "10K Builder",
        emoji = "🚀",
        description = "8 weeks: long runs that build to 10 km",
        weeks = listOf(
            weekWith(1, goalDay(1, 1, "Mon", "30 min run", 30), goalDay(1, 3, "Wed", "30 min run", 30), goalDay(1, 5, "Fri", "30 min run", 30)),
            weekWith(2, goalDay(2, 1, "Mon", "35 min run", 35), goalDay(2, 3, "Wed", "35 min run", 35), goalDay(2, 5, "Fri", "35 min run", 35)),
            weekWith(3, goalDay(3, 1, "Mon", "40 min run", 40), goalDay(3, 3, "Wed", "40 min run", 40), goalDay(3, 5, "Fri", "40 min run", 40)),
            weekWith(4, goalDay(4, 1, "Mon", "45 min run", 45), goalDay(4, 3, "Wed", "45 min run", 45), goalDay(4, 5, "Fri", "45 min run", 45)),
            weekWith(5, goalDay(5, 1, "Mon", "50 min run", 50), goalDay(5, 3, "Wed", "50 min run", 50), goalDay(5, 5, "Fri", "50 min run", 50)),
            weekWith(6, goalDay(6, 1, "Mon", "55 min run", 55), goalDay(6, 3, "Wed", "55 min run", 55), goalDay(6, 5, "Fri", "55 min run", 55)),
            weekWith(7, goalDay(7, 1, "Mon", "60 min run", 60), goalDay(7, 3, "Wed", "60 min run", 60), goalDay(7, 5, "Fri", "60 min run", 60)),
            weekWith(8, kmDay(8, 1, "Mon", "10 km goal run", 10.0), kmDay(8, 3, "Wed", "10 km goal run", 10.0), kmDay(8, 5, "Fri", "10 km goal run", 10.0))
        )
    )

    val HIIT_BLASTER = TrainingPlan(
        key = "hiit",
        name = "HIIT Blaster",
        emoji = "⚡",
        description = "4 weeks: short high-intensity interval sets",
        weeks = listOf(
            weekWith(1, runDay(1, 1, "Mon", "6 × 30s sprints", 30, 30, 6), runDay(1, 3, "Wed", "6 × 30s sprints", 30, 30, 6), runDay(1, 5, "Fri", "6 × 30s sprints", 30, 30, 6)),
            weekWith(2, runDay(2, 1, "Mon", "8 × 40s sprints", 40, 30, 8), runDay(2, 3, "Wed", "8 × 40s sprints", 40, 30, 8), runDay(2, 5, "Fri", "8 × 40s sprints", 40, 30, 8)),
            weekWith(3, runDay(3, 1, "Mon", "10 × 45s sprints", 45, 30, 10), runDay(3, 3, "Wed", "10 × 45s sprints", 45, 30, 10), runDay(3, 5, "Fri", "10 × 45s sprints", 45, 30, 10)),
            weekWith(4, runDay(4, 1, "Mon", "8 × 60s sprints", 60, 45, 8), runDay(4, 3, "Wed", "8 × 60s sprints", 60, 45, 8), runDay(4, 5, "Fri", "8 × 60s sprints", 60, 45, 8))
        )
    )

    val all: List<TrainingPlan> = listOf(C25K, FIVEK_IMPROVER, TENK_BUILDER, HIIT_BLASTER)

    fun byKey(key: String): TrainingPlan? = all.firstOrNull { it.key == key }
}