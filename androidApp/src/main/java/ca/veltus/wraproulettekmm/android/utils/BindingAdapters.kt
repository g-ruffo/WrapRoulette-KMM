package ca.veltus.wraproulettekmm.android.utils

import android.annotation.SuppressLint
import android.text.TextUtils
import android.view.View
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.widget.AutoCompleteTextView
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.getColor
import androidx.core.view.isVisible
import androidx.databinding.BindingAdapter
import ca.veltus.wraproulettekmm.android.R
import ca.veltus.wraproulettekmm.android.data.ErrorMessage
import ca.veltus.wraproulettekmm.android.data.objects.Member
import ca.veltus.wraproulettekmm.android.data.objects.Pool
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputLayout
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*

object BindingAdapters {

    @BindingAdapter("poolBetPosition")
    @JvmStatic
    fun dateToStringConverter(view: TextView, date: Date?) {
        if (date == null) {
            view.text = "--:--"
        }
    }

    @BindingAdapter("dateToStringTimeConverter")
    @JvmStatic
    fun dateToStringTimeConverter(view: TextView, date: Date?) {
        if (date != null) {
            val parsedDate = SimpleDateFormat("HH:mm", Locale.ENGLISH)
            val time = parsedDate.format(date)
            view.text = time
        } else {
            view.text = "--:--"
        }
    }

    @BindingAdapter(
        value = ["currentTimeFromDateToString", "isPoolActiveTime"], requireAll = false
    )
    @JvmStatic
    fun currentTimeFromDateToString(view: TextView, date: Date?, poolActive: Boolean = true) {
        if (date != null && poolActive) {
            val parsedDate = SimpleDateFormat("HH:mm:ss", Locale.ENGLISH)
            val time = parsedDate.format(date)
            view.text = time
        } else {
            view.text = "--:--"
        }
    }

    @BindingAdapter("getTimeStringFromLong")
    @JvmStatic
    fun getTimeStringFromLong(view: TextView, time: Long?) {
        if (time == null) {
            view.text = "--:--"
        } else {
            val seconds = (time / 1000) % 60
            val minutes = (time / (1000 * 60) % 60)
            val hours = (time / (1000 * 60 * 60) % 24)

            view.text = String.format("%02d:%02d:%02d", hours, minutes, seconds)
        }
    }

