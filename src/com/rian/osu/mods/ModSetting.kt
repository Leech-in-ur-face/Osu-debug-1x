@file:Suppress("ktPropBy")

package com.rian.osu.mods

import com.rian.osu.math.preciseRoundBy
import kotlin.math.*
import kotlin.properties.*
import kotlin.reflect.*

//region Base classes

/**
 * Represents a [Mod] specific setting.
 */
sealed class ModSetting<V>(

    /**
     * The legible name of this [ModSetting].
     */
    val name: String,

    /**
     * The value formatter of this [ModSetting].
     *
     * This is used to format the value of this [ModSetting] when displaying it.
     */
    val valueFormatter: ((V) -> String)?,

    /**
     * The default value of this [ModSetting], which is also the initial value of this [ModSetting].
     */
    open var defaultValue: V

) : ReadWriteProperty<Any?, V> {

    /**
     * The initial value.
     */
    val initialValue = defaultValue

    /**
     * The value itself.
     */
    open var value = defaultValue

    /**
     * Whether this [ModSetting] is set to its default value.
     */
    val isDefault
        get() = value == defaultValue

    override fun getValue(thisRef: Any?, property: KProperty<*>): V {
        return value
    }

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: V) {
        this.value = value
    }
}

/**
 * Represents a [Mod] specific setting whose value is constrained to a range of values.
 */
sealed class RangeConstrainedModSetting<V>(
    name: String,
    valueFormatter: (V) -> String = { it.toString() },
    defaultValue: V,

    /**
     * The minimum value of this [RangeConstrainedModSetting].
     */
    minValue: V,

    /**
     * The maximum value of this [RangeConstrainedModSetting].
     */
    maxValue: V,

    /**
     * The step size for the value of this [RangeConstrainedModSetting].
     */
    step: V,

) : ModSetting<V>(name, valueFormatter, defaultValue) {
    /**
     * The minimum value of this [RangeConstrainedModSetting].
     */
    open var minValue = minValue
        set(value) {
            if (field != value) {
                field = value

                // Trigger processValue to ensure the value is within the new range
                this.value = this.value
            }
        }

    /**
     * The maximum value of this [RangeConstrainedModSetting].
     */
    open var maxValue = maxValue
        set(value) {
            if (field != value) {
                field = value

                // Trigger processValue to ensure the value is within the new range
                this.value = this.value
            }
        }

    /**
     * The step size for the value of this [RangeConstrainedModSetting].
     */
    open var step = step
        set(value) {
            if (field != value) {
                field = value

                // Trigger processValue to ensure the value is within the new range
                this.value = this.value
            }
        }

    override var value
        get() = super.value
        set(value) {
            super.value = processValue(value)
        }

    protected abstract fun processValue(value: V): V

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: V) {
        super.setValue(thisRef, property, processValue(value))
    }
}

//endregion


open class IntegerModSetting(
    name: String,
    valueFormatter: (Int) -> String = { it.toString() },
    defaultValue: Int,
    minValue: Int = Int.MIN_VALUE,
    maxValue: Int = Int.MAX_VALUE,
    step: Int = 1
) : RangeConstrainedModSetting<Int>(name, valueFormatter, defaultValue, minValue, maxValue, step) {
    override var defaultValue
        get() = super.defaultValue
        set(value) {
            if (value !in minValue..maxValue) {
                throw IllegalArgumentException("defaultValue must be between minValue and maxValue.")
            }

            super.defaultValue = value
        }

    override var minValue
        get() = super.minValue
        set(value) {
            if (value > maxValue) {
                throw IllegalArgumentException("minValue cannot be greater than maxValue.")
            }

            super.minValue = value
        }

    override var maxValue
        get() = super.maxValue
        set(value) {
            if (value < minValue) {
                throw IllegalArgumentException("maxValue cannot be less than minValue.")
            }

            super.maxValue = value
        }

    override var step
        get() = super.step
        set(value) {
            if (value < 0) {
                throw IllegalArgumentException("step cannot be less than 0.")
            }

            super.step = value
        }

    init {
        require(minValue <= maxValue) { "minValue must be less than or equal to maxValue." }
        require(step >= 0f) { "step must be greater than or equal to 0." }
        require(defaultValue in minValue..maxValue) { "defaultValue must be between minValue and maxValue." }
    }

    override fun processValue(value: Int) = when {
        value < minValue -> minValue
        value > maxValue -> maxValue
        step == 0 -> value
        else -> (round((value - minValue) / step.toFloat()) * step + minValue).roundToInt()
    }
}

