package domain.di

import domain.usecase.GetAddressUseCase
import domain.usecase.KeyEventUseCase
import domain.usecase.QrGenerateUseCase
import org.koin.dsl.module

val domainModule = module {
    factory {
        GetAddressUseCase()
    }

    factory {
        QrGenerateUseCase()
    }

    factory {
        KeyEventUseCase(
            keyEventRepository = get()
        )
    }
}