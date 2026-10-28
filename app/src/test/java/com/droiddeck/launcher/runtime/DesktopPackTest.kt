package com.droiddeck.launcher.runtime

import com.droiddeck.launcher.core.FileUtils
import com.droiddeck.launcher.core.Hashes
import java.io.File
import java.nio.file.Files
import java.util.zip.GZIPOutputStream
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DesktopPackTest {
    private val parent = Files.createTempDirectory("pack").toFile()
    private val root = File(parent, "root").apply { mkdirs() }

    @After fun tearDown() { FileUtils.delete(parent) }

    private fun file(path: String, text: String) = File(root, path).apply { parentFile?.mkdirs(); writeText(text) }

    private fun line(kind: Char, path: String): String {
        val file = File(root, path)
        return when {
            Files.isSymbolicLink(file.toPath()) -> "$kind\tL\t0\t${Files.readSymbolicLink(file.toPath())}\t$path"
            file.isDirectory -> "$kind\tD\t0\t-\t$path"
            else -> "$kind\tF\t${file.length()}\t${Hashes.sha256(file)}\t$path"
        }
    }

    private fun manifest(lines: List<String>) {
        val target = File(root, "${DesktopPack.META}/files.tsv.gz").apply { parentFile?.mkdirs() }
        GZIPOutputStream(target.outputStream()).bufferedWriter().use { w -> lines.forEach { w.write(it); w.write("\n") } }
    }

    private fun installed(): List<String> {
        File(root, DesktopPack.INFO).apply { parentFile?.mkdirs() }
            .writeText("""{"tag":"ubuntu-r1-desktop","url":"https://example.invalid/d.tar.zst","sha256":"00","size":5}""")
        file("usr/bin/runtime-tool", "runtime")
        file("${DesktopPack.META}/originals/var/lib/dpkg/status", "Package: bash\n")
        file("${DesktopPack.META}/originals/etc/gone.conf", "removed by the desktop")
        file("${DesktopPack.META}/originals/etc/group", "root:x:0:")
        file("${DesktopPack.META}/originals/etc/hosts", "runtime hosts")
        file("etc/group", "root:x:0:\nsddm:x:120:")
        file("etc/hosts", "desktop hosts")
        file("var/lib/dpkg/status", "Package: bash\n\nPackage: kwin\n")
        file("usr/bin/kwin_wayland", "kwin")
        file("usr/share/kwin/effect.qml", "effect")
        file("usr/share/kwin/edited.qml", "original")
        Files.createSymbolicLink(File(root, "usr/bin/kwin").toPath(), File("kwin_wayland").toPath())
        val lines = listOf(
            line('N', "usr/share/kwin"), line('N', "usr/bin/kwin_wayland"), line('N', "usr/share/kwin/effect.qml"),
            line('N', "usr/share/kwin/edited.qml"), line('N', "usr/bin/kwin"), line('C', "var/lib/dpkg/status"), line('C', "etc/group"), line('C', "etc/hosts"),
            "R\tF\t22\tx\tetc/gone.conf", "N\tF\t1\tx\t../escape",
        )
        file("usr/share/kwin/edited.qml", "changed by the user")
        file("etc/hosts", "edited by the user")
        file("var/lib/dpkg/status", "Package: bash\n\nPackage: kwin\n\nPackage: vlc\n")
        return lines
    }

    @Test fun readsThePackTheRuntimeNames() {
        installed()
        val info = DesktopPack.info(root)!!
        assertEquals("ubuntu-r1-desktop", info.tag)
        assertEquals(5L, info.size)
    }

    @Test fun removingPutsTheRuntimeBackAndKeepsWhatTheUserChanged() {
        manifest(installed())
        val result = DesktopPack.remove(root)
        assertEquals("Package: bash\n", File(root, "var/lib/dpkg/status").readText())
        assertEquals("removed by the desktop", File(root, "etc/gone.conf").readText())
        assertEquals("root:x:0:", File(root, "etc/group").readText())
        assertEquals("edited by the user", File(root, "etc/hosts").readText())
        assertFalse(File(root, "usr/bin/kwin_wayland").exists())
        assertFalse(Files.exists(File(root, "usr/bin/kwin").toPath(), java.nio.file.LinkOption.NOFOLLOW_LINKS))
        assertFalse(File(root, "usr/share/kwin/effect.qml").exists())
        assertEquals("changed by the user", File(root, "usr/share/kwin/edited.qml").readText())
        assertTrue(File(root, "usr/share/kwin").isDirectory)
        assertEquals("runtime", File(root, "usr/bin/runtime-tool").readText())
        assertFalse(File(root, DesktopPack.META).exists())
        assertFalse(DesktopPack.installed(root))
        assertEquals(3, result.removed)
        assertEquals(3, result.restored)
        assertEquals(3, result.kept)
    }

    @Test fun emptiedDirectoriesGoAway() {
        val lines = installed()
        File(root, "usr/share/kwin/edited.qml").delete()
        manifest(lines)
        DesktopPack.remove(root)
        assertFalse(File(root, "usr/share/kwin").exists())
        assertTrue(File(root, "usr/share").isDirectory)
    }

    @Test fun finishingAnInstallDeletesWhatTheDesktopRemoved() {
        file("etc/gone.conf", "runtime copy")
        manifest(listOf("R\tF\t22\tx\tetc/gone.conf", "R\tF\t1\tx\t../outside"))
        File(root.parentFile, "outside").writeText("keep")
        DesktopPack.finishInstall(root)
        assertFalse(File(root, "etc/gone.conf").exists())
        assertTrue(File(root.parentFile, "outside").exists())
        File(root.parentFile, "outside").delete()
    }

    @Test fun pathsMayNotLeaveTheRuntime() {
        assertNull(DesktopPack.inside(root, "../x"))
        assertNull(DesktopPack.inside(root, "/etc/passwd"))
        Files.createSymbolicLink(File(root, "link").toPath(), root.parentFile.toPath())
        assertNull(DesktopPack.inside(root, "link/x"))
        assertEquals(File(root, "usr/x"), DesktopPack.inside(root, "usr/./x"))
    }
}