open class FloatModSetting(
    name: String,
    valueFormatter: (Float) -> String = { it.toString() },
    defaultValue: Float,
    minValue: Float = Float.MIN_VALUE,
    maxValue: Float = Float.MAX_VALUE,
    step: Float = 0f,

    /**
     * The number of decimal places to round the value to.
     *
     * When set to `null`, the value will not be rounded.
     */
    precision: Int? = null

) : RangeConstrainedModSetting<Float>(
    name,
    valueFormatter,
    if (precision != null) defaultValue.preciseRoundBy(precision) else defaultValue,
    if (precision != null) minValue.preciseRoundBy(precision) else minValue,
    if (precision != null) maxValue.preciseRoundBy(precision) else maxValue,
    if (precision != null) step.preciseRoundBy(precision) else step
) {
    override var defaultValue
        get() = super.defaultValue
        set(value) {
            if (value !in minValue..maxValue) {
                throw IllegalArgumentException("defaultValue must be between minValue and maxValue.")
            }

            super.defaultValue = value
        }

    override var minValue
        get() = super.minValue
        set(value) {
            if (value > maxValue) {
                throw IllegalArgumentException("minValue cannot be greater than maxValue.")
            }

            super.minValue = value
        }

    override var maxValue
        get() = super.maxValue
        set(value) {
            if (value < minValue) {
                throw IllegalArgumentException("maxValue cannot be less than minValue.")
            }

            super.maxValue = value
        }

    override var step
        get() = super.step
        set(value) {
            if (value < 0) {
                throw IllegalArgumentException("step cannot be less than 0.")
            }

            super.step = value
        }

    /**
     * The number of decimal places to round the value to.
     *
     * When set to `null`, the value will not be rounded.
     */
    var precision = precision
        set(value) {
            if (value != null && value < 0) {
                throw IllegalArgumentException("precision must be greater than or equal to 0.")
            }

            field = value

            if (value != null) {
                // Trigger processValue to ensure the value is within the new range
                this.value = this.value
            }
        }

    init {
        require(minValue <= maxValue) { "minValue must be less than or equal to maxValue." }
        require(step >= 0f) { "step must be greater than or equal to 0." }
        require(precision == null || precision >= 0) { "precision must be greater than or equal to 0." }
        require(defaultValue in minValue..maxValue) { "defaultValue must be between minValue and maxValue." }
    }

    override fun processValue(value: Float) = when {
        value < minValue -> minValue
        value > maxValue -> maxValue
        step == 0f -> value

        else -> {
            val value = round((value - minValue) / step) * step + minValue
            val precision = precision

            if (precision != null) value.preciseRoundBy(precision) else value
        }
    }
}

open class NullableFloatModSetting(
    name: String,
    valueFormatter: (Float?) -> String = { it.toString() },
    defaultValue: Float?,
    minValue: Float = Float.MIN_VALUE,
    maxValue: Float = Float.MAX_VALUE,
    step: Float = 0f,

    /**
     * The number of decimal places to round the value to.
     *
     * When set to `null`, the value will not be rounded.
     */
    precision: Int? = null

) : RangeConstrainedModSetting<Float?>(
    name,
    valueFormatter,
    if (precision != null) defaultValue?.preciseRoundBy(precision) else defaultValue,
    if (precision != null) minValue.preciseRoundBy(precision) else minValue,
    if (precision != null) maxValue.preciseRoundBy(precision) else maxValue,
    if (precision != null) step.preciseRoundBy(precision) else step,
) {
    override var defaultValue
        get() = super.defaultValue
        set(value) {
            if (value != null && value !in minValue!!..maxValue!!) {
                throw IllegalArgumentException("defaultValue must be between minValue and maxValue.")
            }

            super.defaultValue = value
        }

    override var minValue
        get() = super.minValue
        set(value) {
            if (value == null) {
                throw IllegalArgumentException("minValue cannot be null.")
            }

            if (value > maxValue!!) {
                throw IllegalArgumentException("minValue cannot be greater than maxValue.")
            }

            super.minValue = value
        }

    override var maxValue
        get() = super.maxValue
        set(value) {
            if (value == null) {
                throw IllegalArgumentException("maxValue cannot be null.")
            }

            if (value < minValue!!) {
                throw IllegalArgumentException("maxValue cannot be less than minValue.")
            }

            super.maxValue = value
        }

    override var step
        get() = super.step
        set(value) {
            if (value == null) {
                throw IllegalArgumentException("step cannot be null.")
            }

            if (value < 0) {
                throw IllegalArgumentException("step cannot be less than 0.")
            }

            super.step = value
        }

    /**
     * The number of decimal places to round the value to.
     *
     * When set to `null`, the value will not be rounded.
     */
    var precision = precision
        set(value) {
            if (value != null && value < 0) {
                throw IllegalArgumentException("precision must be greater than or equal to 0.")
            }

            field = value

            if (value != null) {
                // Trigger processValue to ensure the value is within the new range
                this.value = this.value
            }
        }

    init {
        require(minValue <= maxValue) { "minValue must be less than or equal to maxValue." }
        require(step >= 0f) { "step must be greater than or equal to 0." }
        require(precision == null || precision >= 0) { "precision must be greater than or equal to 0." }

        require(defaultValue == null || defaultValue in minValue..maxValue) {
            "defaultValue must be between minValue and maxValue."
        }
    }

    override fun processValue(value: Float?) = when {
        value == null -> null
        value < minValue!! -> minValue
        value > maxValue!! -> maxValue
        step == 0f -> value

        else -> {
            val minValue = minValue!!
            val step = step!!
            val precision = precision

            val value = round((value - minValue) / step) * step + minValue

            if (precision != null) value.preciseRoundBy(precision) else value
        }
    }
}

open class BooleanModSetting(
    name: String,
    defaultValue: Boolean
) : ModSetting<Boolean>(name, null, defaultValue)
