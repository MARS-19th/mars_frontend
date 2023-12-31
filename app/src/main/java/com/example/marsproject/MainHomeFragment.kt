package com.example.marsproject

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.VideoView
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.example.marsproject.databinding.FragmentMainHomeBinding
import java.net.UnknownServiceException

class MainHomeFragment : Fragment() {
    private lateinit var binding: FragmentMainHomeBinding
    private lateinit var savedname: String // 저장된 닉네임
    private var name: String = "닉네임" // 닉네임
    private var id: String = "아이디" // 아이디
    private var objective: String = "목표" // 상세 목표
    private var title: String = "칭호" // 칭호
    private lateinit var profile: String // 프로필
    private var life: Int = 0 // 목숨
    private lateinit var money: String // 재화
    private lateinit var level: String // 레벨
    private var progress: Double = 0.0 // 진행률

    private lateinit var videoView: VideoView
    @SuppressLint("ResourceType")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentMainHomeBinding.inflate(inflater)
        videoView = binding.root.findViewById(R.id.videoView)
        // 닉네임 정보 불러오기
        savedname = (activity as MainActivity).getName()

        // 유저 데이터 불러오는 쓰레드 생성
        val HomeThread = Thread {
            try {
                val jsonObject = Request().reqget("http://dmumars.kro.kr/api/getuserdata/${savedname}") //get요청

                name = jsonObject.getString("user_name") // 닉네임
                id = jsonObject.getString("user_id") // 아이디
                objective = jsonObject.getString("choice_mark") // 상세 목표
                title = jsonObject.getString("user_title") // 칭호
                profile = jsonObject.getString("profile_local") // 프로필
                life = jsonObject.getInt("life") // 목숨
                money = jsonObject.getInt("money").toString() // 재화
                level = jsonObject.getInt("level").toString() // 레벨
            } catch (e: UnknownServiceException) {
                // API 사용법에 나와있는 모든 오류응답은 여기서 처리
                println(e.message)
                // 이미 reqget() 메소드에서 파싱 했기에 json 형태가 아닌 value 만 저장 된 상태 만약 {err: "type_err"} 인데 e.getMessage() 는 type_err만 반환
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        HomeThread.start() // 쓰레드 시작
        HomeThread.join() // 쓰레드 종료될 때까지 대기

        // 유저 데이터 불러오는 쓰레드 생성 video
        val videoThread = Thread {
            try {
                val jsonObject =
                    Request().reqget("http://dmumars.kro.kr/api/getuseravatar/${savedname}") // get 요청
                val color = jsonObject.getString("color")
                val emotion = jsonObject.getString("look")
                val item = jsonObject.optInt("moun_shop", -1)

                val videoName  = if (item >= 1) {
                    val item = item.toString()
                    val emotion = emotion.toString()
                    val color = color.toString()
                    "${color}_${emotion}_${item}"
                } else {
                    "${color}_${emotion}"
                }
                // 비디오 리소스 이름을 사용하여 리소스 ID 가져오기
                val resID = resources.getIdentifier(videoName, "raw", requireActivity().packageName)

                // 리소스 ID를 사용하여 비디오 경로 생성
                val videoPath = "android.resource://${requireActivity().packageName}/$resID"

                // UI 업데이트를 위해 메인 스레드에서 실행
                // 비디오뷰에 비디오 설정
                videoView.setVideoURI(Uri.parse(videoPath))


                // 비디오 재생 준비 완료 이벤트 처리
                videoView.setOnPreparedListener { mp ->

                    // 비디오 재생 시작
                    videoView.start()
                }

                // 비디오 재생이 끝나면 반복 재생
                videoView.setOnCompletionListener { mp ->
                    // 비디오 재생 다시 시작
                    videoView.start()

                }

            } catch (e: UnknownServiceException) {
                // API 사용법에 나와있는 모든 오류응답은 여기서 처리
                println(e.message)
                // 이미 reqget() 메소드에서 파싱 했기에 json 형태가 아닌 value 만 저장 된 상태 만약 {err: "type_err"} 인데 e.getMessage() 는 type_err만 반환
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        videoThread.start() // 쓰레드 시작
        videoThread.join() // 쓰레드 종료될 때까지 대기

        // 목표 진행률 불러오는 쓰레드 생성
        val progressThread = Thread {
            try {
                val jsonObject = Request().reqget("http://dmumars.kro.kr/api/getuserskill/${savedname}") //get요청

                progress = jsonObject.getJSONArray("results").length() * 11.1 // 완료한 목표의 수만큼 증가
                if(jsonObject.getJSONArray("results").length() == 9) progress = 100.0
                Log.d("목표", progress.toString())
            } catch (e: UnknownServiceException) {
                // API 사용법에 나와있는 모든 오류응답은 여기서 처리
                println(e.message)
                // 이미 reqget() 메소드에서 파싱 했기에 json 형태가 아닌 value 만 저장 된 상태 만약 {err: "type_err"} 인데 e.getMessage() 는 type_err만 반환
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        progressThread.start() // 쓰레드 시작
        progressThread.join() // 쓰레드 종료될 때까지 대기


        // 유저 데이터 변경
        Log.d("ddsfdfddfdfdsdfasadsf", title)
        changeUserData(title, name, id, objective, life, progress.toInt())

        // 기존 프로필 사진 디스크 케쉬 지우기
        Thread {
            Glide.get(requireActivity()).clearDiskCache()
        }.start()

        // 서버에서 프사 이미지 가져와서 profileImage에 적용하기
        Glide.with(this)
            .load("http://dmumars.kro.kr/api/getprofile/${savedname}")
            .placeholder(Color.parseColor("#00000000"))
            .error(R.drawable.profileimage)
            .skipMemoryCache(true)
            .into(binding.profileImage)

        // 클릭 시 친구 찾기로 이동하는 리스너
        binding.searchpeople.setOnClickListener {
            activity?.let{
                // 인텐트 생성 후 액티비티 생성
                val intent = Intent(context, SearchPeopleActivity::class.java) // 주변 친구 찾기 페이지로 설정
                startActivity(intent) // 액티비티 생성
            }
        }


        // 서버에서 프사 이미지 가져와서 profileImage에 적용하기
        Glide.with(this)
            .load("http://dmumars.kro.kr/api/getprofile/${savedname}")
            .placeholder(Color.parseColor("#00000000"))
            .error(R.drawable.profileimage)
            .skipMemoryCache(true)
            .into(binding.profileImage)

        // 클릭 시 목표 프래그먼트로 전환하는 리스너
        binding.objectiveText.setOnClickListener{
            (activity as MainActivity).clickchangeFragment(1) // 목표 프래그먼트로 전환
        }
        binding.progressBar.setOnClickListener{
            (activity as MainActivity).clickchangeFragment(1) // 목표 프래그먼트로 전환
        }

        return binding.root
    }

    // 유저 데이터 변경해주는 함수
    private fun changeUserData(title: String, name: String, id: String, objective: String, life: Int, progress: Int) {
        binding.titleText.text = title // 칭호 텍스트 변경
        binding.userNameText.text = name // 닉네임 텍스트 변경
        binding.userIdText.text = id // 회원 아이디 텍스트 변경
        binding.objectiveText.text = objective // 상세 목표 텍스트 변경

        /*// 목숨 수에 따른 이미지 활성화
        when(life) {
            3 -> {
                binding.lifeImage1.visibility = View.VISIBLE // 활성화
                binding.lifeImage2.visibility = View.VISIBLE // 활성화
                binding.lifeImage3.visibility = View.VISIBLE // 활성화
            }
            2 -> {
                binding.lifeImage1.visibility = View.VISIBLE // 활성화
                binding.lifeImage2.visibility = View.VISIBLE // 활성화
                binding.lifeImage3.visibility = View.INVISIBLE // 비활성화
            }
            1 -> {
                binding.lifeImage1.visibility = View.VISIBLE // 활성화
                binding.lifeImage2.visibility = View.INVISIBLE // 비활성화
                binding.lifeImage3.visibility = View.INVISIBLE // 비활성화
            }
        }*/
        binding.progressBar.progress = progress // 목표 진행률 변경
    }
}