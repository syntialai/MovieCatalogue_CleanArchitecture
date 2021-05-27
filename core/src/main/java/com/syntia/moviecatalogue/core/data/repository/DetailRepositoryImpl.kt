package com.syntia.moviecatalogue.core.data.repository

import com.syntia.moviecatalogue.base.data.remote.response.ResponseWrapper
import com.syntia.moviecatalogue.base.data.repository.BaseRepository
import com.syntia.moviecatalogue.base.data.source.network.NetworkBoundResource
import com.syntia.moviecatalogue.base.domain.model.result.ResultWrapper
import com.syntia.moviecatalogue.core.config.api.ApiPath
import com.syntia.moviecatalogue.core.data.source.local.datasource.FavoriteMoviesLocalDataSource
import com.syntia.moviecatalogue.core.data.source.local.datasource.FavoriteTvShowsLocalDataSource
import com.syntia.moviecatalogue.core.data.source.remote.datasource.DetailRemoteDataSource
import com.syntia.moviecatalogue.core.data.source.remote.response.detail.Detail
import com.syntia.moviecatalogue.core.domain.model.detail.CastUiModel
import com.syntia.moviecatalogue.core.domain.model.detail.DetailUiModel
import com.syntia.moviecatalogue.core.domain.repository.DetailRepository
import com.syntia.moviecatalogue.core.utils.mapper.DetailMapper
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn

class DetailRepositoryImpl(private val detailRemoteDataSource: DetailRemoteDataSource,
    private val favoriteMoviesLocalDataSource: FavoriteMoviesLocalDataSource,
    private val favoriteTvShowsLocalDataSource: FavoriteTvShowsLocalDataSource,
    override val ioDispatcher: CoroutineDispatcher) : DetailRepository, BaseRepository() {

  override suspend fun getDetails(mediaType: String, id: Int): Flow<ResultWrapper<DetailUiModel>> {
    return object : NetworkBoundResource<Detail, DetailUiModel>() {

      override suspend fun fetchData(): Flow<ResponseWrapper<Detail>> {
        return getDetailMethod(mediaType, id, detailRemoteDataSource::getMovieDetails,
            detailRemoteDataSource::getTvDetails)
      }

      override fun mapResponse(data: Detail): DetailUiModel {
        return DetailMapper.toDetailUiModel(data)
      }

      override suspend fun saveFetchResult(data: Detail) {
        if (isMovie(mediaType)) {
          favoriteMoviesLocalDataSource.updateMovieById(DetailMapper.toMovieEntity(data))
        } else {
          favoriteTvShowsLocalDataSource.updateTvShow(DetailMapper.toTvShowsEntity(data))
        }
      }
    }.asFlow().flowOn(ioDispatcher)
  }

  override suspend fun getDetailCasts(mediaType: String,
      id: Int): Flow<ResultWrapper<MutableList<CastUiModel>>> {
    return fetchRemote(suspend {
      getDetailMethod(mediaType, id, detailRemoteDataSource::getMovieCredits,
          detailRemoteDataSource::getTvCredits)
    }, DetailMapper::toCastUiModels)
  }

  private suspend fun <T> getDetailMethod(mediaType: String, id: Int,
      movieMethod: suspend (Int) -> T, tvMethod: suspend (Int) -> T) = if (isMovie(mediaType)) {
    movieMethod.invoke(id)
  } else {
    tvMethod.invoke(id)
  }

  private fun isMovie(mediaType: String) = mediaType == ApiPath.MOVIE
}