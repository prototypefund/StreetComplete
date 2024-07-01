package de.westnordost.streetcomplete.overlays.way_lit

import android.content.Context
import android.os.Bundle
import android.view.View
import de.westnordost.streetcomplete.R
import de.westnordost.streetcomplete.data.osm.edits.update_tags.StringMapChangesBuilder
import de.westnordost.streetcomplete.data.osm.edits.update_tags.UpdateElementTagsAction
import de.westnordost.streetcomplete.data.preferences.Preferences
import de.westnordost.streetcomplete.osm.changeToSteps
import de.westnordost.streetcomplete.osm.lit.LitStatus
import de.westnordost.streetcomplete.osm.lit.LitStatus.AUTOMATIC
import de.westnordost.streetcomplete.osm.lit.LitStatus.NIGHT_AND_DAY
import de.westnordost.streetcomplete.osm.lit.LitStatus.NO
import de.westnordost.streetcomplete.osm.lit.LitStatus.YES
import de.westnordost.streetcomplete.osm.lit.applyTo
import de.westnordost.streetcomplete.osm.lit.asItem
import de.westnordost.streetcomplete.osm.lit.parseLitStatus
import de.westnordost.streetcomplete.overlays.AImageSelectOverlayForm
import de.westnordost.streetcomplete.overlays.AnswerItem
import de.westnordost.streetcomplete.util.ktx.couldBeSteps
import de.westnordost.streetcomplete.util.ktx.valueOfOrNull
import de.westnordost.streetcomplete.view.image_select.DisplayItem
import org.koin.android.ext.android.inject

class WayLitOverlayForm : AImageSelectOverlayForm<LitStatus>() {

    override val items: List<DisplayItem<LitStatus>> = LitStatus.entries.map { it.asItem() }

    override val selectableItems: List<DisplayItem<LitStatus>> =
        listOf(YES, NO, AUTOMATIC, NIGHT_AND_DAY).map { it.asItem() }

    private val prefs: Preferences by inject()

    override val lastPickedItem: DisplayItem<LitStatus>? get() =
        prefs.getLastPicked(this::class.simpleName!!)
            .map { valueOfOrNull<LitStatus>(it)?.asItem() }
            .firstOrNull()

    private var originalLitStatus: LitStatus? = null

    override val otherAnswers get() = listOfNotNull(
        createConvertToStepsAnswer()
    )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        originalLitStatus = parseLitStatus(element!!.tags)
        selectedItem = originalLitStatus?.asItem()
    }

    override fun hasChanges(): Boolean =
        selectedItem?.value != originalLitStatus

    override fun onClickOk() {
        prefs.addLastPicked(this::class.simpleName!!, selectedItem!!.value!!.name)
        val tagChanges = StringMapChangesBuilder(element!!.tags)
        selectedItem!!.value!!.applyTo(tagChanges)
        applyEdit(UpdateElementTagsAction(element!!, tagChanges.create()))
    }

    private fun createConvertToStepsAnswer(): AnswerItem? =
        if (element!!.couldBeSteps()) {
            AnswerItem(R.string.quest_generic_answer_is_actually_steps) { changeToSteps() }
        } else {
            null
        }

    private fun changeToSteps() {
        val tagChanges = StringMapChangesBuilder(element!!.tags)
        tagChanges.changeToSteps()
        applyEdit(UpdateElementTagsAction(element!!, tagChanges.create()))
    }
}
