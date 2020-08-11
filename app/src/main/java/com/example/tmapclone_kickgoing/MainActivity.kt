package com.example.tmapclone_kickgoing

import android.Manifest
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.PointF
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.*
import com.google.firebase.database.ktx.getValue
import com.skt.Tmap.*
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.app_bar_main.*
import org.techtown.mapservicewithtmapapi.GetWeatherTask
import org.techtown.mapservicewithtmapapi.MySingleClickListener
import org.techtown.mapservicewithtmapapi.MylongClickListener


class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private var isLocation = true
    private var actionBar = false
    private lateinit var tMapView:TMapView

    //var currentUser: FirebaseAuth = Activity_login.auth

    private var myLongitude: Double = 0.0 // 현재위치, 경도
    private var myLatitude: Double = 0.0   // 현재위치, 위도
    var data = HashMap<String, TMapMarkerItem>() // 킥고잉 마커명, 위치 저장 해시맵
    var pDistance:makedDistanceEvent = makedDistanceEvent()
    var n=0

    private val GPS_ENABLE_REQUEST_CODE = 2001
    private val PERMISSIONS_REQUEST_CODE = 100
    var REQUIRED_PERMISSIONS = arrayOf<String>(android.Manifest.permission.ACCESS_FINE_LOCATION)


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_baseline_format_list_bulleted_24)


        //val Rlayout:RelativeLayout = findViewById(R.id.map_view) as RelativeLayout
        tMapView = TMapView(this)
        tMapView!!.setSKTMapApiKey("l7xx6974090fe01b4f089324f17b6e5be7f3")
//        map_view.addView(tMapView)  //레이아웃에 Tmap 추가
        tMapView!!.setIconVisibility(true)
        tMapView!!.setCompassMode(true)
        tMapView!!.setSightVisible(true)

        //
        var markerArr = ArrayList<TMapMarkerItem>()     // Marker 목록
        var tMapPolyLineOfCourse = TMapPolyLine()   // 길찾기 기능 수행시 반환되는 PolyLine
        var pointArr = ArrayList<TMapPoint>()   // 출발지, 목적지를 가리키는 TMapPoint
        pointArr.add(TMapPoint(0.0, 0.0))   // 출발지, 목적지를 가리키는 ArrayList 초기화(2칸) 동시에 기본 값 할당
        pointArr.add(TMapPoint(0.0, 0.0))   // 출발지, 목적지를 가리키는 ArrayList 초기화(2칸) 동시에 기본 값 할당

        var myListener = MylongClickListener(tMapView, pointArr, markerArr, this)   // 리스너 생성
        tMapView.setOnCalloutRightButtonClickListener(myListener)       // 리스너 등록
        tMapView.setOnLongClickListenerCallback(myListener)     // 리스너 등록
