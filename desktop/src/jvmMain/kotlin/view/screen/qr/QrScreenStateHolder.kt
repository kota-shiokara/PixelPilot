package view.screen.qr

import androidx.compose.ui.graphics.painter.Painter
import domain.usecase.GetAddressUseCase
import domain.usecase.QrGenerateUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import view.common.ChildStateHolder
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class QrScreenStateHolder(
    val qrGenerateUseCase: QrGenerateUseCase,
    val getAddressUseCase: GetAddressUseCase,
): ChildStateHolder<QrScreenState> {
    private val coroutineScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main + Dispatchers.IO)

    private val qrState: MutableStateFlow<Painter?> = MutableStateFlow(null)

    override val state: StateFlow<QrScreenState> = qrState.map {
        QrScreenState(it)
    }.stateIn(coroutineScope, SharingStarted.WhileSubscribed(), QrScreenState())

    override fun setup() {
        qrState.value = qrGenerateUseCase(getAddressUseCase())
    }

    override fun dispose() {
        coroutineScope.cancel()
    }
}