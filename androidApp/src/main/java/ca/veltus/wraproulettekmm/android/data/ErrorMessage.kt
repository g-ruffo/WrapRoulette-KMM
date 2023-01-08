package ca.veltus.wraproulettekmm.android.data

sealed class ErrorMessage<out R> {
    data class HelperText(val message: String) : ErrorMessage<Nothing>()
    data class ErrorText(val message: String) : ErrorMessage<Nothing>()
}