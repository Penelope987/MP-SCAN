package online.mpscan.app.data

import androidx.work.Data
import org.junit.Assert.*
import org.junit.Test

class DownloadInputTest {
    private val work = Work("work-test", "Título".repeat(3000), "", "data:image/png;base64," + "A".repeat(20000),
        "", "", "", "", emptyList(), 0, 0)

    @Test fun oldInlineCoverExceedsWorkerLimit() {
        assertThrows(IllegalStateException::class.java) {
            Data.Builder().putString(ChapterDownloadWorker.WORK_COVER, work.cover).build()
        }
    }

    @Test fun largeCoverAndTitleCanQueueSingleChapter() {
        val chapter = Chapter("chapter-test", 12.0, "Capítulo".repeat(3000), true, 0)
        val input = ChapterDownloadWorker.requestData(work, chapter)
        assertEquals("work-test", input.getString(ChapterDownloadWorker.WORK_ID))
        assertEquals("chapter-test", input.getString(ChapterDownloadWorker.CHAPTER_ID))
        assertEquals(12.0, input.getDouble(ChapterDownloadWorker.CHAPTER_NUMBER, 0.0), 0.0)
        assertNull(input.getString(ChapterDownloadWorker.WORK_COVER))
    }

    @Test fun largeCoverAndTitleCanQueueWholeWork() {
        val input = ChapterDownloadWorker.requestData(work, null)
        assertTrue(input.getBoolean(ChapterDownloadWorker.DOWNLOAD_ALL, false))
        assertEquals("work-test", input.getString(ChapterDownloadWorker.WORK_ID))
        assertNull(input.getString(ChapterDownloadWorker.WORK_COVER))
    }
}
