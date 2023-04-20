package domain.usecase

import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.toPainter
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.client.j2se.MatrixToImageWriter
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import java.util.*

class QrGenerateUseCase(
    private val writer: QRCodeWriter = QRCodeWriter()
) {
    operator fun invoke(contents : String, size : Int = 512): Painter {
        val hints: MutableMap<EncodeHintType, Any> = EnumMap(EncodeHintType::class.java)
        hints[EncodeHintType.ERROR_CORRECTION] = ErrorCorrectionLevel.L
        hints[EncodeHintType.CHARACTER_SET] = Charsets.UTF_8.displayName()
        hints[EncodeHintType.MARGIN] = 4

        return MatrixToImageWriter
            .toBufferedImage(writer.encode(contents, BarcodeFormat.QR_CODE, size, size, hints))
            .toPainter()
    }
}