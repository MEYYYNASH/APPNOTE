package com.example.widget

import android.content.Context
import android.content.Intent
import android.graphics.Color as AndroidColor
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.example.MainActivity
import com.example.R
import com.example.data.NotesRepository
import com.example.data.local.AppDatabase
import com.example.data.local.toEntity
import com.example.data.local.toNote
import com.example.model.ChecklistItem
import com.example.model.Note
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class GlassNotesGlanceWidget(
    private val forcedDefaultType: WidgetType? = null
) : GlanceAppWidget() {

    override val stateDefinition: GlanceStateDefinition<*> = PreferencesGlanceStateDefinition

    companion object {
        val KEY_REVISION = intPreferencesKey("glance_revision")

        private val SMALL_SIZE = DpSize(115.dp, 105.dp)
        private val MEDIUM_SIZE = DpSize(240.dp, 110.dp)
        private val LARGE_SIZE = DpSize(240.dp, 210.dp)

        const val EXTRA_NOTE_ID = NotesAppWidgetProvider.EXTRA_NOTE_ID
        const val EXTRA_ACTION_CREATE_NOTE = NotesAppWidgetProvider.EXTRA_ACTION_CREATE_NOTE
        const val EXTRA_ACTION_QUICK_CAPTURE = NotesAppWidgetProvider.EXTRA_ACTION_QUICK_CAPTURE
        const val EXTRA_ACTION_QUICK_REMINDER = NotesAppWidgetProvider.EXTRA_ACTION_QUICK_REMINDER
    }

    override val sizeMode = SizeMode.Responsive(
        setOf(SMALL_SIZE, MEDIUM_SIZE, LARGE_SIZE)
    )

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val appWidgetId = try {
            GlanceAppWidgetManager(context).getAppWidgetId(id)
        } catch (_: Exception) {
            0
        }

        val config = withContext(Dispatchers.IO) {
            val loaded = WidgetDataStore.getConfigForWidget(context, appWidgetId)
            if (forcedDefaultType != null && appWidgetId > 0 && loaded.widgetType == WidgetType.NOTES_LIST && forcedDefaultType != WidgetType.NOTES_LIST) {
                loaded.copy(widgetType = forcedDefaultType)
            } else {
                loaded
            }
        }

        val notes = withContext(Dispatchers.IO) {
            val repo = NotesRepository(context)
            val dao = AppDatabase.getInstance(context).noteDao()
            val roomNotes = dao.getActiveNotesSync().map { it.toNote() }
            if (roomNotes.isNotEmpty()) {
                roomNotes
            } else {
                val repoNotes = repo.loadNotes().filter { !it.isTrashed && !it.isArchived }
                if (repoNotes.isNotEmpty()) {
                    dao.upsertAll(repoNotes.map { it.toEntity() })
                }
                repoNotes
            }
        }

        provideContent {
            currentState<Preferences>()[KEY_REVISION] ?: 0
            val size = LocalSize.current
            GlanceWidgetRoot(
                context = context,
                size = size,
                config = config,
                allNotes = notes
            )
        }
    }

    @Composable
    private fun GlanceWidgetRoot(
        context: Context,
        size: DpSize,
        config: WidgetConfig,
        allNotes: List<Note>
    ) {
        val isSmall = size.width < 200.dp && size.height < 170.dp
        val isMedium = !isSmall && size.height < 185.dp

        val effectiveType = when {
            isSmall -> if (config.widgetType == WidgetType.STICKY_NOTE) WidgetType.STICKY_NOTE else WidgetType.QUICK_NOTE
            isMedium && config.widgetType == WidgetType.NOTES_LIST -> WidgetType.CHECKLIST
            else -> config.widgetType
        }

        val bgColor = Color(config.resolveBackgroundArgb())
        val isLight = config.glassTheme.isLightSurface
        val primaryText = parseHexColor(config.textColorHex, if (isLight) Color(0xFF0F172A) else Color.White)
        val secondaryText = if (isLight) Color(0xFF475569) else Color(0xFFCBD5E1)
        val itemSurface = if (isLight) Color(0xCCFFFFFF) else Color(0x26FFFFFF)
        val pillSurface = Color(0xFF6366F1)
        val accentViolet = Color(0xFFA78BFA)

        val baseModifier = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            GlanceModifier
                .fillMaxSize()
                .appWidgetBackground()
                .cornerRadius(android.R.dimen.system_app_widget_background_radius)
                .background(bgColor)
                .padding(12.dp)
        } else {
            GlanceModifier
                .fillMaxSize()
                .appWidgetBackground()
                .cornerRadius(20.dp)
                .background(bgColor)
                .padding(12.dp)
        }

        val filteredNotes = if (config.filterFolder.isNotBlank()) {
            allNotes.filter { it.folder.equals(config.filterFolder, ignoreCase = true) }
        } else {
            allNotes
        }

        val now = System.currentTimeMillis()
        val upcomingReminder = allNotes
            .filter { it.reminderAt != null && it.reminderAt >= now - 60_000L }
            .minByOrNull { it.reminderAt ?: Long.MAX_VALUE }

        Box(modifier = baseModifier) {
            when (effectiveType) {
                WidgetType.QUICK_NOTE, WidgetType.STICKY_NOTE -> {
                    val targetNote = if (config.selectedNoteId.isNotBlank()) {
                        filteredNotes.find { it.id == config.selectedNoteId } ?: filteredNotes.firstOrNull()
                    } else {
                        filteredNotes.firstOrNull()
                    }
                    SmallQuickNoteContent(
                        context = context,
                        note = targetNote,
                        upcomingReminder = if (config.showUpcomingReminder) upcomingReminder else null,
                        primaryText = primaryText,
                        secondaryText = secondaryText,
                        pillSurface = pillSurface,
                        fontSizeSp = config.textSizeSp
                    )
                }

                WidgetType.CHECKLIST -> {
                    val checklistNote = if (config.selectedNoteId.isNotBlank()) {
                        filteredNotes.find { it.id == config.selectedNoteId && it.isChecklist }
                            ?: filteredNotes.firstOrNull { it.isChecklist }
                            ?: filteredNotes.firstOrNull()
                    } else {
                        filteredNotes.firstOrNull { it.isChecklist } ?: filteredNotes.firstOrNull()
                    }
                    ChecklistWidgetContent(
                        context = context,
                        note = checklistNote,
                        upcomingReminder = if (config.showUpcomingReminder) upcomingReminder else null,
                        primaryText = primaryText,
                        secondaryText = secondaryText,
                        itemSurface = itemSurface,
                        pillSurface = pillSurface,
                        accentViolet = accentViolet,
                        fontSizeSp = config.textSizeSp
                    )
                }

                WidgetType.UPCOMING_REMINDER -> {
                    UpcomingRemindersWidgetContent(
                        context = context,
                        notesWithReminders = allNotes.filter { it.reminderAt != null }.sortedBy { it.reminderAt },
                        primaryText = primaryText,
                        secondaryText = secondaryText,
                        itemSurface = itemSurface,
                        pillSurface = pillSurface,
                        fontSizeSp = config.textSizeSp
                    )
                }

                WidgetType.NOTES_LIST -> {
                    LargeNotesListContent(
                        context = context,
                        notes = filteredNotes,
                        folderLabel = config.filterFolder,
                        upcomingReminder = if (config.showUpcomingReminder) upcomingReminder else null,
                        primaryText = primaryText,
                        secondaryText = secondaryText,
                        itemSurface = itemSurface,
                        pillSurface = pillSurface,
                        fontSizeSp = config.textSizeSp
                    )
                }
            }
        }
    }

    @Composable
    private fun SmallQuickNoteContent(
        context: Context,
        note: Note?,
        upcomingReminder: Note?,
        primaryText: Color,
        secondaryText: Color,
        pillSurface: Color,
        fontSizeSp: Int
    ) {
        val openNoteIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            if (note != null) putExtra(EXTRA_NOTE_ID, note.id)
            else putExtra(EXTRA_ACTION_QUICK_CAPTURE, true)
        }
        val createNoteIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_ACTION_CREATE_NOTE, true)
        }
        val reminderIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_ACTION_QUICK_REMINDER, true)
        }

        Column(
            modifier = GlanceModifier.fillMaxSize()
        ) {
            Row(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .clickable(actionStartActivity(openNoteIntent)),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = note?.title?.ifBlank { "Untitled" } ?: "GlassNotes",
                    style = TextStyle(
                        color = ColorProvider(primaryText),
                        fontSize = (fontSizeSp + 1).sp,
                        fontWeight = FontWeight.Bold
                    ),
                    maxLines = 1,
                    modifier = GlanceModifier.defaultWeight()
                )
            }
            Spacer(modifier = GlanceModifier.height(4.dp))
            Text(
                text = if (note != null) {
                    if (note.isChecklist) {
                        val done = note.checklist.count { it.isDone }
                        "$done/${note.checklist.size} tasks done"
                    } else {
                        note.body.replace("\n", " ").ifBlank { "Tap to edit note..." }
                    }
                } else {
                    "Tap + to write a quick note"
                },
                style = TextStyle(
                    color = ColorProvider(secondaryText),
                    fontSize = (fontSizeSp - 1).coerceAtLeast(10).sp
                ),
                maxLines = 2,
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .defaultWeight()
                    .clickable(actionStartActivity(openNoteIntent))
            )
            if (upcomingReminder?.reminderAt != null) {
                val fmt = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(upcomingReminder.reminderAt))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        provider = ImageProvider(R.drawable.ic_widget_bell),
                        contentDescription = "Reminder",
                        colorFilter = ColorFilter.tint(ColorProvider(Color(0xFFF472B6))),
                        modifier = GlanceModifier.size(12.dp)
                    )
                    Spacer(modifier = GlanceModifier.width(4.dp))
                    Text(
                        text = fmt,
                        style = TextStyle(
                            color = ColorProvider(Color(0xFFF472B6)),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        maxLines = 1
                    )
                }
                Spacer(modifier = GlanceModifier.height(4.dp))
            }
            Row(modifier = GlanceModifier.fillMaxWidth()) {
                Box(
                    modifier = GlanceModifier
                        .defaultWeight()
                        .height(30.dp)
                        .cornerRadius(15.dp)
                        .background(pillSurface)
                        .clickable(actionStartActivity(createNoteIntent)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "+ Note",
                        style = TextStyle(
                            color = ColorProvider(Color.White),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
                Spacer(modifier = GlanceModifier.width(6.dp))
                Box(
                    modifier = GlanceModifier
                        .width(34.dp)
                        .height(30.dp)
                        .cornerRadius(15.dp)
                        .background(Color(0x33FFFFFF))
                        .clickable(actionStartActivity(reminderIntent)),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        provider = ImageProvider(R.drawable.ic_widget_bell),
                        contentDescription = "Quick Reminder",
                        colorFilter = ColorFilter.tint(ColorProvider(primaryText)),
                        modifier = GlanceModifier.size(14.dp)
                    )
                }
            }
        }
    }

    @Composable
    private fun ChecklistWidgetContent(
        context: Context,
        note: Note?,
        upcomingReminder: Note?,
        primaryText: Color,
        secondaryText: Color,
        itemSurface: Color,
        pillSurface: Color,
        accentViolet: Color,
        fontSizeSp: Int
    ) {
        val openNoteIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            if (note != null) putExtra(EXTRA_NOTE_ID, note.id)
            else putExtra(EXTRA_ACTION_CREATE_NOTE, true)
        }
        val createIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_ACTION_CREATE_NOTE, true)
        }

        Column(modifier = GlanceModifier.fillMaxSize()) {
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = GlanceModifier
                        .defaultWeight()
                        .clickable(actionStartActivity(openNoteIntent))
                ) {
                    Text(
                        text = note?.title?.ifBlank { "Checklist" } ?: "Checklist",
                        style = TextStyle(
                            color = ColorProvider(primaryText),
                            fontSize = (fontSizeSp + 1).sp,
                            fontWeight = FontWeight.Bold
                        ),
                        maxLines = 1
                    )
                }
                if (note != null && note.isChecklist) {
                    val done = note.checklist.count { it.isDone }
                    Text(
                        text = "$done/${note.checklist.size}",
                        style = TextStyle(
                            color = ColorProvider(accentViolet),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Spacer(modifier = GlanceModifier.width(8.dp))
                }
                Box(
                    modifier = GlanceModifier
                        .width(28.dp)
                        .height(26.dp)
                        .cornerRadius(13.dp)
                        .background(pillSurface)
                        .clickable(actionStartActivity(createIntent)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "+",
                        style = TextStyle(
                            color = ColorProvider(Color.White),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
            Spacer(modifier = GlanceModifier.height(6.dp))

            val items: List<ChecklistItem> = note?.checklist.orEmpty()
            if (items.isEmpty()) {
                Box(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .cornerRadius(12.dp)
                        .background(itemSurface)
                        .clickable(actionStartActivity(openNoteIntent))
                        .padding(10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = note?.body?.take(120)?.ifBlank { "Tap to add tasks..." } ?: "Tap + to create a checklist",
                        style = TextStyle(color = ColorProvider(secondaryText), fontSize = fontSizeSp.sp),
                        maxLines = 3
                    )
                }
            } else {
                LazyColumn(modifier = GlanceModifier.fillMaxSize()) {
                    items(
                        items = items,
                        itemId = { item -> item.id.hashCode().toLong() }
                    ) { item ->
                        Column {
                            Row(
                                modifier = GlanceModifier
                                    .fillMaxWidth()
                                    .cornerRadius(10.dp)
                                    .background(itemSurface)
                                    .clickable(
                                        actionRunCallback<ToggleChecklistAction>(
                                            actionParametersOf(
                                                ToggleChecklistAction.KEY_NOTE_ID to (note?.id ?: ""),
                                                ToggleChecklistAction.KEY_ITEM_ID to item.id
                                            )
                                        )
                                    )
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Image(
                                    provider = ImageProvider(
                                        if (item.isDone) R.drawable.ic_widget_check else R.drawable.ic_widget_circle
                                    ),
                                    contentDescription = if (item.isDone) "Done" else "Pending",
                                    colorFilter = ColorFilter.tint(
                                        ColorProvider(if (item.isDone) Color(0xFF34D399) else accentViolet)
                                    ),
                                    modifier = GlanceModifier.size(16.dp)
                                )
                                Spacer(modifier = GlanceModifier.width(8.dp))
                                Text(
                                    text = item.text.ifBlank { "Task" },
                                    style = TextStyle(
                                        color = ColorProvider(if (item.isDone) secondaryText else primaryText),
                                        fontSize = fontSizeSp.sp
                                    ),
                                    maxLines = 1
                                )
                            }
                            Spacer(modifier = GlanceModifier.height(4.dp))
                        }
                    }
                }
            }
            if (upcomingReminder?.reminderAt != null) {
                val fmt = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(upcomingReminder.reminderAt))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        provider = ImageProvider(R.drawable.ic_widget_bell),
                        contentDescription = "Reminder",
                        colorFilter = ColorFilter.tint(ColorProvider(Color(0xFFF472B6))),
                        modifier = GlanceModifier.size(11.dp)
                    )
                    Spacer(modifier = GlanceModifier.width(4.dp))
                    Text(
                        text = "${upcomingReminder.title.ifBlank { "Reminder" }} • $fmt",
                        style = TextStyle(color = ColorProvider(Color(0xFFF472B6)), fontSize = 10.sp),
                        maxLines = 1
                    )
                }
            }
        }
    }

    @Composable
    private fun UpcomingRemindersWidgetContent(
        context: Context,
        notesWithReminders: List<Note>,
        primaryText: Color,
        secondaryText: Color,
        itemSurface: Color,
        pillSurface: Color,
        fontSizeSp: Int
    ) {
        val addReminderIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_ACTION_QUICK_REMINDER, true)
        }
        val fmt = SimpleDateFormat("MMM d • h:mm a", Locale.getDefault())

        Column(modifier = GlanceModifier.fillMaxSize()) {
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = GlanceModifier.defaultWeight(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        provider = ImageProvider(R.drawable.ic_widget_bell),
                        contentDescription = "Reminders",
                        colorFilter = ColorFilter.tint(ColorProvider(Color(0xFFF472B6))),
                        modifier = GlanceModifier.size(16.dp)
                    )
                    Spacer(modifier = GlanceModifier.width(6.dp))
                    Text(
                        text = "Reminders",
                        style = TextStyle(
                            color = ColorProvider(primaryText),
                            fontSize = (fontSizeSp + 1).sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
                Box(
                    modifier = GlanceModifier
                        .height(26.dp)
                        .cornerRadius(13.dp)
                        .background(pillSurface)
                        .clickable(actionStartActivity(addReminderIntent))
                        .padding(horizontal = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "+ Remind",
                        style = TextStyle(color = ColorProvider(Color.White), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    )
                }
            }
            Spacer(modifier = GlanceModifier.height(6.dp))
            if (notesWithReminders.isEmpty()) {
                Box(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .cornerRadius(12.dp)
                        .background(itemSurface)
                        .clickable(actionStartActivity(addReminderIntent)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No upcoming reminders. Tap + Remind.",
                        style = TextStyle(color = ColorProvider(secondaryText), fontSize = fontSizeSp.sp)
                    )
                }
            } else {
                LazyColumn(modifier = GlanceModifier.fillMaxSize()) {
                    items(
                        items = notesWithReminders,
                        itemId = { n -> n.id.hashCode().toLong() }
                    ) { note ->
                        val openIntent = Intent(context, MainActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                            putExtra(EXTRA_NOTE_ID, note.id)
                        }
                        Column {
                            Row(
                                modifier = GlanceModifier
                                    .fillMaxWidth()
                                    .cornerRadius(10.dp)
                                    .background(itemSurface)
                                    .clickable(actionStartActivity(openIntent))
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = note.title.ifBlank { "Untitled" },
                                    style = TextStyle(
                                        color = ColorProvider(primaryText),
                                        fontSize = fontSizeSp.sp,
                                        fontWeight = FontWeight.Medium
                                    ),
                                    maxLines = 1,
                                    modifier = GlanceModifier.defaultWeight()
                                )
                                Text(
                                    text = fmt.format(Date(note.reminderAt ?: System.currentTimeMillis())),
                                    style = TextStyle(color = ColorProvider(Color(0xFFF472B6)), fontSize = 10.sp)
                                )
                            }
                            Spacer(modifier = GlanceModifier.height(4.dp))
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun LargeNotesListContent(
        context: Context,
        notes: List<Note>,
        folderLabel: String,
        upcomingReminder: Note?,
        primaryText: Color,
        secondaryText: Color,
        itemSurface: Color,
        pillSurface: Color,
        fontSizeSp: Int
    ) {
        val createNoteIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_ACTION_CREATE_NOTE, true)
        }
        val reminderIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_ACTION_QUICK_REMINDER, true)
        }
        val dateFmt = SimpleDateFormat("MMM d", Locale.getDefault())

        Column(modifier = GlanceModifier.fillMaxSize()) {
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (folderLabel.isNotBlank()) folderLabel else "GlassNotes",
                    style = TextStyle(
                        color = ColorProvider(primaryText),
                        fontSize = (fontSizeSp + 2).sp,
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = GlanceModifier.defaultWeight(),
                    maxLines = 1
                )
                Box(
                    modifier = GlanceModifier
                        .height(28.dp)
                        .cornerRadius(14.dp)
                        .background(Color(0x33FFFFFF))
                        .clickable(actionStartActivity(reminderIntent))
                        .padding(horizontal = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        provider = ImageProvider(R.drawable.ic_widget_bell),
                        contentDescription = "Quick Reminder",
                        colorFilter = ColorFilter.tint(ColorProvider(primaryText)),
                        modifier = GlanceModifier.size(14.dp)
                    )
                }
                Spacer(modifier = GlanceModifier.width(6.dp))
                Box(
                    modifier = GlanceModifier
                        .height(28.dp)
                        .cornerRadius(14.dp)
                        .background(pillSurface)
                        .clickable(actionStartActivity(createNoteIntent))
                        .padding(horizontal = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "+ Note",
                        style = TextStyle(
                            color = ColorProvider(Color.White),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }

            if (upcomingReminder?.reminderAt != null) {
                Spacer(modifier = GlanceModifier.height(6.dp))
                val remTime = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
                    .format(Date(upcomingReminder.reminderAt))
                val remIntent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    putExtra(EXTRA_NOTE_ID, upcomingReminder.id)
                }
                Row(
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .cornerRadius(10.dp)
                        .background(Color(0x33EC4899))
                        .clickable(actionStartActivity(remIntent))
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        provider = ImageProvider(R.drawable.ic_widget_bell),
                        contentDescription = "Upcoming Reminder",
                        colorFilter = ColorFilter.tint(ColorProvider(Color(0xFFF9A8D4))),
                        modifier = GlanceModifier.size(13.dp)
                    )
                    Spacer(modifier = GlanceModifier.width(6.dp))
                    Text(
                        text = upcomingReminder.title.ifBlank { "Upcoming Reminder" },
                        style = TextStyle(
                            color = ColorProvider(primaryText),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        maxLines = 1,
                        modifier = GlanceModifier.defaultWeight()
                    )
                    Text(
                        text = remTime,
                        style = TextStyle(color = ColorProvider(Color(0xFFF9A8D4)), fontSize = 10.sp)
                    )
                }
            }

            Spacer(modifier = GlanceModifier.height(8.dp))

            if (notes.isEmpty()) {
                Box(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .cornerRadius(14.dp)
                        .background(itemSurface)
                        .clickable(actionStartActivity(createNoteIntent)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No notes yet. Tap + Note to start.",
                        style = TextStyle(color = ColorProvider(secondaryText), fontSize = fontSizeSp.sp)
                    )
                }
            } else {
                LazyColumn(modifier = GlanceModifier.fillMaxSize()) {
                    items(
                        items = notes.take(12),
                        itemId = { note -> note.id.hashCode().toLong() }
                    ) { note ->
                        val openIntent = Intent(context, MainActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                            putExtra(EXTRA_NOTE_ID, note.id)
                        }
                        val preview = if (note.isChecklist) {
                            val done = note.checklist.count { it.isDone }
                            "$done of ${note.checklist.size} done"
                        } else {
                            note.body.replace("\n", " ").take(70).ifBlank { "Tap to write..." }
                        }
                        Column {
                            Column(
                                modifier = GlanceModifier
                                    .fillMaxWidth()
                                    .cornerRadius(12.dp)
                                    .background(itemSurface)
                                    .clickable(actionStartActivity(openIntent))
                                    .padding(horizontal = 10.dp, vertical = 8.dp)
                            ) {
                                Row(
                                    modifier = GlanceModifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = note.title.ifBlank { "Untitled" },
                                        style = TextStyle(
                                            color = ColorProvider(primaryText),
                                            fontSize = fontSizeSp.sp,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        maxLines = 1,
                                        modifier = GlanceModifier.defaultWeight()
                                    )
                                    Text(
                                        text = dateFmt.format(Date(note.updatedAt)),
                                        style = TextStyle(
                                            color = ColorProvider(secondaryText),
                                            fontSize = 10.sp
                                        )
                                    )
                                }
                                Spacer(modifier = GlanceModifier.height(2.dp))
                                Text(
                                    text = preview,
                                    style = TextStyle(
                                        color = ColorProvider(secondaryText),
                                        fontSize = (fontSizeSp - 2).coerceAtLeast(10).sp
                                    ),
                                    maxLines = 1
                                )
                            }
                            Spacer(modifier = GlanceModifier.height(6.dp))
                        }
                    }
                }
            }
        }
    }

    private fun parseHexColor(hex: String, fallback: Color): Color {
        return try {
            Color(AndroidColor.parseColor(hex))
        } catch (_: Exception) {
            fallback
        }
    }
}

class ToggleChecklistAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val noteId = parameters[KEY_NOTE_ID] ?: return
        val itemId = parameters[KEY_ITEM_ID] ?: return
        if (noteId.isBlank() || itemId.isBlank()) return

        withContext(Dispatchers.IO) {
            val repo = NotesRepository(context)
            val updated = repo.loadNotes().map { note ->
                if (note.id == noteId) {
                    val toggled = note.checklist.map { item ->
                        if (item.id == itemId) item.copy(isDone = !item.isDone) else item
                    }
                    note.copy(checklist = toggled, updatedAt = System.currentTimeMillis())
                } else {
                    note
                }
            }
            repo.saveNotes(updated)
            val dao = AppDatabase.getInstance(context).noteDao()
            dao.replaceAll(updated.map { it.toEntity() })
            pushGlanceRevisionUpdate(context)
        }
    }

    companion object {
        val KEY_NOTE_ID = ActionParameters.Key<String>("note_id")
        val KEY_ITEM_ID = ActionParameters.Key<String>("item_id")
    }
}

suspend fun pushGlanceRevisionUpdate(context: Context) {
    val widgets = listOf(
        GlassNotesGlanceWidget(WidgetType.QUICK_NOTE),
        GlassNotesGlanceWidget(WidgetType.CHECKLIST),
        GlassNotesGlanceWidget(WidgetType.NOTES_LIST)
    )
    val manager = GlanceAppWidgetManager(context)
    for (w in widgets) {
        try {
            val ids = manager.getGlanceIds(w::class.java)
            for (gid in ids) {
                updateAppWidgetState(context, gid) { prefs ->
                    prefs[GlassNotesGlanceWidget.KEY_REVISION] =
                        (prefs[GlassNotesGlanceWidget.KEY_REVISION] ?: 0) + 1
                }
            }
            w.updateAll(context)
        } catch (_: Exception) {
        }
    }
    NotesAppWidgetProvider.updateAllWidgets(context)
}

class QuickNoteGlanceReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = GlassNotesGlanceWidget(WidgetType.QUICK_NOTE)
}

class ChecklistGlanceReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = GlassNotesGlanceWidget(WidgetType.CHECKLIST)
}

class NotesBoardGlanceReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = GlassNotesGlanceWidget(WidgetType.NOTES_LIST)
}
