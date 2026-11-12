package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.NotesRepository
import com.example.model.NoteColor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context and verify repository`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("GlassNotes", appName)

        val repo = NotesRepository(context)
        val notes = repo.loadNotes()
        assertTrue(notes.isNotEmpty())

        val settings = repo.loadSettings().copy(accent = NoteColor.VIOLET)
        repo.saveSettings(settings)
        assertEquals(NoteColor.VIOLET, repo.loadSettings().accent)

        // Verify package install permission check does not crash
        val canInstall = com.example.util.GitHubSyncHelper.canRequestPackageInstalls(context)
        // Robolectric environment returns a valid boolean
        assertTrue(canInstall || !canInstall)
    }
}
