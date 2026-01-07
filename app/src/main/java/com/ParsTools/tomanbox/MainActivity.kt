package com.ParsTools.tomanbox

import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.ParsTools.tomanbox.ui.theme.TomanBoxTheme
import ir.tapsell.plus.AdRequestCallback
import ir.tapsell.plus.AdShowListener
import ir.tapsell.plus.TapsellPlus
import ir.tapsell.plus.TapsellPlusBannerType
import ir.tapsell.plus.TapsellPlusInitListener
import ir.tapsell.plus.model.AdNetworkError
import ir.tapsell.plus.model.AdNetworks
import ir.tapsell.plus.model.TapsellPlusAdModel
import ir.tapsell.plus.model.TapsellPlusErrorModel
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

class MainActivity : ComponentActivity() {
    private val tapsellReady = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        TapsellPlus.initialize(
            this,
            BuildConfig.TAPSELL_KEY,
            object : TapsellPlusInitListener {
                override fun onInitializeSuccess(adNetworks: AdNetworks) {
                    tapsellReady.value = true
                    Log.d(TapsellLogTag, "onInitializeSuccess: ${adNetworks.name}")
                }

                override fun onInitializeFailed(
                    adNetworks: AdNetworks,
                    adNetworkError: AdNetworkError
                ) {
                    Log.e(
                        TapsellLogTag,
                        "onInitializeFailed: ${adNetworks.name}, error: ${adNetworkError.errorMessage}"
                    )
                }
            }
        )
        setContent {
            TomanBoxTheme {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        containerColor = MaterialTheme.colorScheme.background
                    ) { innerPadding ->
                        TomanBoxScreen(
                            tapsellReady = tapsellReady.value,
                            modifier = Modifier.padding(innerPadding)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TomanBoxScreen(
    tapsellReady: Boolean,
    modifier: Modifier = Modifier
) {
    var mode by rememberSaveable { mutableStateOf(ConvertMode.TomanToRial) }
    var input by rememberSaveable { mutableStateOf("0") }

    val result = remember(input, mode) {
        when (mode) {
            ConvertMode.TomanToRial -> convertTomanToRial(input)
            ConvertMode.RialToToman -> convertRialToToman(input)
        }
    }
    val inputDisplay = remember(input) {
        formatInputDisplay(input)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 24.dp)
                .padding(bottom = KeypadReservedSpace),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            StandardBannerAd(isReady = tapsellReady)
            TitleSection()
            ModeSwitch(
                mode = mode,
                onSelect = { mode = it }
            )
            InputCard(
                value = inputDisplay,
                currencyLabel = mode.inputCurrencyLabel
            )
            ResultCard(state = result, mode = mode)
        }
        Keypad(
            onKeyPress = { key ->
                input = applyKeypadInput(input, key)
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 24.dp)
        )
    }
}

@Composable
private fun TitleSection() {
    Text(
        text = "مبدل ارز",
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.onBackground,
        textAlign = TextAlign.Center
    )
}

@Composable
private fun StandardBannerAd(
    isReady: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context as? Activity
    var responseId by remember { mutableStateOf<String?>(null) }
    var bannerContainer by remember { mutableStateOf<FrameLayout?>(null) }

    LaunchedEffect(isReady) {
        if (!isReady || activity == null) {
            return@LaunchedEffect
        }
        TapsellPlus.requestStandardBannerAd(
            activity,
            TapsellTestStandardZoneId,
            TapsellPlusBannerType.BANNER_320x50,
            object : AdRequestCallback() {
                override fun response(tapsellPlusAdModel: TapsellPlusAdModel) {
                    super.response(tapsellPlusAdModel)
                    responseId = tapsellPlusAdModel.responseId
                }

                override fun error(message: String) {
                    Log.e(TapsellLogTag, "Standard banner request error: $message")
                }
            }
        )
    }

    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        AndroidView(
            factory = { ctx ->
                FrameLayout(ctx).also { frame ->
                    frame.layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        Gravity.CENTER
                    )
                    bannerContainer = frame
                }
            },
            update = { frame ->
                if (bannerContainer !== frame) {
                    bannerContainer = frame
                }
            }
        )
    }

