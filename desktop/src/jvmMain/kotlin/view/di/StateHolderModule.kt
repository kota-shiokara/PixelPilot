package view.di

import MainStateHolder
import org.koin.dsl.module
import view.screen.qr.QrScreenStateHolder

val stateHolderModule = module {
    factory {
        QrScreenStateHolder(
            qrGenerateUseCase = get(),
            getAddressUseCase = get()
        )
    }

    factory {
        MainStateHolder(
//            qrScreenStateHolder = get()
            qrGenerateUseCase = get(),
            getAddressUseCase = get()
        )
    }
}