    @BindingAdapter("convertDateToPoolItem")
    @JvmStatic
    fun convertDateToPoolItem(view: TextView, date: String?) {
        if (date != null) {
            val parsedDate = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH)
            val dateFormatter = SimpleDateFormat("MMMM dd, yyyy", Locale.ENGLISH)
            val dateObject = parsedDate.parse(date)
            val convertedDate = dateFormatter.format(dateObject!!)
            view.text = convertedDate
            view.isSelected = true
            view.ellipsize = TextUtils.TruncateAt.MARQUEE
            view.isSingleLine = true
            view.marqueeRepeatLimit = -1
            view.setHorizontallyScrolling(true)
        }
    }

    @BindingAdapter(
        value = ["setCurrentTimeTitleIsActive", "setCurrentTimeTitleWinners", "setCurrentTimeTitleWrapTime"],
        requireAll = true
    )
    @JvmStatic
    fun setCurrentTimeTitle(
        view: TextView, isActive: Boolean, list: List<Member>, wrapTime: Date?
    ) {
        val context = view.context

        if (isActive && wrapTime == null) {
            view.text = context.getString(R.string.currentTime)
        } else if (!isActive && list.isEmpty() && wrapTime != null) {
            view.text = context.getString(R.string.noWinners)
        } else if (!isActive && wrapTime == null) {
            view.text = context.getString(R.string.errorWrapNotSet)
        } else if (!isActive && list.size < 2) {
            view.text = context.getString(R.string.winner)
        } else {
            view.text = context.getString(R.string.winners)
        }
    }

    @BindingAdapter("getTimeFromDate")
    @JvmStatic
    fun getTimeFromDate(view: AutoCompleteTextView, date: Date?) {
        if (date != null) {
            val parsedDate = SimpleDateFormat("HH:mm", Locale.ENGLISH)
            val time = parsedDate.format(date)
            view.setText(time)
        } else {
            view.text.clear()
        }
    }

    @SuppressLint("SetTextI18n")
    @BindingAdapter(
        value = ["calculatePoolPotSizePrice", "calculatePoolPotSizeList"], requireAll = true
    )
    @JvmStatic
    fun calculatePoolPotSize(view: TextView, bidPrice: String?, list: MutableList<Member>) {
        if (!bidPrice.isNullOrEmpty()) {
            val price = bidPrice.toInt()
            val total = "$${(price * list.size)}"
            view.text = total
        } else {
            view.text = "$0"
        }
    }

    @Suppress("SENSELESS_COMPARISON")
    @BindingAdapter("android:calculatePoolItemPotSize")
    @JvmStatic
    fun calculatePoolItemPotSize(view: TextView, pool: Pool) {
        val totalBets = mutableListOf<String>()
        pool.bets.forEach { if (it.value != null) totalBets.add(it.key) }
        val formatter = DecimalFormat("$###0.00")
        val bidAmount = pool.betAmount?.toInt() ?: 0
        val total = formatter.format(bidAmount * totalBets.size)
        view.text = total
    }

    @Suppress("SENSELESS_COMPARISON")
    @BindingAdapter(
        value = ["calculateTotalPoolBetsList", "calculateTotalPoolBetsUid"], requireAll = true
    )
    @JvmStatic
    fun calculateTotalPoolBets(view: TextView, pools: List<Pool>?, uid: String?) {
        val totalBets = mutableListOf<String>()
        if (pools != null && uid != null) {
            pools.forEach {
                it.bets.forEach { bet ->
                    if (bet.value != null && bet.key == uid) totalBets.add(
                        bet.value.toString()
                    )
                }
            }
        }
        view.text = totalBets.size.toString()
    }

    @BindingAdapter(
        value = ["calculateTotalWinningsList", "calculateTotalWinningsUid"], requireAll = true
    )
    @JvmStatic
    fun calculateTotalWinnings(view: TextView, pools: List<Pool>?, uid: String?) {
        val totalBets = mutableListOf<Int>()
        val formatter = DecimalFormat("$###0.00")
        if (pools != null && uid != null) {
            pools.forEach { pool ->
                pool.winners.forEach { winner ->
                    if (winner.uid == uid && winner.tempMemberUid == null) totalBets.add(
                        winner.winnings ?: 0
                    )
                }
            }
        }

        view.text = formatter.format(totalBets.sum())
    }

    // Use this binding adapter to show and hide the views using boolean variables.
    @BindingAdapter("android:fadeVisible")
    @JvmStatic
    fun setFadeVisible(view: View, visible: Boolean? = true) {
        if (view.tag == null) {
            view.tag = true
            view.visibility = if (visible == true) View.VISIBLE else View.GONE
        } else {
            view.animate().cancel()
            if (visible == true) {
                if (view.visibility == View.GONE) view.fadeIn()
            } else {
                if (view.visibility == View.VISIBLE) view.fadeOut()
            }
        }
    }

    @BindingAdapter("setSizeAndEnabled")
    @JvmStatic
    internal fun FloatingActionButton.setSizeAndEnabled(message: String?) {
        this.isEnabled = !(message.isNullOrEmpty() || message.isBlank())
    }

    @BindingAdapter(
        value = ["setResetEnabledArg1", "setResetEnabledArg2"], requireAll = true
    )
    @JvmStatic
    fun setResetEnabledArg(view: View, arg1: String?, arg2: Boolean) {
        view.isEnabled = !(arg1.isNullOrEmpty() || arg1.isBlank()) && !arg2
    }

    @BindingAdapter(
        value = ["setAccountEnabledArg1", "setAccountEnabledArg2", "setAccountEnabledArg3"],
        requireAll = true
    )
    @JvmStatic
    fun setAccountEnabledArg(view: View, arg1: String?, arg2: String?, arg3: Boolean) {
        view.isEnabled =
            !(arg1.isNullOrEmpty() || arg1.isBlank()) && !(arg2.isNullOrEmpty() || arg2.isBlank()) && !arg3
    }

    @BindingAdapter(
        value = ["setPoolEnabledArg1", "setPoolEnabledArg2", "setPoolEnabledArg3", "setPoolEnabledArg4"],
        requireAll = true
    )
    @JvmStatic
    fun setPoolEnabledArg(view: View, arg1: String?, arg2: String?, arg3: Date?, arg4: Boolean) {
        view.isEnabled =
            !(arg1.isNullOrEmpty() || arg1.isBlank()) && !(arg2.isNullOrEmpty() || arg2.isBlank()) && (arg3 != null) && !arg4
    }

    @BindingAdapter(
        value = ["setSignupEnabledArg1", "setSignupEnabledArg2", "setSignupEnabledArg3", "setSignupEnabledArg4", "setSignupEnabledArg5"],
        requireAll = true
    )
    @JvmStatic
    fun setSignupEnabledArg(
        view: View, arg1: String?, arg2: String?, arg3: String?, arg4: String?, arg5: Boolean
    ) {
        view.isEnabled =
            !(arg1.isNullOrEmpty() || arg1.isBlank()) && !(arg2.isNullOrEmpty() || arg2.isBlank()) && (arg3 != null) && !(arg4.isNullOrEmpty() || arg4.isBlank()) && !arg5
    }

    @Suppress("SENSELESS_COMPARISON")
    @BindingAdapter("error")
    @JvmStatic
    internal fun TextInputLayout.setError(errorMessage: ErrorMessage<String>?) {
        this.isErrorEnabled = errorMessage != null
        this.isHelperTextEnabled = errorMessage != null

        when (errorMessage) {
            is ErrorMessage.HelperText -> {
                helperText = errorMessage.message.takeUnless { it == null }
                boxStrokeWidth = 0
                this.isHelperTextEnabled = true
            }
            is ErrorMessage.ErrorText -> {
                error = errorMessage.message.takeUnless { it == null }
                boxStrokeWidth = 1
                this.isErrorEnabled = true
            }
            else -> {
                boxStrokeWidth = 0
                this.isErrorEnabled = errorMessage != null
                this.isHelperTextEnabled = false
                this.isErrorEnabled = false
            }
        }

        boxStrokeWidthFocused = boxStrokeWidth
    }

    @Suppress("SENSELESS_COMPARISON")
    @BindingAdapter(value = ["errorRequired", "errorRequiredInput"], requireAll = true)
    @JvmStatic
    internal fun TextInputLayout.setErrorRequired(
        errorMessage: ErrorMessage<String>?, inputString: String?
    ) {
        this.helperText = "Required*"
        this.isErrorEnabled = errorMessage != null
        this.isHelperTextEnabled = inputString.isNullOrEmpty() || inputString.isBlank()

        when (errorMessage) {
            is ErrorMessage.ErrorText -> {
                error = errorMessage.message.takeUnless { it == null }
                this.isErrorEnabled = true
            }
            else -> {
                this.isErrorEnabled = errorMessage != null
                this.isErrorEnabled = false
            }
        }

        boxStrokeWidthFocused = boxStrokeWidth
    }

    @BindingAdapter(
        value = ["setExpandableFabVisibleState", "setExpandableFabEnabledState"], requireAll = true
    )
    @JvmStatic
    fun setExpandableFabViews(
        view: ExtendedFloatingActionButton, visible: Boolean, enabled: Boolean
    ) {
        if (visible && enabled) {
            view.isClickable = true
            view.show()
        } else {
            view.hide()
            view.isClickable = false
        }
    }

    @BindingAdapter(
        value = ["setExpandableFabState", "setExpandableFabAdminState"], requireAll = true
    )
    @JvmStatic
    fun setExpandableFabState(
        view: ExtendedFloatingActionButton, isClicked: Boolean, isAdmin: Boolean
    ) {
        if (isAdmin) {
            view.visibility = View.VISIBLE
            view.isEnabled = true
            if (isClicked) {
                view.extend()
            } else {
                view.shrink()
            }
        } else {
            view.visibility = View.GONE
            view.isEnabled = false
        }
    }


    @BindingAdapter("isScrolling")
    @JvmStatic
    fun setIsScrolling(view: View, isScrolling: Boolean) {
        if (view.isVisible) {
            if (isScrolling) {
                view.animate().translationX(400f).alpha(0f).setDuration(200)
                    .setInterpolator(AccelerateInterpolator()).start()
            } else {
                view.animate().translationX(0F).alpha(1f).setDuration(200)
                    .setInterpolator(DecelerateInterpolator()).start()
            }
        }
    }

    @BindingAdapter("setPoolItemImage")
    @JvmStatic
    fun setPoolItemImage(view: ImageView, pool: Pool) {
        val context = view.context
        val parsedDate = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH)
        val dateObject = parsedDate.parse(pool.date)

        if (pool.endTime != null) {
            view.setBackgroundColor(getColor(context, R.color.poolItemCompleteGreen))
            view.setImageDrawable(
                ContextCompat.getDrawable(
                    context, R.drawable.ic_baseline_verified_24
                )
            )
        } else if (pool.startTime == null || dateObject != null && (Calendar.getInstance().time.time - dateObject.time) < (Constants.DAY * 2)) {
            view.setBackgroundColor(getColor(context, R.color.poolItemProgressBlue))
            view.setImageDrawable(
                ContextCompat.getDrawable(
                    context, R.drawable.ic_baseline_pending_24
                )
            )
        } else {
            view.setBackgroundColor(getColor(context, R.color.poolItemErrorRed))
            view.setImageDrawable(
                ContextCompat.getDrawable(
                    context, R.drawable.ic_baseline_new_releases_24
                )
            )
        }
    }
}