    LaunchedEffect(responseId, bannerContainer) {
        val currentResponseId = responseId
        val container = bannerContainer
        if (currentResponseId != null && container != null && activity != null) {
            TapsellPlus.showStandardBannerAd(
                activity,
                currentResponseId,
                container,
                object : AdShowListener() {
                    override fun onOpened(tapsellPlusAdModel: TapsellPlusAdModel) {
                        super.onOpened(tapsellPlusAdModel)
                    }

                    override fun onError(tapsellPlusErrorModel: TapsellPlusErrorModel) {
                        super.onError(tapsellPlusErrorModel)
                        Log.e(
                            TapsellLogTag,
                            "Standard banner show error: $tapsellPlusErrorModel"
                        )
                    }
                }
            )
        }
    }

    DisposableEffect(responseId, bannerContainer) {
        onDispose {
            val currentResponseId = responseId
            val container = bannerContainer
            if (currentResponseId != null && container != null && activity != null) {
                TapsellPlus.destroyStandardBanner(activity, currentResponseId, container)
            }
        }
    }
}

@Composable
private fun ModeSwitch(
    mode: ConvertMode,
    onSelect: (ConvertMode) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val firstInteractionSource = remember { MutableInteractionSource() }
    val secondInteractionSource = remember { MutableInteractionSource() }
    val firstPressed by firstInteractionSource.collectIsPressedAsState()
    val secondPressed by secondInteractionSource.collectIsPressedAsState()
    val previewMode = when {
        firstPressed -> ConvertMode.TomanToRial
        secondPressed -> ConvertMode.RialToToman
        else -> mode
    }
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .padding(horizontal = 8.dp, vertical = 6.dp)
                .height(48.dp)
        ) {
            val scope = this
            val spacing = 8.dp
            val segmentWidth = (scope.maxWidth - spacing) / 2f
            val indicatorOffset by animateDpAsState(
                targetValue = if (previewMode == ConvertMode.TomanToRial) 0.dp else segmentWidth + spacing,
                animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing)
            )
            Surface(
                modifier = Modifier
                    .offset(x = indicatorOffset)
                    .width(segmentWidth)
                    .fillMaxHeight(),
                shape = RoundedCornerShape(18.dp),
                color = colors.primary.copy(alpha = 0.16f)
            ) {}
            Box(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(spacing),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SegmentButton(
                        text = "تومان به ریال",
                        selected = mode == ConvertMode.TomanToRial,
                        modifier = Modifier.width(segmentWidth),
                        interactionSource = firstInteractionSource,
                        onClick = { onSelect(ConvertMode.TomanToRial) }
                    )
                    SegmentButton(
                        text = "ریال به تومان",
                        selected = mode == ConvertMode.RialToToman,
                        modifier = Modifier.width(segmentWidth),
                        interactionSource = secondInteractionSource,
                        onClick = { onSelect(ConvertMode.RialToToman) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SegmentButton(
    text: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    interactionSource: MutableInteractionSource,
    onClick: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val textColor by animateColorAsState(
        targetValue = if (selected) colors.primary else colors.onSurfaceVariant
    )
    Box(
        modifier = modifier
            .fillMaxHeight()
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            color = textColor
        )
    }
}

@Composable
private fun InputCard(
    value: String,
    currencyLabel: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 18.dp),
            contentAlignment = Alignment.Center
        ) {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = value,
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Start,
                        modifier = Modifier.align(Alignment.CenterStart)
                    )
                    Surface(
                        modifier = Modifier.align(Alignment.CenterEnd),
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = currencyLabel,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ResultCard(
    state: ConversionState,
    mode: ConvertMode,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .background(MaterialTheme.colorScheme.primary)
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "معادل:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${state.outputNumber.ifBlank { "۰" }} ${mode.outputLabel}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                if (!state.errorMessage.isNullOrBlank()) {
                    Text(
                        text = state.errorMessage.orEmpty(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Start,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                if (state.outputWords.isNotBlank()) {
                    Text(
                        text = "به حروف: ${state.outputWords}",
                        modifier = Modifier.fillMaxWidth(),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            textDirection = TextDirection.Rtl
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Start
                    )
                }
            }
        }
    }
}

@Composable
private fun Keypad(
    onKeyPress: (KeypadKey) -> Unit,
    modifier: Modifier = Modifier
) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        BoxWithConstraints(modifier = modifier) {
            val scope = this
            val spacing = 10.dp
            val buttonWidth = (scope.maxWidth - spacing * 2f) / 3f
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(spacing)
            ) {
                KeypadRows.forEach { keys ->
                    KeypadRow(
                        keys = keys,
                        buttonWidth = buttonWidth,
                        spacing = spacing,
                        onKeyPress = onKeyPress
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(spacing)
                ) {
                    KeypadButton(
                        label = "⌫",
                        tone = KeypadTone.Accent,
                        modifier = Modifier.width(buttonWidth),
                        repeatOnLongPress = true,
                        onClick = { onKeyPress(KeypadKey.Backspace) }
                    )
                    KeypadButton(
                        label = "۰",
                        modifier = Modifier.width(buttonWidth),
                        onClick = { onKeyPress(KeypadZeroKey) }
                    )
                    KeypadButton(
                        label = "تایید",
                        tone = KeypadTone.Primary,
                        modifier = Modifier.width(buttonWidth),
                        onClick = { onKeyPress(KeypadKey.Confirm) }
                    )
                }
            }
        }
    }
}

