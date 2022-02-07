package de.westnordost.streetcomplete.quests.cycleway_width

import de.westnordost.streetcomplete.R
import de.westnordost.streetcomplete.data.osm.osmquests.OsmFilterQuestType
import de.westnordost.streetcomplete.data.osm.osmquests.Tags
import de.westnordost.streetcomplete.data.user.achievements.QuestTypeAchievement.BICYCLIST
import de.westnordost.streetcomplete.measure.ArSupportChecker
import de.westnordost.streetcomplete.osm.Length

class AddCyclewayWidth(
    private val checkArSupport: ArSupportChecker
) : OsmFilterQuestType<Length>() {

    /* All either exclusive cycleways or ways that are cycleway + footway (or bridleway) but
     *  segregated */
    override val elementFilter = """
        ways with (
          highway = cycleway and foot !~ yes|designated
          or highway ~ cycleway|path|footway and bicycle != no and segregated = yes
          or highway = bridleway and bicycle ~ designated|yes and segregated = yes
        )
        and access !~ private|no
        and !width
    """
    override val changesetComment = "Determine cycleways width"
    override val wikiLink = "Key:cycleway"
    override val icon = R.drawable.ic_quest_bicycleway_width
    override val isSplitWayEnabled = true
    override val questTypeAchievements = listOf(BICYCLIST)
    override val defaultDisabledMessage: Int
        get() = if (!checkArSupport()) R.string.default_disabled_msg_no_ar else 0

    override fun getTitle(tags: Map<String, String>) : Int = R.string.quest_cycleway_width_title

    override fun createForm() = AddWidthForm()

    override fun applyAnswerTo(answer: Length, tags: Tags, timestampEdited: Long) {
        val isExclusive = tags["highway"] == "cycleway" && tags["foot"] != "yes" && tags["foot"] != "designated"

        if (isExclusive) {
            tags["width"] = answer.toOsmValue()
        } else {
            tags["cycleway:width"] = answer.toOsmValue()
        }
    }
}