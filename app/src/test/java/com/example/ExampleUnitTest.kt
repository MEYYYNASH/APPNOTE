package com.example

import com.example.util.GitHubSyncHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun `version comparison handles semantic version tags`() {
        // v1.1.15 is equal to 1.1.15
        assertEquals(0, GitHubSyncHelper.compareVersions("v1.1.15", "1.1.15"))
        // v1.1.15 is greater than v1.1.14
        assertTrue(GitHubSyncHelper.compareVersions("v1.1.15", "v1.1.14") > 0)
        // v1.1.14 is less than v1.1.15
        assertTrue(GitHubSyncHelper.compareVersions("v1.1.14", "v1.1.15") < 0)
        // Major version bumps
        assertTrue(GitHubSyncHelper.compareVersions("v2.0.0", "v1.9.9") > 0)
    }

    @Test
    fun `deriveVersionCodeFromTag derives consistent long numbers`() {
        assertEquals(10115L, GitHubSyncHelper.deriveVersionCodeFromTag("v1.1.15"))
        assertEquals(10114L, GitHubSyncHelper.deriveVersionCodeFromTag("1.1.14"))
        assertEquals(20001L, GitHubSyncHelper.deriveVersionCodeFromTag("v2.0.1"))
    }

    @Test
    fun `extractSha256FromText correctly parses 64-char hex digests`() {
        val sample = "APK Asset: app-release.apk, SHA-256: 561fd6d017ebc71d56f39263671d4d0719fc94e3ae0c0d6552ed74e3fc1222c9"
        val extracted = GitHubSyncHelper.extractSha256FromText(sample)
        assertEquals("561fd6d017ebc71d56f39263671d4d0719fc94e3ae0c0d6552ed74e3fc1222c9", extracted)
    }

    @Test
    fun `parseAtomFeed extracts release entries accurately`() {
        val sampleAtom = """
            <?xml version="1.0" encoding="UTF-8"?>
            <feed xmlns="http://www.w3.org/2005/Atom">
              <entry>
                <id>tag:github.com,2008:Repository/1454697625/v1.1.15</id>
                <updated>2026-11-12T04:51:49Z</updated>
                <link rel="alternate" type="text/html" href="https://github.com/MEYYYNASH/APPNOTE/releases/tag/v1.1.15"/>
                <title>APPNOTE v1.1.15</title>
                <content type="html">&lt;h2&gt;APPNOTE v1.1.15&lt;/h2&gt;&lt;p&gt;Version Code: 10115&lt;/p&gt;</content>
              </entry>
              <entry>
                <id>tag:github.com,2008:Repository/1454697625/v1.1.14</id>
                <updated>2026-11-12T04:34:57Z</updated>
                <link rel="alternate" type="text/html" href="https://github.com/MEYYYNASH/APPNOTE/releases/tag/v1.1.14"/>
                <title>APPNOTE v1.1.14</title>
                <content type="html">&lt;p&gt;Old version&lt;/p&gt;</content>
              </entry>
            </feed>
        """.trimIndent()

        val entries = GitHubSyncHelper.parseAtomFeed(sampleAtom)
        assertEquals(2, entries.size)
        assertEquals("v1.1.15", entries[0].tag)
        assertEquals("APPNOTE v1.1.15", entries[0].title)
        assertEquals("2026-11-12T04:51:49Z", entries[0].updated)
        assertTrue(entries[0].contentHtml.contains("Version Code: 10115"))
    }

    @Test
    fun `isTrustedHttpsUrl validates GitHub domains while rejecting untrusted`() {
        assertTrue(GitHubSyncHelper.isTrustedHttpsUrl("https://github.com/MEYYYNASH/APPNOTE/releases/download/v1.1.15/app-release.apk"))
        assertTrue(GitHubSyncHelper.isTrustedHttpsUrl("https://release-assets.githubusercontent.com/asset-id"))
        assertTrue(GitHubSyncHelper.isTrustedHttpsUrl("https://objects.githubusercontent.com/download"))
        assertFalse(GitHubSyncHelper.isTrustedHttpsUrl("http://insecure-site.com/app.apk"))
        assertFalse(GitHubSyncHelper.isTrustedHttpsUrl("https://malicious-domain.com/app.apk"))
    }
}
