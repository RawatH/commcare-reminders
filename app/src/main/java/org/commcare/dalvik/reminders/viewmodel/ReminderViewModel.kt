package org.commcare.dalvik.reminders.viewmodel

import android.app.Application
import androidx.lifecycle.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.commcare.dalvik.reminders.PrefsUtil
import org.commcare.dalvik.reminders.ReminderRepository
import org.commcare.dalvik.reminders.db.ReminderRoomDatabase
import org.commcare.dalvik.reminders.model.Reminder
import org.commcare.dalvik.reminders.utils.TimeUtils
import java.text.ParseException
import java.util.*

class ReminderViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: ReminderRepository
    val futureReminders: LiveData<List<Reminder>>

    init {
        val reminderDao = ReminderRoomDatabase.getDatabase(application).reminderDao()
        repository = ReminderRepository(reminderDao)
        futureReminders = Transformations.switchMap(repository.allReminders) { reminders ->
            val filteredReminders = MutableLiveData<List<Reminder>>()
            filteredReminders.value = reminders.filter { isReminderInFuture(it) }
            filteredReminders
        }
    }

    private fun isReminderInFuture(reminder: Reminder): Boolean {
        try {
            val date = TimeUtils.parseDate(reminder.date)
            return date.time >= Date().time
        } catch (e: ParseException) {
            // do nothing
        }
        return false
    }

    fun syncOnFirstRun() {
        if (PrefsUtil.isSyncPending(getApplication())) {
            viewModelScope.launch(Dispatchers.IO) {
                repository.refreshCasesFromCC(getApplication())
            }
        }
    }
}