package com.syntia.moviecatalogue.features.search

import com.syntia.moviecatalogue.core.domain.model.search.SearchResultUiModel
import com.syntia.moviecatalogue.core.domain.usecase.SearchUseCase
import com.syntia.moviecatalogue.core.helper.BaseViewModelTest
import com.syntia.moviecatalogue.features.search.adapter.SearchResultAdapter
import com.syntia.moviecatalogue.features.search.viewmodel.SearchViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
@InternalCoroutinesApi
class SearchViewModelTest : BaseViewModelTest() {

  private lateinit var viewModel: SearchViewModel

  private val searchUseCase = mock<SearchUseCase>()

  private val queryCaptor = argumentCaptor<String>()

  override fun setUp() {
    super.setUp()
    viewModel = SearchViewModel(searchUseCase)
  }

  @Test
  fun `Given when searchQuery then update live data`() {
    val data = listOf(SearchResultUiModel(
        id = ID,
        name = NAME,
        image = IMAGE,
        releasedYear = YEAR,
        voteAverage = VOTE_AVERAGE_STRING,
        adult = false,
        type = MEDIA_TYPE_MOVIE
    ))
    val flow = data.getFakePagingData()
    val differ = getAsyncPagingDataDiffer(SearchResultAdapter.diffCallback)

    rule.dispatcher.runBlockingTest {
      val job = launch {
        flow.collectLatest { data ->
          differ.submitData(data)
        }
      }

      whenever(searchUseCase.searchByQuery(NAME)) doReturn flow

      viewModel.searchQuery(NAME)
      verify(searchUseCase).searchByQuery(queryCaptor.capture())
      assertEquals(NAME, queryCaptor.firstValue)
      advanceUntilIdle()

      Assert.assertTrue(differ.snapshot().contains(data[0]))
      assertNotNull(viewModel.searchResult.value)

      verifyNoMoreInteractions(searchUseCase)
      job.cancel()
    }
  }
}