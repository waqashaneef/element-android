package im.vector.riotredesign.features.home.room.detail.timeline

import android.arch.paging.PagedList
import com.airbnb.epoxy.EpoxyAsyncUtil
import com.airbnb.epoxy.EpoxyController
import im.vector.matrix.android.api.session.events.model.EnrichedEvent
import im.vector.matrix.android.api.session.events.model.EventType
import im.vector.matrix.android.api.session.events.model.roomMember
import im.vector.matrix.android.api.session.room.model.MessageContent
import im.vector.riotredesign.core.extensions.localDateTime
import im.vector.riotredesign.features.home.LoadingItemModel_

class TimelineEventController(private val timelineDateFormatter: TimelineDateFormatter) : EpoxyController(
        EpoxyAsyncUtil.getAsyncBackgroundHandler(),
        EpoxyAsyncUtil.getAsyncBackgroundHandler()
) {

    private val messagesDisplayedWithInformation = HashSet<String?>()

    private val pagedListCallback = object : PagedList.Callback() {
        override fun onChanged(position: Int, count: Int) {
            requestModelBuild()
        }

        override fun onInserted(position: Int, count: Int) {
            requestModelBuild()
        }

        override fun onRemoved(position: Int, count: Int) {
            requestModelBuild()
        }
    }

    var timeline: PagedList<EnrichedEvent>? = null
        set(value) {
            field?.removeWeakCallback(pagedListCallback)
            field = value
            field?.addWeakCallback(null, pagedListCallback)
        }

    override fun buildModels() {
        buildModels(timeline)
    }

    private fun buildModels(data: List<EnrichedEvent>?) {
        if (data.isNullOrEmpty()) {
            return
        }
        for (index in 0 until data.size) {
            val event = data[index]
            val nextEvent = if (index + 1 < data.size) data[index + 1] else null

            val date = event.root.localDateTime()
            val nextDate = nextEvent?.root?.localDateTime()
            val addDaySeparator = date.toLocalDate() != nextDate?.toLocalDate()

            if (event.root.type == EventType.MESSAGE) {
                val messageContent = event.root.content<MessageContent>()
                val roomMember = event.roomMember()
                if (messageContent == null || roomMember == null) {
                    continue
                }

                val nextRoomMember = nextEvent?.roomMember()
                if (addDaySeparator || nextRoomMember != roomMember) {
                    messagesDisplayedWithInformation.add(event.root.eventId)
                }
                val showInformation = messagesDisplayedWithInformation.contains(event.root.eventId)

                TimelineMessageItem(
                        message = messageContent.body,
                        avatarUrl = roomMember.avatarUrl,
                        showInformation = showInformation,
                        time = timelineDateFormatter.formatMessageHour(date),
                        memberName = roomMember.displayName
                )
                        .onBind { timeline?.loadAround(index) }
                        .id(event.root.eventId)
                        .addTo(this)
            } else {
                BlankItemModel_()
                        .id(event.root.eventId)
                        .onBind { _, _, _ -> timeline?.loadAround(index) }
                        .addTo(this)
            }
            if (addDaySeparator) {
                val formattedDay = timelineDateFormatter.formatMessageDay(date)
                TimelineDaySeparatorItem(formattedDay).id(formattedDay).addTo(this)
            }
        }

        //It's a hack at the moment
        val isLastEvent = data.last().root.type == EventType.STATE_ROOM_CREATE
        LoadingItemModel_()
                .id("backward_loading_item")
                .addIf(!isLastEvent, this)
    }

}