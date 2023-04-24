package domain.model

sealed class Screen(
    val route: String
) {
    object QrScreen: Screen("QR")
    object SessionScreen: Screen("SESSION")
}
