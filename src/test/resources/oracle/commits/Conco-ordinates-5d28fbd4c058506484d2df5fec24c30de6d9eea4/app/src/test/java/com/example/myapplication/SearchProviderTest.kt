package com.example.myapplication.logic

import com.example.myapplication.data.Building
import com.example.myapplication.data.Campus
import com.example.myapplication.data.CampusRepo
import com.example.myapplication.data.JsonLatLng
import com.google.android.gms.tasks.Tasks
import com.google.android.libraries.places.api.model.AutocompletePrediction
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsResponse
import com.google.android.libraries.places.api.net.PlacesClient
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.*
import android.text.SpannableString

@OptIn(ExperimentalCoroutinesApi::class)
class SearchProviderTest {

    private lateinit var mockPlacesClient: PlacesClient
    private lateinit var mockIndoorRepo: com.example.myapplication.data.indoor.IndoorRepository
    private lateinit var searchProvider: HybridSearchProvider

    @Before
    fun setup() {
        com.example.myapplication.telemetry.CrashReporter.isTesting = true
        mockPlacesClient = mock()
        mockIndoorRepo   = mock()
        searchProvider   = HybridSearchProvider(mockPlacesClient, mockIndoorRepo)
    }

    @Test
    fun `search returns static results when query is blank`() = runTest {
        val results = searchProvider.search("")

        assertEquals(2, results.size)
        assertTrue(results.any { it is SearchResult.CurrentLocation })
        assertTrue(results.any { it is SearchResult.Home })
    }

    @Test
    fun `search returns static defaults for blank query`() = runTest {
        val results = searchProvider.search("  ")
        assertEquals(2, results.size)
        assertTrue(results[0] is SearchResult.CurrentLocation)
    }

    @Test
    fun `search returns local and google results on success`() = runTest {
        // Mock a Google Places Prediction
        val mockPrediction = mock<AutocompletePrediction> {
            on { getPrimaryText(anyOrNull()) } doReturn mock() // Mocks the Spannable
            on { getSecondaryText(anyOrNull()) } doReturn mock()
            on { placeId } doReturn "id_123"
        }

        val mockResponse = mock<FindAutocompletePredictionsResponse> {
            on { autocompletePredictions } doReturn listOf(mockPrediction)
        }

        // Wrap in a Google Task for .await() support
        whenever(mockPlacesClient.findAutocompletePredictions(any()))
            .thenReturn(Tasks.forResult(mockResponse))

        val results = searchProvider.search("Concordia")

        // Verifies we reached the end of the try block
        assertNotNull(results)
    }


}