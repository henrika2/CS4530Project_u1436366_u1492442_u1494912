package com.example.paintify.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import junit.framework.Assert.assertEquals
import junit.framework.Assert.assertNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.io.IOException

class DrawingDaoBasicTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: DrawingDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries() // fine for tests
            .build()
        dao = db.drawingDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }


    @Test
    fun insert_then_read_then_delete() = runTest {
        val drawing = DrawingData(
            name = "TestDrawing",
            filePath = "/tmp/test.png",
            widthPx = 600,
            heightPx = 400
        )

        val id = dao.insert(drawing)

        val loaded = dao.selectDrawingById(id).first()
        requireNotNull(loaded)
        assertEquals("TestDrawing", loaded.name)
        assertEquals("/tmp/test.png", loaded.filePath)
        assertEquals(600, loaded.widthPx)
        assertEquals(400, loaded.heightPx)

        dao.delete(loaded)
        val afterDelete = dao.selectDrawingById(id).first()
        assertNull(afterDelete)
    }

    @Test
    fun update_existing_drawing() = runTest {
        val id = dao.insert(
            DrawingData(
                name = "BeforeUpdate",
                filePath = "/tmp/a.png",
                widthPx = 200,
                heightPx = 150
            )
        )

        val before = dao.selectDrawingById(id).first()!!
        val updated = before.copy(name = "AfterUpdate", widthPx = 300)

        dao.update(updated)

        val reloaded = dao.selectDrawingById(id).first()!!
        assertEquals("AfterUpdate", reloaded.name)
        assertEquals(300, reloaded.widthPx)
        assertEquals(150, reloaded.heightPx)
    }


    @Test
    fun all_drawings_returns_desc_order() = runTest {
        val early = DrawingData(
            name = "Oldest",
            filePath = "/tmp/1.png",
            widthPx = 100,
            heightPx = 100,
            createdAt = 1L
        )
        val middle = early.copy(name = "Middle", createdAt = 2L)
        val latest = early.copy(name = "Newest", createdAt = 3L)

        dao.insert(early)
        dao.insert(middle)
        dao.insert(latest)

        val list = dao.allDrawings().first()
        // Should be newest first
        assertEquals(list.map { it.name }, listOf("Newest", "Middle", "Oldest"))
    }


    @Test
    fun select_drawing_by_id_returns_correct_item() = runTest {
        val id1 = dao.insert(DrawingData(name = "A", filePath = "/a", widthPx = 10, heightPx = 10))
        val id2 = dao.insert(DrawingData(name = "B", filePath = "/b", widthPx = 20, heightPx = 20))

        val drawing1 = dao.selectDrawingById(id1).first()
        val drawing2 = dao.selectDrawingById(id2).first()
        val missing = dao.selectDrawingById(999L).first()

        requireNotNull(drawing1)
        requireNotNull(drawing2)
        assertEquals("A", drawing1.name)
        assertEquals("B", drawing2.name)
        assertNull(missing)
    }
}
