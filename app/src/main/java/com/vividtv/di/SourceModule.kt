package com.vividtv.di

import com.vividtv.data.source.SourceStrategy
import com.vividtv.data.source.SampleDataSource
import com.vividtv.data.source.iptv.IptvSourceStrategy
import com.vividtv.data.source.overseas.OverseasSourceStrategy
import com.vividtv.data.source.vod.TvBoxSourceStrategy
import com.vividtv.data.source.vod.VodSourceStrategy
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SourceModule {

    @Provides
    @Singleton
    @IntoSet
    fun provideVodSource(strategy: VodSourceStrategy): SourceStrategy = strategy

    @Provides
    @Singleton
    @IntoSet
    fun provideIptvSource(strategy: IptvSourceStrategy): SourceStrategy = strategy

    @Provides
    @Singleton
    @IntoSet
    fun provideOverseasSource(strategy: OverseasSourceStrategy): SourceStrategy = strategy
    @Provides
    @Singleton
    @IntoSet
    fun provideSampleSource(strategy: SampleDataSource): SourceStrategy = strategy

    @Provides
    @Singleton
    @IntoSet
    fun provideTvBoxSource(strategy: TvBoxSourceStrategy): SourceStrategy = strategy
}
