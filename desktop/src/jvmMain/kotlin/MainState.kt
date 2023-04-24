import domain.model.Screen

data class MainState(
    val currentScreen: Screen? = null,
    val isDark: Boolean = false
)
