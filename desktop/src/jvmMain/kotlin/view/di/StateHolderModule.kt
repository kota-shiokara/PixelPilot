package view.di

import MainStateHolder
import org.koin.dsl.module

val stateHolderModule = module {
    factory {
        MainStateHolder(
            qrGenerateUseCase = get(),
            getAddressUseCase = get()
        )
    }
}