//        tMapView.setOnClickListenerCallBack(MySingleClickListener(findLoad))   // 리스너 생성 및 등록
        map_view.addView(tMapView)

        findLoad.setOnClickListener {
            //인텐트 교환
            val myIntent = Intent(this, FindLoad::class.java)
            var dialog = android.app.AlertDialog.Builder(this)
            var markerItems = arrayOf<String>("지정한 마커를 통한 주소 검색", "사용자 입력을 통한 검색")
            var markerMenuListener = object : DialogInterface.OnClickListener {
                override fun onClick(dialog: DialogInterface?, which: Int) {
                    when (which) {
                        0 -> {  // 마커로 지정해둔 곳 기준으로 길찾기
                            GetWeatherTask(map_view, tMapView, pointArr, tMapPolyLineOfCourse).execute()
                        }
                        1 -> {  // 사용자 입력을 받아 길찾기 > 인텐트 교환
                            myIntent.putExtra("key","0")
                            startActivityForResult(myIntent, 0)
                        }
                    }
                }
            }
            dialog.setItems(markerItems, markerMenuListener)        // 다이얼로그 메뉴 아이템 설정
            dialog.show()       // 다이얼로그 메뉴 보여주기

        }
        //


        nav_view.setNavigationItemSelectedListener(this)

        val headerView = nav_view.getHeaderView(0)
        var name: TextView = headerView.findViewById(R.id.name)
        var email: TextView = headerView.findViewById(R.id.email)

        //name.text = currentUser.currentUser?.displayName

        setGps()

        val database: FirebaseDatabase = FirebaseDatabase.getInstance()
        val kickGoingRef: DatabaseReference = database.getReference("marker_list") // DB에 marker_list에 접근
        kickGoingRef.addValueEventListener(object: ValueEventListener{
            override fun onCancelled(error: DatabaseError) {
            }
            override fun onDataChange(snapshot: DataSnapshot) {
                for(kickgoingSnapshot in snapshot.children){
                    var lat = kickgoingSnapshot.child("latitude").getValue<Double>()
                    var lon = kickgoingSnapshot.child("longitude").getValue<Double>()
                    var model = kickgoingSnapshot.child("model").getValue<String>()
                    n++

                    var kickgoingPoint: TMapPoint = TMapPoint(lat!!, lon!!)
                    var marker: TMapMarkerItem = TMapMarkerItem()
                    marker.tMapPoint = kickgoingPoint
                    marker.visible = TMapMarkerItem.VISIBLE
                    marker.canShowCallout = true
                    marker.calloutTitle=model


                    data.put("marker${n}", marker) // HashMap에 데이터 삽입
                    // 현재위치와 각 킥고잉들의 거리 계산
                    // CalcDistance = makedDistanceEvent.kt 클래스의 메소드
                    var dist = pDistance.CalcDistance(myLatitude, myLongitude, marker.tMapPoint.latitude, marker.tMapPoint.longitude)
                    marker.calloutSubTitle = "${dist}m"
                    tMapView!!.addMarkerItem("marker${n}", marker)
                }
                tMapView!!.setCompassMode(true)
                tMapView!!.invalidate() // 2020-07-30(목) 작성
            }
        })



//        tMapViewEventListener()
        clickListener()     //각종 버튼들의 클릭 리스너
    }   //end of onCreate()

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        val result : ArrayList<Double>?     // 출발지와 목적지에 대한 위경도 값을 받아올 공간
        if (requestCode == 0) {
            result = data!!.getSerializableExtra("Position") as ArrayList<Double>   // 출발지와 목적지에 대한 위경도 값을 받아옴
            var temp1 = TMapPoint(result[1], result[0])     // 받아온 출발지 위경도로 TMapPoint 생성
            var temp2 = TMapPoint(result[3], result[2])     // 받아온 목적지 위경도로 TMapPoint 생성
            var tempArr = ArrayList<TMapPoint>()        // 쓰레드 클래스 매개변수로 넣을 배열 생성
            tempArr.add(temp1)  // 값 넣어주기
            tempArr.add(temp2)  // 값 넣어주기
            var tmapPolyLine = TMapPolyLine()       // 쓰레드 클래스 매개변수로 사용될 폴리라인 생성

            GetWeatherTask(this.map_view, tMapView, tempArr, tmapPolyLine).execute() // 쓰레드 클래스 생성 및 수행
        }
        when (requestCode) {
            GPS_ENABLE_REQUEST_CODE ->
                //사용자가 GPS 활성 시켰는지 검사
                if (checkLocationServicesStatus()) {
                    if (checkLocationServicesStatus()) {
                        Log.d("@@@", "onActivityResult : GPS 활성화 되있음")
                        checkRunTimePermission()
                        return
                    }
                }
        }

    }
    override fun onStart() {
        super.onStart()

    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item?.itemId){
            android.R.id.home -> {
                drawer_layout.openDrawer(GravityCompat.START)
            }
        }
        return super.onOptionsItemSelected(item)
    }

