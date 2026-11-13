package com.callbackdev.thatstheword.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.semantics.contentDescription
import androidx.glance.semantics.semantics
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import com.callbackdev.thatstheword.core.domain.daily.DayStanding
import com.callbackdev.thatstheword.core.domain.daily.TodayWords
import com.callbackdev.thatstheword.core.domain.widget.CardFit
import com.callbackdev.thatstheword.core.domain.widget.CardForm
import com.callbackdev.thatstheword.core.domain.widget.CardPlan
import com.callbackdev.thatstheword.core.domain.widget.CardSpec
import com.callbackdev.thatstheword.core.domain.widget.RowLabel
import com.callbackdev.thatstheword.core.domain.widget.WidgetType

/**
 * «Today's word», the home-screen card (PLANNING.md §7.8, Phase 7b): today's word in each language,
 * drawn as the start page's tiles, and the game language's streak. Its forms and their arithmetic
 * are [CardFit]'s; this file draws them. A language's row opens its word of today; the rest of the
 * card, the game.
 *
 * | rank | what | size | weight | ink |
 * |---|---|---|---|---|
 * | 1 | the tiles: found, missed, under way, waiting | up to 28 dp | – | the feedback's own; the ink |
 * | 2 | a language's name (its code where narrow) | [WidgetType.BODY] | Medium; the game language's Bold | strong |
 * | 3 | the row's status ("Found in 4") | [WidgetType.LABEL] | Regular | quiet |
 * | 4 | the eyebrow, the date, the streak's line | [WidgetType.CAPTION], [WidgetType.BODY], [WidgetType.LABEL] | Medium; Regular | quiet; strong |
 */
class TodayCard : GlanceAppWidget() {
    /** Exact, so [LocalSize] is the size the launcher granted: every form is read off it. */
    override val sizeMode: SizeMode = SizeMode.Exact

    /** The picker's generated preview (API 35+): the placement it lands at, and a narrow card. */
    override val previewSizeMode = SizeMode.Responsive(setOf(WidgetSamples.FourByTwo, WidgetSamples.TwoByTwo))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val appWidgetId = runCatching { GlanceAppWidgetManager(context).getAppWidgetId(id) }.getOrDefault(0)
        val models = CardModels(context, appWidgetId)
        // Read before the load, so only a change after it reloads (WidgetRefresh).
        val loadedAt = WidgetRefresh.revision.value
        val initial = models.load()
        provideContent {
            TodayCardContent(rememberWidgetModel(appWidgetId, initial, loadedAt) { models.load() })
        }
    }

    override suspend fun providePreview(context: Context, widgetCategory: Int) {
        provideContent { TodayCardContent(WidgetSamples.model()) }
    }

    /** The game's card that says so, and the reason in the game's log, instead of Glance's blank box. */
    override fun onCompositionError(context: Context, glanceId: GlanceId, appWidgetId: Int, throwable: Throwable) {
        WidgetFailure.show(context, appWidgetId, throwable)
    }
}

class TodayCardReceiver : ThatsTheWordCardReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TodayCard()
}

/**
 * The whole card for [model] at [LocalSize]: split off the widget so the settings screen's preview
 * and the tests draw the same composition the launcher does.
 */
@Composable
fun TodayCardContent(model: WidgetModel) {
    val context = LocalContext.current
    val size = LocalSize.current
    val form = CardFit.form(size.width.value, size.height.value)
    val (horizontal, vertical) = when (form) {
        CardForm.CELL -> CardFit.CELL_PADDING to CardFit.CARD_PADDING_SNUG
        CardForm.ROW -> CardFit.CARD_PADDING to CardFit.CARD_PADDING_SNUG
        CardForm.TALL -> CardFit.CARD_PADDING to CardFit.CARD_PADDING
    }
    WidgetCard(model, horizontal.dp, vertical.dp) { palette ->
        val today = model.today
        if (today == null) {
            MessageContent(
                context.getString(R.string.widget_unavailable_title),
                context.getString(R.string.widget_unavailable_hint).takeIf { form != CardForm.CELL },
                palette,
            )
        } else {
            val parts = remember(today, palette, size) { CardParts(context, today, palette, size) }
            when (parts.plan.form) {
                CardForm.TALL -> TallContent(parts)
                CardForm.ROW -> RowContent(parts)
                CardForm.CELL -> CellContent(parts)
            }
        }
    }
}

