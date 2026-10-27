package com.example

import androidx.media3.common.MimeTypes
import com.example.data.extractor.ExtractorRegistry
import com.example.data.extractor.JsUnpacker
import com.example.data.extractor.MimeTypeDetector
import com.example.data.extractor.VideoExtractor
import com.example.data.model.ExtractionErrorType
import com.example.data.model.ExtractorResult
import com.example.data.model.VideoStream
import com.example.data.repository.VideoExtractorRepositoryImpl
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class ExtractorRepositoryTest {

    private lateinit var httpClient: OkHttpClient
    private lateinit var mimeTypeDetector: MimeTypeDetector
    private lateinit var registry: ExtractorRegistry
    private lateinit var repository: VideoExtractorRepositoryImpl

    @Before
    fun setup() {
        val context = RuntimeEnvironment.getApplication()
        httpClient = OkHttpClient.Builder().build()
        mimeTypeDetector = MimeTypeDetector(httpClient)
        registry = ExtractorRegistry(httpClient, mimeTypeDetector)
        // Set networkChecker = { true } so tests don't depend on emulator network state
        repository = VideoExtractorRepositoryImpl(
            context = context,
            extractorRegistry = registry,
            networkChecker = { true }
        )
    }

    @Test
    fun testMalformedUrlReturnsError() = runBlocking {
        val result = repository.extractVideo("not_a_valid_url")
        assertTrue(result is ExtractorResult.Error)
        val error = result as ExtractorResult.Error
        assertEquals(ExtractionErrorType.MALFORMED_URL, error.errorType)
    }

    @Test
    fun testOfflineReturnsNoInternetError() = runBlocking {
        val context = RuntimeEnvironment.getApplication()
        val offlineRepo = VideoExtractorRepositoryImpl(
            context = context,
            extractorRegistry = registry,
            networkChecker = { false }
        )
        val result = offlineRepo.extractVideo("https://streamtape.com/e/123456")
        assertTrue(result is ExtractorResult.Error)
        val error = result as ExtractorResult.Error
        assertEquals(ExtractionErrorType.NO_INTERNET, error.errorType)
    }

    @Test
    fun testUnsupportedServerReturnsClearMessage() = runBlocking {
        val result = repository.extractVideo("https://unsupported-streaming-site-123.com/v/999")
        assertTrue(result is ExtractorResult.Error)
        val error = result as ExtractorResult.Error
        assertEquals(ExtractionErrorType.UNSUPPORTED_SERVER, error.errorType)
        assertTrue(error.message.contains("Servidores disponibles"))
    }

    @Test
    fun testDirectHlsStreamDetection() = runBlocking {
        val testHls = "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8"
        val result = repository.extractVideo(testHls)
        assertTrue(result is ExtractorResult.Success)
        val success = result as ExtractorResult.Success
        assertEquals(MimeTypes.APPLICATION_M3U8, success.stream.mimeType)
    }

    @Test
    fun testDirectMp4StreamDetection() = runBlocking {
        val testMp4 = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4"
        val result = repository.extractVideo(testMp4)
        assertTrue(result is ExtractorResult.Success)
        val success = result as ExtractorResult.Success
        assertEquals(MimeTypes.VIDEO_MP4, success.stream.mimeType)
    }

    @Test
    fun testRegisterCustomExtractorDynamically() = runBlocking {
        val customExtractor = object : VideoExtractor {
            override val serverName: String = "MockCustomServer"
            override val supportedDomains: List<String> = listOf("mockcustom.com")

            override suspend fun extract(iframeUrl: String): VideoStream {
                return VideoStream(
                    url = "https://cdn.mockcustom.com/video.m3u8",
                    mimeType = MimeTypes.APPLICATION_M3U8,
                    server = serverName,
                    headers = mapOf("Referer" to "https://mockcustom.com/")
                )
            }
        }

        repository.registerCustomExtractor(customExtractor)

        val result = repository.extractVideo("https://mockcustom.com/embed/12345")
        assertTrue(result is ExtractorResult.Success)
        val success = result as ExtractorResult.Success
        assertEquals("MockCustomServer", success.stream.server)
        assertEquals("https://cdn.mockcustom.com/video.m3u8", success.stream.url)
        assertEquals("https://mockcustom.com/", success.stream.headers["Referer"])
    }

    @Test
    fun testJsUnpackerDecodesPackedScript() {
        val packed = """eval(function(p,a,c,k,e,d){while(c--)if(k[c])p=p.replace(new RegExp('\\b'+c.toString(a)+'\\b','g'),k[c]);return p}('0 1="2://3.4/5.6";',7,7,'var|file|https|server|com|master|m3u8'.split('|')))"""
        assertTrue(JsUnpacker.isPacked(packed))
        val unpacked = JsUnpacker.unpack(packed)
        assertTrue(unpacked.contains("https://server.com/master.m3u8"))
    }
}
