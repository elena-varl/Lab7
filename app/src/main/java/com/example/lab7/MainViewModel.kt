package com.example.lab7

import android.app.Activity
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.lab7.data.StepsData
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.fitness.Fitness
import com.google.android.gms.fitness.FitnessOptions
import com.google.android.gms.fitness.data.DataPoint
import com.google.android.gms.fitness.data.DataSet
import com.google.android.gms.fitness.data.DataSource
import com.google.android.gms.fitness.data.DataType
import com.google.android.gms.fitness.data.Field
import com.google.android.gms.fitness.request.DataDeleteRequest
import com.google.android.gms.fitness.request.DataReadRequest
import java.text.Format
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit

const val MY_PERMISSIONS_REQUEST_ACTIVITY_RECOGNITION = 1
const val GOOGLE_FIT_PERMISSIONS_REQUEST_CODE = 2
const val TAG = "accessGoogleFit"

class MainViewModel : ViewModel() {

    var stepsListLive: MutableLiveData<MutableList<StepsData>> = MutableLiveData()

    private val fitnessOptions = FitnessOptions.builder()
        .addDataType(DataType.TYPE_STEP_COUNT_DELTA, FitnessOptions.ACCESS_READ)
        .addDataType(DataType.AGGREGATE_STEP_COUNT_DELTA, FitnessOptions.ACCESS_READ)
        .addDataType(DataType.TYPE_STEP_COUNT_DELTA, FitnessOptions.ACCESS_WRITE)
        .addDataType(DataType.AGGREGATE_STEP_COUNT_DELTA, FitnessOptions.ACCESS_WRITE)
        .build()

    private lateinit var account: GoogleSignInAccount

    fun init(activity: Activity) {
        if (this::account.isInitialized) {
            return
        }

        account = GoogleSignIn.getAccountForExtension(activity, fitnessOptions)

        if (!GoogleSignIn.hasPermissions(account, fitnessOptions)) {
            GoogleSignIn.requestPermissions(
                activity,
                GOOGLE_FIT_PERMISSIONS_REQUEST_CODE,
                account,
                fitnessOptions
            )
        } else {
            Log.i("init", "already has permissions")
        }
    }

    fun addSteps(activity: Activity, timestamp: LocalDateTime, count: Int) {
        if (!this::account.isInitialized) {
            Log.w(TAG, "account is not initialized")
            return
        }

        val startTime = timestamp.atZone(ZoneOffset.UTC)
        val endTime = startTime.plusMinutes(59)

        val dataSource = DataSource.Builder()
            .setAppPackageName(activity)
            .setDataType(DataType.TYPE_STEP_COUNT_DELTA)
            .setStreamName("$TAG - step count")
            .setType(DataSource.TYPE_RAW)
            .build()

        val dataPoint =
            DataPoint.builder(dataSource)
                .setField(Field.FIELD_STEPS, count)
                .setTimeInterval(
                    startTime.toEpochSecond(),
                    endTime.toEpochSecond(),
                    TimeUnit.SECONDS,
                )
                .build()

        val dataSet = DataSet.builder(dataSource)
            .add(dataPoint)
            .build()

        Fitness.getHistoryClient(activity, account)
            .insertData(dataSet)
            .addOnSuccessListener {
                Log.i(TAG, "DataSet ([$startTime, $endTime]; $count) added successfully!")
                getSteps(activity)
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "There was an error adding the DataSet", e)
            }
    }

    fun removeSteps(activity: Activity, timestamp: LocalDateTime) {
        if (!this::account.isInitialized) {
            Log.w(TAG, "account is not initialized")
            return
        }

        val startTime = timestamp.atZone(ZoneOffset.UTC)
        val endTime = startTime.plusMinutes(59)

        val request = DataDeleteRequest.Builder()
            .setTimeInterval(startTime.toEpochSecond(), endTime.toEpochSecond(), TimeUnit.SECONDS)
            .addDataType(DataType.TYPE_STEP_COUNT_DELTA)
            .build()

        Fitness.getHistoryClient(activity, account)
            .deleteData(request)
            .addOnSuccessListener {
                Log.i(TAG, "Data ([$startTime, $endTime]) deleted successfully!")
                getSteps(activity)
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "There was an error with the deletion request", e)
            }
    }


    fun getSteps(activity: Activity) {
        if (!this::account.isInitialized) {
            Log.w(TAG, "account is not initialized")
            return
        }

        val endTime = LocalDateTime.now().atZone(ZoneOffset.UTC).truncatedTo(ChronoUnit.HOURS)
        val startTime = endTime.minusDays(7)

        val endSeconds = endTime.toEpochSecond()
        val startSeconds = startTime.toEpochSecond()

        val readRequest = DataReadRequest.Builder()
            .aggregate(DataType.AGGREGATE_STEP_COUNT_DELTA)
            .setTimeRange(startSeconds, endSeconds, TimeUnit.SECONDS)
            .bucketByTime(1, TimeUnit.HOURS)
            .enableServerQueries()
            .build()

        val result = mutableListOf<StepsData>()

        Fitness.getHistoryClient(activity, account)
            .readData(readRequest)
            .addOnSuccessListener { response ->
                for (b in response.buckets) {
                    var cnt = 0
                    for (ds in b.dataSets) {
                        for (dp in ds.dataPoints) {
                            cnt += dp.getValue(Field.FIELD_STEPS).toString().toInt()
                        }
                    }
                    val ts = LocalDateTime.ofEpochSecond(
                        b.getStartTime(TimeUnit.SECONDS), 0, ZoneOffset.UTC
                    )
                    result.add(StepsData(ts, cnt))
                }
                result.sortByDescending { it.timestamp }
                stepsListLive.value = result
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "readData failed", e)
            }
    }
}