/** What every form draws from: the day, its words measured in the launcher's face, and the plan. */
private class CardParts(val context: Context, val today: TodayWords, val palette: CardPalette, size: DpSize) {
    val texts = CardTexts(context, today)
    val scale = fontScale(context)
    val density = context.resources.displayMetrics.density
    private val words = texts.measured()
    val plan: CardPlan = CardFit.plan(CardSpec(size.width.value, size.height.value, scale, words))

    fun label(day: DayStanding): String =
        if (plan.label == RowLabel.NAME) texts.name(day.language) else texts.code(day.language)

    /** The label's column, as wide as the widest label, so the three rows' tiles stand aligned. */
    val labelWidth: Float =
        (if (plan.label == RowLabel.NAME) words.nameEm else words.codeEm) * plan.labelSp * scale

    fun tiles(day: DayStanding) = ImageProvider(TilesPainter.paint(day.status, plan.tiles, plan.tile, density, palette))

    val tilesWidth: Float get() = CardFit.tilesWidth(plan.tile, plan.tiles)

    fun weight(day: DayStanding): FontWeight =
        if (day.language == today.gameLanguage) FontWeight.Bold else FontWeight.Medium

    fun open(day: DayStanding): GlanceModifier =
        WidgetIntents.today(context, day.language)?.let { GlanceModifier.clickable(actionStartActivity(it)) }
            ?: GlanceModifier
}

/**
 * Two rows high and up: the eyebrow and the date; a row per language, its name, its tiles and its
 * status; the streak's line at the foot. Five children at most in the column (Glance drops the
 * eleventh child of a container silently).
 */
@Composable
private fun TallContent(parts: CardParts) {
    val plan = parts.plan
    val palette = parts.palette
    val texts = parts.texts
    Column(modifier = GlanceModifier.fillMaxSize()) {
        Text(
            text = if (plan.eyebrow) texts.eyebrow else texts.eyebrowShort,
            style = TextStyle(
                color = palette.secondaryInk,
                fontSize = WidgetType.CAPTION.sp,
                fontWeight = FontWeight.Medium,
            ),
            maxLines = 1,
            modifier = GlanceModifier.semantics { contentDescription = texts.eyebrowWords },
        )
        if (plan.date) {
            Text(
                text = texts.date,
                style = TextStyle(
                    color = palette.primaryInk,
                    fontSize = WidgetType.BODY.sp,
                    fontWeight = FontWeight.Medium,
                ),
                maxLines = 1,
            )
        }
        Column(
            modifier = GlanceModifier.fillMaxWidth().defaultWeight().padding(top = CardFit.SECTION_GAP.dp),
            verticalAlignment = Alignment.Vertical.CenterVertically,
        ) {
            parts.today.days.forEachIndexed { index, day ->
                Row(
                    modifier = parts.open(day)
                        .fillMaxWidth()
                        .padding(top = if (index == 0) 0.dp else plan.rowGap.dp)
                        .semantics { contentDescription = parts.texts.rowDescription(day) },
                    verticalAlignment = Alignment.Vertical.CenterVertically,
                ) {
                    Text(
                        text = parts.label(day),
                        style = TextStyle(
                            color = palette.primaryInk,
                            fontSize = plan.labelSp.sp,
                            fontWeight = parts.weight(day),
                        ),
                        maxLines = 1,
                        modifier = GlanceModifier.width((parts.labelWidth + CardFit.COLUMN_GAP).dp),
                    )
                    Image(
                        provider = parts.tiles(day),
                        contentDescription = null,
                        modifier = GlanceModifier.size(parts.tilesWidth.dp, plan.tile.dp),
                    )
                    if (plan.status) {
                        Text(
                            text = texts.status(day),
                            style = TextStyle(
                                color = palette.secondaryInk,
                                fontSize = WidgetType.LABEL.sp,
                                textAlign = TextAlign.End,
                            ),
                            maxLines = 1,
                            modifier = GlanceModifier.defaultWeight().padding(start = CardFit.COLUMN_GAP.dp),
                        )
                    }
                }
            }
        }
        if (plan.streak) {
            Text(
                text = if (plan.streakShort) texts.streakShort else texts.streakLong,
                style = TextStyle(color = palette.primaryInk, fontSize = WidgetType.LABEL.sp),
                maxLines = 1,
                modifier = GlanceModifier.padding(top = CardFit.SECTION_GAP.dp),
            )
        }
    }
}

