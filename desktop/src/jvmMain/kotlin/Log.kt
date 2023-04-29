object Log {
    fun d(s: String) {
        debug(s)
    }

    fun debug(s: String) {
        println("[DEBUG] $s")
    }
}