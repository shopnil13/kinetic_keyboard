package com.kinetic.keyboard.giphy

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** P5.10: the GIPHY response shape we depend on, pinned without network. */
class GiphyClientTest {

    private val canned = """
        {
          "data": [
            {
              "type": "gif",
              "id": "abc123",
              "url": "https://giphy.com/gifs/abc123",
              "images": {
                "fixed_width": {
                  "url": "https://media.giphy.com/abc123/200w.gif",
                  "webp": "https://media.giphy.com/abc123/200w.webp",
                  "width": "200", "height": "150", "size": "12345"
                },
                "original": {
                  "url": "https://media.giphy.com/abc123/giphy.gif",
                  "width": "480", "height": "360"
                }
              },
              "unknown_future_field": {"nested": true}
            },
            {
              "type": "gif",
              "id": "no-images-entry",
              "images": {}
            }
          ],
          "pagination": {"total_count": 1000, "count": 2, "offset": 0},
          "meta": {"status": 200, "msg": "OK"}
        }
    """.trimIndent()

    @Test
    fun `parses items, prefers webp preview, keeps original for insertion`() {
        val items = GiphyClient.parse(canned)
        assertEquals(1, items.size) // the images-less entry is dropped, unknown fields ignored
        val item = items.single()
        assertEquals("abc123", item.id)
        assertEquals("https://media.giphy.com/abc123/200w.webp", item.previewUrl)
        assertEquals("https://media.giphy.com/abc123/giphy.gif", item.gifUrl)
        assertEquals(200, item.previewWidth)
        assertEquals(150, item.previewHeight)
    }

    @Test
    fun `empty and blank bodies fail loudly, empty data parses to empty list`() {
        assertTrue(GiphyClient.parse("""{"data": []}""").isEmpty())
        assertTrue(GiphyClient.parse("""{}""").isEmpty())
    }

    @Test
    fun `no api key means hasKey false`() {
        assertTrue(!GiphyClient("").hasKey)
        assertTrue(GiphyClient("k").hasKey)
    }
}
