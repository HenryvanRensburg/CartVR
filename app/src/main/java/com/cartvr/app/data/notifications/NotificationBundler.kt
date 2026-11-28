package com.cartvr.app.data.notifications

import android.content.Context
import com.cartvr.app.data.preferences.AppPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Instant

enum class ItemChangeKind {
    ADDED,
    CHECKED
}

data class PendingListChange(
    val itemId: String,
    val listId: String,
    val listName: String,
    val itemName: String,
    val quantity: String?,
    val actorName: String,
    val kind: ItemChangeKind,
    val updatedAt: String
)

class NotificationBundler(
    private val context: Context,
    private val prefs: AppPreferences
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val mutex = Mutex()

    private val pendingByList = mutableMapOf<String, LinkedHashMap<String, PendingListChange>>()
    private val debounceJobs = mutableMapOf<String, Job>()
    private val seenEventKeys = LinkedHashSet<String>()

    suspend fun markKeySeen(key: String): Boolean {
        return mutex.withLock {
            if (!seenEventKeys.add(key)) return@withLock false
            while (seenEventKeys.size > MAX_SEEN_KEYS) {
                val first = seenEventKeys.iterator().next()
                seenEventKeys.remove(first)
            }
            true
        }
    }

    suspend fun enqueueRealtimeChange(change: PendingListChange) {
        val listId = change.listId
        mutex.withLock {
            if (!markSeenLocked(change)) return

            val bucket = pendingByList.getOrPut(listId) { LinkedHashMap() }
            bucket[change.itemId] = change

            debounceJobs[listId]?.cancel()
            debounceJobs[listId] = scope.launch {
                delay(DEBOUNCE_MS)
                flushList(listId)
            }
        }
    }

    suspend fun postImmediateBatch(changes: List<PendingListChange>) {
        if (changes.isEmpty()) return
        val grouped = mutex.withLock {
            val filtered = changes.filter { markSeenLocked(it) }
            filtered.groupBy { it.listId }
        }
        var maxTimestamp: String? = null
        grouped.forEach { (listId, listChanges) ->
            // Cancel any pending realtime debounce for this list since we are posting now
            mutex.withLock {
                debounceJobs.remove(listId)?.cancel()
                pendingByList.remove(listId)
            }
            val latest = postForList(listId, listChanges)
            maxTimestamp = maxIso(maxTimestamp, latest)
        }
        advanceLastNotifiedAt(maxTimestamp)
    }

    private suspend fun flushList(listId: String) {
        val batch = mutex.withLock {
            debounceJobs.remove(listId)
            pendingByList.remove(listId)?.values?.toList().orEmpty()
        }
        if (batch.isEmpty()) return
        val maxTimestamp = postForList(listId, batch)
        advanceLastNotifiedAt(maxTimestamp)
    }

    private fun postForList(listId: String, changes: List<PendingListChange>): String? {
        if (changes.isEmpty()) return null
        val listName = changes.first().listName
        val actors = changes.map { it.actorName }.distinct()
        val actorLabel = when (actors.size) {
            0 -> "Family"
            1 -> actors.first()
            else -> "${actors.first()} & family"
        }

        val added = changes.filter { it.kind == ItemChangeKind.ADDED }
        val checked = changes.filter { it.kind == ItemChangeKind.CHECKED }

        val body = when {
            added.isNotEmpty() && checked.isEmpty() -> {
                if (added.size == 1) {
                    "$actorLabel added ${formatItem(added.first())}"
                } else {
                    "$actorLabel added ${added.size} items"
                }
            }
            checked.isNotEmpty() && added.isEmpty() -> {
                if (checked.size == 1) {
                    "$actorLabel ticked off ${formatItem(checked.first())}"
                } else {
                    "$actorLabel ticked off ${checked.size} items"
                }
            }
            else -> {
                "$actorLabel added ${added.size} and ticked off ${checked.size} items"
            }
        }

        val expandedLines = buildList {
            if (changes.size > 1) {
                added.take(6).forEach { add("+ ${formatItem(it)}") }
                val remainingSlots = (6 - size).coerceAtLeast(0)
                checked.take(remainingSlots).forEach { add("✓ ${formatItem(it)}") }
                if (changes.size > 6) {
                    add("…and ${changes.size - 6} more")
                }
            }
        }

        NotificationHelper.postListUpdateNotification(
            context = context,
            listId = listId,
            title = listName,
            body = body,
            expandedLines = expandedLines
        )

        return changes.map { it.updatedAt }.reduceOrNull { acc, ts -> maxIso(acc, ts) ?: acc }
    }

    private fun formatItem(change: PendingListChange): String {
        val qty = change.quantity?.trim()?.takeIf { it.isNotEmpty() }
        return if (qty != null) "${change.itemName} ($qty)" else change.itemName
    }

    private fun markSeenLocked(change: PendingListChange): Boolean {
        val key = "${change.itemId}:${change.updatedAt}"
        if (!seenEventKeys.add(key)) return false
        while (seenEventKeys.size > MAX_SEEN_KEYS) {
            val first = seenEventKeys.iterator().next()
            seenEventKeys.remove(first)
        }
        return true
    }

    suspend fun advanceLastNotifiedAt(candidateIso: String?) {
        if (candidateIso.isNullOrBlank()) return
        val current = prefs.lastNotifiedAt.first()
        val next = maxIso(current, candidateIso) ?: return
        if (next != current) {
            prefs.setLastNotifiedAt(next)
        }
    }

    private fun maxIso(a: String?, b: String?): String? {
        if (a.isNullOrBlank()) return b
        if (b.isNullOrBlank()) return a
        return runCatching {
            if (Instant.parse(b).isAfter(Instant.parse(a))) b else a
        }.getOrDefault(b)
    }

    companion object {
        private const val DEBOUNCE_MS = 15_000L
        private const val MAX_SEEN_KEYS = 200
    }
}