//
//    private fun tMapViewEventListener(){
//        tMapView!!.setOnClickListenerCallBack(object : TMapView.OnClickListenerCallback {
//            override fun onPressEvent(
//                p0: ArrayList<TMapMarkerItem>?,
//                p1: ArrayList<TMapPOIItem>?,
//                p2: TMapPoint?,
//                p3: PointF?
//            ): Boolean {
//                // tmapView!!.setTrackingMode(true)
//                tMapView!!.setCompassMode(true)
//                tMapView!!.setSightVisible(true)
//                tMapView!!.invalidate()
//                return false
//            }
//
//            override fun onPressUpEvent(
//                p0: ArrayList<TMapMarkerItem>?,
//                p1: ArrayList<TMapPOIItem>?,
//                p2: TMapPoint?,
//                p3: PointF?
//            ): Boolean {
//                //tmapView!!.setTrackingMode(true)
//                tMapView!!.setCompassMode(true)
//                tMapView!!.setSightVisible(true)
//                tMapView!!.invalidate()
//                return false
//            }
//        })
//    }


    private val mLocationListener: LocationListener = object : LocationListener {
        override fun onLocationChanged(location: Location?) {
            if (location != null) {
                myLongitude = location.longitude
                myLatitude = location.latitude

                tMapView!!.setLocationPoint(myLongitude, myLatitude)
                tMapView!!.setCenterPoint(myLongitude, myLatitude)
                tMapView!!.setTrackingMode(true)
            }
        }

        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {

        }

        override fun onProviderEnabled(provider: String?) {

        }

        override fun onProviderDisabled(provider: String?) {

        }
    } // end of Listener

    // 2020-07-29 작성
    // 현재위치를 위한 gps를 받아오는 함수
    fun setGps() {
        val lm = this.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ),
                1
            )
        }
        lm.requestLocationUpdates(
            LocationManager.NETWORK_PROVIDER,  // 등록할 위치제공자(실내에선 NETWORK_PROVIDER 권장)
            1000, 1f,  // 통지사이의 최소 변경거리 (m)
            mLocationListener
        )
    }// end of setGps()

    private fun clickListener(){
        myLocation.setOnClickListener {
            if(isLocation){
                tMapView!!.setTrackingMode(false)
                tMapView!!.setSightVisible(false)
                tMapView!!.setCompassMode(false)
                tMapView!!.invalidate()
                isLocation = false
                myLocation.setImageResource(R.drawable.ic_baseline_my_location_24_black)
            }
            else{
                tMapView!!.setTrackingMode(true)
                tMapView!!.setSightVisible(true)
                tMapView!!.setCompassMode(true)
                isLocation = true
                myLocation.setImageResource(R.drawable.ic_baseline_my_location_24)
            }
        }

        //zoomIn버튼 클릭시 지도 확대
        zoomIn.setOnClickListener {
            tMapView?.MapZoomIn()
        }
        //zoomOut버튼 클릭시 지도 축소
        zoomOut.setOnClickListener {
            tMapView?.MapZoomOut()
        }

        //길찾기 버튼 클릭시 화면 전환
//        findLoad.setOnClickListener {
//            var intent = Intent(this, FindLoad::class.java)
//            startActivity(intent)
//        }

        tMapView!!.setOnClickListenerCallBack(object: TMapView.OnClickListenerCallback{
            override fun onPressEvent(
                p0: java.util.ArrayList<TMapMarkerItem>?,
                p1: java.util.ArrayList<TMapPOIItem>?,
                p2: TMapPoint?,
                p3: PointF?
            ): Boolean {
                actionBar = true
                tMapView!!.setTrackingMode(false)
                tMapView!!.setSightVisible(false)
                tMapView!!.setCompassMode(false)
                tMapView!!.invalidate()
                isLocation = false
                myLocation.setImageResource(R.drawable.ic_baseline_my_location_24_black)
                return false
            }

            override fun onPressUpEvent(
                p0: java.util.ArrayList<TMapMarkerItem>?,
                p1: java.util.ArrayList<TMapPOIItem>?,
                p2: TMapPoint?,
                p3: PointF?
            ): Boolean {
                actionBar = false
                tMapView!!.setTrackingMode(false)
                tMapView!!.setSightVisible(false)
                tMapView!!.setCompassMode(false)
                tMapView!!.invalidate()
                isLocation = false
                myLocation.setImageResource(R.drawable.ic_baseline_my_location_24_black)
                return false
            }
        })



        var newUser: LinearLayout = View.inflate(this@MainActivity, R.layout.login_newuser, null) as LinearLayout
        var builder2:AlertDialog.Builder = AlertDialog.Builder(this@MainActivity)
        builder2.setIcon(R.drawable.kickgoing)
        builder2.setTitle("QucikGoing")
        val newUserButton = newUser.findViewById<Button>(R.id.makeUser)
        val loginButton = newUser.findViewById<Button>(R.id.login)



        //이용하기 버튼 클릭 시 로그인 여부에 따라 로그인 및 회원가입 창 띄우기
        usingButton.setOnClickListener {
            /*
                이용하기 버튼을 두번 클릭 할 경우 기존에 뷰가 남아있어서 오류가 나기때문에 기존의 Parent가 존재 한다면 기존의 뷰를 제거해줘야한다.
            */
            if (newUser.getParent() != null)
                (newUser.getParent() as ViewGroup).removeView(newUser)

            builder2.setView(newUser).setNegativeButton("닫기") { dialogInterface, i ->
                dialogInterface.dismiss()
            }.show()
        } // end of usingButton Listener

        /*
            다이얼로그에 나온 로그인/회원가입 선택 버튼을 눌렀을 경우의 리스너
        */

        newUserButton.setOnClickListener {// 다이얼로그 상자에 회원가입 버튼 누를 시 회원가입 페이지로 인텐트 전환
            val intent: Intent = Intent(this, RegisterEvent::class.java)
            startActivity(intent)
        }

        loginButton.setOnClickListener {
           val intent: Intent = Intent(this, Activity_login::class.java)
            startActivity(intent)
            builder2.setView(null)
        }   //end of loginButton Listener

    }   //end of clickListener
    


    override fun onResume() {
        super.onResume()

        if (!checkLocationServicesStatus()) {
            showDialogForLocationServiceSetting();
        } else {
            checkRunTimePermission();
        }
    }
    override fun onDestroy() {
        super.onDestroy()
        tMapView?.setTrackingMode(false)
        tMapView?.setMarkerRotate(false)
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSIONS_REQUEST_CODE && grantResults.size == REQUIRED_PERMISSIONS.size) {

            // 요청 코드가 PERMISSIONS_REQUEST_CODE 이고, 요청한 퍼미션 개수만큼 수신되었다면
            var check_result = true


            // 모든 퍼미션을 허용했는지 체크합니다.
            for (result in grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    check_result = false
                    break
                }
            }
            if (check_result) {
                Log.d("@@@", "start")
                //위치 값을 가져올 수 있음
                tMapView?.setTrackingMode(true)
            } else {
                // 거부한 퍼미션이 있다면 앱을 사용할 수 없는 이유를 설명해주고 앱을 종료합니다.2 가지 경우가 있습니다.
                if (ActivityCompat.shouldShowRequestPermissionRationale(
                        this,
                        REQUIRED_PERMISSIONS[0]
                    )
                ) {
                    Toast.makeText(
                        this@MainActivity,
                        "퍼미션이 거부되었습니다. 앱을 다시 실행하여 퍼미션을 허용해주세요.",
                        Toast.LENGTH_LONG
                    ).show()
                    finish()
                } else {
                    Toast.makeText(
                        this@MainActivity,
                        "퍼미션이 거부되었습니다. 설정(앱 정보)에서 퍼미션을 허용해야 합니다. ",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun checkRunTimePermission() {
        //런타임 퍼미션 처리
        // 1. 위치 퍼미션을 가지고 있는지 체크합니다.

        //런타임 퍼미션 처리
        // 1. 위치 퍼미션을 가지고 있는지 체크합니다.
        val hasFineLocationPermission = ContextCompat.checkSelfPermission(
            this@MainActivity,
            android.Manifest.permission.ACCESS_FINE_LOCATION
        )


        if (hasFineLocationPermission == PackageManager.PERMISSION_GRANTED) {
            // 2. 이미 퍼미션을 가지고 있다면
            // ( 안드로이드 6.0 이하 버전은 런타임 퍼미션이 필요없기 때문에 이미 허용된 걸로 인식합니다.)
            // 3.  위치 값을 가져올 수 있음
            tMapView?.setTrackingMode(true)
        } else {  //2. 퍼미션 요청을 허용한 적이 없다면 퍼미션 요청이 필요합니다. 2가지 경우(3-1, 4-1)가 있습니다.

            // 3-1. 사용자가 퍼미션 거부를 한 적이 있는 경우에는
            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    this@MainActivity,
                    REQUIRED_PERMISSIONS.get(0)
                )
            ) {

                // 3-2. 요청을 진행하기 전에 사용자가에게 퍼미션이 필요한 이유를 설명해줄 필요가 있습니다.
                Toast.makeText(this@MainActivity, "이 앱을 실행하려면 위치 접근 권한이 필요합니다.", Toast.LENGTH_LONG)
                    .show()
                // 3-3. 사용자게에 퍼미션 요청을 합니다. 요청 결과는 onRequestPermissionResult에서 수신됩니다.
                ActivityCompat.requestPermissions(
                    this@MainActivity, REQUIRED_PERMISSIONS,
                    PERMISSIONS_REQUEST_CODE
                )
            } else {
                // 4-1. 사용자가 퍼미션 거부를 한 적이 없는 경우에는 퍼미션 요청을 바로 합니다.
                // 요청 결과는 onRequestPermissionResult에서 수신됩니다.
                ActivityCompat.requestPermissions(
                    this@MainActivity, REQUIRED_PERMISSIONS,
                    PERMISSIONS_REQUEST_CODE
                )
            }
        }

    }

    private fun showDialogForLocationServiceSetting() {
        val builder: AlertDialog.Builder = AlertDialog.Builder(this@MainActivity)
        builder.setTitle("위치 서비스 비활성화")
        builder.setMessage(
            """앱을 사용하기 위해서는 위치 서비스가 필요합니다.
                위치 설정을 수정하실래요?
                """.trimIndent()
        )
        builder.setCancelable(true)
        builder.setPositiveButton("설정", DialogInterface.OnClickListener { dialog, id ->
            val callGPSSettingIntent =
                Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            startActivityForResult(callGPSSettingIntent, GPS_ENABLE_REQUEST_CODE)
        }).setNegativeButton("취소",
            DialogInterface.OnClickListener { dialog, _ -> dialog.cancel() })
        builder.create().show()
    }



    private fun checkLocationServicesStatus(): Boolean {
        val locationManager =
            getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER))
    }

    override fun onBackPressed() { //뒤로가기 처리
        if(drawer_layout.isDrawerOpen(GravityCompat.START)){
            drawer_layout.closeDrawers()
            // 테스트를 위해 뒤로가기 버튼시 Toast 메시지
            Toast.makeText(this,"back btn clicked",Toast.LENGTH_SHORT).show()
        } else{
            super.onBackPressed()
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when(item.itemId){
            R.id.account-> {
                val intent: Intent = Intent(this, Activity_login::class.java)
                startActivity(intent)
            }
            R.id.how-> Toast.makeText(this,"item2 clicked",Toast.LENGTH_SHORT).show()
            R.id.setting-> Toast.makeText(this,"item3 clicked",Toast.LENGTH_SHORT).show()
        }
        return false
    }
}
