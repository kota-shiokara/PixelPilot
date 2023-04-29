package domain.di

import domain.repository.KeyEventRepository
import org.koin.dsl.module

val repositoryModule = module {
    factory {
        KeyEventRepository()
    }
}