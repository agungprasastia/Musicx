package com.example.musicx.catalog.youtube

import com.example.musicx.core.model.CatalogError
import com.example.musicx.core.model.CatalogFilter
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.IOException

class InnerTubeYouTubeMusicClientTest {
    private class RecordingTransport(
        private val result: Result<String>,
    ) : InnerTubeTransport {
        var calls = 0
        var endpoint: String? = null
        var body: String? = null

        override suspend fun post(endpoint: String, jsonBody: String): Result<String> {
            calls++
            this.endpoint = endpoint
            body = jsonBody
            return result
        }
    }

    @Test
    fun blankQueryFailsBeforeTransportCall() = runBlocking {
        val transport = RecordingTransport(Result.success("{}"))

        val result = InnerTubeYouTubeMusicClient(transport).search("   ", CatalogFilter.ALL)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is CatalogError.EmptyQuery)
        assertEquals(0, transport.calls)
    }

    @Test
    fun blankBrowseIdFailsBeforeTransportCall() = runBlocking {
        val transport = RecordingTransport(Result.success("{}"))

        val result = InnerTubeYouTubeMusicClient(transport).browse(" ")

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is CatalogError.EmptyBrowseId)
        assertEquals(0, transport.calls)
    }

    @Test
    fun searchBuildsGuestWebRemixRequestAndAppliesFilter() = runBlocking {
        val transport = RecordingTransport(Result.success(SEARCH_RESPONSE))

        val result = InnerTubeYouTubeMusicClient(transport)
            .search("midnight", CatalogFilter.SONGS)
            .getOrThrow()

        assertEquals("search", transport.endpoint)
        assertEquals(1, result.size)
        assertTrue(result[0] is CatalogDto.Track)

        val body = Json.parseToJsonElement(transport.body!!).jsonObject
        assertEquals("midnight", body["query"]!!.jsonPrimitive.content)
        val client = body["context"]!!.jsonObject["client"]!!.jsonObject
        assertEquals("WEB_REMIX", client["clientName"]!!.jsonPrimitive.content)
        assertEquals("EgWKAQIIAWoKEAkQChAFEAMQBA==", body["params"]!!.jsonPrimitive.content)
    }

    @Test
    fun allFilterOmitsSearchParams() = runBlocking {
        val transport = RecordingTransport(Result.success(SEARCH_RESPONSE))

        InnerTubeYouTubeMusicClient(transport).search("midnight", CatalogFilter.ALL)

        val body = Json.parseToJsonElement(transport.body!!).jsonObject
        assertFalse(body.containsKey("params"))
    }

    @Test
    fun browseSendsBrowseId() = runBlocking {
        val transport = RecordingTransport(Result.success(BROWSE_RESPONSE))

        val page = InnerTubeYouTubeMusicClient(transport).browse("MPREb_album1").getOrThrow()

        assertEquals("browse", transport.endpoint)
        assertEquals("MPREb_album1", page.id)
        val body = Json.parseToJsonElement(transport.body!!).jsonObject
        assertEquals("MPREb_album1", body["browseId"]!!.jsonPrimitive.content)
    }

    @Test
    fun transportFailureIsPreserved() = runBlocking {
        val failure = CatalogError.Http(429)
        val transport = RecordingTransport(Result.failure(failure))

        val result = InnerTubeYouTubeMusicClient(transport).search("m83", CatalogFilter.ALL)

        assertEquals(failure, result.exceptionOrNull())
    }

    @Test
    fun malformedTransportBodyBecomesTypedParseFailure() = runBlocking {
        val transport = RecordingTransport(Result.success("{not json"))

        val result = InnerTubeYouTubeMusicClient(transport).search("m83", CatalogFilter.ALL)

        assertTrue(result.exceptionOrNull() is CatalogError.InvalidResponse)
    }

    @Test
    fun cancellationPropagatesInsteadOfBecomingFailure() = runBlocking {
        val transport = object : InnerTubeTransport {
            override suspend fun post(endpoint: String, jsonBody: String): Result<String> =
                throw CancellationException("cancelled")
        }

        try {
            InnerTubeYouTubeMusicClient(transport).search("m83", CatalogFilter.ALL)
            fail("Expected cancellation to propagate")
        } catch (expected: CancellationException) {
            assertEquals("cancelled", expected.message)
        }
    }

    @Test
    fun networkErrorDoesNotExposeFullUrl() {
        val error = CatalogError.Network(
            IOException("https://rr1---abc.googlevideo.com/videoplayback?expire=1&sig=secret")
        )

        assertFalse(error.message.orEmpty().contains("googlevideo.com"))
        assertFalse(error.toString().contains("googlevideo.com"))
        assertFalse(error.toString().contains("secret"))
    }

    private companion object {
        val SEARCH_RESPONSE = """
            {
              "contents": {
                "sectionListRenderer": {
                  "contents": [
                    {
                      "musicShelfRenderer": {
                        "contents": [
                          {
                            "musicResponsiveListItemRenderer": {
                              "flexColumns": [
                                {"musicResponsiveListItemFlexColumnRenderer": {"text": {"runs": [{"text": "Midnight City"}]}}},
                                {"musicResponsiveListItemFlexColumnRenderer": {"text": {"runs": [
                                  {"text": "Song"}, {"text": " • "}, {"text": "M83"}, {"text": " • "}, {"text": "4:03"}
                                ]}}}
                              ],
                              "navigationEndpoint": {"watchEndpoint": {"videoId": "vid-1"}}
                            }
                          },
                          {
                            "musicResponsiveListItemRenderer": {
                              "flexColumns": [
                                {"musicResponsiveListItemFlexColumnRenderer": {"text": {"runs": [{"text": "Hurry Up"}]}}},
                                {"musicResponsiveListItemFlexColumnRenderer": {"text": {"runs": [
                                  {"text": "Album"}, {"text": " • "}, {"text": "M83"}
                                ]}}}
                              ],
                              "navigationEndpoint": {"browseEndpoint": {
                                "browseId": "MPREb_album1",
                                "browseEndpointContextSupportedConfigs": {
                                  "browseEndpointContextMusicConfig": {"pageType": "MUSIC_PAGE_TYPE_ALBUM"}
                                }
                              }}
                            }
                          }
                        ]
                      }
                    }
                  ]
                }
              }
            }
        """.trimIndent()

        val BROWSE_RESPONSE = """
            {
              "header": {"musicDetailHeaderRenderer": {"title": {"runs": [{"text": "Hurry Up"}]}}},
              "contents": {
                "singleColumnBrowseResultsRenderer": {
                  "tabs": [
                    {
                      "tabRenderer": {
                        "content": {
                          "sectionListRenderer": {
                            "contents": [
                              {
                                "musicShelfRenderer": {
                                  "contents": [
                                    {
                                      "musicResponsiveListItemRenderer": {
                                        "flexColumns": [
                                          {"musicResponsiveListItemFlexColumnRenderer": {"text": {"runs": [{"text": "Midnight City"}]}}},
                                          {"musicResponsiveListItemFlexColumnRenderer": {"text": {"runs": [
                                            {"text": "Song"}, {"text": " • "}, {"text": "M83"}
                                          ]}}}
                                        ],
                                        "navigationEndpoint": {"watchEndpoint": {"videoId": "vid-1"}}
                                      }
                                    }
                                  ]
                                }
                              }
                            ]
                          }
                        }
                      }
                    }
                  ]
                }
              }
            }
        """.trimIndent()
    }
}
