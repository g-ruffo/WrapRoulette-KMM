package ca.veltus.wraproulettekmm.android.base

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import ca.veltus.wraproulettekmm.android.data.ErrorMessage
import ca.veltus.wraproulettekmm.android.utils.Constants.DEPARTMENT_ERROR
import ca.veltus.wraproulettekmm.android.utils.Constants.EMAIL_ERROR
import ca.veltus.wraproulettekmm.android.utils.Constants.NAME_ERROR
import ca.veltus.wraproulettekmm.android.utils.Constants.PASSWORD_ERROR
import ca.veltus.wraproulettekmm.android.utils.Constants.POOL_DATE_ERROR
import ca.veltus.wraproulettekmm.android.utils.Constants.POOL_NAME_ERROR
import ca.veltus.wraproulettekmm.android.utils.Constants.POOL_START_ERROR
import ca.veltus.wraproulettekmm.android.utils.SingleLiveEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// Base class for View Models to declare the common LiveData objects in a single place
abstract class BaseViewModel(app: Application) : AndroidViewModel(app) {

    // Used to show circular progress view in the fragments.
    private val _showLoading: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val showLoading: StateFlow<Boolean>
        get() = _showLoading.asStateFlow()

    // Used to show the no data image and to hide remaining views within the fragments.
    private val _showNoData: MutableStateFlow<Boolean> = MutableStateFlow(true)
    val showNoData: StateFlow<Boolean>
        get() = _showNoData.asStateFlow()

    val navigationCommand: SingleLiveEvent<NavigationCommand> = SingleLiveEvent()
    val showSnackBar: SingleLiveEvent<String> = SingleLiveEvent()
    val showToast: SingleLiveEvent<String> = SingleLiveEvent()
    val errorPasswordText: MutableStateFlow<ErrorMessage<String>?> = MutableStateFlow(null)
    val errorEmailText: MutableStateFlow<ErrorMessage<String>?> = MutableStateFlow(null)
    val errorDepartmentText: MutableStateFlow<ErrorMessage<String>?> = MutableStateFlow(null)
    val errorNameText: MutableStateFlow<ErrorMessage<String>?> = MutableStateFlow(null)
    val errorPoolNameText: MutableStateFlow<ErrorMessage<String>?> = MutableStateFlow(null)
    val errorPoolDateText: MutableStateFlow<ErrorMessage<String>?> = MutableStateFlow(null)
    val errorPoolStartText: MutableStateFlow<ErrorMessage<String>?> = MutableStateFlow(null)
    val hasNetworkConnection: MutableStateFlow<Boolean> = MutableStateFlow(false)

    fun navigateBack() {
        navigationCommand.postValue(NavigationCommand.Back)
    }

    /**
     * Called when needing to clear a given helper text error message. If the variable is null, clear
     * all messages.
     */
    fun clearErrorHelperText(error: Int?) {
        when (error) {
            PASSWORD_ERROR -> errorPasswordText.value = null
            EMAIL_ERROR -> errorEmailText.value = null
            DEPARTMENT_ERROR -> errorDepartmentText.value = null
            NAME_ERROR -> errorNameText.value = null
            POOL_NAME_ERROR -> errorPoolNameText.value = null
            POOL_DATE_ERROR -> errorPoolDateText.value = null
            POOL_START_ERROR -> errorPoolStartText.value = null

            null -> {
                errorPasswordText.value = null
                errorEmailText.value = null
                errorDepartmentText.value = null
                errorNameText.value = null
                errorPoolNameText.value = null
                errorPoolDateText.value = null
                errorPoolStartText.value = null
            }
        }
    }

    /**
     * Called when needing to set a given helper text error message.
     */
    fun postErrorHelperText(error: Int?, message: ErrorMessage<String>) {
        when (error) {
            PASSWORD_ERROR -> errorPasswordText.value = message
            EMAIL_ERROR -> errorEmailText.value = message
            DEPARTMENT_ERROR -> errorDepartmentText.value = message
            NAME_ERROR -> errorNameText.value = message
            POOL_NAME_ERROR -> errorPoolNameText.value = message
            POOL_DATE_ERROR -> errorPoolDateText.value = message
            POOL_START_ERROR -> errorPoolStartText.value = message
        }
    }

    fun postSnackBarMessage(message: String) {
        showSnackBar.postValue(message)
    }

    fun postToastMessage(message: String) {
        showToast.postValue(message)
    }

    fun setShowLoadingValue(isLoading: Boolean) {
        _showLoading.value = isLoading
    }

    fun setNoDataValue(noData: Boolean) {
        _showNoData.value = noData
    }
}