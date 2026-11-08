package com.example.musicx.catalog.youtube

import com.example.musicx.core.model.CatalogError
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class InnerTubeParserTest {
    private val parser = InnerTubeParser()

    @Test
    fun parsesTypedSearchRowsAndDropsMalformedEntries() {
        val items = parser.parseSearch(SEARCH_RESPONSE)

        assertEquals(4, items.size)

        val track = items[0] as CatalogDto.Track
        assertEquals("vid-1", track.id)
        assertEquals("Midnight City", track.title)
        assertEquals("M83", track.artist)
        assertEquals("Hurry Up, We're Dreaming", track.album)
        assertEquals(243_000L, track.durationMs)
        assertEquals("https://lh3.googleusercontent.com/large", track.artworkUrl)

        val album = items[1] as CatalogDto.Album
        assertEquals("MPREb_album1", album.id)
        assertEquals("M83", album.artist)
        assertEquals(10, album.trackCount)

        val artist = items[2] as CatalogDto.Artist
        assertEquals("UCartist1", artist.id)

        val playlist = items[3] as CatalogDto.Playlist
        assertEquals("VLPLplaylist1", playlist.id)
        assertEquals(12, playlist.trackCount)
    }

    @Test
    fun parsesBrowseHeaderAndShelfItems() {
        val page = parser.parseBrowse(BROWSE_RESPONSE, "MPREb_album1")

        assertEquals("MPREb_album1", page.id)
        assertEquals("Hurry Up, We're Dreaming", page.title)
        assertEquals(2, page.items.size)
        assertTrue(page.items[0] is CatalogDto.Track)
        assertTrue(page.items[1] is CatalogDto.Album)
    }

    @Test
    fun emptyContentsYieldEmptySearch() {
        assertTrue(parser.parseSearch("""{"contents":{}}""").isEmpty())
    }

    @Test
    fun malformedJsonFailsWithTypedError() {
        try {
            parser.parseSearch("{not json")
            fail("Expected typed parse failure")
        } catch (error: CatalogError) {
            assertTrue(error is CatalogError.InvalidResponse)
        }
    }

    @Test
    fun nonObjectRootFailsWithTypedError() {
        try {
            parser.parseSearch("[]")
            fail("Expected typed parse failure")
        } catch (error: CatalogError) {
            assertTrue(error is CatalogError.InvalidResponse)
        }
    }

    @Test
    fun missingArtworkIsNotFabricated() {
        val items = parser.parseSearch(
            """
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
                                {"musicResponsiveListItemFlexColumnRenderer": {"text": {"runs": [{"text": "Song"}]}}},
                                {"musicResponsiveListItemFlexColumnRenderer": {"text": {"runs": [{"text": "Song"}, {"text": " • "}, {"text": "Artist"}]}}}
                              ],
                              "navigationEndpoint": {"watchEndpoint": {"videoId": "vid-2"}}
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
        )

        assertEquals(1, items.size)
        assertNull(items[0].artworkUrl)
    }

    @Test
    fun skipsMusicVideoUploadsInsteadOfCallingThemSongs() {
        val items = parser.parseSearch(
            """
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
                                {"musicResponsiveListItemFlexColumnRenderer": {"text": {"runs": [{"text": "Fan upload"}]}}},
                                {"musicResponsiveListItemFlexColumnRenderer": {"text": {"runs": [
                                  {"text": "Uploader"}, {"text": " • "}, {"text": "4:03"}
                                ]}}}
                              ],
                              "overlay": {"musicItemThumbnailOverlayRenderer": {"content": {"musicPlayButtonRenderer": {
                                "playNavigationEndpoint": {"watchEndpoint": {
                                  "videoId": "video-upload",
                                  "watchEndpointMusicSupportedConfigs": {
                                    "watchEndpointMusicConfig": {"musicVideoType": "MUSIC_VIDEO_TYPE_UGC"}
                                  }
                                }}
                              }}}}
                            }
                          },
                          {
                            "musicResponsiveListItemRenderer": {
                              "flexColumns": [
                                {"musicResponsiveListItemFlexColumnRenderer": {"text": {"runs": [{"text": "Official song"}]}}},
                                {"musicResponsiveListItemFlexColumnRenderer": {"text": {"runs": [
                                  {"text": "Artist"}, {"text": " • "}, {"text": "Album"}, {"text": " • "}, {"text": "3:00"}
                                ]}}}
                              ],
                              "overlay": {"musicItemThumbnailOverlayRenderer": {"content": {"musicPlayButtonRenderer": {
                                "playNavigationEndpoint": {"watchEndpoint": {
                                  "videoId": "official-song",
                                  "watchEndpointMusicSupportedConfigs": {
                                    "watchEndpointMusicConfig": {"musicVideoType": "MUSIC_VIDEO_TYPE_ATV"}
                                  }
                                }}
                              }}}}
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
        )

        assertEquals(1, items.size)
        assertEquals("official-song", items[0].id)
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
                                  {"text": "Song"}, {"text": " • "}, {"text": "M83"}, {"text": " • "},
                                  {"text": "Hurry Up, We're Dreaming"}, {"text": " • "}, {"text": "4:03"}
                                ]}}}
                              ],
                              "thumbnail": {"musicThumbnailRenderer": {"thumbnail": {"thumbnails": [
                                {"url": "https://lh3.googleusercontent.com/small", "width": 60, "height": 60},
                                {"url": "https://lh3.googleusercontent.com/large", "width": 544, "height": 544}
                              ]}}},
                              "navigationEndpoint": {"watchEndpoint": {"videoId": "vid-1"}}
                            }
                          },
                          {
                            "musicResponsiveListItemRenderer": {
                              "flexColumns": [
                                {"musicResponsiveListItemFlexColumnRenderer": {"text": {"runs": [{"text": "Hurry Up, We're Dreaming"}]}}},
                                {"musicResponsiveListItemFlexColumnRenderer": {"text": {"runs": [
                                  {"text": "Album"}, {"text": " • "}, {"text": "M83"}, {"text": " • "}, {"text": "10 songs"}
                                ]}}}
                              ],
                              "navigationEndpoint": {"browseEndpoint": {
                                "browseId": "MPREb_album1",
                                "browseEndpointContextSupportedConfigs": {
                                  "browseEndpointContextMusicConfig": {"pageType": "MUSIC_PAGE_TYPE_ALBUM"}
                                }
                              }}
                            }
                          },
                          {
                            "musicResponsiveListItemRenderer": {
                              "flexColumns": [
                                {"musicResponsiveListItemFlexColumnRenderer": {"text": {"runs": [{"text": "M83"}]}}},
                                {"musicResponsiveListItemFlexColumnRenderer": {"text": {"runs": [{"text": "Artist"}]}}}
                              ],
                              "navigationEndpoint": {"browseEndpoint": {
                                "browseId": "UCartist1",
                                "browseEndpointContextSupportedConfigs": {
                                  "browseEndpointContextMusicConfig": {"pageType": "MUSIC_PAGE_TYPE_ARTIST"}
                                }
                              }}
                            }
                          },
                          {
                            "musicResponsiveListItemRenderer": {
                              "flexColumns": [
                                {"musicResponsiveListItemFlexColumnRenderer": {"text": {"runs": [{"text": "M83 Mix"}]}}},
                                {"musicResponsiveListItemFlexColumnRenderer": {"text": {"runs": [
                                  {"text": "Playlist"}, {"text": " • "}, {"text": "M83"}, {"text": " • "}, {"text": "12 songs"}
                                ]}}}
                              ],
                              "navigationEndpoint": {"browseEndpoint": {
                                "browseId": "VLPLplaylist1",
                                "browseEndpointContextSupportedConfigs": {
                                  "browseEndpointContextMusicConfig": {"pageType": "MUSIC_PAGE_TYPE_PLAYLIST"}
                                }
                              }}
                            }
                          },
                          {
                            "musicResponsiveListItemRenderer": {
                              "flexColumns": [
                                {"musicResponsiveListItemFlexColumnRenderer": {"text": {"runs": [{"text": " "}]}}},
                                {"musicResponsiveListItemFlexColumnRenderer": {"text": {"runs": [{"text": "Song"}, {"text": " • "}, {"text": "Artist"}]}}}
                              ],
                              "navigationEndpoint": {"watchEndpoint": {"videoId": "vid-blank-title"}}
                            }
                          },
                          {
                            "musicResponsiveListItemRenderer": {
                              "flexColumns": [
                                {"musicResponsiveListItemFlexColumnRenderer": {"text": {"runs": [{"text": "No id"}]}}},
                                {"musicResponsiveListItemFlexColumnRenderer": {"text": {"runs": [{"text": "Song"}, {"text": " • "}, {"text": "Artist"}]}}}
                              ]
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
              "header": {
                "musicDetailHeaderRenderer": {
                  "title": {"runs": [{"text": "Hurry Up, We're Dreaming"}]}
                }
              },
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
                                            {"text": "Song"}, {"text": " • "}, {"text": "M83"}, {"text": " • "}, {"text": "4:03"}
                                          ]}}}
                                        ],
                                        "navigationEndpoint": {"watchEndpoint": {"videoId": "vid-1"}}
                                      }
                                    }
                                  ]
                                }
                              },
                              {
                                "musicCarouselShelfRenderer": {
                                  "contents": [
                                    {
                                      "musicTwoRowItemRenderer": {
                                        "title": {"runs": [{"text": " Saturdays = Youth "}]},
                                        "subtitle": {"runs": [{"text": "Album"}, {"text": " • "}, {"text": "M83"}]},
                                        "navigationEndpoint": {"browseEndpoint": {
                                          "browseId": "MPREb_album2",
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
                    }
                  ]
                }
              }
            }
        """.trimIndent()
    }
}