@Composable
private fun KeypadRow(
    keys: List<KeypadKey>,
    buttonWidth: Dp,
    spacing: Dp,
    onKeyPress: (KeypadKey) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(spacing)
    ) {
        keys.forEach { key ->
            KeypadButton(
                label = key.label,
                modifier = Modifier.width(buttonWidth),
                onClick = { onKeyPress(key) }
            )
        }
    }
}

@Composable
private fun KeypadButton(
    label: String,
    modifier: Modifier = Modifier,
    tone: KeypadTone = KeypadTone.Normal,
    repeatOnLongPress: Boolean = false,
    onClick: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val background = when (tone) {
        KeypadTone.Normal -> colors.surface
        KeypadTone.Accent -> colors.surface
        KeypadTone.Primary -> colors.primary
    }
    val contentColor = when (tone) {
        KeypadTone.Normal -> colors.onSurface
        KeypadTone.Accent -> colors.onSurfaceVariant
        KeypadTone.Primary -> colors.onPrimary
    }
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val currentOnClick by rememberUpdatedState(onClick)
    LaunchedEffect(repeatOnLongPress, isPressed) {
        if (repeatOnLongPress && isPressed) {
            currentOnClick()
            while (isActive && isPressed) {
                delay(KeypadLongPressIntervalMillis)
                currentOnClick()
            }
        }
    }
    Surface(
        modifier = modifier.height(56.dp),
        shape = RoundedCornerShape(16.dp),
        color = background,
        contentColor = contentColor,
        tonalElevation = 0.dp,
        shadowElevation = 3.dp
    ) {
        val gestureModifier = Modifier
            .fillMaxSize()
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = if (repeatOnLongPress) ({}) else onClick
            )
        Box(
            modifier = gestureModifier,
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

private data class ConversionState(
    val outputNumber: String,
    val outputWords: String,
    val errorMessage: String?
)

private const val TapsellLogTag = "TapsellPlus"
private const val TapsellTestStandardZoneId = "5cfaaa30e8d17f0001ffb294"
private const val MaxInputLength = 18
private const val KeypadLongPressIntervalMillis = 120L
private val KeypadReservedSpace = 302.dp

private val decimalFormat = DecimalFormat("#,###", DecimalFormatSymbols(Locale.US))

private enum class KeypadTone {
    Normal,
    Accent,
    Primary
}

private sealed class KeypadKey(val label: String) {
    data class Digit(val value: String, val display: String) : KeypadKey(display)
    object Backspace : KeypadKey("⌫")
    object Confirm : KeypadKey("تایید")
}

private val KeypadRows = listOf(
    listOf(
        KeypadKey.Digit("1", "۱"),
        KeypadKey.Digit("2", "۲"),
        KeypadKey.Digit("3", "۳")
    ),
    listOf(
        KeypadKey.Digit("4", "۴"),
        KeypadKey.Digit("5", "۵"),
        KeypadKey.Digit("6", "۶")
    ),
    listOf(
        KeypadKey.Digit("7", "۷"),
        KeypadKey.Digit("8", "۸"),
        KeypadKey.Digit("9", "۹")
    )
)
private val KeypadZeroKey = KeypadKey.Digit("0", "۰")

private fun applyKeypadInput(current: String, key: KeypadKey): String = when (key) {
    is KeypadKey.Digit -> appendDigits(current, key.value)
    KeypadKey.Backspace -> {
        if (current.length <= 1) {
            "0"
        } else {
            current.dropLast(1)
        }
    }
    KeypadKey.Confirm -> current
}

private fun appendDigits(current: String, digits: String): String {
    if (current.length >= MaxInputLength) {
        return current
    }
    val base = if (current == "0") "" else current
    val appended = (base + digits).take(MaxInputLength)
    return appended.trimStart('0').ifBlank { "0" }
}

private enum class ConvertMode(
    val outputLabel: String,
    val inputCurrencyLabel: String
) {
    TomanToRial(
        outputLabel = "ریال",
        inputCurrencyLabel = "تومان"
    ),
    RialToToman(
        outputLabel = "تومان",
        inputCurrencyLabel = "ریال"
    )
}

private fun convertTomanToRial(input: String): ConversionState {
    val normalized = normalizeDigits(input)
    if (normalized.isBlank()) {
        return ConversionState("", "", null)
    }
    val number = normalized.toLongOrNull()
        ?: return ConversionState("", "", "عدد معتبر وارد کنید")
    if (number > Long.MAX_VALUE / 10) {
        return ConversionState("", "", "عدد خیلی بزرگ است")
    }
    val result = number * 10
    return ConversionState(
        outputNumber = formatNumberDisplay(result),
        outputWords = "${numberToPersianWords(result)} ریال",
        errorMessage = null
    )
}

private fun convertRialToToman(input: String): ConversionState {
    val normalized = normalizeDigits(input)
    if (normalized.isBlank()) {
        return ConversionState("", "", null)
    }
    val number = normalized.toLongOrNull()
        ?: return ConversionState("", "", "عدد معتبر وارد کنید")
    val toman = number / 10
    val remainder = (number % 10).toInt()
    val outputNumber = if (remainder == 0) {
        formatNumber(toman)
    } else {
        "${formatNumber(toman)}.${remainder}"
    }
    val outputWords = if (remainder == 0) {
        "${numberToPersianWords(toman)} تومان"
    } else {
        "${numberToPersianWords(toman)} تومان و ${numberToPersianWords(remainder.toLong())} ریال"
    }
    return ConversionState(
        outputNumber = toPersianDigits(outputNumber),
        outputWords = outputWords,
        errorMessage = null
    )
}

private fun formatInputDisplay(input: String): String {
    val normalized = normalizeDigits(input)
    if (normalized.isBlank()) {
        return "۰"
    }
    val number = normalized.toLongOrNull() ?: return toPersianDigits(normalized)
    return formatNumberDisplay(number)
}

private fun formatNumberDisplay(value: Long): String = toPersianDigits(formatNumber(value))

private fun formatNumber(value: Long): String = decimalFormat.format(value)

private fun normalizeDigits(input: String): String = buildString {
    for (char in input) {
        toEnglishDigit(char)?.let { append(it) }
    }
}

private fun toEnglishDigit(char: Char): Char? = when (char) {
    in '0'..'9' -> char
    '۰' -> '0'
    '۱' -> '1'
    '۲' -> '2'
    '۳' -> '3'
    '۴' -> '4'
    '۵' -> '5'
    '۶' -> '6'
    '۷' -> '7'
    '۸' -> '8'
    '۹' -> '9'
    '٠' -> '0'
    '١' -> '1'
    '٢' -> '2'
    '٣' -> '3'
    '٤' -> '4'
    '٥' -> '5'
    '٦' -> '6'
    '٧' -> '7'
    '٨' -> '8'
    '٩' -> '9'
    else -> null
}

private fun toPersianDigits(input: String): String = buildString {
    for (char in input) {
        append(
            when (char) {
                '0' -> '۰'
                '1' -> '۱'
                '2' -> '۲'
                '3' -> '۳'
                '4' -> '۴'
                '5' -> '۵'
                '6' -> '۶'
                '7' -> '۷'
                '8' -> '۸'
                '9' -> '۹'
                ',' -> '،'
                '.' -> '٫'
                else -> char
            }
        )
    }
}

private val units = arrayOf(
    "صفر",
    "یک",
    "دو",
    "سه",
    "چهار",
    "پنج",
    "شش",
    "هفت",
    "هشت",
    "نه"
)
private val teens = arrayOf(
    "ده",
    "یازده",
    "دوازده",
    "سیزده",
    "چهارده",
    "پانزده",
    "شانزده",
    "هفده",
    "هجده",
    "نوزده"
)
private val tens = arrayOf(
    "",
    "",
    "بیست",
    "سی",
    "چهل",
    "پنجاه",
    "شصت",
    "هفتاد",
    "هشتاد",
    "نود"
)
private val hundreds = arrayOf(
    "",
    "صد",
    "دویست",
    "سیصد",
    "چهارصد",
    "پانصد",
    "ششصد",
    "هفتصد",
    "هشتصد",
    "نهصد"
)
private val scales = arrayOf(
    "",
    "هزار",
    "میلیون",
    "میلیارد",
    "تریلیون",
    "کوادریلیون",
    "کوینتیلیون"
)

private fun numberToPersianWords(number: Long): String {
    if (number == 0L) {
        return units[0]
    }
    var remaining = number
    var scaleIndex = 0
    val parts = mutableListOf<String>()
    while (remaining > 0 && scaleIndex < scales.size) {
        val group = (remaining % 1000).toInt()
        if (group > 0) {
            val groupWords = threeDigitsToWords(group)
            val scale = scales[scaleIndex]
            parts.add(if (scale.isNotEmpty()) "$groupWords $scale" else groupWords)
        }
        remaining /= 1000
        scaleIndex++
    }
    return parts.reversed().joinToString(" و ")
}

private fun threeDigitsToWords(number: Int): String {
    val parts = mutableListOf<String>()
    val hundredPart = number / 100
    val remainder = number % 100
    if (hundredPart > 0) {
        parts.add(hundreds[hundredPart])
    }
    if (remainder in 10..19) {
        parts.add(teens[remainder - 10])
    } else {
        val tenPart = remainder / 10
        val unitPart = remainder % 10
        if (tenPart > 0) {
            parts.add(tens[tenPart])
        }
        if (unitPart > 0) {
            parts.add(units[unitPart])
        }
    }
    return parts.joinToString(" و ")
}

@Preview(showBackground = true, widthDp = 420)
@Composable
fun TomanBoxPreview() {
    TomanBoxTheme {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            TomanBoxScreen(tapsellReady = true)
        }
    }
}
