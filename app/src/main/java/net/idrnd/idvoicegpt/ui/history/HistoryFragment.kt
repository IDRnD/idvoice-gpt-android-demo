package net.idrnd.idvoicegpt.ui.history

import android.os.Bundle
import android.view.View
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import net.idrnd.idvoicegpt.R
import net.idrnd.idvoicegpt.ui.ChatMessages
import net.idrnd.idvoicegpt.util.getAppPreferences
import net.idrnd.idvoicegpt.util.getChatHistoryJson
import java.lang.reflect.Type

/**
 * represents the history screen.
 */
class HistoryFragment : Fragment(R.layout.fragment_history) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        activity?.findViewById<Toolbar>(R.id.main_toolbar)?.title = getString(R.string.history)
        val chatRecyclerView = view.findViewById<RecyclerView>(R.id.history_recycler)
        val chatAdapter = ChatAdapter()

        chatRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = chatAdapter
        }

        requireContext().getAppPreferences().also {
            val serializedObject: String? = it.getChatHistoryJson()
            if (serializedObject != null) {
                val gson = Gson()
                val type: Type = object : TypeToken<List<ChatMessages>>() {}.type
                val arrayItems = gson.fromJson<List<ChatMessages>>(serializedObject, type)
                chatAdapter.submitList(arrayItems)
            }
        }
    }
}
