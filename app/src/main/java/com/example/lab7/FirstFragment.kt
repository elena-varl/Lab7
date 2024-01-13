package com.example.lab7

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.icu.util.Calendar
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.text.format.DateFormat
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.DatePicker
import android.widget.EditText
import android.widget.ListView
import android.widget.SeekBar
import android.widget.TextView
import android.widget.TimePicker
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.example.lab7.data.StepsData
import com.example.lab7.databinding.FragmentFirstBinding
import java.time.LocalDateTime

/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class FirstFragment :  Fragment(), DatePickerDialog.OnDateSetListener,
    TimePickerDialog.OnTimeSetListener {

    companion object {
        fun newInstance() = FirstFragment()
    }

    var myDay = 0
    var myMonth: Int = 0
    var myYear: Int = 0

    var myDateTime: LocalDateTime? = null
    var myStepsCount: Int? = null

    private lateinit var viewModel: MainViewModel
    private lateinit var mainActivity: MainActivity

    private lateinit var listView: ListView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(this).get(MainViewModel::class.java)
        mainActivity = this.activity as MainActivity
        viewModel.init(mainActivity)
        viewModel.getSteps(mainActivity)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        listView = mainActivity.findViewById(R.id.steps_list_view)

        val nameObserver = Observer<MutableList<StepsData>> { newList ->
            val adapter = MyAdapter(requireContext(), mainActivity, newList, viewModel)
            listView.adapter = adapter
        }

        viewModel.stepsListLive.observe(viewLifecycleOwner, nameObserver)

        /* mainActivity.findViewById<TextView>(R.id.selectedDateTime).setOnClickListener {
            val calendar: Calendar = Calendar.getInstance()
            val datePickerDialog = DatePickerDialog(
                mainActivity,
                this,
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH),
            )
            datePickerDialog.show()
        }*/
        mainActivity.findViewById<Button>(R.id.select_DateTime).setOnClickListener {
            val calendar: Calendar = Calendar.getInstance()
            val datePickerDialog = DatePickerDialog(
                mainActivity,
                this,
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH),
            )
            datePickerDialog.show()
        }

        mainActivity.findViewById<Button>(R.id.addSteps).setOnClickListener {
            if (!addStepsBtnEnabled()) {
                return@setOnClickListener
            }
            viewModel.addSteps(mainActivity, myDateTime!!, myStepsCount!!)
        }

        mainActivity.findViewById<SeekBar>(R.id.seekBar)
            .setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                    myStepsCount = progress * 200
                    updateUI()
                }

                override fun onStartTrackingTouch(seekBar: SeekBar) {
                }

                override fun onStopTrackingTouch(seekBar: SeekBar) {
                }
            })
    }


    override fun onDateSet(view: DatePicker?, year: Int, month: Int, dayOfMonth: Int) {
        myDay = dayOfMonth
        myYear = year
        myMonth = month + 1
        val calendar = Calendar.getInstance()
        val timePickerDialog = TimePickerDialog(
            mainActivity,
            this,
            calendar.get(Calendar.HOUR),
            0,
            DateFormat.is24HourFormat(mainActivity),
        )
        timePickerDialog.show()
    }

    override fun onTimeSet(view: TimePicker?, hourOfDay: Int, minute: Int) {
        myDateTime = LocalDateTime.of(myYear, myMonth, myDay, hourOfDay, 0, 0)
        updateUI()
    }

    private fun updateUI() {
        var title = myDateTime?.toString() ?: "Select date and time"
        if (myStepsCount != null) {
            title += " — $myStepsCount steps"
        }
        mainActivity.findViewById<TextView>(R.id.selectedDateTime).text = title
        mainActivity.findViewById<Button>(R.id.addSteps).isEnabled = addStepsBtnEnabled()
    }

    private fun addStepsBtnEnabled(): Boolean {
        return myDateTime != null && myStepsCount != null && (myStepsCount ?: 0) > 0
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_main, container, false)
    }

}