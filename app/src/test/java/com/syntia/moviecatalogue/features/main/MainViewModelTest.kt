package com.syntia.moviecatalogue.features.main

import com.syntia.moviecatalogue.base.domain.model.result.ResultWrapper
import com.syntia.moviecatalogue.core.domain.model.trending.TrendingItemUiModel
import com.syntia.moviecatalogue.core.domain.usecase.TrendingUseCase
import com.syntia.moviecatalogue.core.helper.BaseViewModelTest
import com.syntia.moviecatalogue.features.main.viewmodel.MainViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
@InternalCoroutinesApi
class MainViewModelTest : BaseViewModelTest() {

  private lateinit var viewModel: MainViewModel

  private val trendingUseCase = mock<TrendingUseCase>()

  override fun setUp() {
    super.setUp()
    viewModel = MainViewModel(trendingUseCase)
  }

  @Test
  fun `Given when fetch trending items then update live data`() {
    val uiModel = mutableListOf(TrendingItemUiModel(
        id = ID,
        title = TITLE,
        image = IMAGE,
        releasedYear = YEAR,
        voteAverage = VOTE_AVERAGE_STRING,
        type = MEDIA_TYPE_MOVIE
    ))
    val response = getFlow(ResultWrapper.Success(uiModel), true).flowOn(dispatcher)

    rule.dispatcher.runBlockingTest {
      whenever(trendingUseCase.getTrendingItems()) doReturn response

      viewModel.fetchTrendingItems()

      verify(trendingUseCase).getTrendingItems()
      delay(1000)

      viewModel.trendingItems.observeForTesting { actual ->
        assertEquals(uiModel, actual)
      }

      verifyNoMoreInteractions(trendingUseCase)
    }
  }
}