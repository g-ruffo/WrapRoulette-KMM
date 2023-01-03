package ca.veltus.wraproulettekmm

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform