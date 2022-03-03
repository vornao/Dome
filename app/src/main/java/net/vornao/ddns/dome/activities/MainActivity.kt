package net.vornao.ddns.dome.activities

import androidx.appcompat.app.AppCompatActivity
import net.vornao.ddns.dome.handler.DeviceHandler
import android.widget.TextView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import net.vornao.ddns.dome.adapters.RVAdapter
import net.vornao.ddns.dome.handler.DatabaseHelper
import android.database.sqlite.SQLiteDatabase
import android.nfc.NfcAdapter
import android.app.PendingIntent
import com.google.gson.Gson
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import com.google.firebase.messaging.FirebaseMessaging
import net.vornao.ddns.dome.callbacks.UpdatesCallback
import net.vornao.ddns.dome.callbacks.UpdateDeviceCallback
import com.google.android.material.snackbar.Snackbar
import net.vornao.ddns.dome.R
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import android.annotation.SuppressLint
import androidx.appcompat.view.menu.MenuBuilder
import android.content.Intent
import net.vornao.ddns.dome.fragments.HouseSelectionFragment
import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.SimpleItemAnimator
import net.vornao.ddns.dome.devices.Thermostat
import net.vornao.ddns.dome.devices.EnergyMonitor
import net.vornao.ddns.dome.devices.RemoteSwitch
import android.content.ContentValues
import android.app.NotificationManager
import android.app.NotificationChannel
import android.database.Cursor
import com.google.gson.JsonObject
import android.os.Vibrator
import android.os.VibrationEffect
import android.nfc.tech.Ndef
import android.nfc.NdefRecord
import android.nfc.Tag
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.preference.PreferenceManager
import net.vornao.ddns.dome.devices.Device
import net.vornao.ddns.dome.shared.Const
import net.vornao.ddns.dome.shared.Secrets
import java.lang.AssertionError
import java.lang.Exception
import java.lang.IllegalStateException
import java.nio.charset.StandardCharsets
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean

@SuppressLint("NotifyDataSetChanged")
class MainActivity : AppCompatActivity() {
    private lateinit var deviceHandler: DeviceHandler
    private lateinit var rvDevices: ArrayList<Device>
    private lateinit var MQTT_TOPIC: String
    private var MAX_POWER: Int = 3300
    private var EMON_NOTIFICATION_ACTIVE = false
    private var HEAT_NOTIFICATION_ACTIVE = true
    private var HEAT_NOTIFICATION_TIME = "18:25"
    private lateinit var emptyView: TextView
    private lateinit var refreshLayout: SwipeRefreshLayout
    private val blockUpdates = AtomicBoolean(false)
    private lateinit var adapter: RVAdapter

    // databse stuff
    private var dbHelper: DatabaseHelper? = null
    private var db: SQLiteDatabase? = null

    // nfc section
    private var nfcAdapter: NfcAdapter? = null
    private var pendingIntent: PendingIntent? = null
    private val gson = Gson()

    // cannot make anonymus innner class: sharedpreference register listeners using weakereference
    private val prefListener = OnSharedPreferenceChangeListener { sharedPreferences, key ->
        when (key) {
            Const.MQTT_TOPIC_C -> {
                MQTT_TOPIC = sharedPreferences.getString(Const.MQTT_TOPIC_C, "").toString()
                Log.d("PREFLIST", MQTT_TOPIC)
                refreshDevices()
            }
            "emon_notification" ->
                if (sharedPreferences.getBoolean(key, true))
                    FirebaseMessaging.getInstance().unsubscribeFromTopic(MQTT_TOPIC)
                else
                    FirebaseMessaging.getInstance().subscribeToTopic(MQTT_TOPIC)
            else -> {
            }
        }
    }
    private val updatesCallback: UpdatesCallback = object : UpdatesCallback {
        override fun enableUpdates() {
            blockUpdates.set(false)
        }

        override fun disableUpdates() {
            blockUpdates.set(true)
        }
    }
    private val updateDeviceCallback: UpdateDeviceCallback = object : UpdateDeviceCallback {
        @SuppressLint("NotifyDataSetChanged")
        override fun onSuccess() {
            if (!blockUpdates.get()) adapter.notifyDataSetChanged()
        }

        override fun onFailure() {
            @SuppressLint("NotifyDataSetChanged")
            if (!blockUpdates.get()) adapter.notifyDataSetChanged()
            val sb = Snackbar.make(window.decorView,
                    R.string.snackbar_conn_error,
                    Snackbar.LENGTH_LONG)
            sb.show()
        }

        override fun onSuccess(pos: Int) {
            if (!blockUpdates.get()) adapter.notifyItemChanged(rvDevices.indexOf(devices[pos]))
        }

        override fun onConnSuccess() {
            val sb = Snackbar.make(window.decorView,
                    R.string.snackbar_update_success,
                    Snackbar.LENGTH_LONG)
            sb.show()
            if (EMON_NOTIFICATION_ACTIVE) subscribeToNotification()
        }

        override fun onNewDevice(id: Int) {
            Log.d("NDEV", "added")
            rvDevices.add(devices[id]!!)
            devices[id]!!.viewPosition = rvDevices.indexOf(devices[id])
            adapter.notifyItemInserted(rvDevices.indexOf(devices[id]))
            emptyView.visibility = View.GONE
            addDeviceToDB(id)
        }
    }

    /**
     * Handle drag and remove of recyclerview cards.
     * swipe action is not handled
     */
    private val touchHelperCallback: ItemTouchHelper.SimpleCallback = object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.START or ItemTouchHelper.END or ItemTouchHelper.UP or ItemTouchHelper.DOWN,
            ItemTouchHelper.RIGHT) {
        override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
            val from = viewHolder.adapterPosition
            val to = target.adapterPosition
            rvDevices[from].viewPosition = to
            rvDevices[to].viewPosition = from
            Collections.swap(rvDevices, from, to)
            adapter.notifyItemMoved(from, to)

            // update device position in list
            updateDevicePosition(rvDevices[from].id, rvDevices[from].viewPosition)
            updateDevicePosition(rvDevices[to].id, rvDevices[to].viewPosition)
            return true
        }

        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
            if (direction == ItemTouchHelper.RIGHT) {
                if (!rvDevices[viewHolder.adapterPosition].isOffline) {
                    Snackbar.make(window.decorView, R.string.delete_device_error, Snackbar.LENGTH_SHORT).show()
                    errorClickFeedback()
                    // tell rvadapter to put elem back in view
                    adapter.notifyDataSetChanged()
                } else {
                    longClickFeedback()
                    deleteDatabaseEntry(rvDevices.removeAt(viewHolder.adapterPosition).id)
                    adapter.notifyItemRemoved(viewHolder.adapterPosition)
                    Snackbar.make(window.decorView, R.string.item_deleted_msg, Snackbar.LENGTH_SHORT).show()
                }
            }
        }

        override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
            super.clearView(recyclerView, viewHolder)
            blockUpdates.set(false)
            refreshLayout.isEnabled = true
        }

        override fun onSelectedChanged(vh: RecyclerView.ViewHolder?, state: Int) {
            super.onSelectedChanged(vh, state)
            if (state == ItemTouchHelper.ACTION_STATE_DRAG) {
                // we need to disable updates: if rv is updated during card positioning
                // action would be suddenly interrupted.
                // (same for remote switch renaming)
                refreshLayout.isEnabled = false
                blockUpdates.set(true)
                longClickFeedback()
                Snackbar.make(
                        window.decorView,
                        resources.getString(R.string.drag_card_snackbar),
                        Snackbar.LENGTH_SHORT).show()
            } else if (state == ItemTouchHelper.ACTION_STATE_SWIPE) {
                refreshLayout.isEnabled = false
                blockUpdates.set(true)
            }
        }
    }

    /** Menu buttons handler  */
    @SuppressLint("RestrictedApi")
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.actionbar_menu, menu)
        (menu as MenuBuilder).setOptionalIconsVisible(true)
        return true
    }

    @SuppressLint("NonConstantResourceId")
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // action with ID action_refresh was selected
        return when (item.itemId) {
            R.id.action_refresh -> {
                refreshDevices()
                true
            }
            R.id.action_settings -> {
                val intent = Intent(this, SettingsActivity::class.java)
                this.startActivity(intent)
                true
            }
            R.id.actionJoin -> {
                val intentJoin = Intent(this, NfcReadActivity::class.java)
                this.startActivity(intentJoin)
                true
            }
            R.id.actionAddDevice -> {
                val intentAdd = Intent(this, DeviceSetupActivity::class.java)
                this.startActivity(intentAdd)
                true
            }
            R.id.actionChange -> {
                val b = HouseSelectionFragment()
                val bundle = Bundle()
                bundle.putStringArray("available-topics", topicsAsStringArray)
                b.arguments = bundle
                b.show(supportFragmentManager, "select-topic-fragment")
                true
            }
            else -> true
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // arraylists makes recyclerview management a lot easier.
        // since in rv we need to access elements by position, we copy
        // device reference into a brand new arraylist.
        // HashMap is preferred when accessing data by id, such as during MQTT updates.
        rvDevices = ArrayList()

        // display message if no devices are registered to current environment.
        emptyView = findViewById(R.id.empty_view)
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this.applicationContext)


        // if first start, set default user and topic
        if (sharedPreferences.getBoolean("first-start", true)) {
            sharedPreferences.edit().putString(Const.MQTT_BROKER_C, Secrets.DEFAULT_SERVER)
                    .putString(Const.MQTT_PORT_C, Secrets.DEFAULT_PORT)
                    .putString(Const.MQTT_USER_C, Secrets.DEFAULT_USER)
                    .putString(Const.MQTT_PASS_C, Secrets.DEFAULT_PASS)
                    .putBoolean("first-start", false)
                    .apply()
        }
        val MQTT_SERVER = sharedPreferences.getString(Const.MQTT_BROKER_C, Secrets.DEFAULT_SERVER)
        val MQTT_PORT = sharedPreferences.getString(Const.MQTT_PORT_C, Secrets.DEFAULT_PORT)
        val MQTT_USERNAME = sharedPreferences.getString(Const.MQTT_USER_C, Secrets.DEFAULT_USER)
        val MQTT_PASSWORD = sharedPreferences.getString(Const.MQTT_PASS_C, Secrets.DEFAULT_PASS)

        MQTT_TOPIC = sharedPreferences.getString(Const.MQTT_TOPIC_C, null).toString()
        MAX_POWER = sharedPreferences.getString("emon_max_power", "3300")!!.toInt() + 700
        EMON_NOTIFICATION_ACTIVE = sharedPreferences.getBoolean("emon_notification", true)
        HEAT_NOTIFICATION_ACTIVE = sharedPreferences.getBoolean("heat_notification_check", true)
        HEAT_NOTIFICATION_TIME = sharedPreferences.getString("heat_notification_time", "22:00")!!

        // MQTT Wrapper setup
        deviceHandler = DeviceHandler(
                applicationContext, String.format("tcp://%s:%s", MQTT_SERVER, MQTT_PORT),
                MQTT_USERNAME,
                MQTT_PASSWORD!!,
                devices,
            MQTT_TOPIC,
                updateDeviceCallback
        )

        // Start MQTT for selected home
        deviceHandler.connect()
        val llm = LinearLayoutManager(this)
        refreshLayout = findViewById(R.id.swipeRefresh)
        refreshLayout.setColorSchemeColors(resources.getColor(R.color.teal_700, theme))
        refreshLayout.setOnRefreshListener {
            refreshDevices()
            refreshLayout.isRefreshing = false
        }

        // set up recyclerview to show devices
        // RVadapter setup
        val rv = findViewById<RecyclerView>(R.id.rv)
        adapter = RVAdapter(rvDevices, MAX_POWER, rv, updatesCallback, this)
        (rv.itemAnimator as SimpleItemAnimator?)!!.supportsChangeAnimations = false
        rv.setHasFixedSize(false)
        rv.layoutManager = llm
        rv.adapter = adapter
        val ith = ItemTouchHelper(touchHelperCallback)
        ith.attachToRecyclerView(rv)

        // restore cached devices -> run on new thread, heavy operation
        Thread {
            dbHelper = DatabaseHelper.getInstance(applicationContext)
            db = dbHelper?.writableDatabase
            deviceFactory()
            addTopicToDB(MQTT_TOPIC)
            runOnUiThread {
                if (rvDevices.isEmpty()) emptyView.visibility = View.VISIBLE
                adapter.notifyDataSetChanged()
            }
        }.start()
        firebaseSetup()
        nfcSetup()
        sharedPreferences.registerOnSharedPreferenceChangeListener(prefListener)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        intent?.let { setIntent(it) }
        handleNfcTag(intent!!)
    }

    override fun onDestroy() {
        super.onDestroy()
        // keep MQTT and db open as long as possible
        deviceHandler.close()
        dbHelper!!.close()
        db!!.close()
    }

    override fun onPause() {
        super.onPause()
        nfcAdapter!!.disableForegroundDispatch(this)
    }

    override fun onResume() {
        super.onResume()
        nfcAdapter!!.enableForegroundDispatch(this, pendingIntent, null, null)
        dbHelper = DatabaseHelper.getInstance(applicationContext)
        db = dbHelper?.writableDatabase
    }
    //Here starts database stuff
    /** Restore previous devices: if a device is not online, we'll wait for it
     * !!!!!!!!!!!!  DO NOT UPDATE UI HERE, RUN BY NON UI THREAD   !!!!!!!!!!!
     */
    private fun deviceFactory() {
        val cursor: Cursor = try {
            db!!.query(
                    DatabaseHelper.DEVICE_TABLE,
                    null, DatabaseHelper.DEVICE_HOUSE + " = ?", arrayOf(MQTT_TOPIC),
                    null,
                    null,
                    null)
        } catch (e: IllegalStateException) {
            Snackbar.make(window.decorView, R.string.generic_connection_error_message, Snackbar.LENGTH_SHORT).show()
            return
        }
        while (cursor.moveToNext()) {
            val devID = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.DEVICE_id))
            val devType = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.DEVICE_TYPE))
            val devName = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.DEVICE_NAME))
            val homeID = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.DEVICE_HOUSE))
            val pos = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.RV_POSITION))
            Log.d("DATABASE", devID.toString())
            when (devType) {
                Const.THERMOSTAT -> {
                    val t: Device = Thermostat(devID, devType, deviceHandler, homeID, devName)
                    devices[devID] = t
                    rvDevices.add(t)
                }
                Const.ENERGY_MONITOR -> {
                    val e: Device = EnergyMonitor(devID, devType, deviceHandler, homeID, devName)
                    devices[devID] = e
                    rvDevices.add(e)
                }
                Const.REMOTE_SWITCH -> {
                    val r: Device = RemoteSwitch(devID, devType, deviceHandler, homeID, devName)
                    devices[devID] = r
                    rvDevices.add(r)
                }
            }
            devices[devID]!!.description = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.DEVICE_INFO))
            devices[devID]!!.viewPosition = pos
        }
        cursor.close()
        Collections.sort(rvDevices)
    }

    /** called when new house is added from main  */
    private fun addTopicToDB(topic: String) {
        val values = ContentValues()
        values.put(DatabaseHelper.HOUSE_ID, topic)
        db!!.insert(DatabaseHelper.HOUSES_TABLE, null, values)
    }

    /** Called when new device detected  */
    private fun addDeviceToDB(id: Int) {
        val n = devices[id]
        val values = ContentValues()
        assert(n != null)
        values.put(DatabaseHelper.DEVICE_id, n!!.id)
        values.put(DatabaseHelper.DEVICE_NAME, n.name)
        values.put(DatabaseHelper.DEVICE_INFO, null as ByteArray?)
        values.put(DatabaseHelper.DEVICE_HOUSE, n.home_id)
        values.put(DatabaseHelper.DEVICE_TYPE, n.type)
        values.put(DatabaseHelper.RV_POSITION, n.viewPosition)
        values.put(DatabaseHelper.DEVICE_HOUSE, MQTT_TOPIC)
        db!!.insert(DatabaseHelper.DEVICE_TABLE, null, values)
    }

    /** Called when device is moved  */
    private fun updateDevicePosition(id: Int, pos: Int) {
        val n = devices[id]
        val values = ContentValues()
        try {
            assert(n != null)
        } catch (e: AssertionError) {
            return
        }
        values.put(DatabaseHelper.DEVICE_id, n!!.id)
        values.put(DatabaseHelper.DEVICE_NAME, n.name)
        values.put(DatabaseHelper.DEVICE_INFO, null as ByteArray?)
        values.put(DatabaseHelper.DEVICE_HOUSE, n.home_id)
        values.put(DatabaseHelper.DEVICE_TYPE, n.type)
        values.put(DatabaseHelper.RV_POSITION, pos)
        db!!.update(DatabaseHelper.DEVICE_TABLE, values,
                DatabaseHelper.DEVICE_id + " = ?", arrayOf(id.toString()))
    }

    private fun deleteDatabaseEntry(id: Int) {
        db!!.delete(DatabaseHelper.DEVICE_TABLE, DatabaseHelper.DEVICE_id + "= ?", arrayOf(id.toString()))
    }

    // distinct would be unnecessary since we're querying on primary keys
    private val topicsAsStringArray: Array<String?>?
        get() =// distinct would be unnecessary since we're querying on primary keys
            try {
                val cursor = db!!.query(
                        DatabaseHelper.HOUSES_TABLE, arrayOf(DatabaseHelper.HOUSE_ID),
                        null,
                        null,
                        null,
                        null,
                        null)
                val res = arrayOfNulls<String>(cursor.count)
                while (cursor.moveToNext()) {
                    res[cursor.position] = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.HOUSE_ID))
                }
                cursor.close()
                res
            } catch (e: IllegalStateException) {
                null
            }

    private fun firebaseSetup() {

        // Check if we need notification channels ->
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
                applicationContext
                        .resources
                        .getString(R.string.default_notification_channel_id),
                "Dome Notifications",
                NotificationManager.IMPORTANCE_DEFAULT)
        notificationManager.createNotificationChannel(channel)

        // Firebase messages topic is the same as the MQTT house topic for coherence.
        // New devices will subscribe to notification topics from firebase as well as mqtt.
        // We don't have to hang on with changing tokens etc.
        FirebaseMessaging.getInstance().subscribeToTopic(MQTT_TOPIC)
    }

    /**
     * Check if current instance of dome has to receive alert notifications
     * Notification subscription is done via MQTT, publishing our intention to
     * dome/firebase-requests topic.
     */
    private fun subscribeToNotification() {
        if (EMON_NOTIFICATION_ACTIVE) {
            val jsonObject = JsonObject()
            jsonObject.addProperty("notification-type", "energy-monitor")
            jsonObject.addProperty("house-id", MQTT_TOPIC)
            jsonObject.addProperty("threshold", MAX_POWER)
            deviceHandler.publish("dome/firebase-requests", jsonObject.toString(), 2)
            FirebaseMessaging.getInstance().subscribeToTopic("dome-$MQTT_TOPIC-energy-monitor")
        }
        if (HEAT_NOTIFICATION_ACTIVE) {
            val jsonObject1 = JsonObject()
            jsonObject1.addProperty("notification-type", "thermostat")
            jsonObject1.addProperty("house-id", MQTT_TOPIC)
            jsonObject1.addProperty("threshold", HEAT_NOTIFICATION_TIME)
            deviceHandler.publish("dome/firebase-requests", jsonObject1.toString(), 2)
            FirebaseMessaging.getInstance().subscribeToTopic("dome-$MQTT_TOPIC-thermostat")
        }
    }

    /**
     * Create new Dome context when application is first started and db empty
     * @return String with new topic
     */
    private fun createNewDomeTopic(): String {
        return "dome-" + (Math.random() * 10000).toInt()
    }

    /**
     * Provides simple tactile feedback when something relevant occurs
     */
    private fun longClickFeedback() {
        val v = applicationContext.getSystemService(VIBRATOR_SERVICE) as Vibrator
        v.vibrate(VibrationEffect.createOneShot(50, 255))
    }

    private fun errorClickFeedback() {
        val v = applicationContext.getSystemService(VIBRATOR_SERVICE) as Vibrator
        v.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 50, 100, 50, 100), -1))
    }


    private fun refreshDevices() {
        longClickFeedback()
        Snackbar.make(window.decorView, "Updating...", Snackbar.LENGTH_LONG).show()
        deviceHandler.close()
        deviceHandler.houseTopic = "dome/$MQTT_TOPIC"


        devices.clear()
        rvDevices.clear()
        adapter.notifyDataSetChanged()

        Thread {
            deviceFactory()
            runOnUiThread {
                if (rvDevices.isEmpty())
                    emptyView.visibility = View.VISIBLE
                else
                    emptyView.visibility = View.GONE
            }
        }.start()
        deviceHandler.connect()
    }

    // nfc from main activity, very similar to nfcreadactivity.java
    private fun nfcSetup() {
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        pendingIntent = PendingIntent.getActivity(
                this,
                0,
                Intent(this,
                        this.javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0)
    }

    private fun handleNfcTag(intent: Intent) {
        val tag = intent.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG)
        try {
            assert(tag != null)
        } catch (e: AssertionError) {
            return
        }
        val ndef = Ndef.get(tag)
        val records: Array<NdefRecord> = try {
            val msg = ndef.cachedNdefMessage
            msg.records
        } catch (e: Exception) {
            runOnUiThread {
                Snackbar.make(
                        window.decorView,
                        getString(R.string.error_reading_tag_sb),
                        Snackbar.LENGTH_SHORT).show()
            }
            return
        }
        val decoded = String(records[0].payload, StandardCharsets.UTF_8)
        val TAG = "NFC"
        Log.d(TAG, decoded)
        val text = decoded.substring(3)
        val readData = gson.fromJson(text, JsonObject::class.java)
        try {
            if (readData[Const.MQTT_TOPIC_C].asString != MQTT_TOPIC) {
                PreferenceManager
                        .getDefaultSharedPreferences(this.applicationContext)
                        .edit()
                        .putString(Const.MQTT_TOPIC_C, readData[Const.MQTT_TOPIC_C].asString)
                        .putString(Const.MQTT_USER_C, readData[Const.MQTT_USER_C].asString)
                        .putString(Const.MQTT_PASS_C, readData[Const.MQTT_PASS_C].asString)
                        .putString(Const.MQTT_BROKER_C, readData[Const.MQTT_BROKER_C].asString)
                        .putString(Const.MQTT_PORT_C, readData[Const.MQTT_PORT_C].asString)
                        .apply()
                addTopicToDB(readData[Const.MQTT_TOPIC_C].asString)
                refreshDevices()
            }
        } catch (e: Exception) {
            //same reason of nfcReadActivity
            runOnUiThread {
                Snackbar.make(
                        window.decorView,
                        getString(R.string.error_reading_tag_sb),
                        Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    companion object {
        @JvmField
        val devices = HashMap<Int, Device>()
    }
}