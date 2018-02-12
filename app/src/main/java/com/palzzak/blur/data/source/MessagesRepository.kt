package com.palzzak.blur.data.source

import com.palzzak.blur.data.Message
import javax.inject.Inject

/**
 * Created by yooas on 2018-01-19.
 */
class MessagesRepository: MessagesDataSource {
    @Inject
    lateinit var mMessagesLocalDataSource: MessagesDataSource

    private val mCachedMessages: MutableMap<Long, Message> = LinkedHashMap()

    override fun getMessages(callback: MessagesDataSource.LoadMessagesCallback) {
        if (!mCachedMessages.isEmpty()) {
            callback.onMessagesLoaded(ArrayList(mCachedMessages.values))
            return
        }

        mMessagesLocalDataSource.getMessages(object: MessagesDataSource.LoadMessagesCallback{
            override fun onMessagesLoaded(messages: List<Message>?) {
                if (messages == null || messages.isEmpty()) {
                    return
                }
                refreshCache(messages)
                refreshLocalDataSource(messages)
                callback.onMessagesLoaded(ArrayList(mCachedMessages.values))
            }
        })
    }

    private fun refreshLocalDataSource(messages: List<Message>) {
        mMessagesLocalDataSource.deleteAllMessages()
        messages.map {
            mMessagesLocalDataSource.saveMessage(it)
        }
    }

    private fun refreshCache(messages: List<Message>){
        mCachedMessages.clear()
        messages.map {
            mCachedMessages[it.mVoiceId] = it
        }
    }

    override fun getMessage(id: Long, callback: MessagesDataSource.GetMessageCallback) {
        val cachedMessage = getMessageById(id)
        if (cachedMessage != null) {
            callback.onMessageLoaded(cachedMessage)
            return
        }

        mMessagesLocalDataSource.getMessage(id, object: MessagesDataSource.GetMessageCallback{
            override fun onMessageLoaded(message: Message?) {
                if (message == null) {
                    return
                }
                mCachedMessages[message.mVoiceId] = message
                callback.onMessageLoaded(message)
            }
        })
    }

    private fun getMessageById(id: Long): Message? {
        if (mCachedMessages.isEmpty()) {
            return null
        }
        return mCachedMessages[id]
    }

    override fun saveMessage(message: Message) {
        mMessagesLocalDataSource.saveMessage(message)
        mCachedMessages[message.mVoiceId] = message
    }

    override fun deleteAllMessages() {
        mMessagesLocalDataSource.deleteAllMessages()
        mCachedMessages.clear()
    }

    override fun deleteMessage(id: Long) {
        mMessagesLocalDataSource.deleteMessage(id)
        mCachedMessages.remove(id)
    }
}