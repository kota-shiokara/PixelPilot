import androidx.compose.ui.graphics.painter.Painter
import domain.usecase.GetAddressUseCase
import domain.usecase.QrGenerateUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class MainStateHolder(
    val qrGenerateUseCase: QrGenerateUseCase,
    val getAddressUseCase: GetAddressUseCase,
) {
    val qrState: StateFlow<Painter> = MutableStateFlow(qrGenerateUseCase(getAddressUseCase()))

    
}