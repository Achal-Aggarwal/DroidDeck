package com.droiddeck.launcher.runtime

import org.junit.Assert.assertEquals
import org.junit.Test

class AccountsMergeTest {
    private val own = "root:x:10123:10123:root:/root:/bin/bash"
    private val zero = "root:x:0:0:root:/root:/bin/bash"

    @Test fun systemUsersSurvive() {
        val existing = "root:x:0:0:root:/root:/bin/bash\ndaemon:x:1:1:daemon:/usr/sbin:/usr/sbin/nologin\n_apt:x:42:65534::/nonexistent:/usr/sbin/nologin\nmessagebus:x:100:102::/nonexistent:/usr/sbin/nologin\n"
        assertEquals("$own\n$zero\ndaemon:x:1:1:daemon:/usr/sbin:/usr/sbin/nologin\n_apt:x:42:65534::/nonexistent:/usr/sbin/nologin\nmessagebus:x:100:102::/nonexistent:/usr/sbin/nologin\n",
            LinuxRuntime.mergedAccounts(existing, 2, own, zero, 10123))
    }

    @Test fun anOldSingleLineFileIsReplacedOnce() {
        val merged = LinuxRuntime.mergedAccounts("$own\n", 2, own, zero, 10123)
        assertEquals("$own\n$zero\n", merged)
        assertEquals(merged, LinuxRuntime.mergedAccounts(merged, 2, own, zero, 10123))
    }

    @Test fun anotherAppUidIsDropped() {
        assertEquals("$own\n$zero\n", LinuxRuntime.mergedAccounts("root:x:10099:10099:root:/root:/bin/bash\n", 2, own, zero, 10123))
    }

    @Test fun noFileYet() {
        assertEquals("$own\n$zero\n", LinuxRuntime.mergedAccounts(null, 2, own, zero, 10123))
    }

    @Test fun groupsKeepMembers() {
        val existing = "root:x:0:\nsudo:x:27:\naudio:x:29:pulse\n"
        assertEquals("root:x:10123:\nroot:x:0:\nsudo:x:27:\naudio:x:29:pulse\n",
            LinuxRuntime.mergedAccounts(existing, 2, "root:x:10123:", "root:x:0:", 10123))
    }
}
