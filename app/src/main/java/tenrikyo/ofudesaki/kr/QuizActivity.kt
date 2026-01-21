package tenrikyo.ofudesaki.kr

import android.app.AlertDialog
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.URL

class QuizActivity : AppCompatActivity() {

    // 데이터 관련 변수
    private val allContent = mutableListOf<ContentItem>() // ContentItem 클래스가 있다고 가정
    private lateinit var adapter: ArrayAdapter<ContentItem>

    // UI 관련 변수
    private lateinit var filterStatusLayout: LinearLayout
    private lateinit var filterStatusText: TextView
    private lateinit var returnButton: Button

    // 업데이트 관련 상수
    private val UPDATE_JSON_URL = "https://raw.githubusercontent.com/GOMPANGEDANCE/ofudesaki/refs/heads/main/version.json"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_quiz)

        // 1. 전체 화면 설정 (상태바 숨기기 등)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.hide(android.view.WindowInsets.Type.statusBars())
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_FULLSCREEN)
        }

        // 2. 데이터 로드
        loadTemporaryData()

        // 3. UI 컴포넌트 초기화
        val chaptersLayout: LinearLayout = findViewById(R.id.chaptersLayout)
        val searchEditText: EditText = findViewById(R.id.searchEditText)
        val contentListView: ListView = findViewById(R.id.contentListView)
        filterStatusLayout = findViewById(R.id.filterStatusLayout)
        filterStatusText = findViewById(R.id.filterStatusText)
        returnButton = findViewById(R.id.returnButton)

        // 4. 어댑터 연결
        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, allContent)
        contentListView.adapter = adapter

        // 5. 챕터 버튼 동적 생성 (1호~18호)
        for (i in 1..18) {
            val button = Button(this).apply {
                text = "제${i}호"
                setOnClickListener {
                    val filterText = "${i}-"
                    adapter.filter.filter(filterText)
                    filterStatusText.text = "현재 '제${i}호' 내용만 보는 중입니다."
                    filterStatusLayout.visibility = View.VISIBLE
                }
            }
            chaptersLayout.addView(button)
        }

        // 6. 필터 해제 버튼
        returnButton.setOnClickListener {
            adapter.filter.filter("")
            searchEditText.text.clear()
            filterStatusLayout.visibility = View.GONE
        }

        // 7. 검색창 리스너
        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                adapter.filter.filter(s)
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // 8. 리스트 아이템 클릭 리스너
        contentListView.setOnItemClickListener { parent, view, position, id ->
            val selectedItem = adapter.getItem(position)

            if (selectedItem != null) {
                val actualPosition = allContent.indexOf(selectedItem)

                val intent = Intent(this, ContentDetailActivity::class.java).apply {
                    putExtra("EXTRA_POSITION", actualPosition)
                    putParcelableArrayListExtra("EXTRA_CONTENT_LIST", ArrayList(allContent))
                }
                startActivity(intent)
            }
        }

        // 9. 앱 실행 시 업데이트 체크 실행
        checkUpdate()
    }

    // ==========================================
    // 여기서부터는 업데이트 관련 함수들 (onCreate 밖으로 뺌)
    // ==========================================

    private fun checkUpdate() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // 서버 JSON 읽기
                val jsonString = URL(UPDATE_JSON_URL).readText()
                val jsonObject = JSONObject(jsonString)
                val serverVersionCode = jsonObject.getInt("versionCode")
                val downloadUrl = jsonObject.getString("url")

                // 현재 버전 읽기
                val currentVersionCode = packageManager.getPackageInfo(packageName, 0).longVersionCode

                // 버전 비교
                if (serverVersionCode > currentVersionCode) {
                    withContext(Dispatchers.Main) {
                        showUpdateDialog(downloadUrl)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun showUpdateDialog(apkUrl: String) {
        AlertDialog.Builder(this)
            .setTitle("업데이트 알림")
            .setMessage("새로운 기능이 추가된 버전이 있습니다.\n지금 업데이트 하시겠습니까?")
            .setPositiveButton("업데이트") { _, _ ->
                downloadApk(apkUrl)
            }
            .setNegativeButton("나중에") { _, _ -> }
            .show()
    }

    private fun downloadApk(url: String) {
        val request = DownloadManager.Request(Uri.parse(url))
            .setTitle("앱 업데이트 다운로드 중")
            .setDescription("잠시만 기다려 주세요...")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalFilesDir(this, Environment.DIRECTORY_DOWNLOADS, "update.apk")
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(true)

        val downloadManager = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val downloadId = downloadManager.enqueue(request)

        // 다운로드 완료 리시버
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                val id = intent?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                if (id == downloadId) {
                    installApk()
                    try {
                        unregisterReceiver(this) // 리시버 해제
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }

        // 리시버 등록 (Android 13 대응)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(receiver, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE), Context.RECEIVER_EXPORTED)
        } else {
            registerReceiver(receiver, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
        }
    }

    private fun installApk() {
        val file = File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "update.apk")

        // 1. 알 수 없는 출처 허용 여부 체크 (Android 8.0 이상)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!packageManager.canRequestPackageInstalls()) {
                Toast.makeText(this, "앱 설치를 위해 권한을 허용해 주세요.", Toast.LENGTH_LONG).show()
                val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:$packageName"))
                startActivity(intent)
                return
            }
        }

        // 2. 파일이 있으면 설치 화면 띄우기
        if (file.exists()) {
            val intent = Intent(Intent.ACTION_VIEW)
            val apkUri = FileProvider.getUriForFile(
                this,
                "$packageName.provider",
                file
            )

            intent.setDataAndType(apkUri, "application/vnd.android.package-archive")
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

            startActivity(intent)
        } else {
            Toast.makeText(this, "다운로드된 파일에 문제가 있습니다.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadTemporaryData() {
        // 기존에 작성하셨던 데이터 로드 로직을 여기에 넣으세요.
        // 예: allContent.add(ContentItem(...))
    }
}
                    allContent.add(
                        ContentItem(
                            korean = " 모든 시대 온 세상 사람들을 살펴보아도\n 신의 뜻 아는 자는 전혀 없으므로 1-1\n",
                            japanese = "よろつよのせかい一れつみはらせど\nむねのハかりたものハないから",
                            english = "yorozuyo no sekai ichiretsu miharasedo\nmune no wakarita mono wa nai kara",
                            commentary = " 一. 어버이신이 이 세상을 창조한 이래\n장구한 세월이 흐르는 동안\n수없이 많은 사람들이 살아왔으나,\n어느 시대를 보아도 넓은 세상 가운데\n누구 하나 신의 뜻을 아는 사람이 없었다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 그러리라 일러준 일이 없으니\n 아무 것도 모르는 것이 무리는 아니야 1-2\n",
                            japanese = "そのはづやといてきかした事ハない\nなにもしらんがむりでないそや",
                            english = "sono hazu ya toite kisashita koto wa nai\nnanimo shiran ga muri de nai zo ya",
                            commentary = "二. 그도 그럴것이 지금까지 이 진실한 가르침을\n일러준 일이 없었으니 무리가 아니다.\n가끔 그 시대의 성현을 통해서 일러주긴 했으나.\n그것은 모두가 시의에 적합하신 신의(神意)의\n표현일 뿐 최후의 가르침은 아니다.\n그것은 아직 시순이 오지 않았기 때문에\n부득이한 것이었다. "
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이번에는 신이 이 세상에 나타나서\n 무엇이든 자세한 뜻을 일러준다 1-3\n",
                            japanese = "このたびハ神がをもていあらハれて\nなにかいさいをといてきかする",
                            english = "konotabi wa Kami ga omote i arawarete\nnanika isai o toite kikasuru",
                            commentary = "三. 그러나 드디어 이번에야말로 순각한이 도래\n했으므로 어버이신 천리왕님이 이 세상에 나타나서\n자신의 뜻을 소상히 일러줄테다.\n이번이란 1838년 10월 26일,\n순각한의 도래로 어버이신님이 교조님을 현신으로\n삼아 이 가르침을 시작하신 때를 말한다.\n신이 이 세상에 나타나서란 어버이신님이 교조님을 현신으로 삼아,\n즉 교조님의 입을 통해서 당신의 뜻을 세상 사람들에게 알리시는 것을 말한다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 신이 진좌하는 터전이라고\n 말하고 있지만 근본은 모르겠지 1-4\n",
                            japanese = "このところやまとのしバのかみがたと\nゆうていれども元ハしろまい",
                            english = "kono tokoro Yamato no Jiba no kamigata to\nyute iredomo moto wa shiromai",
                            commentary = "四. 이곳은 신이 진좌하는 터전이라고 말하고 있으나 그 근본을 모를 것이다. \n터전은 어버이신님이 태초에 인간을 잉태하신 곳. 즉 우리들 인간의 근본되는 본고장을 가리킨다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이 근본을 자세히 듣게 되면\n 어떤 자도 모두 그리워진다 1-5\n",
                            japanese = "このもとをくハしくきいた事ならバ\nいかなものでもみなこいしなる",
                            english = "kono moto o kuwashiku kiita koto naraba\nikana mono demo mina koishi naru",
                            commentary = "五. 으뜸인 터전에 신이 진좌하고 있는 그 근본을 자세히 들어서 알게 된다면 누구든 자신의 근본 고향인 터전을 그리워하게 될 것이다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 듣고 싶거든 찾아오면 일러줄 테니\n 만가지 자세한 뜻 으뜸인 인연 1-6\n",
                            japanese = "きゝたくバたつねくるならゆてきかそ\nよろづいさいのもとのいんねん",
                            english = "kiki takuba tazune kuru nara yute kikaso\nyorozu isai no moto no innen",
                            commentary = "六. 이 근본을 듣고 싶은 사람은 찾아오라. 이 세상 태초를 비롯해서 모든 리를 소상히 가르칠 테니."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 신이 나타나 무엇이든 자세한 뜻을 일러주면\n 온 세상 사람들의 마음 용솟음친다 1-7\n",
                            japanese = "かみがでてなにかいさいをとくならバ\nせかい一れつ心いさむる",
                            english = "Kami ga dete nanika isai o toku naraba\nsekai ichiretsu kokoro isamuru",
                            commentary = "七. 으뜸인 어버이신님이 이 세상에 나타나서 없던 인간을 창조한 어버이신의 수호와 구제한줄기의 길에 대해 소상히 일러주면, 세상 사람들의 마음은 이 진실한 가르침에 의해 모두 용솟음치게 될 것이다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 모든 사람들을 속히 도우려고 서두르니\n 온 세상 사람들의 마음도 용솟음쳐라 1-8\n",
                            japanese = "いちれつにはやくたすけをいそぐから\nせかいの心いさめかゝりて",
                            english = "ichiretsu ni hayaku tasuke o isogu kara\nsekai no kokoro isame kakarite",
                            commentary = "八. 누구든 차별없이 모든 사람들을 하루빨리 구제하고 싶으니 이러한 어버이신의 뜻을 잘 깨달아 세상 사람들의 마음도 용솟음치도록 하라. \n 이상의 노래는 신악가와 「팔수」와 거의 같다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 차츰차츰 마음이 용솟음치게 되면\n 온 세상 풍년 들고 곳곳마다 번창하리라 1-9\n",
                            japanese = "だん／＼と心いさんてくるならバ\nせかいよのなかところはんじよ",
                            english = "dandan to kokoro isande kuru naraba\nsekai yononaka tokoro hanjo",
                            commentary = "九. 세상 사람들의 마음이 차츰 용솟음치게 되면 번민도 고통도 없어지고 모두 서로 도와 가며 각자의 일에 힘쓰게 된다. 따라서 어버어신도 그 마음에 따라 세상 만물이 풍성하고 가업(家業)도 번영하여, 어디에 가더라도 싸움이나 시비가 없이 인류는 평화롭고 행복하게 살게 된다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 앞으로는 신악근행의 손짓을 가르쳐서\n 모두 갖추어서 근행하기를 고대한다 1-10\n",
                            japanese = "このさきハかくらづとめのてをつけて\nみんなそろふてつとめまつなり",
                            english = "konosaki wa Kagura-zutome no te o tsukete\nminna sorote Tsutome matsu nari",
                            commentary = "十, 이제부터 어버이신은 사람들에게 신악근행의 손짓을 가르칠 테니, 인원을 갖추어 근행하길 간절히 바란다.\n신악근행이란 감로대를 중심으로 태초의 십주(十住) 신님의 리를 찬양하며 열 사람이 올리는 근행으로서, 이 근행에 의해 어버이신님을 용솟음치게 하고, 제세구인(済世救人)의 수호를 기원하는 것이다.\n" +
                                    "이 근행은 또 장소의 뜻에서 감로대근행이라고 하고, 신과 인간이 함께 용솟음치므로 즐거운근행이라고도 하며, 또 구제한줄기의 근행이므로 구제근행이라고도 한다.(제6호 30수의 주석, 제10호 25～27수의 주석 및 제15호 52수의 주석 참조) 이 근행은 터전 이외에서는 허용되지 않는다.(지도말씀 1889, 3, 31 참조)"
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 모두 갖추어서 서둘러 근행을 하면\n 곁의 사람이 용솟음치면 신도 용솟음친다 1-11\n",
                            japanese = "みなそろてはやくつとめをするならバ\nそばがいさめバ神もいさむる",
                            english = "mina sorote hayaku Tsutome o suru naraba\nsoba ga isameba Kami mo isamuru",
                            commentary = "十一, 인원을 갖추어 하루라도 빨리 신악근행을 하게 되면 어버이신은 진실한 어버이이기 때문에, 자녀들은 인간이 기뻐 용솟음치는 모습을 보고 어버이신도 함께 용솟음치게 된다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 무엇이건 신의 마음 침울해지면\n  농작물도 모두 침울해진다 1-12\n",
                            japanese = "いちれつに神の心がいづむなら\nものゝりうけかみないつむなり",
                            english = "ichiretsu ni Kami no kokoro ga izumu nara\nmono no ryuke ga mina izumu nari",
                            commentary = "十二. 무릇 어버이신의 마음이 침울해지면 농작물도 저절로 생기를 잃어 충분한 수확을 하지 못하게 된다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 농작물을 침울하게 하는 마음은 안타까운 일\n 침울하지 않도록 어서 용솟음쳐라 1-13\n",
                            japanese = "りうけいのいつむ心ハきのとくや\nいづまんよふとはやくいさめよ",
                            english = "ryukei no izumu kokoro wa kinodoku ya\nizuman yo to hayaku isame yo",
                            commentary = "十三. 농작물을 충분히 여물지 않게 하는 사람들의 침울한 마음은 어버이신이 볼 때 가엾은 일이므로, 오곡이 풍성하도록 어서 어버이신의 마음을 용솟음치게 하라."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 농작물이 용솟음치도록 하려거든\n 신악근행과 손춤을 행하라 1-14\n",
                            japanese = "りうけいがいさみでるよとをもうなら\nかぐらつとめやてをとりをせよ",
                            english = "ryukei ga isami deru yo to omou nara\nKagura-zutome ya Teodori o seyo",
                            commentary = "十四, 손춤은 신악가 팔수부터 十二장까지를 말한다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이번에는 서둘러 손춤을 시작하라\n 이것을 계기로 신기함이 있으리 1-15\n",
                            japanese = "このたびハはやくてをどりはじめかけ\nこれがあいずのふしきなるそや",
                            english = "konotabi wa hayaku Teodori hajime kake\nkore ga aizu no fushigi naru zo ya",
                            commentary = "十五, 신악근행과 손춤을 행하면 사람들의 마음이 즐거워지고 마음의 티끌도 깨끗이 털려 맑아지므로 어버이신도 그러한 마음을 보고 섭리하게 된다. 그러므로 하루빨리 신악근행과 손춤을 행하라. 그러면 이 근행을 계기로 어버이신의 신기한 섭리가 반드시 나타나게 된다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이 같은 신기함은 나타나지 않았으나\n 날이 오면 확실히 알게 된다 1-16\n",
                            japanese = "このあいずふしぎとゆうてみへてない\nそのひきたれバたしかハかるぞ",
                            english = "kono aizu fushigi to yute miete nai\nsono hi kitareba tashika wakaru zo",
                            commentary = "十六, 이렇게 해서 나타나는 어버이신의 섭리는 영묘(靈妙)한 것이지만 , 아직은 나타나지 않았기 때문에 지금 당장 사람들로서는 알 수가 없다. 그러나 어버이신이 마침내 신기한 섭리를 나타내는 날이 오면 과연 영묘한 것임을 누구나 분명히 알게 될 것이다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 그날이 와서 무엇인가 알게 되면\n 어떤 자도 모두 감탄하리라 1-17\n",
                            japanese = "そのひきてなにかハかりがついたなら\nいかなものてもみながかんしん",
                            english = "sono hi kite nanika wakari ga tsuita nara\nikana mono demo mina ga kanshin",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 나타난 다음에 일러주는 것은 세상 보통 일\n 나타나기 전부터 일러두는 거야 1-18\n",
                            japanese = "みへてからといてかゝるハせかいなみ\nみへんさきからといてをくそや",
                            english = "miete kara toite kakaru wa sekainami\nmien saki kara toite oku zo ya",
                            commentary = "十八, 무슨 일이든 눈앞에 나타난 다음에 일러주는 것은 세상 보통 일이지만,  어버이신은 눈앞의 일뿐만 아니라 장래의 일까지 미리 일러주므로, 어버이신의 말 가운데 인간생각으로 이해 안되는 것이 있다라도 이것을 의심하거나 부정하는 경솔한 짓을 해서는 안된다. 어디까지나 어버이신의 말을 믿고 그것이 실현될 날을 기다려야 한다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 앞으로는 윗사람들 차츰차츰\n 마음을 맞추어 화목하도록 1-19\n",
                            japanese = "このさきハ上たる心たん／＼と\n心しづめてハぶくなるよふ",
                            english = "konosaki wa kami taru kokoro dandan to\nkokoro sizumete wabuku naru yo",
                            commentary = "十九. 이제부터 윗사람들은 서로 마음을 맞추어 화목하지 않으면 안된다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이 화목 어려운 듯하지만\n 차츰차츰 신이 수호하리라 1-20\n",
                            japanese = "このハほくむつかしよふにあるけれと\nだん／＼神がしゆこするなり",
                            english = "kono waboku muzukashi yoni aru keredo\ndandan Kami ga shugo suru nari",
                            commentary = "二十, 이 화목은 어려운 듯하나 차차로 어버이신이 수호함에 따라 머지않아 틀림없이 실현될 것이다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이 세상은 리로써 되어 있는 세상이다\n 무엇이든 만가지를 노래의 리로써 깨우쳐 1-21\n",
                            japanese = "このよふハりいでせめたるせかいなり\nなにかよろづを歌のりでせめ",
                            english = "kono yo wa rii de semetaru sekai nari\nnanika yorozu o uta no ri de seme",
                            commentary = "二十一, 이 세상은 어버이신의 의도, 즉 천리에 의해 성립되어 있으므로, 인간의 행위는 물론 그 밖의 모든 리를 노래로써 깨우치겠다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 깨우친다 하여 손질로써 하는 것이 아니니\n 입으로도 아니고 붓끝으로 깨우쳐 1-22\n",
                            japanese = "せめるとててざしするでハないほどに\nくちでもゆハんふでさきのせめ",
                            english = "semeru tote tezashi suru dewa nai hadoni\nkuchi demo yuwan fudesaki no seme",
                            commentary = "二十二, 깨우친다 해도 인간들처럼 완력으로 하는 것도 아니요, 말로 꾸짖는 것도 아니다. 다만 붓으로 깨우치는 것이다.\n붓끝이란 친필을 말한다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 무엇이든 잘한 것은 좋으나\n 잘못함이 있으면 노래로써 알린다 1-23\n",
                            japanese = "なにもかもちがハん事ハよけれども\nちがいあるなら歌でしらする",
                            english = "nanimo kamo chigawan koto wa yokeredomo\nchigai aru nara uta de shirasuru",
                            commentary = "二十三, 모든 것이 어버이신의 뜻에 맞으면 좋으나, 만약 어버이신의 뜻에 맞지 않는 일이 있으면 노래로써 알릴테니 잘 깨달아 마음이 잘못되지 않도록 해야 한다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 알려 주면 나타나니 안타까운 일\n 어떤 질병도 마음으로부터 1-24\n",
                            japanese = "しらしたらあらハれでるハきのどくや\nいかなやまいも心からとて",
                            english = "shirashitara araware deru wa kinodoku ya\nikana yamai mo kokoro kara tote",
                            commentary = "二十四, 잘못된 마음을 노래로써 알린다면 곧 나타나는데, 이는 가엾기는 하지만 어떤 질병도 각자의 마음에서 비롯되는 것인 만큼 어쩔 수 없는 일이다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 질병이라 해도 세상 보통 것과 다르니\n 신의 노여움 이제야 나타났다 1-25\n",
                            japanese = "やまいとてせかいなみでハないほどに\n神のりいふくいまぞあらハす",
                            english = "yamai tote sekainami dewa nai hodoni\nKami no rippuku imazo arawasu",
                            commentary = "二十五, 질병이라 해도 세상에 흔이 있는 예사로운 질병이라 생각해서는 안된다. 어버이신의 뜻에 맞지 않기 때문에 지금 노여움을 나타낸 것이다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 지금까지 신이 하는 말을 듣지 않으니\n 부득이 표면에 나타낸 것이다 1-26\n",
                            japanese = "いまゝでも神のゆう事きかんから\nぜびなくをもてあらハしたなり",
                            english = "imamade mo Kami no yu koto kikan kara\nzehi naku omote arawashita nari",
                            commentary = "二十六, 지금까지 여러 번 훈계를 했으나 전혀 듣지 않으므로 부득이 사람들의 눈에 뛰게 표면에 나타낸 것이다.\n교조님의 장남 슈우지는 오랫동안 앓고 있던 다리병이 쉽사리 낫지 않을 뿐만아니라 가끔 통증이 심해 괴로워했다.\n" +
                                    "교조님은 이를 질병이 아니라 어버이신님의 꾸지람이므로 깊이 참회하여 마음을 고치도록 깨우치시는 동시에 집터의 청소를 서두르셨다. 이하 슈우지에 대한 어버이신님의 엄한 가르침은, 슈우지 개인에 대한 꾸지람이라 생각지 말고 이를 본보기로 모든 사람들을 깨우치신 것이라 해석해야 할 것이다. "
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이토록 신의 섭섭함이 나타났으니\n 의사도 약도 이에는 당할 수 없다 1-27\n",
                            japanese = "いらほどの神のざんねんでてるから\nいしやもくすりもこれハかなハん",
                            english = "kora hodono Kami no zannen deteru kara\nisha mo kusuri mo kore wa kanawan",
                            commentary = "二十七, 가벼운 질병은 간단히 치료가 되지만 어버이신의 엄한 꾸지람을 받았을 때에는 의사나 약으로도 근본적인 치료가 불가능하다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이것만은 예삿일로 생각 말라\n 어떻든 이것은 노래로 깨우친다 1-28\n",
                            japanese = "これハかりひとなみやとハをもうなよ\nなんてもこれハ歌でせめきる",
                            english = "kore bakari hitonami ya towa omouna yo\nnandemo kore wa uta de semekiru",
                            commentary = "二十八, 이 다리병만은 흔이 있는 예사로운 질병이라 가볍게 여겨서는 안된다.\n" +
                                    "그러므로 어디까지나 그 근본을 노래로써 깨우칠 것이다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이번에는 집터의 청소를 깨끗하게\n 해 보일 테니 이것을 보아 다 1-29\n",
                            japanese = "このたびハやしきのそふじすきやかに\nいたゝてみせるこれをみてくれ",
                            english = "konotabi wa yashiki no soji sukiyaka ni\nshitatete miseru kore o mite kure",
                            commentary = "二十九, 이번에는 터전의 리를 밝히기 위해 집터를 깨끗이 청소할 것이니 모두 명심하라.\n집터란 교조님이 사시는 곳으로서  인류의 본고장인 터전이 있는 곳이다"
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 청소만 깨끗이 하게 되면\n 신의 뜻을 알게 되어 말하고 말하게 되는 거야 1-30\n",
                            japanese = "そふじさいすきやかしたる事からバ\nしりてはなしはなしするなり",
                            english = "soji sai sukiyaka shitaru koto naraba\nshirite hanashite hanashi suru nari",
                            commentary = "三十, 이 집터의 청소가 깨끗이 된 다음에는 터전의 리가 나타나서 저절로 이 길은 널리 퍼져간다.\n신의 뜻을 알게 되어 말하고 말하게 되는 거야란 나타난 신님의 뜻을 깨닫고 그것을 다음 또 다음 또 다음으로 말해서 전하게 된다는 뜻."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이제까지 섭섭함은 어떤 것인가\n 다리를 저는 것이 첫째가는 섭섭함 1-31\n",
                            japanese = "これまでのざんねんなるハはにの事\nあしのちんばか一のさんねん",
                            english = "koremade no zannen naru wa nanino koto\nashi no chinba ga ichi no zannen",
                            commentary = "三十一, 이제까지 어버이신이 몹시 섭섭하게 여기고 있는 일이 무엇인가 하면 그것은 슈우지의 다리병인 것이다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이 다리는 질병이라 말하고 있지만\n 질병이 아니라 신의 노여움 1-32\n",
                            japanese = "こんあしハやまいとゆうているけれど\nやまいでハない神のりいふく",
                            english = "kono ashi wa yamai to yute iru keredo\nyamai dewa nai Kami no rippuku",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 노여움도 예삿일이 아닌 만큼\n 첩첩이 쌓인 까닭이야 1-33\n",
                            japanese = "りいふくも一寸の事でハないほどに\nつもりかさなりゆへの事なり",
                            english = "rippuku mo chotto no koto dewa nai hodoni\ntsumori kasanari yue no koto nari",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 노여움도 무슨 까닭인가 하면\n 나쁜 일 제거되지 않는 까닭이다 1-34\n",
                            japanese = "りいふくもなにゆへなるどゆうならハ\nあくじがのかんゆへの事なり",
                            english = "rippuku mo naniyue naru to yu naraba\nakuji ga nokan yue no koto nari",
                            commentary = "제 1호 39수의 주석 참조."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이 나쁜 일 깨끗이 제거되지 않고서는\n 역사에 방해가 되는 줄로 알아라 1-35\n",
                            japanese = "このあくじすきやかのけん事にてハ\nふしんのしやまになるとこそしれ",
                            english = "kono akuji sukiyaka noken koto nitewa\nfushin no jama ni naru to koso shire",
                            commentary = "三十五, 이 나쁜 일이 집터에서 깨끗이 제거되지 않는 한 어버이신이 세계구제를 위한 마음의 역사를 수행하는 데 방해가 되는 줄로 알아라.\n역사란 슈우지가 어버이신님의 뜻에 따라 교조님과 마음을 하나로 하여 만가지 구제의 가르침을 널리 전하는 일에 노력하는 것을 의미하며, 또 하나는 일에 노력하는 것을 의미하여, 또 하나는 집터에서하는 건축이란 의미를 내포하고 있다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이 나쁜 일 아무리 끈덕진 것이라 해도\n 신이 깨우쳐서 제거해 보일 테다 1-36\n",
                            japanese = "このあくじなんぼしぶといものやどて\n神がせめきりのけてみせるで",
                            english = "kono akuji nanbo shibutoi mono ya tote\nKami ga semekiri nokete miseru de",
                            commentary = "三十六, 끈덕진 것이란 집착이 강하다는 뜻이다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이 나쁜 일 깨끗이 제거하게 되면\n 다리를 저는 것도 깨끗해진다 1-37\n",
                            japanese = "このあくじすきやかのけた事ならバ\nあしのちんバもすきやかとなる",
                            english = "kono akuji sukiyaka noketa koto naraba\nsashi no chinba mo sukiyaka to naru",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 다리만 깨끗이 낫게 되면\n 다음에는 역사 준비만을 1-38\n",
                            japanese = "あしちいかすきやかなをりしたならバ\nあとハふしんのもよふハかりを",
                            english = "ashi saika sukiyaka naori shita naraba\nato wa fushin no moyo bakari o",
                            commentary = "三十八, 나쁜 일이 제거되어 다리병이 깨끗이 낫게 되면 그 다음에는 오직 역사 준비만을 한다.\n역사란 마음의 역사, 죽 세계인류의 마음을 맑히는 것을 뜻한다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 잠깐 한마디 정월 三十일로 날을 정해서\n 보내는 것도 신의 마음에서이니 1-39\n",
                            japanese = "一寸はなし正月三十日とひをきりて\nをくるも神の心からとて",
                            english = "choto hanashi shogatsu misoka to hi o kirite\nokuru mo Kami no kokoro kara tote",
                            commentary = "三十九, 수유지는 오랫동안 정실이 없이 오찌에란 내연의 처와 살면서 오또지로오(音次郎)란 아들까지 두었었다. 그리고 이들은 집터에서 동거하고 있었는데, 이것은 본래 어버이신님의 의도에 맞지 않는 나쁜 일이었으므로, 이 오찌에를 친정으로 돌려보내라고 말씀하신 것이다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 곁의 사람들은 무슨 영문인가 생각하겠지만\n 앞일을 알 수 없는 까닭이다 1-40\n",
                            japanese = "そバなものなに事するとをもへども\nさきなる事をしらんゆへなり",
                            english = "soba na mono nani goto suru to omoedomo\nsaki naru koto o shiran yue nari",
                            commentary = "四十, 정월 三十일로 날을 정해서 친정으로 돌려보내는 것을 사람들은 무엇 때문일까하고 이상하게 여기겠지만, 이것은 뒤에 나타날 사실을 모르기 때문이다.\n곁의 사람들이란 교조님 측근에 있는 사람들인데, 좁게는 나까야마 댁의 사람들이며 넓게는 이 길을 찾아 모여 온 사람들을 말한다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 그날이 와서 나타나면 곁의 사람들\n 신이 하는 말은 무엇이든 틀림이 없다 1-41\n",
                            japanese = "そのひきてみへたるならバそばなもの\n神のゆう事なにもちがハん",
                            english = "sono hi kite mietaru naraba soba na mono\nKami no yu koto nanimo chigawan",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 지금까지는 신이 하는 말을 의심하고서\n 무엇이든 거짓이라 말하고 있었다 1-42\n",
                            japanese = "いまゝでハ神のゆう事うたこふて\nなにもうそやとゆうていたなり",
                            english = "imamade wa Kami no yu koto utagote\nnanimo uso ya to yute ita nari",
                            commentary = " "
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이 세상을 창조한 신이 하는 말은 \n 천의 하나도 틀림이 없다 1-43\n",
                            japanese = "このよふをはじめた神のゆう事に\nせんに一つもちがう事なし",
                            english = "kono yo o hajimeta Kami no yu koto ni\nsen ni hitotsu mo chigau koto nashi",
                            commentary = " "
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 차츰차츰 나바나거든 납득하라\n 어떤 마음도 모두 나타날 테니 1-44\n",
                            japanese = "だん／＼とみへてきたならとくしんせ\nいかな心もみなあらハれる",
                            english = "dandan to miete kita nara tokushin se\nikana kokoro mo mina arawareru",
                            commentary = "四十一～四十四, 오찌에는 친정으로 돌아간후, 며칠이 안되어 병으로  자리에 눕게되고 끝내 다시 일어나지 못했다. 만약 인정에 끌려 기일을 늦추었더라면 집터의 청소는 결국 할 수가 없었을 것이다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 모든 시대 온 세상을 살펴보면\n 인간이 가는 길도 가지각색이다 1-45\n",
                            japanese = "よろづよのせかいぢふうをみハたせバ\nみちのしだいもいろ／＼にある",
                            english = "yorozuyo no sekaiju o miwataseba\nmichi no shidai mo iroiro ni aru",
                            commentary = "四十五, 예나 지금이나 세상 사는 모습을 살펴보면 인생행로는 천태만상이다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 앞으로는 길에 비유해서 말한다\n 누구 일이라고 새삼 말하지 않아 1-46\n",
                            japanese = "このさきハみちにたとへてはなしする\nどこの事ともさらにゆハんで",
                            english = "konosaki wa michi ni tatoete hanashi suru\ndoko no koto tomo sarani yuwan de",
                            commentary = "四十六, 사람이 사는 길은 여러 갈래가 있는데, 앞으로는 길에 비유해서 일러줄 터이니, 어디 누구의 일이라고 말하지 않지만 하나하나 모두 단단히 듣고 잘 생각하라.\n 남의 일로 여겨 흘려 들어서는 안된다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 산언덕 가시밭 낭떠러 비탈길도\n 칼날 갈은 험한 길도 헤쳐 나가면 1-47\n",
                            japanese = "やまさかやいばらぐろふもがけみちも\nつるぎのなかもとふりぬけたら",
                            english = "yamasaka ya ibara guro mo gakemichi mo\ntsurugi no naka mo torinuke tara",
                            commentary = "四十七, 산언덕을 넘고 가시덤불을 지나 낭떠러지 비탈길도, 칼날 같은 험한 길도 지나가면."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 아직도 보이는 불속 깊은 물속을\n 그것을 지나가면 좁은 길이 보이느니 1-48\n",
                            japanese = "まだみへるひのなかもありふちなかも\nそれをこしたらほそいみちあり",
                            english = "mada mieru hi no naka mo ari fuchinaka mo\nsore o koshitara hosoi michi ari",
                            commentary = "四十八, 아직도 불속이 있고 깊은 물속도 있으나 그것을 차츰 지나가면 비로소 좁은 길이 나온다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 좁은 길을 차츰차츰 넘어가면 큰길이야\n 이것이 확실한 본길이니라 1-49\n",
                            japanese = "ほそみちをだん／＼こせばをふみちや\nこれがたしかなほんみちである",
                            english = "hosomichi o dandan koseba omichi ya\nkore ga tashikana honmichi de aru",
                            commentary = "四十九, 이좁은 길을 차츰 지나가면 마침내 큰길이 나온다. 이처럼 온갖 어려운 길을 지나서 나타나는 큰길이야말로 참으로 틀림없는 한길이다. \n四十七～四十九,이상 3수는 길에 비유해서 사람들이 걸어가여 할 길의 과정을 가르치신 것으로서, 이같은 시련을 견디며 끝까지 곤경을 극복해 나간다면 반드시 좋은 길로 나아가게 됨을 잘 깨달아야 한다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이 이야기는 남의 일이 아닌 만큼\n 신한줄기로서 이것은 자신의 일이야 1-50\n",
                            japanese = "このはなしほかの事でわないほとに\n神一ぢよでこれわが事",
                            english = "kono hanashi hoka no koto dewa nai hodoni\nKami ichijo de kore waga koto",
                            commentary = "五十, 이 이야기는 결코 남의 이기가 아니다. 인간들 을 구제하려는 진실한 어버이신의 가르침으로서, 이것은 바로 너희들 자신의 이야기다.\n이러한 길을 교조님은 몸소 걸어 우리들에게 모본의 길을 보여주셨다. 그러므로 이 모본을 그리며 따르는 이 길의 자녀들은 모두 이것을 자기 일로 생각하라는 가르침이다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이제까지는 안의 일만을 말했다\n 이제 앞으로는 이야기를 바꿀 테다  1-51\n",
                            japanese = "いまゝでハうちなる事をばかりなり\n、もふこれからハもんくがハるぞ",
                            english = "imamade wa uchi naru koto o bakari nari\nmo korekara wa monku kawaru zo",
                            commentary = "五十一, 이제까지는 주로 집터 안의 일에 대해서만 여러 가지로 일러주었으나, 앞으로는 널리 일반 세상일에 대해서도 일러주겠다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 모든 시대 온 세상을 살표보아도\n 악이란 전혀 없는 거야 1-52\n",
                            japanese = "よろづよにせかいのところみハたせど\nあしきのものハちらにないぞや",
                            english = "yorozuyo ni sekai no tokoro miwatasedo\nashiki no mono wa sara ni nai zo ya",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 모든 사람들에게 악이란 없는 것이지만\n 다만 조금 티끌이 묻었을 뿐이다 1-53\n",
                            japanese = "一れつにあしきとゆうてないけれど\n一寸のほこりがついたゆへなり",
                            english = "ichiretsu ni ashiki to yute nai keredo\nchoto no hokori ga tsuita yue nari",
                            commentary = " "
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 앞으로는 마음을 가다듬어 생각하라\n 나중에 후회 없도록 하라 1-54\n",
                            japanese = "このさきハ心しづめてしやんせよ\nあとでこふくハいなきよふにせど",
                            english = "konosaki wa kokoro shizumete shiyan seyo\nato de kokwai naki yoni seyo",
                            commentary = " "
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 지금까지 먼 길 걸어온 과정을\n 매우 지루하게 여겼을 테지 1-55\n",
                            japanese = "いまゝでハながいどふちふみちすがら\nよほどたいくつしたであろをな",
                            english = "imamade wa nagai dochu michisugara\nyohodo taikutsu shita de aro na",
                            commentary = "五十五, 어버이신이 원래 없던 세계 없던 인간을 창조한 이래 아주 오랜 세월이 흘렀는데, 그 동안 사람들은 진실한 가르침을 들을 수 없었기 때문에 의지할 어버이도 모른 채 무척 지루하게 살아왔을 것이다.\n이 노래는 지금까지 오랫동안 인간이 진실한 천계(天啓)를 들을 수 없었던 것을 길에 비유해서 가르치신 것이다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이번에는 이미 확실한 참배장소가\n 이룩되었으니 납득하라 1-56\n",
                            japanese = "このたびハもふたしかなるほいりしよ\nみへてきたぞへとくしんをせよ",
                            english = "konotabi wa mo tashika naru mairisho\nmiete kita zoe tokushin o seyo",
                            commentary = "五十六, 순각한의 도래로 진실한 어버이신이 이 세상에 나타나서 인간창조의 본고장에 참배장소인 근행장소도 이룩했으므로, 사람들은 망설임 없이 안심하고 신앙을 하라."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이제부터는 먼 길 걸어온 과정을\n 일러줄 테니 깊이 생각하라 1-57\n",
                            japanese = "これからハながいどふちふみちすがら\nといてきかするとくとしやんを",
                            english = "korekara wa nagai dochu michisugara\ntoite kikasuru tokuto shiyan o",
                            commentary = "五十七, 인간들은 창조한 이래 오랜 세월 동안 여러 경로를 거쳐왔는데, 이제부터는, 이제부터는 그 거쳐온 과정을 일러줄 터이니 잘 듣고 생각하라. 그러면 어버이신이 어떻게 마음을 기울여 왔는지도 알게 될 것이다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 앞으로는 안을 다스릴 준비\n 신은 마음 서두르고 있다 1-58\n",
                            japanese = "このさきハうちをおさめるもよふだて\n神のほふにハ心せきこむ",
                            english = "konosaki wa uchi o osameru moyodate\nKami no ho niwa kokoro sekikomu",
                            commentary = "五十八, 이제부터는 집터 안을 맑히는 준비를 시작하겠는데, 이것이 하루라도 빨리 이루어지도록 어버이신은 서두르고 있다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 차츰차츰 신이 하는 말을 들어 다오\n 그럿된 말은 결코 하지 않아 1-59\n",
                            japanese = "だん／＼と神のゆふ事きいてくれ\nあしものことハさらにゆハんで",
                            english = "dandan to Kami no yu koto kiite kure\nashiki no koto wa sarani yuwan de",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이 아이 二년 三년 가르치려고\n 애 쓰고 있지만 신의 손 뗌 1-60\n",
                            japanese = "このこ共二ねん三ねんしこもふと\nゆうていれども神のてはなれ",
                            english = "kono kodomo ni nen san nen shikomo to\nyute iredomo Kami no tebanare",
                            commentary = "六十, 이 아이를 二, 三년 가르치려고 그 부모는 애쓰고 있지만 어버이신은 이 아이가 이미 수명이 다 되었음을 잘 알고 있다.\n이 아이란 슈우지의 서녀(庶女)인 오슈우를 말하는데, 당시에 부모는 신부수업을 가르치려 하고 있었는데, 어버이신님은 그가 출직할 것을 예언하신 것이다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 생각하라 어버이가 아무리 애를 써도\n 신의 손 뗌 이에는 당할 수 없다 1-61\n",
                            japanese = "いやんせよをやがいかほどをもふても\n神のてばなれこれハかなハん",
                            english = "shiyan seyo oya ga ika hodo omotemo\nKami no tebanare kore wa kanawan",
                            commentary = "六十一, 잘 생각해 보라, 부모가 아무리 자식 귀여운 마음에서 오래 살기를 바랄지라도 어버이신이 수호하지 않는다면 어쩔 수 없는 것이니 이 점을 잘 깨달아야 한다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이 세상에는 악한 것 섞여 있으므로\n 인연을 쌓아서는 안되는 거야 1-62\n",
                            japanese = "このよふハあくしもじりであるからに\nいんねんつけ事ハいかんで",
                            english = "kono yo wa akuji majiri de aru karani\ninnen tsukeru koto wa ikan de",
                            commentary = "六十二, 이 세상은 자칫하면 악에 물들기 쉬운 곳이므로 주의해서 악인연을 짓지 않도록 해야 한다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 자신으로는 벌써 五十이라 생각하지만\n 신의 눈으로는 아직 장래가 있다 1-63\n",
                            japanese = "わかるにハもふ五十うやとをもへとも\n神めへにハまださきがある",
                            english = "waga mi niwa mo goju ya to omoedomo\nKami no me niwa mada saki ga aru",
                            commentary = "六十三, 이제 곧 나이 五十이므로 자신으로서는 꽤나 나이가 많다고 생각하겠지만 어버이신이 볼 때는 아직도 장래가 있다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 올해부터 六十년은 확실하게\n 신이 단단히 맡을 테다 1-64\n",
                            japanese = "ことしより六十ねんハしいかりと\n神のほふにハしかとうけやう",
                            english = "kotoshi yori rokuju nen wa shikkari to\nKami no ho niwa shikato ukeyau",
                            commentary = "六十四, 올해부터 앞으로 六十년은 어버이신이 확실히 맡는다.\n이것은 슈우지에게 하신 말씀으로서, 당시 四十九세였다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이제부터는 마음 단단히 바꿔라\n 나쁜 일 치우고 젊은 아내를 1-65\n",
                            japanese = "これからハ心しいかりいれかへよ\nあくじはろふてハかきによほふ",
                            english = "korekara wa kokoro shikkari irekae yo\nakuji harote wakaki nyobo",
                            commentary = "六十五, 젊은 아내란 인연이 있어 슈우지의 부인이 된 야마또(大和) 지방 헤구리(平群)군 뵤도도오지(平等寺) 마을의 고히가시 마사끼찌(小東政吉)의 차녀 마쯔에로서 당시 十九세였다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이것 역시 어려운 듯하지만\n 신이 나가면 데려올 거야 1-66\n",
                            japanese = "これとてもむつかしよふにあるけれど\n神がでたならもろてくるそや",
                            english = "kore totemo mutsukashi yoni aru keredo\nKami ga deta nara morote kuru zo ya",
                            commentary = "六十六, 이것은 나이 차이로 어려운 일처럼 생각되겠지만 어버이신이 나간다면 반드시 혼담을 성사시킬 것이다.\n이 혼담은 처음에 닷다(龍田) 마을의 감베에란 중매인이 고히가시 댁에 교섭을 했으나 일이 잘되지 않았다. 그래서 교조님이 몸소 행차하여 여러 가지로 일러주시자, 저쪽에서도 비로소 납득하게 되어 이윽고 혼담이 성립되었다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 나날이 마음 다한 그 다음에는\n 앞으로 책임을 모두 맡길 테다 1-67\n",
                            japanese = "にち／＼に心尽くしたそのゑハ\nあとのしはいをよろづまかせる",
                            english = "nichinichi ni kokoro tsukushita sono ue wa\nato no shihai o yorozu makaseru",
                            commentary = "六十七, 마음을 다해서 매일 어버이신의 일에 전념한다면 앞으로 집터의 책임을 모두 맡긴다.\n이것은 슈우지에게 하신 말씀이다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 다섯 사람 가운데 둘은 집에 두라\n 나머지 세 사람은 신이 맡는다 1-68\n",
                            japanese = "五人あるなかのにゝんハうちにをけ\nあと三人ハ神のひきうけ",
                            english = "go nin aru naka no ni nin wa uchi ni oke\nato san nin wa Kami no hikiuke",
                            commentary = "六十八, 이것은 슈우지의 부인 마쯔에의 친정 고히가시 댁에 대해 하신 말씀으로 고히가시 마사끼찌에게는 오사꾸, 마쯔에, 마사따로오(政太郎), 가메끼찌〔龜吉․후에 사다지로오(政次郎)로 개명〕, 오또끼찌〔音吉․후에 센지로오(仙次郎)로 개명〕등, 다섯 자녀가 있었다. 어버이신님은 이들 중 두 사람은 집안일을 시키고, 나머지 세 사람은 어버이신님께 바치면 그 뒷일은 모두 맡겠다고 말씀하셨다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 모든 시대 온 세상의 일을 살펴보고\n 마음을 가다듬어 생각해 보라 1-69\n",
                            japanese = "よろづよのせかいの事をみはらして\n心しづめてしやんしてみよ",
                            english = "yorozuyo no sekai no koto o miharashite\nkokoro shizumete shiyan shite miyo",
                            commentary = "六十九, 세상에서 일어나는 여러 가지 일들을들을 살펴보고 마음을 가다듬어 이제부터 가르치는 것을 잘 생각해 보라 "
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 지금까지도 신의 세상이었지만\n 중매하는 일은 이것이 처음이야 1-70\n",
                            japanese = "いまゝても神のせかいであるけれど\nあかだちするハ今がはじめや",
                            english = "imamade mo Kami no sekai de aru keredo\nnakadachi suru wa ima ga hajime ya",
                            commentary = "七十, 지금까지도 이 세상의 모든 일은 어버이신이 지배해 왔지만, 어버이신이 세상밖으로 나가서 부부의 연을 맺어 주는 것은 이것이 처음이다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이제부터 세상 사람들은 비웃을 거야\n 중매하는 일은 이것이 처음이야 1-71\n",
                            japanese = "これからハせかいの人ハをかしがる\nなんぼハろてもこれが大一",
                            english = "korekara wa sekai no hito wa okashigaru\nnanbo warotemo kore ga daiichi",
                            commentary = "七十一, 세상 사람들은 이 결혼을 나이 차이로 합당치 않다고 비웃고 있지만, 그것은 사람들이 어버이신의 참뜻을 모르기 때문이다. 그리고 이 결혼은 서로 같은 인연을 모아 부부의 연을 맺는 것으로서, 이는 인생의 근본인 만큼 무엇보다도 중요한 것이다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 세상에서는 무엇을 하느냐고 말하겠지\n 사람이 비웃어도 신은 좋아한다 1-72\n",
                            japanese = "せかいにハなに事するとゆうとである\n人のハらいを神がたのしむ",
                            english = "sekai niwa nani goto suru to yu de aro\nhito no warai o Kami ga tanoshimu",
                            commentary = "七十二, 세상에서는 왜 저런 일을 하는가고 이상하게 여기겠지만, 이는 전혀 인연에 대해 모르기 때문이다. 그러나 머지않아 이를 깨닫는 날이 올 것이므로 어버이신으로서는 이 같은 한때의 웃음거리가 오히려 즐거운 일이다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 각자가 생각하는 마음은 안돼\n 신의 마음은 전연 틀리는 거야 1-73\n",
                            japanese = "めへめへのをもふ心ハいかんでな\n神の心ハみなちがうでな",
                            english = "meme no omou kokoro wa ikan de na\nKami no kokoro wa mina chigau de na",
                            commentary = "七十三, 이 결혼에 대해 사람들은 인간마음으로 여러 가지 해석들을 하지만, 어버이신은 이와는 전혀 다른 깊은 뜻이 있다.\n이 결혼에는 근행을 서두르시는, 즉 근행 준비로서 인원을 갖추려고 서두르시는 깊은 신의(神意)가 내포되어 있다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 전생 인연 모아서 수호한다\n 이것으로 영원히 확실하게 다스려진다 1-74\n",
                            japanese = "せんしよのいんねんよせでしうごふする\nこれハもつだいしとをちまる",
                            english = "zensho no innen yosete shugo suru\nkore wa matsudai shikato osamaru",
                            commentary = "七十四, 전생에서부터 깊은 인연이 있는 사람을 이 접터에 이끌어 들여 부부가 되도록 영원히 확실하게 다스린다.\n 전생인연이란 슈지 선생과 마쓰에님과의 관계를 두고 하신 말씀으로, 두 사람은 전생의 인연에 의해서 금생에 부부가 되어야 할 사이였으며, 또 터전에 깊은 인연이 있는 사람들이었다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이제부터는 한길을 내기 시작한다\n 세상사람들의 마음 모두 용솟음치게 할 거야 2-1\n",
                            japanese = "これからハをくハんみちをつけかける\nせかいの心みないさめるで",
                            english = "korekara wa okwan michi o tsuke kakeru\nsekai no kokoro mina isameru de",
                            commentary = "一, 지금까지는 주로 이 길을 그리며 찾아오는 사람들을 가르쳐 왔으나, 이제부터는 세계에 널리 가르침을 펴서 세상 사람들의 마음을 용솟음치게 한다.\n한길이란 많은 사람이 아무런 위험 없이 안심하고 다닐 수 있는 큰길이라는 뜻이데, 여기서는 빈부 귀천이나 민족 여하를 불문하고 모두 빠짐없이 전인류를 구제하는 길이란 뜻이다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 윗사람들은 마음 용솟음치게 될 것이니\n 언제라 할 것 없이 각한이 다가왔다 2-2\n",
                            japanese = "上たるハ心いさんでくるほとに\nなんどきにくるこくけんがきた",
                            english = "kami taru wa kokoro isande kuru hodoni\nnandoki ni kuru kokugen ga kita",
                            commentary = "二, 한길을 닦아 가면 윗사람들도 이 가름침을 듣고 마음이 용솟음쳐 찾아오게 된다. 더욱이 이것은 머지않은 일로서 이제 곧 실현될 단계에 와 있다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 찻잎을 따고 나서 고르기를 마치면\n 다음에 할 일은 즐거운근행이야 2-3\n",
                            japanese = "ちやつんであとかりとりてしもたなら\nあといでるのハよふきづとめや",
                            english = "cha tsunde ato karitorite shimota nara\nato i deru no wa Yoki-zutome ya",
                            commentary = "三, 찻잎을 따고 나서 가지 고르기를 마치면 그 다음에는 드디어 즐거운근행을 시작한다.\n이 지역에서 찻잎을 따는 시기는 대체로 음력 5월 중순경이다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이 근행 어떻게 이루어진다고 생각하는가\n 윗사람들 마음 용솟음칠 거야 2-4\n",
                            japanese = "このつとめとこからくるとをもうかな\n上たるところいさみくるぞや",
                            english = "kono Tsutome doko kara kuru to omou kana\nkami taru tokoro isami kuru zo ya",
                            commentary = "四, 이 즐거운근행은 누가 행하는가 하면 마음이 바꿔진 사람들부터 행하게 된다. 그렇게 되면 윗사람들도 저절로 마음이 용솟음치게 될 것이다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 차츰차츰 신의 수호는\n 모두 진기한 일만 시작할 거야 2-5\n",
                            japanese = "たん／＼と神のしゆごふとゆうものハ\nめつらし事をみなしかけるで",
                            english = "dandan to Kami no shugo to yu mono wa\nmezurashi koto o mina shikakeru de",
                            commentary = "五, 어버이신은 영묘한 수호로써 차츰 사람들이 생각지도 못했던 신기한 힘을 나타내어 지금까지 모르던 진기한 일을 시작한다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 나날이 신이 마음 서두르는 것을\n 모든 사람들은 어떻게 생각하는가 2-6\n",
                            japanese = "にち／＼に神の心のせきこみを\nみないちれつハなんとをもてる",
                            english = "nichinichi ni Kami no kokoro no sekikomi o\nmina ichiretsu wa nanto omoteru",
                            commentary = " "
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 어떤 것이든 질병이나 아픔이란 전혀 없다\n 신의 서두름 인도인 거야 2-7\n",
                            japanese = "なにゝてもやまいいたみハさらになし\n神のせきこみてびきなるそや",
                            english = "nani nitemo yamai itami wa sarani nashi\nKami no sekikomi tebiki naru zo ya",
                            commentary = "七, 세상 사람들은 질병이니 통증이니 하고들 있으나 결코 그런 것이 아니다. 그것은 모두 어버이신의 깃은 의도에 의한 서두름이며 인도이다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 서두르는 것도 무슨 까닭인가 하면\n 근행인원이 필요하므로 2-8\n",
                            japanese = "せきこみもなにゆへなるとゆうならば\nつとめのにんぢうほしい事から",
                            english = "sekikomi mo naniyue naru to yu naraba\nTsutome no ninju hoshii koto kara",
                            commentary = "八, 어버이신이 왜 서두르고 있는가 하면, 그것은 빨리 근행인원을 이끌어 들이고 싶기 때문이다.\n근행인원은 제10호 25～27수의 주석 참조."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이 근행 어떤 것이라 생각하는가\n 만가지구제의 준비만을 2-9\n",
                            japanese = "このつとめなんの事やとをもている\nよろづたすけのもよふばかりを",
                            english = "kono Tsutome nanno koto ya to omote iru\nyorozu tasuke no moyo bakari o",
                            commentary = "九, 어버이신이 바라고 있는 즐거운근행은 무엇 때문이라고 생각하는가, 이것으로 세상의 만가지를 구제하고 싶기 때문이다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이 구제 지금뿐이라고는 생각 말라\n 이것이 영원한 고오끼인 거야 2-10\n",
                            japanese = "このたすけいまばかりとハをもうなよ\nこれまつたいのこふきなるぞや",
                            english = "kono tasuke ima bakari towa omouna yo\nkore matsudai no Koki naru zo ya",
                            commentary = "十, 이 구제는 현재뿐이라고 생각해서는 안된다. 이것은 영원한 본보기가 되어 언제까지나 구제의 결실을 거두게 된다.\n영원한 고오끼란 영원히 후세에까지 전해져 구제한줄기의 가르침의 토대가 됨을 말한다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 잠깐 한마디 신경증이다 광증이다 말하고 있다\n 질병이 아니라 신의 서두름 2-11\n",
                            japanese = "一寸はなしのぼせかんてきゆうている\nやまいでハない神のせきこみ",
                            english = "choto hanashi nobose kanteki yute iru\nyamai dewa nai Kami no sekikomi",
                            commentary = "十一, 세상 사람들은 흔히들 저 사람은 신경증이다 광증이다고 말하고 있으나 결코 정신이 돈 것도 아니요, 질병도 아니다. 빨리 이 길로 이끌어 들이려는 어버이신의 서두름이다.\n다음 노래의 주석을 참조"
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 차츰차츰 진실한 신한줄기를\n 일러주어도 아직도 몰라 2-12\n",
                            japanese = "たん／＼としんぢつ神の一ちよふ\nといてきかせどまだハかりない",
                            english = "dandan to shinjitsu Kami no ichijo o\ntoite kikasedo mada wakari nai",
                            commentary = "十二, 여러가지로 진실한 신한줄기의 길을 일러주어도 아직 깨닫지 못하고 있다.\n 쯔지 추우사꾸(辻忠作)는 1863년에 입신했는데, 그 동기는 누이동생 구라의 발광 때문이었다. 그는 어느 날, 친척되는 이찌노모또(櫟本)마을의 가지모또(梶本)가에서 쇼야시끼의 신님은 만가지구제를 하시는 신님이란 말을 듣고 비로소 신앙할 마음이 생겼다. 추우사꾸는 즉시 교조님을 찾아 뵙고 여러 가지 가르침을 들은 후 신앙을 했던 바, 구라의 발광은 씻은 듯이 나았다. 그래서 그는 이 길을 열심히 신앙하게 되었으며, 구라는 그 뒤에 혼담이 이루어져 센조꾸(千束)라는 곳으로 시집을 갔다. 그러나 그 후 추우사꾸는 집안 사람들의 반대에 부딪쳐 차차 신앙심이 약해짐에 따라 교조님께 전혀 발걸음을 하지 않게 되었는데. 그러자 이상하게도 구라의 병이 다시 재발하여 시집에서 쫒겨나게 되었다.\n이에 사람들은 구라가 정신이 돌았기 때문에 쫓겨 났다느니, 혹은 이혼을 당했기 때문에 돌았다느니 하는 여러 가지 말들을 하고 있었으나, 그것은 결코 사람들이 흔히 말하는 질병도 광증도 아닌, 오직 신앙에서 멀어진 추우사꾸를 이 길로 다시 이끌어 들이시려는 어버이신님의 깊은 의도였던 것이다. 이러한 가르침을 들은 추우사꾸는 자신의 잘못을 참회하고 다시 열심히 어버이신님의 일에 힘쓰게 되었다. 그러자 구라의 발광은 씻은 듯이 나아 다시 시집으로 돌아가게 되었다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 어서어서 세상에 나가려고 생각하지만\n 길이 없어서는 나가려야 나갈 수 없다 2-13\n",
                            japanese = "といてきかせどまだハかりなも\nみちがのふてハでるにでられん",
                            english = "hayabaya to omote deyo to omoedomo\nmichi ga note wa deru ni deraren",
                            commentary = "十三, 지금까지는 여기 찾아오는 사람에게만 가르침을 일러주었으나, 이제는 한시라도 빨리 이쪽에서 밖으로 나아가 세상 사람들에게 널리 가르침을 일러주고자 한다. 그러나 길이 억어서는 나아갈 수가 없다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이 길을 빨리 내려고 생각하지만\n 다른 곳에서는 낼 데가 없다 2-14\n",
                            japanese = "このみちをはやくつけよとをもへとも\nほかなるとこでつけるとこなし",
                            english = "kono michi o hayaku tsukeyo to omoedomo\nhoka naru toko de tsukeru toko nashi",
                            commentary = "十四, 세상에 널리 이 가르침을 전하고자 하나, 이 길은 아무데서나 낼 수 있는 것이 아니기 때문에 이곳 아닌 다른 곳에서는 낼 수가 없다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이 길을 진실로 생각한다면\n 가슴속으로 만가지를 생각하라 2-15\n",
                            japanese = "このみちをしんぢつをもう事ならば\nむねのうちよりよろづしやんせ",
                            english = "kono michi o shinjitsu omou koto naraba\nmune no uchi yori yorozu shiyan se",
                            commentary = "十五, 이 신한줄기의 길을 진실로 생각한다면 마음을 가다듬어 만사를 잘 생각해 보라."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이 이야기 무엇을 말한다고 생각하는가\n 신의 구제장소를 서두르는 것이 2-16\n",
                            japanese = "このはなしなんの事やとをもている\n神のうちわけばしよせきこむ",
                            english = "kono hanashi nanno koto ya to omote iru\nKami no uchiwake basho sekikomu",
                            commentary = "十六, 지금 가르친 이야기의 참뜻은 무엇이라 생각하는가, 그것은 방방곡곡에 구제장소를 마련하기를 서두르는 것이다.\n구제장소란 장래 안과 중간과 밖에 각각33개소, 도합 93개소가 생기게 되는데, 어떤 어려운 질병이라도 이 구제장소를 도는 동안에 구제받게 되며, 이 가운데 한 곳은 아주 먼 벽지에 있다. 그러나 이 곳을 빠뜨려서는 구제받지 못한다. 그리고 가령 도중에서 구제를 받았더라도 탈것이나 지팡이를 버리지 말고, 고맙게 도움받았다는 사실을 사람들에게 알리며, 이것을 마지막에는 터전에 올려야 한다. 만약 도중에서 이를 버릴 경우에는 일단 구제를 받았더라도 다시 본래대로 된다고 말씀하셨다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이 길이 조금 나타나기 시작하면\n 세상 사람들의 마음 모두 용솟음친다 2-17\n",
                            japanese = "このみちが一寸みゑかけた事ならば\nせかいの心みないさみてる",
                            english = "kono michi ga choto miekaketa koto naraba\nsekai no kokoro mina isami deru",
                            commentary = "十七, 이 가르침이 나타나기 시작하면 세상 사람들의 마음은 모두 용솟음치게 된다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 무엇이든 신이 하는 말 단단히 들어라\n 집터의 청소가 되었으면 2-18\n",
                            japanese = "なにゝても神のゆう事しかときけ\nやしきのそふぢでけた事なら",
                            english = "nani nitemo Kami no yu koto shikato kike\nyashiki no soji deketa koto nara",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 벌써 보인다 한눈 팔 새 없을 만큼\n 꿈결같이 티끌이 털리는 거야 2-19\n",
                            japanese = "もふみへるよこめふるまないほどに\nゆめみたよふにほこりちるぞや",
                            english = "mo mieru yokome furu ma mo nai hodoni\nyume mita yoni hokori chiru zo ya",
                            commentary = "十八, 十九, 어떤 일이라도 어버이신의 가르침을 단단히 듣도록 하라. 집터 안의 사람들이 마음을 청소하여 이것이 어버이신이게 통하게 되면 눈 깜빡할 사이에 티끌은 털리고 만다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이 티끌 깨끗하게 털어 버리면\n 앞으로는 만가지 구제한줄기 2-20\n",
                            japanese = "このほこりすきやかはろた事ならば\nあとハよろづのたすけ一ちよ",
                            english = "kono hokori sukiyaka harota koto naraba\nato wa yorozu no tasuke ichijo",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 앞으로는 차츰차츰 근행 서둘러서\n 만가지구제의 준비만을 2-21\n",
                            japanese = "このさきハたん／＼つとめせきこんで\nよろづたすけのもよふばかりを",
                            english = "konosaki wa dandan Tsutome sekikonde\nyorozu tasuke no moyo bakari o",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 온 세상 어디가 나쁘다 아프다 한다\n 신의 길잡이 인도임을 모르고서 2-22\n",
                            japanese = "せかいぢうとこがあしきやいたみしよ\n神のみちをせてびきしらすに",
                            english = "sekaiju doko ga ashiki ya itamisho\nKami no michiose tebiki shirazu ni",
                            commentary = "二十二, 세상에서는 어디가 나쁘다, 어디가 아프다고 말하고 있으나, 사실은 질병이라 전혀 없다. 설사 나쁜 데나 아픈데가 있더라도 그것은 어버이신의 가르침이며 인도에다. 그런데도 세상에서는 이를 전혀 깨닫지 못하고 질병이라고만 생각하고 있다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이 세상에 질병이란 없는 것이니\n 몸의 장애 모두 생각해 보라 2-23\n",
                            japanese = "このよふにやまいとゆうてないほどに\nみのうちさハりみなしやんせよ",
                            english = "kono yoni yamai to yute nai hodoni\nminouchi sawari mina shiyan seyo",
                            commentary = "二十三, 이 세상에는 질병이란 전혀 없다. 따라서 만약 몸에 이상이 생길 때는 어버이신이 무엇 때문에 이같은 가르침이나 인도를 나타내 보이는가를 잘 생각해 보라."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 나날이 신이 서두르는 이 구제\n 모든 사람든은 어떻게 생각하는가 2-24\n",
                            japanese = "にち／＼に神のせきこみこのたすけ\nみな一れつハなんとをもてる",
                            english = "nichinichi ni Kami no sekikomi kono tasuke\nmina ichiretsu wa nanto omoteru",
                            commentary = "二十四, 나날이 어버이신이 사람들의 맘의 장애를 인도로 하여 구제를 서두르고 있음을 세상 사람들은 어떻게 생각하고 있는가. 이러한 어버이신의 간절한 마음을 어서 깨달아 주었으면."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 녺은 산의 못은 맑은 물이지만\n 첫머리는 탁하고 찌꺼기 섞여 있다 2-25\n",
                            japanese = "高山のをいけにハいた水なれど\nてバなハにこりごもくまぢりで",
                            english = "takayama no oike ni waita mizu naredo\ndebana wa nigori gomoku majiri de",
                            commentary = "二十五, 깊은 산속에 있는 못의 맑은 물이란 사람에 비유해서 하신 말씀으로, 인간이 처음 태어날 때는 누구나가 다 청청한 마음을 지니고 있으나, 세월이 지남에 따라 자신만을 생각하는 욕심 때문에 티끌이 쌓여 차츰 마음이 흐려짐을 뜻한다. 첫머리란 입신 당시의 혼탁한 마음을 가리키는 것으로 해석된다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 차츰차츰 마음을 가다듬어 생각하면\n 맑은 물로 바꿔질거야 2-26\n",
                            japanese = "だん／＼と心しづめてしやんする\nすんだる水とかハりくるぞや",
                            english = "dandan to kokoro shizumete shiyan suru\nsundaru mizu to kawari kuru zo ya",
                            commentary = "二十六, 처음에는 티끌이 섞인 탁한 마음일지라도 어버이신의 가르침을 듣고 깊이 반성하여 마음의 티끌을 제거하도록 노력하면, 차츰 물이 맑아지듯 마음이 청정하게 바꿔진다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 산중에 있는 물속에 들어가서\n 어떤 물이라도 맑히리라 2-27\n",
                            japanese = "山なかのみづのなかいと入こんで\nいかなる水もすます事なり",
                            english = "yama naka no mizu no naka i to irikonde\nika naru mizu mo sumasu koto nari",
                            commentary = "二十七, 산중에 있는 물속에 들어가서 그 물이 아무리 탁할지라도 깨끗이 맑히겠다.\n혼탁한 세상을 정화하는 것이 본교의 사명임을 말씀하신 것이다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 나날이 마음 다하는 사람들은\n 가슴속 진정하라 끝에는 믿음직하리 2-28\n",
                            japanese = "にち／＼に心つくするそのかたわ\nむねをふさめよすゑハたのもし",
                            english = "nichinichi ni kokoro tsukusuru sono kata wa\nmune o osame yo sue wa tanomoshi",
                            commentary = "二十八, 나날이 이 길을 위해 마음을 다하여 이바지해 온 사람은 이젠 머지않았으니 어떤 가운데서도 마음을 쓰러뜨리지 말고 따라오라. 끝에는 반드시 믿음직한 길이 나타날 것이다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이제부터는 높은산의 못에 뛰어들어가\n 어떤 찌꺼기도 청소하리라 2-29\n",
                            japanese = "これからハ高山いけいとびはいり\nいかなごもくもそうぢするなり",
                            english = "korekara wa takayama ike i tobihairi\nikana gomoku mo soji suru nari",
                            commentary = "二十九, 이제부터는 길을 내기 어려운 곳에도 들어가 아무리 마음이 혼탁한 자라도 티끌을 털어내어 청정하게 할 것이다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 찌꺼기만 깨끗이 치워버리면\n 그 다음에 물은 맑아지리라 2-30\n",
                            japanese = "こもくさいすきやかだしてしもたなら\nあとなる水ハすんであるなり",
                            english = "gomoku sai sukiyaka dashite shimota nara\nato naru mizu wa sunde aru nari",
                            commentary = "三十, 아무리 티끌이 쌓여 있는 자라도 그 마음의 더러움을 완전히 씻어 버린다면, 그 다음은 맑은물처럼 깨끗해진다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이제부터는 미칠곳과 미친곳에 대해 말한다\n 무슨 말을 하는지 모를 테지 2-31\n",
                            japanese = "これからハからとにほんのはなしする\nなにをゆうともハかりあるまい",
                            english = "korekara wa kara to nihon no hanashi suru\nnani o yu tomo wakari arumai",
                            commentary = "三十一, 지금부터는 미칠곳과 미친곳에 대해 이야기를 하겠는데, 어버이신이 무슨 말을 할지 잘 모를 것이다.\n이 노래 이하 34수의 노래까지는 제2호 47수의 주석 참조."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 모르는자가 미친곳의 땅에 들어와서\n 멋대로 하는 것이 신의 노여움 2-32\n",
                            japanese = "とふぢんがにほんのぢいゝ入こんで\nまゝにするのが神のりいふく",
                            english = "tojin ga nihon no jii i irikonde\nmamani suru no ga Kami no rippuku",
                            commentary = "三十二, 어버이신의 가르침을 아직 모르는 자가 미친 곳에 들어가 뽐내며 제멋대로 하고 있는 것이 어비이신으로서는 참으로 안타까운 일이다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 차츰차츰 미친곳을 구제할 준비를 하니\n 모르는자도 신이 마음대로 하리라 2-33\n",
                            japanese = "たん／＼とにほんたすけるもよふだて\nとふじん神のまゝにするなり",
                            english = "dandan to nihon tasukeru moyodate\ntojin Kami no mamani suru nari",
                            commentary = "三十三, 어버이신은 차츰 미친곳에 어버이신의 참뜻을 널리 알려서 세계 인류를 구제할 준비를 하고 있으므로, 아직 어버이신의 가르침을 모르는 자에게도 머지않아 신의를 납득시켜 용솟을치며 신은(神恩)을 입도록 한다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 앞으로는 미칠곳과 미친곳을 구분한다\n 이것을 분간하게 되면 온 세상 안정이 된다 2-34\n",
                            japanese = "このさきハからとにほんをハけるてな\nこれハかりたらせかいをさまる",
                            english = "konosaki wa kara to nihon o wakeru de na\nkore wakaritara sekai osamaru",
                            commentary = "三十四, 금후는 미칠곳과 미친곳을 구분하는데 이것만 알게 되면 사람들의 마음은 맑아져 세상은 절로 안정이 된다.\n미친곳이란 태초에 어버이신님이 이 세상 인간을 창조하신 터전이 있는 곳, 따라서 이 가르침을 맨 먼저 편 세계구제의 본고장이 있는 곳을 말한다. 또 어버이신님의 뜻을 이미 알고 있는 자를 말한다. 미칠곳이란 어버이신님의 뜻이 전해질 곳, 또는 다음에 어버이신님의 가르침을 전해 듣게 될 자를 말한다.\n아는자란 어버이신님의 가르침을 먼저 들은 자, 즉 어버이신님의 가르침을 이미 깨달은 자를 만한다.\n모르는자란 이 가르침을 다음에 듣게 될 자, 즉 아직 어버이신님의 가르침을 모르는 자를 말한다.미칠곳과 미친곳에 관한 일련의 노래는 친필을 집필하실 당시, 과학기술을 도입하기에 급급한 나머지, 물질문명에만 현혹되어 문명 본래의 생명인 인류애와 공존공영(共存共榮)의 정신은 이해하려 하지 않고 오직 물질주의, 이기주의로만 흐르고 있던 당시 사람들에게 엄한 경고를 하시고, 빨리 어버이신님의 뜻을 깨달아 구제한줄기의 정신으로 나아가라고 격려하신 노래이다. 즉, 어버이신님의 눈으로 보시면 온 세상 사람들은 모두 귀여운 자녀이다. 따라서 어버이신님의 뜻을 알든 모르든, 또 가르침을 먼저 들은 자든 나중에 듣는 자든 여기에는 아무런 차별도 없이 온 세상 사람들을 궁극적으로 똑같이 구제하시려는 것이 어버이신님의 어버이마음이다. 그러므로 어버이신은 사람들의 마음이 맑아져서 서로가 형제자며임을 자각하고, 서로 위하고 서로 돕는 마음이 되어 하루라도 빨리 즐거운 삻을 누릴 날이 오기를 서두르고 계신다.(제10호 55, 56수의 주석, 제 12호 7수의 주석 참조)"
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이제까지 윗사람들은 신의 마음 모르고서\n 세상 보통일로 생각하고 있었다 2-35\n",
                            japanese = "いまゝでハ上たる心ハからいで\nせかいなみやとをもていたなり",
                            english = "imamade wa kami taru kokoro wakaraide\nsekainami ya to omote ita nari",
                            commentary = "三十五, 지금까지 윗사람들은 어버이신의 마음을 모르기 때문에 이 길의 참뜻을 이해하지 못하고 세상에 흔히 있는 보통 일로 생각하고 있었다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이제부터는 신이 몸 안에 들아가서\n 마음을 속히 깨닫도록 할 테다 2-36\n",
                            japanese = "これからハ神がたいない入こんで\n心すみやかわけてみせるで",
                            english = "korekara wa Kami ga tainai irikonde\nkokoro sumiyaka wakete miseru de",
                            commentary = "三十六, 이제부터는 어버이신이 그러한 사람들의 몸속에 들어가서 이 길의 진가를 깨닫도록 할 것이다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 나날이 모여드는 사람에게 오지 마라\n 해도 도리어 점점 더 불어난다 2-37\n",
                            japanese = "にち／＼によりくる人にことハりを\nゆへばだん／＼なをもまあすで",
                            english = "nichinichi ni yorikuru hito ni kotowari o\nyueba dandan naomo masu de",
                            commentary = "三十七, 매일 교조를 그리며 오는 사람들에게 오지 마라고 거절을 해도, 오히려 찾아오는 사람들은 점차 불어날 뿐이다. 교조님을 그리며 모여 오는 사람들에게 교조님은 이 길의 가르침을 일러주셨는데, 세상에는 이 가르침을 올바로 이해하는 사람이 적었기 때문에 여러 가지 오해를 사고, 그로 인하여 각계 각층으로부터 자주 방해를 받았다. 교조님의 측근들은, 이래 서는 교조님께 누를 끼칠 뿐이라는 생에서 참배하러 오는 사람들을 못오도록 거절 했으나, 어버이신님의 의도는 이 가르침을 널리 펴려는 것이었으므로, 아무래도 교조님을 그리며 돌아오는 사람들을 제지할 수 없을뿐더러, 도리어 점차 불어나게 될 뿐이라는 뜻이다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 아무리 많은 사람이 오더라도\n 조금도 염려 말라 신이 맡는다 2-38\n",
                            japanese = "いかほどのをふくの人がきたるとも\nなにもあんぢな神のひきうけ",
                            english = "ika hodono oku no hito ga kitaru tomo\nnanimo anjina Kami no hikiuke",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이 세상 창조의 진기한 감로대\n 이것으로 온 세상 안정이 된다 2-39\n",
                            japanese = "めつらしいこのよはじめのかんろたい\nこれがにほんのをさまりとなる",
                            english = "mezurashii kono yo hajime no Kanrodai\nkore ga nihon no osamari to naru",
                            commentary = "三十九, 이 세상 인간창조의 리를 나타내는 진기한 감로대가 세워져서 감로대근행을 올리게 되면, 그 영덕(靈德)에 의해 어버이신의 참뜻이 온 세상에 널리 알려지게 되고, 그로 인해 온 세상 사람들은 용솟음치며 즐거운 삶을 누리게 된다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 높은산에 물과 불이 보인다\n 누구의 눈에도 이것이 안 보이는가 2-40\n",
                            japanese = "高山に火と水とがみへてある\nたれがめへにもこれがみへんか",
                            english = "takayama ni hii to mizu to ga miete aru\ntare ga me nimo kore ga mien ka",
                            commentary = "四十, 윗사람들에게도 어버이신의 수호가 알려지고 있는데도 모두들은 이것을 모르는가.\n물과 불이란 '물과 불은 근본의 수호'라고 가르치신 바와 같이, 어버이신님의 절대 하신 수호를 의미한다. 물이라면 음료수도 물, 비도 물, 몸의 수분도 물, 해일도 물이다. 불이라면 등불도 불, 체온도 불, 화재도 불이다. "
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 차츰차츰 어떤 말도 일러주었다\n 확실한 것이 보이고 있으므로 2-41\n",
                            japanese = "たん／＼といかなはなしもといてある\nたしかな事がみゑてあるから",
                            english = "dandan to ikana hanashi mo toite aru\ntashikana koto ga miete aru kara",
                            commentary = "四十一, 지금까지도 여러 가지로 장래의 일을 일러주었는데, 그것은 어버이신이 무엇이든 장래의 일을 잘 알고 있기 때문이다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 행복을 누리도록 충분히 수호하고 있다\n 몸에 받게 될 테니 이것을 즐거워하라 2-42\n",
                            japanese = "しやハせをよきよふにとてじうぶんに\nみについてくるこれをたのしめ",
                            english = "shiyawase o yoki yoni tote jubun ni\nmi ni tsuite kuru kore o tanoshime",
                            commentary = "四十二, 모든 사람들이 행복해지도록 어버이신은 충분히 수호하고 있는 만큼, 마음을 바르게 하여 어버이신의 뜻에 따르는 자는 누구나 행복을 누리게 된다. 이것을 낙으로 삼아 살아가도록."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 무엇이든 허욕 부린 그 다음에는\n 신의 노여움 나타날 거야 2-43\n",
                            japanese = "なにもかもごふよくつくしそのゆへハ\n神のりいふくみへてくるぞや",
                            english = "nanimo kamo goyoku tsukushi sono ue wa\nKami no rippuku miete kuru zo ya",
                            commentary = "四十三, 무엇이든지 어버이신의 가르침을 지키지 않고 사리 사욕을 부린 자는 반드시 어버이신의 노여움이 나타나 괴로움을 겪게 될 것이다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 차츰차츰 15일부터 나타나기 시작하리라\n 선과 악이 모두 나타날 것이니 2-44\n",
                            japanese = "たん／＼と十五日よりみゑかける\n善とあくとハみなあらハれる",
                            english = "dandan to ju go nichi yori miekakeru\nzen to aku towa mina arawareru",
                            commentary = "四十四, 차츰 15일부터 여러 가지 일이  나타날 것이다. 선은 선, 악은 악으로 어버이신이 선명하게 구분해 보이리라."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이 이야기는 누구예 일이라 말하지 않아\n 나타나거든 모두 납득하라 2-45\n",
                            japanese = "このはなしとこの事ともゆハんてな\nみへてきたればみなとくしんせ",
                            english = "kono hanashi doko no koto tomo yuwan de na\nmiete kitareba mina tokushin se",
                            commentary = "四十五, 지금까지 말한 이 가르침은 특별히 누구를 두고 한 것이 아니지만 조만간 나타날 것이니 그때가 되면 납득하게 될 것이다.\n이상 3수에 관련된 사료(史料)로는 1872년 야마또 지방 히가시와까이(東若井) 마을에 살던 마쯔오 이찌베에(松尾市兵衛)의 이야기가 전해지고 있다. 이찌베에는 이 길의 신앙에 매우 열성적이어서 당시 뛰어난 제자 가운데 한 사람으로 열심히 가르침을 전하고 있었는데, 매사에 이유가 많고 성질이 강한 사람인지라 막상 자신에 관한 중요한 일에 부딪치면 어버이신님의 가르침을 순직하게 그대로 받아들이지를 못했다. 그로 말미암아 어버이신님으로부터 손질을 받고 있던 그의 장남이 음력 7월 보름 백중날에 출직하고 말았던 것이다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 높은산에 있는 아는자와 모르는자를\n 구분하는 것도 이것도 기둥이야 2-46\n",
                            japanese = "高山のにほんのものととふぢんと\nわけるもよふもこれもはしらや",
                            english = "takayama no nihon no mono to tojin to\nwakeru moyo mo kore mo hashira ya",
                            commentary = "四十六, 지도층에 있는 사람 가운데, 어버이신의 뜻을 깨달아 마음이 맑아진 사람과 인간의 지혜와 힘만을 믿고 아직 어버이신의 가르침을 모르는 사람을 구분하는 것도 이 감로대의 영덕으로 하는 것이다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 모르는자와 아는자를 구분하는 것은\n 물과 불을 넣어서 구분하리라 2-47\n",
                            japanese = "とふじんとにほんのものとハけるのハ\n火と水とをいれてハけるで",
                            english = "tojin to nihon no mono to wakeru no wa\nhii to mizu to o irete wakeru de",
                            commentary = "四十七, 아직 어버이신의 가르침을 모르는 사람과 어버이신의 뜻을 깨달은 사람을 구분하는 것은, 어버이신의 절대한 힘에 의한 것이다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이번에는 문 안에 있는 건물을\n 서둘러서 헐어 버려라 3-1\n",
                            japanese = "このたびハもんのうちよりたちものを\nはやくいそいでとりはらいせよ",
                            english = "konotabi wa mon no uchi yori tachimono o\n" +
                                    "hayaku isoide toriharai seyo",
                            commentary = "一,이번에는 이 길의 발전에 방해가 되는 진터 안의 건물을 철거해 버려라.\n어버이신님은 교조님이 거처할 건물을 짓도록 서두르셨다. 그래서 이해에는 대문과 이에 붙은 거처방 및 창고를 짓기 시작했다. 그러기 위해서는 먼저 새끼줄을 쳐서 건축할 위치를 정해야 했는데, 그 무렵 집터 안에는 방해가 되는 건물이 있었으므로 이를 철거하여 빨리 깨끗이 청소하다록 서두르셨던 것이다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 빨리 청소를 끝낸 다음에는\n 새로 건축할 준비를 서두르기 부탁이야 3-2\n",
                            japanese = "すきやかにそふぢしたてた事ならば\nなハむねいそぎたのみいるそや",
                            english = "sukiyaka ni soji shitateta koto naraba\n" +
                                    "nawamune isogi tanomi iru zo ya",
                            commentary = "二,빨리 집터 안의 구석구석까지 빠짐없이 청소를 하고 나면 새 집을 지을 준비를 서두르도록.\n이리하여 새로 지은 건물은 1875년에 준공되었는데, 교조님은 이해부터 1883년까지 8년 동안 여기서 가르침을 배푸셨다. 중남의 문간채라 불리는 건물이 바로 이것이다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 진실로 청소를 한 그 다음에는\n 신한줄기로 마음 용솟음친다 3-3\n",
                            japanese = "しんぢつにそふぢをしたるそのゝちハ\n神一ぢよで心いさむる",
                            english = "shinjitsu ni soji o shitaru sono nochi wa\n" +
                                    "Kami ichijo de kokoro isamuru",
                            commentary = "三,진실로 개끗이 청소를 하고 나면 그 다음에는 오직 이 길만을 항해 나아가게 되어 마음이 저절로 용솟음친다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 차츰차츰 세상 사람들의 마음 용솟음치면\n 이것으로 온 세상 안정이 된다 3-4\n",
                            japanese = "だん／＼とせかいの心いさむなら\nこれがにほんのをさまりとなる",
                            english = "dandan to sekai no kokoro isamu nara\n" +
                                    "kore ga nihon no osamari to naru",
                            commentary = "四,점차로 이 길이 퍼져 세상 사람들의 마음이 용솟음치게 되면, 어버이신의 뜻을 사람들이 깨닫게 되어 온 세상은 절로 안정이 된다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이제까지는 어떤 일도 분간 못했다\n 이제부터 보이는 신기한 징조가 3-5\n",
                            japanese = "いまゝでハなによの事もハかりない\nこれからみゑるふしぎあいづが",
                            english = "imamade wa nani yono koto mo wakari nai\n" +
                                    "korekara mieru fushigi aizu ga",
                            commentary = "五,이제까지는 어떤 일에 대해서도 어버이의 마음을 깨닫지 못했으나, 앞으로는 신기한 징조가 나타날 것이니 그것으로 어버이신이 뜻하는 바를 잘 납득해야 한다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 아니 오는 자에게 무리로 오라고는 않는다\n 따라온다면 언제까지나 좋아 3-6\n",
                            japanese = "こんものにむりにこいとハゆうでなし\nつきくるならばいつまでもよし",
                            english = "kon mono ni muri ni koi towa yu de nashi\n" +
                                    "tsukikuru naraba itsu made mo yoshi",
                            commentary = "六,신앙은 자유이므로 이 길의 가르침에 귀의하기를 싫어하는 사람에게 무리로 신앙하라고는 결코 말하지 않는다. 그러나 교조님을 흠모하여 따라오는 사람에게는 언제까지나 좋은 길이 열린다는 뜻이다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이제부터는 물에 비유해서 말한다\n 맑음과 탁함으로 깨닫도록 하라 3-7\n",
                            japanese = "これからハ水にたとゑてはなしする\nすむとにごりでさとりとるなり",
                            english = "korekara wa mizu ni tatoete hanashi suru\n" +
                                    "sumu to nigori de satori toru nari",
                            commentary = "七,이제부터는 물을 예로 들어 일러주겠는데, 물이 맑다는 것은 사람들의 마음에 티끌이 없이 깨끗하다는 뜻이며, 탁하다는 것은 사람들의 마음에 티끌이 쌓여 있다는 뜻이므로, 이에 따라 각자의 마음을 반성해야 한다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 진실로 신이 마음 서두르는 것은\n 중심의 기둥을 빨리 세우고 싶다 3-8\n",
                            japanese = "しんぢつに神の心のせきこみわ\nしんのはしらをはやくいれたい",
                            english = "shinjitsu ni Kami no kokoro no sekikomi wa\n" +
                                    "shin no hashira o hayaku iretai",
                            commentary = "八,어버이신이 진실로 서두르고 있는 것은 하루라도 빨리 중심을 정하고 싶은 것이다.\n중심의 기둥이란 원래 건축용어인데, 무슨 일에서나 가장 중심이 되는 것을 뜻한다. 본교에서는 근행인 경우 김로대를 가르키고, 사람인 경우 이 길의 중심이 되는 분을 가리키며, 마음인 경우에는 중심되는 생각을 말한다. 즉, 인간창조의 리를 나타내며, 구제한줄기의 신앙의 중심 지점을 나타내는 감로대를 '온 세상의 중심의 기둥이야'라고 말씀하셨고.(제8호 85수 참조)또 본교의 중심되는 분을 '안을 다스릴 중심의 기둥'이라 말씀하셨다.(제3호 56수 참조)따라서 이 노래는 감로대근행의 완성을 목표로 앞에서 말한 두가지를 건설, 확립할 것을 서두르시는 노래이다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이 기둥 빨리 세우려고 생각하지만\n 탁한 물이라 장소를 모르겠다 3-9\n",
                            japanese = "このはしらはやくいれよとをもへども\nにごりの水でところわからん",
                            english = "kono hashira hayaku ireyo to omoedomo\n" +
                                    "nigori no mizu de tokoro wakaran",
                            commentary = "九,이 기둥을 빨리 세우려고 서두르게 있으나, 모든 사람들의 마음이 흐리기 때문에 세 울 수가 없다.\n당시 감로대의 모형은 만들어져 있었으나, 이를 세워야 할 터전은 아직 정해지지 않았다. 그리고 어버이신님은 나까야마(中山)가의 후계자로서, 앞으로 이 길의 진주(眞柱)가 될 사람을 이찌노모또 마을에 사는 가지모또가의 3남인 신노스께(真之亮)님으로 정하시고, 빨리 집터에 정주시키려 하셨으나, 측근에서는 이러한 어버이신님의 뜻을 깨닫지 못하고 각자 제멋대로 생각하기 때문에 사람들의 의견이 일치되지 않았는데, 이를 두고 하신 말씀이다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이 물을 빨리 맑힐 준비\n 숯과 모래로 걸러서 맑혀라 3-10\n",
                            japanese = "この水をはやくすまするもよふだて\nすいのとすなにかけてすませよ",
                            english = "kono mizu o hayaku sumasuru moyodate\n" +
                                    "suino to suna ni kakete sumase yo",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이 숯은 다른 것이라 생각 말라\n 가슴속과 입이 모래요 숯이라 3-11\n",
                            japanese = "このすいのどこにあるやとをもうなよ\nむねとくちとがすなとすいのや",
                            english = "kono suino doko ni aru ya to omouna yo\n" +
                                    "mune to kuchi to ga suna to suino ya",
                            commentary = "十一,이 숯은 다른 것이 아니다. 각자의 마음과 입이 솣이다. 즉, 깨닫고 개ㄱ우침으로써 마음을 작 닦아 어버이신의 뜻에 순응해야 한다.\n가슴속과 입이란 깨닫고 깨우친다는 뜻."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이 이야기를 속히 깨닫게 되면\n 당장에 세울 중심의 기둥 3-12\n",
                            japanese = "このはなしすみやかさとりついたなら\nそのまゝいれるしんのはしらを",
                            english = "kono hanashi sumiyaka satori tsuita nara\n" +
                                    "sono mama ireru shin no hashira o",
                            commentary = "十二,사람들이 이러한 어버이신의 말을 듣고 빨리 깨달아, 어버이신의 의도에 마음을 모은다면 당장이라도 중심의 기둥을 세울 것이다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 기둥만 단단히 세우게 되면\n 이 세상 확실히 안정이 된다 3-13\n",
                            japanese = "はしらさいしいかりいれた事ならば\nこのよたしかにをさまりがつく",
                            english = "hashira sai shikkari ireta koto naraba\n" +
                                    "kono yo tashikani osamari ga tsuku",
                            commentary = "十三,이 기둥만 터전에 단단히 세우게 되면, 그것으로 이 길의 기초가 확립되고, 가르침은 점차로 널리 퍼져 세상은 틀림없이 안정이 된다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이 이야기는 깨달아야만 하는 만큼\n 이것을 깨달으면 증거가 나타날 거야 3-14\n",
                            japanese = "このはなしさとりばかりであるほどに\nこれさとりたらしよこだめしや",
                            english = "kono hanashi satori bakari de aru hodoni\n" +
                                    "kore satoritara shoko dameshi ya",
                            commentary = "十四,이 길은 깨닫고 깨우치는 길이므로 비유해서 하는 말을 잘 깨달아, 진주를 정하고 감로대를 세우며 인원을 갖추어서 감로대근행을 하게 된다면, 그것으로 세상은 확실히 안정이 된다는 어버이신의 의도가 실증될 것이다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이 세상 인간을 창조한 으뜸인 신\n 누구도 아는 자는 없으리라 3-15\n",
                            japanese = "このよふのにんけんはじめもとの神\nたれもしりたるものハあるまい",
                            english = "kono yo no ningen hajime moto no Kami\n" +
                                    "tare mo shiritaru mono wa arumai",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 진흙바다 속에서 수호를 가르치\n 그것이 차츰차츰 번성해진 거야 3-16\n",
                            japanese = "どろうみのなかよりしゆごふをしへかけ\nそれがたん／＼さかんなるぞや",
                            english = "doroumi no naka yori shugo oshiekake\n" +
                                    "sore ga dandan sakan naru zo ya",
                            commentary = "十六,태초에 月日 어버이신님이 진흙바다 가운데서 도구들을 끌어모아 인간을 창조하신 이래 영묘한 수호를 베풀어 주심으로써 마침내 오늘날과 같은 인간으로 성장한 것이다.(제6호 29~31 참조)"
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이번에 구제한줄기를 가르치는\n 이것도 없던 일 시작하는 거야 3-17\n",
                            japanese = "このたびハたすけ一ぢよをしゑるも\nこれもない事はしめかけるで",
                            english = "konotabi wa tasuke ichijo oshieru mo\n" +
                                    "kore mo nai koto hajime kakeru de",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이제까지 없던 일을 시작하는 것은\n 근본을 창조한 신이기 때문에 3-18\n",
                            japanese = "いまゝでにない事はじめかけるのわ\nもとこしらゑた神であるから",
                            english = "imamade ni nai koto hajime kakeru no wa\n" +
                                    "moto koshiraeta Kami de aru kara",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 나날이 신의 이야기가 첩첩이\n 쌓여 있어도 말하려야 말할 수 없다 3-19\n",
                            japanese = "にち／＼に神のはなしがやま／＼と\nつかゑてあれどとくにとかれん",
                            english = "nichinichi ni Kami no hanashi ga yamayama to\n" +
                                    "tsukaete aredo toku ni tokaren",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 무엇이든 말 못할 것은 없겠지만\n 마음을 맑혀서 듣는 자가 없다 3-20\n",
                            japanese = "なにゝてもとかれん事ハないけれど\n心すましてきくものハない",
                            english = "nani nitemo tokaren koto wa nai keredo\n" +
                                    "kokoro sumashite kiku mono wa nai",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 빨리 마음 맑혀서 듣는다면\n 만가지 이야기 모두 일러준다 3-21\n",
                            japanese = "すみやかに心すましてきくならば\nよろづのはなしみなときゝかす",
                            english = "sumiyaka ni kokoro sumashite kiku naraba\n" +
                                    "yorozu no hanashi mina tokikikasu",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이 세상에 확실한 징험이 나타난다\n 이것만은 틀릴이 없다고 생각하라 3-22\n",
                            japanese = "このよふのたしかためしかかけてある\nこれにまちがいないとをもゑよ",
                            english = "kono yo no tashika tameshi ga kakete aru\n" +
                                    "kore ni machigai nai to omoe yo",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이 징험이 빨리 나타나기만 하면\n 어떤 이야기도 모두 참인 거야 3-23\n",
                            japanese = "このためしすみやかみゑた事ならば\nいかなはなしもみなまことやで",
                            english = "kono tameshi sumiyaka mieta koto naraba\n" +
                                    "ikana hanashi mo mina makoto ya de",
                            commentary = "二十三,이 징험이 실제로 빨리 나타나기만 하면 어버이신의 이야기는 어떤 것도 틀림이 없음을 알게 될 것이다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 무엇이든 어떤 이야기도 일러줄 테니\n 무슨 말을 해도 거짓이라 생각 말라 3-24\n",
                            japanese = "なにもかもいかなはなしもとくほどに\nなにをゆうてもうそとをもうな",
                            english = "nanimo kamo ikana hanashi mo toku hodoni\n" +
                                    "nani o yutemo uso to omouna",
                            commentary = "二十四,지금까지 사람이 모르는, 사람의 능력으로는 생각지도 못하는 것을 깨우쳐 주려고 하는데, 어떤 것이라도 어버이신이 하는 말은 틀림이 없다고 생각해야 한다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 눈에 안 보이는 신이 하는 말 하는 일\n 무엇을 하는지 좀처럼 모르겠지 3-25\n",
                            japanese = "めへにめん神のゆう事なす事わ\nなにをするとも一寸にしれまい",
                            english = "me ni men Kami no yu koto nasu koto wa\n" +
                                    "nani o suru tomo chotto ni shiremai",
                            commentary = "二十五,사람의 눈에 보이지 않는 어버이신이 하는 말, 하는 일이므로 좀처럼 깨달을 수도 없고, 또 앞으로 어떤 일을 할지 전혀 모를 것이다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이내 곧 나타나는 이야기인 만큼\n 이것이 확실한 증거인 거야 3-26\n",
                            japanese = "はや／＼とみへるはなしであるほどに\nこれがたしかなしよこなるぞや",
                            english = "hayabaya to mieru hanashi de aru hodoni\n" +
                                    "kore ga tashikana shoko naru zo ya",
                            commentary = "二十六,어버이신이 일러준 것은 이내 나타나기 때문에, 이것이야말로 어버이신이 하는 말은 틀림이 없다는 좋은 증거가 될 것이다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이것을 보고 무엇을 듣든지 즐거워하라\n 어떤 이야기도 모두 이와 같으니 3-27\n",
                            japanese = "これをみてなにをきいてもたのしめよ\nいかなはなしもみなこのどふり",
                            english = "kore o mite nani o kiitemo tanoshime yo\n" +
                                    "ikana hanashi mo mina kono dori",
                            commentary = "二十七,이렇게 나타나는 증거를 보고 장래를 낙으로 삼아라, 어버이신의 가르침은 모두 이와 같으리라."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 남의 것을 빌리면 이자가 붙는다\n 빨리 갚고 치사하도록 하라 3-28\n",
                            japanese = "人のものかりたるならばりかいるで\nはやくへんさいれゑをゆうなり",
                            english = "hito no mono karitaru naraba ri ga iru de\n" +
                                    "hayaku hensai re o yu nari",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 아이가 밤에 운다는 생각은 틀린 거야\n 아이가 우는 것이 아니라 신의 타이름이니 3-29\n",
                            japanese = "子のよなきをもふ心ハちがうでな\nこがなくでな神のくときや",
                            english = "ko no yonaki omou kokoro wa chigau de na\n" +
                                    "ko ga nakude nai Kami no kudoki ya",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 빨리 신이 알려 주는 것이니\n 무슨 일이든 단단히 분간하라 3-30\n",
                            japanese = "はや／＼と神がしらしてやるほどに\nいかな事でもしかときゝわけ",
                            english = "hayabaya to Kami ga shirashite yaru hodoni\n" +
                                    "ikana koto demo shikato kikiwake",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 부모들의 마음 틀리지 않도록\n 어서 생각해 보는 것이 좋을 거야 3-31\n",
                            japanese = "をや／＼の心ちがいのないよふに\nはやくしやんをするがよいぞや",
                            english = "oyaoya no kokoro chigai no nai yoni\n" +
                                    "hayaku shiyan o suru ga yoi zo ya",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 진실로 남을 구제할 마음이라면\n 신의 타이름은 아무것도 없는 거야 3-32\n",
                            japanese = "しんぢつに人をたすける心なら\n神のくときハなにもないぞや",
                            english = "shinjitsu ni hito o tasukeru kokoro nara\n" +
                                    "Kami no kudoki wa nanimo nai zo ya",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 각자가 지금만 좋으면 그만이라고\n 생각하는 마음은 모두 틀리는 거야 3-33\n",
                            japanese = "めへ／＼にいまさいよくばよき事と\nをもふ心ハみなちがうでな",
                            english = "meme ni imasai yokuba yoki koto to\n" +
                                    "omou kokoro wa mina chigau de na",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 처음부터 아무리 큰길을 걷고 있어도\n 장래 있을 좁은 길 보지 못하니 3-34\n",
                            japanese = "てがけからいかなをふみちとふりても\nすゑのほそみちみゑてないから",
                            english = "degake kara ikana omichi toritemo\n" +
                                    "sue ni hosomichi miete nai kara",
                            commentary = "三十四,누구든지 처음부터 편안한 길을 걷고 싶어하는 것은 앞으로 다가올 고생의 길을 모르기 때문이다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 인간안 어리석기 때문에\n 앞으로 닥칠 길을 전연 모른다 3-35\n",
                            japanese = "にんけんハあざないものであるからに\nすゑのみちすじさらにわからん",
                            english = "ningen wa azanai mono de aru karani\n" +
                                    "sue no michisuji sarani wakaran",
                            commentary = "三十五,인간은 어리석기 때문에 장래 어떤 길을 걸을지 전혀 모른다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 지금의 일을 무엇이라 말하지 마라\n 장래에 한길이 나타나리라 3-36\n",
                            japanese = "いまの事なにもゆうでハないほどに\nさきのをふくハんみちがみへるで",
                            english = "ima no koto nanimo yu dewa nai hodoni\n" +
                                    "saki no okwan michi ga mieru de",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 지금 어떤 길을 걸더도 탄식하지 마라\n 장래 있을 본길을 낙으로 삼아라 3-37\n",
                            japanese = "いまのみちいかなみちでもなけくなよ\nさきのほんみちたのしゆでいよ",
                            english = "ima no michi ikana michi demo nagekuna yo\n" +
                                    "saki no honmichi tanoshun de iyo",
                            commentary = "三十六, 三十七,어버이신은 현재 일만을 가리켜 말하는 것이 아니므로, 지금 걷고 있는 길이 아무리 괴롭더라도 절대로 불평을 하거나 불만을 품어서는 안된다. 장래에는 반드시 큰길이 나타날 것이니 그것을 낙으로 삼고 즐겁게 걸어가야 한다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 진실로 구제한줄기 마음일 것 같으면\n 아무 말 아니해도 확실히 받아들인다 3-38\n",
                            japanese = "しんぢつにたすけ一ぢよの心なら\nなにゆハいでもしかとうけとる",
                            english = "shinjitsu ni tasuke ichijo no kokoro nara\n" +
                                    "nani yuwai demo shikato uketoru",
                            commentary = "三十八,어떻든 구제하려는 마음만 있다면, 입으로 말하지 않더라도 어버이신은 그 마음을 받아들여 반드시 리에 맞는 수호를 한다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 입으로만 아첨하는 것 쓸데없어\n 마음 가운데 정성만 있다면 3-39\n",
                            japanese = "口さきのついしよはかりハいらんもの\nしんの心にまことあるなら",
                            english = "kuchi saki no tsuisho bakari wa iran mono\n" +
                                    "shin no kokoro ni makoto aru nara",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 차츰차츰 무엇이든 이 세상은\n 신의 몸이야 생각해 보라 3-40\n",
                            japanese = "たん／＼となに事にてもこのよふわ\n神のからだやしやんしてみよ",
                            english = "dandan to nani goto nitemo kono yo wa\n" +
                                    "Kami no karada ya shiyan shite miyo",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 인간은 모두가 신의 대물이야\n 무엇으로 알고 쓰고 있는가 3-41\n",
                            japanese = "にんけんハみな／＼神のかしものや\nなんとをもふてつこているやら",
                            english = "ningen wa minamina Kami no kashimono ya\n" +
                                    "nanto omote tsukote iru yara",
                            commentary = "四十, 四十一,이 세상 만물은 모두 어버이신님이 창조하신 것이며, 전우주는 어버이신님의 몸이다. 따라서 인간도 자신의 능력으로 된 것이 아니다. 어버이신님이 만드신 것을 어버이신님으로부터 빌려 받아, 어버이신님의 품속인 이 세계에서 어버이신님의 수호에 의해 살아가고 있는 것이다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 올해는 진기한 일을 시작해서\n 이제까지 모르던 일을 할 거야 3-42\n",
                            japanese = "ことしにハめつらし事をはじめかけ\nいまゝでしらぬ事をするぞや",
                            english = "kotoshi niwa mezurashi koto o hajime kake\n" +
                                    "imamade shiranu koto o suru zo ya",
                            commentary = "四十二,교조님은 1874년 음력 5월에 신악탈을 가지러 삼마이뎅(三昧田)에 가셨다. 그리고 일반 사람들 뿐만 아니라 높은산에도 포교를 하여 이 길의 가르침을 알리시려고, 이해 10월에 나까따(仲田), 마쯔오 두 사람을 오야마또(大和)신사에 보내셨다. 그 결과 신직(神職)과 관헌의 주의를 끌게 되어, 음력 11월 15일에는 야마무라고뗑(山村御殿)에 불려 가신 것을 비롯하여, 그 뒤에도 자주 핍박을 받으셨는데, 어버이신님은 이것을 오히려 이 길을 넗혀 나가는 하나의 방법으로 여기셨다. 또 붉은 옷을 입으신 것도 이해부터였다.(제5호 56,57수의 주석 참조)"
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 지금까지는 무슨 일도 세상 보통 일로 여겼으나\n 이제부터는 진심으로 납득하리라 3-43\n",
                            japanese = "いまゝでハなによの事もせかいなみ\nこれからわかるむねのうちより",
                            english = "imamade wa nani yono koto mo sekainami\n" +
                                    "korekara wakaru mune no uchi yori",
                            commentary = "四十三,지금까지는 어떤 일에 대해서도 이러한 정신을 몰랐으나, 이제부터는 충분히 일러줄 것이므로 확실히 알게 될 것이다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이번에 구제한줄기 시작하는 것도\n 내 몸으로 체험을 하고 나서 3-44\n",
                            japanese = "このたびハたすけ一ちよにかゝるのも\nわがみのためしかゝりたるうゑ",
                            english = "konotabi wa tasuke ichijo ni kakaru no mo\n" +
                                    "waga mi no tameshi kakaritaru ue",
                            commentary = "四十四,이번에 구제한줄기의 길을 가르침에 있어 몸소 겪으신 체험을 바탕으로 하여 사람들을 구제한 것을 말씀하신 것이다. 순산허락도 몸소 순산시험을 체험하신 다음 내려 주셨고, 또 히가시와까이 마을에 사는 마쯔오 이쩨베에 대에 구제하러 가셨을 때도 단식한 지 30여일이 지난 무렵으로서, 먹으려야 먹을 수 없는 질별의 괴로움을 체험하고 나서 가셨던 것이다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 구제라 해서 절하고 비는 것이 아니오\n 물어 보고 하는 것도 아니지만 3-45\n",
                            japanese = "たすけでもをかみきとふでいくてなし\nうかがいたてゝいくでなけれど",
                            english = "tasuke demo ogami kito de ikude nashi\n" +
                                    "ukagai tatete iku de nakeredo",
                            commentary = "四十五,사람을 구제하는 것도 지금까지처럼 단순히 절을 하거나, 기도를 하거나, 물어 보거나 해서 구제하는 그런 것이 아니다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 여기서 만가지 일을 일러준다\n 신한줄기로 가슴속에서 3-46\n",
                            japanese = "このところよろつの事をときゝかす\n神いちじよでむねのうちより",
                            english = "kono tokoro yorozu no koto o tokikikasu\n" +
                                    "Kami ichijo de mune no uchi yori",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 잘 깨닫도록 가슴속 깊이 생각하라\n 남을 구제하면 제 몸 구제받는다 3-47\n",
                            japanese = "わかるよふむねのうちよりしやんせよ\n人たすけたらわがみたすかる",
                            english = "wakaru yo mune no uchi yori shiyan seyo\n" +
                                    "hito tasuketara waga mi tasukaru",
                            commentary = "四十六, 四十七,이 터전에서는 인간창 이래의 어버이신의 수호를 자세히 일러주고 있는데, 이야기를 깊이 잘 깨달아서 신한줄기의 마음을 작정하고 구제한줄기의 길로 나아가라. 남을 구제해야만 그 리에 의해서 제 몸이 구제받게 된다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 높은산은 온 세상 사람들을 생각대로\n 제멋대로 하지만 앞을 보지 못한다 3-48\n",
                            japanese = "高山ハせかい一れつをもうよふ\nまゝにすれともさきハみゑんで",
                            english = "takayama wa sekai ichiretsu omou yo\n" +
                                    "mamani suredomo saki wa mien de",
                            commentary = "四十八,어느 사회에서나 상류층 사람들은 자신만을 위해 제멋대로 행동하고 있지만, 장래에 닥쳐 올 일을 모르는 것이다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 차츰차츰 많이 모아 둔 이 나무들\n 용재가 될 것은 없는 거야 3-49\n",
                            japanese = "だん／＼とをふくよせたるこのたちき\nよふほくになるものハないぞや",
                            english = "dandan to oku yosetaru kono tachiki\n" +
                                    "yoboku ni naru mono wa nai zo ya",
                            commentary = "四十九,용재(用材)란 어버이신님의 일에 종사하는 사람을 뜻한다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 어떤 나무도 많이 모으기는 했으나\n 비뚤어지고 구부저려 이것 마땅찮다 3-50\n",
                            japanese = "いかなきもをふくよせてハあるけれど\nいがみかゞみハこれわかなハん",
                            english = "ikana ki mo oku yosete wa aru keredo\n" +
                                    "igami kagami wa kore wa kanawan",
                            commentary = "五十,여러 종류의 나무를 많이 모기는 했지만, 비뚤어지고 구부러진 나무는 아무래도 쓰기에 마땅치 않다\n 비뚤어지고 구부러져란 나무에 비유하여 마음이 비뚤어져 순직하지 못한 사람을 가리킨 말이다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 세상 사람들의 가슴속에 중심의 기둥\n 신은 서두르고 있다 빨리 보이고 싶어서 3-51\n",
                            japanese = "せかいぢうむねのうちよりしんばしら\n神のせきこみはやくみせたい",
                            english = "sekaiju mune no uchi yori Shinbashira\n" +
                                    "Kami no sekikomi hayaku misetai",
                            commentary = "五十一,온 세상 사람들이 마음을 바꾸어 모두가 맑고 깨끗한 마음이 된다면, 이 세상은 어버이신이 바라는 즐거운 삶의 세계가 되고, 터전에는 감로대가 서게 된다. 어버이신은 일각이라도 빨리 사람들에게 이것을 보이려고 서두르고 있다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 세상 사람들의 가슴속을 맑히는 이 청소\n 신이 빗자루야 단단히 두고 보라 3-52\n",
                            japanese = "せかいぢうむねのうちよりこのそふぢ\n神がほふけやしかとみでいよ",
                            english = "sekaiju mune no uchi yori kono soji\n" +
                                    "Kami ga hoke ya shikato mite iyo",
                            commentary = "五十二,그러기 위해서는 세상 사람들의 마음을 청소하지 않으면 안된다. 그래서 어버이신은 스스로 빗자루가 되어 청소하ㅐ 테니, 나타나는 리를 단단히 주의해서 보도록 하라."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이제부터는 신이 이 세상에 나타나서\n 산에까지 청소할 거야 3-53\n",
                            japanese = "これからハ神がをもていあらわれて\n山いかゝりてそふちするぞや",
                            english = "korekara wa Kami ga omote i arawarete\n" +
                                    "yama i kakarite soji suru zo ya",
                            commentary = "五十三,앞으로는 어버이신이 이 세상에 나타나서 상류층 사람들의 마음도 청소를 한다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 모든 사람들을 신이 청소하게 되면\n 마음 용솟음쳐 즐거움이 넘칠 거야 3-54\n",
                            japanese = "いちれつに神がそうちをするならば\n心いさんてよふきつくめや",
                            english = "ichiretsu ni Kami ga soji o suru naraba\n" +
                                    "kokoro isande yokizukume ya",
                            commentary = "五十四,온 세상 사람들의 마음을 어버이신이 청소하여 깨끗이 맑힌다면 이 세상은 평화롭고 즐거운 세계가 되고, 사람들의 마음도 용솟음치며 즐겁게 살게 된다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 무엇이든 신이 맡았으니까\n 무슨 일이든 자유자재로 3-55\n",
                            japanese = "なにもかも神がひきうけするからハ\nどんな事でもぢうよぢさを",
                            english = "nanimo kamo Kami ga hikiuke suru kara wa\n" +
                                    "donna koto demo juyojizai o",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이번에는 안을 다스릴 중심의 기둥\n 빨리 세우고 싶다 물을 맑혀서 3-56\n",
                            japanese = "このたびハうちをふさめるしんばしら\nはやくいれたい水をすまして",
                            english = "konotabi wa uchi o osameru Shinbashira\n" +
                                    "hayaku iretai mizu o sumashite",
                            commentary = "五十六,이번에는 사람들의 마음을 맑혀서 나까야마가의 후계자이며 이 길의 중심인 진주가 될 신노스께를 빨리 맞아들이고 싶다.\n 안을 다스릴 중심의 기둥 이 노래를 집필한 1874년에 신노스께님은 9세였는데, 출생 전부터 진주가 될 신노스께라 불리어 장래 나까아먀가의 후계자로, 또 이 길의 중심이 될 진주로 정해졌 있었다. 그래서 신노스께님은 일찍부터 집터에 와 있었는데, 어버이신님은 하루라도 빨리 집터에 정주시키려고 서두르고 계셨다. 본교에서는 이 길의 중심이 되는 사람을 진주님이라고 한다.(제3호 8의 주석 참조)"
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 높은산의 중심의 기둥은 모르는자야\n 이것이 첫째가는 신의 노여움 3-57\n",
                            japanese = "高山のしんのはしらハとふじんや\nこれが大一神のりいふく",
                            english = "takayama no shin no hashira wa tojin ya\n" +
                                    "kore ga daiichi Kami no rippuku",
                            commentary = "五十七,지도층에 있는 사람들은 아직 어버이신의 뜻을 깨닫지 못하고 인간생각에만 흐르고 있는데, 이것이 무엇보다 어버이신을 노엽게 하고 있다.\n 이것은 당시의 관헌이 종교를 이해하려고 하지 않는 태도에 대해서 하신 말씀이다. 높은 산의 중심의 기둥이란 상류층의 중심되는 인물을 말한다. 모르는자란 아직 어버이신님의 뜻을 모르는 사람(제3호 8, 9수의 주석 참조)"
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 윗사람들은 점점 더 세상을 멋대로 한다\n 신의 섭섭함 어떻게 생각하는가 3-58\n",
                            japanese = "上たるハだん／＼せかいまゝにする\n神のざんねんなんとをもうぞ",
                            english = "kami taru wa dandan sekai mamani suru\n" +
                                    "Kami no zannen nanto omou zo",
                            commentary = "윗사람들 가운데는 세상 모든 일이 인간의 힘으로 좌우되는 줄로 생각하고 제멋대로 행동하는 자가 있는데, 이는 이 세상을 창조한 어버이신의 깊은 마음을 모르기 때문으로 심히 유감스러운 일이다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 지금까지는 무슨 말을 해도 나타나지 않았다\n 벌써 이제는 시기가 왔다 3-59\n",
                            japanese = "いまゝでハなにをゆうてもみへてない\nもふこのたびハせへつうがきた",
                            english = "imamade wa nani o yutemo miete nai\n" +
                                    "mo konotabi wa setsu ga kita",
                            commentary = "五十九,지금까지 여러 가지로 일러주었지만 그것이 아직은 실제로 나타나지 않았다. 그러나 이제는 누구나 알 수 있는 시기가 다가왔다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이제부터는 즐거운 근행을 또 시작한다\n 무슨 일인지 좀처럼 모르겠지 3-60\n",
                            japanese = "これからハよふきづとめにまたかゝる\nなんの事やら一寸にしれまい",
                            english = "korekara wa Yoki-zutome ni mata kakaru\n" +
                                    "nanno koto yara choto ni shiremai",
                            commentary = "六十,이제부터는 즐거운근행을 다시 서두를 것이데, 그것은 무슨 까닭인지 좀처럼 모를 것이다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 지금까지도 신의 뜻을 되풀이 이야기하고\n 일러주어도 무슨 말일지 3-61\n",
                            japanese = "今までもしりてはなしてはなしとも\nといてあれどもなんの事やら",
                            english = "imamade mo shirite hanashite hanashi tomo\n" +
                                    "toite aredomo nanno koto yara",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이제까지는 어떤 말을 일러주어도\n 날이 오지 않아서 나타나지 않은 거야 3-62\n",
                            japanese = "これまでハいかなはなしをといたとて\nひがきたらんでみへてないぞや",
                            english = "koremade wa ikana hanashi o toita tote\n" +
                                    "hi ga kitarande miete nai zo ya",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이제부터는 이미 시기가 왔으므로\n 말하면 그대로 나타나는 거야 3-63\n",
                            japanese = "これからわもふせへつうがきたるから\nゆへばそのまゝみへてくるぞや",
                            english = "korekara wa mo setsu ga kitaru kara\n" +
                                    "yueba sono mama miete kuru zo ya",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 단단히 들어라 36 25의 저물 무렵에\n 가슴속의 청소를 신이 하는 거야 3-64\n",
                            japanese = "しかときけ三六二五のくれやいに\nむねのそふぢを神がするぞや",
                            english = "shikato kike san roku ni go no kureyai ni\n" +
                                    "mune no soji o Kami ga suru zo ya",
                            commentary = "六十四,이것은 입교 후 36년이 되는 어느 달 25일의 저녁나절에 집터 청소를 하러 올 사람이 있음을 말씀하신 것이다. 그 무렵에는 밖에서 집터 총소를 하러 올 사람이 별로 없었는데, 이날 저녁나절에 닷다에 사는 요스께(與助)라는 사람의 부인 또요와 감베에라는 사람의 모친후사가 참배하러 와서, 집터 안이 지저분한 것을 보고 내일이 26일 제일인데 이렇게 지저분해서야 신님께 죄송한 일이라며 깨끗이 청소를 하고 돌아갔다. 그 전날 밤, 또요는 가슴이 아파 괴로워하던 중, 전에도 한번 한 적이 있는 터전의 신님께 참배를 하여 도움을 받아야겠다고 결심을 하자, 가슴의 통증이 씼은 듯 나았다. 그래서 이날 사례참배를 하러 왔던 것이다. 이것은 일례를 들어 말씀하신 것인데, 앞으로는 이처럼 사람들이 마음을 청소하도록 어버이신님이 손질을 하신다는 뜻이다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 생각하라 아무리 맑은 물이라 해도\n 진흙을 넣으면 탁해지느니라 3-65\n",
                            japanese = "しやんせよなんぼすんだる水やとて　とろをいれたらにごる事なり",
                            english = "shiyan seyo nanbo sundaru mizu ya tote\n" +
                                    "doro o iretara nigoru koto nari",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 탁한 물을 빨리 맑히지 않으면\n 중심의 기둥을 세울 수 없다 3-66\n",
                            japanese = "にごり水はやくすまさん事にてわ　しんのはしらのいれよふがない",
                            english = "nigori mizu hayaku sumasan koto nitewa\n" +
                                    "shin no hashira no ireyo ga nai",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 기둥만 빨리 세우게 되면\n 영원히 확실하게 안정이 된다 3-67\n",
                            japanese = "はしらさいはやくいれたる事ならば\nまつたいしかとをさまりがつく",
                            english = "hashira sai hayaku iretaru koto naraba\n" +
                                    "matsudai shikato osamari ga tsuku",
                            commentary = "六十六, 六十七,사람들이 마음을 빨리 맑히지 않으면 싱앙의 중심인 감래대를 세울 소도 억고, 또 이 길의 중심이 되는 자를 집터에 정주시킬 수도 없다. 이것만 빨리 된다면 이 길의 기초는 확립되는 것이다.\n 이것은 감로대의 터전 결정과 이 길의 중심이 될 초대 진주님을 집터에 정주시키고자 서두르신 것이다.(제3호 8, 9의 주석 참조)"
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이 세상을 창조한 신의 진실을\n 일러줄 테니 거짓이라 생각 말라 3-68\n",
                            japanese = "このよふをはじめた神のしんぢつを\nといてきかするうそとをもうな",
                            english = "kono yo o hajimeta Kami no shinjitsu o\n" +
                                    "toite kikasuru uso to omouna",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 지금까지도 심학 고기 있지만\n 근본을 아는 자는 없는 거야 3-69\n",
                            japanese = "いまゝでもしんがくこふきあるけれど\nもとをしりたるものハないぞや",
                            english = "imamade mo shingaku koki aru keredo\n" +
                                    "moto o shiritaru mono wa nai zo ya",
                            commentary = "六十九,종래에도 사람들은 심학(心學)이니 고기(古記)니 말들을 하고 있지만, 이 세상이 만들어진 과정을 참으로 아는 자는 아무도 없다.\n심학(心學)이란 당시 사회에 널리 퍼져 있던 윤리사상으로, 심학도화(心學道話)를 말한다.\n고기(古記)란 여기서는 예부터 전해져 오는 건국설화를 말한다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 그러리라 진흙바다 속의 과정을\n 아는 자는 없을 테니까 3-70\n",
                            japanese = "そのはづやどろうみなかのみちすがら\nしりたるものハないはづの事",
                            english = "sono hazu ya doroumi naka no michisugara\n" +
                                    "shiritaru mono wa nai hazu no koto",
                            commentary = "七十,그도 그럴 것이 태초에 진흙바다 가운데서 인간을 창조한 과정을 알고 있는 사람은 아무도 없을 것이기 때문이다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이제부터는 이 세상 창조 이래 없던 일을\n 차츰차츰 일러줄 것이니 3-71\n",
                            japanese = "これまでハこのよはじめてない事を\nたん／＼といてきかす事なり",
                            english = "koremade wa kono yo hajimete nai koto o\n" +
                                    "dandan toite kikasu koto nari",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 무엇이든 없던 일만을 일러주지만\n 여기에 틀림은 없는 거야 3-72\n",
                            japanese = "なにもかもない事はかりとくけれど\nこれにまちごた事ハないぞや",
                            english = "nanimo kamo nai koto bakari toku keredo\n" +
                                    "kore ni machigota koto wa nai zo ya",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 11에 9가 없어지고 괴로움이 없는\n 정월 26일을 기다린다 3-73\n",
                            japanese = "十一に九がなくなりてしんわすれ\n正月廿六日をまつ",
                            english = "u ichi ni ku ga nakunarite shin wasure\n" +
                                    "shogatsu niju roku nichi o matsu",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 그 동안에 중심도 이루어 욕심 버리고\n 인원을 갖추어서 근행할 준비하라 3-74\n",
                            japanese = "このあいだしんもつきくるよくハすれ\nにんぢうそろふてつとめこしらゑ",
                            english = "kono aida shin mo tsukikuru yoku wasure\n" +
                                    "ninju sorote Tsutome koshirae",
                            commentary = "七十三, 七十四,이 노래는 교조님이 현신을 감추실 것을 예고하신 것이다. 즉, 세상에서는 교조님을 묙표로 하여 점점 심하게 박해를 가해 오고, 그로 인해 차츰 이 길이 늦어지자 교조님은 자신의 수명을 25년 줄여 몸을 감춤으로써 세상의 압박을 작게 하여 이 길을 넗히려 한다. 그리고 그때까지는 진주도 정해지고 감로대도 세워질 것이므로, 모두들은 마음을 맑히고 빨리 인원을 갖추어서 근행을 할 준비를 하라고 깨우치신 것이다. 그러나 당시 사람들은 이것을 몰랐으며, 후일 지도말씀에 의해서 비로서 어버이신님의 깊은 뜻을 알게 되었다. '자아 자아, 26일이라 붓으로 적어 놓고 시작한 리를 보라. 자아 자아, 또 정월 26일부터 현신의 문을 열고 세계를 평탄한 땅으로 밞아 고르러 나가서 시작한 리와, 자아 자와, 없애버리겠다는 말을 듣고 한 리와, 두 가지를 비교해서 리를 분간하면, 자아 자아, 라는 선명하게 깨달아질 것이다.'\t (1889. 3. 10)"
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 나닐이 신이 마음 서두르는 것은\n 자유자재를 빨리 보이고 싶어서 3-75\n",
                            japanese = "にち／＼に神の心のせきこみハ\nぢうよじざいをはやくみせたい",
                            english = "nichinichi ni Kami no kokoro no sekikomi wa\n" +
                                    "juyojizai o hayaku misetai",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이제부터는 인원 갖추어 근행한다\n 이것으로 확실히 온 세상 안정이 된다 3-76\n",
                            japanese = "これからハにんぢうそろをてつとめする　これでたしかににほんをさまる",
                            english = "korekara wa ninju sorote Tsutome suru\n" +
                                    "kore de tashikani nihon osamaru",
                            commentary = "七十六,이제부터 인원을 갖추어 신악근행을 하면 사람들의 마음도 맑아지고 어버이신의 마음도 용솟음치기 때문에, 이것으로 온 세상은 학실히 안정이 된다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 진실로 구제한줄기이기 때문에\n 전혀 두려움 억는 거야 3-77\n",
                            japanese = "しんぢつにたすけ一ぢよてあるからに\nなにもこわみハさらにないぞや",
                            english = "shinjitsu ni tasuke ichijo de aru karani\n" +
                                    "nanimo kowami wa sarani nai zo ya",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 무엇이든 구제한줄기 멈추게 하면\n 신의 섭섭함이 몸의 장애로 나타난다 3-78\n",
                            japanese = "なにもかもたすけ一ぢよとめるなら\n神のさんねんみにさハりつく",
                            english = "nanimo kamo tasuke ichijo tomeru nara\n" +
                                    "Kami no zannen mi ni sawari tsuku",
                            commentary = "七十七, 七十八,이 길을 진실한 구제한줄기의 길인 만큼 이것을 빨리 넒히도록 노력하라고 어버이신님은 서두르고 있지만, 결의 사람들은 세상이 겁나고 관헌이 두려운 나머지 곧잘 움츠러들기가 일쑤였다. 그러나 이래서는 이 길이 늦어지므로 앞으로 누구든지 이 구제한줄기의 활동을 저지한다면, 어버이신님을 섭섭케하여 그 결과 반드시 몸에 장애를 방게 될 것이니 잘 생각하라고 곁의 사람들에게 깨우치신 것이다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 생각하라 만가지구제의 이 준비\n 인간의 재주라고 전혀 생각 말라 3-79\n",
                            japanese = "しやんせよ万たすけのこのもよふ\nにんけんハざとさらにをもうな",
                            english = "shiyan seyo yorozu tasuke no kono moyo\n" +
                                    "ningen waza to sarani omouna",
                            commentary = "七十九,잘 생각해 보라, 만가지구제를 위하여 어버이신이 마음을 다하고있는 이 준비가 과연 인간의 기교로써 되겠는가, 그런데도 지금까지 어버이신이 하는 일은 아무것도 모른 채 이것을 인간마음으로 의심하고 있는 것이다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 지금까지는 만가지를 전혀 모르는 채\n 모두 인간마음뿐이야 3-80\n",
                            japanese = "いまゝでハなにかよろづがハからいで\nみなにんけんの心ばかりで",
                            english = "imamade wa nanika yorozu ga wakaraide\n" +
                                    "mina ningen no kokoro bakari de",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이제부터는 신의 마음과 욋사람의\n 마음과 마음을 비교한다 3-81\n",
                            japanese = "これからハ神の心と上たるの\n心と心のひきやハせする",
                            english = "korekara wa Kami no kokoro to kami taru no\n" +
                                    "kokoro to kokoro no hikiyawase suru",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이 이야기를 예삿일로 생각 말라\n 신이 차마 볼 수 없기 때문이니 3-82\n",
                            japanese = "このはなし一寸の事やとをもうなよ\n神がしんぢつみかねたるゆへ",
                            english = "kono hanashi chotto no koto ya to omouna yo\n" +
                                    "Kami ga shinjitsu mikanetaru yue",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이제부터는 신의 힘과 윗사람의\n 힘을 서로 겨룬다고 생각하라 3-83\n",
                            japanese = "これからハ神のちからと上たるの\nちからくらべをするとをもへよ",
                            english = "korekara wa Kami no chikara to kami taru no\n" +
                                    "chikara kurabe o suru to omoe yo",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 아무리 힘센 자도 있으면 나와 보라\n 신의 쪽에서는 갑절의 힘을 3-84\n",
                            japanese = "いかほどのごふてきあらばだしてみよ\n神のほふにもばいのちからを",
                            english = "ika hodono goteki araba dashite miyo\n" +
                                    "Kami no ho nimo bai no chikara o",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 진실한 신이 이 세상에 나왔으니\n 어떤 준비도 한다고 생각하라 3-85\n",
                            japanese = "しんぢつの神がをもていでるからハ\nいかなもよふもするとをもゑよ",
                            english = "shinjitsu no Kami ga omote i deru kara wa\n" +
                                    "ikana moyo mo suru to omoe yo",
                            commentary = "八十一～八十五,이 구제한줄기의 길을 어서 넒히려고 어버이신님은 서두르고 있지만, 지방 관헌이 이 길에 대한 몰이해로 여러 가지 압박을 가하고 있고, 또 곁의 사람들은 이것을 두려워한 나머지 망설이고만 있으니 어버ㅇ신님은 차마 볼 수가 없다. 윗사람들의 몰이해는 진실로 이 길의 진수를 모르는 탓이므로, 앞으로는 윗사람들에게도 어버이신님의 뜻을 깨닫게 할 준비를 한다. 그리고 신직과 승려들이 인간의 지혜나 힘으로 아무리 강력하게 비난하고 반대하더라도, 어버이신님은 그것을 녹이고도 남을 따뜻한 어버이마음이 있고, 또 이러한 어버이신님이 세상에 나타나 있는 이상 어떠한 수호도 할 것이니, 안심하고 구제한줄기의 길로 나가라고 격려하고 계신다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 지금까지는 미칠곳이 미친곳을 멋대로 했다\n 신의 섭섭함을 어찌해야 할지 3-86\n",
                            japanese = "いまゝでわからがにほんをまゝにした\n神のざんねんなんとしよやら",
                            english = "imamade wa kara ga nihon o mamani shita\n" +
                                    "Kami no zannen nanto shiyo yara",
                            commentary = "八十六,지금까지는 인간생각에 사로잡혀 어버이신의 가르침을 멋대로 방해해 왔는데, 어버이신은 이것을 참으로 안타깝게 여기고 있다.\n 미친곳, 미칠곳은 제2호 34수의 주석 참조."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 앞으로는 미친곳이 미칠곳을 생각대로 한다\n 모든 사람들은 명심해 두라 3-87\n",
                            japanese = "このさきハにほんがからをまゝにする\nみな一れつハしよちしていよ",
                            english = "konosaki wa nihon ga kara o mamani suru\n" +
                                    "mina ichiretsu wa shochi shite iyo",
                            commentary = "八十七,앞으로는 어버이신의 뜻을 일러주어 지금까지 전혀 모르던 곳에도 구제한줄기의 어버이마음이 고루 미치도록 자유자재한 섭리를 나타낼 터이니, 모두들은 이 점을 잘 명심해 두도록 하라."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 같은 나무의 뿌리와 가지라면\n가지는 꺾여도 뿌리는 번성해진다 3-88\n",
                            japanese = "をなじきのねへとゑだとの事ならバ\nゑたハをれくるねハさかいでる",
                            english = "onaji ki no ne to eda to no koto naraba\n" +
                                    "eda wa orekuru ne wa sakai deru",
                            commentary = "八十八,한 나무의 뿌리와 가지라면, 설사 가지는 꺽일지라도 뿌리는 그대로 남아 번성해진다.\n 교조님이 단식을 하실 때, 그렇게 단식을 하셔도 괜찮습니까 하고 걱정하는 사람에게\n'마음의 뿌리야, 마음만 어버이신님께 통하고 있으면 결코 시들지 않는 거야.'라는 의미의 말씀을 들려주신 적이 있다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 지금까지는 미칠곳을 위대하다 했지만\n 이제부터 앞으로는 꺾일 뿐이야 3-89\n",
                            japanese = "いまゝでわからハゑらいとゆうたれど\nこれからさきハをれるはかりや",
                            english = "imamade wa kara wa erai to yu taredo\n" +
                                    "korekara saki wa oreru bakari ya",
                            commentary = "八十九,지금까지는 어버이신의 가르침을 듣지 않아도 단지 지혜나 기술 등만 뛰어나면 훌륭한 사람이라고 해 왔지만, 앞으로는 그런 사람도 인간생각을 버리고 어버이신의 뜻을 깨달아 따라오게 된다.\n 꺾인다는 것은 인간생각을 꺾고 어버이신님의 가르침을 그리며 찾아온다는 뜻이다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 미친곳을 보라 약한 듯 생각했지만\n 뿌리가 나타나면 굴복하게 될 것이다 3-90\n",
                            japanese = "にほんみよちいさいよふにをもたれど\nねがあらハればをそれいるぞや",
                            english = "nihon miyo chiisai yoni omotaredo\n" +
                                    "ne ga arawareba osore iru zo ya",
                            commentary = "九十,지금까지는 어버이신의 가르침이 전해진 곳을 가볍게 보고 업신여겨 왔으나, 어버이신의 뜻이 세상에 나타나 구제한줄기에 의해 사람들의 마음이 맑아지면 어떤 사람도 모두 그곳을 그리워하고 흠모하게 된다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이 힘 인간의 재주라 생각말라\n 신의 힘이라 이에는 당할 수 없다 3-91\n",
                            japanese = "このちからにんけんハさとをもハれん\n神のちからやこれハかなわん",
                            english = "kono chikara ningen waza to omowaren\n" +
                                    "Kami no chikara ya kore wa kanawan",
                            commentary = "九十一,이 절대한 섭리는 인간의 힘이라고는 생각할 수 없다. 모두가 어버이신의 힘이 나타난 것이므로 누구든 마음으로 감동하지 않을 수 없게 된다.\n八十六～九十一,위의 노래들은 어느 것이나 집필 당시인 1874년경의 사회상을 우려한 것으로, 인간창조시 어버이신님을 진실한 어비이로 하여 태어난 형제자매들이 서로 돕고 서로 위하는 마음이 없이, 저마다 이기주의에 빠져 서로 싸우고 서로 해치며 살아가고 있는데 대해 강력하게 반성을 촉구하시면서, 모두가 구제한줄기의 길로 나아가 서로 화목한 가운데 즐거운 삶을 누리도록 하라고 사람들의 신앙심을 고무하신 내용이다.(제2호 40수의 주석 참조)"
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이 세상은 번창하게 살고 있지만\n 근본을 아는 자는 없으므로 3-92\n",
                            japanese = "このよふハにぎハしくらしいるけれど\nもとをしりたるものハないので",
                            english = "kono yo wa nigiwashi kurashi iru keredo\n" +
                                    "moto o shiritaru mono wa nai node",
                            commentary = "九十二,세상 사람들은 아무 생각 없이 흥청거리며 살고 있지만, 태초의 어버이의 수호에 대해서 알고 있는 사람은 아무도 없다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이 근본을 자세히 알게 되면\n 질병이 생길 리는 없을 텐데 3-93\n",
                            japanese = "このもとをくハしくしりた事ならバ\nやまいのをこる事わないのに",
                            english = "kono moto o kuwashiku shirita koto naraba\n" +
                                    "yamai no okoru koto wa nai no ni",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 아무것도 모르고 지내는 이 자녀들\n 신의 눈에는 애처로운 일 3-94\n",
                            japanese = "なにもかもしらずにくらすこの子共\n神のめへにハいぢらき事",
                            english = "nanimo kamo shirazu ni kurasu kono kodomo\n" +
                                    "Kami no me niwa ijirashiki koto",
                            commentary = "九十三, 九十四, 어버이신이 인간을 창조한 으뜸인 리를 잘 알고 있지만, 나쁜 마음도 생기지 않고 질병을 앓는 사람도 없을 터인데, 사람들은 아무것도 모른 채 제멋대로 마음을 쓰고 있다. 어버이신의 눈으로 볼 때 이것이 매우 가엾은 일이다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 어떤 것이든 질병이란 전혀 없다\n 마음 잘못 쓴 길이 있으므로 3-95\n",
                            japanese = "なにゝてもやまいとゆうてさらになし\n心ちがいのみちがあるから",
                            english = "nani nitemo yamai to yute sarani nashi\n" +
                                    "kokoro chigai no michi ga aru kara",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이 길은 인색 탐 편애\n 욕심과 교만이 이것이 티끌이야 3-96\n",
                            japanese = "このみちハをしいほしいとかハいと\nよくとこふまんこれがほこりや",
                            english = "kono michi wa oshii hoshii to kawaii to\n" +
                                    "yoku to koman kore ga hokori ya",
                            commentary = "九十六,인간이 쓰는 마음 가운데 인색, 탐, 편애, 욕심, 교만 등, 이것이 티끌이 되는 것이다.\n 본교에서는 인간의 괴로움은 마음속에 티끌이 쌓여 있기 때문에 생간다고 말한다. 이 티끌은 위의 노래에 나타난 것 이외에 미움, 원망, 분노 등, 세 가지가 더 있으며, 이것이 이른바 여덦가지 티끌이다. 대개 이것들이 인간의 나쁜 마음을 유도하는 근본이 된다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이 세상의 인간은 모두 신의 자녀야\n 신이 하는 말 단단히 분간하라 3-97\n",
                            japanese = "このよふのにんけんハみな神のこや\n神のゆう事しかときゝわけ",
                            english = "kono yo no ningen wa mina Kami no ko ya\n" +
                                    "Kami no yu koto shikato kikiwake",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 티끌만 깨끗하게 털어 버리면\n 다음에는 진기한 구제할 거야 3-98\n",
                            japanese = "ほこりさいすきやかはろた事ならば\nあとハめづらしたすけするぞや",
                            english = "hokori sai sukiyaka harota koto naraba\n" +
                                    "ato wa mezurashi tasuke suru zo ya",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 진실한 마음에 따른 이 구제\n 앎지 않고 죽지 않고 쇠하지 않도록 3-99\n",
                            japanese = "しんぢつの心しだいのこのたすけ\nやますしなずによハりなきよふ",
                            english = "shinjitsu no kokoro shidai no kono tasuke\n" +
                                    "yamazu shinazu ni yowari naki yo",
                            commentary = "九十九,어버이신이 구제한다고 해도 그것은 원하는 사람의 마음은 보고 하는 것으로, 진실한 마음이 어버이신의 뜻에 맞는다면 누구나 정명(定命)까지 앎지 않고 죽지 않고 쇠하지도 않고 살아갈 수가 있다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이 구제 115세 정명으로\n 하고 싶은 한결같은 신의 마음 3-100\n",
                            japanese = "このたすけ百十五才ぢよみよと\nさだめつけたい神の一ぢよ",
                            english = "kono tasuke hyaku ju go sai jomyo to\n" +
                                    "sadame tsuketai Kami no ichijo",
                            commentary = "百,어버이신으로서는 인간의 정명을 115세로 정하고 싶은 것이다.\n 감로대가 완성되어 하늘에서 내려 주는 감로를 받아 먹게 되면 누구나 정명을 누리게 되고, 또 마음에 따라서는 언제까지나 이 세상에서 살아갈 수가 있다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 나닐이 신이 마음 서두르는 것을\n 곁의 사람들은 어떻게 생각하는가 3-101\n",
                            japanese = "にち／＼に神の心のせきこみを\nそばなるものハなんとをもてる",
                            english = "nichinichi ni Kami no kokoro no sekikomi o\n" +
                                    "soba naru mono wa nanto omoteru",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 윗사람이 두려워 침울하고 있다\n 신의 서두름이라 두려울 것 없는 거야 3-102\n",
                            japanese = "上たるをこわいとをもていすみいる\n神のせきこみこわみないぞや",
                            english = "kami taru o kowai to omote izumi iru\n" +
                                    "Kami no sekikomi kowami nai zo ya",
                            commentary = "百二,당시 지방 관헌이나 세상의 오해를 두려워하고 있는 곁의 사람들에 대해서 신앙을 고무하신 노래이다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 가슴앎이를 질병이라 생각 말라\n 신의 서두름이 쌓여 있기 때문이니 3-103\n",
                            japanese = "むねあしくこれをやまいとをもうなよ\n神のせきこみつかゑたるゆへ",
                            english = "mune ashiku kore o yamai to omouna yo\n" +
                                    "Kami no sekikomi tsukaetaru yue",
                            commentary = "百三,가슴이 답답하여 기분이 나쁘더라도 이것을 졀병이라고 생각해서는 안된다. 그것은 어버이신의 서두름이 쌓여 있는 표시이다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 차츰차츰 신의 마음은\n 신기함을 나타내어 구제를 서두른다 3-104\n",
                            japanese = "たん／＼と神の心とゆうものわ\nふしぎあらハしたすけせきこむ",
                            english = "dandan to Kami no kokoro to yu mono wa\n" +
                                    "fushigi arawashi tasuke sekikomu",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이 신기함은 어떤 것이라 생각하는가\n 티끌을 털어내어 깨끗이 청소한다 3-105\n",
                            japanese = "このふしきなんの事やとをもている\nほこりはろふてそふぢしたてる",
                            english = "kono fushigi nanno koto ya to omote iru\n" +
                                    "hokori harote soji shitateru",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 다음에 빨리 기둥을 세우게 되면\n 이것으로 이 세상은 안정이 된다 3-106\n",
                            japanese = "あとなるにはやくはしらをいれたなら\nこれでこのよのさだめつくなり",
                            english = "ato naru ni hayaku hashira o ireta nara\n" +
                                    "kore de kono yo no sadame tsuku nari",
                            commentary = "百六,사람들이 마음을 청소하고 난 다음에는 신앙의 중심을 정하고 싶다. 이것만 이루어지면 이 세상은 안전이 된다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이 이야기가 빨리 나타나게 되면\n 어떤 자도 모두 납득하라 3-107\n",
                            japanese = "このはなしはやくみへたる事ならば\nいかなものでもみなとくしんせ",
                            english = "kono hanashi hayaku mietaru koto naraba\n" +
                                    "ikana mono demo mina tokushin se",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 지금까지는 증거 징험이라 하고 있지만\n 감로대란 무엇인지 3-108\n",
                            japanese = "いまゝでハしよこためしとゆへあれど\nかんろふだいもなんの事やら",
                            english = "imamade wa shoko tameshi to yue aredo\n" +
                                    "Kanrodai mo nanno koto yara",
                            commentary = "百八,지금까지 증거 징험이라 말하게 있지만 아직 아무것도 나타나지 않으므로, 모든 사람들은 감로대에 대한 어버이신의 뜻도 잘 이해하지 못할 것이다.\n증거 징험이란 태초에 인간을 창제한 증거로 터전에 감래대를 세우게 되면, 이 세상에는 참으로 평화로운 즐거운 삶의 세계가 실현된다는 실현된다는 신언을 실증하는 것을 말한다.(제3호 41, 제17호 9 참조) 감로대에 대해서는 제8호 78～86, 제9호 18～20, 44～64, 제10호 21, 22, 79, 제17호 2~5 참조)"
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이 사람을 4년 전에 데려가서\n 신이 안았다 이것이 증거야 3-109\n",
                            japanese = "このものを四ねんいせんにむかいとり\n神がだきしめこれがしよこや",
                            english = "kono mono o yo nen izen ni mukaitori\n" +
                                    "Kami ga dakishime kore ga shoko ya",
                            commentary = "百九,이 사람을 4년 전에 데려가서 그 혼을 다시 으뜸인 인연이 있는 터전에 태어나게 하려고 어버이신이 품에 안고 있는데, 이 것이 곧 어버이신의 자유자재한 섭리를 나타내는 증거이다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 진실로 빨리 돌려줄 준비\n 신의 서두름 이것이 첫째야 3-110\n",
                            japanese = "しんぢつにはやくかやするもよふたて\n神のせきこみこれがたい一",
                            english = "shinjitsu ni hayaku kayasuru moyodate\n" +
                                    "Kami no sekikomi kore ga daiichi",
                            commentary = "百十,이 혼이 빨리 으뜸인 터전에 다시 태어나도록 준비하고 있는데, 이것이 어버이신의 첫째가는 서두름이다.\n 이것은 4년 전인 1870년 음력 3월 15일에 출직한 슈지 선생의 서녀인 오슈우님을 말씀하신 것이다. 그는 이 길을 위해 없어서는 안될 중요한 혼의 인연을 지니고 있었기 때문에 어버이신님이 그 혼을 품에 안고 계시면서, 빨리 인연이 있는 으뜸인 터전에 다시 태어나게 하려고 서두려셨던 것이다. 여기 한가지 덧붙여 둘 것은, 1873년 양력 1월 1일은 1873년 음력 2월 13일에 해당한다.(제1호 60, 61의 주석 참조) "
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이제까지는 자유자재라고 자주 말했어도\n 아무것도 나타난 것은 없었지만 3-111\n",
                            japanese = "これまでハぢうよじざいとまゝとけど\nなにもみへたる事わなけれど",
                            english = "koremade wa juyojizai to mama tokedo\n" +
                                    "nanimo mietaru koto wa nakeredo",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이제부터는 어떤 이야기도 해 두고서\n 그것이 나타나면 자유자재야 3-112\n",
                            japanese = "これからハいかなはなしもときをいて\nそれみゑたならじうよぢざいや",
                            english = "korekara wa ikana hanashi mo tokioite\n" +
                                    "sore mieta nara juyojizai ya",
                            commentary = "百十一, 百十二,이제까지 어버이신의 섭리는 자유자재라고 자주 일러주었지만, 그 자유자재한 섭리가 아직  사람들의 눈앞에 나타난 적은 없었다. 앞으로는 어떤 이야기도 미리 해 두고서 그 이야기대로 증거가 나타나면 그것으로 어버이신의 섭리는 자유자재임을 분명히 깨달아야 한다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 지금까지의 일은 아무 말도 하지 않도록\n 26일에 시작할 테야 3-113\n",
                            japanese = "いまゝでの事ハなんにもゆてくれな\n廿六日にはじめかけるで",
                            english = "imamade no koto wa nan nimo yute kurena\n" +
                                    "niju roku nichi ni hajime kakeru de",
                            commentary = "百十三,이듬해인 1885년 음력 5월 26일에 감로대의 터전이 결정되었다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이제부터는 세상 사람들의 마음 용솟음치게 해서\n 온 세상을 안정시킬 준비할 거야 3-114\n",
                            japanese = "これからハせかいの心いさめかけ\nにほんをさめるもよふするぞや",
                            english = "korekara wa sekai no kokoro isame kake\n" +
                                    "nihon osameru moyo suru zo ya",
                            commentary = "百十四,이제부터는 세상 사람들의 마음을 용솟음치게 해서 온 세상을 원만히 안정시킬 준비를 한다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 인간의 마음이란 어리석어서\n 나타난 것만 이야기한다 3-115\n",
                            japanese = "にんけんの心とゆうハあざのふて\nみへたる事をばかりゆうなり",
                            english = "ningen no kokoro to yu wa azanote\n" +
                                    "mietaru koto o bakari yu nari",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이제부터는 없던 일만 일러둔다\n 이제부터 앞날을 똑똑히 두고 보라 3-116\n",
                            japanese = "これからハない事ばかりといてをく\nこれからさきをたしかみていよ",
                            english = "korekara wa nai koto bakari toite oku\n" +
                                    "korekara saki o tashika mite iyo",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 무슨 일이든 차츰차츰 말하기 시작하리라\n 나타난 것은 새삼 말하지 않아 3-117\n",
                            japanese = "どのよふな事もたん／＼ゆいかける\nみへたる事ハさらにゆハんで",
                            english = "dono yona koto mo dandan yuikakeru\n" +
                                    "mietaru koto wa sarani yuwan de",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이 세상을 창조한 신의 중심의 기둥\n 빨리 세우고 싶다 한결같은 신의 마음 3-118\n",
                            japanese = "このよふをはじめた神のしんばしら\nはやくつけたい神の一ぢよ",
                            english = "kono yo o hajimeta Kami no Shinbashira\n" +
                                    "hayaku tsuketai Kami no ichijo",
                            commentary = "百十八,이 세상 인간을 창조한 리를 나타내는 감로대를 일각이라도 빨리 세우고자 하는 것이 어버이신의 한결같은 마음이다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 눈에 안 보이는 신이 하는 말 하는 일을\n 차츰차츰 듣고서 생각해 보라 3-119\n",
                            japanese = "めへにめん神のゆう事なす事を\nたん／＼きいてしやんしてみよ",
                            english = "me ni men Kami no yu koto nasu koto o\n" +
                                    "dandan kiite shiyan shite miyo",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 지금의 길 윗사람 멋대로라 생각하고 있다\n 틀린 생각이야 신의 마음대로야 3-120\n",
                            japanese = "いまのみち上のまゝやとをもている\n心ちがうで神のまゝなり",
                            english = "ima no michi kami no mama ya to omote iru\n" +
                                    "kokoro chigau de Kami no mama nari",
                            commentary = "百二十,윗사람들은 이 길을 멋대로 할 수 있다고 생각하고 억누르려 하지만, 이 길은 구제한줄기의 길이므로 인간생각대로 되는 것이 아니라 어버이신의 의도대로 되는 것이다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 윗사람은 온 세상을 멋대로 한다\n 신의 섭섭함 이것을 모르는가 3-121\n",
                            japanese = "上たるハせかいぢううをまゝにする\n神のざんねんこれをしらんか",
                            english = "kami taru wa sekaiju o mamani suru\n" +
                                    "Kami no zannen kore o shiran ka",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이제까지는 온 세상을 윗사람 멋대로\n 이제 앎으로는 양상이 바뀔 거야 3-122\n",
                            japanese = "これまでハよろづせかいハ上のまゝ\nもふこれからハもんくかハるぞ",
                            english = "koremade wa yorozu sekai wa kami no mama\n" +
                                    "mo korekara wa monku kawaru zo",
                            commentary = "百二一, 百二二,온갖 세상 일은 모두가 인간의 힘으로 되는 줄로 생각하고 윗사람 가운데는 메사를 제멋대로 하는 자가 있으나, 그것은 이 세상을 창조한 어버이신의 뜻을 모르기 때문이며, 따라서 본래 없던 세계 억던 인간을 만든 어버이신의 눈으로 볼 때, 참으로 유감스런 일이 아닐 수 없다. 그러나 으뜸인 어버어신이 이 세상에 나타난 이상, 앞으로는 어버이신의 뜻을 깨닫게 하여 인간들이 제멋대로 행동하지 못하도록 한다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이 세상을 창조한 이래 아무것도\n 일러준 일이 없으므로 3-123\n",
                            japanese = "このよふをはじめてからハなにもかも\nといてきかした事ハないので",
                            english = "kono yo o hajimete kara wa nanimo kamo\n" +
                                    "toite kikashita koto wa nai node",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 윗사람은 온 세상을 제멋대로\n 생각하고 있는 것은 틀리는 거야 3-124\n",
                            japanese = "上たるハせかいぢううをハがまゝに\nをもているのハ心ちかうで",
                            english = "kami taru wa sekaiju o waga mamani\n" +
                                    "omote iru no wa kokoro chigau de",
                            commentary = "百二四,윗사람들 가운데는 세상 모든 일이 자기 생각대로 돈다고 여기고 있는 자가 있으나, 이것은 틀린 생각이다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 높은산에서 자라난 나무나 골짜기에서\n 자라난 나무나 모두 다 같은 것 3-125\n",
                            japanese = "高山にそだつる木もたにそこに\nそたつる木もみなをなじ事",
                            english = "takayama ni sodatsuru kii mo tanisoko ni\n" +
                                    "sodatsuru kii mo mina onaji koto",
                            commentary = "百二五,상류사회 사람들이나 하류사회 사람들이나 생활 수준은 다르지만 모두가 어버이신의 자녀라는 점에서는 하등 차별이 없다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 인간은 모두가 신의 대물이야\n 신의 자유자재 이것을 모르는가 3-126\n",
                            japanese = "にんけんハみな／＼神のかしものや\n神のぢうよふこれをしらんか",
                            english = "ningen wa minamina Kami no kashimono ya\n" +
                                    "Kami no juyo kore o shiran ka",
                            commentary = "百二六,인간의 육체는 인간 자신이 만든 것이 아니고, 이 세상을 창조한 어버이신이 만들어서 인간에게 빌려 주고 있는 것이다. 인간이 살아가고 있는 것도 모두 어버이신의 자유자재한 수호가 있기 때문인데 이것을 모르는가."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 사람들은 모두 제 몸 조심하라\n 신이 언제 어디로 나갈는지 3-127\n",
                            japanese = "いちれつハみな／＼わがみきをつけよ\n神がなんどきとこへいくやら",
                            english = "ichiretsu wa minamina waga mi ki o tsuke yo\n" +
                                    "Kami ga nandoki doko e iku yara",
                            commentary = "百二七,사람들은 누구나 자신의 몸을 돌이켜보고 그것이 어버이신의 차물(借物)임을 잘 깨달아 부디 조심하도록 하라. 마음에 그릇됨이 있으면 어버이신의 수호는 언제 중지될지도 모른다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 잠깐 한마디 신이 마음 서두르는 것은\n 용재 모을 준비만을 3-128\n",
                            japanese = "一寸はなし神の心のせきこみハ\nよふぼくよせるもよふばかりを",
                            english = "choto hanashi Kami no kokoro no sekikomi wa\n" +
                                    "yoboku yoseru moyo bakari o",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 차츰차츰 많은 나무들이 있지만\n 어느 것이 용재가 될지 모르겠지 3-129\n",
                            japanese = "たん／＼とをふくたちきもあるけれど\nどれがよふほくなるしれまい",
                            english = "dandan to oku tachiki mo aru keredo\n" +
                                    "dore ga yoboku naru ya shiremai",
                            commentary = "百二九,여러 가지 나무들이 많이 있으나 그 가운데서 어느 것이 용재가 될지 모를 것이다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 용재도 적은 수가 아닌 만큼\n 많은 용재가 필요하므로 3-130\n",
                            japanese = "よふぼくも一寸の事でハないほどに\nをふくよふきがほしい事から",
                            english = "yoboku mo chotto no koto dewa nai hodoni\n" +
                                    "oku yoki ga hoshii koto kara",
                            commentary = "百三十,용재도 적은 수로서는 안되므로 많은 수를 필요로 한다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 나날이 용재에게는 손질을 한다\n 어디가 나쁘다고 전혀 생각 말라 3-131\n",
                            japanese = "にち／＼によふほくにてわていりする\nどこがあしきとさらにおもうな",
                            english = "nichinichi ni yoboku nitewa teiri suru\n" +
                                    "doko ga ashiki to sarani omouna",
                            commentary = "百三一,어버이신이 용재로 쓰려고 생각하는 사람에게는 항상 손질을 하므로, 몸에 어딘가 좋지 않은 데가 있더라도 질병으로 여기지 말고 깊이 생각도록 하라."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 같은 나무도 차츰차츰 손질하는 것도 있고\n 그대로 넘어뜨리는 나무도 있다 3-132\n",
                            japanese = "をなじきもたん／＼ていりするもあり\nそのまゝこかすきいもあるなり",
                            english = "onaji ki mo dandan teiri suru mo ari\n" +
                                    "sono mama kokasu kii mo aru nari",
                            commentary = "百三二,같은 인간이라도 차츰 손질을 해서 유용하게 쓰는 자가 있는가 하면 그렇지 못한 자도 있는데, 그것은 각자의 마음쓰기 나름이다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 온갖 자유자재한 이 징험\n 다른 곳에서는 결코 안 하는 거야 3-133\n",
                            japanese = "いかなるのぢうよじざいのこのためし\nほかなるとこでさらにせんぞや",
                            english = "ika naru no juyojizai no kono tameshi\n" +
                                    "hoka naru toko de sarani sen zo ya",
                            commentary = "百三三,자유자재한 온갖 이 징험들은 터전이기 때문에 할 수 있는 것이지 다른 곳에서는 결코 못한다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 지금까지도 징험이라 말해 왔지만\n 이제 이번이 마지막 징험인 거야 3-134\n",
                            japanese = "いまゝでもためしとゆうてといたれど\nもふこのたびハためしをさめや",
                            english = "imamade mo tameshi to yute toitaredo\n" +
                                    "mo konotabi wa tameshi osame ya",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 차츰차츰 무엇이든 이 세상은\n 신의 몸이야 생각해보라 3-135\n",
                            japanese = "たん／＼となに事にてもこのよふわ\n神のからだやしやんしてみよ",
                            english = "dandan to nani goto nitemo kono yo wa\n" +
                                    "Kami no karada ya shiyan shite miyo",
                            commentary = "百三五, 제3호 40, 41, 참조."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이번에는 신이 이 세상에 나왔으니\n 만가지 일을 모두 가르칠 테다 3-136\n",
                            japanese = "このたびハ神がをもていでゝるから\nよろづの事をみなをしへるで",
                            english = "konotabi wa Kami ga omote i deteru kara\n" +
                                    "yorozu no koto o mina oshieru de",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 각자의 몸은 차물임을\n 모르고 있어서는 아무것도 모른다 3-137\n",
                            japanese = "めへ／＼のみのうちよりのかりものを\nしらずにいてハなにもわからん",
                            english = "meme no minouchi yori no karimono o\n" +
                                    "shirazu ni ite wa nanimo wakaran",
                            commentary = "百三七,사람들이 각자의 몸은 어버이신으로부터 빌린 것이라는 사실을 모르고서는 그 밖에 다른 것도 결코 알 리가 없다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 생각하라 질병이라 전혀 없다\n 신의 길잡이 훈계인 거예 3-138\n",
                            japanese = "しやんせよやまいとゆうてさらになし\n神のみちをせいけんなるぞや",
                            english = "shiyan seyo yamai to yute sarani nashi\n" +
                                    "Kami no michiose iken naru zo ya",
                            commentary = "百三八, 제2호 22, 23 참조."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 조그마한 눈병도 부스럼도\n 신경증도 아픔도 신의 인도야 3-139\n",
                            japanese = "一寸したるめへのあしくもできものや\nのぼせいたみハ神のてびきや",
                            english = "choto shitaru me no ashiku mo dekimono ya\n" +
                                    "nobose itami wa Kami no tebiki ya",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 지금까지는 높은산이라 할지라도\n 용재가 나온 일은 없었지만 3-140\n",
                            japanese = "いまゝでハ高い山やとゆうたとて\nよふほくみへた事ハなけれど",
                            english = "imamade wa takai yama ya to yuta tote\n" +
                                    "yoboku mieta koto wa nakeredo",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 앞으로는 높은산에서도 차츰차츰\n 용재 찾아낼 준비를 할 거야 3-141\n",
                            japanese = "このさきハ高山にてもたん／＼と\nよふぼくみだすもよふするぞや",
                            english = "konosaki wa takayama nitemo dandan to\n" +
                                    "yoboku midasu moyo suru zo ya",
                            commentary = "百四一,지금까지는 상류사회 사람으로서 이 길에 이바지한 사람은 거의 없었으나, 앞으로는 그 사회에서도 용재가 될 사람을 찾아낼 준비를 한다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 온 세상을 빨리 구제할 이 준비\n 위나 아래나 모두 용솟음치게 해서 3-142\n",
                            japanese = "いちれつにはやくたすけるこのもよふ\n上下ともに心いさめで",
                            english = "ichiretsu ni hayaku tasukeru kono moyo\n" +
                                    "kami shimo tomoni kokoro isamete",
                            commentary = "百四二,온 세상 사람들을 빨리 돕기 위한 준비로서 어버이신은 윗사람이나 아랫사람이나 모두 용솟음치게 한다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 나날이 세상 사람들의 마음 용솟음치면\n 농작물도 모두 용솟음친다 3-143\n",
                            japanese = "にち／＼にせかいの心いさむなら\nものゝりうけハみないさみでる",
                            english = "nichinichi ni sekai no kokoro isamu nara\n" +
                                    "mono no ryuke wa mina isami deru",
                            commentary = "百四三,세상 사람들이 즐겁게 용솟음치면 어버이신의 마음도 용솟음치게 되고, 그로 인해 농작물도 자연히 풍작이 된다. 제1호 12~14 참조"
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 무엇이든 구제한줄기이기 때문에\n 모반의 뿌리를 빨리 끊고 싶다 3-144\n",
                            japanese = "なにゝてもたすけ一ちよであるからに\nむほんねへをはやくきりたい",
                            english = "nani nitemo tasuke ichijo de aru karani\n" +
                                    "muhon no ne o hayaku kiritai",
                            commentary = "百四四,이 길은 오로지 구제한줄기의 가르침이기 때문에, 리에 항거하는 사람들이 생기지 않도록 나쁜 마음의 뿌리를 빨리 끊어주고 싶다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 지금의 길은 티끌투성이므로\n 빗자루로 청소 시작하라 3-145\n",
                            japanese = "いまのみちほこりだらけであるからに\nほふけをもちてそうぢふしたて",
                            english = "ima no michi hokori darake de aru karani\n" +
                                    "hoke o mochite soji o shitate",
                            commentary = "百四五,지금 나아가고 있는 길은 어버이신의 눈으로 보면 티끌투성이므로 빗자루를 가지고 청소를 하라."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 다음 길은 넓고 티끌이 없으니\n 몇 사람이라도 데리고 걸어라 3-146\n",
                            japanese = "あとなるハみちハひろくでごもくなし\nいくたりなりとつれてとふれよ",
                            english = "ato naru wa michi wa hirokute gomoku nashi\n" +
                                    "ikutari nari to tsurete tore yo",
                            commentary = "百四六,청소를 한 길은 넓고도 개끗해서 즐겁게 걸을 수 있으니 몇 사람이라도 데리고 가라."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 22의 2의 5에 이야기 시작하여\n 만가지 인연 모두 일러준다 3-147\n",
                            japanese = "二二の二の五つにはなしかけ\nよろついんねんみなときゝかす",
                            english = "nii nii no nii no itsutsu ni hanashi kake\n" +
                                    "yorozu innen mina tokikikasu",
                            commentary = "百四七,이것은 1874년 2월 22일 밤 5각(오후 8시경)에 집필된 노래이다. 당시 쯔지 추우사꾸는 낮에는 집안 일을 돌보고 있었는데, 이날은 이가 아파 괴로워하다가 터전에 참배를 해서 도움을 받을 생각으로 집을 나서자 금방 씻은 듯이 치통이 나았다. 그래서 추우사꾸는 고맙게 생각하며 서둘러 참배하고는 교조님께 그 경위를 아뢰자, 교조님은\n\"금방 이것을 썻어요. 잘 보아요.\"\n하고 그에게 이 노래를 보이시면서, 어버이신님의 말씀을 순순히 일러주셨다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 높은산의 설교를 듣고 진실한\n 신의 이야기를 듣고서 생각하라 3-148\n",
                            japanese = "高山のせきゝよきいてしんしつの\n神のはなしをきいてしやんせ",
                            english = "takayama no sekkyo kiite shinjitsu no\n" +
                                    "Kami no hanashi o kiite shiyan se",
                            commentary = "百四八,신직이나 승려들의 설교를 듣고, 또 이 길의 이야기를 들은 다음, 어느 것이 진실한 어버이신의 뜻을 전하고 있는가를 비교해서 생각해 봐야 한다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 나날이 신의 이야기를 차츰차츰\n 듣고 즐거워하라 고오끼인 거야 3-149\n",
                            japanese = "にち／＼に神のはなしをたん／＼と\nきいてたのしめこふきなるぞや",
                            english = "nichinichi ni Kami no hanashi o dandan to\n" +
                                    "kiite tanoshime Koki naru zo ya",
                            commentary = "百四九,나날이 진실한 어버이신의 이야기를 듣고 즐거워하라. 이 이야기야말로 언제까지나 변함없이 영원히 전해질 세계구제의 가르침인 것이다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 지금의 길은 어떤 길이라 생각하는가\n 무엇인지 모르는 길이지만 4-1\n",
                            japanese = "いまのみちなんのみちやとをもている\nなにかわからんみちであれども",
                            english = "ima no michi nanno michi ya to omote iru\n" +
                                    "nanika wakaran michi de aredomo",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 눈앞에 한길이 보인다\n 저기 있던 것이 벌써 여기 다가와 4-2\n",
                            japanese = "このさきハをふくハんみちがみへてある\nもふあこにあるこゝいきたなり",
                            english = "konosaki wa okwan michi ga miete aru\n" +
                                    "mo ako ni aru koko i kita nari",
                            commentary = "一, 二,지금 나아가고 있는 길은 좁은 길로서 사람들의 눈에는 믿음직하게 보이지 않겠지만, 그러나 벌써 눈앞에 한길이 보이고있으며, 더구나 저만치 있던 것이 금세 여기까지 다가와 있다."
                        )
                    )

                    allContent.add(
                        ContentItem(
                            korean = " 이 날짜 언제라고 생각하는가\n 5월 5일에 반드시 온다 4-3\n",
                            japanese = "このひがらいつの事やとをもている\n五月五日にたしかでゝくる",
                            english = "kono higara itsu no koto ya to omote iru\n" +
                                    "go gatsu itsuka ni tashika detekuru",
                            commentary = "三,교조님 친정에서 만들고 있던 신악탈이 완성되어, 교조님이 이것을 가지러 가신 1874년 6월 18일로서 음력 5월 5일에 해당한다. 한편, 같은 해 음력 4월경에는 야마자와 료오스께(山澤良助)와 야마나까 추우시찌(山中忠七) 등, 두 사람이 병으로 누워 있었다. 또 삼마이뎅 마을에 사는 마에가와 한자브로오(前川半三郞)의 부인 다끼는 당시 수족이 부자유하여 거의 앉으뱅이와 같은 상태에 놓여 있었다. 그런데 이들이 공교롭게도 5월 5일 교조님께 가르침을 받고 도움을 청하기 위해 터전에 참배를 하러 왔었는데, 어버이신님은 이 노래를 통해 미리 이것을 예언하신 것이다."
                        )
                    )

                    allContent.add(
                        ContentItem(
                            korean = " 그로부터 사례참배 시작이다 이것을 보라\n 밤낮을 가리지 않게 될 거야 4-4\n",
                            japanese = "それよりもをかけはぢまるこれをみよ\nよるひるしれんよふになるぞや",
                            english = "sore yori mo okage hajimaru kore o miyo\n" +
                                    "yoru hiru shiren yoni naru zo ya",
                            commentary = "四,앞으로는 어버이신의 진기한 구제를 받고 터전에 사례참배를 하러 오는 사람들이 밤낮없이 끊이지 않을 것이다."
                        )
                    )

                    allContent.add(
                        ContentItem(
                            korean = " 차츰차츰 6월이 되면\n 증거수호부를 줄 것이라 생각하라 4-5\n",
                            japanese = "たん／＼と六月になる事ならば\nしよこまむりをするとをもへよ",
                            english = "dandan to roku gatsu ni naru koto naraba\n" +
                                    "Shoko Mamori o suru to omoe yo",
                            commentary = "五,증거수호부(證據守護符)란 본고장인 터전에 돌아온 사람이 출원하면 귀참한 증거로서 내려 주는 신부(神符)이다. 이것은 1874년 6월부터 내려 주게 되었다."
                        )
                    )

                    allContent.add(
                        ContentItem(
                            korean = " 그로부터는 차츰차츰 역사 서둘러서\n 무엇인가 바쁘게 될 것이다 4-6\n",
                            japanese = "それからハたん／＼ふしんせきこんで\nなにかいそがし事になるなり",
                            english = "sorekara wa dandan fushin sekikonde\n" +
                                    "nanika isogashi koto ni naru nari",
                            commentary = "六,여기서 역사란 나까야마가의 대문과 이에 붙은 교조님의 거처방 및 창고 건축을 말한다. 어버이신님은 이 건축을 서두르시면서, 역사가 시작되면 사람들이 많이 모여 와서 매우 바쁘게 된다고 말씀하신 것이다.(제3호 1의 주석 참조)"
                        )
                    )

                    allContent.add(
                        ContentItem(
                            korean = " 이제부터 신의 마음은 나날이\n 서두르고 있다고 요량하라 4-7\n",
                            japanese = "これからハ神の心ハにち／＼に\nせきこみあるとをもいこそしれ",
                            english = "korekara wa Kami no kokoro wa nichinichi ni\n" +
                                    "sekikomi aru to omoikoso shire",
                            commentary = ""
                        )
                    )

                    allContent.add(
                        ContentItem(
                            korean = " 아무리 급히 서두르고 있을지라도\n 입으로는 아무것도 말하지 않는 거야 4-8\n",
                            japanese = "いかほどのをふくせきこみあるとても\nくちでハなにもゆうでないぞや",
                            english = "ika hodono oku sekikomi aru totemo\n" +
                                    "kuchi dewa nanimo yu de nai zo ya",
                            commentary = "七, 八,어버이신이 인류구제를 위해 아무리 서두를지라도 결코 입으로 이렇게 하라, 저렇게 하라고 지시하지는 않는다."
                        )
                    )

                    allContent.add(
                        ContentItem(
                            korean = " 앞으로는 많이 모여올 사람들을\n 빨리 알려 두고 싶지만 4-9\n",
                            japanese = "このさきハをふくみへくる人ゞを\nはやくしらしてをことをもゑど",
                            english = "konosaki wa oku miekuru hitobito o\n" +
                                    "hayaku shirashite oko to omoedo",
                            commentary = "九,이 길이 점차 세상에 전해져 한길이 됨에 따라 많은 사람들이 터전을 그리며 찾아오게 될 터인데, 그 가운데서 용재될 사람을 곁의 사람들에게 미리 알려 두고자 하나 아마 믿지 않을 것이다."
                        )
                    )

                    allContent.add(
                        ContentItem(
                            korean = " 차츰차츰 진기한 사람 보인다\n 누구의 눈에도 이것이 안 보이는가 4-10\n",
                            japanese = "だん／＼とめつらし人がみへてある\nたれがめへにもこれがみゑんか",
                            english = "dandan to mezurashi hito ga miete aru\n" +
                                    "tare ga me nimo kore ga mien ka",
                            commentary = ""
                        )
                    )

                    allContent.add(
                        ContentItem(
                            korean = " 이제부터 다음 이야기를 하마 여러가지\n 길을 두고 보라 진기한 길 4-11\n",
                            japanese = "これからのあとなるはなし山／＼の\nみちをみていよめづらしきみち",
                            english = "korekara no ato naru hanashi yamayama no\n" +
                                    "michi o mite iyo mezurashiki michi",
                            commentary = "十一,이제부터 다음 이야기를 일러주겠는데, 어버이신이 하는 이야기에는 결코 거짓이 없다. 차차로 "
                        )
                    )

                    allContent.add(
                        ContentItem(
                            korean = " 재미있다 많은 사람 모여들어서\n 하늘의 혜택이라 말할 거야 4-12\n",
                            japanese = "をもしろやをふくの人があつまりて\n天のあたゑとゆうてくるそや",
                            english = "omoshiro ya oku no hito ga atsumarite\n" +
                                    "ten no atae to yute kuru zo ya",
                            commentary = "十二,터전의 리가 나타나서 어버이신의 의도를 알게 되면, 많은 사람들이 입을 모아 하늘의 덕을 찬양하며 그 혜택을 받고자 터전으로 참배하러 오게 된다."
                        )
                    )

                    allContent.add(
                        ContentItem(
                            korean = " 나날이 몸의 장애로 또 오는구나\n 신이 기다리고 있음을 모르고서 4-13\n",
                            japanese = "にち／＼にみにさハりつくまたきたか\n神のまちかねこれをしらすに",
                            english = "nichinichi ni mi ni sawari tsuku mata kita ka\n" +
                                    "Kami no machikane kore o shirazu ni",
                            commentary = "十三,몸에 장애를 받을 때마다 터전으로 돌아오지만 시간이 지나면 그 고마움을 잊어비리고 만다. 어버이신이 이렇게 자주 몸을 통해 알리는 것은 신의 용재로 쓰려고 기다리고 있기 때문인데, 이 마음도 모르고 멍청히 살고 있다."
                        )
                    )

                    allContent.add(
                        ContentItem(
                            korean = " 차츰차츰 근행인원의 손을 갖추면\n 이것을 계기로 무엇이든 시작한다 4-14\n",
                            japanese = "だん／＼とつとめのにんぢうてがそろい\nこれをあいつになにもでかける",
                            english = "dandan to Tsutome no ninju te ga soroi\n" +
                                    "kore o aizu ni nanimo dekakeru",
                            commentary = "十四,근행인원을 차차로 갖추게 된면 그것을 계기로 만가지구제의 길을 시작한다.\n근행인원은 제6호 29~31수의 주석 참조."
                        )
                    )

                    allContent.add(
                        ContentItem(
                            korean = " 나날이 신의 마음을 차츰차츰\n 윗사람들의 마음에 빨리 알린다면 4-15\n",
                            japanese = "にち／＼の神の心わだん／＼と\n上の心にはやくみせたら",
                            english = "nichinichi no Kami no kokoro wa dandan to\n" +
                                    "kami no kokoro ni hayaku misetara",
                            commentary = "十五,나날이 구제를 서두르는 어버이신의 마음을 빨리 윗사람들에게 알린다면 이 길은 세상에 널리 전파될 것이다."
                        )
                    )

                    allContent.add(
                        ContentItem(
                            korean = " 윗사람들은 아무것도 모르고서 모르는자를\n 따르는 마음 이것이 안타깝다 4-16\n",
                            japanese = "上たるわなにもしらずにとふぢんを\nしたがう心これがをかしい",
                            english = "kami taru wa nanimo shirazu ni tojin o\n" +
                                    "shitagau kokoro kore ga okashii",
                            commentary = "十六,지도청에 있는 사람들은 아무것도 모른 채 아직 어버이신의 가르침을 모르는 자의 말만 듣고 그대로 따르고 있는데, 이러한 마음자세가 참으로 안타깝기만 하다.\n모르는자는 제2호 34수의 주석 참조."
                        )
                    )

                    allContent.add(
                        ContentItem(
                            korean = " 나날이 신이 마음 서두르는 것은\n 모르는자들 마음 바꾸기를 기다린다 4-17\n",
                            japanese = "にち／＼に神の心のせきこみハ\nとふぢんころりこれをまつなり",
                            english = "nichinichi ni Kami no kokoro no sekikomi wa\n" +
                                    "tojin korori kore o matsu nari",
                            commentary = "十七,나날이 어버이신이 서두르고 있는 것은 아직 어버이신의 가르침을 모르는 자들도 깨끗하게 마음을 바꾸어 신의를 깨닫도록 하는 것으로서, 그날이 빨리 오기를 기다리고 있다."
                        )
                    )

                    allContent.add(
                        ContentItem(
                            korean = " 지금까지 소의 병에 의한 알림을 생각해 보고\n 윗사람들 모두 조심하라 4-18\n",
                            japanese = "いまゝでのうしのさきみちをもてみよ\n上たるところみなきをつけよ",
                            english = "imamade no ushi no sakimichi omote miyo\n" +
                                    "kami taru tokoro mina ki o tsuke yo",
                            commentary = "十八,종전에 유행했던 비참한 소의 병을 잘 생각해 보라, 그때 나쁜 병이 유행했던 것은 윗사람들이 어버이신의 마음을 깨닫지 못하고 오직 인간생각에만 흘렀기 때문이니, 앞으로 모두들은 이 점을 각별히 조심하도록 하라.\n 소의 병에 의한 알림 전해 오는 말에 의하면 그해 야마또 지방에는 급성 소의 병이 유형하여 갑자기 많은 소들이 쓰러졌으며, 그 이듬해에는 사람들에게도 악성전염병이 크게 유형했다고 한다."
                        )
                    )

                    allContent.add(
                        ContentItem(
                            korean = " 이것만 모두 납득하게 된다면\n 세상 사람들의 마음 모두 용솟음친다 4-19\n",
                            japanese = "これさいかみなみへきたる事ならば\nせかいの心みないさみくる",
                            english = "kore saika mina mie kitaru koto naraba\n" +
                                    "sekai no kokoro mina isami kuru",
                            commentary = "十九,이것만 사람들이  알게 된다면 세상 사람들의 마음은 모두 용솟음치게 된다."
                        )
                    )

                    allContent.add(
                        ContentItem(
                            korean = " 무엇이든 세상 사람들의 마음 용솟음치면\n 신의 마음도 용솟음친다 4-20\n",
                            japanese = "なにゝてもせかいの心いさむなら\n神の心もみないさむなり",
                            english = "nani nitemo sekai no kokoro isamu nara\n" +
                                    "Kami no kokoro mo mina isamu nari",
                            commentary = ""
                        )
                    )

                    allContent.add(
                        ContentItem(
                            korean = " 오늘의 길 어떤 길이라 생각하는가\n 진기한 일이 나타날 거야 4-21\n",
                            japanese = "けふの日ハいかなるみちとをもうかな\nめづらし事がみゑてくるぞや",
                            english = "kyonohi wa ika naru michi to omou kana\n" +
                                    "mezurashi koto ga miete kuru zo ya",
                            commentary = "二十一,현재 걷고 있는 이 길이 어떤 길이라 생각하는가, 멍청히 걷고 있을 때가 아니다. 곧 진기한 일이 눈앞에 나타나게 된다."
                        )
                    )

                    allContent.add(
                        ContentItem(
                            korean = " 차츰차츰 어떤 일도 나타난다\n 어떤 길이건 모두 즐거워하라 4-22\n",
                            japanese = "だん／＼になにかの事もみへてくる\nいかなるみちもみなたのしめよ",
                            english = "dandan ni nanika no koto mo miete kuru\n" +
                                    "ika naru michi mo mina tanoshime yo",
                            commentary = "二十二,모든 일이 어버이신의 의도대로 차츰 나타날 것이니, 현재는 아무리 어려운 길이라도 마음을 움츠리지 말고 장래를 낙으로 삼아 걸어가야 한다."
                        )
                    )

                    allContent.add(
                        ContentItem(
                            korean = " 나날이 즐거운근행의 손짓을 익히게 되면\n 신의 즐거움 어떠하리오 4-23\n",
                            japanese = "にち／＼によふきづとめのてがつけば\n神のたのしゆみいかほとの事",
                            english = "nichinichi ni Yoki-zutome no te ga tsukeba\n" +
                                    "Kami no tanoshimi ika hodono koto",
                            commentary = ""
                        )
                    )

                    allContent.add(
                        ContentItem(
                            korean = " 어서어서 근행인원을 고대한다\n 곁의 사람들의 마음은 무엇을 생각하는가 4-24\n",
                            japanese = "はや／＼とつとめのにんぢうまちかねる\nそばな心わなにをふもうや",
                            english = "hayabaya to Tsutome no ninju machikaneru\n" +
                                    "soba na kokoro wa nani o omou ya",
                            commentary = "二十三, 二十四,어버이신이 바라고 있는 즐거운근행의 손짓을 모두가 나날이 익히게 된다면, 어버이신은 얼마나 기쁘랴. 이토록 근행인원이 하루라도 빨리 모이기를 고대하고 있는 어버이신의 마음을 모른 채, 곁의 사람들은 대체 무엇을 생각하고 있는가."
                        )
                    )

                    allContent.add(
                        ContentItem(
                            korean = " 원래 질병이란 없는 것이지만\n 몸의 장애 나타나는 것은 신의 소용 4-25\n",
                            japanese = "いかなるのやまいとゆうてないけれど\nみにさわりつく神のよふむき",
                            english = "ika naru no yamai to yute nai keredo\n" +
                                    "mi ni sawari tsuku Kami no yomuki",
                            commentary = "二十五,원래 질병이란 이 세상에 없는 것이지만, 몸에 장애가 나타나는 것은 어버이신이 소용에 쓰려고 인도하는 것이므로 이 점을 깊이 깨닫지 않으면 안된다."
                        )
                    )

                    allContent.add(
                        ContentItem(
                            korean = " 소둉도 어떤 것인지 좀처럼 몰라\n 신의 의도 태산 같아서 4-26\n",
                            japanese = "よふむきもなにの事やら一寸しれん\n神のをもわくやま／＼の事",
                            english = "yomuki mo nanino koto yara choto shiren\n" +
                                    "Kami no omowaku yamayama no koto",
                            commentary = ""
                        )
                    )

                    allContent.add(
                        ContentItem(
                            korean = " 무엇이든 신의 의도 어쨌든\n 모두 일러준다면 마음 용솟음칠 거야 4-27\n",
                            japanese = "なにもかも神のをもハくなにゝても\nみなといたなら心いさむで",
                            english = "nanimo kamo Kami no omowaku nani nitemo\n" +
                                    "mina toita nara kokoro isamu de",
                            commentary = ""
                        )
                    )

                    allContent.add(
                        ContentItem(
                            korean = " 차츰차츰 무엇이든 의도를 일러주면\n 몸의 장애도 깨끗해진다 4-28\n",
                            japanese = "だん／＼になにもをもハくときゝれば\nみのうちよりもすゝやかになる",
                            english = "dandan ni nanimo omowaku tokikireba\n" +
                                    "minouchi yori mo susuyaka ni naru",
                            commentary = "二十八,차츰 어버이신이 의도를 일러줄 것인데, 이것을 사람들이 깨닫게 된다면 저절로 마음도 용솟음치고 몸도 건강하게 된다."
                        )
                    )

                    allContent.add(
                        ContentItem(
                            korean = " 또 앞으로 즐거운근행 고대한다\n 무엇이냐 하면 신악근행인 거야 4-29\n",
                            japanese = "またさきのよふきづとめをまちかねる\nなんの事ならかぐらつとめや",
                            english = "matasaki no Yoki-zutome o machikaneru\n" +
                                    "nanno koto nara Kagura-zutome ya",
                            commentary = "二十九,또 앞으로는 하루빨리 인원을 갖추어 즐거운근행을 올려 주기를 고대하고 있는데, 그것은 어떤 근행인가 하면 신악근행이다."
                        )
                    )

                    allContent.add(
                        ContentItem(
                            korean = " 온 세상에 많은 사람들이 있지만\n 신의 마음을 아는 자는 없다 4-30\n",
                            japanese = "せかいぢうをふくの人であるけれど\n神の心をしりたものなし",
                            english = "sekaiju oku no hito de aru keredo\n" +
                                    "Kami no kokoro o shirita mono nashi",
                            commentary = ""
                        )
                    )

                    allContent.add(
                        ContentItem(
                            korean = " 이번에는 신의 마음의 진실을\n 무엇이든 자세히 모두 가르칠테다 4-31\n",
                            japanese = "このたびハ神の心のしんぢつを\nなにかいさいをみなをしゑるで",
                            english = "konotabi wa Kami no kokoro no shinjitsu o\n" +
                                    "nanika isai o mina oshieru de",
                            commentary = ""
                        )
                    )

                    allContent.add(
                        ContentItem(
                            korean = " 무엇이든 신한줄기를 알게 되면\n 미칠곳에 못 이길 리는 없는 거야 4-32\n",
                            japanese = "なにゝても神一ちよをしりたなら\nからにまけそな事ハないぞや",
                            english = "nani nitemo Kami ichijo o shirita nara\n" +
                                    "kara ni makeso na koto wa nai zo ya",
                            commentary = "三十二,무슨 일이든지 신한줄기의 진실한 가르침을 참으로 깨닫게 된다면, 어버이신의 가르침을 아직 모르는 사람들의 인간생각 따위에 휘둘릴 리는 없다."
                        )
                    )

                    allContent.add(
                        ContentItem(
                            korean = " 앞으로는 미칠곳과 미친곳을 빨리\n 차츰차츰 구분할 준비만을 4-33\n",
                            japanese = "このさきハからとにほんをすみやかに\nだん／＼ハけるもよふばかりを",
                            english = "konosaki wa kara to nihon o sumiyaka ni\n" +
                                    "dandan wakeru moyo bakari o",
                            commentary = "三十三,앞으로는 어버이신의 가르침을 아는 자와 아직 모르는 자를 구분해서 점차 온 세상 사람들의 마음을 맑힐 준비를 한다."
                        )
                    )

                    allContent.add(
                        ContentItem(
                            korean = " 이것만 빨리 알게 되면\n 신의 섭섭함 풀어지리라 4-34\n",
                            japanese = "これさいかはやくわかりた事ならば\n神のざんねんはれる事なり",
                            english = "kore saika hayaku wakarita koto naraba\n" +
                                    "Kami no zannen hareru koto nari",
                            commentary = ""
                        )
                    )

                    allContent.add(
                        ContentItem(
                            korean = " 진실한 신의 섭섭함이 풀어지면\n 세상 사람들의 마음 모두 용솟음친다 4-35\n",
                            japanese = "しんぢつの神のざんねんはれたなら\nせかいの心みないさみでる",
                            english = "shinjitsu no Kami no zannen hareta nara\n" +
                                    "sekai no kokoro mina isami deru",
                            commentary = "三十四, 三十五,이것만 사람들이 잘 깨닫게 되면 어버이신의 섭섭한 마음도 풀려 활짝 개게 될 것이고, 그에 따라 세상 사람들의 마음도 저절로 용솟음치게 될 것이다."
                        )
                    )

                    allContent.add(
                        ContentItem(
                            korean = " 차츰차츰 온 세상을 진실하게\n 구제할 준비만을 하는 거야 4-36\n",
                            japanese = "だん／＼とせかいぢううをしんぢつに\nたすけるもよふはかりするぞや",
                            english = "dandan to sekaiju o shinjitsu ni\n" +
                                    "tasukeru moyo bakari suru zo ya",
                            commentary = "三十六,어버이신은 모든 인간알 진실로 구제하고자 하는 일념뿐이기 때문에 이제부터는 오로지 구제할 준비만 한다.\n구제할 준비란 감로대를 세워 구제근행인 감로대근행을 올릴 준비를 말한다.(다음 노래 참조)"
                        )
                    )

                    allContent.add(
                        ContentItem(
                            korean = " 그 후로는 앎지 않고 죽지 않고 쇠하지 않고\n 마음에 따라 언제까지나 살리라 4-37\n",
                            japanese = "そのゝちハやまずしなすによハらすに\n心したいにいつまでもいよ",
                            english = "sono nochi wa yamazu shinazu ni yowarazu ni\n" +
                                    "kokoro shidai ni itsu made mo iyo",
                            commentary = "三十七,그 후로는 질병으로 신음하지도 않고 죽지도 않고 노쇠하지도 않고 언제까지나 제 생긱대로 즐거운 삶을 누리게 된다.\n 언제까지나는 제3호 100수의 주석 참조."
                        )
                    )

                    allContent.add(
                        ContentItem(
                            korean = " 또 앞으로 연수가 지나게 되면\n 늙어지는 것은 전혀 없는 거야 4-38\n",
                            japanese = "またさきハねんけんたちた事ならば\nとしをよるめハさらにないぞや",
                            english = "matasaki wa nengen tachita koto naraba\n" +
                                    "toshi o yoru me wa sarani nai zo ya",
                            commentary = "三十八,그 뿐만 아니라, 앞으로 차츰 연수가 지나 사람들의 마음이 맑아지면 언제까지나 늙지 안고 원기왕성하게 활동할 수 있도록 수호한다."
                        )
                    )

                    allContent.add(
                        ContentItem(
                            korean = " 지금까지는 아무것도 몰랐었다\n 이제부터 앞으로 모두 가르칠 테다 4-39\n",
                            japanese = "いまゝでハなにの事でもしれなんだ\nこれからさきハみなをしゑるで",
                            english = "imamade wa nanino koto demo shirenanda\n" +
                                    "korekara saki wa mina oshieru de",
                            commentary = ""
                        )
                    )

                    allContent.add(
                        ContentItem(
                            korean = " 지금까지는 모두들의 마음과 안의\n 마음이 크게 달랐었지만 4-40\n",
                            japanese = "いまてハなみなの心とうちなるの\n心かをふいちがいなれども",
                            english = "ima dewa na mina no kokoro to uchi naru no\n" +
                                    "kokoro ga oi chigai naredomo",
                            commentary = ""
                        )
                    )

                    allContent.add(
                        ContentItem(
                            korean = " 내일부터는 무엇이든 부탁할 테니\n 신의 말대로 따르지 않으면 안돼 4-41\n",
                            japanese = "あすにちハなんでもたのみかけるでな\n神のいぢよにつかねばならん",
                            english = "asunichi wa nandemo tanomi kakeru de na\n" +
                                    "Kami no iijo ni tsukaneba naran",
                            commentary = "四十, 四十一,지금까지는 이 길 안의 사람들의 마음과 밖의 사람들의 마음이 크게 달랐지만, 앞으로는 다같이 어버이신의 말대로 따르지 않으면 안된다.\n 당시는 본교가 초창기였으므로 이 길 안의 사람이나 밖의 사람이나 어버이신님이 구제한줄기와 근행을 서두르시는 뜻을 충분히 이해하지 못했고, 또 양쪽 의견도 서로 달랐다 그래서 앞으로는 이러한 인간마음을 버리고 다 같이 어버이신님의 말씀대로 구제한줄기에 매진하지 않으면 안된다고 말씀하신 것이다."
                        )
                    )

                    allContent.add(
                        ContentItem(
                            korean = " 나날이 몸의 장애로 납득하라\n 마음 틀린 바를 신이 알린다 4-42\n",
                            japanese = "にち／＼にみにさハりつくとくしんせ\n心ちがいを神がしらする",
                            english = "nichinichi ni mi ni sawari tsuku tokushin se\n" +
                                    "kokoro chigai o Kami ga shirasuru",
                            commentary = ""
                        )
                    )

                    allContent.add(
                        ContentItem(
                            korean = " 각자의 몸으로부터 생각해서\n 마음작정하고 신에게 의탁하라 4-43\n",
                            japanese = "めへ／＼のみのうちよりもしやんして\n心さだめて神にもたれよ",
                            english = "meme no minouchi yori mo shiyan shite\n" +
                                    "kokoro sadamete Kami ni motare yo",
                            commentary = ""
                        )
                    )

                    allContent.add(
                        ContentItem(
                            korean = " 무엇이든 신의 의도 깊은 것이니\n 곁의 사람들은 그것을 모르고서 4-44\n",
                            japanese = "なにゝても神のをもわくふかくある\nそばなるものハそれをしらすに",
                            english = "nani nitemo Kami no omowaku fukaku aru\n" +
                                    "soba naru mono wa sore o shirazu ni",
                            commentary = "四十四,곁의 사람들에게 신상으로 알리는 것은 어버이신의 깊은 의도가 있기 때문이다. 따라서 이것을 단순한 질병이라고만 생각하지 말고 그 근본을 깨달아야 할 것이다."
                        )
                    )

                    allContent.add(
                        ContentItem(
                            korean = " 오늘까지는 어떤 길도 보이지 않지만\n 곧 보일 것이니 마음작정을 하라 4-45\n",
                            japanese = "けふまでハなによのみちもみへねども\nはやくみゑるでしやんさだめよ",
                            english = "kyomade wa nani yono michi mo miene domo\n" +
                                    "hayaku mieru de shiyan sadame yo",
                            commentary = "四十五,지금까지는 어떤 길도 아직 보이지 않았기 때문에 이 진실한 가르침을 깊이 믿는 자가 없었으나, 이제부터는 마음쓰는대로 곧 리가 나타날 것이니 잘 생각해서 마음 작정을 해야 한다."
                        )
                    )

                    allContent.add(
                        ContentItem(
                            korean = " 이 길을 속히 알리려고 생각하지만\n 깨달음이 억어서는 이것이 어렵다 4-46\n",
                            japanese = "このみちをはやくしらそとをもへども\nさとりがのふてこれがむつかし",
                            english = "kono michi o hayaku shiraso to omoe domo\n" +
                                    "satori ga note kore ga mutsukashi",
                            commentary = "四十六,이 구제한줄기의 길을 빨리 세상에 알려서 즐거운 삶을 누리게 하고 싶지만, 인간 마음이 방해가 되어 어버이신의 뜻을 깨닫지 못하기 때문에 그것을 실현하기가 매우 어렵다."
                        )
                    )

                    allContent.add(
                        ContentItem(
                            korean = " 차츰차츰 붓으로 알려 두었지만\n 깨달음이 없는 것이 신의 섭섭함 4-47\n",
                            japanese = "たん／＼とふでにしらしてあるけれど\nさとりないのが神のざんねん",
                            english = "dandan to fude ni shirashite aru keredo\n" +
                                    "satori nai no ga Kami no zannen",
                            commentary = "四十七,차츰차츰 장래의 일을 이 붓끝으로 알려 주고 있는데도 곁의 사람들이 그 뜻을 전혀 깨닫지 못하니, 어버이신으로서는 이 점이 참으로 안타까운 일이다."
                        )
                    )

                    allContent.add(
                        ContentItem(
                            korean = " 무엇이든 신이 하는 말 단단히 들어라\n 모두가 각자의 마음 나름이야 4-48\n",
                            japanese = "なにゝても神のゆう事しかときけ\nみなめゑめの心しだいや",
                            english = "nani nitemo Kami no yu koto shikato kike\n" +
                                    "mina meme no kokoro shidai ya",
                            commentary = ""
                        )
                    )

                    allContent.add(
                        ContentItem(
                            korean = " 진실로 마음 용솟음치며 생각해서\n 신에게 의탁하여 즐거운근행을 4-49\n",
                            japanese = "なにゝても神のゆう事しかときけ\nみなめゑめの心しだいや",
                            english = "shinjitsu ni kokoro isande shiyan shite\n" +
                                    "Kami ni motarete Yoki-zutome o",
                            commentary = "四十九,진실로 마음 용솟음치며 어버이신의 가르침을 잘 깨닫는 한편, 매사에 인간생각을 버리고 어버이신에게 의탁해서 즐거운 근행을 해야 한다."
                        )
                    )

                    allContent.add(
                        ContentItem(
                            korean = " 이 이야기 다른 일로 생각 말라\n 오직 거름에 대한 이야기인 거야 4-50\n",
                            japanese = "このはなしなにの事やとをもうなよ\nこゑ一ぢよのはなしなるぞや",
                            english = "kono hanashi nanino koto ya to omouna yo\n" +
                                    "koe ichijo no hanashi naru zo ya",
                            commentary = "五十,지금 이야기하고자 하는 것은 다른 것이 아니라, 오직 거름에 대한 이야기일 뿐이다.\n 터전에서 올리는 만가지구제의 근행가운데 거름의 근행이 있다. 이 거름의 근행은 한번에 겨 서말, 재 서말, 흙 서말을 혼합한 것을 감로대에 올려 기원하는 것인데, 이때 금비 4천 관에 상당하는 거름의 수호를 받게 된다. 그런 다음, 원하는 사람에게 필요한 분량만큼 내려 주는데, 받은 사람은 이것을 자기 전답에 뿌리는 것이다. 한편, 거름의 수훈이란 겨 서홉, 재 서홉, 흙 서홉을 신전에 올리고 거름의 수훈을 받은 사람이 이 수훈을 전한 다음 전답에 뿌리면 금비 40관에 상당하는 효과가 있다고 가르치셨다."
                        )
                    )

                    allContent.add(
                        ContentItem(
                            korean = " 거름이라 하여 다른 것이 효과 있다 생각 말라\n 마음의 성진실이 효과인 거야 4-51\n",
                            japanese = "こへやとてなにがきくとハをもうなよ\n心のまことしんぢつがきく",
                            english = "koe ya tote nani ga kiku towa omouna yo\n" +
                                    "kokoro no makoto shinjitsu ga kiku",
                            commentary = "五十一,거름이라 해도 겨, 재, 흙 그 자체가 효과를 내는 것이 아니라, 진심으로 어버이신에게 의탁하는 그 마음이 어버이신에게 통함으로써 효과가 나타나는 것이다."
                        )
                    )

                    allContent.add(
                        ContentItem(
                            korean = " 진실한 마음을 살펴본 뒤에는\n 어떤 수호도 한다고 생각하라 4-52\n",
                            japanese = "しんぢつの心みさだめついたなら\nいかなしゆこふもするとをもゑよ",
                            english = "shinjitsu no kokoro misadame tsuita nara\n" +
                                    "ikana shugo mo suru to omoe yo",
                            commentary = ""
                        )
                    )

                    allContent.add(
                        ContentItem(
                            korean = " 단단히 들어라 만가지 일을 다 가르쳐\n 어디나 차별은 전혀 없는 거야 4-53\n",
                            japanese = "しかときけよろつの事をみなをしへ\nどこにへだてわさらにないぞや",
                            english = "shikato kike yorozu no koto o mina oshie\n" +
                                    "doko ni hedate wa sarani nai zo ya",
                            commentary = ""
                        )
                    )

                    allContent.add(
                        ContentItem(
                            korean = " 어떤 곳에서 오는 사람이라도\n 모두 인연이 있기 때문에 4-54\n",
                            japanese = "どのよふなところの人がでゝきても\nみないんねんのものであるから",
                            english = "dono yona tokoro no hito ga detekite mo\n" +
                                    "mina innen no mono de aru kara",
                            commentary = "五十四,어떤 곳에서도 많은 사람들이 터전을 그리며 돌아오게 될 터인데, 그것은 그들이 모두 진실한 어버이신의 자녀이기 때문에 그 인연으로 해서 귀참하는 것인 만큼, 결코 우연이라 생각해서는 안된다."
                        )
                    )

                    allContent.add(
                        ContentItem(
                            korean = " 인간을 창조한 집터이니라\n 그 인연으로써 하강했다 4-55\n",
                            japanese = "にんけんをはじめだしたるやしきなり\nそのいんねんであまくたりたで",
                            english = "ningen o hajime dashitaru yashiki nari\n" +
                                    "sono innen de amakudarita de",
                            commentary = "五十五,터전은 인간이 잉태된 본고장이다. 그러한 안태본(安胎本)의 인연으로 해서 어버이신이 이곳에 하강한 것이다.\n 이것이 곧 본교에서 말하는 터전의 인연(집터의 인연)이다. 본교의 발상(發祥)은 이 터전의 인연과 순각한의 인연 및 교조 혼의 인연 등, 이 세 가지 인연이 일치함으로써 비롯된 것이다."
                        )
                    )

                    allContent.add(
                        ContentItem(
                            korean = " 앞으로는 온 세상을 한결같이\n 구제할 길을 모두 가르칠 테다 4-56\n",
                            japanese = "このさきハせかいぢううを一れつに\nたすけしゆごふをみなをしゑるで",
                            english = "konosaki wa sekaiju o ichiretsu ni\n" +
                                    "tasuke shugo o mina oshieru de",
                            commentary = ""
                        )
                    )

                    allContent.add(
                        ContentItem(
                            korean = " 차츰차츰 만가지 구제를 모두 가르쳐\n 미칠곳과 미친곳을 구분할 뿐이야 4-57\n",
                            japanese = "だん／＼とよろづたすけをみなをしへ\nからとにほんをわけるばかりや",
                            english = "dandan to yorozu tasuke o mina oshie\n" +
                                    "kara to nihon o wakeru bakari ya",
                            commentary = ""
                        )
                    )

                    allContent.add(
                        ContentItem(
                            korean = " 나날이 미칠곳과 미친곳을 구분하는 길\n 이것이 신이 서두르는 한결같은 마음 4-58\n",
                            japanese = "にち／＼にからとにほんをわけるみち\n神のせきこみこれが一ぢよ",
                            english = "nichinichi ni kara to nihon o wakeru michi\n" +
                                    "Kami no sekikomi kore ga ichijo",
                            commentary = "五十七, 五十八,점차 만가지구제의 근행을 가르치고 리를 일러주어서, 어버이신의 가르침을 아는 자와 아직 모르는 자를 구분해서 밝힌다. 어버이신이 한결같은 마음으로 서두르고 있는 것은 바로 이것뿐이다. \n제2호 34의 주석 참조."
                        )
                    )

                    allContent.add(
                        ContentItem(
                            korean = " 이 길을 빨리 구분하게 되면\n 나머지 만가지는 신의 마음대로야 4-59\n",
                            japanese = "このみちをはやくわけたる事ならば\nあとのよろづハ神のまゝなり",
                            english = "kono michi o hayaku waketaru koto naraba\n" +
                                    "ato no yorozu wa Kami no mama nari",
                            commentary = "五十九,이것만 빨리 구분해서 밝히게 되면 그 다음에는 만사를 어버이신이 자유자재로 수호한다."
                        )
                    )

                    allContent.add(
                        ContentItem(
                            korean = " 오늘은 무엇인가 진기한 일을 시작하므로\n 만가지 인연 모두 따라온다 4-60\n",
                            japanese = "けふの日ハなにかめづらしはじめだし\nよろづいんねんみなついてくる",
                            english = "kyonohi wa nanika mezurashi hajime dashi\n" +
                                    "yorozu innen mina tsuite kuru",
                            commentary = "六十,이번에는 어떻든 진기한 구제한줄기의 길을 시작하여 이 세상 태초의 진실을 일러준다. 그러면 사람들은 모두 어버이신의 자녀임을 깨닫고 이 길을 따라온다."
                        )
                    )

                    allContent.add(
                        ContentItem(
                            korean = " 인연도 많은 사람이기 때문에\n 어디나 차별이 있다고 생각 말라 4-61\n",
                            japanese = "いんねんもをふくの人であるからに\nとこにへだてハあるとをもうな",
                            english = "innen mo oku no hito de aru karani\n" +
                                    "doko ni hedate wa aru to omouna",
                            commentary = "六十一,가지각색의 인연으로 말미암아 서로 인종이 다르고 처지가 다르긴 하나 그렇더라도 어버이신의 수호에 차별이 있는 것은 아니다. 그리고 나라나 사람에 따라서도 차별하지 않는다. 공평무사야말로 어버이신의 마음이다."
                        )
                    )

                    allContent.add(
                        ContentItem(
                            korean = " 이 세상을 창조한 신이기에\n 온 세상 사람들이 모두 다 자녀다 4-62\n",
                            japanese = "このよふを初た神の事ならば\nせかい一れつみなわがこなり",
                            english = "kono yo o hajimeta Kami no koto naraba\n" +
                                    "sekai ichiretsu mina waga ko nari",
                            commentary = ""
                        )
                    )

                    allContent.add(
                        ContentItem(
                            korean = " 온 세상 자녀가 귀엽기 때문에\n 여러가지로 마음 기울이는 거야 4-63\n",
                            japanese = "いちれつのこともがかハいそれゆへに\nいろ／＼心つくしきるなり",
                            english = "ichiretsu no kodomo ga kawai sore yue ni\n" +
                                    "iroiro kokoro tsukushi kiru nari",
                            commentary = ""
                        )
                    )

                    allContent.add(
                        ContentItem(
                            korean = " 이 자녀에게 무엇이든 가르쳐서 어서어서\n 신이 마음 서두르고 있음을 보라 4-64\n",
                            japanese = "このこともなにもをしへてはや／＼と\n神の心のせきこみをみよ",
                            english = "kono kodomo nanimo oshiete hayabaya to\n" +
                                    "Kami no kokoro no sekikomi o miyo",
                            commentary = "六十二～六十四,온 세상 사람들은 똑같이 어버어신의 자녀이다. 그러므로 이들에게 무엇이든 가르쳐서 즐겁게 살도록 하려고 서두르고 있는 어버이신의 마음을 너희들은 알아야 한다."
                        )
                    )

                    allContent.add(
                        ContentItem(
                            korean = " 차츰차츰 자녀의 성인을 고대한다\n 신의 의도 이것뿐이다 4-65\n",
                            japanese = "だん／＼とこどものしゆせまちかねる\n神のをもわくこればかりなり",
                            english = "dandan to kodomo no shusse machikaneru\n" +
                                    "Kami no omowaku kore bakari nari",
                            commentary = "六十五,자녀의 성인이란 어버이신님의 자녀인 인간이 점차 어버이신님으로부터 가르침을 받아 욕심을 잊고 어버이신님의 의도를 잘 깨닫게 되는 것을 말한다."
                        )
                    )

                    allContent.add(
                        ContentItem(
                            korean = " 자녀만 어서 세상에 나가게 되면\n 미칠곳을 미친곳의 땅으로 하리라 4-66\n",
                            japanese = "こどもさいはやくをもていだしたなら\nからをにほんのぢいにするなり",
                            english = "kodomo sai hayaku omote i dashita nara\n" +
                                    "kara o nihon no jii ni suru nari",
                            commentary = "六十六,사람들의 신앙이 향상되어 널리 구제한줄기의 활동을 하게만 된다면, 지금까지 어버이신의 뜻을 모르던 곳까지도 어버이신의 구제한줄기의 길을 빠짐억이 전하여 모두가 평화롭고 즐거운 삶을 누릴 수 있도록 수호할 것이다. 이것이야말로 어버이신의 이상이다.\n제2호 34의 주석 참조. "
                        )
                    )

                    allContent.add(
                        ContentItem(
                            korean = " 진실로 자녀들은 마음 단단히 가져라\n 신의 마음은 서두름뿐이야 4-67\n",
                            japanese = "しんぢつにこどもの心しかとせよ\n神の心ハせくばかりやで",
                            english = "shinjitsu ni kodomo no kokoro shikato seyo\n" +
                                    "Kami no kokoro wa seku bakari ya de",
                            commentary = "六十七,자녀들아, 진실로 마음을 단단히 정해서 어버이신을 따라오라. 어버이신은 오직 구제한줄기만을 서두르고 있을 뿐이다."
                        )
                    )

                    allContent.add(
                        ContentItem(
                            korean = " 나날이 신이 서두르고 있는 이 고민\n 어서 구제할 준비를 해 다오 4-68\n",
                            japanese = "にち／＼に神のせきこみこのなやみ\nはやくたすけるもよふしてくれ",
                            english = "nichinichi ni Kami no sekikomi kono nayami\n" +
                                    "hayaku tasukeru moyo shite kure",
                            commentary = "六十八,나날이 어버이신이 온 세상 사람들을 구제하기 위해 서두르고 있는 이 마음을 헤아려서, 어서 만가지구제의 준비를 서둘러 다오."
                        )
                    )

                    allContent.add(
                        ContentItem(
                            korean = " 안의 사람은 윗사람 때문에 침울하고 있다\n 두려울 것 없다 신이 맡았으니 4-69\n",
                            japanese = "うちなるハ上をふもふていづみいる\nこわみないぞや神のうけやい",
                            english = "uchi naru wa kami o omote izumi iru\n" +
                                    "kowami nai zo ya Kami no ukeyai",
                            commentary = "六十九,당시 내부 사람들은 관헌의 탄압이 두려워 마음이 움츠리고 있었는데, 어버이신님은 그들에게 결코 두려워할 것 없다. 신이 단단히 맡아 줄 것이니 안심하고 매진하라고 말씀하신 것이다."
                        )
                    )

                    allContent.add(
                        ContentItem(
                            korean = " 지금까지와는 길이 바뀌었으니\n 속히 서둘러서 한길을 내도록 4-70\n",
                            japanese = "いまゝでとみちがかわりてあるほどに\nはやくせきこみをふくハんのみち",
                            english = "imamade to michi ga kawarite aru hodoni\n" +
                                    "hayaku sekikomi okwan no michi",
                            commentary = ""
                        )
                    )

                    allContent.add(
                        ContentItem(
                            korean = " 이 길은 언제 이루어지리라 생각하는가\n 빨리 나와 보라 벌써 눈앞에 다가와 4-71\n",
                            japanese = "このみちハいつの事やとをもている\nはやくてゝみよもふいまの事",
                            english = "kono michi wa itsu no koto ya to omote iru\n" +
                                    "hayaku dete miyo mo ima no koto",
                            commentary = "七十一,언제 한길로 나가게 될 것인가고 궁금하게 여기겠지만, 그것은 먼 장래의 일이 아니다. 당장이라도 눈앞에 나타난다."
                        )
                    )

                    allContent.add(
                        ContentItem(
                            korean = " 차츰차츰 붓으로 알려 두었으니\n 어서 마음에 깨닫도록 하라 4-72\n",
                            japanese = "だん／＼とふてにしらしてあるほどに\nはやく心にさとりとるよふ",
                            english = "dandan to fude ni shirashite aru hodoni\n" +
                                    "hayaku kokoro ni satori toru yo",
                            commentary = ""
                        )
                    )

                    allContent.add(
                        ContentItem(
                            korean = " 이것만 빨리 깨닫게 되면\n 몸의 괴로움도 깨끗이 없어진다 4-73\n",
                            japanese = "これさいかはやくさとりがついたなら\nみのうちなやみすゞやかになる",
                            english = "kore saika hayaku satori ga tsuita nara\n" +
                                    "minouchi nayami suzuyaka ni naru",
                            commentary = "七十二, 七十三,붓으로 이미 여러 가지 알려 두엇으니 이것을 어서 깨닫도록 하라. 어버이신의 마음만 깨닫게 되면 몸의 장애도 도움받고 마음도 깨끗이 맑아진다."
                        )
                    )

                    allContent.add(
                        ContentItem(
                            korean = " 근행도 시작은 손춤 또 신악\n 조금 좁은 길 내어 두었지만 4-74\n",
                            japanese = "つとめても初てをどりまたかぐら\n一寸のほそみちつけてあれども",
                            english = "Tsutome demo hajime Teodori mata Kagura\n" +
                                    "choto no hosomichi tsukete aredomo",
                            commentary = ""
                        )
                    )

                    allContent.add(
                        ContentItem(
                            korean = " 차츰차츰 잡초가 우거져 길을 모르니\n 속히 본길 낼 준비를 4-75\n",
                            japanese = "だん／＼とくさがしこりてみちしれす\nはやくほんみちつけるもよふを",
                            english = "dandan to kusa ga shikorite michi shirezu\n" +
                                    "hayaku honmichi tsukeru moyo o",
                            commentary = "七十四, 七十五,어버이신님은 처음에는 손춤을, 다음에는 신악근행을, 이렇게 차츰 가르치면서 빨리 즐거운근행을 올리기를 바라고 계셨으나, 당시 신자들 가운데는 관헌의 압박과 간섭 때문에 불안을 느끼고 신앙을 그만둘 생각을 하거나, 또는 태만한 마음에서 근행을 게을리하고 있었는데, 이것은 마치 좁은 길에 잡초가 우거져 길을 덮어 버린 것과 같은 이치로서, 이래서는 이 길이 늦어질 뿐이므로 빨리 본길로 나갈 준비를 하라고 말씀하신 것이다."
                        )
                    )

                    allContent.add(
                        ContentItem(
                            korean = " 나날이 마음 용솟음치며 서둘러라\n 속히 본길을 내게 되면 4-76\n",
                            japanese = "にち／＼に心いさんでせきこめよ\nはやくほんみちつけた事なら",
                            english = "nichinichi ni kokoro isande sekikome yo\n" +
                                    "hayaku honmichi tsuketa koto nara",
                            commentary = ""
                        )
                    )

                    allContent.add(
                        ContentItem(
                            korean = " 진실로 이 본길을 내게 되면\n 장래에는 오로지 즐거움만 넘칠 거야 4-77\n",
                            japanese = "しんぢつにこのほんみちがついたなら\nすへハたのもしよふきづくめや",
                            english = "shinjitsu ni kono honmichi ga tsuita nara\n" +
                                    "sue wa tanomoshi yokizukume ya",
                            commentary = ""
                        )
                    )

                    allContent.add(
                        ContentItem(
                            korean = " 마을 사람들을 구제하고자 더욱 서두르고 있다\n 어서 깨달아 주도록 4-78\n",
                            japanese = "村かたハなをもたすけをせへている\nはやくしやんをしてくれるよふ",
                            english = "murakata wa naomo tasuke o sete iru\n" +
                                    "hayaku shiyan o shite kureru yo",
                            commentary = "七十八,특히 마을 사람들을 하루빨리 구제하고자 어버이신은 서두르고 있으니, 이 마음을 속히 깨달아 다오.\n 마을 사람들이란 당시 쇼야시까 마을(현재 교회본부 소재지) 사람들을 말한다."
                        )
                    )

                    allContent.add(
                        ContentItem(
                            korean = " 온 세상 신에게는 모두 다 자녀\n 사람들은 모두 어버이로 생각하라 4-79\n",
                            japanese = "せかいぢう神のたあにハみなわがこ\n一れつハみなをやとをもゑよ",
                            english = "sekaiju Kami no ta niwa mina waga ko\n" +
                                    "ichiretsu wa mina Oya to omoe yo",
                            commentary = ""
                        )
                    )

                    allContent.add(
                        ContentItem(
                            korean = " 온 세상에서는 설교랍시고 시작해서\n 일러주니 들어러 가다 4-80\n",
                            japanese = "せかいぢうせきゝよとしてはちめかけ\nといてきかするきゝにいくなり",
                            english = "sekaiju sekkyo to shite hajime kake\n" +
                                    "toite kikasuru kiki ni iku nari",
                            commentary = ""
                        )
                    )

                    allContent.add(
                        ContentItem(
                            korean = " 아무리 보이는 것을 말할지라도\n 근본을 모르면 알지 못하리 4-81\n",
                            japanese = "いかほどにみゑたる事をゆうたとて\nもとをしらねばハかるめハなし",
                            english = "ika hodoni mietaru koto o yuta tote\n" +
                                    "moto o shiraneba wakaru me wa nashi",
                            commentary = "八十, 八十一,세상에서는 설교랍시고 사람이 행해야 할 길을 일러주고, 또 이것을 들으러 가는 사람도 많다. 그러나 눈에 보이는, 이미 알고 있는 일만을 설교할 뿐 태초의 인연에 관해서는 일러주지 않기 때문에, 사람들이 마음속으로 납득할 리가 없다. "
                        )
                    )

                    allContent.add(
                        ContentItem(
                            korean = " 차츰차츰 없던 일만 말해 두고서\n 그것 나타나면 이것이 진실이야 4-82\n",
                            japanese = "だん／＼とない事ばかりゆてをいて\nそれでたならばこれがまことや",
                            english = "dandan to nai koto bakari yute oite\n" +
                                    "sore deta naraba kore ga makoto ya",
                            commentary = "八十二,어버이신은 눈에 안 보이는 일이나 장래 있을 일만을 말하기 때문에 듣는 사람 가운데는 의심하는 사람도 있으나, 그것이 사실로 나타나게 되면 그때는진실임을 믿게 될 것이다."
                        )
                    )

                    allContent.add(
                        ContentItem(
                            korean = " 한결같이 신에 의탁하는 이 자녀\n 어서 세상에 나갈 준비를 하라 4-83\n",
                            japanese = "一れつに神にもたれるこのこども\nはやくをもていでるもよふせよ",
                            english = "ichiretsu ni Kami ni motareru kono kodomo\n" +
                                    "hayaku omote i deru moyo seyo",
                            commentary = "八十三,육친의 어버이를 의지하듯, 어버이신에게 의탁하여 따라오는 자녀들아, 빨리 밝은 길로 나갈 준비를 하라."
                        )
                    )

                    allContent.add(
                        ContentItem(
                            korean = " 진실로 세상에 나가려거든\n 마음을 가다듬어 중심을 찾아라 4-84\n",
                            japanese = "しんぢつにをもてゞよふとをもうなら\n心しづめてしんをたづねよ",
                            english = "shinjitsu ni omote deyo to omou nara\n" +
                                    "kokoro shizumete shin o tazune yo",
                            commentary = "八十四,진실로 밝은 길로 나가려고 생각한다면 마음을 가다듬어 중심을 찾아라\n 중심이란 이 길의 진수(眞髓)로서 사람인 경우 교조님을 뜻하며, 장소인 경우에는 집터의 중심이 되는 터전을 가리키는데, 터전이 정해진 것은 1875년 터전 결정에 의해서이다."
                        )
                    )

                    allContent.add(
                        ContentItem(
                            korean = " 이 자녀의 진실한 가슴속을\n 살펴본 뒤 어떤 준비도 4-85\n",
                            japanese = "このこどもしんぢつよりもむねのうち\nみさだめつけばいかなもよふも",
                            english = "kono kodomo shinjitsu yori mo mune no uchi\n" +
                                    "misadame tsukeba ikana moyo mo",
                            commentary = "八十五,어버이신을 그리며 도움받기를 원하는 자녀들의 진실한 마음을 살펴본 다음, 구제를 위한 어떠한 준비도 해 주리라."
                        )
                    )

                    allContent.add(
                        ContentItem(
                            korean = " 나날이 신의 마음은 서둘러도\n 자녀의 마음으로는 아는 바 없으므로 4-86\n",
                            japanese = "にち／＼に神の心わせきこめど\nこともの心わかりないので",
                            english = "nichinichi ni Kami no kokoro wa sekikomedo\n" +
                                    "kodomo no kokoro wakari nai node",
                            commentary = ""
                        )
                    )

                    allContent.add(
                        ContentItem(
                            korean = " 자녀도 적은 수가 아닌 만큼\n 많은 사람 가슴속이라 더욱 모른다 4-87\n",
                            japanese = "こともでも一寸の人でハないからに\nをふくのむねがさらにハからん",
                            english = "kodomo demo chotto no hito dewa nai karani\n" +
                                    "oku no mune ga sarani wakaran",
                            commentary = "八十六, 八十七,어버이신은 나날이 구제한줄기의 길을 서두르고 있으나, 자녀들은 이것을 모르니 참으로 안타깝다. 자녀도 적은 수가 아닌 많은 사람이 어버이신의 마음을 모르므로 세상은 더욱 혼동할 수 밖에 없6ㅏ."
                        )
                    )

                    allContent.add(
                        ContentItem(
                            korean = " 지금까지는 학문 등을 말할지라도\n 나타나지 않은 일은 결코 모르겠지 4-88\n",
                            japanese = "いまゝでハがくもんなぞとゆうたとて\nみゑてない事さらにしろまい",
                            english = "imamade wa gakumon nazo to yuta tote\n" +
                                    "miete nai koto sarani shiromai",
                            commentary = ""
                        )
                    )

                    allContent.add(
                        ContentItem(
                            korean = " 앞으로는 나타나지 않은 일 차츰차츰\n 만가지 일을 모두 일러둔다 4-89\n",
                            japanese = "このさきハみへてない事だん／＼と\nよろずの事をみなといてをく",
                            english = "konosaki wa miete nai koto dandan to\n" +
                                    "yorozu no koto o mina toite oku",
                            commentary = ""
                        )
                    )

                    allContent.add(
                        ContentItem(
                            korean = " 이제부터는 이 세상 창조 이래 없던 근행\n 차츰차츰 가르쳐 손짓을 익히게 한다 4-90\n",
                            japanese = "これからハこのよはじめてないつとめ\nだん／＼をしへてをつけるなり",
                            english = "korekara wa kono yo hajimete nai Tsutome\n" +
                                    "dandan oshie te o tsukeru nari",
                            commentary = "九十,이제부터는 이 세상 창조 이래 일찍이 본 적이 없는 즐거운근행을 차츰 가르치고 손짓도 익히게 하여 이것을 널리 펴나가리라."
                        )
                    )

                    allContent.add(
                        ContentItem(
                            korean = " 이 근행을 온 세상을 구제하는 길\n 벙어리도 말하게 하리라 4-91\n",
                            japanese = "このつとめせかいぢううのたすけみち\nをしでもものをゆハす事なり",
                            english = "kono Tsutome sekaiju no tasuke michi\n" +
                                    "oshi demo mono o yuwasu koto nari",
                            commentary = "九十一,즐거운근행이야말로 온 세상 사람들을 구제하는 진실한 길로서, 이 근행을 통해 벙어리도 말할 수 있도록 신기한 구제를 한다."
                        )
                    )

                    allContent.add(
                        ContentItem(
                            korean = " 나날이 근행인원 단단히 하라\n 마음을 가다듬어 빨리 손짓을 익혀라 4-92\n",
                            japanese = "にち／＼につとめのにんぢうしかとせよ\n心しづめてはやくてをつけ",
                            english = "nichinichi ni Tsutome no ninju shikato seyo\n" +
                                    "kokoro shizumete hayaku te o tsuke",
                            commentary = "九十二,나날이 근행인원들은 단단히 하라. 마음을 가다듬어 즐거운근행의 손짓을 빨리 익히도록 하라."
                        )
                    )

                    allContent.add(
                        ContentItem(
                            korean = " 이 근행 어떤 것이라 생각하는가\n 세상을 안정시켜 구제할 뿐이다 4-93\n",
                            japanese = "このつとめなにの事やとをもている\nせかいをさめてたすけばかりを",
                            english = "kono Tsutome nanino koto ya to omote iru\n" +
                                    "sekai osamete tasuke bakari o",
                            commentary = "九十三,이 즐거운근행은 무엇 때문에 하는 것이라 생각하는가. 온 세상 사람들의 마음을 깨끗이 맑혀서 구제하고 세상을 안정시키려는 일념에서 어버이신이 시작한 근행이다."
                        )
                    )

                    allContent.add(
                        ContentItem(
                            korean = " 이 길이 확실히 나타난다면\n 질병의 뿌리는 끊어져 버린다 4-94\n",
                            japanese = "このみちがたしかみゑたる事ならば\nやまいのねゑわきれてしまうで",
                            english = "kono michi ga tashika mietaru koto naraba\n" +
                                    "yamai no ne wa kirete shimau de",
                            commentary = "九十四,이 구제한줄기의 길이 빨리 세상에 나타나서 사람들이 믿게 되면, 질병의 근본이 되는 마음의 티끌이 털려 이 세상은 즐거운 삶의 세계가 된다."
                        )
                    )

                    allContent.add(
                        ContentItem(
                            korean = " 진실한 마음에 따라 누구에게나\n 어떤 수호도 안 한다고는 말하지 않아 4-95\n",
                            japanese = "しんぢつの心しだいにいづかたも\nいかなしゆごふもせんとゆハんで",
                            english = "shinjitsu no kokoro shidai ni izukata mo\n" +
                                    "ikana shugo mo sen to yuwan de",
                            commentary = "九十五,진실한 마음에 따라 누구에게나 차별없이 어떤 수호도 나타내 준다."
                        )
                    )

                    allContent.add(
                        ContentItem(
                            korean = " 지금의 길 신의 서두름 안의 사람들\n 염려할 것 없다 단단히 두고 보라 4-96\n",
                            japanese = "いまのみち神のせきこみうちなるハ\nあんぢないぞやしかとみていよ",
                            english = "ima no michi Kami no sekikomi uchi naru wa\n" +
                                    "anji nai zo ya shikato mite iyo",
                            commentary = "九十六,이 길을 빨리 세상에 펴려고 서두르고 있는 데 대해 내부 사람들은 이러니저러니하며 염려들을 하고 있으나, 결코 걱정할 것 없다. 앞으로 되어가는 모습을 단단히 두고 보라."
                        )
                    )

                    allContent.add(
                        ContentItem(
                            korean = " 이제까지와는 길이 바뀐다고 말해 두었다\n 신은 틀린 말은 하지 않아 4-97\n",
                            japanese = "これまでとみちがかわるとゆうてある\n神ハちごふた事ハゆハんで",
                            english = "koremade to michi ga kawaru to yute aru\n" +
                                    "Kami wa chigota koto wa yuwan de",
                            commentary = ""
                        )
                    )

                    allContent.add(
                        ContentItem(
                            korean = " 앞으로는 신의 서두르는 마음을\n 입으로는 아무래도 말하려야 말할 수 없다 4-98\n",
                            japanese = "このさきハ神の心のせきこみを\nくちでハどふむゆうにゆハれん",
                            english = "konosaki wa Kami no kokoro no sekikomi o\n" +
                                    "kuchi dewa domu yu ni yuwaren",
                            commentary = ""
                        )
                    )

                    allContent.add(
                        ContentItem(
                            korean = " 아무리 어려운 일이라 할지라도\n 일러주지 않고서는 알 수가 없다 4-99\n",
                            japanese = "いかほどにむつかし事とゆうたとて\nとかすにいてハわかるめハなし",
                            english = "ika hodoni mutsukashi koto to yuta tote\n" +
                                    "tokazu ni ite wa wakaru me wa nashi",
                            commentary = "九十八, 九十九,온 세상 사람들을 구제하려고 서두르는 어버이신의 마음을 말로써는 아무래도 다 설명하기가 어렵다. 그러나 아무리 어렵다 할지라도 일러주지 않으면 아무도 모를 것이다."
                        )
                    )

                    allContent.add(
                        ContentItem(
                            korean = " 나날이 신의 의도 차츰차츰\n 일러줄 테니 이것 들어 다오 4-100\n",
                            japanese = "にち／＼に神のをもわくだんだんと\nといてをくぞやこれきいてくれ",
                            english = "nichinichi ni Kami no omowaku dandan to\n" +
                                    "toite oku zo ya kore kiite kure",
                            commentary = "百,그래서 좀처럼 입으로써는 다 말할 수 없지만, 점차로 어버이신의 의도를 일러줄 테니, 이것을 듣고 모두 잘 생각하라."
                        )
                    )

                    allContent.add(
                        ContentItem(
                            korean = " 이 길은 무엇인가 어렵고 진기한\n 길인 만큼 단단히 두고 보라 4-101\n",
                            japanese = "このみちハなにかむつかしめつらしい\nみちであるぞやたしかみていよ",
                            english = "kono michi wa nanika mutsukashi mezurashii\n" +
                                    "michi de aru zo ya tashika mite iyo",
                            commentary = "百一,이 길은 쉽사리 알기 어려운 길이긴 하나 세상에 빌할 데 없는 참으로 훌룡한 가르침인 만큼, 어버이신이 일러둔 것은 반드시 사실로 나타날 것이니 장래를 단단히 두고 보라"
                        )
                    )

                    allContent.add(
                        ContentItem(
                            korean = " 이 길을 헤쳐나가면 그 다음은\n 미칠곳을 미친곳의 땅으로 하리라 4-102\n",
                            japanese = "このみちをとふりぬけたらそのさきハ\nからハにほんのぢいにしてある",
                            english = "kono michi o torinuketara sonosaki wa\n" +
                                    "kara wa nihon no jii ni shite aru",
                            commentary = "百二,이 길을 헤쳐 나가 사람들의 마음이 맑아지면, 그 다음에는 어버이신의 뜻을 아직 알지 못하는 곳에 구제한줄기의 어버이마음을 빠짐없이 전하도록 하라. 이미 신의 혜택을 풍성하게 입도록 해 두었다."
                        )
                    )

                    allContent.add(
                        ContentItem(
                            korean = " 미칠곳의 땅을 미친곳의 땅으로 하면\n 이것은 영원히 이어져 나간다 4-103\n",
                            japanese = "からのぢをにほんぢいにしたならば\nこれまつだいのいきどふりなり",
                            english = "kara no ji o nihon no jii ni shita naraba\n" +
                                    "kore matsudai no iki dori nari",
                            commentary = "百三,어버이신의 가르침이 아직 미치지 않은 곳에 빠짐억이 이 길이 전해져 온 세상 사람들이 어버이신의 구제한줄기의 마음을 깨닫게 되면, 거기서 오는 기쁨은 이 지상에 넘칠 것이고, 그것은 또한 영원히 변함없이 이어져 나가게 된다.\n제2호 34의 주석 참조."
                        )
                    )

                    allContent.add(
                        ContentItem(
                            korean = " 이 세상을 다스리는 것도 위 하늘도 위\n 윗사람과 신의 마음 구분하리라 4-104\n",
                            japanese = "このよふを納も上天もかみ\n上と神との心わけるで",
                            english = "kono yo o osameru mo kami ten mo Kami\n" +
                                    "kami to Kami to no kokoro wakeru de",
                            commentary = "百四,이 세상을 다스리는 사람도 위라 하고, 어버이신도 위라 하는데, 비록 말은 같을지라도 그 마음은 반다시 같다고 할 수 없다. 그것은 세상을 다스리는 사람의 마음이나 어버이신의 마음이 사람들의 행복을 염원하는 점에 있어서는 같지만, 장래를 내다보고 못 보는 점에 있어서는 다르기 때문이다. 이 점을 분명히 구분하겠다."
                        )
                    )

                    allContent.add(
                        ContentItem(
                            korean = " 차츰차츰 보이지 않는 것을 말해 두고서\n 장차 보이면 이것이 신이야 4-105\n",
                            japanese = "だん／＼とみゑん事をばゆてをいて\nさきでみゑたらこれが神やで",
                            english = "dandan to mien koto oba yute oite\n" +
                                    "saki de mietara kore ga Kami ya de",
                            commentary = "百五,현재로는 사람들의 눈에 보이지 않는 것을 차차로 말해 두고서, 그것이 장래에 실현된다면 이것이 곧 어버이신의 이야기인 증거이다."
                        )
                    )

                    allContent.add(
                        ContentItem(
                            korean = " 아무리 보이는 것을 말할지라도\n 장차 안 보이면 안다고 할 수 없어 4-106\n",
                            japanese = "いかほどにみゑたる事をゆうたとて\nさきでみゑねはわかりあるまい",
                            english = "ika hodoni mietaru koto o yuta tote\n" +
                                    "saki de mieneba wakari arumai",
                            commentary = "百六,아무리 현재의 일을 자신 있게 말할지라도, 장래에 말한 바가 그대로 나타나지 않는다면 참으로 만사를 알 고 있다고는 할 수 없다."
                        )
                    )

                    allContent.add(
                        ContentItem(
                            korean = " 이제부터는 온 세상 사람들의 가슴속\n 위나 아래나 모두 깨닫게 할 테다 4-107\n",
                            japanese = "これからハせかいぢううのむねのうち\n上下ともにわけてみせるで",
                            english = "korekara wa sekaiju no mune no uchi\n" +
                                    "kami shimo tomoni wakete miseru de",
                            commentary = "百七,이제부터는 윗사람이나 아랫사람이나 차별 없이 시비선악(是非善惡)을 밝혀서 어버이신의 뜻을 깨닫도록 할 것이다."
                        )
                    )

                    allContent.add(
                        ContentItem(
                            korean = " 이것을 보라 세상이나 안이나 차별 없다\n 가슴속부터 청소할 거야 4-108\n",
                            japanese = "これをみよせかいもうちもへたてない\nむねのうちよりそふぢするぞや",
                            english = "kore o miyo sekai mo uchi mo hedate nai\n" +
                                    "mune no uchi yori soji suru zo ya",
                            commentary = "百八,이를 위해서 세상 사람들이나 내부 사람들이나 차별 없이 모두 깨끗이 마음을 청소하게 할 테니 잘 두고 보라."
                        )
                    )

                    allContent.add(
                        ContentItem(
                            korean = " 이 청소 어려운 일이지만\n 질병이라는 것은 없다고 말해 둔다 4-109\n",
                            japanese = "このそふぢむつかし事であるけれど\nやまいとゆうわないとゆてをく",
                            english = "kono soji mutsukashi koto de aru keredo\n" +
                                    "yamai to yu wa nai to yute oku",
                            commentary = "百九,이처럼 온 세상 사람들의 마음을 청소하기는 어려운 일이지만, 그러나 신상을 통해 그것을 해 보이겠다. 그러므로 신상이 나타나더라도 그것은 질병이 아니라 어버이신의 인도이며 꾸지람이란 것을 미리 일러둔다."
                        )
                    )

                    allContent.add(
                        ContentItem(
                            korean = " 어떤 아픔도 괴로움도 부스럼도\n 열도 설사도 모두가 티끌이니라 4-110\n",
                            japanese = "どのよふないたみなやみもでけものや\nねつもくだりもみなほこりやで",
                            english = "dono yona itami nayami mo dekemono ya\n" +
                                    "netsu mo kudari mo mina hokori ya de",
                            commentary = "百十,어떤 아픔도, 괴로움도, 부스럼도, 열도, 설사도 모두 질병이 아니라 마음의 티끌이다."
                        )
                    )

                    allContent.add(
                        ContentItem(
                            korean = " 이 세상을 창조한 이래 아무것도\n 윗사람에게 가르친 일은 없으리라 4-111\n",
                            japanese = "このよふを初てからになにもかも\n上ゑをしへた事ハあるまい",
                            english = "kono yo o hajimete karani nanimo kamo\n" +
                                    "kami e oshieta koto wa arumai",
                            commentary = "百十一,태초에 어버이신이 없던 세계 없던 인간을 창조한 이래, 지금까지 윗사람들에게 어버이신의 의도를 일러준 일이 없다."
                        )
                    )

                    allContent.add(
                        ContentItem(
                            korean = " 이번에는 무엇이든 만가지를 윗사람에게\n 알려 주게 되면 4-112\n",
                            japanese = "このたびハなにかよろづを上たるゑ\nしらしてをいた事であるなら",
                            english = "konotabi wa nanika yorozu o kami taru e\n" +
                                    "shirashite oita koto de aru nara",
                            commentary = ""
                        )
                    )

                    allContent.add(
                        ContentItem(
                            korean = " 그러면 더러는 생각하는 사람도 있겠지\n 모두 모여들어 이야기하면 4-113\n",
                            japanese = "それからハなかにハしやんするもあろ\nみなよりよふてはなししたなら",
                            english = "sorekara wa naka niwa shiyan suru mo aro\n" +
                                    "mina yoriyote hanashi shita nara",
                            commentary = ""
                        )
                    )

                    allContent.add(
                        ContentItem(
                            korean = " 그중에는 진실로 마음 든든하다고\n 생각하는 사람도 있을 거야 4-114\n",
                            japanese = "そのなかにしんぢつ心たのもしい\nをもてしやんをするものもある",
                            english = "sono naka ni shinjitsu kokoro tanomoshii\n" +
                                    "omote shiyan o suru mono mo aru",
                            commentary = "百十二～百十四,그러나 이제는 이미 시기가 다가왔으므로 인간창조 이래의 어버이신의 의도를 모두 윗사람들에게 가르쳐 주려고 한다. 그러면 개중에는 이에 대해 깊이 생각해 보는 사람도 있을 것이고, 또 여러 사람이 모여 와서 서로 이야기를 하다 보면 그 가운데는 과연 이 길은 진실로 믿음직한 가르침이라고 깨닫는 사람도 있을 것이다."
                        )
                    )

                    allContent.add(
                        ContentItem(
                            korean = " 이 길이 윗사람에게 통하게 된다면\n 신의 자유자재 곧 나타내리라 4-115\n",
                            japanese = "このみちを上ゑとふりた事ならば\n神のぢうよふすぐにあらわす",
                            english = "kono michi o kami e torita koto naraba\n" +
                                    "Kami no juyo suguni arawasu",
                            commentary = "百十五,이 길이 윗사람들에게 이해되기만 한다면 어버이신은 자유자재한 섭리를 즉시 나타내 보이겠다."
                        )
                    )

                    allContent.add(
                        ContentItem(
                            korean = " 이 세상을 창조한 신의 자유자재를\n 보여 준 일은 전혀 없으므로 4-116\n",
                            japanese = "このよふを初た神のぢうよふを\nみせたる事ハさらにないので",
                            english = "kono yo o hajimeta Kami no juyo o\n" +
                                    "misetaru koto wa sarani nai node",
                            commentary = ""
                        )
                    )

                    allContent.add(
                        ContentItem(
                            korean = " 무엇이든 모르는 동안은 그대로야\n 신의 자유자재 알린다면 4-117\n",
                            japanese = "なにゝてもしらんあいだハそのまゝや\n神のぢうよふしらしたるなら",
                            english = "kono yo o hajimeta Kami no juyo o\n" +
                                    "misetaru koto wa sarani nai node",
                            commentary = ""
                        )
                    )

                    allContent.add(
                        ContentItem(
                            korean = " 이것을 듣고 모든 사람든은 생각하라\n 무엇이든 만가지는 마음 나름이야 4-118\n",
                            japanese = "これきいてみな一れつわしやんせよ\nなにかよろつハ心しだいや",
                            english = "kore kiite mina ichiretsu wa shiyan seyo\n" +
                                    "nanika yorozu wa kokoro shidai ya",
                            commentary = "百十六～百十八,이 세상을 창조한 어버이신의 자유자재한 섭리를 지금까지 알린 적도 보인 적도 전혀 없으므로, 사람들이 모르는 것도 무리는 아니다. 그러므로 모르고 있는 동안은 그런대로 좋지만, 일단 어버이신이 이 세상에 나타나 자유자재한 섭리를 해 보일 때는 그것을 보고 세상 사람들은 모두 깊이 생각해야 한다. 신상이나 사정 등, 만사는 모두 각자의 마음가짐에 따른 어버이신의 수호이다."
                        )
                    )

                    allContent.add(
                        ContentItem(
                            korean = " 오늘은 아무것도 보이지 않지만\n 6월에 가서 보라 모두 나타나리라 4-119\n",
                            japanese = "けふの日ハなにがみへるやないけれど\n六月をみよみなでかけるで",
                            english = "kyonohi wa nani ga mieru ya nai keredo\n" +
                                    "roku gatsu o miyo mina dekakeru de",
                            commentary = "百十九,지금으로서는 아무것도 보이지 않지만 6월에 가서 보라. 진기한 일이 나타날 것이다.\n 1874년부터 6월부터 증거수호부를 내려주셨다.(제4호 5의 주석 참조)"
                        )
                    )

                    allContent.add(
                        ContentItem(
                            korean = " 지금까지는 높은산이라 뽐내고 있다\n 골짜기에서는 위축되어 있을 뿐 4-120\n",
                            japanese = "いまゝでハ高い山やとゆうている\nたにそこにてハしけんばかりを",
                            english = "imamade wa takai yama ya to yute iru\n" +
                                    "tanisoko nitewa shiken bakari o",
                            commentary = "百二十,지금까지 상류층 사람들은 제멋대로 뽐내고 있고, 하류층 사람들은 짓눌려서 위축되어 있다."
                        )
                    )

                    allContent.add(
                        ContentItem(
                            korean = " 이제부터는 높은산이나 골짜기나\n 태초의 이야기를 일러주리라 4-121\n",
                            japanese = "これからわ高山にてもたにそこも\nもとはじまりをゆうてきかする",
                            english = "korekara wa takayama nitemo tanisoko mo\n" +
                                    "moto hajimari o yute kikasuru",
                            commentary = "百二一,자, 이제부터는 상류층 사람들에게도 하류층 사람들에게도 다같이 태초의 이야기를 확실히 일러주겠다.\n 태초에 창조시에는 모든 인간이 차별 없이 다 똑같았지만, 나중에 상류층 사람으로 태어나거나 하류층 사람으로 태어나게 된 것은 모두 각자의 전생 인연 때문이다."
                        )
                    )

                    allContent.add(
                        ContentItem(
                            korean = " 이 세상을 창조한 태초는 진흙바다\n 그 가운데는 미꾸라지뿐이었다 4-122\n",
                            japanese = "このよふのはぢまりだしハとろのうみ\nそのなかよりもどちよばかりや",
                            english = "kono yo no hajimari dashi wa doro no umi\n" +
                                    "sono naka yori mo dojo bakari ya",
                            commentary = ""
                        )
                    )

                    allContent.add(
                        ContentItem(
                            korean = " 이 미꾸라지를 무엇이라 생각하는가\n 이것이 인간의 씨앗인 거야 4-123\n",
                            japanese = "このどぢよなにの事やとをもている\nこれにんけんのたねであるそや",
                            english = "kono dojo nanino koto ya to omote iru\n" +
                                    "kore ningen no tane de aru zo ya",
                            commentary = ""
                        )
                    )

                    allContent.add(
                        ContentItem(
                            korean = " 이것을 신이 끌어올려 먹고 나서\n 차츰차츰 수호하여 인간으로 삼아 4-124\n",
                            japanese = "このものを神がひきあけくてしもて\nだん／＼しゆごふにんけんとなし",
                            english = "kono mono o Kami ga hikiage kute shimote\n" +
                                    "dandan shugo ningen to nashi",
                            commentary = ""
                        )
                    )

                    allContent.add(
                        ContentItem(
                            korean = " 그로부터 신의 수호는\n 여간한 것이 아니었던 거야 4-125\n",
                            japanese = "それよりも神のしゆことゆうものわ\nなみたいていな事でないぞや",
                            english = "soreyori mo Kami no shugo to yu mono wa\n" +
                                    "nami taitei na koto de nai zo ya",
                            commentary = "百二二～百二五,이 세상 태초는 진흙바다와 같은 혼돈한 상태였으며, 그 가운데 미꾸라지가 많이 살고 있었다. 어버이신은 이 미꾸라지를 끌어올려 모두 먹고 그 마음씨를 알아본 다음, 이것들을 씨앗으로 삼아 차츰 수호하여 인간을 창조했다. 그러므로 오늘날과 같은 인간으로 키우기까지 어버이신의 고심은 참으로 여간한 것이 아니었다.\n 이 일련의 노래의 주요한 뜻은 신님의 은혜의 위대함과 세상 사람들은 모두 형제 자매 임을 가르쳐 주신 것이다.(第六호 二十九～五十一 참조)"
                        )
                    )

                    allContent.add(
                        ContentItem(
                            korean = " 이 이야기를 예삿일로 생각 말라\n 온 세상 사람들을 구제하고 싶어서 4-126\n",
                            japanese = "このはなし一寸の事やとをもうなよ\nせかい一れつたすけたいから",
                            english = "kono hanashi chotto no koto ya to omouna yo\n" +
                                    "sekai ichiretsu tasuketai kara",
                            commentary = "百二六,인간창조에 관한 이 이야기를 대수롭지 않게 생각해서는 안된다. 온 세상 자녀들을 구제하려는 어버이신의 의도를 전하는 것이므로, 잘 들어 명심해 두지 않으면 안된다.\n 百二二～百二六 : 태초이야기입니다."
                        )
                    )

                    allContent.add(
                        ContentItem(
                            korean = " 나날이 신의 마음의 진실은\n 깊은 의도가 있다고 생각하라 4-127\n",
                            japanese = "にち／＼に神の心のしんぢつわ\nふかいをもわくあるとをもへよ",
                            english = "nichinichi ni Kami no kokoro no shinjitsu wa\n" +
                                    "fukai omowaku aru to omoe yo",
                            commentary = ""
                        )
                    )

                    allContent.add(
                        ContentItem(
                            korean = " 지금까지는 아는자가 모르는자에 이끌려\n 시달려 온 것이 신의 섭섭함 4-128\n",
                            japanese = "いまゝでハにほんかからにしたごふて\nまゝにしられた神のざんねん",
                            english = "imamade wa nihon ga kara ni shitagote\n" +
                                    "mamani shirareta Kami no zannen",
                            commentary = "百二八,지금까지는 어버이신의 가르침이 아직 어버이신의 뜻을 전혀 모르는 사람들의 인간생각에 의해 무시당해 왔는데, 이것이 어버이신으로서는 참으로 안타까운 일이다."
                        )
                    )

                    allContent.add(
                        ContentItem(
                            korean = " 이 갚음 신의 섭리 이것을 보라\n 어떤 자도 흉내를 못 내리라 4-129\n",
                            japanese = "このかやし神のはたらきこれをみよ\nいかなものでもまねわでけまい",
                            english = "kono kayashi Kami no hataraki kore o miyo\n" +
                                    "ikana mono demo mane wa dekemai",
                            commentary = "百二九,이에 대한 갚음은 어버이신이 섭리로써 할 것이니 잘 두고 보라. 이것은 인간 힘으로써는 흉내내지 못하리라."
                        )
                    )

                    allContent.add(
                        ContentItem(
                            korean = " 아무리 힘센 자라 할지라도\n 신이 물러나면 이에는 당하지 못해 4-130\n",
                            japanese = "いかほどのごふてきたるとゆうたとて\n神がしりぞくこれかないまい",
                            english = "ika hodono goteki taru to yuta tote\n" +
                                    "Kami ga shirizoku kore kanaimai",
                            commentary = "百三十,아무리 힘센 자라도 만약 몸에 어버이신의 수호가 사라진다면, 힘을 쓰는 것은 물론 움직이기조차 할 수 없게 될 것이다."
                        )
                    )

                    allContent.add(
                        ContentItem(
                            korean = " 무엇이든 모든 사람들은 이와 같이\n 신이 자요자재로 한다고 생각하라 4-131\n",
                            japanese = "なにゝてもみな一れつハこのどふり\n神がぢうよふするとをもゑよ",
                            english = "nani nitemo mina ichiretsu wa kono dori\n" +
                                    "Kami ga juyo suru to omoe yo",
                            commentary = "百三一,온 세상 사랑들은 무슨 일이든지 모두 이와 갈이 어버이신이 자유자재로 수호하고 있음을 잘 깨달아야 한다."
                        )
                    )

                    allContent.add(
                        ContentItem(
                            korean = " 생각하라 젊은이 노인 약한 자라도\n 마음에 따라 어떤 자유자재도 4-132\n",
                            japanese = "しやんせよハかいとしよりよハきでも\n心しだいにいかなぢうよふ",
                            english = "shiyan seyo wakai toshiyori yowaki demo\n" +
                                    "kokoro shidai ni ikana juyo",
                            commentary = "百三二,어버이신의 자유자재한 섭리를 깊이 인삭하라. 젊은이나 노인이나, 또 아무리 약한자라도 각자의 마음가짐에 따라 어떤 수호도 해 줄 것이다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 지금까지도 한결같이 살아왔으나\n 신의 자유자재 아는 자 없다 4-133\n",
                            japanese = "いまゝでもをなぢくらしていたるとも\n神のぢうよふしりたものなし",
                            english = "imamade mo onaji kurashite itaru tomo\n" +
                                    "Kami no juyo shirita mono nashi",
                            commentary = "百三三,이 가르침을 시작하기 전에도 똑같이 어버이신의 수호로 살아왔으나, 일러준 일이 없었기 때문에 누구도 어버이신의 자유자재한 섭리를 아는 자가 없었다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이제부터은 만가지를 모두 일러줄 테니\n 마음 틀리지 않도록 하라 4-134\n",
                            japanese = "これからハよろづの事をみなとくで\n心ちがいのないよふにせよ",
                            english = "korekara wa yorozu no koto o mina tokude\n" +
                                    "kokoro chigai no nai yoni seyo",
                            commentary = "百三四,어버이신이 이 세상에 나타난 이상, 만가지 리를 일러줄 테니, 모두들은 마음가짐이 틀리지 않도록 하라."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 지금까지는 우마란 말 가끔 있었지만\n 전후를 아는 자는 없으리라 5-1\n",
                            japanese = "いまゝでハぎうばとゆうハまゝあれど\nあとさきしれた事ハあるまい",
                            english = "imamade wa gyuba to yu wa mama aredo\n" +
                                    "ato saki shireta koto wa arumai",
                            commentary = "一,세상에서는 흔히 우마의 길로 떨어진다고 말하고들 있으나, 어떤 사람이 우마의 길로 떨어지는지, 그리고 어떻게 하면 우마의 길에서 구제되는지를 지금까지는 분명하게 일러준 일이 없으므로 아무도 모를 것이다.\n주(註) 우마란 우마와 같은 생활, 즉 축생도(畜生道)란 뜻."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이번에는 앞일을 이 세상에서 알려 둘 테니 몸의 장애를 보라 5-2\n",
                            japanese = "このたびハさきなる事を此よから\nしらしてをくでみにさハりみよ",
                            english = "konotabi wa saki naru koto o kono yo kara\n" +
                                    "shirashite oku de mi ni sawari miyo",
                            commentary = "二,이번에는 몸에 장애를 주어서 내생의 일을 금생에 미리 알려 줄 것이니 그것을 보고 잘 반성하라."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이 세상은 아무리 제 몸 생각해도\n 신의 노여움에는 당할 수 없다 5-3\n",
                            japanese = "このよふハいかほどハがみをもふても\n神のりいふくこれハかなハん",
                            english = "kono yo wa ika hodo waga mi omotemo\n" +
                                    "Kami no rippuku kore wa kanawan",
                            commentary = "三, 이 세상은 어버이신이 다스리기 때문에, 아무리 제 몸을 생각하며 자기 이익만을 꾀한다 하더라도 일단 어버이신의 노여움이 나타나면 어찌할 도리가 없다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 각자 제 몸 생각은 쓸데없어\n 신이 각각 분간할 거야 5-4\n",
                            japanese = "めへ／＼にハがみしやんハいらんもの\n神がそれ／＼みわけするぞや",
                            english = "meme ni waga mi shiyan wa iran mono\n" +
                                    "Kami ga sorezore miwake suru zo ya",
                            commentary = "一～四,이상 4수의 노래는 당시 터전 이웃 마을에 살고 있던 어떤 여자를 실례로 들어 일러주신 것이라 전해지고 있다. 그 여자는 간약(奸惡)한 성질을 지닌 사람으로서, 교조님으로부터 여러 가지로 은혜를 입은 터에 집터 앞을 지나 다니면서도 한번 들러 인사조차 하는 일이 없었다. 그런 여자인지라 다른 사람에 대해서도 매정하고 무자비했다. 교조님은 언제나 곁의 사람들에게\n" + "\"은혜를 모르는 자는 우마의 길로 떨어진다.\"\n" + "\"소와 같은 신세가 된다.\"\n" + "고 말씀하셨다. 과연 그 여자는 1874년부터 걸을 수 없게 되어, 20여 년간이나 앉은뱅이로 가족들의 신세를 지다가 이 세상을 떠나고 말았다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 한 집안에 같이 살고 있는 가운데도\n 신도 부처도 있다고 생각하라 5-5\n",
                            japanese = "一やしきをなじくらしているうちに\n神もほとけもあるとをもへよ",
                            english = "hito yashiki onaji kurashite iru uchi ni\n" +
                                    "Kami mo hotoke mo aru to omoe yo",
                            commentary = "五,한 지붕 아래 같이 살고 있는 사람이라도 그 마음은 제각기 달라서 같지가 않다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이것을 보고 어떤 자도 납득하라\n 선과 악을 구분해 보일 테다 5-6\n",
                            japanese = "これをみていかなものでもとくしんせ\n善とあくとをわけてみせるで",
                            english = "kore o mite ikana mono demo tokushin se\n" +
                                    "zen to aku to o wakete miseru de",
                            commentary = "六,각자의 마음쓰기에 때라 선과 악으로 구분해서 보일테니, 몸의 장애를 보고 모두들은 깨달아라."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이 이야기를 모든 사람들은 생각하라\n 같은 마음은 전혀 없으리라 5-7\n",
                            japanese = "このはなしみな一れつハしやんせよ\nをなじ心わさらにあるまい",
                            english = "kore o mite ikana mono demo tokushin se\n" +
                                    "zen to aku to o wakete miseru de",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 부모자식간 부부간 형제간이라도\n 모두 각각으로 마음 다른 거 5-8\n",
                            japanese = "をやこでもふう／＼のなかもきよたいも\nみなめへ／＼に心ちがうで",
                            english = "kore o mite ikana mono demo tokushin se\n" +
                                    "zen to aku to o wakete miseru de",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 온 세상 어디 누구라고는 말하지 않아\n 마음의 티끌이 몸의 장애가 된다 5-9\n",
                            japanese = "せかいぢうどこのものとハゆハんでな\n心のほこりみにさハりつく",
                            english = "sekaiju doko no mono towa yuwan de na\n" +
                                    "kokoro no hokori mi ni sawari tsuku",
                            commentary = "九,이 세상 어느 누구라도 자신이 마음에 쌓아 온 티끌은 반드시 어버이신의 손질을 받아 몸의 장애로 나타나게 된다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 제 몸의 괴로움을 생각하여\n 신에게 의탁하는 마음을 정하라 5-10\n",
                            japanese = "みのうちのなやむ事をばしやんして\n神にもたれる心しやんせ",
                            english = "minouchi no nayamu koto oba shiyan shite\n" +
                                    "Kami ni motareru kokoro shiyan se",
                            commentary = "十,몸의 장애는 어버이신의 인도이므로 어버이신에게 의탁하여 그 뜻을 따르려는 마음 작정을 하라"
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 아무리 어려운 것이라 할지라도\n 신의 자유자재 빨리 보이고 싶다 5-11\n",
                            japanese = "どのよふなむつかし事とゆうたとて\n神のぢうよふはやくみせたい",
                            english = "dono yona mutsukashi koto to yuta tote\n" +
                                    "Kami no juyo hayaku misetai",
                            commentary = "十一,아무리 중한 신상, 괴로운 사정이라도 마음에 따라 어버이신의 자유자재한 섭리로 빨리 구제하고 싶다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 지금까지는 신의 자유자재 진실을\n 아는 자는 전혀 없었으므로 5-12\n",
                            japanese = "いまゝでハ神のぢうよふしんぢつを\nしりたるものさらにないので",
                            english = "imamade wa Kami no juyo shinjitsu o\n" +
                                    "shiritaru mono wa sarani nai node",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이제부터는 아무리 어려운 질병도\n 마음에 따라 낫지 않는 것이 없다 5-13\n",
                            japanese = "これからハいかなむつかしやまいでも\n心したいになをらんでなし",
                            english = "korekara wa ikana mutsukashi yamai demo\n" +
                                    "kokoro shidai ni naorande nashi",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 진실한 마음을 신이 받아들이면\n 어떤 자유자재도 보여 줄 테다 5-14\n",
                            japanese = "しんぢつの心を神がうけとれば\nいかなぢうよふしてみせるてな",
                            english = "shinjitsu no kokoro o Kami ga uketoreba\n" +
                                    "ikana juyo shite miseru de na",
                            commentary = "十四,인간이 진실한 마음으로 어버이신에게 의탁하여 리(理)를 소중히 받들면서 구제를 원하면, 그 마음을 살펴보아 어떤 수호라도 자유자재로 해 보일 테다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이토록 신의 진실을 나타내는 이 이야기\n 곁의 사람들은 어서 깨달아라 5-15\n",
                            japanese = "こらほどの神のしんぢつこのはなし\nそばなるものハはやくさとれよ",
                            english = "kora hodono Kami no shinjitsu kono hanashi\n" +
                                    "soba naru mono wa hayaku satore yo",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이것만 빨리 깨닫게 되면\n 무슨 일이든 모두 이와 같으리라 5-16\n",
                            japanese = "これさいかはやくさとりがついたなら\nなにゝついてもみなこのどふり",
                            english = "kore saika hayaku satori ga tsuita nara\n" +
                                    "nani ni tsuitemo mina kono dori",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 지금까지는 무슨 일이든 서두르지 않았지만\n 이제 서두른다 한길을 5-17\n",
                            japanese = "けふまでハなによの事もせかねとも\nもふせきこむでをふくハんのみち",
                            english = "kyomade wa nani yono koto mo sekane domo\n" +
                                    "mo sekikomu de okwan no michi",
                            commentary = "十七,지금까지는 어떤 일도 그렇게 서두르지 않았으나, 이제는 시기가 다가왔기 때문에 드디어 한길로 나가도록 서두른다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이 길을 세상 보통 일로 생각 말라\n 이것은 영원한 고오끼의 시작 5-18\n",
                            japanese = "このみちハせかいなみとハをもうなよ\nこれまつだいのこふきはぢまり",
                            english = "kono michi wa sekainami towa omouna yo\n" +
                                    "kore matsudai no Koki hajimari",
                            commentary = "十八,이 길은 지금까지 흔히 있던 가르침과 같은 것으로 생각 말라, 이 길은 후세에 길이 전해 갈 구제한줄기의 길의 시작이다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이 인원 어디 있다고는 말하지 않아\n 몸의 장애로 모두 오게 되리라 5-19\n",
                            japanese = "このにんぢうとこにあるとハゆハんでな\nみのうちさハりみなくるであろ",
                            english = "kono ninju doko ni aru towa yuwan de na\n" +
                                    "minouchi sawari mina kuru de aro",
                            commentary = "十九,이 근행인원은 어디에 있는 누구라고는 말하지 않으나, 모두 각각 몸의 장애로 터전에 이끌려 올 것이다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이 장애는 인도로도 훈계로도 노여움으로도\n 모두들 각자 생각해 보라 5-20\n",
                            japanese = "このさハりてびきいけんもりいふくも\nみなめへ／＼にしやんしてみよ",
                            english = "kono sawari tebiki iken mo rippuku mo\n" +
                                    "mina meme ni shiyan shite miyo",
                            commentary = "二十,몸의 장애라 할지라도 거기에는 어버이신이 이 길로 이끌어 들이기 위한 인도도 있고, 마음을 잘못 쓴 데 대한 훈계도 있고, 또 노여움도 있으므로 모두 각자 잘 생각해야 한다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이 이야기 어떻게 생각하고 듣고 있는가\n 귀여운 나머지 타이르는 거야 5-21\n",
                            japanese = "このはなしなんとをもふてきいている\nかハいあまりてくどく事なり",
                            english = "kono hanashi nanto omote kiite iru\n" +
                                    "kawai amarite kudoku koto nari",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 어떤 훈계 노여움이라 할지라도\n 이것을 구제 않겠다고는 결코 말하지 않아 5-22\n",
                            japanese = "どのよふにいけんりいふくゆうたとて\nこれたすけんとさらにゆハんで",
                            english = "dono yoni iken rippuku yuta tote\n" +
                                    "kore tasuken to sarani yuwan de",
                            commentary = "二十二,어떤 훈계를 받고 노여움을 사더라도 결코 낙심해서는 안된다. 어버이신은 어떤 난병이라도 구제하지 않겠다고는 말하지 않는다. 모두들을 구제해 주고 싶기 때문에 서두르는 것이다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 인간의 제 자식 훈계를 생각해 보라\n 화를 내는 것도 귀엽기 때문에 5-23\n",
                            japanese = "にんけんのハがこのいけんをもてみよ\nはらのたつのもかハいゆへから",
                            english = "ningen no waga ko no iken omote miyo\n" +
                                    "hara no tatsu no mo kawai yue kara",
                            commentary = "二十三,어버이신의 가르침은 인간이 제 자식에 대해 훈계를 하는 것과 같은 것이다. 부모가 화를 내는 것은 자식이 귀여운 나머지 장래를 영려하기 때문이지, 결코 미워서 내는 것이 아니다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 잘 생각하여 마음을 작정하고 따라오라\n 장차는 믿음직한 길이 있는 거야 5-24\n",
                            japanese = "しやんして心さためてついてこい\nすゑハたのもしみちがあるぞや",
                            english = "shiyan shite kokoro sadamete tsuitekoi\n" +
                                    "sue wa tanomoshi michi ga aru zo ya",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 지금까지는 마음에 잘못이 있더라도\n 날이 오지 않아서 묵과해 두었다 5-25\n",
                            japanese = "いまゝでハ心ちがいわありたとて\nひがきたらんてみゆるしていた",
                            english = "imamade wa kokoro chigai wa arita tote\n" +
                                    "hi ga kitarande miyurushite ita",
                            commentary = "二十五,지금까지도 인간들의 마음에 잘못이 없었다고는 할 수 없으나, 이 가르침을 일러주면 알 수 있을 만큼 아직 성인되지 않았을 뿐더라, 그 시기도 오지 않았기 때문에 지금까지 그대로 보고만 있었다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이번에는 어쨓든 가슴속\n 청소를 할 것이니 모두들 알아차려라 5-26\n",
                            japanese = "このたびはなんでもかでもむねのうち\nそうちをするでみなしよちせよ",
                            english = "konotabi wa nandemo kademo mune no uchi\n" +
                                    "soji o suru de mina shochi seyo",
                            commentary = "二十六,그러나 이번에는 어쩠든 인간들의 마음을 구석구석 청소할 것이니, 모두들은 이것을 잘 알아차려라."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 가슴속을 청소한다고 하는 것도\n 신의 의도가 깊기 때문에 5-27\n",
                            japanese = "むねのうちそふぢをするとゆうのもな\n神のおもハくふかくあるから",
                            english = "mune no uchi soji o suru to yu no mo na\n" +
                                    "Kami no omowaku fukaku aru kara",
                            commentary = "二十七,인간들의 마음을 청소하는 것도 어버이신의 깊은 의도가 있기 때문이다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이 청소 깨끗하게 하지 않는 한\n 가슴속의 진실을 알 수 없으므로 5-28\n",
                            japanese = "このそふぢすきやかしたてせん事に\nむねのしんぢつわかりないから",
                            english = "kono soji sukiyaka shitate sen koto ni\n" +
                                    "mune no shinjitsu wakari nai kara",
                            commentary = "二十八,마음을 청소하여 티끌을 깨끗이 털어내지 않으면, 흐린 마음으로는 어버이신의 마음의 진실을 알 수 없기 때문이다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이 마음을 진실로 깨닫게 되면\n 이 세상 창조의 손짓을 가르친다 5-29\n",
                            japanese = "この心しんからわかりついたなら\nこのよはぢまりてをつけるなり",
                            english = "kono kokoro shin kara wakari tsuita nara\n" +
                                    "kono yo hajimari te o tsukeru nari",
                            commentary = "二十九,어버이신의 마음을 진실로 깨닫게 되면, 이 세상 창조의 리를 나타내는 감로대 근행의 손짓을 가르친다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 지름길도 욕심도 교만도 없도록 \n 오직 본길로만 나오라 5-30\n",
                            japanese = "ちかみちもよくもこふまんないように\nたゞ一すぢのほんみちにでよ",
                            english = "chikamichi mo yoku mo koman nai yoni\n" +
                                    "tada hitosuji no honmichi ni deyo",
                            commentary = "三十,성과를 빨리 보려는 조급한 마음에서 쓸데없이 지름길을 찾거나, 이익에 눈이 어두운 나머지 욕심을 부리거나 또는 교만한 마음을 가져서는 결코 참된 한길로 나갈 수 없으므로, 이같은 잘못된 마음에 빠지지 말고 오직 본길로만 나아가야 한다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이 길을 따라오게 되면 언제까지나\n 이것이 터전의 고오끼가 되는거야 5-31\n",
                            japanese = "このみちについたるならばいつまても\nこれにいほんのこふきなるのや",
                            english = "kono michi ni tsuitaru naraba itsu made mo\n" +
                                    "kore nippon no Koki naru no ya",
                            commentary = "三十一,이렇게 해서 구제한줄기의 길로 나가게 되면, 이것은 어비이신이 터전에 나타나 가르친 진실한 길로 후세에 영원히 전해지면서, 많은 사람들을 구제하는 참된 모본의 길이 되는 것이다.\n 고오끼란 후세에 길이 전해져서 많은 사람들을 구제하게 될 근본되는 진실한 가르침이란 뜻."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 터전에도 고오끼가 이루어지면\n 어떻든 미칠곳을 마음대로 하리라 5-32\n",
                            japanese = "にほんにもこふきがでけた事ならば\nなんでもからをまゝにするなり",
                            english = "nihon nimo Koki ga deketa koto naraba\n" +
                                    "nandemo kara o mamani suru nari",
                            commentary = "三十二,인간을 창조한 터전에 세계 인류를 구제할 고오끼가 이루어지면, 다음에는 아직 어버이신의 가르침이 전해지지 않은 곳에도 점차로 어버이신의 뜻을 펴서 의도대로 풍성한 신의 혜택을 입도록 하겠다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이 세상 창조 이래의 진실을\n 아직 지금까지는 말한 바 없다 5-33\n",
                            japanese = "このよふをはぢめてからのしんぢつを\nまたいまゝでハゆうた事なし",
                            english = "kono yo o hajimete kara no shinjitsu o\n" +
                                    "mada imamade wa yuta koto nashi",
                            commentary = "三十三,이 세상을 창조한 이래로 지금까지 어버이신이 기울인 마음의 진실을 아직 누구에게도 일러준 바가 없었다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이 이야기 어려운 것이지만\n 말하지 않으면 아무도 모르리라 5-34\n",
                            japanese = "このはなしむつかし事であるけれど\nゆハずにいればたれもしらんで",
                            english = "kono hanashi mutsukashi koto de aru keredo\n" +
                                    "yuwazu ni ireba tare mo shiran de",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 차츰차츰 무슨 일이든 일러준다\n 마음을 가다듬어 단단히 들어라 5-35\n",
                            japanese = "たん／＼とどのよな事もゆてきかす\n心しずめてしかときくなり",
                            english = "dandan to dono yona koto mo yute kikasu\n" +
                                    "kokoro shizumete shikato kiku nari",
                            commentary = "三十五,인간들이 성인되어감에 따라서 무엇이든 일러줄 터이니, 마음을 가다듬어 단단히 듣도록 하라."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 지금까지는 여러가지 술범이 있었지만\n 이제부터 술법 따위는 효험 없어 5-36\n",
                            japanese = "いまゝでハいかなるほふとゆうたとて\nもふこれからハほふハきかんで",
                            english = "imanade wa ika naru ho to yuta tote\n" +
                                    "mo korekara wa ho wa kikande",
                            commentary = "三十六,지금까지는 세상에서 여러 가지 법(法)이나 술(術) 등이 행해지고 있었지만, 이제부터 그런 것은 효험이 없어진다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이제까지 변두리에서는 술법이랍시고\n 가르치고 있지만 앞으로 두고 보라 5-37\n",
                            japanese = "これまてハゑださきにてわほふなぞと\nをしへてあれどさきをみていよ",
                            english = "koremade wa edasaki nitewa ho nazo to\n" +
                                    "oshiete aredo saki o mite iyo",
                            commentary = "三十七,이제까지 변두리에서는 법이나 술 등을 가르치고 있었지만, 앞으로 그런 것들이 얼마만한 힘을 나타내는가 두고 보라.\n 변두리란 앞으로 어버이신의 진실한 가르침이 미칠 곳을 말한다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 미친곳은 지금까지 아무것도 몰랐지만\n 이제부터 장래를 낙으로 삼아라 5-38\n",
                            japanese = "にほんにハいまゝでなにもしらいでも\nこれからさきのみちをたのしゆめ",
                            english = "nihon niwa imamade nanimo shirai demo\n" +
                                    "korekara saki no michi o tanoshume",
                            commentary = "三十八,법이나 술 등은 전혀 몰라도, 터전에는 어버이신의 진실한 가르침이 있어 이것으로 번영해 갈 것이니 장래를 낙으로 삼도록 하라."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 술법이라 해서 다른 누구 한다고 생각 말라\n 이 세상 창조한 신이 하는 일 5-39\n",
                            japanese = "ほふやとてたれがするとハをもうなよ\nこのよ初た神のなす事",
                            english = "ho ya tote tare ga suru towa omouna yo\n" +
                                    "kono yo hajimeta Kami no nasu koto",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 아무리 어려운 일이라 할지라도\n 신이 진실을 받아들인다면 5-40\n",
                            japanese = "どのよふなむつかし事とゆうたとて神がしんちつうけとりたなら",
                            english = "dono yona mutsukashi koto to yuta tote\n" +
                                    "Kami ga shinjitsu uketorita nara",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 지금까지는 미칠곳 미친곳이라 했지만\n 이제부터 앞으로는 미친곳뿐이야 5-41\n",
                            japanese = "いまゝでハからやにほんとゆうたれど\nこれからさきハにほんばかりや",
                            english = "imamade wa kara ya nihon to yutaredo\n" +
                                    "korekara saki wa nihon bakari ya",
                            commentary = "四十一,지금까지는 어버이신의 가르침이 이미 미친 곳과 다음에 미칠 곳을 구분하여 왔으나, 앞으로는 널리 온 세상에 어버이신의 가르침이 전해져서, 세계인류는 모두 어버이신의 뜻을 깨닫고 풍성한 신의 혜택을 받아 즐겁게 용솟음치며 살게 된다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 가지 끝은 무성해 보여도 대수롭지 않아\n 부딪치면 부러진다 앞으로 두고 보라 5-42\n",
                            japanese = "ゑださきハをふきにみへてあかんもの\nかまへばをれるさきをみていよ",
                            english = "edasaki wa oki ni miete akan mono\n" +
                                    "kamaeba oreru saki o mite iyo",
                            commentary = "四十二, 제3호 88～90 참조, 다음 노래의 주석 참조."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 근본되는 것은 작은 듯하나 뿌리가 소중해\n 무슨 일이든 근본을 알라 5-43\n",
                            japanese = "もとなるハちいさいよふでねがえらい\nとのよな事も元をしるなり",
                            english = "moto naru wa chiisai yo de ne ga erai\n" +
                                    "dono yona koto mo moto o shiru nari",
                            commentary = "四十三,나무 뿌리는 그 가지에 비하면 보잘 것 없는 듯해도 나무에 있어서는 가장 소중한 것이듯, 지엽적인 일을 밝히려면 먼저 그 뿌리가 되는 근본을 캐지 않으면 안된다.\n 인간의 지혜나 힘만을 믿지 말고, 어버이신의 뜻을 깨닫도록 하라는 가르침이다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 술이나 법이 훌륭하다 생각 말라\n 마음의 정성 이것이 진실 5-44\n",
                            japanese = "ぢつやとてほふがへらいとをもうなよ\nこゝろのまことこれがしんぢつ",
                            english = "jitsu ya tote ho ga erai to omouna yo\n" +
                                    "kokoro no makoto kore ga shinjitsu",
                            commentary = "四十四,술이나 법 등이 훌륭하다 하나 그 자체에 힘이 있는 것이 아니라, 그것을 쓰는 사람의 성진실한 마음에 힘이 있는 것이다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 인간은 어리석기 때문에\n 진기한 것을 술븝이라고들 한다 5-45\n",
                            japanese = "にんけんハあざないものであるからに\nめづらし事をほふなぞとゆう",
                            english = "ningen wa azanai mono de aru karani\n" +
                                    "mezurashi koto o ho nazo to yu",
                            commentary = "四十五,인간은 어리석기 때문에 조금 이상한 일이라도 하면 곧 술이다, 법이다 하면서 현혹되고 만다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 지금까지는 신이 나타나 있어도\n 아직 진실을 아는 자는 없다 5-46\n",
                            japanese = "いまゝでハ神があらハれでたるとて\nまだしんぢつをしりたものなし",
                            english = "imamade wa Kami ga araware detaru tote\n" +
                                    "mada shinjitsu o shirita mono nashi",
                            commentary = "四十六,지금까지 어버이신이 스스로 이 세상에 나타나서 진기한 구제를 하고 있어도, 세상 사람들은 이것을 아직도 법이나 술 등으로 생각할 뿐, 진실한 마음에 따라 구제한다는 사실을 모르고 있다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 앞으로는 무슨 일이든 진실을\n 가르쳐 줄 것이니 5-47\n",
                            japanese = "このさきハどのよな事もしんじつを\nをしへてをいた事であるなら",
                            english = "konosaki wa dono yona koto mo shinjitsu o\n" +
                                    "oshiete oita koto de aru nara",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 그로부터 신의 섭리를 무엇이든\n 자유자재로 나타낼 테다 5-48\n",
                            japanese = "それからハ神のはたらきなにもかも\nぢうよじざいをしてみせるでな",
                            english = "sorekara wa Kami no hataraki nanimo kamo\n" +
                                    "juyojizai o shite miseru de na",
                            commentary = "四十七, 四十八,앞으로는 무슨 일이든 미리 진실을 가르쳐 준 다음에 어버이신은 자유자재한 섭리를 나타내 보이겠다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 진실한 신이 섭리하게 되면\n 온 세상 사람들은 마음 맑아지리라 5-49\n",
                            japanese = "しんぢつの神のはたらきしかけたら\nせかい一れつ心すみきる",
                            english = "shinjitsu no Kami no hataraki shikaketara\n" +
                                    "sekai ichiretsu kokoro sumikiru",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 섭리도 어떤 것이라 생각하는가\n 마음 받아들이는 대로 갚아 주리라 5-50\n",
                            japanese = "はたらきもいかなる事とをもうかな\n心うけとりしだいかやしを",
                            english = "hataraki mo ika naru koto to omou kana\n" +
                                    "kokoro uketori shidai kayashi o",
                            commentary = "五十,어버이신이 자유자재한 섭리를 한다고 하니 어떤신기한 일을 할 것인가고 궁금히 여기겠지만, 어버이신은 인간들의 진실한 마음을 살펴보고 거기에 따라 곧 갚음을 하는 것이다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이 갚음 어떤 것이라 생각하는가\n 멀리 천리나 떨어져 있어도 5-51\n",
                            japanese = "このかやしなにの事やとをもうかな\nみちのりせんりへだてありても",
                            english = "kono kayashi nanino koto ya to omou kana\n" +
                                    "michinori sen ri hedate aritemo",
                            commentary = "五十一,어저이신은 이 갚음을 어떻게 하는가고 생각할지 모르나, 설사 천리나 2천리 밖에 떨어져 있는 사람일지라도 어버이ㅅ은 그 사람의 마음을 살핀 다음, 곧 거기에 대해 갚음을 하는 것이다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이것은 무슨 말을 하거나 생각하거나\n 받아들이는 대로 곧 갚아 주리라 5-52\n",
                            japanese = "この事ハなにをゆうてもをもふても\nうけとりしだいすぐにかやしを",
                            english = "kono koto wa nani o yutemo omotemo\n" +
                                    "uketori shidai suguni kayashi o",
                            commentary = "五十二,어버이신은 입으로 말하고 마음으로 생각하는 사소한 ㄱ이라도 받아들이는 대로 갚음을 한다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이 갚음 대수롭지 않은 일로 생각 말라\n 선악 할 것 없이 모두 갚을 테다 5-53",
                            japanese = "このかやしなんの事やとをもうなよ\nせんあくともにみなかやすてな",
                            english = "kono kayashi nanno koto ya to omouna yo\n" +
                                    "zen aku tomoni mina kayasu de na",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 선한 말을 해도 악한 생각을 해도\n 그대로 곧 갚음을 하는 거야 5-54\n",
                            japanese = "よき事をゆうてもあしきをもふても\nそのまゝすくにかやす事なり",
                            english = "yoki koto o yutemo ashiki omotemo\n" +
                                    "sono mama suguni kayasu koto nari",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이것을 납득하게 되면 모든 사람들은\n 누구라도 모두 맑아진다 5-55\n",
                            japanese = "この事をみへきたならば一れつわ\nどんなものでもみなすみわたる",
                            english = "kono koto o miekita naraba ichiretsu wa\n" +
                                    "donna mono demo mina sumiwataru",
                            commentary = "五十五,어버이신의 자유자재한 갚음이 실현되고, 또 사람들이 그 이치를 알게 되면 모두 다 천리로 깨달아 저절로 마음이 맑아지게 된다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 오늘은 아무것도 보이지 않지만\n 8월에 가서 보라 모두 나타나리라 5-56\n",
                            japanese = "けふの日ハなにがみへるやないけれど\n八月をみよみなみへるでな",
                            english = "kyonohi wa nani ga mieru ya nai keredo\n" +
                                    "hachi gatsu o miyo mina mieru de na",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 나타나는 것도 무엇이 나타날지 모르겠지\n 높은산에서부터 한길이 5-57\n",
                            japanese = "みへるのもなにの事やらしれまいな\n高い山からをふくハんのみち",
                            english = "mieru no mo nanino koto yara shiremai na\n" +
                                    "takai yama kara okwan no michi",
                            commentary = "五十六, 五十七,1874년 음력 10월에 마쯔에 이쩨베에, 나까따 기사브로오(仲田儀三郞) 등, 두 사람은 교조님의 본부로 오야마또 신사의 신직에게 가서 천신지기(天神地祇)의 모습과 그 수호에 대해 물었다. 때마침 거기에는 현내(縣內)에 있는 관폐대사(官弊大社)의 신직들이 많이 모여 있었는데, 오야마또 신사의 하라(原)라고 하는 자가 “신의 모습 같은 것은 일찍이 들어 본 적도 없다. 그런 어려석은 소리를 하는 자는 쇼야시끼의 할머니겠지. 해괴한 말이다. 무슨 증거될 만한 것이라도 있는가.”고 엄하게 따져 물므로, 마쯔오는 “쇼야시끼에는 어버이신님의 수호에 대해 이렇게 말하고 있다.”고 하면서 교의서를 꺼내 들고 반론을 폈다. 그러자 하라라는 자는 아무 대꾸도 못하다가, 이윽고 온갖 욕설을 퍼부어댔다. 그 자리에 있던 신직들은 “기기(記紀)에도 나오지 안는 신명을 말하는 것은 부당한 일이므로 이것은 따져 볼 필요가 있다. 이소노까미 신궁(石上神宮)은 그 구역 사람이 이러한 이설(異說)을 부르짖도록 내버려 둔 데 대해 단속이 불충분했다는 비난을 면치 못할 것이다. 마땅히 이소노까미 신궁에서 조사를 해야 한다. 조만간 조사하러 갈 것이니, 그리 알고 있어라.”고 호통을 쳤다. 과연 이소노까미 신궁에서 신직 5명이 따지러 왔으나, 교조님의 막힘 없는 답변에 그만 기가 질려 물러나와서는 그 길로 담바이찌(丹波市)경찰분서에 가서 고발했다. 이에 따라 경관이 집터에 달려와 근행장소의 발, 어폐, 거울 등을 마구 몰수하여 마을 관리에게 맡기고 돌아갔다. 그 후 이해 음력 11월 15일에 교조님은 나까따, 쯔지, 하따(畑) 마을의 오오히가시 주우베에(大東重兵衛) 등을 거느리고 호출에 응하여 야마무라고뗑으로 행차하셨다. '높은산에서부터 한길'이란, 이 사실을 두고 하신 말씀으로서, 즉 관헌을 상징하는 이른바 높은산에 포교하는 것을 뜻한다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이 길을 내려고 준비해 온 것을\n 곁의 사람은 아무것도 모르고서 5-58\n",
                            japanese = "このみちをつけよふとてにしこしらへ\nそばなるものハなにもしらすに",
                            english = "kono michi o tsukeyo tote ni shikoshirae\n" +
                                    "soba naru mono wa nanimo shirazu ni",
                            commentary = "五十八,이 한길을 내려고 지금까지 여러 가지로 준비를 해 왔으나, 곁의 사람들은 이것이 어버이신의 깉은 의도에서 비롯되었음을 아무도 모르고 있다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이곳에 부르러 오는것도 나오는 것도\n 신의 의도가 있기 때문에 5-59\n",
                            japanese = "このとこへよびにくるのもでゝくるも\n神のをもハくあるからの事",
                            english = "kono toko e yobi ni kuru no mo detekuru mo\n" +
                                    "Kami no omowaku aru kara no koto",
                            commentary = "五十九,이 터전에 경관이 연행하러 오는 것도 조사하러 오는 것도 모두 어비이신의 깊은 의도가 있어서 그렇게 하는 것이다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 그것을 전혀 모르고서 곁의 사람들은\n 세상 보통 일로 생각해서 5-60\n",
                            japanese = "その事をなにもしらすにそばなるハ\nせかいなみなる事をふもをて",
                            english = "sono koto o nanimo shirazu ni soba naru wa\n" +
                                    "sekainami naru koto o omote",
                            commentary = "六十,이 소환이나 조사는 어버이신의 깉은 의도에서 그렇게 하고 있는 것임을 모르고서, 곁의 사람들은 세상 보통 일로 여겨 걱정하거나 두려워하고 있다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 무엇이든 세상 보통 일로 생각 말라\n 무엇인가 진기한 길이 있는 거야 5-61\n",
                            japanese = "なにゝてもせかいなみとハをもうなよ\nなにかめつらしみちがあるぞや",
                            english = "nani nitemo sekainami towa omouna yo\n" +
                                    "nanika mezurashi michi ga aru zo ya",
                            commentary = "六十一,무슨 일이 생기더라도 결코 세상 보통 일이 아닌 만큼 걱정할 필요가 없다. 오히려 그것이 하나의 마디가 되어 무언가 진기한 길이 열리게 된다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 차츰차츰 이 세상 창조한 지 오래지만\n 아무도 진실을 아는 자 없다 5-62\n",
                            japanese = "だん／＼とこのよはぢめてひハたてど\nたれかしんぢつしりたものなし",
                            english = "dandan to kono yo hajimete hi wa tatedo\n" +
                                    "tare ka shinjitsu shirita mono nashi",
                            commentary = "六十二,이 세상을 창조한 이래 오랜 세월이 지났음에도 누구 하나 어버이신의 진실을 아는 사람은 없다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 아무리 신이 마음을 서둘러도\n 모두들의 마음은 아직 멍청해 5-63\n",
                            japanese = "いかほどに神の心わせゑたとて\nみなの心ハまたうゝかりと",
                            english = "ika hodoni Kami no kokoro wa seta tote\n" +
                                    "mina no kokoro wa mada ukkari to",
                            commentary = "六十三,어버이신이 아무리 서둘러도 모두들의 마음은 그저 멍청하여 깨달음이 없다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 어서어서 생각해 보고 서둘러라\n 뿌리 파헤칠 준비 왜 아니하나 5-64\n",
                            japanese = "はや／＼としやんしてみてせきこめよ\nねへほるもよふなんでしてでん",
                            english = "hayabaya to shiyan shite mite sekikome yo\n" +
                                    "ne horu moyo nande shiteden",
                            commentary = "六十四,어버이신이 뜻하는 바가 무엇인지를 잘 생각하여 그 근본을 캘 준비를 왜 하지 않는가. 자, 빨리 이 준비에 착수하라"
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이 세상의 진실한 뿌리 캐는 법을\n 아는 자는 전혀 없었으므로 5-65\n",
                            japanese = "このよふのしんぢつねへのほりかたを\nしりたるものハさらにないので",
                            english = "kono yo no shinjitsu ne no horikata o\n" +
                                    "shiritaru mono wa sarani nai node",
                            commentary = "六十五,이 세상의 근본, 즉 어버이신이 이 세상 인간을 창조한 근본을 어더ㄷ게 하면 캘 수 있는지 그 방법을 아는 자는 지금까지 아무도 없었다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이 뿌리를 진실로 캐게 되면\n 참으로 믿음직한 길이 될 것인데 5-66\n",
                            japanese = "このねへをしんぢつほりた事ならば\nま事たのもしみちになるのに",
                            english = "kono ne o shinjitsu horita koto naraba\n" +
                                    "makoto tanomoshi michi ni naru no ni",
                            commentary = "六十六만약 이 근본을 진실로 캐게 되면, 참으로 믿음직한 길이 될 것이다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이 길을 헤쳐 나가면\n 위나 아래나 모두 마음 용솟음칠 텐데 5-67\n",
                            japanese = "このみちをほりきりとふりぬけたなら\n上下ともに心いさむに",
                            english = "kono michi o horikiri tori nuketa nara\n" +
                                    "kami shimo tomoni kokoro isamu ni",
                            commentary = "六十七,이 길을 어디까지나 어버이신의 뜻에 맞게 걸어 나간다면, 마침내는 위나 아래나 모두 마음이 용솟음치는 즐거운 삶의 세계가 나타나게 된다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이제부터는 어떻든 온 세상 사람들을\n 용솟음치게 할 준비만 하는 거야 5-68\n",
                            japanese = "これからハなんでもせかい一れつを\nいさめるもよふばかりするそや",
                            english = "korekara wa nandemo sekai ichiretsu o\n" +
                                    "isameru moyo bakari suru zo ya",
                            commentary = "六十八,이제부터는 어떻든 온 세상 사람들의 마음을 용솟음치게 할 준비만을 한다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 차츰차츰 무엇이든 미친곳에는\n 모르는 것이 없도록 5-69\n",
                            japanese = "だん／＼となに事にてもにほんにハ\nしらん事をわないとゆうよに",
                            english = "dandan to nani goto nitemo nihon niwa\n" +
                                    "shiran koto owa nai to yu yoni",
                            commentary = "六十九,차츰 이 세상의 근본을 모두 일러주어서, 어버이신의 가르침이 이미 미친 곳에는 무엇이든 모르는 것이 없도록 수호하려 하고 있다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 무엇이든 온 세상에 가르치고 싶다\n 신의 의도 깊기 때문에 5-70\n",
                            japanese = "なにもかもせかいぢうゝへをしへたい\n神のをもわくふかくあるのに",
                            english = "nanimo kamo sekaiju e oshietai\n" +
                                    "Kami no omowaku fukaku aru no ni",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 그것을 모르고서 온 세상은 한결같이\n 무엇인가 위태로운 듯 생각해서 5-71\n",
                            japanese = "それしらすせかいぢうゝハ一れつに\nなんどあぶなきよふにをもふて",
                            english = "sore shirazu sekaiju wa ichiretsu ni\n" +
                                    "nando abunaki yoni omote",
                            commentary = "七十, 七十一,무엇이든 온 세상 사람들에게 가르쳐 주고 싶은 것이 어버이신의 깊은 의도이나, 그것을 모르는 세상 사람들은 모두 한결같이 어버이신의 가르침이 거짓이 아닌가고 의심하며 무언가 위태로운 듯이 여기고 있다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 무슨 말이든 신이 하는 거야\n 무슨 위태로움이 있겠는가 5-72\n",
                            japanese = "とのよふな事でも神のゆう事や\nなんのあぶなき事があるそや",
                            english = "dono yona koto demo Kami no yu koto ya\n" +
                                    "nanno abunaki koto ga aru zo ya",
                            commentary = "七十二,무슨 말이든 모두 어비이신이 하는 것인데, 거기에 무슨 거짓이냐 위태로움이 있을리 있겠는가."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 무엇이든 세상에서는 만가지 일을 차츰차츰\n 말하고 있으나 알지는 못한다 5-73\n",
                            japanese = "なにもかもよろずの事をだん／＼と\nゆうていながらわかりたるなし",
                            english = "nanimo kamo yorozu no koto o dandan to\n" +
                                    "yute inagara wakaritaru nashi",
                            commentary = "七十三,세상에서는 무엇이든 다 알고 있는 듯이, 그리고 입으로는 진실인 듯이 말하고 있으나, 참으로 그 근본을 알고 있는 자는 아무도 없다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이제부터는 부디 진실로 가슴속\n 빨리 맑힐 준비를 해 다오 5-74\n",
                            japanese = "これからハどうぞしんぢつむねのうち\nはやくすまするもよふしてくれ",
                            english = "korekara wa dozo shinjitsu mune no uchi\n" +
                                    "hayaku sumasuru moyo shite kure",
                            commentary = "七十四,이제부터는 어떻든 빨리 마음의 티끌을 털어 내어 깨끗이 맑힐 준비를 해 다오."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 온 세상 많은 사람이기 때문에\n 이것 맑히는 것이 어려운 일 5-75\n",
                            japanese = "せかいぢうをふくの人てあるからに\nこれすまするがむつかしい事",
                            english = "sekaiju oku no hito de aru karani\n" +
                                    "kore sumasuru ga mutsukashii koto",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 아무리 어려운 일이라 할지라도\n 각자 마음의 진실을 보라 5-76\n",
                            japanese = "いかほどにむつかし事とゆうたとて\nわが心よりしんちつをみよ",
                            english = "ika hodoni mutsukashi koto to yuta tote\n" +
                                    "waga kokoro yori shinjitsu o miyo",
                            commentary = "七十五, 七十六,온 세상에는 여러 사람이 많이 살고 있기 때문에, 그들의 마음을 모두 맑히는 일은 결코 쉬운 것이 아니지만, 사람들이 각자 자신의 마음부터 맑혀서 어버이신의 진실한 마음을 깨달아 여기에 맞도록 노력한다면 안될 것은 없다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이 마음 맑아져서 알게 되면\n 그대로 나타나리라 5-77\n",
                            japanese = "この心すむしわかりた事ならば\nそのまゝみゑる事であるなり",
                            english = "kono kokoro sumushi wakarita koto naraba\n" +
                                    "sono mama mieru koto de aru nari",
                            commentary = "七十七,사람들의 마음이 맑아져서 어버이신의 뜻을 깨닫게 되면 곧 어버이신의 자유자재한 섭리가 나타난다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 나날이 신의 진실한 섭리를\n 아는 자는 전혀 없으리라 5-78\n",
                            japanese = "にち／＼に神のしんぢつはたらきを\nしりたるものハさらにあるまい",
                            english = "nichinichi ni Kami no shinjitsu hataraki o\n" +
                                    "shiritaru mono wa sarani arumai",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 무엇이든 신의 자유자재란\n 진기한 일을 나타낼 테다 5-79\n",
                            japanese = "なにゝても神のぢうよとゆうものハ\nめづらし事をしてみせるでな",
                            english = "nani nitemo Kami no juyo to yu mono wa\n" +
                                    "mezurashi koto o shite miseru de na",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 어떤 진기한 일이라 할지라도\n 신이 하는 일 이루는 일뿐이니 5-80\n",
                            japanese = "とのよふなめつらし事とゆうたとて\n神のする事なす事はかり",
                            english = "dono yona mezurashi koto to yuta tote\n" +
                                    "Kami no suru koto nasu koto bakari",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 지금까지는 아무것도 몰랐었다\n 조금 나타나기 시작한 좁은 길 5-81\n",
                            japanese = "いままでハなによの事もしれなんだ\n一寸みへかけたほそいみちすじ",
                            english = "imamade wa nani yono koto mo shirenanda\n" +
                                    "choto miekaketa hosoi michisuji",
                            commentary = "八十一,지금까지는 사람들이 아무것도 몰랐으나, 이제는 좁은 길이 조금 나타나 어버이신의 진실한 마음을 알기 시작했다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이 길을 차츰차츰 그리며 나아가면\n 틀림없이 저만치 보이는 본길 5-82\n",
                            japanese = "このみちをだん／＼しといいくならば\nなんてもむこにみへるほんみち",
                            english = "kono michi o dandan shitoi iku naraba\n" +
                                    "nandemo muko ni mieru honmichi",
                            commentary = "八十二,이 좁은 길을 그리며 마음을 맑혀서 차츰 앞으로 나아가면 틀림없이 길이 열려 한 길에 이를 수 있다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이제까지 지나온 길은\n 미칠곳도 미친곳도 알지 못했다 5-83\n",
                            japanese = "これまでにとふりてきたるみちすぢハ\nからもにほんもわかりないので",
                            english = "koremade ni torite kitaru michisuji wa\n" +
                                    "kara mo nihon mo wakari nai node",
                            commentary = "八十三,이제까지 인간들이 지나온 과정을 살펴보면, 어버이신의 가르침을 일러주기 전에는 미친곳이나 미칠곳이나 전혀 구별 없이 아무것도 모른 채 살아왔었다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 앞으로는 아무리 미칠곳이라 할지라도\n 미친곳이 지는 일은 없는 거야 5-84\n",
                            japanese = "このさきハなんぼからやとゆうたとて\nにほんがまけるためしないそや",
                            english = "konosaki wa nanbo kara ya to yuta tote\n" +
                                    "nihon ga makeru tameshi nai zo ya",
                            commentary = "八十四,앞으로는 아직 어버이신의 가르침을 모르는 자가 제 아무리 지혜나 힘이 뛰어나다 하더라도, 어버이신의 가르침을 이미 납득하고 있는 자를 마음대로 지배하지는 못하게 된다.\n제2호 34의 주석 참조."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이 세상 태초의 뿌리를 캐게 하리라\n 힘이 있으면 송두리째 캐 보라 5-85\n",
                            japanese = "このよふのもとはじまりのねをほらそ\nちからあるならほりきりてみよ",
                            english = "kono yo no moto hajimari no ne o horaso\n" +
                                    "chikara aru nara horikirite miyo",
                            commentary = "八十五,이 세상 태초의 근본을 캐도록 해 줄 것이니, 인간은 지혜와 힘이 미치는 한 송두리째 캐서 어버이신의 뜻을 빨리 깨닫도록 하라."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이 뿌리를 송두리째 캐기만 하면\n 어떤 저도 당할 수 없다 5-86\n",
                            japanese = "このねへをほりきりさいかしたるなら\nどのよなものもかなうものなし",
                            english = "kono ne o horikiri saika shitaru nara\n" +
                                    "dono yona mono mo kanau mono nashi",
                            commentary = "八十六,이 근본을 송두리째 캐서 어버이신의 뜻을 깨닫게만 되면, 어떤 사람도 여기에 당할 수 없을 것이다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 단단히 들어라 입으로 말하거나 생각하거나\n 어디서 말하거나 생각하거나 5-87\n",
                            japanese = "しかときけくちでゆうてもをもふても\nどこでゆうてもをもふたるとて",
                            english = "shikato kike kuchi de yutemo omotemo\n" +
                                    "doko de yutemo omotaru tote",
                            commentary = "八十七,잘 들어 두라, 입으로 말하거나 마음으로 생각하거나, 또 아무리 먼 곳에서 말하거나 생각하더라도 어버이신은 모두 환히 꿰뚫어 보고 선악을 판별하고 있다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 그대로 갚는다 함은 바로 이것이야\n 신이 물러난다 모두들 알아차려라 5-88\n",
                            japanese = "そのまゝにかやしとゆうハこの事や\n神がしりぞくみなしよちせよ",
                            english = "sono mamani kayashi to yu wa kono koto ya\n" +
                                    "Kami ga shirizoku mina shochi seyo",
                            commentary = "八十八,어버이신은 판별한 대로 곧 어떠한 갚음도 하는데, 만약 사람들이 어버이신이 하는 말, 하는 일에 대해 여러 가지로 의심하거나 위태롭게 여기거나 비방하거나 하면, 어버이신은 이에 대한 갚음으로써 몸의 수호를 중단해 버릴 것이니, 이 점 모두들은 잘 깨닫도록 하라."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이번에는 진기한 일을 말하리라\n 마음을 가다듬어 이것을 들어 다오 6-1\n",
                            japanese = "このたひハめづらし事をゆいかける\n心しずめてこれきいてくれ",
                            english = "nani goto mo Kami no suru koto yu koto ya\n" +
                                    "soba ni shinpai kakeru koto nashi",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 무엇이든 신이 하는 일 하는 말이야\n 곁의 사라들은 걱정할 것 없다 6-2\n",
                            japanese = "なに事も神のする事ゆう事や\nそばにしんバいかける事なし",
                            english = "konotabi wa mezurashi koto o yui kakeru\n" +
                                    "kokoro shizumete kore kiite kure",
                            commentary = "二,무슨 일을 하거나 무슨 말을 하거나, 이것은 모두 어버이신이 하는 일이요 하는 말이므로, 곁의 사람드에게 결코 걱정 끼칠 리는 없다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이 이야기 부디 진실로 모든 사람들은\n 마음을 가다듬어 알아차려 다오 6-3\n",
                            japanese = "このはなしどふどしんぢつ一れつわ\n心しづめてしよちしてくれ",
                            english = "kono hanashi dodo shinjitsu ichiretsu wa\n" +
                                    "kokoro shizumete shochi shite kure",
                            commentary = "三,어버이신이 일러주는 이 이야기를 모두들은 부디 마음을 가다듬어 잘 깨달아 주도록."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이 길은 어떤 것이라 생각하는가\n 이 세상 안정시키는 진실한 길 6-4\n",
                            japanese = "このみちハどふゆう事にをもうかな\nこのよをさめるしんぢつのみち",
                            english = "kono michi wa doyu koto ni omou kana\n" +
                                    "kono yo osameru shinjitsu no michi",
                            commentary = "四,어버이신이 내려고 하는 이 길은 어떤 가르침이라 생각하는가. 이것은 사람들의 마음을 다스려서 세상을 안정시키는 진실한 길이다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 윗사람이 물과 불을 깨닫게 되면\n 저절로 안정되어 즐거움이 넘칠 거야 6-5\n",
                            japanese = "上たるの火と水とをわけたなら\nひとりをさまるよふきづくめに",
                            english = "kami taru no hii to mizu to o waketa nara\n" +
                                    "hitori osamaru yokizukume ni",
                            commentary = "五,윗사람들이 어버이신의 수호를 잘 깨닫게 되면, 이 세상은 저절로 평화롭고 즐거움이 넘치게 된다.\n 불과 물은 제2호 40의 주석 참조.\n 깨닫게 되면이란 말의 원어는 이해한다는 뜻만이 아니고, 가린다는 뜻도 포함되어 있다. 물과 줄을 혼합하면 혼돈하여 문란한 상태가 된다. 5푼5푼의 활동이 있는 곳에 조화의 세계가 있다. 이것이 즉 즐거운 삶으로서, 이는 모두 어버이신님의 수호에 의한다. 불과 물을 가린다는 것은 혼돈한 상태를 분명히 하여 어버이신님의 수호를 깨닫게 한다는 뜻."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이 물과 불을 깨닫는다 함은 여기서\n 즐거운근행을 하는 것이라 생각하라 6-6\n",
                            japanese = "この火水わけるとゆうハこのところ\nよふきづとめをするとをもゑよ",
                            english = "kono hi mizu wakeru to yu wa kono tokoro\n" +
                                    "Yoki-zutome o suru to omoe yo",
                            commentary = "六,어버이신의 수호를 깨닫게 하여 평화롭고 즐거운 삶의 세계를 실현하는 데는, 터전에서 즐거운근행을 하는 것이 무엇보다도 필요한 것임을 잘 깨달아야 한다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이 세상을 창조하는 것과 같은 것\n 진기한 일을 나태낼 테다 6-7\n",
                            japanese = "このよふをはじめかけたもをなぢ事\nめずらし事をしてみせるでな",
                            english = "kono yo o hajime kaketa mo onaji koto\n" +
                                    "mezurashi koto o shite miseru de na",
                            commentary = "七,어버이신이 원래 없던 세계 없던 인간을 창조한 것을 사람들은 신기한 일로 생각할 것인데, 이와 같은 진기한 일을 어버이신은 앞으로도 나타내 보일 것이다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이 세상을 창조한 이래 없던 근행\n 또 시작하여 확실히 안정시킨다 6-8\n",
                            japanese = "このよふをはじめてからにないつとめ\nまたはじめかけたしかをさめる",
                            english = "kono yo o hajimete karani nai Tsutome\n" +
                                    "mata hajime kake tashika osameru",
                            commentary = "八,어버이신은 이 세상 인간을 창조한 진기한 섭리를 구제한줄기에 다시 나타내기 위하여 창조 이래 없었던 즐거운근행을 시작할 것이며, 이로써 사람들의 마음을 바꾸어 세상이 평온하게 안정되도록 수호할 것이다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이 세상 月日의 마음의 진실을\n 아는 자는 전혀 없으리라 6-9\n",
                            japanese = "このよふの月日の心しんぢつを\nしりたるものわさらにあるまい",
                            english = "kono yo no Tsukihi no kokoro shinjitsu o\n" +
                                    "shiritaru mono wa sarani arumai",
                            commentary = "九,이 세상을 창조한 으뜸인 어버이신의 마음의 진실을 참으로 아는 자는 아무도 없을 것이다.\n月日은 으뜸인 신, 진실한 신이신 천리왕님을 말한다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이제까지는 어떤 신도\n 눈에 보이지 않는다고 말하고 있었다 6-10\n",
                            japanese = "これまでハいかなる神とゆうたとて\nめゑにみへんとゆうていたなり",
                            english = "koremade wa ika naru Kami to yuta tote\n" +
                                    "me ni mien to yute ita nari",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이번에는 여러 신의 근본되는 신이\n 나타나서 이야기해 주리라 6-11\n",
                            japanese = "このたびわとのよな神もしんぢつに\nあらハれだしてはなしするなり",
                            english = "konotabi wa dono yona Kami mo shinjitsu ni\n" +
                                    "araware dashite hanashi suru nari",
                            commentary = "十, 十一,이제까지는 어떤 신도 눈에 보이지 않는다고 말해 왔으나, 이번에는 여러 신의 근본이 되는 천리왕님이 이 세상에 나타나서 이야기를 하는 것이다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 지금부터는 무슨 말을 하거나 생각하거나\n 그대로 나타난다 이것이 신기해 6-12\n",
                            japanese = "いまからハなにをゆうてもをもふても\nそのまゝみへるこれがふしぎや",
                            english = "imakara wa nani o yutemo omotemo\n" +
                                    "sono mama mieru kore ga fushigi ya",
                            commentary = "十二,지금부터는 무슨 말을 하거나 생각하거나 모두 그대로 실현된다. 이것이 어버이신의 신기한 섭리이다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 무엇이든 가을을 계기로 나타난다\n 즐거운근행을 빨리 시작하라 6-13\n",
                            japanese = "なにもかもあきをあいづにみへかける\nよふきづとめにはやくかゝれよ",
                            english = "nanimo kamo aki o aizu ni mie kakeru\n" +
                                    "Yoki-zutome ni hayaku kakare yo",
                            commentary = "十三,무슨일이든지 가을을 계기로 실현될 것이니 어서 즐거운근행을 시작하도록 하라.\n 제 六호는 一八七四년 十二월부터 집필하셨는데, 이 노래는 다음 해인 5월에 터전을 결정하고 그 후 '온 세상 마음 맑히는 감로대'의 손질을 가르칠 것을 미리 예언하신 것이라 생각된다. 가을이란 수확기란 뜻으로서, 여기서는 보리 수확기를 말한다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 온 세상에 많은 사람이 살고 있으나\n 사람들은 모두 안개 속에 사는 것 같아서 6-14\n",
                            japanese = "せかいぢうをふくくらするそのうちわ\n一れつハみなもやのごとくや",
                            english = "sekaiju oku kurasuru sono uchi wa\n" +
                                    "ichiretsu wa mina moya no gotoku ya",
                            commentary = "十四,세상에는 많은 사람이 살고 있으나, 그 마음은 모두 안개처럼 흐려 있어서 앞을 못 본다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 나날이 맑아져 알게 되는 신의 뜻\n 성인됨에 따라 나타날 거야 6-15\n",
                            japanese = "にち／＼にすむしわかりしむねのうち\nせゑぢんしたいみへてくるぞや",
                            english = "nichinichi ni sumushi wakarishi mune no uchi\n" +
                                    "sejin shidai miete kuru zo ya",
                            commentary = "十五,이 길을 걸어가면 하루하루 마음이 맑아져 성인의 길로 나아가게 되는데, 그에 따라 어버이신의 참뜻도 알 수 있게 된다. 성인(成人)이란 마음의 성인을 뜻한다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이 길이 확실히 나타난다면\n 장래를 참으로 낙으로 삼아라 6-16\n",
                            japanese = "このみちがたしかみへたる事ならば\nこのさきたしかたのしゆでいよ",
                            english = "kono michi ga tashika mietaru koto naraba\n" +
                                    "konosaki tashika tanoshun de iyo",
                            commentary = "十六,이 길이 확실히 나타난다면, 앞으로도 어버이신이 하는 말, 하는 일에 조금도 틀림이 없음을 굳게 믿고 장래에 실현될 즐거운 삶의 세계를 낙으로 삼도록 하라."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 차츰차츰 마음 용솟음치며 서둘러라\n 빨리 본길로 나가기를 고대한다 6-17\n",
                            japanese = "たん／＼と心いさんでせきこめよ\nはやくほんみちいそぎでるぞや",
                            english = "dandan to kokoro isande sekikome yo\n" +
                                    "hayaku honmichi isogi deru zo ya",
                            commentary = "十七,지금 어떤 길을 걷고 있더라도 낙심하지 말고 마음 용솟음치며 즐거운 삶을 향해 정진하라. 어버이신은 하루 빨리 본길로 나가도록 서두르고 있다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 진실한 근행인원 열 사람의\n 마음을 신이 받아들인다면 6-18\n",
                            japanese = "しんぢつのつとめの人ぢう十人の\n心を神がうけとりたなら",
                            english = "shinjitsu no Tsutome no ninju ju nin no\n" +
                                    "kokoro o Kami ga uketorita nara",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 그로부터는 무슨 일이든 차츰차츰\n 신의 의도 모두 일러준다 6-19\n",
                            japanese = "それからハどのよな事もたん／＼と\n神のをもわくみなときゝかす",
                            english = "sorekara wa dono yona koto mo dandan to\n" +
                                    "Kami no omowaku mina tokikikasu",
                            commentary = "十八, 十九,진실한 근행인원 열 사람이 갖추어지고 그 사람들의 마음을 어버이신이 받아들인다면, 그 후로는 무슨 일이든 점차로 어버이신의 뜻을 모두 일러줄 테다.\n진실한 근행인원 열 사람이란 어버이신님의 뜻에 맞는 마음을 갖고 근행인원으로서 역할을 다하는 열 사람을 가리키는데, 즐거운근행은 십주신(十住神)의 리를 나타내는 이 열 사람이 올리도록 되어 있다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 나날이 신이 마음을 서둘러도\n 인원 열 사람 갖추지 않으면 6-20\n",
                            japanese = "にち／＼に神の心わせゑたとて\n人ぢう十人そろいなけねば",
                            english = "nichinichi ni Kami no kokoro wa seta tote\n" +
                                    "ninju ju nin soroi nakeneba",
                            commentary = "二十,빨리 즐거운근행을 행하도록 하려고 어버이신은 나날이 서두르고 있지만, 성진실한 마음을 가진 열 사람의 근행인원이 갖추어지지 않으면 아무것도 안된다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 열 사람 가운데 수족되는 세 사람은\n 몰 불 바람이 함께 물러날 줄 알라 6-21\n",
                            japanese = "十人のなかに三人かたうでわ\n火水風ともしりぞくとしれ",
                            english = "ju nin no naka ni san nin kataude wa\n" +
                                    "hii mizu kaze tomo shirizoku to shire",
                            commentary = "二十一,근행인원 열 사람 가운데 수족이 될 세 사람에게 물,불,바람의 수호가 중단되는 일이 있을 것인데, 이것을 잘 알아차리도록 하라.\n 히가시와까이 마을의 마쯔오 이찌베에, 닷다 마을의 이누이 감베에(乾勘兵衛),오오니시(大西)마을의 기따노 감베에(北野勘兵衛) 등, 세 사람의 출직을 보고, 당시 사람들은 이 노래를 상기했다고 한다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 무슨 일이든 신이 하는 거야\n 이것을 질병이라고는 전혀 생각 말라 6-22\n",
                            japanese = "どのよふな事でも神のする事や\nこれをやまいとさらにをもうな",
                            english = "dono yona koto demo Kami no suru koto ya\n" +
                                    "kore o yamai to sarani omouna",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 무엇이든 진실한 신의 자유자재를\n 알리고 싶어서 해 보일 테다 6-23\n",
                            japanese = "なにもかもしんぢつ神のぢふよふを\nしらしたいからしてみせるでな",
                            english = "nanimo kamo shinjitsu Kami no juyo o\n" +
                                    "shirashitai kara shite miseru de na",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이제까지는 어떤 길을 걸었어도\n 날이 오지 않아서 침울해 있었다 6-24\n",
                            japanese = "これまでハいかなるみちをとふりても\nひがきたらんでいづみいたなり",
                            english = "koremade wa ika naru michi o toritemo\n" +
                                    "hi ga kitarande izumi ita nari",
                            commentary = "二十四,이제까지는 사람들이 어떤 길을 걷고 있어도, 여기에 대해 충분히 깨우칠 시기가 아직 오지 않았기 때문에 어버이신은 잠자코 있었던 것이다.\n 침울이란 여기서는 잠자코 있다, 대기하다, 보류하다는 뜻.(제六호 五十九 참조)"
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 앞으로는 무슨 일이든 차츰차츰\n 본진실을 일러준다 6-25\n",
                            japanese = "このさきハどのよな事もたん／＼と\nほんしんぢつをゆうてきかする",
                            english = "konosaki wa dono yona koto mo dandan to\n" +
                                    "honshinjitsu o yute kikasuru",
                            commentary = "二十五,앞으로는 무슨 일이든 차차로 어버이신의 본진실을 일러준다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 지금까지는 어떤 신도 많이 있어서\n 절한다 기도한다 말하고 있었지만 6-26\n",
                            japanese = "いまゝでハいかなる神も山／＼に\nをがみきとふとゆうたなれども",
                            english = "imamade wa ika naru Kami mo yamayama ni\n" +
                                    "ogami kito to yuta naredomo",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이 근본을 아는 자가 있다면\n 찾아가 보라 신이 허락한다 6-27\n",
                            japanese = "このもとをしりたるものかあるならば\nたづねいてみよ神がゆるする",
                            english = "kono moto o shiritaru mono ga aru naraba\n" +
                                    "tazune ite miyo Kami ga yurusuru",
                            commentary = "二十六,二十七,지금까지는 여러 가지 신이 많이 있어서 그 어느 것이나 절하고 기도하면 효혐이 있다고 말해 왔으나, 왜 그같은 효혐이 있는지, 그 근본에 대해서는 아는 자가 없다. 만약 있다면 신이 허락할 테니 찾아가서 물어 보라."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 또 앞으로는 무슨 일이든 차츰차츰\n 본길을 내게 되면 6-28\n",
                            japanese = "またさきハとのよな事もたん／＼と\nほんみちつけた事であるなら",
                            english = "matasaki wa dono yona koto mo dandan to\n" +
                                    "honmichi tsuketa koto de aru nara",
                            commentary = "二十八,앞으로는 어버이신이 확실하게 한길을 내게 되며 그때는 무슨 일이든 차차로 일러줄 것이다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 지금까지 없던 일만 말해서\n 만가지구제의 근행을 가르친다 6-29\n",
                            japanese = "いまゝでにない事ばかりゆいかけて\nよろづたすけのつとめをしへる",
                            english = "imamade ni nai koto bakari yui kakete\n" +
                                    "yorozu tasuke no Tsutome oshieru",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이 근행 열 사람의 인원 가운데\n 태초의 어버이가 있는 거야 6-30\n",
                            japanese = "このつとめ十人にんぢうそのなかに\nもとはじまりのをやがいるなり",
                            english = "kono Tsutome ju nin ninju sono naka ni\n" +
                                    "moto hajimari no Oya ga iru nari",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이자나기(남자추형종자)와 이자나미(여자추형묘상)을 이끌어 들여\n 인간 창조의 수호를 가르쳤다 6-31\n",
                            japanese = "いざなぎといざなみいとをひきよせて\nにんけんはぢめしゆごをしゑた",
                            english = "Izanagi to Izanamii to o hikiyosete\n" +
                                    "ningen hajime shugo oshieta",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이 근본은 진흙바다 속의 인어와 흰뱀\n 그것을 끌어내어 부부로 삼았다 6-32\n",
                            japanese = "このもとハどろうみなかにうをとみと\nそれひきだしてふう／＼はちめた",
                            english = "kono moto wa doroumi naka ni uo to mi to\n" +
                                    "sore hikidashite fufu hajimeta",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이 세상 태초는 진흙바다\n 그 가운데는 미꾸라지 뿐이었다 6-33\n",
                            japanese = "このよふの元はじまりハとろのうみ\nそのなかよりもどぢよばかりや",
                            english = "kono yo no moto hajimari wa doro no umi\n" +
                                    "sono naka yori mo dojo bakari ya",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 그 속에 인어와 흰뱀이 섞여 있어\n 잘 살펴보니 인간의 얼굴 6-34\n",
                            japanese = "そのうちにうをいみいとがまちりいる\nよくみすませばにんげんのかを",
                            english = "sono uchi ni uo to mii to ga majiriiru\n" +
                                    "yoku misumaseba ningen no kao",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 그것을 보고 착상한 것은 진실한\n 月日의 마음인 거야 6-35\n",
                            japanese = "それをみてをもいついたハしんぢつの\n月日の心ばかりなるそや",
                            english = "sore o mite omoi tsuita wa shinjitsu no\n" +
                                    "Tsukihi no kokoro bakari naru zo ya",
                            commentary = "三十一～三十五,태초이야기입니다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이들에게 도구를 끌어들여 차츰차츰\n 수호를 가르치게 되면 6-36\n",
                            japanese = "このものにどふくをよせてたん／＼と\nしゆこふをしゑた事であるなら",
                            english = "kono mono ni dogu o yosete dandan to\n" +
                                    "shugo oshieta koto de aru nara",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이 도구 구니사즈찌(결합수호)와 쯔끼요미(지탱수호)\n 이들을 몸 속에 끌어넣어 가르친다면 6-37\n",
                            japanese = "このどふくくにさづちいと月よみと\nこれみのうちゑしこみたるなら",
                            english = "kono dogu Kunisazuchii to Tsukiyomi to\n" +
                                    "kore minouchi e shikomitaru nara",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 구모요미(수기승강수호)와 가시꼬니(풍기수호)와 오오또노베(인출수호)와\n 다이쇼꾸뗀(절단수호)를 끌어들이면 6-38\n",
                            japanese = "くもよみとかしこねへとをふとのべ\nたいしよく天とよせた事なら",
                            english = "Kumoyomi to Kashikonee to Otonobe\n" +
                                    "Taishokuten to yoseta koto nara",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 그것으로써 확실히 세상을 창조하려고\n 신은 의논을 했다 6-39\n",
                            japanese = "それからハたしかせかいを初よと\nかみのそふだんしまりついたり",
                            english = "sorekara wa tashika sekai o hajime yo to\n" +
                                    "Kami no sodan shimari tsuitari",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 여기서부터 신의 수호란\n 예삿일이 아닌 거야 6-40\n",
                            japanese = "これからわ神のしゆごとゆうものハ\nなみたいていなことでないそや",
                            english = "korekara wa Kami no shugo to yu mono wa\n" +
                                    "nami taitei na koto de nai zo ya",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 지금까지 없던 일만을 시작하니\n 참으로 어려운 일 6-41\n",
                            japanese = "いまゝてにない事ばかりはちめるわ\nなにをゆうのもむつかしき事",
                            english = "imamade ni nai koto bakari hajimeru wa\n" +
                                    "nani o yu no mo mutsukashiki koto",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이 세상을 창조한 진실을\n 누구도 아는 자는 없으리라 6-42\n",
                            japanese = "このよふをはちめかけたるしんぢつを\nたれかしりたるものハあるまい",
                            english = "kono yo o hajime kaketaru shinjitsu o\n" +
                                    "tare ka shiritaru mono wa arumai",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이제부터는 무슨 일이든 차츰차츰\n 일러줄 테니 거짓이라 생각 말라 6-43\n",
                            japanese = "これからハとのよな事もたん／＼と\nゆうてきかするうそとをもうな",
                            english = "korekara wa dono yona koto mo dandan to\n" +
                                    "yute kikasuru uso to omouna",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 인간을 창조할 때 인어와 흰뱀\n 이것을 못자리와 씨앗으로 삼아서 6-44\n",
                            japanese = "このものに月日たいない入こん\nたん／＼しゆごをしゑこんだで",
                            english = "ningen o hajime kaketa wa uo to mi to\n" +
                                    "kore nawashiro to tane ni hajimete",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이들의 몸 속에 月日이 들어가서\n 차츰차츰 수호를 가르친 거야 6-45\n",
                            japanese = "にんけんをはぢめかけたハうをとみと\nこれなわしろとたねにはじめて",
                            english = "kono mono ni Tsukihi tainai irikonde\n" +
                                    "dandan shugo oshie konda de",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이 자녀 수는 9억 9만 9천에\n 9백 9십 9인인 거야 6-46\n",
                            japanese = "このこかす九をく九まんに九せん人\n九百九十に九人なるそや",
                            english = "kono kokazu kuoku kuman ni kusen nin\n" +
                                    "kuhyaku kuju ni ku nin naru zo ya",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이 인수를 3일 3야에 잉태해서\n 3년 3개월 머물러 있었다 6-47\n",
                            japanese = "この人を三か三よさにやどしこみ\n三ねん三月とゝまりていた",
                            english = "kono nin o mikka miyosa ni yadoshi komi\n" +
                                    "san nen mi tsuki todomarite ita",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 그로부터 태어난 것은 5푼부터야\n 5푼 5푼으로 성인했다 6-48\n",
                            japanese = "それよりもむまれだしたハ五分からや\n五分五分としてせへぢんをした",
                            english = "soreyori mo umare dashita wa go bu kara ya\n" +
                                    "go bu go bu to shite sejin o shita",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이것에 한번 가르친 이 수호\n 같은 태내에 세 번 잉태했다 6-49\n",
                            japanese = "このものに一どをしゑたこのしゆごふ\nをなぢたいない三どやどりた",
                            english = "kono mono ni ichido oshieta kono shugo\n" +
                                    "onaji tainai sando yadorita",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이 세상의 진실한 신은 月日이니라\n 그 밖에는 모두 도구들인 거야 6-50\n",
                            japanese = "このよふのしんぢつの神月日なり\nあとなるわみなどふくなるそや",
                            english = "kono yo no shinjitsu no Kami Tsukihi nari\n" +
                                    "ato naru wa mina dogu naru zo ya",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 인간을 창조하려고 차츰차츰\n 끌어들여 사용한 그것에 신명을 6-51\n",
                            japanese = "にんけんをはしめよふとてたん／＼と\nよせてつこふたこれに神なを",
                            english = "ningen o hajime yo tote dandan to\n" +
                                    "yosete tsukota kore ni kamina o",
                            commentary = "二十九～五十一,지금까지 없던 일만을 일러주어서 만가지 구제의 즐거운근행을 가르친다. 이 신악근행 열사람의 근행인원 가운데는 태초 인간을 창조한 월일의 리를 받은 자도 있다. 이 월일은 자녀들의 즐거운 삶을 보기 위해 인간세계를 창조할 뜻을 세우고, 이자나기노미꼬또(남자추형종자의리)와 이자나미노미꼬또(여자추형묘상의리)를 이끌어 들여 이들에게 인간창조의 수호를 가르쳤다. 이들은 태초 진흙바다 가운데서 미꾸라지와 함께 섞여 놀던 인어와 흰뱀으로서, 자세히 살펴보니 인간의 얼굴을 하고 있는지라, 이것을 보고 인간창조를 생각해 낸 월일은 먼저 이들을 이끌어 들여 부부의 본으로 삼은 것이다. 월일은, '이들에게 도구들을 끌어넣어 차츰 수호를 가르친다면 필경 훌륭한 인간이 되리라'는 생각에서 쯔끼요미노미꼬또(지탱수호의리)와 구사니사즈찌노미꼬또(결합수호의리)를 이끌어 들여, 각각 인어와 흰뱀에게 끌어넣어서 남자의 도구와 뼈 몇 버팀의 도구, 그리고 여자의 도구와 피부 및 연결의 도구로 하고, 또 구모요미노미꼬또(수기승강수호의리)와 가시꼬네노미꼬또(풍기수호의리) 등을 이끌어 들여 각각 먹고 마시고 내고 들이는 것, 숨을 들이쉬고 내쉬는 것, 끌어내는 것, 끊는 것 들의 도구로 하여 마침내 인간을 창조하기 시작했는데, 이로부터 어버이신의 수호는 참으로 여간한 것이 아니었다. 어쨌든 지금까지 없던 인간을 창조하는 것이기 때문에 이 어려움은 참으로 대단한 것이었다. 어버이신의 인간창조 및 생성 발전에 대한 이같은 소호를 지금까지 누구 하나 아는 자가 없었으므로, 이제부터 이것을 자세히 일러주고자 하니 아무리 생소한 이야기일지라도 의심하지 말고 단단히 들어야 한다.\n 인간창조시 으뜸이 되는 도구는 인어와 흰뱀으로서, 이들을 씨앗과 못자리의 도구로 삼고 그 몸속에 월일이 들어가 수호를 가르쳤다. 즉, 으뜸인 터전에서 어버이신이 이자나기노미꼬또(남자추형종자의리)와 이자나미노미꼬또(여자추형묘상의리)를 부부의 본으로 삼아 9억 9만 9천9백9십9인의 자녀를 3일 3야에 걸쳐 잉태시키고, 3년 3개월 머무른 다음, 5푼으로 태어나게 해서 5푼5푼으로 성인시켰던 것이다 그리하여 한번 가르친 수호에 의해 같은 태내에 세 번 잉태, 다시 태어나 자라고 다시 태어나 자라기를 거듭한 끝에 마침내 다섯 자 인간이 되었다.\n 인간창조시 으뜸인 신은 월일로서 그 밖에는 모두 도구들이다. 월일이 도구들을 끌어 모아 인간창조의 수호를 가르친 다음, 이들 창조의 효능, 수호의 리에 각각 신명(神命)을 내렸다.\n 으뜸인 어버이란 월일로서 달님을 구니도꼬다찌노미꼬또(월덕수기의리), 햇님을 오모따리노미꼬또(일덕화기의리)라 일컫는다. 여기서 말하는 근행이란 신악근행으로서, 이것은 감로대를 둘러싸고 십주신의 역할을 맡은 열 사람의 근행인원이 올린다.(제 1호 10의 주석 참조)\n31의 노래 이하에서는 신악근행의 리를 밝히시는 한편, 어버이신님이 이 세상 인근을 창조하실 때의 고심을 깨닫게 하기 위해 태초의 이야기를 자세히 일러주고 계신다.\n 이 이야기는 어버이신님의 뜻을 시적(詩的)인 표현을 써서 가르쳐 주고 있는 만큼, 우리들은 이것을 형이하적(形而下的)으로 이해할 것이 아니라, 마음의 문을 열고 이 이야기의 근저에 숨겨져 있는 어버이신님의 인간창조의 진실을 깨달아 구제한줄기의 어버이마음을 납득해야 한다.(시적 표현에 대해서는 第一호 二十一의 노래의 리 참조)\n 이 세상 태초는 진흙바다로서 그 가운데는 월일만 계셨을 뿐이다. 어느 날 월일은 \n“우리만으로는 어무런 낙도 없으나 세계를 창조하고 인간을 만들어서 그들이 즐겁게 사는 모습을 보고 함께 즐기도록 하자.“\n고 의논하셨다.\n 그리하여 미꾸라지 등속이 많이 놀고 있는 진흙바다 속을 살펴보니, 그 가운데 인어와 흰뱀을 섞여 있었다. 그래서 다시 잘 살펴보니 그 모습과 살결이 월일이 만들려고 생각하신 인간과 흡사하므로,\n 이것을 바탕으로 하여 인간을 창조하려 하셨다. 그리하여 이들을 부르자 기뻐하며 일직선으로 월일을 향해 헤엄쳐 왔다. 월일은 이들에게 잘 이야기하여 납득을 시킨 뒤 인간창조를 위해 씨앗과 못자리의 도구로 쓰기로 하셨다.\n 다시 진흙바다 속을 살펴보니, 동남쪽에 등이 납작하여 잘 쓰러지지 않는 거북이 있었다. 이것을 불러들여 납득을 시킨 뒤 먹어서 그 마음씨를 알아본즉, 이것은 껍질이 튼튼하고 끈질긴 성질이 강하기 때문에 여자의 도구와 피부 및 인간이 태어난 뒤에 이 세상과의 연결의 도구로 쓰기로 하셨다. 이를 수호하는 신명을 구니사즈찌노미꼬또(결합수호의리)라 한다.\n 다음에 서북쪽을 보니, 위세 좋고 지탱하는 힘이 강한 범고래가 있었다. 이것을 불러들여 납득을 시킨 뒤 먹어서 그 마음씨를 알아보니, 이것은 위세가 당당하기 때문에 남자의 도구와 뼈 몇 버팀의 도구로 쓰기로 하셨다. 이를 수호하는 신명을 쯔끼요미노미꼬또(지탱수호의리)라 한다.\n 다음에 동쪽을 보니, 들고나는 것이 자유자재한 장어가 있었다. 이것을 불러들여 납득을 시킨 뒤, 먹어서 그 마음씨를 알아본 다음, 먹고 마시고 내고 들이는 도구로 쓰기로 하셨다. 이를 수호하는 신명을 구모요미노미꼬또라 한다.\n 다음에 서남쪽을 보니, 몸뚱이가 매우 넓적한 가자미가 있었다, 이것을 불러들여 납득시킨 뒤, 먹어서 그 마음씨를 알아본즉, 이것은 튼튼해서 잡아당겨도 좀처름 끊어지지 않으므로, 인간이 태어날 때 태내에서 그ㄱ어내는 도구로 쓰기로 하셨다. 이를 수호하는 신명을 오오또노베노미꼬또(인출수호의리)라 한다.\n 다음에 동북쪽을 보니 배가 부른 복어가 있었다. 이것을 불러들여 납득을 시킨 뒤, 먹어서 그 마음씨를 알아본즉, 이것은 식탐이 많아 대식(大食)을 해서 죽는 수도 있고, 또 이것을 잘못 먹어도 중독되어 죽는 수가 있기 때문에, 인간이 태어날 때는 산모와의 태연(胎緣)을 끊고, 출직할 때는 세상과의 연을 끈는 도구로 쓰기로 하셨다. 이를 수호하는 신명을 다이쇼꾸뗀노미꼬또(절단수호의리)라 한다.\n 이리하여 인간창조의 도구도 모두 갖추어졌으므로 드디어 이 세상 인간을 창조하시게 되었는데, 이때 어버이신님은 인어와 흰뱀에게 “자녀 수와 같은 연수가 지나면 으뜸인 터전에 데리고 와서 신으로 예배받게 할 것이니, 자녀들이 즐거운 놀이를 하는 것을 보고 함께 즐기도록 하자.”\n 고 약속하는 다음, 세계와 인간창조의 성업(聖業)에 착수하셨다. 즉 인어에게는 쯔끼요미노미꼬또(지탱수호의리)의 수호를 가르쳐 거기에 달님이 듭시고, 흰뱀에게는 구니사즈찌노미꼬또(결합수호의리)의 섭리를 가르쳐 거기에 햇님이 듭시어 각각 씨앗과 못자리로 삼으셨다. 이 남자의 본 씨앗을 이자나기노미꼬또(남자추형종자의리), 여자의 본 못자리를 이자나미노미꼬또(여자추형묘상의리)라 한다.\n 이리하여 어버이신 천리왕님은 이자나기노미꼬또(남자추형종자의리)와 이자나미노미꼬또(여자추형묘상의리)를 부부의 본으로 삼고 미꾸라지를 먹어서 그 마음씨를 알아본 다음, 인간의 씨앗으로 삼아 터전에서 3일 3야 동안에 9억 9만 9천9백9십9인의 자녀를 잉태케 하셨다. 이리하여 이자나미노미꼬또(여자추형묘상의리)는 3년 3개월 동안 터전에 머무른 다음, 75일 간에 걸쳐 자녀 모두를 낳으셨다. 최초에 태어난 것은 5푼이었는데, 5푼5푼으로 성인하여 99년 동안에 세 치까지 성인했을 때 모두 출직하고 말았다. 그리고 이때 이자나기노미꼬또(남자추형종자의리)도 은신하셨다.\n 그러나 한번 가르침을 받은 수호에 의해 이자나미노미꼬또(여자추형묘상의리)는 다시 태내에 처음 같은 자녀 수를 잉태하여 낳으셨는데, 이들도 5푼으로 태어나 5푼5푼으로 성인하여 99년 동안에 세치 5푼까지 성인한 다음 또다시 모두 출직했다. 그 후 이자나미노미꼬또는 세 번째 태내에 처음과 같은 자녀 수를 잉태하셨는데, 이번에도 5푼으로 태어나 5푼5푼으로 성인하여 99년 동안에 네 치까지 성인했을 때, 이자나미노미꼬또(여자추형묘상의리)는\n “이만큼 성인했으니 언젠가는 다섯 자 인간이 되리라”\n 하고 빙그레 웃으며 은신하셨다. 그리고 자녀들도 그 뒤를 따라 모두 출직하고 말았다.\n 그 후 어버이신님의 수호에 의해 인간은 벌레, 새, 짐승 등으로 8천 여덞 번 환생을 거듭했다. 그래서 인간은 어떤 동물의 흉내라도 가능한 것이다. 그리하여 8천 여덞 번의 환생이 끝나자 또다시 모두 출직하고, 최후에 암원숭이 한 마리만 남았는데, 그 태내에서 남자 다섯, 여자 다섯 모두 열 사람이 태어났다. 이때도 5푼으로 태어나서 5푼5푼으로 성인하여 여덞 치가 되었을 때, 진흙바다 가은데 높고 낮은 곳이 생기기 시작했고, 한자 여덞치로 성인했을 때, 바다와 육지와 구분되기 시작했다. 그리고 이때부터는 한배에 남자 하나, 여자 하나, 둘씩 태어났고, 더욱 성인하여 석자가 되었을 때는 말을 하기 시작했다. 그래서 지금도 세 살이 되면 말을 하기 시작하고 지혜도 생기게 되는 것이다. 석자가 된 후부터는 한재에 한 사람씩 태어났다. 그리고 먹을 것을 찾아 차츰 넒은 세계로 흩어지기 시작했다. 다섯 자가 되었을 때, 바다와 육지가 완전히 나누어지고 하늘과 땅이 정해졌으며, 해와 달도 뚜렷이 나타나게 되었다. 그리고 인간은 물속에서 나와 각각 가까운 육지로 올라와 살게 되었다. 이리하여 오늘날과 같이 세계 방방곡곡에 인간이 살게 된 것이다. 그러므로 세계 인류는 모두 어버이신님의 귀여운 자녀이고 인간끼리는 같은 형제자매이다. 그 동안 9억 8만년은 물속에서 살았다. 그리고 어버이신님은 육지에 올라온 후 6천년은 지혜를 주셨으며, 3천9백9십9년은 학문을 가르치셨다. 이처름 어버이신님은 차츰 인간을 가르쳐 열 가운데 아홉은 가르치셨으나, 마지막 한 가지는 아직 밝히지 않으셨다.\n 그러다 1838년 10월 26일, 마침내 태초에 약속한 자녀 수와 같은 연한이 도래하자, 이 세상 인간을 창조하신 어버이신 천리왕님이 으뜸인 터전에 하강하여 교조님을 월일의 현신으로 삼아 그 입을 통하여 마지막 한 가지인 최후의 가르침, 즉 세계 인류 구제를 위한 구극의 가르침을 일러주시게 되었다.\n 천리왕님이란 만물을 섭리하시는 어버이신님, 즉 이 세상과 인간을 창조하고 수호하는 으뜸인 신, 진실한 신님이시다. 그리고 이러한 어버이신님의 열 가지 수호의 리에 신명을 붙인 것이 십주신명(十住神名)이다. 십주신명은 구니도꼬다찌노미꼬또(월덕수기의리), 오모따리노미꼬또(일덕화기의리), 구니사즈찌노미꼬또(결합수호의리), 쯔끼요미노미꼬또(지탱수호의리), 구모요미노미꼬또(수기승강수호의리), 가시꼬네노미꼬또(풍기수호의리), 다이쇼꾸뗀노미꼬또(절단수호의리),오오또노베노미꼬또(일출수호의리), 이자니기노미꼬또(남자추형종자의리), 이자나미노미꼬또(여자추형묘상의리) 등이다.\n 현재 천리왕님의 신명은 터전에 내려져 있으며, 그 리로써 교조님은 존명(存命)한 채로 영원히 여기에 머무르고 계신다. 이것을 교조존명의 리라 일컫는다. 참으로 으뜸인 터전이야말로 천리왕님이 진좌하고 계시는 곳이며, 구제한줄기의 근원이고 본교 신앙의 생명이다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이자나기(남자추형종자)와이자나미(여자추형묘상)이 첫째 신\n 이것 데쇼꼬(씨앗)의 다이진구(못자리)이다 6-52\n",
                            japanese = "いざなぎといざなみいとが一の神\nこれてしよこの大じんくゝなり",
                            english = "Izanagi to Izanamii to ga ichi no Kami\n" +
                                    "kore Teshoko no Daijingu nari",
                            commentary = "五十二, 남자추형종자의리와 여자추형묘상의리는 인간창조의 첫째가는 신이다"
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 또 앞으로는 무엇인가 차츰차츰 일러주지만\n 지금까지 모르던 일뿐이야 6-53\n",
                            japanese = "またさきハなにかたん／＼とくけれど\nいまゝてしらん事ばかりやで",
                            english = "matasaki wa nanika dandan toku keredo\n" +
                                    "imamade shiran koto bakari ya de",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 앞으로는 무슨 말을 해도 인간을\n 창조해 낸 일뿐이야 6-54",
                            japanese = "このさきハなにをゆうてもにんげんを\nはぢめかけたる事ばかりやで",
                            english = "konosaki wa nani o yutemo ningen o\n" +
                                    "hajime kaketaru koto bakari ya de",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " \n12월 21일 이후의 이야기\n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이 세상을 창조한 집터이니라\n 인간을 창조한 으뜸인 어버이니라 6-55\n",
                            japanese = "このよふをはぢめだしたるやしきなり\nにんけんはじめもとのをやなり",
                            english = "kono yo o hajime dashitaru yashiki nari\n" +
                                    "ningen hajime moto no Oya nari",
                            commentary = "五十五,터전은 태초에 인간이 잉태되었던 본고장이며, 한편 교조님은 온 세상 사람들의 어버이인 혼의 인연을 지니신 분이다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 月日이 그것을 살펴보고 하강했다\n 무엇이든 만가지 알리고 싶어서 6-56\n",
                            japanese = "月日よりそれをみすましあまくだり\nなにかよろづをしらしたいから",
                            english = "Tsukihi yori sore o misumashi amakudari\n" +
                                    "nanika yorozu o shirashitai kara",
                            commentary = "五十六,어버이신은 터전의 인연, 교조 혼의 인연을 살펴보고 하강하게 되었으며, 교조의 입을 빌려 가르치는 것은 인간들에게 일체 만가지를 알려 주고 싶기 때문이다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 진실로 月日이 생각하는 바는\n 각각의 몸을 받아들이게 되면 6-57\n",
                            japanese = "しんぢつに月日の心をもうにわ\nめへ／＼のやしろもろた事なら",
                            english = "shinjitsu ni Tsukihi no kokoro omou niwa\n" +
                                    "meme no yashiro morota koto nara",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 그로부터는 자유자재로 언제라도\n 생각대로 이야기할 것인데 6-58\n",
                            japanese = "それよりもぢうよぢざいにいつなりと\nをもうまゝなるはなしゝよもの",
                            english = "soreyori mo juyojizai ni itsu nari to\n" +
                                    "omou mama naru hanashi shiyo mono",
                            commentary = "五十七, 五十八,어버이신이 참으로 생각하는 바는, 각각의 몸을 받아들여 근행인원이 모두 갖추어지게 되면, 그때부터는 언제까지나 생각대로 이야기를 하려는 것이다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 지금까지도 月日의 현신으로 확실히\n 받아들이고 있었지만 침울해 있었다 6-59\n",
                            japanese = "いまゝでも月日のやしろしいかりと\nもろてあれどもいづみいたなり",
                            english = "imamade mo Tsukihi no yashiro shikkari to\n" +
                                    "morote aredomo izumi ita nari",
                            commentary = "五十九,지금까지 어버이신의 현신으로서 교조를 받아들이고는 있었지만 아무튼 침울해 있었다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이번에는 확실히 이 세상에 나타나서\n 무엇이든 만가지 모두 일러준다 6-60\n",
                            japanese = "このたびハたしかをもていあらハれて\nなにかよろつをみなゆてきかす",
                            english = "konotabi wa tashika omote i arawarete\n" +
                                    "nanika yorozu o mina yute kikasu",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 지금까지는 어두운데에 있었기에\n 아무것도 보이지 않았지만 6-61\n",
                            japanese = "いままでハみすのうぢらにいたるから\nなによの事もみへてなけれど",
                            english = "Imamade wa mizu no uchira ni itaru kara\nNaniyo no koto mo miete nakeredo",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이번에는 밝은 곳으로 나왔으니\n 무엇이든 곧 보이리라 6-62\n",
                            japanese = "このたびハあかいところいでたるから\nとのよな事もすぐにみゑるで",
                            english = "konotabi wa akai tokoro i detaru kara\n" +
                                    "dono yona koto mo suguni mieru de",
                            commentary = "六十一, 六十二,어두운 테란 어버이신님이 위엄을 나타내지 않고 보료하고 계셨다는 뜻으로서, 구체적으로는 교조님이 지금까지 검은 예복을 입고 계신 것을 가리킨다.\n 밝은 곳이란 어버이신님이 몸소 세상 밖으로 모습을 나타내신다는 뜻으로, 구체적으로 교조님이 야마무라고뗑에 다녀오신 후부터 붉은 옷을 입으신 것을 가리킨다.(제3호 42 주석, 제5호 56, 57의 주석 참조)"
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이 붉은 옷을 어떻게 생각하는가\n 안에　月日이 들어 있는 거야 6-63\n",
                            japanese = "このあかいきものをなんとをもている\nなかに月日がこもりいるそや",
                            english = "kono akai kimono o nanto omote iru\n" +
                                    "naka ni Tsukihi ga komori iru zo ya",
                            commentary = "六十三,이 붉은 옷을 무엇이라 생각하는가. 이것은 교조의 몸 속에 어버이신이 들어있다는 증거이다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 지금까지도 月日의 생각대로였지만\n 날이 오지 않아서 묵과해 두었다 6-64\n",
                            japanese = "いまゝでも月日のまゝであるけれど\nひがきたらんでみゆるしていた",
                            english = "imamade mo Tsukihi no mama de aru naredo\n" +
                                    "hi ga kitarande miyurushite ita",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이번에는 벌써 충분히 날이 차서\n 무엇이든 만가지를 마음대로 하리라 6-65\n",
                            japanese = "このたびハもふぢうふんにひもきたり\nなにかよろづをまゝにするなり",
                            english = "konotabi wa mo jubun ni hi mo kitari\n" +
                                    "nanika yorozu o mamani suru nari",
                            commentary = "六十四, 六十五,이 세상 인간을 창조한 아래 지금까지 어버이신은 만사를 자유자재를 섭리해왔지만, 사람들이 무슨 말을 하거나 무슨 일을 하더라도 그냥 묵과하고 있었던 것은, 아작 사람들의 마음이 성인의 경지에 이르지 않았기 때문이었다. 그러나 이제는 마음이 충분히 성인의 경지에 이르릈기 때문에, 무슨 일이건 모두 어버이신이 자유 자재한 섭리를 나타낼 것이다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 그것을 모르고서 높은산에서는 무엇이든\n 어떻게 생각하고 멋대로 하는가 6-66\n",
                            japanese = "それしらす高山にてハなにもかも\nなんとをもふてまゝにするぞや",
                            english = "sore shirazu takayama nitewa nanimo kamo\n" +
                                    "nanto omote mamani suru zo ya",
                            commentary = "六十六,이처럼 어버이신이 자유자재한 섭리를 나타내고 있는 것도 모르고 윗사람들은 자신의 부귀와 권세만을 맏고 무엇이든지 멋대로 하는 자가 많은데, 도대체 이것을 어떻게 생각하고 하는 일인가."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 무슨 일이든 여기에는 인간의\n 마음이란 추호도 있다고 생각 말라 6-67\n",
                            japanese = "なに事もこのところにハにんけんの\n心ハさらにあるとおをもうな",
                            english = "nani goto mo kono tokoro niwa ningen no\n" +
                                    "kokoro wa sarani aru to omouna",
                            commentary = "六十七,교조는 어버이신의 현신이기 때문에, 교조가 하는 말이나 하는 일에는 추호도 인간 마음이 섞여 있다고 생각지 마라.\n 이 노래는 교조님이 인간의 몸을 하고 계시기 때문에, 자칫하면 그 입을 통해 하시는 천계의 소리마저 단순한 인간의 소리로 여기고, 또 교조님이 하시는 일도 단순한 인간의 행위로 생각하기 쉬운 사람들에게 경고하신 말씀이다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 어떤 말을 하는 것도 붓끝으로 쓰는 것도\n 月日의 마음으로 지시할 뿐이야 6-68\n",
                            japanese = "どのよふな事をゆうにもふでさきも\n月日の心さしすばかりで",
                            english = "dono yona koto o yu nimo fudesaki mo\n" +
                                    "Tsukihi no kokoro sashizu bakari de",
                            commentary = "六十八,교조가 어떤 말을 하는 것도, 또 붓으로 친필을 기록하는 것도 모두 어버이신의 지시에 따라 하는 것이므로, 거기에는 조금도 인간마음이 섞여 있지 않는 것이다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 높은산은 무슨 말을 하거나 생각하거나\n 모두 인간마음뿐이야 6-69\n",
                            japanese = "高山ハなにをゆうてもをもうにも\nみなにんけんのこころばかりで",
                            english = "takayama wa nani o yutemo omou nimo\n" +
                                    "mina ningen no kokoro bakari de",
                            commentary = "六十九,윗사람들은 무슨 말을 하거나 무슨 생각을 하더라도 그것은 모두 인간마음에서 제멋대로 하는 말, 하는 생각일 뿐이다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 月日이 붙인 이름을 없애 버려\n 이 섭섭함을 어떻게 생각하는가 6-70\n",
                            japanese = "月日よりつけたなまいをとりはらい\nこのさんねんをなんとをもうぞ",
                            english = "Tsukihi yori tsuketa namai o toriharai\n" +
                                    "kono zannen o nanto omou zo",
                            commentary = "七十,어버이신이 붙인 신명을 단지 인간마음에서 없애 버리려고 하는 데 대한 어버이신의 섭섭한 마음을 모두들은 어떻게 생각하고 있는가.\n 이 노래는 1874년 음력 11월 17일에 나라중교원(奈良中教院)으로부터 소환을 받고“천리왕님리안 신은 없다. 따라서 앞으로는 이러한 신을 섬기지 않도록 하라.”는 엄명을 받은 일에 대한 말씀이다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 진실한　月日의 노여움과 섭섭함은\n 예사로운 것이 아니라고 생각하라 6-71\n",
                            japanese = "しんちづの月日りいふくさんねんわ\nよいなる事でないとをもゑよ",
                            english = "shinjitsu no Tsukihi rippuku zannen wa\n" +
                                    "yoi naru koto de nai to omoe yo",
                            commentary = "신명을 없애 버리려는 데 대한 진실한 어버이신의 노여움과 섭섭함은 결코 예사로운 것이 아님을 알아야 한다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 지금까지는 높은산이라 뽐내며\n 무엇이든 만가지를 멋대로 했지만 6-72\n",
                            japanese = "いまゝでハ高い山やとはびかりて\nなにかよろづをまゝにしたれど",
                            english = "imamade wa takai yama ya to habikarite\n" +
                                    "nanika yorozu o mamani shitaredo",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이제부터는 月日이 대신 마음대로 한다\n 무슨 일이든 흉내를 내어 보라 6-73\n",
                            japanese = "これからハ月日かハりてまゝにする\nなにかの事をまねをしてみよ",
                            english = "korekara wa Tsukihi kawarite mamani suru\n" +
                                    "nanika no koto o mane o shite miyo",
                            commentary = "이 노래는 당시 관헌이 자신들의 권세를 믿고 본교에 압박을 가하며 걸핏하면 멋대로 간섭을 해 왔으나, 이제부터는 어버이신이 자유자재한 섭리로써 마음대로 할 것이니, 무슨 일이든지 흉내를 낼 수 있으면 한번 내 보라고 하신 말씀이다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이곳에서 무슨 말을 하든지 행하든지\n 月日의 생각만 있을 뿐이야 6-74\n",
                            japanese = "このところなにをゆうにもなす事も\n月日のをもう事ばかりやで",
                            english = "kono tokoro nani o yu nimo nasu koto mo\n" +
                                    "Tsukihi no omou koto bakari ya de",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이제부터는 月日의 섭섭한 마음을\n 풀어 버릴 준비한 하는 거야 6-75\n",
                            japanese = "これからハ月日の心ざんねんを\nはらするもよふばかりするそや",
                            english = "korekara wa Tsukihi no kokoro zannen o\n" +
                                    "harasuru moyo bakari suru zo ya",
                            commentary = "이제부터는 어버이신의 섭섭한 마음을 어떻든 풀어 볼 작정이다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 앞으로는 어떤 티끌이 나타나더라도\n 이것을 질병이라고는 전혀 생각 말라 6-76\n",
                            japanese = "このさきハどのよなほこりたつとても\nこれをやまいとさらにをもうな",
                            english = "konosaki wa dono yona hokori tatsu totemo\n" +
                                    "kore o yamai to sarani omouna",
                            commentary = "앞으로는 어떤 신상이 생기더라도 이것을 결코 질병이라 생각해서는 안 된다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 지금까지도 月日의 섭섭함 첩첩이\n 쌓여 있는 것을 갚음하는 거야 6-77\n",
                            japanese = "いまゝでも月日さんねん山／＼に\nつもりてあるをかやしするぞや",
                            english = "imamade mo Tsukihi zannen yamayama ni\n" +
                                    "tsumorite aru o kayashi suru zo ya",
                            commentary = "이것은 전부터 어버이신의 마음에 첩첩이 쌓여 있는 섭섭함을 갚음하는 것이다.\n 갚음이란 보복이라는 뜻이지만, 여기서는 결코 그런 뜻으로 사용된 것이 아니다. 왜냐하면, 신상은 어버이신님이 우리 안간들의 그릇된 마음을 일깨워 주시려는 어버이마음의 발로이기 때문이다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 지금까지도 갚음한다고 일러주었지만\n 어떤 것일까고 생각하고 있었다 6-78\n",
                            japanese = "いまゝでもかやしとゆうてといたれど\nなんの事やとをもていたなり",
                            english = "imamade mo kayashi to yute toitaredo\n" +
                                    "nanno koto ya to omote ita nari",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 진실로 갚음이란 이런 거야\n 높은산은 모두들 명심해 두라 6-79\n",
                            japanese = "しんぢつにかやしとゆうハこの事や\n高山ハみなしよちしていよ",
                            english = "shinjitsu ni kayashi to yu wa kono koto ya\n" +
                                    "takayama wa mina shochi shite iyo",
                            commentary = "지금까지도 갚음한다고 가끔 일러주었으나, 사람들은 그것이 무엇을 뜻하는지 몰랐었다. 그런데 어버이신이 일러준 갚음의 참뜻은 이렇게 신상과 사정으로 알린다는 의미이므로, 윗사람들은 모두 이것을 잘 명심해서 마음가짐에 잘못이 없도록 해야 한다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이 세상은 진흙바다였는데\n 거기에 月日이 있었을 뿐이다 6-80\n",
                            japanese = "このよふわどろうみなかの事なるし\nなかに月日がいたるまでなり",
                            english = "kono yo wa doroumi naka no koto narushi\n" +
                                    "naka ni Tsukihi ga itaru made nari",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 月日이 진실로 착상한 것은\n 무엇인가 세상을 창조해 보았으면 6-81\n",
                            japanese = "月日よりしんぢつをもいついたるわ\nなんとせかいをはじめかけたら",
                            english = "Tsukihi yori shinjitsu omoi tsuitaru wa\n" +
                                    "nanto sekai o hajime kaketara",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 없던 세상을 창조하기란 어렵다\n 어쨌든 도구를 찾아낼 준비를 6-82\n",
                            japanese = "ないせかいはじめかけるハむつかしい\nなんとどふぐをみたすもよふを",
                            english = "nai sekai hajime kakeru wa mutsukashii\n" +
                                    "nanto dogu o midasu moyo o",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 살펴보니 그 속에 미꾸라지도 인어와 흰뱀도\n 다른 것들도 있었다 6-83\n",
                            japanese = "みすませばなかにどぢよもうをみいも\nほかなるものもみへてあるなり",
                            english = "misumaseba naka ni dojo mo uo mii mo\n" +
                                    "hoka naru mono mo miete aru nari",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 그것들을 모두 이끌어 들여 의논하여\n 인간 창조의 수호를 시작하면 6-84\n",
                            japanese = "そのものをみなひきよせてたんぢやい\n　にんけんしゆごはぢめかけたら",
                            english = "sono mono o mina hikiyosete danjiyai\n" +
                                    "ningen shugo hajime kaketara",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 없던 세상 창조하려고 이 月日\n 차츰차츰 마음 기울인 까닭에 6-85\n",
                            japanese = "ないせかいはじめよふとてこの月日\nたん／＼心つくしたるゆへ",
                            english = "nai sekai hajime yo tote kono Tsukihi\n" +
                                    "dandan kokoro tsukushi taru yue",
                            commentary = "이 세상 태초는 진흙바다였는데 거기에 월인만 있었다. 그때 월일은 “우리만으로는 아무런 낙도 없다. 무언가 인간 세계라도 한번 만들어 볼까.”\n 하고 의논했다. 그러나 없던 세계를 만들기란 참으로 어려운 일이었다. 그래서 어떻든 인간창조에 사용할 도구들을 찾을 생각으로 진흙바다 속을 가만히 살펴보니 그 가운데는 미꾸라지, 인어, 흰뱀 등을 비롯하여 다른 여러 도구들이 눈에 띄었다. 그래서 그들은 모두 이끌어 들여 납득시킨 다음 마침내 인간을 창조하기 시작했다. 이처럼 어버이신이 차음 마음을 기울여서 없던 세계 없던 인간을 창조했기 때문에, 오늘날과 같은 인간이 있게 된 것이다.\n제六호 二十七～五十一 및 총주 참조."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이 길을 아는 자는 전혀 없다\n 月日의 섭섭함 어떻게 생각하는가 6-86\n",
                            japanese = "このみちをしりたるものハさらになし\n月日ざんねんなんとをもうぞ",
                            english = "kono michi o shiritaru mono wa sarani nashi\n" +
                                    "Tsukihi zannen nanto omou zo",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이토록 생각해서 창조한 이 세상\n　月日의 마음 얼마나 섭섭하랴 6-87\n",
                            japanese = "こらほどにをもてはじめたこのせかい\n月日の心なんとざんねん",
                            english = "kora hodoni omote hajimeta kono sekai\n" +
                                    "Tsukihi no kokoro nanto zannen",
                            commentary = "어버이신이 이토록 마음을 기울여서 이 세상 인간을 창조한 과정을 아무도 아는 자가 없다는 것은 참으로 섭섭한 일이 아닐 수 없다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 月日이 차츰차츰 마음 기울여\n 그 덕택으로 되어진 인간이니라 6-88\n",
                            japanese = "月日よりたん／＼心つくしきり\nそのゆへなるのにんけんである",
                            english = "Tsukihi yori dandan kokoro tsukushi kiri\n" +
                                    "sono yue naru no ningen de aru",
                            commentary = "인간들은 자기 의사로 태어나고, 자기 힘으로 살고 있다고 생각하고 있으나 그런 것이 아니다. 어버이신이 온갖 마음을 기울이고 애를 쓴 덕택으로 인간이 창조된 것이다.\n 어버이신님은 인간을 창조하실 때뿐만 아니라, 지금도 끊임없이 인간들을 위해 수호하고 계심을 잊어서는 안된다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 그것을 모르고서 지금 높은산은\n 모두들 뽐내고 멋대로 하고 있다 6-89\n",
                            japanese = "それしらす今のところハ高山ハ\nみなはびかりてまゝにしている",
                            english = "sore shirazu ima no tokoro wa takayama wa\n" +
                                    "mina habikarite mamani shite iru",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 月日은 이것이 제일 섭섭해\n 어떤 갚음을 할지 모르는 거야 6-90\n",
                            japanese = "この月日大一これがざんねんな\nどんなかやしをするやしれんで",
                            english = "kono Tsukihi daiichi kore ga zannen na\n" +
                                    "donna kayashi o suru ya shiren de",
                            commentary = "지금 윗시람들은 어버이신이 그처럼 마음을 기울이고 애를 써서 인간을 창조한 사실을 전혀 모른 채 저마다 뽐내며 멋대로 행동하고 있는데, 어버이신으로서는 이것처럼 섭섭한 일이 없으므로 앞으로 어떤 갚음을 할는지 모른다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이 세상 산사태도 뇌성벽력도\n 지진도 태풍도 月日의 노여움 6-91\n",
                            japanese = "このせかい山ぐゑなそもかみなりも\nぢしんをふかぜ月日りいふく",
                            english = "kono sekai yamague nazo mo kaminari mo\n" +
                                    "jishin okaze Tsukihi rippuku",
                            commentary = "이 세상의 천재지변, 즉 산 사태, 낙뢰, 지진, 태풍 등과 같은 재액은 어버이신의 노여움이 나타난 것으로서, 이를 통해 어버이신은 사람들이 마음을 반성하도록 촉구하는 것이다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 아무리 큰 신사 높은산도 방심 말라\n 어느 때 月日 뛰어나갈는지 6-92\n",
                            japanese = "どのよふなたいしや高山ゆたんしな\nなんとき月日とんてゞるやら",
                            english = "dono yona taisha takayama yudan shina\n" +
                                    "nandoki Tsukihi tondederu yara",
                            commentary = "큰 신사 높은 산이란 당시 권세를 휘둘러 본교를 탄압한 사람들을 말한다. 이 노래는 제멋대로 권세를 휘둘러 본교를 탄압해 온 사람들도 마음을 반성하지 않으면 언제 어느 때 어버이신님이 뛰어 나가 어떤 일을 할는지 모른다는 말씀이다.(제5호 56, 57의 주석 참조)\n"
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 사람들은 모두 제 몸 조심하라\n 月日 조금도 가차없는 거야 6-93\n",
                            japanese = "一れつハみな／＼わがみきをつけよ\n月日ゑんりよわさらにないぞや",
                            english = "ichiretsu wa minamina waga mi ki o tsuke yo\n" +
                                    "Tsukihi enryo wa sarani nai zo ya",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 무엇이든 철저히 미리 알려서\n 그로부터 시작하는 月日의 일 6-94\n",
                            japanese = "なにもかもせへいゝバいにことわりて\nそれからかゝる月日しことを",
                            english = "nanimo kamo seeippai ni kotowarite\n" +
                                    "sorekara kakaru Tsukihi shigoto o",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 무슨 일이든 원망스럽게 생각 말라\n 모두 각자 제 몸을 원망하라 6-95\n",
                            japanese = "とのよふな事もらみにをもうなよ\nみなめへ／＼のみうらみである",
                            english = "dono yona koto mo urami ni omouna yo\n" +
                                    "mina meme no miurami de aru",
                            commentary = "어버이신은 무슨 일이든 충분히 일러준 다음에 시작하므로, 설사 무슨 일이 생기더라도 결코 어버이신을 원망해서는 안된다. 그것은 모두 각자 제멋대로 한데서 생겨난 일이므로 자신을 원망해야 할 것이다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이 이야기 차츰차츰 되풀이 일러주었다\n 이것 단단히 분간해 들어 다오 6-96\n",
                            japanese = "このはなしたん／＼くどきつめてある\nこれしいかりときゝわけてくれ",
                            english = "kono hanashi dandan kudoki tsumete aru\n" +
                                    "kore shikkari to kikiwakete kure",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 인간들은 모두 각자의 마음에 따라\n 月日 분간하고 있다고 생각하라 6-97\n",
                            japanese = "一れつハみなめへ／＼のむねしたい\n月日みハけているとをもゑよ",
                            english = "ichiretsu wa mina meme no mune shidai\n" +
                                    "Tsukihi miwakete iru to omoe yo",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 月日은 진실한 마음을 살펴보고\n 받아들이는 대로 갚음을 하는 거야 6-98\n",
                            japanese = "月日よりしんぢつ心みさだめて\nうけとりしたいかやしするなり",
                            english = "Tsukihi yori shinjitsu kokoro misadamete\n" +
                                    "uketori shidai kayashi suru nari",
                            commentary = "어버이신은 사람들의 진실한 마음을 살펴본 다음, 마음 그대로 모두 갚음을 한다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 지금까지는 무슨 말을 하거나 생각하거나\n 모두 인간마음뿐이야 6-99\n",
                            japanese = "いまゝでハなにをゆうてもをもふても\nみなにんけんの心はかりで",
                            english = "imamade wa nani o yutemo omotemo\n" +
                                    "mina ningen no kokoro bakari de",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이제부터는 좋은 일이나 나쁜 일이나\n 그대로 곧 갚음을 하는 거야 6-100\n",
                            japanese = "これからハよき事してもあしきでも\nそのまゝすぐにかやしするなり",
                            english = "korekara wa yoki koto shite mo ashiki demo\n" +
                                    "sono mama suguni kayashi suru nari",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 지금까지는 무엇이든 용서해 주었지만\n 이제 앞으로는 용서 없는 거야 6-101\n",
                            japanese = "いまゝでハなにかさとりもありたけど\nもふこれからハさとりないぞや",
                            english = "imamade wa nanika satori mo aritakedo\n" +
                                    "mo korekara wa satori nai zo ya",
                            commentary = "어버이신이 하강하여 이 가르침을 일러주기 전에는 아직 마음이 성인되지 않았기 때문에, 모두 제멋대로 인간마음을 써 왔어도 가만히 보고만 있었다. 그러나 이제부터는 어버이신이 좋은 일이나 나쁜 일이나 모두 그대로 가차없이 갚음을 한다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이 세상의 진실한 어버이는 月日이니라\n 무엇이든 만가지를 수호하는 거　6-102\n",
                            japanese = "このよふのしんぢつのをや月日なり\nなにかよろづのしゆこするぞや",
                            english = "kono yo no shinjitsu no Oya Tsukihi nari\n" +
                                    "nanika yorozu no shugo suru zo ya",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 앞으로는 무슨 말을 해도 거짓은 없다\n 모두 진실이라 생각하고 잘 들어라 6-103\n",
                            japanese = "このさきハなにをゆうてもうそハない\nみなしんぢつとをもてきゝわけ",
                            english = "konosaki wa nani o yutemo uso wa nai\n" +
                                    "mina shinjitsu to omote kikiwake",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 무슨 일이든 月日이 진실로\n 생각해서 시작한 일뿐이야 6-104\n",
                            japanese = "どのよふな事でも月日しんぢつに\nをもてはじめた事ばかりやで",
                            english = "dono yona koto demo Tsukihi shinjitsu ni\n" +
                                    "omote hajimeta koto bakari ya de",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 지금까지는 질병이라면 의사다 약이다 하며\n 모두들 걱정하고 있었지만 6-105\n",
                            japanese = "いまゝでハやまいとゆへばいしやくするり\nみなしんバいをしたるなれども",
                            english = "imamade wa yamai to yueba isha kusuri\n" +
                                    "mina shinpai o shitaru naredomo",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이제부터는 아픔도 괴로움도 부스럼도\n손과 손춤으로 모두 구제할 테야 6-106\n",
                            japanese = "これからハいたみなやみもてきものも\nいきてをどりでみなたすけるで",
                            english = "korekara wa itami nayami mo dekimono mo\n" +
                                    "Iki Teodori de mina tasukeru de",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이 구제 지금까지는 모르는 일이지만\n 이제부터 앞으로는 징험해 보라 6-107\n",
                            japanese = "このたすけいまゝでしらぬ事なれど\nこれからさきハためしゝてみよ",
                            english = "kono tasuke imamade shiranu koto naredo\n" +
                                    "korekara saki wa tameshi shite miyo",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 아무리 어려운 질병도\n 진실한 숨으로 구제하리라 6-108\n",
                            japanese = "どのよふなむつかしきなるやまいでも\nしんぢつなるのいきでたすける",
                            english = "dono yona mutsukashiki naru yamai demo\n" +
                                    "shinjitsu naru no Iki de tasukeru",
                            commentary = "지금까지는 질병이 생기면 의사다, 약이다 하며 걱정만 하고 있었으나, 원래 질병이란 사람들의 그릇된 마음가짐을 알려서 그것을 바꾸게 하려는 어버이신님의 깊은 뜻이므로, 조용히 반성하여 어버이신님의 뜻이 무엇인지 잘 깨닫도록 해야 한다. 약으로써는 비록 질병은 고칠지 모르나 그 질병이 생기는 근본인 마음은 고칠 수 없는 것이다. 그리고 마음을 고치지 않으면 언제까지나 어버이신님의 의도에 따를 수도 없고, 또 즐거운 삶을 누릴 수 있는 세상도 오지 않는다. 그러므로 사람들의 마음을 고치는 것이 어버이신님의 뜻이기 때문에, 어버이신님의 알림은 비단 신상으로만 나타나지 않고 괴로운 사정으로도 나타나게 된다. 이 리를 충분히 깨달아 마음을 고치면, 어버이신닌은 그 성진실한 마음을 받아들여 어떤 신상이나 사정도 도와주신다. 그러므로 아무리 의사가 손을 뗀 난병이라 할지라도 본인의 마음에 따라 숨과 손춤에 의해 깨끗이 수호받을 수가 있는 것이다.\n 그러나 신상구제는 어버이신님의 의도를 실현해 가는 하나의 과정일 뿐, 궁극적 목적은 마음을 바꾸는 데 있으므로, 의약을 쓰는 것도 무방하나, 한편으로는 왜 질병에 걸리게 되었는가 하는 근본원일을 잘 깨달아야 한다. 원래 의술도 지금까지 어버이신님이 가르쳐 주신 것이며, 약도 어버이신님이 내려 주시는 혜택이므로, 이것을 이용할 때는 어버이신님의 은혜임을 깊이 생각하고 감사해야 한다.\n 교조님도 의약은 불필요한 것이라고 말씀하신 적이 없다. 참으로 어버이신님의 마음은 보다 더 넓고 깊은 데 있는 것이다. 그러므로 질병은 근본이 되는 마음을 고치도록 하기 위한 가르침임을 몰라서는 어버이신님의 뜻을 따를 수가 없다. 이 점에 틀림이 억도록 하기 위해 어버이신님은 다음과 같은 지도말씀으로써 간전히 깨우쳐 주고 계신다.\n 1890년 7월 7일 오전 10시 반\n “자아 자아, 하나도 어려울 것 없다. 어려운 일 하라고는 안한다. 잘 분간하라. 이런 일도 저런 일도 인간마음으로 어렵게 하고 있다. 어디서나 다 이러니 지금 당장 손을 쓰지 않으면 안된다. 원래부터 의사는 필요 없다. 약도 먹을 필요 없다는 말은 이 길의 가르침에는 없는 거야. 원래는 의사한테도 가보고 약도 먹고 하여, 끝내 의사가 손뗀 사람을 도우려는 것으로, 누구에게도 의사에게 갈 필요 없다. 약도 먹을 필요 없다는 말은 한 바가 없는데 어디서 나온 말이냐, 손뗀 사람을 돕는 것은 누구도 뭐라고 하지 않겠지. 신에게 도움받은 것은 이 길의 시작과 같은 것, 모두 걷기 쉬운 길을 어렵게 걷고 있다. 지금으로서는 어렵다고 하는데, 그런 마음으로는 아무것도 안된다. 지금 어렵다고 해서는 아무것도 안된다. 우선 도리에 따라 행해야 한다는 생각이 리에 어긋나기 때문에 아무것도 안된다. 이래 가지고서는 교회가 잘 다스려질 리 없겠지, 규약 규약이라 한다. 교회 규약에 따라 행하려 해서는 이 길은 다스려지지 않는다. 이 길을 바꾸기 때문에 저마다 빠져나갈 수 없는 길을 만들게 된다. 이래라저래라고는 말하지 않는다. 금방 다스려진다. 여차할 경우에는 곧 다스려 준다. 어려운 것은 잠깐 동안이야. 이것만 명심하라, 곳곳마다 야릇한 소문이 나돌고 있다. 한 사람의 잘못된 결정으로 만사람 버려서는 안된다.”\n 숨이란 입김을 세번 불어서 전하는 수훈을 말한다.\n 손춤이란 손을 흔들어서 전하는 수훈을 말한다. 이것은 '악한 것 제거 도와주소서'하고 부르며 전한디.\n 여기서는 이 두 가지만 들었지만, 이 외에도 물수훈, 식물수훈 등이 있다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 月日은 진실한 마음을 살펴보고\n 어떤 수호도 한다고 생각하라 6-109\n",
                            japanese = "月日よりしんじつ心みさためて\nいかなしゆこふもするとをもゑよ",
                            english = "Tsukihi yori shinjitsu kokoro misadamete\n" +
                                    "ikana shugo mo suru to omoe yo",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 갓난아이 마마도 홍역도 하지 않고\n 앓지 않고 죽지 않고 살아가게 되면 6-110\n",
                            japanese = "むまれこふほふそはしかもせんよふに\nやますしなすにくらす事なら",
                            english = "umare ko hoso hashika mo sen yoni\n" +
                                    "yamazu shinazu ni kurasu koto nara",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 단단히 들어라 어떤 자유자재를 해도\n 月日의 마음인 거야 6-111\n",
                            japanese = "しかときけいかなぢうよふするとても\n月日の心ばかりなるぞや",
                            english = "shikato kike ikana juyo suru totemo\n" +
                                    "Tsukihi no kokoro bakari naru zo ya",
                            commentary = "갓난아이가 마마도 홍역도 하지 않고, 또 사람이 질병에 걸리지도 요절도 하지 않고 살아가게 된다면 그보다 더한 행복은 없을 것이다. 그러나 이같은 자유자재한 수호를 하는 것도 모두 어버이신의 마음인 것이다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 지금까지도 대체로 되풀이 일러주었지만\n 아직 못다 일러준 月日의 의도 6-112\n",
                            japanese = "いまゝでもたいてくどきもといたれど\nまだゆいたらん月日をもわく",
                            english = "imamade mo taite kudoki mo toitaredo\n" +
                                    "mada yuitaran Tsukihi omowaku",
                            commentary = "지금까지 무슨 일이건 대체로 일러주었으나 아직 어버이신의 의도만은 모두 일러주지 않았다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이번에는 무엇인가 月日의 섭섭함\n 쌓여 있으니 모두 말해 둔다 6-113\n",
                            japanese = "このたびハなにか月日のざんねんを\nつもりあるからみなゆうてをく",
                            english = "konotabi wa nanika Tsukihi no zannen o\n" +
                                    "tsumori aru kara mina yute oku",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이곳에서 구제한줄기를 저지당해\n 어떻든 갚음을 하지 않고서는 못 배겨 6-114\n",
                            japanese = "このところたすけ一ぢよとめられて\nなんてもかやしせすにいられん",
                            english = "kono tokoro tasuke ichijo tomerarete\n" +
                                    "nandemo kayashi sezu ni iraren",
                            commentary = "이 터전에서 시작한 구제한줄기의 길에 대해 간섭하며 저지하려 드는 것은 어버이신으로서는 참으로 섭섭함을 금할 길이 없다. 그러므로 어떻든 이에 대한 갚음을 하지 않고서는 베길 수가 없는 것이다.\n제6호 70수의 주석 참조."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이 갚음 큰 신사 높은산 무너뜨릴 테니\n 모든 사람들은 명심해 두라 6-115\n",
                            japanese = "このかやしたいしや高山とりはらい\nみな一れハしよちしていよ",
                            english = "kono kayashi taisha takayama toriharai\n" +
                                    "mina ichiretsu wa shochi shite iyo",
                            commentary = "이 갚음은 권세를 맏고 제멋대로 간섭하고 탄압하는 사람들의 그릇된 마음가짐을 일소하여 어버이신이 깨닫도록 하는 것이니, 모두들은 이것을 잘 명싱해 두도록 하라\n 큰 신사 높은 산에 대해서는 제 6호 92수 주석 참조.\n 무너뜨린다는 것은 사람들의 마음의 티끌을 일소하여 탄압과 간섭 등, 행위를 멈추게 하고 지금까지의 비행을 고치게 한다는 뜻."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이 이야기 어떻게 생각하고 듣고 있는가\n 하늘에는 불비 바다는 해일이야 6-116\n",
                            japanese = "このはなしなんとをもふてきいている\nてんび火のあめうみわつなみや",
                            english = "kono hanashi nanto omote kiite iru\n" +
                                    "tenbi hi no ame umi wa tsunami ya",
                            commentary = "어버이신의 이 이야기를 사람들은 어떻게 생각하고 듣고 있는가. 어버이신의 구제한줄기의 의도를 저지하면 그에 대한 갚음은 하늘에서는 불비(火雨), 바다에서는 해일로써 나타나게 된다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이토록 걱정하는 月日의 마음을\n 온 세상은 어떻게 생각하는가 6-117\n",
                            japanese = "こらほどの月日の心しんバいを\nせかいぢうハなんとをもてる",
                            english = "kora hodono Tsukihi no kokoro shinpai o\n" +
                                    "sekaiju wa nanto omoteru",
                            commentary = "이처럼 어버이신은 여러가지로 걱정을 하고 있는데, 세상 사람들은 도대체 이를 어떻게 생각하고 있는가."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 차츰차츰 타이르기도 한탄하기도 하지만\n 진실한 마음이면 구제하리라 6-118\n",
                            japanese = "たん／＼とくどきなけきハとくけれど\nしんぢつなるの心たすける",
                            english = "dandan to kudoki nageki wa toku keredo\n" +
                                    "shinjitsu naru no kokoro tasukeru",
                            commentary = "어버이신은 온 세상 사람들을 염려해서 여러가지로 일러주기도 하고 때로는 탄식을 하기도 하지만, 어버이신의 의도를 깨닫고 참으로 마음을 반성하여 성진실한 마음이 된다면 어버이신은 그 사람을 반드시 구제하겠다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 어떤 자도 모두 다 지녀다\n 月日이 걱정하는 마음을 보라 6-119\n",
                            japanese = "どのよふなものも一れつハかこなり\n月日の心しんばいをみよ",
                            english = "dono yona mono mo ichiretsu waga ko nari\n" +
                                    "Tsukihi no kokoro shinpai o miyo",
                            commentary = "사람은 누구나 다 어버이신의 자녀이므로, 그 자녀를 생각하는 어버이마음에서 여러가지로 걱정하고 있는 만큼, 모두들은 이 마음을 깊이 헤아려 주기 바란다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이 세상 삼라만상은 모두 月日\n 인간은 모두 月日의 대물 6-120\n",
                            japanese = "このよふハ一れつハみな月日なり\nにんけんハみな月日かしもの",
                            english = "kono yo wa ichiretsu wa mina Tsukihi nari\n" +
                                    "ningen wa mina Tsukihi kashimono",
                            commentary = "이 세상 삼라만상은 어버이신이 창조하고 수호하므로, 어버이신의 몸 그 자체이며 인간에게 빌려준 몸도 역시 어버이신이 창조하고 수호하고 있는 것이다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 온 세상 이 진실을 알게 된다면\n 억센 고집 억지 욕심 내는 자 없다 6-121\n",
                            japanese = "せかいぢうこのしんぢつをしりたなら\nごふきごふよくだすものわない",
                            english = "sekaiju kono shinjitsu o shirita nara\n" +
                                    "goki goyoku dasu mono wa nai",
                            commentary = "온 세상 사람들은 각자의 몸이 어버이신에 의해서 창조되었고, 또 어버이신으로부터 빌려 받은 것이라는 이 사실을 깨닫게 된다면, 아무도 억센 고집과 억지 욕심을 부리며 제멋대로 행동하는 자는 없을 것이다. "
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 마음만 진실을 알게 된다면\n 아무런 두려움도 위태로움도 없다 6-122\n",
                            japanese = "こゝろさいしんぢつよりもわかりたら\nなにもこわみもあふなきもない",
                            english = "kokoro sai shinjitsu yori mo wakaritara\n" +
                                    "nanimo kowami mo abunaki mo nai",
                            commentary = "누구나 이 대물 차물의 리만 진실로 깨닫게 된다면, 아무런 두려움도 위태로움도 없다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 月日의 가르침은 모두 무시하고\n 남은 것은 인간마음뿐이야 6-123\n",
                            japanese = "月日よりをしゑる事ハみなけして\nあとハにんけん心ばかりで",
                            english = "Tsukihi yori oshieru koto wa mina keshite\n" +
                                    "ato wa ningen gokoro bakari de",
                            commentary = "어버이신이 일러주는 것은 모두 무시한 채 전혀 지키려 하지 않고, 오직 인간마음에서 무엇이든 제멋대로 하고 있기 때문에 두렵고 위태로운 일에 부딪치게 되는 것이다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 지금까지도 이 세상 창조한 진실을\n 가르쳐 주려고 생각했지만 6-124\n",
                            japanese = "いまゝでもこのよはじめたしんぢつを\nをしへてをことをもたなれども",
                            english = "imamade mo kno yo hajimeta shinjitsu o\n" +
                                    "oshiete oko to omota naredom",
                            commentary = "종전부터 어버이신은 이 세상을 창조한 진실한 이야기를 가르쳐 줄 생각으로 나날이 마음을 서두르고 있으면서도, 지금까지 가르치지 않는 것은 아직 때가 일러 시기가 오기를 기다리고 있기 때문이다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 月日이 나날이 마음 서둘러도\n 각한을 기다리고 있다고 생각하라 6-125\n",
                            japanese = "月日よりにち／＼心せきこめど\nこくげんまちているとをもゑよ",
                            english = "Tsukihi yori nichinichi kokoro sekikomedo\n" +
                                    "kokugen machite iru to omoe yo",
                            commentary = "어버이신이 이 세상을 창조한 과정을 오늘 가르칠까 내일 가르칠까 하고 나날이 마음을 서두르고 있는 것은, 어버이신의 이야기를 일러주어야 할 시기가 아직 오지 않았기 때문에 그 시기를 기다리고 있는 것이다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이 이야기 어떻게 생각하고 듣고 있는가\n 月日의 의도 두터운 혜택을 6-126\n",
                            japanese = "このはなしなんとをもふてきいている\n月日をもわくふかいりやくを",
                            english = "kono hanashi nanto omote kiite iru\n" +
                                    "Tsukihi omowaku fukai riyaku o",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이것만은 예삿일로 생각 말라\n 月日의 일 거룩한 의도 6-127\n",
                            japanese = "こればかり人なみやとハをもうなよ\n月日のしごとゑらいをもわく",
                            english = "kore bakari hitonami ya towa omouna yo\n" +
                                    "Tsukihi no shigoto erai omowaku",
                            commentary = "어버이신이 베풀어 주고자 하는 크나큰 혜택을 예삿일로 여겨서는 안된다. 그것은 어버이신의 거룩한 의도에서 나오는 것이다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 月日이 자유자재를 가끔 일러주지만\n 아직 지금까지는 나타낸 일 없다 6-128\n",
                            japanese = "月日よりぢうよちざいとまゝとけと\nまだいまゝでわみゑた事なし",
                            english = "Tsukihi yori juyojizai to mama tokedo\n" +
                                    "mada imamade wa mieta koto nashi",
                            commentary = "어버이신이 자유자재한 섭리를 한다고 재삼 일러주고는 있으나, 지금까지는 그것이 눈앞에 나타난 적이 없으므로 확실히 알지는 못할 것이다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이번에는 자유자재를 진실로\n 나타낼 것이니 이것이 참이야 6-129\n",
                            japanese = "このたびハぢうよぢざいをしんぢつに\nしてみせたならこれかまことや",
                            english = "konotabi wa juyojizai o shinjitsu ni\n" +
                                    "shite miseta nara kore ga makoto ya",
                            commentary = "이번에는 어버이신이 자유자재한 섭리를 나타내 보일 것이니, 이것을 보게되면 어버이신의 이야기가 거짓이 아님을 알게 될 것이다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이떤 일을 하는 것도 모두　月日\n 진실로써 하는 구제한줄기 6-130\n",
                            japanese = "とのよふな事をするのもみな月日\nしんぢつよりのたすけ一ぢよ",
                            english = "dono yona koto o suru no mo mina Tsukihi\n" +
                                    "shinjitsu yori no tasuke ichijo",
                            commentary = "무슨 일이든 모두 어버이신이 하는데, 그것은 진실로 인간을 구제하고자 하는 일념 때문이다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 태내에 잉태하게 하는 것도 月日이니라\n 출산하게 하는 것도 月日의 보살핌 6-131\n",
                            japanese = "たいないゑやどしこむのも月日なり\nむまれだすのも月日せわどり",
                            english = "tainai e yadoshi komu no mo Tsukihi nari\n" +
                                    "umare dasu no mo Tsukihi sewadori",
                            commentary = "태내에 아기를 잉태하는 것도 어버이신의 수호가 있기 때문이며, 또 그 아기가 태어나는 것도 어버이신의 수호가 있기 때문이다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이번에는 무슨 일이든 진실로\n 모두 나타내 보일 테다 6-132\n",
                            japanese = "このたびハどのよな事もしんぢつに\nみなあらわれてしてみせるでな",
                            english = "konotabi wa dono yona koto mo shinjitsu ni\n" +
                                    "mina arawarete shite miseru de na",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이것을 보고 어떤 자도 납득하라\n 마음에 따라 어떤 자유자재도 6-133\n",
                            japanese = "これをみていかなものでもとくしんせ\n心したいにいかなぢうよふ",
                            english = "kore o mite ikana mono demo tokushin se\n" +
                                    "kokoro shidai ni ikana juyo",
                            commentary = "이번에는 어버이신이 이 세상에 나타나서 무슨 일이든 모두 실제로 나타내 보일 것이니, 이것을 보고 지금까지 어버이신의 섭리를 의심하고 있던 사람들도 각자의 마음에 따라 어버이신은 과연 어떤 수호도 자유자재로 해 준다는 것을 잘 납득하라."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 무슨 일을 해도 진실한\n 마음에 따라 모두 나타낼 테다 6-134\n",
                            japanese = "どのよふな事をするのもしんぢつの\n心したいにみなしてみせる",
                            english = "dono yona koto o suru no mo shinjitsu no\n" +
                                    "kokoro shidai ni mina shite miseru",
                            commentary = "무슨 일을 하든 어버이신은 각자의 진실한 마음에 따라 모두 그대로 섭리를 나타내 보일 것이다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 月日이 38년 이전에\n 하강했던 으뜸인 인연 7-1\n",
                            japanese = "",
                            english = "",
                            commentary = "진실한 어버이신이 지금으로부터 38년 이전에 터전에 하강한 것은 집터의 인연, 교조 혼의 인연, 순각한의 인연 등, 세가지 인연 때문이었다.\n 당시로부터 38년 이전, 즉 1838년 10월 26일, 어버이신님이 교조님을 현신으로 삼아 터전에 하강하신 것을 말한다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 月日은 그러한 인연이 있기 때문에\n 무엇이든 자세하게 일러주고 싶어서 7-2\n",
                            japanese = "",
                            english = "",
                            commentary = "어버이신은 그러한 으뜸인 인연이 있기 때문에 하강해서 거기에 대해 무엇이든 자세히 일러주고자 하는 것이다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 윗사람은 그것을 모르고서 무엇이든 세상 보통 일로 생각하고 있다 7-3\n",
                            japanese = "",
                            english = "",
                            commentary = "윗사람들은 그러한 어버이신의 의도를 조금도 모른 채, 어버이신이 일러주는 바를 세상 보통의 가르침으로 대수롭지 않게 생각하고 있다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이곳이 으뜸인 터전이므로\n 이 세상 태초를 모를 리 없다 7-4\n",
                            japanese = "",
                            english = "",
                            commentary = "이 집터는 세계와 인간을 창조한 으뜸인 터전이 있는 곳이므로, 어버이신이 그것을 창조한 태초에 관해 무엇 하나 모를 리가 없다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 윗사람에게 이 진실을 어서어서\n 알려 주려고 月日 생각하지만 7-5\n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 윗사람은 그것을 모르고서 각자\n 제 몸 생각만을 일삼고 있어서 7-6\n",
                            japanese = "",
                            english = "",
                            commentary = "어버이신은 윗사람들에게 한시라도 빨리 이 으뜸인 인연에 관한 이야기를 일러주려고 생각하고 있으나, 윗사람들은 그걸 전혀 모른 채, 각자 제멋대로 제 몸 생각만을 일삼으며 어버이신의 이야기에 귀를 기울이려고 하지 않는다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 月日에게는 차츰차츰 보이는 길에\n 두렵고 위태로운 길이 있으므로 7-7\n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 月日은 그 길을 어서 알리고\n 싶어서 걱정하고 있다 7-8\n",
                            japanese = "",
                            english = "",
                            commentary = "어버이신은 인간들이 제 몸 생각만을 하며 살아가기 때문에, 앞날에 두렵고 위태로운 길이 있음을 잘 알고 있으며, 그러므로 그것을 빨리 알려 잘못됨이 없도록 가르치려고 여러 가지로 걱정하고 있다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 인간이 제 자식 생각하는 것과 같이\n 두렵고 위태로운 길을 염려한다 7-9\n",
                            japanese = "",
                            english = "",
                            commentary = "마치 인간의 부모가 제 자식에 관한 일을 여러 가지로 걱정하는 것과 마찬가지로, 어버이신도 자녀인 모든 인간이 두렵고 위태로운 길을 걷지 않도록 하기 위해 여러 가지로 애를 태우며 염려하고 있는 것이다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 그것을 모르고서 모든 사람들은 각자\n 모두 멍청히 살고 있으니 7-10\n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이 세상 무엇이든 만가지를 한결같이\n 月日이 지배한다고 생각하라 7-11\n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이 이야기는 어떤 것이라 생각하는가\n 이제부터 장래의 길을 두고 보라 7-12\n",
                            japanese = "",
                            english = "",
                            commentary = "이 세상 만사는 무엇이나 하나같이 어버이신이 지배하고 있음을 잘 납득하라고 말해도 그것이 무엇을 뜻하는지 잘 모르겠지만, 이제부터 앞으로 나타나는 일을 잘 두고 보라. 그러면 과연 그렇구나 하고 알게 될 것이다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 아무리 높은산이라도 물에 잠기고\n 골짜기라도 위태로운 것이 없다 7-13\n",
                            japanese = "",
                            english = "",
                            commentary = "아무리 부귀와 권세를 누리고 있을지라도 일단 인연이 나타나면 신상이나 사정으로 괴로워하지 않을 수 없다. 또, 아무리 가난하게 살고 있을지라도 진실한 마음만 갖고 있으면 근심 걱정 없이 즐겁게 살 수 있다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 무엇이든 月日이 지배하므로\n 크다 작다 말하지 마라 7-14\n",
                            japanese = "",
                            english = "",
                            commentary = "무슨 일이든 모두 어버이신이 지배하고 있는 만큼, 크다 작다, 강하다 약하다는 등의 차별을 멋대로 정해서 이러니저러니해서는 안된다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이제까지도 어떻든 용재가 필요하므로\n 대체로 찾아보고 있었지만 7-15\n",
                            japanese = "",
                            english = "",
                            commentary = "이제까지도 어버이신의 수족이 되어 활동한 사람이 필요하므로 무척이나 찾고 있었다.\n 용재란 마음의 역사, 세계구제의 역사에 쓰이는 재목이라는 뜻이다. 어버이신님의 구제한줄기의 성업에 수족으로 쓰이는 사람을 말한다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이번에는 골짜기에서 자그마한\n 나무들이 수없이 보인다 7-16\n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이 나무도 차츰차츰 月日이 손질하여\n 만들어 키우면 나라의 기둥이야 7-17\n",
                            japanese = "",
                            english = "",
                            commentary = "그런데 이번에는 신분이 낮은 사람들 가운데서 아직은 미흡하나 앞으로 용재가 될 만한 진실한 사람이 많이 보인다. 이 사람들도 이제부터 어버이신이 차츰 가르쳐 키운다면 장래에는 훌륭한 용재가 되어 세상을 위해서도 요긴하게 쓰이게 된다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 그 다음에는 나날이 月日이 살펴서\n 다음 용재를 준비하기만 7-18\n",
                            japanese = "",
                            english = "",
                            commentary = "그 뒤로는 어버이신이 나날이 사람들의 마음을 살펴보고 다음 용재가 될 사람을 키울 준비를 서두른다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 그리하여 노목부터 차츰차츰\n 손질하여 끌어들여 또 다음 준비를 7-19\n",
                            japanese = "",
                            english = "",
                            commentary = "그리하여 연륜이 쌓인 사람, 즉 이 길에서 연수의 리가 쌓인 사람부터 차츰 손질을 하여 끌어들여 용재로 쓰고, 다시 그 다음 사람을 차례차례로 그ㄱ어들여 용재를 키울 준비를 한다.\n 노목(老木)이란 연륜이 쌓인 사람, 즉 신앙에 정진하여 마음성인이 된 사람을 말한다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 나날이 月日의 의도는 깊은 만큼\n 같은 장소에서 두 그루 세 그루 7-20\n",
                            japanese = "",
                            english = "",
                            commentary = "어버이신의 의도는 심원해서 같은 곳에서도 두 사람, 세 사람 용재될 사람을 찾아서 키운다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이 나무도 여송 남송 말하지 않아\n 어떤 나무든지 月日의 의도 7-21\n",
                            japanese = "",
                            english = "",
                            commentary = "용재가 될 사람에게 남녀의 구별은 없다. 남자든 여자든 마음이 맑은 진실한 사람이면 어버이신은 용재로 삼을 생각이다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 앞으로 무슨 이야기를 하느냐 하면\n 용재 준비만을 말할 것이다 7-22\n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 용재도 적은 수가 아닌 것이니\n 50 60명의 인수가 필요하다 7-23\n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이 인수 언제까지나 줄지 않게\n 영원히 이어나가 끊임없도록 7-24\n",
                            japanese = "",
                            english = "",
                            commentary = "용재도 두세 사람의 적은 수가 아니다. 적어도 5,60명의 인수가 필요한 것이다. 그리고 그 인수는 언제까지나 줄지 않고 영원히 이어지도록 할 것이다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이토록 생각하는 月日의 진실을\n 모두들은 어떻게 생각하는가 7-25\n",
                            japanese = "",
                            english = "",
                            commentary = "이토록 여러 가지로 애를 쓰며 걱정하고 있는 어버이신의 마음을 모두들은 어떻게 생각하고 있는가."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 어떤 이야기를 일러주는 것도\n 구제하고자 하는 한결같은 마음뿐이야 7-26\n",
                            japanese = "",
                            english = "",
                            commentary = "어러가지로 이야기를 해 주는 것도 온 세상 사랄듬을 빨리 구제하고자 하는 한결 같은 마음 때문이다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 모든 사람들의 마음이 진실로\n 빨리 알게 될 것 같으면 7-27\n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 그 다음에는 月日이 만가지 지배한다\n 무엇이든 만가지를 구제할 거야 7-28\n",
                            japanese = "",
                            english = "",
                            commentary = "온 세삼 사람들이 마음을 맑혀서 빨리 어버이신의 뜻을 깨닫게 된다면, 그 다음에는 세상만사를 지배하는 어버이신이 자유자재한 섭리를 나타내어 섭리를 나타내어 무엇이든 만가지를 구제할 테다"
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이 구제 빨리 혜택을 보이고자\n 月日은 마음 서두르고 있을 뿐이야 7-29\n",
                            japanese = "",
                            english = "",
                            commentary = "이 만가지구제의 혜택을 빨리 보이고 싶은 일념에서 어버이신은 온 세상 사람들이 어서 마음 바꾸기를 서두르고 있는 것이다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 무엇이든 이 서두름이 있음으로써\n 마음의 청소를 서두르는 거야 7-30\n",
                            japanese = "",
                            english = "",
                            commentary = "온 세상 사람들을 빨리 구제하려는 생각에서 마음 바꾸기를 서두르고 있는 만큼, 모두들은 이러한 어버이신의 듯을 잘 깨달아 각자의 마음을 빨리 맑혀서 진실한 마음이 되도록 하라."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이 이야기는 남의 일이라 생각 말라\n 모두 각자의 집안 이야기야 7-31\n",
                            japanese = "",
                            english = "",
                            commentary = "어버이신이 하는 이 이야기를 남의 일로 생각하여 귀 밖으로 들어서는 안된다. 이것은 모두 각자의 집안에 대한 어버이신의 깨우침이다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 각자는 가슴속으로 부터 단단히\n 진실을 내라 곧 나타나리라 7-32\n",
                            japanese = "",
                            english = "",
                            commentary = "각자는 마음을 깨끗이 청소하고 단단히 작정해서 빨리 진실한 마음을 내도록 하라, 그러면 어버이신의 혜택이 곧 나타난다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 月日에게 이 서두름이 있음으로써\n 무엇인가 마음이 바쁜 것이다 7-33\n",
                            japanese = "",
                            english = "",
                            commentary = "어버이신은 모든 사람을 빨리 도우려는 생각에서 마음 바꾸기를 서두르고 있기 때문에, 어버이신의 마음은 무언가 마냥 바쁘기만 하다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이것만 어서 자유자재로 나타난다면\n 月日의 마음은 절로 용솟음칠 텐데 7-34\n",
                            japanese = "",
                            english = "",
                            commentary = "모든 사람들이 마음을 바꿈으로써 어버이신의 자유째한 수호가 빨리 나타나 처마다 만가지구제를 받게 되면, 어버이신의 마음은 절로 용솟음치게 된다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 지금까지도 지금이 이 세상 시작이라고\n 말하고 있었지만 무슨 뜻인지 7-35\n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이번의 자유자재를 보고 납득하라\n 지금까지 이런 일은 모르겠지 7-36\n",
                            japanese = "",
                            english = "",
                            commentary = "지금까지는 지금이 이 세상 시작이라고 번번이 말해 왔으나, 사람들은 그것이 무슨 뜻인지 전혀 모르고 있었다. 그러나 이번에 나타난 어버이신의 자유자재한 섭리를 보고 그 의미를 잘 깨달아라. 각자가 마음을 바꿈으로써 어떤 수호도 자유자재로 받을 수 있다는 이 진기한 사실을 지금까지는 아무도 몰랐을 것이다.\n 지금이 이 세상 시작이란 어버이신님이 이 구극의 가르침으로써 온 세상 사람들의 몸과 마음을 갱생시키는 일에 착수하신다는 뜻이다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 月日이 태내로 들어가서\n 자유자래를 모두 나타내 보일 테다 7-37\n",
                            japanese = "",
                            english = "",
                            commentary = "어버이신이 태내에 들어가서 자유자재한 수호를 나태내 보일 테다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이토록 자유자재의 진실을\n 이야기하는 것은 지금이 처음이야 7-38\n",
                            japanese = "",
                            english = "",
                            commentary = "이처럼 자유자재한 수호를 하는 어버이신의 진실한 가르침을 모든 사람들에게 일러주는 것은 지금이 처음이다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 앞으로는 언제라도 이와 같이\n 자유자재를 빨리 알린다 7-39\n",
                            japanese = "",
                            english = "",
                            commentary = "앞으로는 언제까라도 이처럼 자유자재한 수호를 한다는 것을 알려둔다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 지금까지도 여러가지 길을 걸어왔지만\n 순산구제의 졍험은 처음이야 7-40\n",
                            japanese = "",
                            english = "",
                            commentary = "지금까지 인간들은 갖가지 길을 걸어 왔으나, 이번에 즐거운근행을 통해 순산구제를 하는 것은 실로 이 세상 창조 이래 일찍이 경험하지 못했던 처음 있는 일이다.\n 순산구제란 순산허락을 통해 안산을 수호하는 구제로서, 순산허락을 받은 사람은 누구나 수월하게 출산할 수 있으며, 산후에도 여러가지 기(忌)할 것, 기댈 것이 필요없이 곧 일어나서 평상시와 같이 활동할 수 있다. 또 순산허락은 이를 허락 받은 사람의 마음에 따라 산기(産期)를 당기기도 늦추기도 하는, 자유자재한 수호를 할 수 있는 신기한 구제이다. 이 순산허락은 태초 인간창조시 어버이신님이 터전에서 인간을 잉태시킨 그 리에 따라 터전에서 내려 주며, 따라서 이것은 터전에서만 행할 수 있다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이번에는 순산구제의 진실을\n 어서 나타내려고 月日 서두르지만 7-41\n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 모든 사람들은 지금까지 모르는 일이므로\n 모두들 잠잠히 침울해 있다 7-42\n",
                            japanese = "",
                            english = "",
                            commentary = "이번에는 순산구제의 수호를 빨리 나타내려고 어버이신은 서두르고 있으나, 모든 사람들은 지금까지 전례가 없던 일이므로 반신반의하며 주저하고 있다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 진실로 마음을 작정하여 원한다면\n 자유자재를 지금 당장이라도 7-43\n",
                            japanese = "",
                            english = "",
                            commentary = "성진실한 마음으로 어버이신이게 원한다면 지금 당장이라도 자유자재한 수호를 해준다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이 일은 누구도 모르는 일이므로\n 가슴속을 몰라주니 月日의 섭섭함 7-44\n",
                            japanese = "",
                            english = "",
                            commentary = "이같은 자유자재한 수호는 지금까지 아무도 모르는 진기한 일이라 사람들은 어버이신의 뜻을 깨닫지 못하고 있는데, 어버이신으로서는 이것이 무엇보다 안타까운 일이다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 지금까지는 아무것도 나타나지 않았지만\n 이제부터 앞으로는 당장 나타나리라 7-45\n",
                            japanese = "",
                            english = "",
                            commentary = "지금까지는 아무것도 실제로 나타나지 않았지만, 앞으로는 어버이신의 수호가 당장 나타날 것이다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 진실한 마음이 있으면 무엇이든\n 빨리 기원하라 곧 이루어지리라 7-46\n",
                            japanese = "",
                            english = "",
                            commentary = "진실한 마음만 있다면 무엇이든 빨리 어버이신에게 기원토록 하라. 그러면 어버이신은 그것을 받아들여 마음에 따라 모두 이루어 줄 것이다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 무엇이든 안된다고 말하지 않아\n 구제한줄기를 서두르고 있으므로 7-47\n",
                            japanese = "",
                            english = "",
                            commentary = "어떤 소원이라도 들어주지 않는다고는 말하지 않는다. 어버이신은 오직 구제한줄기만을 서두르고 있으므로 진실한 마음으로 원한다면 무엇이든 수호해준다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이토록 月日이 마음 서둘러도\n 곁의 사람들의 마음은 왜 침울한가 7-48\n",
                            japanese = "",
                            english = "",
                            commentary = "이토록 어버이신은 구제한줄기를 서두르고 있는데 곁의 사람들은 왜 마음이 침울한가."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 어서어서 마음 용솟음치며 서둘러야\n 月日의 고대 이것을 모르는가 7-49\n",
                            japanese = "",
                            english = "",
                            commentary = "빨리 용솟음치는 마음으로 구제를 서두르도록 하라. 어버이신이 이토록 고대하고 있음을 곁의 사람들은 모르는가."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 月日은 자유자재를 진실로\n 빨리 보이고 싶다 이 한결같은 마음 7-50\n",
                            japanese = "",
                            english = "",
                            commentary = "진실로 어버이신은 자유자재한 수호를 빨리 나타내어 모든 사람들에게 보이고 싯다. 이것은 어버이신의 한결같은 염원이다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이토록 생각하는 月日의 진실을\n 곁의 사람들의 마음 아직 세상 보통 일로 여겨 7-51\n",
                            japanese = "",
                            english = "",
                            commentary = "자유자재한 수호를 빨리 나타내어 온세상 자녀들을 구제하려는 어버이신의 간절한 마음을 곁의 사람들은 전혀 깨닫자 못하고, 그저 세상 보통 일로만 여기는 것이 어버이신으로서는 참으로 안타깝다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 무슨 말을 해도 인간의\n 마음이 아니고 月日의 마음이야 7-52\n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 지금까지는 무슨 말을 해도 인간의\n 마음이 섞인 듯이 생각해서 7-53\n",
                            japanese = "",
                            english = "",
                            commentary = "무슨 말을 일러주더라도 그것은 결코 인간생각으로 하는 말이 아니고 모두 어버이신의 마음으로 하는 말이다. 그런데도 지금까지는 어버이신이 무슨 말을 일러주어도 그것을 의심하여 인간생각이 섞여 있는 듯이 생각해 왔다.(다음 노래의 주석 참조)"
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 단단히 들어라 이제부터 마음 바꿔\n 인간마음 있다고 생각 말라 7-54\n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 지금까는 같은 인간인 양\n 생각하고 있으니 아무것도 몰라 7-55\n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이제부터는 무슨 말을 하건 행하건\n 인간이라고 전혀 생각 말라 7-56\n",
                            japanese = "",
                            english = "",
                            commentary = "사람들은 교조님이 인간의 모습을 하고 계셨기 때문에, 교조님이 하시는 말씀에도 인간마음이 섞여 있는 듯이 생각하여 가볍게 듣고 흘림으로써 좀처럼 어버이신님의 뜻을 깨달을 수가 없었다. 그러나 '입은 인간 마음은 월일이야'라고 하신 바와 같이, 비록 교조님은 인간의 모습을 하고, 또 그 입으로 일러주시기는 하나  마음은 월일의 마음, 즉 어버이신님의 마음이므로 이제부터는 무슨 말씀을 하시든 인간의 마음으로 하는 것이 아니고, 어디까지나 어버이신님의 마음으로 하신다는 것을 믿어야 한다고 이 노래들은 거듭 경계하고 있다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 지금까지는 날이 아직 오지 않아서\n 무슨 일이든 미루고 있었다 7-57\n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 앞으로는 무슨 말을 할지라도\n 두려움도 위태로움도 없다고 생각하라 7-58\n",
                            japanese = "",
                            english = "",
                            commentary = "지금까지는 아직 시기가 이르므로 무슨 일이든 미루어 왔으나, 이제는 시순이 다가왔으므로 어버이신이 한껏 힘을 나태낼 것이니, 앞으로는 무슨 말을 일러주더라도 결코 두려움이나 위태로움 따위는 없는 만큼, 안심하고 구제한줄기로 향해 정진토록 하라.\n 이 노래들은 당시 관헌의 간섭으로 침울해 있던 곁의 사람들을 채찍질하신 말씀이다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이제부터는 月日이 나갈 것이니\n 무슨 일이든 갚음하여 줄 테니 7-59\n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 月日에게는 이제까지의 섭섭함이\n 첩첩이 쌓이고 쌓여 있다 7-60\n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 아무리 섭섭함이 쌓이 있을지라도\n 이래라 저래라고는 말하지 않는 거야 7-61\n",
                            japanese = "",
                            english = "",
                            commentary = "이제부터는 어버이신이 직접 나가서 무슨 일이든 모두 실제로 갚음하여 준다. 그렇게 하는 이유는 지금까지 어버이신에게 섭섭함이 첩첩이 쌓여 있기 때문이다. 그러나 어버이신이 섭섭하다든가 갊음한다고 하는 것은 모두 온 세상 자녀들이 귀엽기 때문이며, 따라서 어버이신이 갚음한다고 하는 것은 모두 온 세상 자녀들이 귀엽기 때문이며, 따라서 어버이신이 갚음한다고 하더라도 그것은 세상에 흔히 있는 보복과는 근본적으로 다른 것이다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이제부터는 아무리 높은 곳일지라도\n 이 진실을 빨리 나타내고 싶다 7-62\n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 윗사람들의 마음에 빨리 납득이 되면\n 月日은 자유자재 빨리 나타낼 것인데 7-63\n",
                            japanese = "",
                            english = "",
                            commentary = "윗사람들이 이러한 어버이신이 뜻을 빨리 납득하게만 되면, 어버이신은 자유자재한 수호를 빨리 나타낼 텐데, 윗사람들은 이 점이 아직 충분치 뭇하다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 月日이 이 자유자재를 온 세상에\n 빨리 두루 알린다면 7-64\n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이번에 잉태한 것을 안의 사람들은\n 어떻게 생각하고 기다리고 있는가 7-65\n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이것만은 예삿일로 생각 말라\n 어떻든 月日의 대단한 의도 7-66\n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이 자를 6년 전 3월\n 15일에 데려갔던 거야 7-67\n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 그로부터 지금까지 月日 단단히\n 안고 있었다 빨리 보이고 싶다 7-68\n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 그것을 모르고서 안의 사람들은 무엇이든\n 세상 보통 일로 생각하여 7-69\n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이 이야기는 어떤 것이라 생각하는가\n 이것이 첫째가는 이 세상 시작 7-70\n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 또 앞으로 올 길의 양상 차츰차층\n 만가지 일을 모두 일러둔다 7-71\n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이름은 다마에 어서 보고 싶거든\n 月日이 가르친 손춤을 단단히 7-72\n",
                            japanese = "",
                            english = "",
                            commentary = "슈지 선생의 서녀인 오슈인님은 6년 전, 즉 1870년 3월 15일에 출직했다. 오슈우님의 혼은 원래 으뜸인 집터와 깊은 인연이 있기 때문에, 빨리 그 곳에 다시 태어나게 하려고 어버이신님은 그 혼을 단단히 안고 계시다가, 시순이 오자 슈지 선생의 보인인 마쓰에님에게 잉태시키셨는데, 위의 노래들은 이 사실을 예언하신 말씀이다. 그러나 어버이신님의 이러한 ㄱ은 의도를 전혀 알지 못하는 곁의 사람들은 어버이신님의 예언을 가볍게 듣고 흘려 버리고만 있었다. 한편, 어버이신님은 이버에 태어나는 아이가 여자 아기임을 아시고 미리부터 '다마에'라는 이름까지 지어 두셨는데, 이는 곧어어버이신님의 섭리가 자요자재임을 실증하는 것이라 하겠다. 그리고 이같은 자유자재한 습리를 보고 싶으면 어버이신님이 가르치신 손춤을 단단히 익혀서 빨리 근행을 올리라고 말씀하신 것이다.(제 1호 61수 및 제3호 20의 주석 참조)"
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이 이야기 진실로 생각한다면\n 마음을 작정하여 빨리 시작하라 7-73\n",
                            japanese = "",
                            english = "",
                            commentary = "이같은 어버이신의 이야기를 진실로 믿는다면 마음을 가다듬어 빨리 근행을 시작하라."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 지금까지는 무슨 말을 해도 모든 사람들이\n 가슴속도 모르고 날도 오지 않아서 7-74\n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 차츰차츰 가슴속을 알게 되고 날도 온다\n 몹시 서두르는 月日의 마음 7-75\n",
                            japanese = "",
                            english = "",
                            commentary = "지금까지는 아무리 일러주어도 모든 사람들의 마음이 어버이신의 뜻을 깨달을 수 있을 만큼 맑지 못하고, 또 아직 그 시기도 오지 않았기 때문에 근행을 미루어 왔으나, 이제는 사람드의 마음도 맑아져서 어버이신의 뜻을 깨달을 수 있게 되고, 곧 시기도 올 것이므로 빨리 근행을 시작하도록 어버이신은 몹시 서두르고 있다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이것만 빨리 나타나게 되면\n 어떤 자도 당할 수 없다 7-76\n",
                            japanese = "",
                            english = "",
                            commentary = "이것만 빨리 실현하게 된다면 어떤 사람도 어버이신의 자유자재한 섭리에 당하지 못할 것이다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 月日이 이것을 확실히 나타낸다면\n 다음의 일은 무슨 일이든 7-77\n",
                            japanese = "",
                            english = "",
                            commentary = "어버이신이 이 아기만 빨리 다시 태어나게 한다면, 그 다음에는 무슨 일이든 자유자재로 수호한다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 지금까지도 순산 마마 이 허락을\n 모두들은 어떻게 생각하고 있었는지 7-78\n",
                            japanese = "",
                            english = "",
                            commentary = "지금까지 순산허락과 마마 수호부를 내려 주었지만, 모두들은 그것을 어덯게 생각하고 있는가. 아마 그 깊은 리에 대해서는 아무것도 모르고 있을 것이다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이번에는 무슨 일이든 확실하게\n 모두 진실로 일러준다 7-79\n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이제부터는 순산구제도 확실히\n 고통 없이 빨리 낳게 한다 7-80\n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 차츰차츰 입으로 무슨 말을 할지라도\n 月日 말대로 하지 않으면 안돼 7-81\n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 月日이 이르는 무슨 일이든 단단히\n 말하는 대로 하라 틀림이 없다 7-82\n",
                            japanese = "",
                            english = "",
                            commentary = "어버이신이 일러주는 것은 무슨일이든 마음에 잘 새겨 말하는 대로 하라. 어버이신이 하는 말에는 절대 틀림이 없다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 어떤 구제를 하는 것도 모두 근행\n 月日 말대로 단단히 한다면 7-83\n",
                            japanese = "",
                            english = "",
                            commentary = "어떤 구제도 모두 즐거운근행의 의해서 행하는 것이므로, 어버이신이 일러주는대로 단단히 근행을 한다면 어떤 구제도 모두 나타날 것이다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 진실한 마음이 있으면 月日도\n 확실히 맡아서 구제할 거야 7-84\n",
                            japanese = "",
                            english = "",
                            commentary = "어버이신이 이르는 대로 단단히 근행을 하겠다는 진실한 마음만 있으면, 어버이신도 확실히 맡아서 반드시 구제해 준다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이번의 구제는 진실로\n 맡아서 하는 구제로서 지금이 처음이야 7-85\n",
                            japanese = "",
                            english = "",
                            commentary = "이번에는 어버이신이 확실히 맡아서 구제를 하는데 이런 구제는 이것이 처음이다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이토록 月日은 마음 서두르고 있다\n 곁의 사람들의 마음도 근행 준비를 7-86\n",
                            japanese = "",
                            english = "",
                            commentary = "이토록 어버이신이 서두르고 있는 만큼, 곁의 사람들도 이러한 어버이신의 마음을 잘 개달아 빨리 근행 준비를 하도록 하라."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이 준비 무엇에만 한정된 것 아닌 만큼\n 무슨 일이든 모두 근행인 거야 7-87\n",
                            japanese = "",
                            english = "",
                            commentary = "이처럼 준비를 ㅓ두르고 있는 근행은 어느 한 가지 구제에만 한정되는 것이 아니고, 어떤 구제도 오직 이 근행에 의해서 나타나는 것이다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 근행이라 해도 같은 것이 아닌 만큼\n 모두 각각으로 손짓을 가르친다 7-88\n",
                            japanese = "",
                            english = "",
                            commentary = "한마디로 근행이라 하지만 다 같은 것이 아니다. 모두 각각으로 근행에 따른 손짓을 가르친다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 지금까지 걸어온 길의 과정을\n 어떤 길인지 아는 자 없다 7-89\n",
                            japanese = "",
                            english = "",
                            commentary = "지금까지 걸어온 길이 어떤 길인지 그 과정을 아는 사람은 아무도 없다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이제부터는 어떤 길도 차츰차츰\n 만가지 길의 과정 모두 일러준다 7-90\n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 月日로부터 갖가지 길의 과정 듣게 되면\n 이 섭섭함은 무리가 아닐 거야 7-91\n",
                            japanese = "",
                            english = "",
                            commentary = "어버이신으로부터 길을 걸어온 갖가지 과정에 대한 이ㅑ기를 듣게 된다면, 어버이신 섭섭해 하고 있는 것도 결코 무리가 아니라는 것을 알게 될 것이다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 月日이 이것을 풀어 버리면\n 다음에는 즐거움이 넘칠 거야 7-92\n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 나날이 즐거움이 넘친다는 것은\n 어떤 것인지 아무도 모르겠지 7-93\n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 무엇이든 즐거움이란 모두 근행\n 진기한 것을 모두 가르칠 테다 7-94\n",
                            japanese = "",
                            english = "",
                            commentary = "무엇이든 즐거움이란 모두 근행에 의해서 실현된다. 그러므로 이제부터 근행에 따른 여러가지 진기한 일을 모두 가르칠테다. "
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 차츰차츰 근행을 가르치는 이 준비\n 가슴속을 모두 청소한다 7-95\n",
                            japanese = "",
                            english = "",
                            commentary = "어버이신은 점차로 근행을 가르칠 준비를 하고 있는데, 이에 앞서 각자의 마음의 티끌을 먼저 깨끗이 청소할 것이다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 앞으로는 나날이 마음 용솟음칠 거야\n 여러가지 근행의 손짓을 가르친다 7-96\n",
                            japanese = "",
                            english = "",
                            commentary = "마음의 티끌을 깨끗이 청소하고 나면 나날이 마음이 용솟음칠 것인데, 그런 연후에 만가지 근행의 손짓을 가르친다.\n 1875년 음력 5월 26일에 교조님은 감로대를 세울 터전을 결정하신 후, 감로대근행의 손짓을 대략 가르치고, 이어서 같은 해에 '순산' 과 '마마' 근행의 손짓을 가르치셨다. 그리고 그 후 잇따라 '거름', '움틈' 등, 모두 12가지 근행의 손짓을 가르치셨는데, 만가지 근행이란 바로 이것을 두고 하는 말이다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이 근행응은 어떤 것이라 생각하는가\n 순산과 마마의 구제한줄기 7-97\n",
                            japanese = "",
                            english = "",
                            commentary = "이 근행은 ㅇ떤 것이라 생각하는가. 이것은 순산과 마마를 돕기 위한 구제한줄기의 근행이다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이 구제 어떤 것이라 생각하는가\n 마마하지 않도록 근행을 가르친다 7-98\n",
                            japanese = "",
                            english = "",
                            commentary = "이 구제는 어떻게 하는 것이라 생각하는가. 그것은 마마를 하지 않도록 하는 근행을 가르쳐서 구제하는 것이다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이 길을 빨리 가르칠 이 근행\n 온 세상 사람들의 마음을 맑힌다 7-99\n",
                            japanese = "",
                            english = "",
                            commentary = "이 길을 온 세상 사람들에게 빨리 전하고자 만가지 근행을 가르친다. 그리고 이 근행으로 온 세상 사람들의 마음을 진실로 맑힌다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이 이야기를 어떻게 듣고 있는가\n 세계를 구제할 준비만을 7-100\n",
                            japanese = "",
                            english = "",
                            commentary = "어버이신이 하는 이 이야기를 모두들은 어떻게 생각하고 듣고 있는가. 어버이신은 온 세상 사람들을 구제할 준비만을 일러주고 있는 것이다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 어떤 구제를 하는 것도 진실한\n 어버이가 있으니 모두 맡는다 7-101\n",
                            japanese = "",
                            english = "",
                            commentary = "어떤 구제를 하는 것도 모두 진실한 어비이가 존명하고 있기 때문에 가능한 것이다.\n 진실한 어버이란 어버이신 천리왕님으로서, 여기서는 어버이신님의 현신이신 교조님을 가리킨다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이것을 진실이라 생각한다면\n 성진실한 마음에 따라 7-102\n",
                            japanese = "",
                            english = "",
                            commentary = "교조가 어버이라는 것을 진실로 믿는다면, 그러한 성진실한 마음을 받아들여 어떤 수호도 할 것이다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 마음만 진실로 맑아진다면\n 무슨 일이든 틀림이 없다 7-103\n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 지금까지는 月日 아무리 생각해도\n 곁의 사람들의 마음은 알지 못하므로 7-104\n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이제부터는 어떤 근행을 가르쳐도\n 인간의 마음은 아니다 7-105\n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 月日은 무슨 일이든 가르칠 테다\n 이 세상 창조 이래 없던 일만을 7-106\n",
                            japanese = "",
                            english = "",
                            commentary = "어버이신은 모든 것을 다 가르칠 것인데, 그것은 이 세상 창조 이래 지금까지 없었던 일이며, 사람들이 모르는 일뿐이다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 인간마음으로 생각하는 듯한 일\n 月日은 결코 말하지 않는 거야 7-107\n",
                            japanese = "",
                            english = "",
                            commentary = "인간마음으로 생각하는 것과 같은 자기 본위적이고 장래를 내다보지 못하는 따위의 일을 어버이신은 결코 말하지 않는다. 어버이신이 일러주는 것은 먼 장래까지도 내다보고 세계 인류를 구제하는 일들 뿐이다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 月日은 무슨 일이든 온 세상\n 사람들에게 가르쳐 즐거움이 넘치도록 7-108\n",
                            japanese = "",
                            english = "",
                            commentary = "어버이신은 무슨 일이든 차별 없이 온 세상 사람들에게 가르쳐, 모두가 즐거움이 넘치는 삶을 살게 하려고 생각하고 있다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 온 세상 모든 사람들의 마음이 맑아져서\n 즐거움이 넘치게 살아가게 되면 7-109\n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 月日도 확실히 마음 용솟음치면\n 인간들도 모두 다 같은 것 7-110\n",
                            japanese = "",
                            english = "",
                            commentary = "온 세상 사람들이 진실로 마음이 맒아져 즐거움이 넘치는 삶을 살게 되면, 어버이신의 마음도 용솟음치게 되고, 어버이신이 용솟음치면 인간들도 다 함께 용솟음치게 된다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이 세상 사람들의 마음 용솟음치면\n 月日도 인간도 다 같은 거야 7-111\n",
                            japanese = "",
                            english = "",
                            commentary = "이처럼 온 세상 사람들의 마음이 용솟음치게 되면, 어버이신도 인간들도 다함께 용솟음쳐서 이 세상에는 참으로 즐거움이 넘치게 될 것이다.\n 이 노래는 사람들의 마음이 성진실한 마음으로 깨끗이 맑아지면, 즐거운 삶이 실현되고 나아가 신앙의 극치인 신인화합(神人和合)의 경지에 이르게 됨을 말하고 있다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 나날이 月日의 섭서함 첩첩이\n 쌓여 있는 것을 풀어버리고자 8-1\n",
                            japanese = "",
                            english = "",
                            commentary = "온 세상 사람들이 어버이신의 마음을 헤아리지 못하는 데 대한 섭섭함이 첩첩이 쌓여 있는데, 어버이신은 이것을 풀어 버리려고 여러 가지로 마음을 다하고 있다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이 이야기 월일의 의도 차츰차츰\n 무엇이든 만가지를 구제하는 것은 8-2\n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 진실한 마음에 따라 어떤\n 근행을 해도 이 모두 구제인 거야 8-3\n",
                            japanese = "",
                            english = "",
                            commentary = "차츰 어버이신의 뜻을 사람들에게 일러주어 만가지구제를 할 것인데, 이를 실현하려면 진실한 마음으로 근행을 올려야 한다. 그러면 어버이신은 그 진실한 마음을 받아들여 어떤 구제도 모두 즐거운 근행의 리로써 나타낼 것이다. "
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 月日에게는 온 세상 사람들이 모두 다 자녀\n 구제하고자 하는 마음뿐이야 8-4\n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 그런데도 저지를 당해 섭섭해\n 더구나 그 뒤는 없애 버리기까지 8-5\n",
                            japanese = "",
                            english = "",
                            commentary = "어버이신에게는 온 세상 사람들이 모두 다 자녀이므로 오직 구제하고자 하는 마음 이외에 아무것도 없다. 그런데도 구제근행을 못하게 할 뿐만 아니라, 나중에는 신병마저 없애 버리니 참으로 유감천만이 아닐 수 없다.\n 제5호 56,57 및 제6호 70수의 주석 참조."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 그래서 구제근행을 못하게 되니\n 월일의 마음 얼마나 섭섭하랴 8-6\n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 근행도 月日이 차츰차츰 손짓을 가르쳐\n 그것은 인간의 마음이 아니다 8-7\n",
                            japanese = "",
                            english = "",
                            commentary = "이 근행은 어버이신이 직접 여러가지로 손짓을 가르치는 만큼, 그것은 인간마음으로 하는 것이 아니다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 무슨 일을 해도 인간의\n 마음이 있다고는 전혀 생각 말라 8-8\n",
                            japanese = "",
                            english = "",
                            commentary = "비단 근행뿐만 아니라 무슨 일을 해도 그것은 모두 어버이신이 하는 것이므로, 결코 인간마음으로 한다고는 생각지 마라."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 여기서 하는 어떤 이야기도 月日이니라\n 어떤 준비도 모두 月日이야 8-9\n",
                            japanese = "",
                            english = "",
                            commentary = "터전에서 교조가 하는 이야기는 모두 어버이신이 하는 것이며, 또 어떤 준비를 하는 것도 모두 어버이신이 하는 것이다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 어떤 장애가 생기더라도 인간의\n 마음은 결코 있다고 생각 말라 8-10\n",
                            japanese = "",
                            english = "",
                            commentary = "그러므로 몸에 어떤 장애가 생기더라도 그것은 어버이신이 하는 것이므로, 거기에 인간마음이 있다고는 결코 생각지 마라."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이 세상을 창조한 月日이므로\n 무슨 일이든 모르는 것이 없다 8-11\n",
                            japanese = "",
                            english = "",
                            commentary = "이 세상 인간을 창조한 어버이신이기 때문에 무슨 일이든 모르는 게 없다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 온 세상 사람들의 가슴속\n 月日에게 모두 비치느니라 8-12\n",
                            japanese = "",
                            english = "",
                            commentary = "온 세상 사람들의 마음은 아무리 사소한 것일지라도 거울에 비치듯 어버이신의 마음에 모두 비친다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 그것을 모르고서 모두 인간마음으로\n 제 몸 생각만을 일삼고 있어서 8-13\n",
                            japanese = "",
                            english = "",
                            commentary = "그것을 모르고 온 세상 사람들은 항상 인간마음으로 제 몸 생각만을 일삼고 있다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 앞으로는 모든 일에 차츰차츰 진실한\n 길을 가르쳐 줄 것이니 8-14\n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이 세상을 창조한 것은 月日이니라\n 무엇이든 자세하게 모두 가르치리라 8-15\n",
                            japanese = "",
                            english = "",
                            commentary = "원래 이 세상을 창조한 것은 어버이신이므로 무엇이든 만가지를 자세히 가르치는 것이다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 그러기까지는 아무도 아는 자 없다\n 무엇이든 月日의 생각뿐이므로 8-16\n",
                            japanese = "",
                            english = "",
                            commentary = "어버이신이 가르치기 전에는 누구도 창조의 과정을 아는 사람이 없었다. 그것도 당연한 것이 이 세상 만물을 오직 어버이신의 마음에서 비롯되었기 때문이다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 오늘까지도 무슨 일이든 月日이라고\n 일러 왔지만 아직 모르고 있는 거야 8-17\n",
                            japanese = "",
                            english = "",
                            commentary = "지금까지 이 세상 만물을 창조하고 수호하는 것은 모두 어버이신이라고 말해 왔으나, 모든 사람들은 아직 그것을 모르고 있는 것 같다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 단단히 들어라 이 세상 창조한 진실을\n 자세히 일러주었지만 8-18\n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 세상에는 아무도 아는 자 없다\n 무슨 말을 해도 알아듣지 못한다 8-19\n",
                            japanese = "",
                            english = "",
                            commentary = "단단히 들어라. 어버이신이 이 세상을 창조한 진실한 이야기를 자세히 일러주었지만, 지금까지 세상에서 아무도 아는 자가 없을 뿐만 아니라, 아무리 일러주어도 알아듣지를 못하니 참으로 안타까운 일이다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 그러리라 이 세상 창조 이래 없던 일만을\n 차츰차츰 일러주는 것이므로 8-20\n",
                            japanese = "",
                            english = "",
                            commentary = "그것도 당연한 것이 이 세상이 창조된 이래로 지금까지 없던일만을 일러주기 때문에 알아듣지 못하는 것도 무리는 아니다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이 세상 창조한 진실을\n 알려 두지 않을 것 같으면 8-21\n",
                            japanese = "",
                            english = "",
                            commentary = "어버이신이 이 세상을 창조한 진실한 이야기를 일러주지 안으면 아무것도 모를 것이므로 반드시 일러주어야겠다"
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 지금까지도 구제한줄기라 자주 말해 왔으나\n 본진실을 모르기 때문에 8-22\n",
                            japanese = "",
                            english = "",
                            commentary = "지금까지 구제한줄기라는 말은 자주해 왔으나, 사람들은 어버이신이 이 세상을 창조한 깊은 의도를 모르기 때문에 그 뜻을 충분히 납득하지 못했던 것이다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 무슨 말이든 月日이 하는 거야\n 이것 진실이라 생각하고 듣는다면 8-23\n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 무슨 일이든 차츰차츰 일러준다\n 이것을 진실이라 생각하고 잘 들어라 8-24\n",
                            japanese = "",
                            english = "",
                            commentary = "무슨 말이든 이 세상을 창조한 어버이신이 하는 말이니 이것을 진실이라 믿고 듣는다면, 무슨 일이든 차차로 일러주겠다. 그러므로 이것을 참말이라 믿고 잘 듣도록 하라."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이 세상의 으뜸이라 하는 곳은\n 이 장소밖에 달리 없느니라 8-25\n",
                            japanese = "",
                            english = "",
                            commentary = "이 세상 인간을 창조한 으뜸인 본고장은 이 터전 이외에 달리 아무데도 없다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이 이야기는 어떤 것이라 생각하는가\n 어떤 이야기도 모두 하고 싶어서 8-26\n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이 세상은 창조한 진실을\n 모든 사람들은 알아차리지 않으면 8-27\n",
                            japanese = "",
                            english = "",
                            commentary = "이 세상을 창조한 어버이신의 깊은 의도를 온 세상 사람들이 알지 못하면 무엇을 말해도 모르게 된다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 어떤 구제를 할지라도 흔히 있는\n 그런 것은 말하지 않으니 8-28\n",
                            japanese = "",
                            english = "",
                            commentary = "어버이신이 어떤 구제를 할지라도 세상에 흔히 있는 일 따위는 말하지 않으므로 잘 모르겠지만, 사실은 그 점을 잘 깨닫지 않으면 안된다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 지금까지 있었던 일 보이는 일\n 그런 것은 말하지 않는 거야 8-29\n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이제까지 없던 일만을 일러줘서\n 진실한 구제를 할 거야 8-30\n",
                            japanese = "",
                            english = "",
                            commentary = "과거에 있었던 일이나 현재 나타나 있는 일 따위는 말하지 않는다. 지금까지 그 누구도 체험하지 못한 새로운 진리를 계시하여 온 세상 사람들을 근본적으로 구제할 테다"
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이 구제 어떤 것이라 생각하는가\n 마마하지 않도록 수호부 줄 준비를 8-31\n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 또 순산구제의 자요자재 언제라도\n 늦추기도 당기기도 하리라 8-32\n",
                            japanese = "",
                            english = "",
                            commentary = "이 진실한 구제란 어떤 것이라 생각하는가. 그것은 수호부를 만들어 주어서 마마를 하지 않도록 하고, 또 순산허락을 내려서 자유자재로 산기를 늦추기도 하고 당기기도 하는 그런 구제이다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이토록 자유자재를 하는 것도\n 예사로운 일이라고는 전혀 생각 말라 8-33\n",
                            japanese = "",
                            english = "",
                            commentary = "이같은 자유자재한 구제를 하는 것도 예사로운 일이 아닌 만큼 결코 가볍게 생각해서는 안된다. 어버이신으로서는 깊고 깊은 생각이 있어서 하는 일이다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 月日은 여간아닌 마음을 기울이는데도\n 온 세상은 아직도 세상 보통 일로 여겨 8-34\n",
                            japanese = "",
                            english = "",
                            commentary = "어버이신은 여간아닌 마음을 기울이고 있는데도, 세상 사람들은 전혀 어버이신의 마음을 헤아리지 못하고 그저 세상 보통 일로만 여기고 있다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이 세상을 창조한 본심을\n 일러주지 않고서는 8-35\n",
                            japanese = "",
                            english = "",
                            commentary = "어버이신이 이 세상을 창조한 본심 (本心)을 일러주지 않으면 아무것도 모를 것이다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이곳 근행장소는 인간을\n 창조해 낸 장소인 거야 8-36\n",
                            japanese = "",
                            english = "",
                            commentary = "이 근행장소는 태초에 인간을 창조한 으뜸인 터전이다.\n근행장소란 구제근행을 행하는 장소란 뜻으로서, 현재는 으뜸인 터전에 세워져 있는 본부 신전이다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 인간을 창조한 이 어버이는\n 존명으로 있다 이것이 진실이야 8-37\n",
                            japanese = "",
                            english = "",
                            commentary = "원래 없던 인간을 창조한 어버이는 현재의 교조로서 실제로 살아 있다. 이것이 확실한 사실이다.\n이 노래는 어버이신님이 교조님을 현신으로 삼아 직접 이 세상에 나타나 계시는 것을 말씀하신 것이다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이 이야기를 정말이라 여기는 자\n 어디에도 전혀 없으리라 8-38\n",
                            japanese = "",
                            english = "",
                            commentary = "이 이야기를 정말이라 여기고 듣는 사람은 이 세상 어디를 찾아보아도 없을 것이다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이처럼 없던 일만을 말하는 것은\n 이것이 진실이며 모두 참이기 때문이야 8-39\n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 지금까지 없던 세계를 창조한 것은\n 모르는 일을 가르치기 시작해서 8-40\n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이번에도 없던 일 모르는 일만을\n 말해서 또 가르침을 시작한다 8-41\n",
                            japanese = "",
                            english = "",
                            commentary = "어버이신은 원래 없던 세계를 창조하려고 지금까지 아무도 모르는 일을 가르치기 시작했는데, 이번에도 이제까지 없던 일과 아직 사람들이 모르는 일들을 일러주어서 이 가르침을 시작한다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 어떤 자도 모르는 일뿐이니\n 이것을 가르치는 月日의 의도 8-42\n",
                            japanese = "",
                            english = "",
                            commentary = "아무도 모르는 일만 가르치기 시작하는 것은 어버이신에게 깊은 의도가 있기 때문이다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 月日은 차츰차츰 모르는 일만을\n 무엇이든 가르쳐서 세계를 구제하리라 8-43\n",
                            japanese = "",
                            english = "",
                            commentary = "어버이신은 점차로 사람들이 전혀 모르는 일만을 가르치서 세계 인류를 구제할테다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이토록 생각하는 月日의 걱정을\n 세상 사람들은 아무것도 모르고서 8-44\n",
                            japanese = "",
                            english = "",
                            commentary = "온 세상 사람들을 구제하려고 이토록 애쓰고 있는 어버이신의 마음을 사람들은 전혀 모르고서, 오직 인간마음으로 제 몸 생각만을 하고 있다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 마음의 진실만 月日이 받아들인다면\n 어떤 구제도 모두 맡아 주리라 8-45\n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 어떤 구제를 말하는 것도 진실한\n 어버이가 있으므로 月日 말하는 거야 8-46\n",
                            japanese = "",
                            english = "",
                            commentary = "어버이신이 어떤 구제도 맡겠다고 말하는 것도 진실한 어버이인 교조가 있기 때문이다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이 月日 으뜸인 터전과 으뜸인\n 인연 있으므로 자유자재를 8-47\n",
                            japanese = "",
                            english = "",
                            commentary = "어버이신이 자유자재한 수호를 하는 것은 태초에 인간을 창조한 으뜸인 터전과 진실한 어버이가 있기 때문이다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이 이야기 왜 이처럼 되풀이하느냐 하면\n 구제한줄기를 맡는 근본이므로 8-48\n",
                            japanese = "",
                            english = "",
                            commentary = "이러한 이야기를 왜 이처럼 되풀이해서 일러주는가하면, 그것이 곧 구제한줄기를 보장해 주는 근본이기 때문이다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이 근본은 어디를 찾아보아도\n 아는 자는 전혀 없으리라 8-49\n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 그러리라 月日 몸 안에 들어가서\n 이야기하는 것은 지금이 처음이야 8-50\n",
                            japanese = "",
                            english = "",
                            commentary = "이 만가지 자세한 근본을 어디에 가서 찾아보아도 누구 하나 아는 사람이 없을 것이다. 그것도 그럴 것이 어버이신이 사람 몸 속에 들어가서 이야기하는 것은 이번이 처음이기 때문이다.\n月日 몸 안에 들어가서란 어버이신님이 교조님이 현신으로 삼아 그 몸 속에 드신 것을 말한다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이 세상 삼라만상을 보고 있는 月日이므로\n 어디 일인들 모르는 것이 없다 8-51\n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 月日이 각각 모두 살펴서\n 선과 악을 분간할 거야 8-52\n",
                            japanese = "",
                            english = "",
                            commentary = "어버이신은 이 세상 삼라만상을 굽어보고 있기 때문에 세상 어디 일인들 모르는 것이 없다. 그러므로 어버이신은 인간들이 생각하고 행하는 일들을 잘 살펴서 선악을 구별해 섭리할 것이다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 月日이 왜 이처럼 되풀이하느냐 하면\n 악한 일 나타나는 것이 안타까워서 8-53\n",
                            japanese = "",
                            english = "",
                            commentary = "어버이신이 왜 이렇게 되풀이해서 일러주는가 하면, 악한 일이 나타나는 것이 가엾기 때문이다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 차츰차츰 은혜가 중첩된 그 다음에는\n 우마로 떨어지는 길이 있으므로 8-54\n",
                            japanese = "",
                            english = "",
                            commentary = "인간은 어버이신의 깊은 의도에 의해 창조되고, 또 어버이신의 은혜로 살아가고 있다. 그런데도 불구하고 그 은혜에 대한 고마움을 모르고, 또 보은의 길을 걷지도 않고서 제멋대로 살아가면 은혜가 중첩이 되어 끝에 가서는 우마의 길로 떨어지게 되는데, 어버이신으로서는 이것이 못내 가엾은 일이다.\n 제 5호 4의 주석 참조."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 어떤 자도 月日이 진실을\n 받아들이면 모두 구제할 테야 8-55\n",
                            japanese = "",
                            english = "",
                            commentary = "그렇지만 아무리 티끌이 쌓여 신상이나 사정으로 고민하고 괴로워하는 사람이라도, 참으로 마음을 바꿔 성진실한 마음으로 원한다면 어버이신은 그 마음을 받아들여 반드시 모두 구제하겠다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 지금까지 어떤 이야기를 했어도\n 아무것도 나타난 것은 없었지만 8-56\n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이제까지 모두 나타난 일일지라도\n 근본을 전혀 모르기 때문에 8-57\n",
                            japanese = "",
                            english = "",
                            commentary = "지금까지 어떤 이야기를 해도 실제로 눈앞에 나타난 일은 아무것도 없었다. 또, 설사 나타났더라도 사람들은 전혀 근본을 모르기 때문에 눈으로 보기만 할 뿐 그 의미를 몰랐던 것이다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 뇌성벽력도 지진  태풍 홍수도\n 이것은 月日의 섭섭함과 노여움 8-58\n",
                            japanese = "",
                            english = "",
                            commentary = "五十八, 제 六호 九十一, 二十六 참조."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이것을 지금까지 아무도 모르므로\n 이번에 月日이 미리 알려 둔다 8-59\n",
                            japanese = "",
                            english = "",
                            commentary = "이것을 지금까지는 아무도 몰랐으므로 이번에 어버이신이 미리 알려 두는 것이다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 月日에게는 모든 사람들이 다 자녀다\n 한없이 귀여워하고 있지만 8-60\n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 사람들은 모두 각자 가슴속에\n 티끌이 가득히 쌓여 있으므로 8-61\n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이 티끌 깨끗하게 청소하지 않으면\n 月日이 아무리 생각한다 해도 8-62\n",
                            japanese = "",
                            english = "",
                            commentary = "어버이신에게는 이 세상 모든 사람들이 다 자녀이다. 그래서 한없이 귀여워하고는 있지만, 각자 마음에 티끌이 가득히 쌓여 있어서 이것을 깨끗이 털어버리지 않으면, 어버이신이 아무리 자녀를 생각한다 하더라도 이 어버이마음을 납득시킬 수가 없다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 月日은 두렵고 위태로운 길을\n 염려하고 있지만 각자는 모르고서 8-63\n",
                            japanese = "",
                            english = "",
                            commentary = "어버이신은 자녀들이 두렵고 위태로운 길을 걷고 있는 것을 염려하고 있으나, 모두들은 이러한 어버이신ㅇ의 마음을 전혀 모르고 있다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 아무리 높은 곳이라 할지라도\n　月日에게는 모두가 다 자녀야 8-64\n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 그것을 모르고서 어버이가 하는 일을 저지하고\n 또 없애 버리니 이것은 무슨 까닭인지 8-65\n",
                            japanese = "",
                            english = "",
                            commentary = "아무리 신분이 높은 사람이라 할지라도 어버이신에게는 모두 다 귀여운 자녀이다. 그런데도 이것을 모르고 어버이가 하는 일을 저지할 뿐만 아니라, 심지어는 신명까지 없애 버ㅣㄹ니 도대체 이건 무슨 심사인가.\n 1874년 음력 11월 15일에 나라현쳥(奈良縣廳) 사사계(社寺係)에서는 교조님과 그 측근들을 야마무라고뗑으로 호출하여 취조를 했고, 17일에는 나라중교원에서 쯔지, 마쯔오, 나까따 등, 세 사람을 소환하여, 천리왕님이란 신은 없다면서 신앙을 금지시키는 한편, 여페, 거울, 발 등을 몰수하는 사건이 있었는데, 위의 노래는 이에 대한 어버이신님의 격한 노여움을 나타내고 있다.(第五호 五十六, 五十七, 제 六호 七十 및 第八호 五 참조)"
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 月日은 지금까지 어떤 일에도\n 나타난 일은 없었지만 8-66\n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이번에는 가슴속을 깨끗이\n 털지 않고서는 다음 준비를 못해 8-67\n",
                            japanese = "",
                            english = "",
                            commentary = "지금까지는 어떤 일에도 어버이신이 세상 밖에 나타나 자유자재한 섭리를 한적이 없었으나, 이번에는 직접 밖에 나타나 사람들의 마음에 쌓이고 쌓인 티끌을 깨끗이 털어냄으로써 어비이신의 섭섭한 마음을 풀지 않고서는 앞으로 만가지구제의 준비를 할 수가 없다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이다음에는 어떤 자도 한결같이\n 구제하고자 하는 수단뿐이야 8-68\n",
                            japanese = "",
                            english = "",
                            commentary = "다음에는 어떤 사람도 다 구제할 수 있는 방법을 강구한다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 앞으로는 구제한줄기를 시작하면\n 어떤 자도 융솟음칠 뿐이야 8-69\n",
                            japanese = "",
                            english = "",
                            commentary = "앞으로 구제한줄기를 시작한다면 어떤 사람도 마음이 용솟음칠 것이다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 무엇이든　月日이 한번 말한 것은\n 틀리는 법이 없는 거야 8-70\n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 지금까지도 되풀이해서 일러두었다\n 그러나 마음에 깨달음이 없으므로 8-71\n",
                            japanese = "",
                            english = "",
                            commentary = "지금까지도 여러 번 되풀이해서 일러 주었으나, 아직 마음에 깨달음이 없으므로 다시 되풀이해서 일러준다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 단단히 들어라 갈은 인간인 양\n 생각하고 있는 것은 틀린 거야 8-72\n",
                            japanese = "",
                            english = "",
                            commentary = "단단히 들어라, 어버이신의 현신인 교조를 같은 인간으로 생각하고 있는데, 그것은 매우 잘못된 생각아이다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 어떤 일을 가르치기 시작하는 것도\n 으뜸인 어버이가 아니고서는 안 돼 8-73\n",
                            japanese = "",
                            english = "",
                            commentary = "어떤 일을 차르치는 것도 으뜸인 어버이가 아니고서는 할 수 없는 일이다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 지금까지는 무엇이든 가르치쳐서 온 것도\n 지금ㄲ가지는 무엇이든 가르쳐서 온 것도\n 모두 이와 같이 시작한 거야 8-74\n",
                            japanese = "",
                            english = "",
                            commentary = "지금까지 어버이신이 무엇이든지 가르쳐 온 것도 모두 이와 같은 이유 때문이다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 인간을 창조한 어버이가 또 한 사람\n 어디 있다면 찾아보라 8-75\n",
                            japanese = "",
                            english = "",
                            commentary = "인간을 창조한 어버이가 달리 또 있다면 어디든 가서 찾아보라"
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이처럼 모르는 일을 차츰차츰\n 말하고 있지만 이것이 진실이야 8-76\n",
                            japanese = "",
                            english = "",
                            commentary = "이처럼 사람들이 아직 모르는 일만을 일러주고 있지만, 그것은 모두 진실한 이야기이다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 나날이 모르는 일이나 없던 일을\n 가르치는 것이 月日의 낙이야 8-77\n",
                            japanese = "",
                            english = "",
                            commentary = "나날이 사람들이 아직 모르는 일이나 지금까지 세상에서 볼 수 없던 일을 가르쳐, 자녀의 마음이 성인하는 모습을 보는 것이 어버이신으로서는 더할 나위 없는 낙이다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이 세상 인간을 창조한 어버이에게\n 하늘의 혜택이 있다고 들었지만 8-78\n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이 이야기 어떤 것인지 조금도 몰라\n 月日이 식물을 주려고 하는 거야 8-79\n",
                            japanese = "",
                            english = "",
                            commentary = "이 세상 인간을 창조한 어버이에게 하늘의 혜택이 있을 것라는 말은 일찍이 들었을 테지만, 그 이야기가 무엇을 뜻하는지는 전혀 몰랐을 것이다. 그것은 어버이신이 하늘에서 식물(食物)을 내려 주는 일이다.\n 식물이란 교조님에게 어버이신님이 내려주시는 수명약(壽命藥)이다. 이것은 감로대에 평발(平鉢)을 얹어서 받도록 되어 있다. 그리고 어버이신님은 이 식물을 교조님의 마음에 따라 자녀들에게 주도록 허락하셨다.(제 九호 五十五～六十 참조)"
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이 이야기 어떤 것인가 하면\n 감로대에 평발을 얹어서 8-80\n",
                            japanese = "",
                            english = "",
                            commentary = "이 식물은 어떻게 주는가. 감로대에 평발을 얹어 두면 거기에 어버이신이 하늘에서 내려 주는 것이다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 앞으로는 어기저기 몸의 장애\n 月日의 손질이라 생각하라 8-81\n",
                            japanese = "",
                            english = "",
                            commentary = "앞으로는 어버이신이 사람들의 마음을 깨우치기 위해, 여기저기 몸에 장애를 주어서 손질을 할 것이니 그리 알아라."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 돌아왔으면 제 몸의 장애와 비교하여\n 같은 것이거든 빨리 청소를 8-82\n",
                            japanese = "",
                            english = "",
                            commentary = "몸에 손질을 받아 터전에 돌아오면 집터 안에 먼지나 티끌이 없는지 잘 살펴서, 만약 티끌이 있으면 제 몸의 장애와 비교해 보고 빨리 이것을 청소토록 하라."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 청소한 곳을 거닐다 멈추어선\n 그 자리에다 감로대를 8-83\n",
                            japanese = "",
                            english = "",
                            commentary = "이처럼 제 마음의 티끌과 집터 안의 티끌을 깨끗이 청소한 다음, 그 깨끗한 집터 안을 거닐다가 발이 멈춘 곳에 감로대를 세워라.\n 교조님이 먼저 몸소 집터 안을 거닐다가 발이 멈춘 곳에 표시를 한 다음, 고깡님을 비롯하여 신앙심이 돈독한 두세 사람에게 눈을 가린 채 집터 안을 거닐게 하셨다. 그러자 같은 지점에서 모두 발이 땅에 들러붙듯 움직일 수가 없었다. 이리하여 감로대를 세울 으뜸인 터전은 1875년 음력 5월 26일에 결정되었다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 세우고 나면 그 다음에 근행의 손을 갖추어\n 빨리 시작하라 마음 용솟음칠 거야 8-84\n",
                            japanese = "",
                            english = "",
                            commentary = "감로대를 세우고 나면 그 다음에는 근행인원을 갖추어서 빨리 신악근행을 시작하라. 그러면 모두들의 마음은 절로 용솟음치게 된다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이것만은 어디를 찾아도 없는 것이니\n 이것이 온 세상의 중심의 기둥이야 8-85\n",
                            japanese = "",
                            english = "",
                            commentary = "감로대만은 터전 이외에 세상 어디를 찾아보아도 절대로 없다. 이것이야말로 어버이신의 세계 인류 창조와 구제한줄기의 깊은 의도를 나타내는 온 세상의 중심이 되는 기둥이다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이것만 확실히 이루어지면\n 어떤 자도 두려울 것 없다 8-86\n",
                            japanese = "",
                            english = "",
                            commentary = "감로대만 확실히 세우게 되면 어떤 사람도 두려울 것이 없다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 무엇이라 하든 진실한 증거를\n 보여 주지 않고서는 다음 준비를 못해 8-87\n",
                            japanese = "",
                            english = "",
                            commentary = "누가 무엇이라 하든 인간을 창조한 으뜸인 터전에 그 진실한 증거로서 감로대를 세우지 않으면, 앞으로 만가지구제의 준비를 할 수 없다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 아무리 높은곳에 있는 자에게도\n 자요자재로 이야기해 주리라 8-88\n",
                            japanese = "",
                            english = "",
                            commentary = "아무리 사회적 신분이 높은 사람일지라도 어버이신은 아무 거리낌이나 망설임없이 자유자재로 이야기를 들려줄 것이다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 지금까지는 무슨 말을 해도 인간의\n 마음인 양 생각하고 있었지만 9-1\n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이번에는 무슨 말을 해도 인간의\n 마음이 있다곤 전혀 생각 말라 9-2\n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 무슨 말이든 단단히 들어 다오\n 인간마음은 전혀 섞여 있지 않아 9-3\n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 月日은 무슨 말이든 확실히\n 말하기 시작할 테니 이것 들어 다오 9-4\n",
                            japanese = "",
                            english = "",
                            commentary = "一～四, 第七호 五十二～五十六 참조."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 月日이 몸 속에 든 두 사람을 각각\n 별실에 따로 있게 해 주었으면 9-5\n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 어쨌든 그 다음에 확실히 맡아서\n 구제할 테야 단단히 두고 보라 9-6\n",
                            japanese = "",
                            english = "",
                            commentary = "어버이신님이 몸 속에 든 두 사람을 따로 별실에 있게 해 준다면 앞으로 무엇을 원하든 반드시 이를 맡아 구제할 것이니 단단히 두고 보라.\n 두 사람이란 교조님과 고깡님을 가리킨다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 앞으로는 어떤 구제를 하는 것도\n 모두 맡는 근본이 있기 때문에 9-7\n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 무슨 말을 하더라도 흘리지 말고\n 단단히 새겨들어 알아차려 다오 9-8\n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 앞으로는 아무리 어려운 질병도\n 모두 맡아서 구제할 거야 9-9\n",
                            japanese = "",
                            english = "",
                            commentary = "이 일은 앞으로 어떤 것도 맡아서 구제하는 근본이므로, 어버이신이 하는 말은 무엇이든 흘리지 말고 단단히 명심해서 실행해 주기 바란다. 그렇게 하면 앞으로 어떤 난병이라도 모두 맡아서 반드시 구제할 테다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 인간에게 질병이란 없는 것이지만\n 이 세상 시작을 아는 자 없다 9-10\n",
                            japanese = "",
                            english = "",
                            commentary = "인간에게 질병이란 없는 것이지만, 어버이신의 인간 창조의 의도를 모르기 때문에 그 무지로 인하여 자신도 모르게 마음에 티끌을 쌓아 마침내 질병을 앓게 되는 것이다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이것을 알리고자 차츰차츰\n 수리와 거름으로 의사와 약을 9-11\n",
                            japanese = "",
                            english = "",
                            commentary = "즐거운 삶을 이상(理想)으로 하여 인간을 창조한 어버이신의 의도를 모든 사람들에게 알리고자, 먼저 수리와 거름으로 차츰 의약(医薬)을 가르쳐 그 실마리로 삼아 온 것이다. 수리(修理)와 거름(肥)이란 농삿일인 손질과 거름질이란 말로서, 어버이신님의 자비로운 어버이마음을 알리기 위한 보살핌이란 뜻이다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이제부터는 무엇이든 만가지를 모두 일러줄 테니\n 무슨 일이든 단단히 들어라 9-12\n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이제까지도 대체로 이야기해 주었지만\n 아직 말하지 아니한 진실 9-13\n",
                            japanese = "",
                            english = "",
                            commentary = "이제까지 어버이신의 뜻을 대강 일러주었으나, 아직 일러주지 못한 진실이 있는데 이제부터 그것을 일러주겠다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 오늘부터는 무슨 말을 할는지\n 月日의 마음은 진실로 서두른다 9-14\n",
                            japanese = "",
                            english = "",
                            commentary = "오늘부터 무슨 말을 할는지 모르나, 어버이신 매우 서두르고 있으니 그리 알고 단단히 듣도록 하라."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 나날이 무엇을 서두르는가 하면\n 月日 뛰어나갈 준비만을 9-15\n",
                            japanese = "",
                            english = "",
                            commentary = "제 二호 一의 노래와 같이 어버이신님은 한길을 내려고 서두르신다. 그것은 교조님이 터전에서 가르치는 것만으로는 세계에 이 길을 내기가 좀처럼 어렵기 때문이다. 그래서 어버이신님이 몸소 높은 곳이나 먼 곳으로 나아가 전세계에 이 길을 널리 퍼려고 서두르시는 것이다. 전날 교조님이 야마무라고뗑에 소환되어 가신 것도 이 길을 세계에 널리 알리시려는 어버이신님의 뜻이엇다. 그리고  제 五호 五十九의 노래에서,\n 이곳에 부르러 오는 것도 나오는 것도 신의 의도가 있기 때문에 라고 하신 말씀과 같이 경관의 간섭도, 연형이나 구류도 모두 어버이신님이 몸소 나아가 활동하시기 위한 준비임을 알 수 있다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이 이야기 단단히 듣고 알아차려라\n 무슨 일을 할지 모르는 거야 9-16\n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 앞으로 올 길의 양상 단단히\n 분간해 다오 月日 부탁이야 9-17\n",
                            japanese = "",
                            english = "",
                            commentary = "앞으로 전개될 구제한줄기의 길의 양상을 잘 분간해 주기를 어버이신은 단단히 부탁한다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 月日이 뛰어나간 것을 듣게 되면\n 감로대를 빨리 세우도록 9-18\n",
                            japanese = "",
                            english = "",
                            commentary = "어버이신이 몸소 밖으로 뛰어나가 활동하고 있는 것을 참으로 안다면 하루라도 빨리 감로대를 세우도록 하라."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 감로대 세울 곳을 확실히\n 터전으로 정할 마음작정을 9-19\n",
                            japanese = "",
                            english = "",
                            commentary = "감로대를 세울 터전을 결정한 것은 1875년 음력 5월 26일이다. 이봐ㄷ 앞서 1873년에는 이브리 이조오에게 분부하여 모형 감로대를 만들게 하셨다.(제 九호 四十五 참조)\n 어버이신님은 이미 그 당시부터 감로대의 건설을 서두르고 계셨던 것이다. 이 노래의 대체적인 뜻은 감로대를 세울 터전은 신의에 따라 이미 정해졌으므로, 그 기초 공사를 할 마음작정을 하라는 것이다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이것만 확실히 정해 두면\n 무슨 일이든 두려울 것이 없다 9-20\n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 月日이 뛰어나갈 곳을 잠깐 한마디하면\n 높은 곳과 먼 곳 9-21\n",
                            japanese = "",
                            english = "",
                            commentary = "어버이신이 어디로 뛰어나가 활동할 것인가를 잠깐 말해 두겠다. 그것은 윗사람들이 사는 높은 곳과 거리가 떨어진 먼 곳인데, 어버이신은 이곳으로 뛰어나가 자유자재한 수호로써 상류사회와 먼 외지에 이 길을 낸다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 그 이야기를 듣게 되면 모든 사람들은\n 얼마나 月日이 위대한가고 9-22\n",
                            japanese = "",
                            english = "",
                            commentary = "어버이신이 몸소 밖으로 뛰어나가 눈부신 활동을 하는 이야기를 듣게 되면 모든 사람들은 과연 어버이신의 활동은 위대하구나 하고 감탄할 것이다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 온 세상이 모두 차츰차츰 말하겠지\n 그날이 오면 속이 후련해지리라 9-23\n",
                            japanese = "",
                            english = "",
                            commentary = "이리하여 어버이신의 뜻은 온 세상 사람들에게 알려져 모두들은 입을 모아 그 덕을 찬양하게 될 것이다. 그때가 되면 비로소 울적했던 마음이 후련해질 것이다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 지금까지 38년 이전부터\n 사람들의 답답한 가슴속 참으로 안타까워 9-24\n",
                            japanese = "",
                            english = "",
                            commentary = "38년 이전부터 오늘에 이르기까지 어둡고 험난한 길을 걸으며 온갖 고생을 한 모두들의 괴로운 심정을 어버이신은 참으로 가엾게 생각한다.\n 38년에 대해서는 제7호 1수의 주석 참조."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이번에는 무슨 일이든 확실히\n 모든 사람들에게 알리는 거야 9-25\n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 알린다 해도 무엇을 알린단 생각하는가\n 으뜸인 어버이를 확실히 알린단 9-26\n",
                            japanese = "",
                            english = "",
                            commentary = "알린다 해서 무엇을 알린다고 생각하는가, 그것은 으뜸인 어버이를 확실히 알리는 것이다.\n 으뜸인 어버이란 교조님을 가리킨다. 교조님은 어버이신님의 현신으로서의 리와, 또 인간을 창조한 리에 의해서 이 가르침을 창시하셨다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이런 일을 말해서 알리는 것도\n 무엇 때문인지 아무도 모르겠지 9-27\n",
                            japanese = "",
                            english = "",
                            commentary = "이와 같은 일을 일러주는 것을 무엇 때문인지, 이에 대한 어버이신의 뜻은 아무도 모를 것이다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이 세상 모든 사람들을 진실로\n 구제하고 싶어서 알리기 시작하는 거야 9-28\n",
                            japanese = "",
                            english = "",
                            commentary = "자녀인 이 세상 모든 사람들을 구제하고자 하는 일념에서 어버이신의 깊은 뜻을 알리기 시작하는 것이다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 지금까지 없었던 구제를 하므로\n 근본을 알려 주지 않고서는 9-29\n",
                            japanese = "",
                            english = "",
                            commentary = "이 세상 창조이래 일찍이 없었던 구제를 하므로, 으뜸인 리를 알려 주지 않고서는 모든 사람들이 진실한 구제를 받을 수 없다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 지금까지 모르던 일을 가르치므로\n 으뜸인 어버이를 확실히  알린다 9-30\n",
                            japanese = "",
                            english = "",
                            commentary = "지금까지 누구도 모르던 일을 가르치는 것이기에 먼저 으뜸인 어버이를 확실히 알리지 않을 수 없다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 으뜸인 어버이를 확실히 알게 되면\n 무슨 일이든 모두 맡는다 9-31\n",
                            japanese = "",
                            english = "",
                            commentary = "으뜸인 어버이를 확실히 알게 되면 어떤 소원이라도 모두 맡아 구제하겠다.\n 이 노래는 월일의 현신이신 교조님을 참으로 어버이라 믿는다면, 어떤 진기한 구제도 마음에 따라 받을 수 있음을 가르쳐 주고 있다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이 이야기 인간이 한다고는 생각 말라\n 月日의 마음인 거야 9-32\n",
                            japanese = "",
                            english = "",
                            commentary = "이 이야기는 인간이 한다고 생각해서는 안된다. 어버이신이 자녀를 귀여워하는 일념에서 일러주는 것이다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 모든 시대 온 세상 사람들을 살펴 보라\n 질병에도 여러가지가 있다 9-33\n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이번에는 아무리 어려운 질병도\n 맡아서 구제하고 비방을 가르친다 9-34\n",
                            japanese = "",
                            english = "",
                            commentary = "비방(秘方)이란 다른 종교에서는 찾아볼 수 없는 진실한 구제라는 뜻이다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이제부터는 확실히 처방을 일러준다\n 무슨 말을 해도 알아차려 다오 9-35\n",
                            japanese = "",
                            english = "",
                            commentary = "처방이란 구제하는 길을 약에 비유해서 한 말이다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이번에 앓고 있는 질병은 괴로울 테지\n 다음에 올 일을 낙으로 삼아라 9-36\n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 앞서부터 철저히 미리 알려\n 둔 거야 생각해 보라 9-37\n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 무슨 일이든 앞서부터\n 미리 알린 다음에 시작한 일이야 9-38\n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이 이야기는 어떤 것이라 생각하는가\n 月日 자유자재 알리고 싶어서 9-39\n",
                            japanese = "",
                            english = "",
                            commentary = "이 4수는 고깡님에 대한 말씀이다.(제11호 25~40수 총주 참조)"
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 진실로 이 일을 어서어서\n 月日은 마음 서두르고 있지만 9-40\n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 곁의 사람에게 아무리 月日 부탁해도\n 분간해 듣지 않으니 얼마나 섭섭한지 9-41\n",
                            japanese = "",
                            english = "",
                            commentary = "어버이신은 이 일을 하루라도 빨리 알려 주고 싶은 일념에서 여러가지로 서두르고 있지만, 어버이신이 아무리 부탁하고 간곡히 일러주어도 곁의 사람들은 전혀 분간해 듣지 않으니 참으로 안타까워 견딜 수 없다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 무슨 말을 하더라도 지금의 일을\n 말한다고는 전혀 생각 말라 9-42\n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 차츰차츰 무슨 이야기를 하더라도\n 앞으로 올 일만을 말해 둔다 9-43\n",
                            japanese = "",
                            english = "",
                            commentary = "지금 무슨 말을 하더라도 현재 목적의 일을 말한다고 생각해서는 안된다. 어디까지나 앞으로 올 일에 대해서만 말해 두는 것이다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이제부터 무슨 이야기를 하느냐 하면\n 오직 감로대의 이야기만 9-44\n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 지금의 감로대라 하는 것은\n 임시 모형일 뿐이야 9-45\n",
                            japanese = "",
                            english = "",
                            commentary = "지금의 감로대란 1873년 이브리 이조오에게 분부하여 만든 목조 모형 감로대를 가리킨다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이제부터는 차츰차츰 단단히 일러준다\n 감로대의 준비만을 9-46\n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이 대를 약간 파내어 직경을\n 석 자로 해서 6각으로 하라 9-47\n",
                            japanese = "",
                            english = "",
                            commentary = "47수에서 64수까지의 노래는 감로대에 대한 말씀이다. 그 형태는 다음과 같다. 맨 밑단은 직경 3자에 높이 8치로 6각형. 제2단은 직경 2자 4치에 높이는 8치로 6각형. 제3단부터는 직경 1자 2치에 높이 6치로 6각형. 그리고 각 단의 중심에는 윗면에 직경 3치, 깊이 5푼의 둥근 구멍이 있고, 아랫면에는 역시 직경 3치, 높이 5푼의 혹이 있어 위에서 끼우게 되어 있다. 이 대의 맨 위에는 5되들이 평발(平鉢)을 얹어 놓는다. 이 감로대는 태초에 인간을 창조한 증거로서 본고장인 으뜸인 터전에 세운다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 지금까지 여러가지로 이야기해 온 것은\n 이 대를 세울 준비뿐이야 9-48\n",
                            japanese = "",
                            english = "",
                            commentary = "지금까지 여러가지로 일러준 것은 하루라도 빨리 이 대를 터전에 세우기 위한 준비 때문이었다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이것만 확실히 세워 두면\n 이무런 두려움도 위태로움도 없다 9-49\n",
                            japanese = "",
                            english = "",
                            commentary = "감로대만 확실히 으뜸인 터전에 세둬두면 아무 두려움도 위태로움도 없다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 月日의 지시에 따라 한 일을\n 이것 멈추게 하면 제 몸도 멈춘다 9-50\n",
                            japanese = "",
                            english = "",
                            commentary = "감로대를 세울 준비는 오로지 어버이신의 지시에 따라 하는 일이기 때문에, 만약 누구라도 이 일을 방해하는 사람은 숨이 끊겨 버릴 것이다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이것을 보고 진실로 훌륭하다고\n 이것은 月日의 가르침이기에 9-51\n",
                            japanese = "",
                            english = "",
                            commentary = "감로대를 세우게 되면 이것을 보고 참으로 고마운 일이다, 기쁜 일이다, 과연 진실한 어버이신의 가르침이다 하면서, 많은 사람들이 터전을 그리며 돌아오게 될 것이다"
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이 대가 세워지는 대로 근행한다\n 무슨 일이든 이루어지지 않는 것이 없다 9-52\n",
                            japanese = "",
                            english = "",
                            commentary = "감로대가 완성되면 곧 즐거운근행을 시작한다. 그렇게 하면 어떤 소원이라도 이루어지지 않는 게 없다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이 대도 언제 어떻게 하라 말하지 않아\n 이루어지면 근행할 거야 9-53\n",
                            japanese = "",
                            english = "",
                            commentary = "감로대 건설은 언제 어떻게 하라고 일일이 공정(工程)과 일정을 정해 주지는 않지만, 완공되는 대로 곧 즐거운근행을 시작할 것이다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이 근행만 시작하게 된다면\n 무엇이든 이루어지지 않는 것이 없는 거야 9-54\n",
                            japanese = "",
                            english = "",
                            commentary = "감로대를 둘러싸고 어버이신이 가르친 즐거운근행을 시작하게 되면 어떤 소원이라도 이루어지지 않는게 없다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이것을 보라 확실히 月日은 식물의\n 혜택을 어김없이 내려 주리라 9-55\n",
                            japanese = "",
                            english = "",
                            commentary = "단단히 두고 보라. 어버이신은 식물의 혜택을 틀림없이 내려 준다.\n 감로대가 완성되어 즐거운근행으로 기원하면 맨 윗단에 얹어 둔 평발에다 하늘에서 반드시 감로를 내려 주시는데, 이것이 곧 식물이며 하늘의 혜택이다.\n 「식물」은 제8호 79의 주석 참조."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 무슨 일이든 확실히 진실한\n 증거가 없으면 위태로운 일 9-56\n",
                            japanese = "",
                            english = "",
                            commentary = "무슨 일이든 틀림이 없다는 확실한 증거가 없으면 위태로워 믿을 수 없게 된다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이제부터는 무슨 일이든 차츰차츰\n 자세히 일러준다 이것 배반 말라 9-57\n",
                            japanese = "",
                            english = "",
                            commentary = "이제부터는 차츰 온갖 일에 대해 자세히 일러줄 테니, 어버이신의 말을 배반하지 않도록 하라."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이 이야기 다른 말이라 생각 말라\n 오직 감로대의 준비만을 9-58\n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이 대도 층계층계 쌓아 올려\n 또 그 위에는 두자 네치로 9-59\n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 그 위에 평발을 얹어 두면\n 그 다음에 확실히 식물을 줄 테야 9-60\n",
                            japanese = "",
                            english = "",
                            commentary = "제9호 47,55수의 주석 참조."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 식물을 누구에게 내려 주느냐 하면\n 이 세상 창조한 어버이에게 내려 준다 9-61\n",
                            japanese = "",
                            english = "",
                            commentary = "이 식물을 누구에게 내려 주는가 하면, 이 세상 인간을 창조한 어버이에게 주는 것이다\n 이 세상 인간을 창조한 어버이란 이 세상을 창조하신 어버이신님의 리를 받은 교조님을 가리킨다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 하늘로부터 혜택을 받는 그 어버이의\n 마음을 누구도 아는 자 없다 9-62\n",
                            japanese = "",
                            english = "",
                            commentary = "하늘로부터 직접 식물의 혜택을 받는 그 어버이의 마음을 이세상의 어느 누구도 아는 사람이 없다.\n 어버이의 마음이란 교조님의 마음으로서, 그 마음은 곧 월일 어버이신님의 마음이다.(제12호 67,68수 참조)"
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 月日은 확실히 마음을 살펴서\n 그 다음에 내려 줄 식물인 거야 9-63\n",
                            japanese = "",
                            english = "",
                            commentary = "어버이신은 세상 사람들의 마음이 확실히 맑혀졌는지를 살펴본 다음에 자유자재한 효험이 있는 이 식물을 교조에게 내려줄 것이다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 月日이 이것을 내려 주면\n 다음에는 어버이의 마음에 따라 9-64\n",
                            japanese = "",
                            english = "",
                            commentary = "어버이신이 교조에게 이 식물을 내려주면 그 다음에는 교조가 임의로 모든 사람들에게 내려 주게 된다."
                        )
                    )

                    allContent.add(
                        ContentItem(
                            korean = " 진실한 마음을 月日이 살펴보고\n 하늘에서 내려 주는 혜택이란 10-1\n",
                            japanese = "",
                            english = "",
                            commentary = "진실한 마음을 살펴본 다음, 어버이신이 하늘에서 내려 주는 혜택을 어떻게 생각하고 있는가."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 대수롭지 않다고는 결코 생각 말라\n 하늘로서는 깊은 의도가 있으므로 10-2\n",
                            japanese = "",
                            english = "",
                            commentary = "이것을 별로 대수롭지 않는 것으로 생각하서는 결코 안된다. 어버이신으로서는 깊은 의도가 있어서 내려 주는 것이다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이 이야기 어떤 것인가 하면\n 미칠곳도 다음미칠곳도 마음 맑혀서 10-3\n",
                            japanese = "",
                            english = "",
                            commentary = "지금 어버이신이 하는 이 이야기가 무슨 뜻인가 하면, 온 세상 누구나 할 것 없이 아직 어버이신의 가르침을 전혀 모르고 사는 사람들의 마음도 모두 한결같이 맑힌다는 뜻이다.\n 미칠곳은 제2호 34수의 주석 참조.\n 다음미칠곳이란 맨 나중에 어버이신님의 뜻이 미칠 곳, 또는 맨 나중에 깨닫게 될 사람이라는 뜻이다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이 마음 어떻게 해서 맑히는가 하면\n 月日 뛰어나가 여기저기로 10-4\n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 차츰차츰 月日 몸 안에 들어가서\n 자유자재를 나타내기 시작하는 그야 10-5\n",
                            japanese = "",
                            english = "",
                            commentary = "이처럼 온 세상 사람들의 마음을 어떻게 해서 맑히는가 하면, 어버이신이 밖으로 뛰어나가 여기저기 점차로 사람들의 몸 속에 들어가서 자유자재한 수호를 나타냄으로써 맑히는 것이다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 그렇게 되면 아무리 모르는자라 할지라도\n 아는자에게는 당할 수 없다 10-6\n",
                            japanese = "",
                            english = "",
                            commentary = "그렇게 되면 이제까지 어버이신의 가르침을 모르고 인간마음으로 뽐내고 있던 사람들도, 어버이신의뜻을 깨달은 사람을 존경하고 그리워하게 된다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 나날이 나타나는 것으로 납득하라\n 언제 무슨 이야기를 들을 10-7\n",
                            japanese = "",
                            english = "",
                            commentary = "언제 무슨 이야기를 들을는지 모르니, 나날이 나타나는 어버이신의 수호를 보고 잘 납득하라."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 무슨 이야기를 듣더라도 앞서부터\n 이것은 月日의 이야기인 거야 10-8\n",
                            japanese = "",
                            english = "",
                            commentary = "인간으로서는 도저히 상상할 수 없는 이야기를 하라도, 그것은 모두 어버이신이 앞으로 있을 일을 미리 일러주는 것이므로 잘 들어 두도록 하라."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이제부터 아는자는 차츰차츰\n 月日 돌볼 테니 이것을 두고 보라 10-9\n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 무엇이든 어던 일이든 모두 가르쳐\n 모르는 일이 없도록 할 테다 10-10\n",
                            japanese = "",
                            english = "",
                            commentary = "이제부터 어버이신의 뜻을 알고 따라오는 사람에게는 차츰 무엇이든지 모두 가르쳐 도르는 일이 없도록 한다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 터전에 기둥을 세우게 되면\n 앓지 않고 죽지 않고 쇠하지 않도록 10-11\n",
                            japanese = "",
                            english = "",
                            commentary = "으뜸인 터전에 감로대를 세우게 되면, 질병으로 앓고나 요절하거나 쇠약해지는 일이 없어진다.\n 제3호 99수,100수, 제4호 37수, 제8호 79수의 주석 참조."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 지금까지는 모르는자들 뽐내며\n 멋대로 했었다 이번엔 갚음을 10-12\n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이 이야기 누구에게 어떻게 하라고 않는다\n 月日 뛰어나가 마음대로 하리리 10-13\n",
                            japanese = "",
                            english = "",
                            commentary = "지금까지는 어버이신의 뜻을 아직 모르는 사람들이 뽐내며 제멋대로 행동해 왔으나, 이번에는 그에 대해 갚음을 한다. 그 갚음도 어버이신이 어느 누구에게 명하여 하는 것이 아니라, 어버이신이 몸소 밖으로 뛰어나가 자유자재한 수호로써 한다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이제까지 달과 해를 모르는 자는 없다\n 그러나 근본을 아는 자는 없다 10-14\n",
                            japanese = "",
                            english = "",
                            commentary = "이제까지 달과 해를 모르는 사람은 없지만, 그것이 하늘에 나타난 어버이신의 모습이며, 이 어버이신이 만물을 창조하고 수호하고 있다는 사실을 아는 사람은 아무도 없다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이번에는 무슨 일이든 진실을\n 일러주어서 구제를 서두르는 거야 10-15\n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이 날짜는 언제쯤 되느냐 하면\n 논매기 작업이 끝나는 대로 10-16\n",
                            japanese = "",
                            english = "",
                            commentary = "논매기 작업 당시 집터 인근에서는 모내기를 한 후, 논매기 작읍을 3번 정도 했는데, 2번째는 입추 18일 전쯤이고, 3번째는 날씨에 따라 일정치는 않으나 순조로우면 입추 전후였다. 입추는 양력으로는 8월 7일이다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 그 다음에는 무엇인가 진기한 길이 되리라\n 근행인원 모두 모여 온다 10-17\n",
                            japanese = "",
                            english = "",
                            commentary = "그 다음에는 어버이신의 수호로 상상도 못했던 진기한 길이 나타날 것이며, 또 근행인원도 모두 갖추게 된다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 차츰차츰 나날이 마음 용솟음칠 거야\n 참으로 온 세상은 놀라운 풍년 10-18\n",
                            japanese = "",
                            english = "",
                            commentary = "그렇게 되면 날이 갈수록 사람들의 마음은 용솟음칠 것이며, 온 세상에는 어버이신의 혜택이 충만하게 될 것이다. 그리고 집터는 터전을 그리며 돌아오는 사람들로 붐비게 된다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 나날이 어서 근행을 서둘러라\n 어떤 재난도 모두 면하리라 10-19\n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 아무리 어려운 질병도\n 근행 하나로 모두 도움받으리라 10-20\n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 어떤 근행을 하느냐 하면\n 오직 감로대근행만을 10-21\n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이 대를 어떤 것이라 생각하는가\n 이것은 온 세상의 어버이인 거야 10-22\n",
                            japanese = "",
                            english = "",
                            commentary = "이 대를 어떻게 생각하고 있는가. 그것은 온 세상 사람들의 어버이인 것이다.\n 태초에 어버이신님은 으뜸인 터전에서 인간을 창조하셨는데, 그 증거로서 터전에다  감로대를 세우는 것이다. 따라서 이 대는 세상 모든 사람들의 생명의 근원이며 진기한 구제의 원천이다.(제10호 79수 및 제17호 2~9수 참조)"
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이것만 참으로 진실이라 생각하면\n 月日 분간하여 모두 맡는다 10-23\n",
                            japanese = "",
                            english = "",
                            commentary = "참으로 이러한 시실을 납득하고 믿는다면, 어버이신은 그 사람의 마음을 분간하여 어떤 수호도 모두 맡아 준다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 月日이 맡는다고 하는 이상\n 천의 하나도 틀림이 없다 10-24\n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이 이야기 어떻게 듣고 있는가\n 감로대근행이란 10-25\n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 간단한 근행이라고는 생각 말라\n 36인의 인원이 필요해 10-26\n",
                            japanese = "",
                            english = "",
                            commentary = "어버이신님이 일러주신 근행인원은\n 신악 10인\n 악기 9인\n 손춤 36인(6인 1조)\n이라 하셨는데, 이 이외에도 가꾸닝(악인 또는 학인) 20인이 더 있으므로, 모두 75인이다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 그중에 악기 넣어서 19인\n 신악근행의 인원이 필요하다 10-27\n",
                            japanese = "",
                            english = "",
                            commentary = "이 이야기를 어떻게 생각하고 듣고 있는가. 이 감로대근행을 간단한 것으로 생각해서는 안된다. 손춤 인원은 36인이다. 신악근행에는 신악 10인, 악기 9인이 필요하다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 진실로 마음을 작정하여 생각하라\n 전갈인들 단단히 부탁이야 10-28\n",
                            japanese = "",
                            english = "",
                            commentary = "전갈인들은 어버이신이 뜻하는 바를 잘 깨달아서 단단히 마음을 작정하여 감로대근행의 준비에 힘써 다오."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이 대를 만들려고 차츰차츰\n 月日 인원 준비하는 거야 10-29\n",
                            japanese = "",
                            english = "",
                            commentary = "감로대 건설의 제일보로서 어버이신은 차츰 근행인원을 갖출 준비를 시작하고 있다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 인원이 확실히 모이게 되면\n 저절로 대도 이루어지는 거야 10-30\n",
                            japanese = "",
                            english = "",
                            commentary = "근행인원만 확실히 갖추어지면 감로대는 저절로 서게 된다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이 순서는 어떤 것인가 하면\n 月日 근행의 손짓을 가르치는 것부터 10-31\n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 그 다음에 月日 한결같이 온 세상에\n 데리고 나가면 저절로 되는 거야 10-32\n",
                            japanese = "",
                            english = "",
                            commentary = "그 서게 되는 순서는 어떤가하면, 먼저 어버이신이 이 근행의 손짓을 가르친다음, 이를 배운 사람들이 어버이신의 지시에 따라 온 세상에 널리 가르치게 되면 감로대는 저절로 서게 된다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이것만 확실히 될 것 같으면\n 다달이 근행도 틀림이 없다 10-33\n",
                            japanese = "",
                            english = "",
                            commentary = "감로대가 서게 되고 근행의 손짓을 확실히 익히게 되면 다달이 올리는 감로대근행도 틀림없이 행할 수 있게 된다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 근행만 틀림없이 행하게 되면\n 하늘의 혜택도 틀림이 없다 10-34\n",
                            japanese = "",
                            english = "",
                            commentary = "다달이 근행만 틀림없이 행하게 되면 하늘의 혜택도 틀림없이 내려 준다"
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이 길은 성진실이라 어려운\n 길인 거야 모두들 생각하라 10-35\n",
                            japanese = "",
                            english = "",
                            commentary = "이 길은 정성의 길로서 성진실한 마음이 되기란 좀처럼 쉬운 일이 아니지만, 그러나 성진실한 마음이 되면 신기한 구제를 받을 수 있고, 참으로 즐거운 길이 열리므로 모두들은 이 점을 잘 생각해 다오."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이 인원 어디에 있는지 모르겠지\n 月日 분간하여 모두 이끌어 들인다 10-36\n",
                            japanese = "",
                            english = "",
                            commentary = "이 근행인원으로 뽑힐 사람이 어느 곳 누구인지는 아무도 모를 테지. 그러나 어버이신은 그런 사람의 마음을 분간하여 모두 이끌어 들인다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 어떤 곳에 있는 자라 할지라도\n 月日은 자유자재를 나타내 보일 테다 10-37\n",
                            japanese = "",
                            english = "",
                            commentary = "어버이신이 근행인원으로 쓰고자 하는 사람은 어디에 있더라도 자유자재한 섭리를 나타내어 반드시 이끌어 들인다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 차츰차츰 인원 갖추어진 그 다음에\n 진실을 보아서 역할을 정한다 10-38\n",
                            japanese = "",
                            english = "",
                            commentary = "차츰 근행인원이 갖추어지면, 그 다음에는 사람들의 진실을 살펴보고 근행의 역할을 정한다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 역할도 어떤  것인가 하면\n 신악 열 사람과 다음은 악기 10-39\n",
                            japanese = "",
                            english = "",
                            commentary = "제10호 26,27수의 주석 참조."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이것만 빨리 확실히 갖추어지면\n 무슨 일이든 안되는 것이 없다 10-40\n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 오늘부터는 차츰차츰 이야기를 바꾸어\n 지금까지 모르던 일만 말한다 10-41\n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 지금까지도 여러 가지 길이 있었지만\n 月日 가르치지 않은 것은 없는 거야 10-42\n",
                            japanese = "",
                            english = "",
                            commentary = "지금까지 세상에는 여러가지 가르침이 있었지만, 그것은 모두 인간들이 성인함에 따라 어버이신이 가르쳐 온 것이다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 月日이 대체로 무엇이든 차츰차츰\n 가르쳐 왔지만 10-43\n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이번에는 아직 그밖에 모르는 일\n 무엇이나 실을 모두 일러준다 10-44\n",
                            japanese = "",
                            english = "",
                            commentary = "어버이신은 인간이 성인함에 따라, 여러가지를 대체로 가르쳐 왔으나, 이번에는 아직 가르치지 않은 이 세상의 가장 중요한 근본에 대해서 일러준다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이제까지는 모르는자들이 뽐냈었다\n 이것도 月日이 가르쳐 온 거야 10-45\n",
                            japanese = "",
                            english = "",
                            commentary = "이제까지는 어버이신의 뜻을 전혀 모르면서도 단지 인간적인 지혜나 능력만 뛰어나면 그만이라고 생각하는 사람들이 뽐내고 있었는데, 이것 역시 인간들이 성인함에 따라 어버이신이 가르쳐 온 것이다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이번에는 月日이 근본으로 되돌아와\n 나무 뿌리 확실히 모두 나타낼 테야 10-46\n",
                            japanese = "",
                            english = "",
                            commentary = "이번에는 어버이신이 터전에 나타나 구제한줄기의 섭리를 모두 학실히 나타낼 것이다"
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이 세상 근본을 확실히 아는 자는\n 어디에도 전혀 없으리라 10-47\n",
                            japanese = "",
                            english = "",
                            commentary = "이 세상은 태초에 어터이신에 의해서 창조되었다는 근본을 확실히 아는 자는 이 넓은 세계에 아무도 없을 것이다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 진실로 이 근본만 확실히\n 알 것 같으면 어디에 가더라도 10-48\n",
                            japanese = "",
                            english = "",
                            commentary = "진실로 어버이신이 이 세상과 인간을 창조한 그 근본을 확시히 마음에 납득한다면, 세상 어디에 가더라도 두려울 것이 없다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이 이야기 어떻게 생각하고 듣고 있는가\n 이것을 전갈인에게 가르치고 싶은 거야 10-49\n",
                            japanese = "",
                            english = "",
                            commentary = "이 이야기를 어떻게 생각하고 듣고 있는가. 그것은 이 근본을 전갈인들에게 단단히 가르치고 싶기 때문이다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 무슨 일을 月日이 생각하고 있느냐 하면\n 인간의 근본을 온 세상에 10-50\n",
                            japanese = "",
                            english = "",
                            commentary = "어버이신이 무엇을 생각하고 있는가 하면, 인간창조의 근본을 온 세상 사람들에게 널리 가르치고 싶은 일이다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 어서어서 이 진실을 모든 사람들에게\n 알린다면 이야기를 깨닫게 될 거야 10-51\n",
                            japanese = "",
                            english = "",
                            commentary = "인간창조의 근본을 모든 사람들에게 알린다면, 그 다음에는 어떤 일도 모두 깨닫게 된다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 아무리 이야기를 일러주더라도\n 근본을 일러주지 않고서는 10-52\n",
                            japanese = "",
                            english = "",
                            commentary = "아무리 이야기를 해 주더라도 어버이신이 이 세상 인간을 창조한 근본을 일러주지 않으면 아무것도 모르게 된다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 근본만 확실히 일러주게 되면\n 무슨 말을 해도 모두 분간해 듣는다 10-53\n",
                            japanese = "",
                            english = "",
                            commentary = "이 근본만 확실히 일러주게 되면 무슨 말을 해도 모두 분간해 듣게 된다"
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이 세상 땅과 하늘은 진실한 어버이\n 거기서 생겨난 인간이니라 10-54\n",
                            japanese = "",
                            english = "",
                            commentary = "하늘과 땅은 진실한 어버이인 구니도꼬다찌노미꼬또(월덕수기의리)와 오모따리노미꼬또(일덕화기의리)의 리가 나타난 것이며, 인간은 어버이신의 수호로 그 사이에서 태어났기 때문에 모두 어버이신의 귀여운 자녀고 인간끼리는 모두 형제자매이다.\n 하늘은 달님인 구니도꼬다찌노미꼬또(월덕수기의리)의 리이고, 땅은 햇님인 오모따리노미꼬또(일덕화기의리)의 리이다. 태초에 월일 어버이신님은 이자나기노미꼬또(남자추형종자의리)와 이자나미노미꼬또(여자추형묘상의리)의 몸속에 듭시어 인간을 창조하셨다. 이 월일의 리는 현재 땅과 하늘의 모습으로 나타나 있으며, 인간은 그 사이에서 태어나 살아가고 있다. 즉, 땅과 하늘은 인간의 진실한 어버이이다. 인간은 천지를 포괄하시는 어버이신님의 품속에 안겨서 밤낮없이 베풀어 주시는 어버이신님의 수호로 살아가고 있다. 신악가에서\n 이 세상 땅과 하늘의 본을 받아서 부부를 점지하여 왔었으므로 이것이 이 세상의 시초이니라\n고 하신 말씀은 바로 이러한 뜻이다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이제부터는 미칠곳도 미친곳도 모르는\n 일만을 일러줄 테니 단단히 들어라 10-55\n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 무슨 일이든 모르는 것이 없도록\n 모든 사람들에게 가르치고 싶어서 10-56\n",
                            japanese = "",
                            english = "",
                            commentary = "이제부터는 세상 사람 누구도 모르는 이야기만 하겠는데, 그것은 무슨 일이든 모르는 것이 없도록 만가지를 자세히 가르치고 싶기 때문이니, 사람들은 인간을 한결같이 귀여운 자녀로 여기는 어버이신의 차별 없는 어버이마음을 잘 깨달아서, 어버이신이 하는 말을 단단히 들어주기 바란다.\n 미칠곳이나 미친곳이나 아무 차별을 하지 않는 것이 진실한 어버이마음이다. 즉, 어버이신님이 보실 땐, 미칠고이나 미친곳이나 모두 귀여운 자녀가 사는 곳이며, 먼저 안 사람이나 나중에 알 사람이나 다 같은 귀여운 자녀이다. 또 어버이신님의 뜻을 깨닫고 따라오는 사람도 반대하는 사람도 모두 귀여운 자녀이다. 따라서 궁극적으로는 온 세상 사람들을 모두 구제하시려는 것이 어버이신님의 구제한줄기의 참뜻이다. 제2호에서부터 미칠곳과 미친곳에 대해 가르쳐 오신 어버이신님의 뜻은 바로 여기에 있다.(제2호 34수의 주석 참조)"
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 나날이 月日이 생각하는 것은\n 많은 사람들의 가슴속을 맑히고 싶어서 10-57\n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이 마음을 어떻게 하면 알릴 수 있을까\n 어떻게든 빨리 이것을 알리고 싶어 10-58\n",
                            japanese = "",
                            english = "",
                            commentary = "어버이신이 나날이 염원하고 있는 것은 온 세상 사람들의 마음을 어떻게 하면 맑힐 수 있을까, 어떻게든 어버이신의 뜻을 알려서 하루라도 빨리 사람들의 마음을 맑혀야겠다는 것이다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 온 세상이 진실롤 가슴속을\n 알게 되면 月日은 즐거울 텐데 10-59\n",
                            japanese = "",
                            english = "",
                            commentary = "온 세상 사람들의 마음이 맑아져서 어버이신의 뜻을 참으로 깨닫게 된다면 이보다 더한 기쁨은 없을 텐데."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 그리고는 모든 사람들이 가슴속을\n 알게 되면 月日은 즐거울 텐데 10-60\n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 차츰차츰 나날이 마음 용솟음치게 해서\n 즐거움이 넘치는 삶을 모두에게 가르쳐서 10-61\n",
                            japanese = "",
                            english = "",
                            commentary = "온 세상 사람들의 마음이 참으로 맑아진다면, 그 다음에는 차츰 사람들의 마음을 용솟음치게 해서 이 세상을 즐거움이 넘치는 세상이 되도록 가르친다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 온 세상 맑은 사람들의 가슴속을\n 모두 맑히게 된다면 10-62\n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 그 다음에는 月日의 마음도 용솟음쳐서\n 무슨 일이든 모두 가르칠 테다 10-63\n",
                            japanese = "",
                            english = "",
                            commentary = "온 세상 많은 사람들의 마음이 모두 맑아지면, 어버이신의 마음도 절로 용솟음쳐서 무슨 일이든 모두 가르칠 테다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 무슨 일이든 月日이 진실로\n 모든 사람들에게 가르치고 싶은 거야 10-64\n",
                            japanese = "",
                            english = "",
                            commentary = "어버이신은 무슨 일이든 본진실을 귀여운 자녀인 온 세상 사람들에게 모두 가르치고 싶다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 진실한 마음을 바라는 月日은\n 무슨 일이든 가르치고 싶어서 10-65\n",
                            japanese = "",
                            english = "",
                            commentary = "어버이신은 무슨 일이든 가르치고 싶으니, 모두들은 진실한 마음이 되어 어버이신의 이야기를 잘 들어주기 바란다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이 이야기는 무엇을 가르친다고 생각하는가\n 이제부터는 앞으로 올 만가지 길의 과정을 10-66\n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 오늘까지는 아무것도 나타나지 않았지만\n 날이 다가오면 저절로 나타나리라 10-67\n",
                            japanese = "",
                            english = "",
                            commentary = "지금까지는 아무것도 나타나지 않았지만, 차차 날이 다가오면 자연히 만가지 수호가 나타나게 된다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 무엇이든 질병이라 생각 말라\n 무엇이든 만가지는 月日의 손질이야 10-68\n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 月日은 나날이 마음을 서두르며\n 어떤 준비를 할지 모르는 거야 10-69\n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 서두름도 무엇 때문인지 모르겠지\n 감로대의 준비만 10-70\n",
                            japanese = "",
                            english = "",
                            commentary = "서두른다고 해도 무엇을 서두르는지 모르겠지, 어버이신은 오직 감로대를 건설할 준비만을 서두르고 있는 것이다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 나날이 뭄의 장애가 나타나면\n 이것은 月日의 손질인 거야 10-71\n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 차츰차츰 살펴보니 온 세상은\n 안타까워서 양상을 바꾸고 싶어 10-72\n",
                            japanese = "",
                            english = "",
                            commentary = "차츰 온 세상을 두루 살펴보니 참으로 가엾은 사람이 많은지라. 어버이신은이것이 안타까워 어떻게 해서라도 이 세상의 모습을 바꾸고 싶다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 생각해 보라 입으로 무슨 말을 할지라도\n 확실한 증거 없으면 안돼 10-73\n",
                            japanese = "",
                            english = "",
                            commentary = "가만히 생각해 보라. 설사 입으로 무슨 말을 할지라도 확실한 증거가 없으면 믿을 수 없는 것이다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 月日이 몸 안에 들어가서\n 자유자재로 지시하는 것이 증거야 10-74\n",
                            japanese = "",
                            english = "",
                            commentary = "어버이신은 교조를 현신으로 삼아 자유자재로 여러 가지 지시를 하고 있는데, 이것이 무엇보다 확실한 증거이다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 그러므로 지금까지 어디에도 없던\n 일만을 말해서 시작할 테야 10-75\n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 지금까지 없던 일만을 말하는 것도\n 이것도 모두 月日의 가르침이야 10-76\n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이번에 감로대라 하는 것도\n 이것도 이제까지 모르는 일이야 10-77\n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 무슨 말을 하는 것도 모두 月日\n 모르던 일을 가르치고 싶어서 10-78\n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이 집터에 감로대를 세우는 것은\n 인간을 창조해 낸 증거 10-79\n",
                            japanese = "",
                            english = "",
                            commentary = "이 집터에 감로대를 세우는 것은 태초에 터전에서 인간을 창조했기 때문에 그 중거로서 세우는 것이다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 무엇이든 모두 이와 같이 시작해서\n 온 세상 사람들의 마음을 맑힌다 10-80\n",
                            japanese = "",
                            english = "",
                            commentary = "무엇이든지 이와 같이 으뜸인 리에 의거하여 이 길을 가르치기 시작하여, 머지않아 온 세상 사람들의 마음을 맑힐 것이다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 나날이 어떻든 온 세상 사람들을\n 용솟음치게 할 준비만 하는 거야 10-81\n",
                            japanese = "",
                            english = "",
                            commentary = "어버이신은 나날이 어떻게 해서라도 온 세상 사람들의 마음을 용솟음치게 하려고 오직 그 준비에만 마음을 기울이고 있다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 차츰차츰 세상 사람들의 마음 용솟음치면\n 농작물도 모두 용솟음친다 10-82\n",
                            japanese = "",
                            english = "",
                            commentary = "차츰 온 세상 사람들의 마음이 용솟음치게 되면 이에 따라 농작물도 용솟음쳐서 좋은 결실로 풍작을 이루게 된다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이 마음 어떻게 해서 용솟음치는가 하면\n 月日이 일꾼 데리고 나갈 거야 10-83\n",
                            japanese = "",
                            english = "",
                            commentary = "온 세상 사람들의 마음이 어떻게 해서 용솟치게 되는가 하면, 어버이신이 수족이 되는 일꾼을 데리고 세계 방방곡곡을 다니며 사람들을 구제하기 때문에 용솟음치게 되는 것이다.\n 일꾼이란 어버이신님의 수족이 되어 구제한줄기를 위해 노력하는 사람들을 말한다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 그때까지 여기저기서 어떤\n 이야기도 차츰차츰 모두들 듣겠지 10-84\n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 어떤 이야기를 듣는 것도 모두가 미리\n 말해 둔 거야 생각해 보라 10-85\n",
                            japanese = "",
                            english = "",
                            commentary = "그때까지 여기저기서 어러가지 이야기를 듣게 되겠지만, 그 모두가 다 이미 어버이신이 일러준 이야기뿐이다. 잘 생각해 보라."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 月日은 무슨 말을 차츰차츰 하는가 하고\n 생각하겠지만 앞으로 낙이 되는 거야 10-86\n",
                            japanese = "",
                            english = "",
                            commentary = "어버이신이 왜 이런 이야기를 하는가고 생각하겠지만, 그것이 장래 낙이 되는 것이다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 어떻든 月日의 생각에는\n 터전에 고오끼가 필요하므로 10-87\n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 터전에 고오끼를 확실히 만들어서\n 그것을 펴 나간다면 미칠곳도 마음대로 10-88\n",
                            japanese = "",
                            english = "",
                            commentary = "어버이신은 인간을 창조한 터전에 고오끼를 만들어 두고자 여러 가지로 마음을 다하고 있다. 터전에 구제한줄기의 근본이 되는 고오끼를 만들어서 그것을 온 세상에 펴 나간다면, 아직 어버이신의 뜻을 모르는 곳에도 가르침이 전해져 사람들의 마음을 맑히게 된다. 제5호 31수의 주석 및 제 10호 55~65수 참조"
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이 이야기 어떻게 생각하는가 모두들\n 아는자는 모두 자신의 일이야 10-89\n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 그것을 모르고서 어떻게 생각하고 윗사람들은\n 가슴속을 몰라 주니 月日 섭섭해 10-90\n",
                            japanese = "",
                            english = "",
                            commentary = "고오끼를 만들라는 어버이신의 이야기를 모두들은 어떻게 생각하고 듣고 있는가. 이미 어버이신의 뜻을 알고 있는 사람에게는 이것은 모두 자신의 일이다. 한편, 윗사람들은 어버이신의 이러한 마음을 모르고 있으니 참으로 섭섭한 일이 아닐 수 없다.\n 제2호 34수의 주석 및 제10호 55~65수 참조."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이곳에 어떤 고오끼가 만들어져도\n 이것은 온 세상의 보물인 거야 10-91\n",
                            japanese = "",
                            english = "",
                            commentary = "터전에서 만들어지는 고오끼는 어떤 것도 모두 어버이신의 뜻에 의해 만들어지는 것이므로, 이것은 온 세상의 보물인 것이다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 모든 사람들은 마음을 작정하여 생각하라\n 어서 고오끼를 기다리도록 하라 10-92\n",
                            japanese = "",
                            english = "",
                            commentary = "모든 사람들은 마음을 작정하여 생각하라. 빨리 고오끼가 만들어질 날을 기다리고 있거라."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 진실한 고오끼가 만들어진다면\n 무슨 일이든 月日 펴 나갈 테다 10-93\n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 月日이 펴 나간다 해도\n 모든 사람들은 납득하지 못하리라 10-94\n",
                            japanese = "",
                            english = "",
                            commentary = "어버이신이 고오끼를 온 세상에 펴 나간다 해도 말만으로는 어떻게 펴 나갈 것인지 사람들은 납득하지 못할 것이다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 그러므로 전갈인에게 단단히\n 부택해 둘 테니 명심해 두라 10-95\n",
                            japanese = "",
                            english = "",
                            commentary = "그러므로 이 고오끼를 온 세상에 널리 번하도록 전갈인에게 단단히 부탁하니 잘 명심해 두도록 하라."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이 날짜 각한이 올 것 같으면\n 언제 月日이 어디로 나갈지 10-96\n",
                            japanese = "",
                            english = "",
                            commentary = "고오끼가 만들어져 이것을 널리 펴야 할 시기가 온다면, 어버이신이 언제 어디로 나갈지 모른다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 나날이 전갈인들 단단히\n 마음을 가다듬어 빨리 시작하라 10-97\n",
                            japanese = "",
                            english = "",
                            commentary = "나날이 전갈인들은 단단히 마음을 가다듬어 이 고오끼를 하루라도 빨리 펴 나가도록 준비하라."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이 길은 어떤 길이라고 모두들은\n 생각하고 있는가 좀처럼 모르겠지 10-98\n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 月日은 어떻든 진실을\n 마음 굳건히 헤쳐 나갈 테야 10-99\n",
                            japanese = "",
                            english = "",
                            commentary = "어버이신은 어떻든 구제한줄기의 길을 펴 나가기 위해 마음을 굳게 세워 어떤 난관도 헤쳐 나갈 것이다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이 길이 위에 통하게 될 것 같으면\n 자유자재로 섭리하리라 10-100\n",
                            japanese = "",
                            english = "",
                            commentary = "이 구제한줄기의 길을 윗사람들이 알게 된다면, 어버이신은 자유자재한 섭리를 나타내 보일 것이다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 月日이 이 섭리를 나타낸다면\n 아무리 힘센 자라 할지라도 10-101\n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 마음으로 진실을 깨닫고 맑아져서\n 무슨 일이든 어버이에게 의탁한다 10-102\n",
                            japanese = "",
                            english = "",
                            commentary = "어버이신이 자유자재한 섭리를 나타낸다면, 아무리 완강히 반대하는 사람이라도 진심으로 어버이신의 진실한 뜻을 이해하고 마음을 맑혀서 무슨 일이든 모두 어버이신에게 의탁하게 된다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 앞으로는 온 세상 사람들 누구나\n 모두 즐거움이 넘치도록 할 테다 10-103\n",
                            japanese = "",
                            english = "",
                            commentary = "앞으로는 온 세상 사람들 누구나 모두 즐거움이 넘치는 생활을 누리도록 할 것이다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 차츰차츰 이 길의 양상을\n 모두들은 자신의 일로 생각하라 10-104\n",
                            japanese = "",
                            english = "",
                            commentary = "이 길을 펴 나감에 따라 여러가지 양상이 벌어지겠지만, 모두들은 이를 자신의 일로 여기고 더욱 힘쓰도록 하라."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 가슴이 몹시 아픈 것은\n 月日의 서두름이야 11-1\n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 앞으로는 모든 사람들에게 차츰차츰\n 몸의 장애가 나타나리라 11-2\n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 어떤 장애가 나타나더라도 염려 말라\n 月日의 크나큰 의도 11-3\n",
                            japanese = "",
                            english = "",
                            commentary = "앞으로는 모든 사람들에게 차차로 몸의 장애가 나타날 것이다. 그러나 어떤 장애가 나타나더라도 걱정할 것 없다. 그것은 모두 어버이신의 깊은 의도가 있기 때문이다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 몸에 장애가 나타나더라도 각자\n 마음에 따라 모두 분간할 테다 11-4\n",
                            japanese = "",
                            english = "",
                            commentary = "몸의 장애가 나타나더라도 그것은 모두 각자의 마음에 따라 나타나는 것이므로 어버이신은 그것을 통해 각자의 마음을 하나 하나 모두 분간할 것이다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 진실로 생각하는 마음과 각자가\n 제 몸만을 생각하는 마음을 11-5\n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 月日은 어떤 마음을 가진 자라도\n 이번에는 명확히 구분해 보일 테다 11-6\n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 어떤 마음도 명확히 보고 있으므로\n 月日 이번에는 모두 구분할 테다 11-7\n",
                            japanese = "",
                            english = "",
                            commentary = "진실로 이 길을 생각하고 세상을 생각하는 마음과 제 몸만을 생각하는 마음 등, 어떤 마음도 명확히 보고 있기 때문에, 이번에는 어버이신이 각자가 써 온 마음을 각각 신상으로 나타내어 분명히 구분해 보일 테다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 입으로만 아첨하는 것 쓸데없어\n 마음의 진실을 月日이 보고 있다 11-8\n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이제까지도 여러가지 이야기를 했지만\n 본진실이 나타나지 않으므로 11-9\n",
                            japanese = "",
                            english = "",
                            commentary = "이제까지 사람들의 마음을 바꾸기 위해 여러 가지로 이야기를 일러 왔지만, 아직 어버이신의 본진실이 나타나지 않았기 때문에 모두들은 잘 납득하지 못했을 것이다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 오늘은 무슨 이야기를 할지라도\n 틀린 말은 하지 않을 것이니 11-10\n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 月日이 한번 일러둔 것은\n 언제까지나 틀림이 없다 11-11\n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 그것을 모르고서 곁의 사람들의 마음은 누구도\n 세상 보통 일로 생각하여 11-12\n",
                            japanese = "",
                            english = "",
                            commentary = "어버이신이 한번 일러둔 것은 언젠가는 반드시 그대로 나타난다. 그런데도 그걸 모르고서 결의 사람들은 깊은 의도가 있어 일러주는 어버이신의 이야기를 세상 보통 일로 여기고 무심코 듣고 있다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이번에 앓고 있는 것으로써 납득하라\n 모두들의 마음도 본인의 마음도 11-13\n",
                            japanese = "",
                            english = "",
                            commentary = "현재 앓고 있는 신삼을 통해 곁의 사람들이나 앓고 있는 본인이나 앞서부터 일러준 어버어신의 이야기가 틀림없음을 납득하라.\n 이 노래는 고깡님의 신상을 두고 하신 말씀이다.(제9호 36~39수의 주석 및 제11호 25~40의 총주 참조)"
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이것 자유자재임에는 틀림없으나\n 모두들의 마음에 깨달음이 없으면 11-14\n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 모든 사람들이 깨닫게만 되면\n 月日 맡아서 확실히 구제할 테다 11-15\n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이 구제 어떤 것이라 생각하는가\n 사흘째는 밖에 나다니도록 11-16\n",
                            japanese = "",
                            english = "",
                            commentary = "이번 신상도 어버이신이 자유자재로 수호할 수 있음에는 틀림없으나, 모두들이 어버이신의 뜻을 깨닫지 못하기 때문에 어쩔 수 없다. 그러나 만일 모두들이 어버이신의 뜻을 깨닫고 마음을 반성해서 그대로 따른다면, 신상의 괴로움은 어버이신이 확실히 맡아서 사흘째는 밖에 나다닐 수 있도록 틀림없이 구제할 것이다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이제까지도 月日의 말이라 하여 차츰차츰\n 이야기를 해 왔지만 11-17\n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 아직 핵심은 전연 모르겠지\n 이번에는 어떤 것도 나타내리라 11-18\n",
                            japanese = "",
                            english = "",
                            commentary = "이제까지 교조가 하는 말은 모두 어버이신이 하는 말이라고 일러 왔으나, 아직 그 핵심은 잘 모를 것이다. 그래서 이번에는 무엇이든 자유자재한 수호를 나타내어 모두들이 참으로 납닥할 수 있도록 하겠다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이야기를 해도 같은 장소에서 하면\n 무엇인가 인간마음으로 하는 것처럼 11-19\n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 모두들 생각하는 마음은 안타까운 일\n 이번에는 장소를 바꿔서 이야기를 11-20\n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이것을 듣고 어떤 자도 납득하라\n 月日의 자유자재 모두 이와 같으리라 11-21\n",
                            japanese = "",
                            english = "",
                            commentary = "아무리 진실한 어버이신의 이야기라도 시종 같은 곳에서 하면, 어딘가 인간 마음으로 하는 것같이 생각되어 곧이 듣지 않을 것이다. 그러나 이래서는 참으로 안타까운 일이므로, 이번에는 장소를 바꿔서 이야기를 하겠는데, 아무리 장소가 바뀌더라도 어버이신의 이야기에는 추호도 틀림이 없다. 그러므로 모두들은 어버이신의 이야기를 믿고 그 뜻을 잘 깨달아 주기 바란다. 무릇 어버이신이 나타내는 자유자재한 수호는 모두 이와 같음을 알아야 한다.\n 같은 장소란 어버이신님이 시종 교조님의 입만 빌려 말씀하신다는 뜻이다. 장소를 바꾸어서 이야기를 이란 고깡님에게 부채를 쥐게 하여 어버이신님의 뜻을 전하게한 사실을 말한다. 그러자 교조님의 말씀과 고깡님의 말이 조금도 틀리지 않았는데, 이로써 교조님이 시종 하시는 말씀은 인간마음으로 하는 것이 아니라, 어버이신님의 뜻을 전하고 계신다는 것을 사람들에게 입증해 보이셨던 것이다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 몸 안에 月日이 들어가서 자유자재로\n 말하고 있지만 깨닫지 못하겠지 11-22\n",
                            japanese = "",
                            english = "",
                            commentary = "어버이신이 교조의 몸 안에 들어가서 자유자재로 말하고 있지만 모두들은 그것을 전혀 깨닫지 못할 것이다.\n 이 노래는 어버이신님이 교조님의 입을 통해서 여러가지로 말씀하고 계시건만, 그것을 어버이신님의 말씀이라 생각지 않고, 세상 보통 사람의 말로 여기고 있던 당시 사람들을 경계하신 말씀이다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 앞으로는 철저히 차츰차츰\n 미리 알린 다음에 시작하는 거야 11-23\n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 月日이 나타낸다고 할지라도\n 차츰차츰 무엇이든 미리 알린 다음에 11-24\n",
                            japanese = "",
                            english = "",
                            commentary = "앞으로는 무슨 일이든 철저히 미리 알린 다음에 나타내기 시작한다. 즉, 어버이신이 사람들이 잘못 쓰는 마음을 신상과 사정으로 나타내 깨우칠 경우에도, 미리 무엇이든 충분히 주의를 준 다음에 하는 일이므로 결코 갑작스런 일은 아니다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이번에 앓고 있는 것을 질병이라고\n 생각하고 있는 것은 틀린 거야 11-25\n",
                            japanese = "",
                            english = "",
                            commentary = "1872년 음력 6월 17일에 이찌노모또 마을에 사는 가지모또가로 출가한 오하루님(교조님의 3녀)이 출직했다. 당시 가지모또가에는 위로 15세부터 아래로 그해 태어난 갓난아기까지 자녀가 다섯이나 되어, 가사와 자녀양육문제 등으로 매우 어려운 처지에 놓여 있었다. 그래서 가지모또가에서는 교조님께 오하루님의 여동생인 고깡님을 후처로 주십사고 간청했으나, 교조님은 어버이신님의 뜻에 따라 이를 승낙하지 않으셨다. 왜냐하면, 고깡님은 터전에 인연이 있는 사람이므로 언제까지나 터전에 두고 어버이신님의 구제한줄기의 길에 쓰려고 생각하셨기 때문이다.(제11호 28~32수 및 69~72수의 주석 참조)\n 그러나 주위의 권유도 있고 더욱이 곤경에 처해 있던 가지모또가의 사정이 하도 딱한지라 당시 호주였던 슈지 선생이나 본인인 고깡님도 그만 인정에 끌려, 마침내 고깡님은 가지모또가로 가서 살게 되었다. 그러나 어버이신님의 원대한 뜻을 인간이 헤아리기는 어려운 일이다. 그 후 1875년에 결국 고깡님은 어버이신님의 가르침을 받아 점점 용태가 악화되자, 비로소 신의의 준엄함을 깨닫고 마침내 결단을 내려 병든 몸을 이끌고 교조님께로 돌아왔다. 그러나 그때 마침 공교롭게도 교조님은 집터의 문간채 신축 공사 사건으로 그해 음력 8월 26일부터 3일간 나라 감옥에 옥고를 겪고 계셨는데, 고깡님은 애석하게도 교조님이 부재중인 음력 8월 28일에 39세를 일기로 그만 출직하고 말았다. 이날 교조님은 감옥에서 돌아와 이를 보시고는 잠시 슬픈 표정을 지으시더니,\n“너는 아무데도 안가는 거야. 매미가 허물을 벗는 것과 같은 거야, 네 영혼은 이 집터에 머물고 있다가 다시 여기에 태어나는 거야.”\n 하고 마치 산 사람에게 이야기하듯미소를 지으며 말씀하셨다.(제9호 36~39수의 주석 참조)"
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이것만은 질병이라고 생각 말라\n 月日의 자유자재 알리고 싶어서 11-26\n",
                            japanese = "",
                            english = "",
                            commentary = "이번에 앓고 있는 것을 세상에 흔히 있는 질병이라고 생각해서는 안된다. 이것만은 절대로 질병이라고 생각지 마라. 이것은 전지전능한 어버이신이 자요자재한 섭리를 알리고 싶어서 나타낸 것이다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 무엇이든 어떤 일이든 알리는 것은\n 앞일에 대한 의도가 있기 때문에 11-27\n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이 이야기 어떤 것인가 하면\n 앞으로 만가지는 月日이 밑는다 11-28\n",
                            japanese = "",
                            english = "",
                            commentary = "어버이신이 하는 이 이야기는 무슨 뜻인가 하면, 앞으로 만가지 일을 어버이신이 모두 맡는다는 것이다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 月日이 맡는다고 하는 것도\n 으뜸인 인연이 있기 때문에 11-29\n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 인연도 어떤 인연인가 하면\n 인간 창조의 으뜸인 도구야 11-30\n",
                            japanese = "",
                            english = "",
                            commentary = "으뜸인 인연이란 고깡님의 인연을 말한다.(제11호 69~72수의 주석 참조)"
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이 자에게 月日 만가지를 가르친다\n 그리하여 진기한 구제를 할 테다 11-31\n",
                            japanese = "",
                            english = "",
                            commentary = "이 자란 고깡님을 가리킨다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이 일을 예삿일로 생각 말라\n 이것은 터전의 고오끼인 거야 11-32\n",
                            japanese = "",
                            english = "",
                            commentary = "이 일을 예삿일로 생각해서는 안된다. 이것은 터전에서 만들어진 세계 인류 구제의 고오끼이다.\n고오끼 제5호 31수의 주석 및 제11호 67,68 수 참조"
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 그 사람 돌아오면 무엇이나 아주 깨끗하게\n 도움 받을 것을 일찍이 알았더라면 11-33\n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 그것을 모르고서 끝내 보내지 않고 거기서\n 요양시키려고 생각했던 거야 11-34\n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이런 일을 일찍이 알았더라면\n 괴로움도 없고 걱정도 없었을 텐데 11-35\n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 인간은 어리석기 때문에\n 月日이 이르는 말 배반했다 11-36\n",
                            japanese = "",
                            english = "",
                            commentary = "고깡님이 가지모또가로 들어간 것은 어버이신님의 뜻을 배반한 처사로서 애당초 여느 사람들과 같은 인정에 끌리지 말았어야 옳았지만, 그렇더라도 어버이신님의 말씀에 좇아 일찍 집터로 돌아왔더라면 깨끗하게 도움받아 아무 괴로움도 걱정도 없었을 텐데, 가지모또가에서는 그걸 모르고 자기 집에서 요양시킬 생각으로 돌려보내지 않고 하루하루 날을 늦추었기 때문에, 결코 어버이신님의 자유자재한 수호를 받을 수 없었던 것이다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이제부터는 무슨 일이든 月日에게\n 의탁하지 않으면 안되는 거야 11-37\n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 무슨 일이든 月日에게\n 의탁하고 있으면 두려울 것이 없다 11-38\n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이처럼 훌륭한 길을\n 모르고 있었음이 뒷날의 후회 11-39\n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 앞으로는 무슨 말을 듣더라도\n 月日이 이르는 말 배반하지 않도록 11-40\n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 月日이 몸 속에 들기도 한 자의 가슴이\n 아픈 것을 어떻게 생각하는가 11-41\n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이 아픈 것을 예삿일로 생각 말라\n 月日의 마음은 참으로 걱정이다 11-42\n",
                            japanese = "",
                            english = "",
                            commentary = "어버이신이 몸 속에 들기도 한 사람이 가슴을 앓고 있는 것을 모두들은 어떻게 생각하고 있는가. 이 가슴앓이를 예삿일로 여겨서는 안된다. 어버이신은 이것을 매우 걱정하고 있다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " ㅡ것을 모르고서 모둔 사람들은 각자\n 제 몸만 생각하며 일할 뿐이다 11-43\n",
                            japanese = "",
                            english = "",
                            commentary = "어버이신이 이토록 걱정하는 것도 모르고 모든 사람들은 각자 제 몸 생각만을 일삼고 있다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 月日에게는 어떤 길도 보인다\n 세상 사람들은 그것을 알지도 못하 11-44\n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이 길을 모든 자녀들은 생각하라\n 어떤 길이 있을지 모르는 거야 11-45\n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 무슨 일이든 미리 알려 둔다\n 나중에 후회 없도록 하라 11-46\n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이것은 무엇을 말하느냐고 모두들\n 생각하겠지만 자녀가 귀여워서 11-47\n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 온 세상 많은 자녀들의 가슴속에\n 이것을 깨닫게 할 방법은 없을까 11-48\n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이런 것을 자꾸 되풀이 말하는 것도\n 앞날을 염려하고 있기 때문에 11-49\n",
                            japanese = "",
                            english = "",
                            commentary = "이런 말을 자꾸 되풀이하는 것도 자녀들의 앞날을 염려해서 미리 주의를 환기시키기 위함이다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이제부터는 무슨 일이든 일러준다\n 이것을 결코 거짓이라 생각 말라 11-50\n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이번 月日의 일에 대해 단단히 들어라\n 그릇된 일은 하지 않을 테니 11-51\n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 어떻든 진기한 구제를 가르치고 싶어\n 그래서 시작한 일인 거야 11-52\n",
                            japanese = "",
                            english = "",
                            commentary = "이번에 어버이신이 하는 일에 대해 일러주는 말을 단단히 들어주기 바란다. 결코 모두들에게 좋지 않은 일은 하지 않는다. 어떻든 어버이신은 사람들에게 진기한 구제를 가르치고 싶어서 시작하는 일이다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 지금까지와 달리 마음을 단단히 바꾸어서\n 즐거움이 넘치는 마음이 되도록 11-53\n",
                            japanese = "",
                            english = "",
                            commentary = "지금까지와는 달리 마음을 단단히 바꾸어서 앞으로는 즐거움이 넘치는 마음이 되도록 하라."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이 마음 어떻게 해서 된다고 생각하는가\n 月日이 몸 안에 들어가게 되면 11-54\n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 나날이 저절로 마음 용솟음친다\n 즐거움이 넘치는 마음이 되도록 11-55\n",
                            japanese = "",
                            english = "",
                            commentary = "즐거움이 넘치는 마음이 어더ㄷ게 해서 되는가고 생각하겠지만, 어버이신이 몸 안에 들어가서 수호하면 차차 마음이 저절로 용솟음쳐서 즐거움이 넘치는 마음이 된다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 月日이 나날이 마음 용솟음치게 해서\n 즐거움이 넘치도록 만들어 갈 테야 11-56\n",
                            japanese = "",
                            english = "",
                            commentary = "어버이신이 사람들의 마음을 나날이 용솟음치게 해서 이 세상을 차츰 즐거움이 넘치는 세계로 만들어 나갈 것이다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이 이야기 어떻게 생각하고 듣고 있는가\n 구제한줄기의 준비만을 11-57\n",
                            japanese = "",
                            english = "",
                            commentary = "이 이야기를 모두들은 어떻게 생각하고 듣고 있는가. 그것은 어버이신이 오직 구제한줄기의 준비만을 서두르고 있다는 뜻이다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 月日은 무슨 일이든 보고 있으니\n 무슨 말을 해도 모두들 알아차려라 11-58\n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 올해부터 70년은 부부 다 같이\n 앓지 않고 쇠하지 않고 살아가게 되면 11-59\n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 그보다 더한 즐거움은 없으리라\n 이것을 참으로 낙으로 삼아라 11-60\n",
                            japanese = "",
                            english = "",
                            commentary = "올해보터 앞으로 70년을 부부가 다 같이 질병에 걸리지도 않고 노쇠하지도 않고 살아가게 된다면, 그보다 더한 즐거움은 없을 것이다. 그러니 이것을 낙으로 삼도록 하라.\n 부부 다 같이란 슈지 선생과 그 부인 마쓰에님을 가리킨다. 이것을 볼 때 당시 교조님이 이들 두 사람의 마음을 얼마나 격려하시고, 또 얼마나 간절히 마음의 자각을 촉구하셨는지를 엿볼 수 있다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 月日은 지금까지 어디에도 없던\n 일만을 말할 테다 잘 알아차려라 11-61\n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이처럼 없던 일만을 말하지만\n 앞으로 두고 보라 모두 참인 거야 11-62\n",
                            japanese = "",
                            english = "",
                            commentary = "이처럼 어버이신은 지금까지 전혀 닥치지 않은 일만을 말하고 있지만 앞으로 두고 보라. 모두 그대로 나타날 것이다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 어쨌든 진기한 일을 하게 되므로\n 어떤 이야기도 없던 일만을 11-63\n",
                            japanese = "",
                            english = "",
                            commentary = "어쨌든 지금까지 볼 수 없던 진기한 구제를 하기 때문에 자연 어버이신이 하는 이야기도 지금까지 없던 일만을 말하게 되는 것이다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 아무리 없던 일만을 말할지라도\n 앞으로 두고 보라 신기함이 나타나리라 11-64\n",
                            japanese = "",
                            english = "",
                            commentary = "아무리 없던 일만을 말할지라도 앞으로 두고 보라. 어버이신이 하는 말은 모두 그대로 신기하게 나타날 것이다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 지금 앓고 있는 것은 괴롭지만\n 이제부터 앞으로는 마음 즐겁다 11-65\n",
                            japanese = "",
                            english = "",
                            commentary = "지금 앓고 있는 질병은 괴롭겠지만, 이 괴로움으로 말미암아 마음을 바꾸고 어버이신의 말대로 해 나간다면, 앞으로는 마냥 마음이 즐거울 것이다.\n 지금 앓고 있는 것은 당시 슈지 선생의 부부 가운데 누군가가 질병으로 앓고 있었던 것으로 추측된다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이런 이야기 자꾸 되풀이 말하는 것도\n 이것은 영원한 고오끼인 거야 11-66\n",
                            japanese = "",
                            english = "",
                            commentary = "이런 이야기를 자꾸 되풀이해서 일러주는 것도 이것이 장래 영원히 전해져서 구제한줄기의 토대가 될 고오끼 때문이다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 月日이 이번에 여기에 나타나서\n 어떤 이야기도 하는 것은 11-67\n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 무슨 일이든 차츰차츰 알리고 싶어\n 터전의 고오끼 모두 만든다 11-68\n",
                            japanese = "",
                            english = "",
                            commentary = "어버이신이 이번에 으뜸인 터전에 나타나서 여러 가지 이야기를 하는 것은 온세상 사람들에게 모든 진실을 차차로 알리고 싶기 때문인데, 그러기 위해서 어버이신은 구제한줄기의 길을 모두 가르쳐 터전에서 인류구제의 본보기가 될 영원한 고오끼를 만들어 나갈 것이다.(제5호 31수의 주석 참조)"
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이 세상을 창조한 곳은 야마또로서\n 야마베군 쇼야시끼니라 11-69\n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 그 안에 나까야마씨의 집터\n 인간 창조의 도구가 보이는 거야 11-70\n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이 도구들은 이자나기(남자추형종자)와 이자나미(여자추형묘상)과\n 구니사즈찌(결합수호)와 쯔끼요미(지탱수호)야 11-71\n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 月日이 그것을 살펴보고 하강했다\n 모엇이든 만가지 가르칠 준비를 11-72\n",
                            japanese = "",
                            english = "",
                            commentary = "태초에 이 세상 인간을 창조한 곳은 야마또 지방 야마베(山邊)군 쇼야시끼(庄屋敷)마을인데, 그 마을에 있는 나까야마씨라는 집터에 인간창조시 도구의 리를 지닌 사람들이 보인다. 이 도구들을 이자나기노미꼬또(남자추형종자의리)와 이자나미노미꼬토(여자추형묘상의리), 그리고 구니사즈찌노미꼬또(지탱수호의리) 등 이다. 어버이신은 이들을 살펴보고 순각한의 도래와 더불어 으뜸인 터전에 하강하여 교조를 현신으로 삼아 구제한줄기의 길을 가르칠 준비를 해 왔다.\n 교조님은 이 길을 창시하심에 있어 말로 다할 수 없는 고난을 겪으셨고, 남편되는 젬베에님도 가산 일체를 바쳐 교조님이 구제한줄기의 가르침을 펴는 데 협력하느라 여간아닌 고초를 겪으셨다. 또, 슈지 선생는 가난한 가운데 가계를 꾸려가며, 한편으로는 세상의 박해와 조소에 대해 백방으로 이 길을 지키려고 노력했으며, 고깡님도 어머님을 위로하고 오빠를 격려하면서 한 집안의 화목과 이 길의 발전을 위해 말할 수 없는 고생을 겪었던 것이다. 이런 점으로 미루어 볼 떼, 교조님 일가의 고난은 실로 각자가 모두 인간창조의 도구의 리를 몸에 지니고 계셨기 때문이라 생각되며, 바로 여기에 어버이신님의 깊은 의도가 있음을 엿볼 수 있다.(제6호 29~51수의 주석 참조)"
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이곳에서 무엇을 하는 것도 어떤\n 일을 하는 것도 모두 月日이니라 11-73\n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 무슨 말을 하는 것도 모두 月日\n 곁의 사람들은 흉내를 내 보라 11-74\n",
                            japanese = "",
                            english = "",
                            commentary = "이 터전에서 어떤 일을 하는 것도 사람이 아니다. 모두 어버이신이 하는 일이다. 또, 무슨 말을 하는 것도 사람이 아니고 어버이신이다. 사람이 멋대로 하는 일이거나 말이라고 생각한다면 곁의 사람들은 한번 흉내를 내 보라. 결코 할 수 없을 것이다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이 세상 창조 이래 오늘까지는\n 본진실을 말한 바 없다 11-75\n",
                            japanese = "",
                            english = "",
                            commentary = "어버이신이 이 세상 인간을 창조한 이래 지금까지 어버이신의 본진실을 말한 바가 없다.(제11호 69~72수 참조)"
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 오늘은 무슨 일이든 진실을\n 말하지 않으면 안될 것이기에 11-76\n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 각자는 인간마음으로 말한다고는 생각 말라\n 月日의 생각대로 말하는 거야 11-77\n",
                            japanese = "",
                            english = "",
                            commentary = "각자는 인간마음으로 말하고 있다고 생각해서는 안된다. 모두 어버이신의 생각대로 말하고 있는 것이다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 언제 돌아오더라도 각자의\n 마음에서라고는 전혀 생각 말라 11-78\n",
                            japanese = "",
                            english = "",
                            commentary = "언제 터전에 돌아오더라도 각자 인간 마음에서 돌아온다고는 결코 생각해서는 안 된다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 어떤 자도 진심으로 납득을\n 시켜서 돌아가게 하는 거야 이것을 두고 보라 11-79\n",
                            japanese = "",
                            english = "",
                            commentary = "어떤 자도 어버이신이 진심으로 납득을 시켜서 돌아가게 할 것이다. 이것을 단단히 두고 보라."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 아무리 힘센 자도 영리한 자도\n 月日의 마음에는 당할 수 없다 11-80\n",
                            japanese = "",
                            english = "",
                            commentary = "아무리 힘센 자도 영리한 자도 어버이신의 능력에는 당하지 못하고, 그저 그리워하며 흠모할 뿐이다.\n 이 노래는 아무리 완력이 세고 지혜가 뛰어나다고 뽐내는 자라도, 어버이신님의 자유자재한 섭리와 심원한 의도에는 도저히 당할 수 없다는 것을 말씀하고 계신다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " \n12월 27일 부터",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 오늘부터는 月日月이 세상을 살펴보고\n 가슴속의 청소를 시작하는 거야 12-1\n",
                            japanese = "",
                            english = "",
                            commentary = "오늘부터는 어버이신이 온 세상 사람들을 살펴보고 그 마음의 티끌을 털어 내기 시작한다. "
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " 이 청소는 안이나 세상이나 차별 없다\n 각자의 마음을 모두 나타낼 테야 12-2\n",
                            japanese = "",
                            english = "",
                            commentary = "이 마음의 청소는 내부 사람이나 세상 사람이나 조금도 차별이 없다. 각자의 마음을 모두 그대로 나타낼 것이다."
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " \n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " \n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " \n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " \n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " \n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " \n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " \n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " \n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " \n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " \n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " \n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " \n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " \n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " \n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " \n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " \n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " \n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " \n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " \n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " \n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " \n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " \n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " \n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " \n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " \n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " \n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " \n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " \n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " \n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " \n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " \n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " \n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " \n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " \n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " \n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " \n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " \n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " \n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " \n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " \n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " \n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " \n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " \n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " \n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " \n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " \n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " \n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " \n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " \n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " \n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " \n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " \n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " \n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " \n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " \n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " \n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " \n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " \n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " \n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " \n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " \n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " \n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " \n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " \n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " \n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " \n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " \n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " \n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " \n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " \n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " \n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " \n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " \n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " \n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " \n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " \n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " \n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " \n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " \n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " \n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " \n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " \n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " \n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " \n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " \n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " \n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " \n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " \n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " \n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " \n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " \n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " \n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " \n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " \n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " \n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " \n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " \n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " \n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " \n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " \n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " \n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " \n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " \n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " \n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " \n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " \n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " \n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " \n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " \n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " \n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " \n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " \n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " \n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " \n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " \n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " \n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " \n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " \n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " \n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " \n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " \n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " \n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " \n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " \n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " \n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " \n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " \n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " \n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " \n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " \n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " \n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " \n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )
                    allContent.add(
                        ContentItem(
                            korean = " \n",
                            japanese = "",
                            english = "",
                            commentary = ""
                        )
                    )

                }
            }
        }
    }
}