/** One row high: a column per language, its label over its tiles over its status; the streak's own. */
@Composable
private fun RowContent(parts: CardParts) {
    val plan = parts.plan
    val palette = parts.palette
    val texts = parts.texts
    val size = LocalSize.current
    val columns = parts.today.days.size + if (plan.streak) 1 else 0
    val inner = size.width.value - CardFit.CARD_PADDING * 2
    val column = (inner - (columns - 1) * CardFit.COLUMN_GAP) / columns
    Row(modifier = GlanceModifier.fillMaxSize(), verticalAlignment = Alignment.Vertical.CenterVertically) {
        parts.today.days.forEachIndexed { index, day ->
            Column(
                modifier = parts.open(day)
                    .width(column.dp + if (index == 0) 0.dp else CardFit.COLUMN_GAP.dp)
                    .padding(start = if (index == 0) 0.dp else CardFit.COLUMN_GAP.dp)
                    .semantics { contentDescription = texts.rowDescription(day) },
                horizontalAlignment = Alignment.Horizontal.CenterHorizontally,
            ) {
                Text(
                    text = parts.label(day),
                    style = TextStyle(
                        color = palette.primaryInk,
                        fontSize = plan.labelSp.sp,
                        fontWeight = parts.weight(day),
                        textAlign = TextAlign.Center,
                    ),
                    maxLines = 1,
                )
                Image(
                    provider = parts.tiles(day),
                    contentDescription = null,
                    modifier = GlanceModifier
                        .padding(top = CardFit.STACK_GAP.dp)
                        .size(parts.tilesWidth.dp, (plan.tile + CardFit.STACK_GAP).dp),
                )
                if (plan.status) {
                    Text(
                        text = texts.status(day),
                        style = TextStyle(
                            color = palette.secondaryInk,
                            fontSize = WidgetType.CAPTION.sp,
                            textAlign = TextAlign.Center,
                        ),
                        maxLines = 1,
                        modifier = GlanceModifier.padding(top = CardFit.STACK_GAP.dp),
                    )
                }
            }
        }
        if (plan.streak) {
            Column(
                modifier = GlanceModifier
                    .width((column + CardFit.COLUMN_GAP).dp)
                    .padding(start = CardFit.COLUMN_GAP.dp)
                    .semantics { contentDescription = texts.streakLong },
                horizontalAlignment = Alignment.Horizontal.CenterHorizontally,
            ) {
                Text(
                    text = texts.streakFigure,
                    style = TextStyle(
                        color = palette.primaryInk,
                        fontSize = WidgetType.TITLE.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                    ),
                    maxLines = 1,
                )
                Text(
                    text = texts.streakWord,
                    style = TextStyle(
                        color = palette.secondaryInk,
                        fontSize = WidgetType.CAPTION.sp,
                        textAlign = TextAlign.Center,
                    ),
                    maxLines = 1,
                )
            }
        }
    }
}

/** One cell: the three languages, each its code and the day's one tile. */
@Composable
private fun CellContent(parts: CardParts) {
    val plan = parts.plan
    val palette = parts.palette
    Column(
        modifier = GlanceModifier.fillMaxSize(),
        verticalAlignment = Alignment.Vertical.CenterVertically,
        horizontalAlignment = Alignment.Horizontal.CenterHorizontally,
    ) {
        val days = if (plan.languages == 1) listOf(parts.today.of(parts.today.gameLanguage)) else parts.today.days
        days.forEachIndexed { index, day ->
            Row(
                modifier = parts.open(day)
                    .padding(top = if (index == 0) 0.dp else CardFit.STACK_GAP.dp)
                    .semantics { contentDescription = parts.texts.rowDescription(day) },
                verticalAlignment = Alignment.Vertical.CenterVertically,
            ) {
                Text(
                    text = parts.label(day),
                    style = TextStyle(
                        color = palette.primaryInk,
                        fontSize = plan.labelSp.sp,
                        fontWeight = parts.weight(day),
                    ),
                    maxLines = 1,
                    modifier = GlanceModifier.width((parts.labelWidth + CardFit.COLUMN_GAP).dp),
                )
                Image(
                    provider = parts.tiles(day),
                    contentDescription = null,
                    modifier = GlanceModifier.size(plan.tile.dp, plan.tile.dp),
                )
            }
        }
    }
}
