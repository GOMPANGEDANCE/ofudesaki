package tenrikyo.ofudesaki.kr

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class QuizActivity : AppCompatActivity() {

    private val allContent = mutableListOf<ContentItem>()
    private lateinit var adapter: ArrayAdapter<ContentItem>
    private lateinit var filterStatusLayout: LinearLayout
    private lateinit var filterStatusText: TextView
    private lateinit var returnButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_quiz)

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            window.insetsController?.hide(android.view.WindowInsets.Type.statusBars())
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (android.view.View.SYSTEM_UI_FLAG_FULLSCREEN)
        }

        loadTemporaryData()

        val chaptersLayout: LinearLayout = findViewById(R.id.chaptersLayout)
        val searchEditText: EditText = findViewById(R.id.searchEditText)
        val contentListView: ListView = findViewById(R.id.contentListView)
        filterStatusLayout = findViewById(R.id.filterStatusLayout)
        filterStatusText = findViewById(R.id.filterStatusText)
        returnButton = findViewById(R.id.returnButton)

        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, allContent)
        contentListView.adapter = adapter

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

        returnButton.setOnClickListener {
            adapter.filter.filter("")
            searchEditText.text.clear()
            filterStatusLayout.visibility = View.GONE
        }

        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                adapter.filter.filter(s)
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        contentListView.setOnItemClickListener { parent, view, position, id ->
            // 1. 사용자가 클릭한 아이템을 가져옵니다. (예: "2-1" ContentItem 객체)
            val selectedItem = adapter.getItem(position)

            if (selectedItem != null) {
                // 2. 원본 'allContent' 리스트에서 이 객체가
                //    실제로 몇 번째 인덱스인지 찾아냅니다. (이것이 핵심!)
                val actualPosition = allContent.indexOf(selectedItem)

                val intent = Intent(this, ContentDetailActivity::class.java).apply {
                    // 3. 필터링된 position(0) 대신,
                    //    원본 리스트의 실제 위치(actualPosition)를 전달합니다.
                    putExtra("EXTRA_POSITION", actualPosition)
                    putParcelableArrayListExtra("EXTRA_CONTENT_LIST", ArrayList(allContent))

                    // --- 리팩토링 제안 ---
                    // ContentDetailActivity는 어차피 List와 actualPosition을 받아서
                    // item을 다시 찾으므로, 아래 4줄은 사실 없어도 됩니다.
                    // (지금은 그냥 두셔도 문제는 없습니다.)
                    putExtra("KOREAN_TEXT", selectedItem.korean)
                    putExtra("JAPANESE_TEXT", selectedItem.japanese)
                    putExtra("ENGLISH_TEXT", selectedItem.english)
                    putExtra("COMMENTARY_TEXT", selectedItem.commentary)
                }

                startActivity(intent)
            }
        }
    } // override fun onCreate가 여기서 끝납니다.

    private fun loadTemporaryData() {
        allContent.add(
            ContentItem(
                korean = " 모든 시대 온 세상 사람들을 살펴보아도\n 신의 뜻 아는 자는 전혀 없으므로 1-1",
                japanese = "よろつよのせかい一れつみはらせど\nむねのハかりたものハないから",
                english = "yorozuyo no sekai ichiretsu miharasedo\nmune no wakarita mono wa nai kara",
                commentary = " 一. 어버이신이 이 세상을 창조한 이래\n장구한 세월이 흐르는 동안\n수없이 많은 사람들이 살아왔으나,\n어느 시대를 보아도 넓은 세상 가운데\n누구 하나 신의 뜻을 아는 사람이 없었다."
            )
        )
        allContent.add(
            ContentItem(
                korean = " 그러리라 일러준 일이 없으니\n 아무 것도 모르는 것이 무리는 아니야 1-2",
                japanese = "そのはづやといてきかした事ハない\nなにもしらんがむりでないそや",
                english = "sono hazu ya toite kisashita koto wa nai\nnanimo shiran ga muri de nai zo ya",
                commentary = "二. 그도 그럴것이 지금까지 이 진실한 가르침을\n일러준 일이 없었으니 무리가 아니다.\n가끔 그 시대의 성현을 통해서 일러주긴 했으나.\n그것은 모두가 시의에 적합하신 신의(神意)의\n표현일 뿐 최후의 가르침은 아니다.\n그것은 아직 시순이 오지 않았기 때문에\n부득이한 것이었다. "
            )
        )
        allContent.add(
            ContentItem(
                korean = " 이번에는 신이 이 세상에 나타나서\n 무엇이든 자세한 뜻을 일러준다 1-3",
                japanese = "このたびハ神がをもていあらハれて\nなにかいさいをといてきかする",
                english = "konotabi wa Kami ga omote i arawarete\nnanika isai o toite kikasuru",
                commentary = "三. 그러나 드디어 이번에야말로 순각한이 도래\n했으므로 어버이신 천리왕님이 이 세상에 나타나서\n자신의 뜻을 소상히 일러줄테다.\n이번이란 1838년 10월 26일,\n순각한의 도래로 어버이신님이 교조님을 현신으로\n삼아 이 가르침을 시작하신 때를 말한다.\n신이 이 세상에 나타나서란 어버이신님이 교조님을 현신으로 삼아,\n즉 교조님의 입을 통해서 당신의 뜻을 세상 사람들에게 알리시는 것을 말한다."
            )
        )
        allContent.add(
            ContentItem(
                korean = " 신이 진좌하는 터전이라고\n 말하고 있지만 근본은 모르겠지 1-4",
                japanese = "このところやまとのしバのかみがたと\nゆうていれども元ハしろまい",
                english = "kono tokoro Yamato no Jiba no kamigata to\nyute iredomo moto wa shiromai",
                commentary = "四. 이곳은 신이 진좌하는 터전이라고 말하고 있으나 그 근본을 모를 것이다. \n터전은 어버이신님이 태초에 인간을 잉태하신 곳. 즉 우리들 인간의 근본되는 본고장을 가리킨다."
            )
        )
        allContent.add(
            ContentItem(
                korean = " 이 근본을 자세히 듣게 되면\n 어떤 자도 모두 그리워진다 1-5",
                japanese = "このもとをくハしくきいた事ならバ\nいかなものでもみなこいしなる",
                english = "kono moto o kuwashiku kiita koto naraba\nikana mono demo mina koishi naru",
                commentary = "五. 으뜸인 터전에 신이 진좌하고 있는 그 근본을 자세히 들어서 알게 된다면 누구든 자신의 근본 고향인 터전을 그리워하게 될 것이다."
            )
        )
        allContent.add(
            ContentItem(
                korean = " 듣고 싶거든 찾아오면 일러줄 테니\n 만가지 자세한 뜻 으뜸인 인연 1-6",
                japanese = "きゝたくバたつねくるならゆてきかそ\nよろづいさいのもとのいんねん",
                english = "kiki takuba tazune kuru nara yute kikaso\nyorozu isai no moto no innen",
                commentary = "六. 이 근본을 듣고 싶은 사람은 찾아오라. 이 세상 태초를 비롯해서 모든 리를 소상히 가르칠 테니."
            )
        )
        allContent.add(
            ContentItem(
                korean = " 신이 나타나 무엇이든 자세한 뜻을 일러주면\n 온 세상 사람들의 마음 용솟음친다 1-7",
                japanese = "かみがでてなにかいさいをとくならバ\nせかい一れつ心いさむる",
                english = "Kami ga dete nanika isai o toku naraba\nsekai ichiretsu kokoro isamuru",
                commentary = "七. 으뜸인 어버이신님이 이 세상에 나타나서 없던 인간을 창조한 어버이신의 수호와 구제한줄기의 길에 대해 소상히 일러주면, 세상 사람들의 마음은 이 진실한 가르침에 의해 모두 용솟음치게 될 것이다."
            )
        )
        allContent.add(
            ContentItem(
                korean = " 모든 사람들을 속히 도우려고 서두르니\n 온 세상 사람들의 마음도 용솟음쳐라 1-8",
                japanese = "いちれつにはやくたすけをいそぐから\nせかいの心いさめかゝりて",
                english = "ichiretsu ni hayaku tasuke o isogu kara\nsekai no kokoro isame kakarite",
                commentary = "八. 누구든 차별없이 모든 사람들을 하루빨리 구제하고 싶으니 이러한 어버이신의 뜻을 잘 깨달아 세상 사람들의 마음도 용솟음치도록 하라. \n 이상의 노래는 신악가와 「팔수」와 거의 같다."
            )
        )
        allContent.add(
            ContentItem(
                korean = " 차츰차츰 마음이 용솟음치게 되면\n 온 세상 풍년 들고 곳곳마다 번창하리라 1-9",
                japanese = "だん／＼と心いさんてくるならバ\nせかいよのなかところはんじよ",
                english = "dandan to kokoro isande kuru naraba\nsekai yononaka tokoro hanjo",
                commentary = "九. 세상 사람들의 마음이 차츰 용솟음치게 되면 번민도 고통도 없어지고 모두 서로 도와 가며 각자의 일에 힘쓰게 된다. 따라서 어버어신도 그 마음에 따라 세상 만물이 풍성하고 가업(家業)도 번영하여, 어디에 가더라도 싸움이나 시비가 없이 인류는 평화롭고 행복하게 살게 된다."
            )
        )
        allContent.add(
            ContentItem(
                korean = " 앞으로는 신악근행의 손짓을 가르쳐서\n 모두 갖추어서 근행하기를 고대한다 1-10",
                japanese = "このさきハかくらづとめのてをつけて\nみんなそろふてつとめまつなり",
                english = "konosaki wa Kagura-zutome no te o tsukete\nminna sorote Tsutome matsu nari",
                commentary = "十, 이제부터 어버이신은 사람들에게 신악근행의 손짓을 가르칠 테니, 인원을 갖추어 근행하길 간절히 바란다.\n신악근행이란 감로대를 중심으로 태초의 십주(十住) 신님의 리를 찬양하며 열 사람이 올리는 근행으로서, 이 근행에 의해 어버이신님을 용솟음치게 하고, 제세구인(済世救人)의 수호를 기원하는 것이다.\n" +
                        "이 근행은 또 장소의 뜻에서 감로대근행이라고 하고, 신과 인간이 함께 용솟음치므로 즐거운근행이라고도 하며, 또 구제한줄기의 근행이므로 구제근행이라고도 한다.(제六호 三十의 주석, 제十호 二十五～二十七의 주석 및 제 十五호 五十二의 주석 참조) 이 근행은 터전 이외에서는 허용되지 않는다.(지도말씀 一八八九, 三, 三一 참조)"
            )
        )
        allContent.add(
            ContentItem(
                korean = " 모두 갖추어서 서둘러 근행을 하면\n 곁의 사람이 용솟음치면 신도 용솟음친다 1-11",
                japanese = "みなそろてはやくつとめをするならバ\nそばがいさめバ神もいさむる",
                english = "mina sorote hayaku Tsutome o suru naraba\nsoba ga isameba Kami mo isamuru",
                commentary = "十一, 인원을 갖추어 하루라도 빨리 신악근행을 하게 되면 어버이신은 진실한 어버이이기 때문에, 자녀들은 인간이 기뻐 용솟음치는 모습을 보고 어버이신도 함께 용솟음치게 된다."
            )
        )
        allContent.add(
            ContentItem(
                korean = " 무엇이건 신의 마음 침울해지면\n  농작물도 모두 침울해진다 1-12",
                japanese = "いちれつに神の心がいづむなら\nものゝりうけかみないつむなり",
                english = "ichiretsu ni Kami no kokoro ga izumu nara\nmono no ryuke ga mina izumu nari",
                commentary = "十二. 무릇 어버이신의 마음이 침울해지면 농작물도 저절로 생기를 잃어 충분한 수확을 하지 못하게 된다."
            )
        )
        allContent.add(
            ContentItem(
                korean = " 농작물을 침울하게 하는 마음은 안타까운 일\n 침울하지 않도록 어서 용솟음쳐라 1-13",
                japanese = "りうけいのいつむ心ハきのとくや\nいづまんよふとはやくいさめよ",
                english = "ryukei no izumu kokoro wa kinodoku ya\nizuman yo to hayaku isame yo",
                commentary = "十三. 농작물을 충분히 여물지 않게 하는 사람들의 침울한 마음은 어버이신이 볼 때 가엾은 일이므로, 오곡이 풍성하도록 어서 어버이신의 마음을 용솟음치게 하라."
            )
        )
        allContent.add(
            ContentItem(
                korean = " 농작물이 용솟음치도록 하려거든\n 신악근행과 손춤을 행하라 1-14",
                japanese = "りうけいがいさみでるよとをもうなら\nかぐらつとめやてをとりをせよ",
                english = "ryukei ga isami deru yo to omou nara\nKagura-zutome ya Teodori o seyo",
                commentary = "十四, 손춤은 신악가 팔수부터 十二장까지를 말한다."
            )
        )
        allContent.add(
            ContentItem(
                korean = " 이번에는 서둘러 손춤을 시작하라\n 이것을 계기로 신기함이 있으리 1-15",
                japanese = "このたびハはやくてをどりはじめかけ\nこれがあいずのふしきなるそや",
                english = "konotabi wa hayaku Teodori hajime kake\nkore ga aizu no fushigi naru zo ya",
                commentary = "十五, 신악근행과 손춤을 행하면 사람들의 마음이 즐거워지고 마음의 티끌도 깨끗이 털려 맑아지므로 어버이신도 그러한 마음을 보고 섭리하게 된다. 그러므로 하루빨리 신악근행과 손춤을 행하라. 그러면 이 근행을 계기로 어버이신의 신기한 섭리가 반드시 나타나게 된다."
            )
        )
        allContent.add(
            ContentItem(
                korean = " 이 같은 신기함은 나타나지 않았으나\n 날이 오면 확실히 알게 된다 1-16",
                japanese = "このあいずふしぎとゆうてみへてない\nそのひきたれバたしかハかるぞ",
                english = "kono aizu fushigi to yute miete nai\nsono hi kitareba tashika wakaru zo",
                commentary = "十六, 이렇게 해서 나타나는 어버이신의 섭리는 영묘(靈妙)한 것이지만 , 아직은 나타나지 않았기 때문에 지금 당장 사람들로서는 알 수가 없다. 그러나 어버이신이 마침내 신기한 섭리를 나타내는 날이 오면 과연 영묘한 것임을 누구나 분명히 알게 될 것이다."
            )
        )
        allContent.add(
            ContentItem(
                korean = " 그날이 와서 무엇인가 알게 되면\n 어떤 자도 모두 감탄하리라 1-17",
                japanese = "そのひきてなにかハかりがついたなら\nいかなものてもみながかんしん",
                english = "sono hi kite nanika wakari ga tsuita nara\nikana mono demo mina ga kanshin",
                commentary = ""
            )
        )
        allContent.add(
            ContentItem(
                korean = " 나타난 다음에 일러주는 것은 세상 보통 일\n 나타나기 전부터 일러두는 거야 1-18",
                japanese = "みへてからといてかゝるハせかいなみ\nみへんさきからといてをくそや",
                english = "miete kara toite kakaru wa sekainami\nmien saki kara toite oku zo ya",
                commentary = "十八, 무슨 일이든 눈앞에 나타난 다음에 일러주는 것은 세상 보통 일이지만,  어버이신은 눈앞의 일뿐만 아니라 장래의 일까지 미리 일러주므로, 어버이신의 말 가운데 인간생각으로 이해 안되는 것이 있다라도 이것을 의심하거나 부정하는 경솔한 짓을 해서는 안된다. 어디까지나 어버이신의 말을 믿고 그것이 실현될 날을 기다려야 한다."
            )
        )
        allContent.add(
            ContentItem(
                korean = " 앞으로는 윗사람들 차츰차츰\n 마음을 맞추어 화목하도록 1-19",
                japanese = "このさきハ上たる心たん／＼と\n心しづめてハぶくなるよふ",
                english = "konosaki wa kami taru kokoro dandan to\nkokoro sizumete wabuku naru yo",
                commentary = "十九. 이제부터 윗사람들은 서로 마음을 맞추어 화목하지 않으면 안된다."
            )
        )
        allContent.add(
            ContentItem(
                korean = " 이 화목 어려운 듯하지만\n 차츰차츰 신이 수호하리라 1-20",
                japanese = "このハほくむつかしよふにあるけれと\nだん／＼神がしゆこするなり",
                english = "kono waboku muzukashi yoni aru keredo\ndandan Kami ga shugo suru nari",
                commentary = "二十, 이 화목은 어려운 듯하나 차차로 어버이신이 수호함에 따라 머지않아 틀림없이 실현될 것이다."
            )
        )
        allContent.add(
            ContentItem(
                korean = " 이 세상은 리로써 되어 있는 세상이다\n 무엇이든 만가지를 노래의 리로써 깨우쳐 1-21",
                japanese = "このよふハりいでせめたるせかいなり\nなにかよろづを歌のりでせめ",
                english = "kono yo wa rii de semetaru sekai nari\nnanika yorozu o uta no ri de seme",
                commentary = "二十一, 이 세상은 어버이신의 의도, 즉 천리에 의해 성립되어 있으므로, 인간의 행위는 물론 그 밖의 모든 리를 노래로써 깨우치겠다."
            )
        )
        allContent.add(
            ContentItem(
                korean = " 깨우친다 하여 손질로써 하는 것이 아니니\n 입으로도 아니고 붓끝으로 깨우쳐 1-22",
                japanese = "せめるとててざしするでハないほどに\nくちでもゆハんふでさきのせめ",
                english = "semeru tote tezashi suru dewa nai hadoni\nkuchi demo yuwan fudesaki no seme",
                commentary = "二十二, 깨우친다 해도 인간들처럼 완력으로 하는 것도 아니요, 말로 꾸짖는 것도 아니다. 다만 붓으로 깨우치는 것이다.\n붓끝이란 친필을 말한다."
            )
        )
        allContent.add(
            ContentItem(
                korean = " 무엇이든 잘한 것은 좋으나\n 잘못함이 있으면 노래로써 알린다 1-23",
                japanese = "なにもかもちがハん事ハよけれども\nちがいあるなら歌でしらする",
                english = "nanimo kamo chigawan koto wa yokeredomo\nchigai aru nara uta de shirasuru",
                commentary = "二十三, 모든 것이 어버이신의 뜻에 맞으면 좋으나, 만약 어버이신의 뜻에 맞지 않는 일이 있으면 노래로써 알릴테니 잘 깨달아 마음이 잘못되지 않도록 해야 한다."
            )
        )
        allContent.add(
            ContentItem(
                korean = " 알려 주면 나타나니 안타까운 일\n 어떤 질병도 마음으로부터 1-24",
                japanese = "しらしたらあらハれでるハきのどくや\nいかなやまいも心からとて",
                english = "shirashitara araware deru wa kinodoku ya\nikana yamai mo kokoro kara tote",
                commentary = "二十四, 잘못된 마음을 노래로써 알린다면 곧 나타나는데, 이는 가엾기는 하지만 어떤 질병도 각자의 마음에서 비롯되는 것인 만큼 어쩔 수 없는 일이다."
            )
        )
        allContent.add(
            ContentItem(
                korean = " 질병이라 해도 세상 보통 것과 다르니\n 신의 노여움 이제야 나타났다 1-25",
                japanese = "やまいとてせかいなみでハないほどに\n神のりいふくいまぞあらハす",
                english = "yamai tote sekainami dewa nai hodoni\nKami no rippuku imazo arawasu",
                commentary = "二十五, 질병이라 해도 세상에 흔이 있는 예사로운 질병이라 생각해서는 안된다. 어버이신의 뜻에 맞지 않기 때문에 지금 노여움을 나타낸 것이다."
            )
        )
        allContent.add(
            ContentItem(
                korean = " 지금까지 신이 하는 말을 듣지 않으니\n 부득이 표면에 나타낸 것이다 1-26",
                japanese = "いまゝでも神のゆう事きかんから\nぜびなくをもてあらハしたなり",
                english = "imamade mo Kami no yu koto kikan kara\nzehi naku omote arawashita nari",
                commentary = "二十六, 지금까지 여러 번 훈계를 했으나 전혀 듣지 않으므로 부득이 사람들의 눈에 뛰게 표면에 나타낸 것이다.\n교조님의 장남 슈우지는 오랫동안 앓고 있던 다리병이 쉽사리 낫지 않을 뿐만아니라 가끔 통증이 심해 괴로워했다.\n" +
                        "교조님은 이를 질병이 아니라 어버이신님의 꾸지람이므로 깊이 참회하여 마음을 고치도록 깨우치시는 동시에 집터의 청소를 서두르셨다. 이하 슈우지에 대한 어버이신님의 엄한 가르침은, 슈우지 개인에 대한 꾸지람이라 생각지 말고 이를 본보기로 모든 사람들을 깨우치신 것이라 해석해야 할 것이다. "
            )
        )
        allContent.add(
            ContentItem(
                korean = " 이토록 신의 섭섭함이 나타났으니\n 의사도 약도 이에는 당할 수 없다 1-27",
                japanese = "いらほどの神のざんねんでてるから\nいしやもくすりもこれハかなハん",
                english = "kora hodono Kami no zannen deteru kara\nisha mo kusuri mo kore wa kanawan",
                commentary = "二十七, 가벼운 질병은 간단히 치료가 되지만 어버이신의 엄한 꾸지람을 받았을 때에는 의사나 약으로도 근본적인 치료가 불가능하다."
            )
        )
        allContent.add(
            ContentItem(
                korean = " 이것만은 예삿일로 생각 말라\n 어떻든 이것은 노래로 깨우친다 1-28",
                japanese = "これハかりひとなみやとハをもうなよ\nなんてもこれハ歌でせめきる",
                english = "kore bakari hitonami ya towa omouna yo\nnandemo kore wa uta de semekiru",
                commentary = "二十八, 이 다리병만은 흔이 있는 예사로운 질병이라 가볍게 여겨서는 안된다.\n" +
                        "그러므로 어디까지나 그 근본을 노래로써 깨우칠 것이다."
            )
        )
        allContent.add(
            ContentItem(
                korean = " 이번에는 집터의 청소를 깨끗하게\n 해 보일 테니 이것을 보아 다 1-29",
                japanese = "このたびハやしきのそふじすきやかに\nいたゝてみせるこれをみてくれ",
                english = "konotabi wa yashiki no soji sukiyaka ni\nshitatete miseru kore o mite kure",
                commentary = "二十九, 이번에는 터전의 리를 밝히기 위해 집터를 깨끗이 청소할 것이니 모두 명심하라.\n집터란 교조님이 사시는 곳으로서  인류의 본고장인 터전이 있는 곳이다"
            )
        )
        allContent.add(
            ContentItem(
                korean = " 청소만 깨끗이 하게 되면\n 신의 뜻을 알게 되어 말하고 말하게 되는 거야 1-30",
                japanese = "そふじさいすきやかしたる事からバ\nしりてはなしはなしするなり",
                english = "soji sai sukiyaka shitaru koto naraba\nshirite hanashite hanashi suru nari",
                commentary = "三十, 이 집터의 청소가 깨끗이 된 다음에는 터전의 리가 나타나서 저절로 이 길은 널리 퍼져간다.\n신의 뜻을 알게 되어 말하고 말하게 되는 거야란 나타난 신님의 뜻을 깨닫고 그것을 다음 또 다음 또 다음으로 말해서 전하게 된다는 뜻."
            )
        )
        allContent.add(
            ContentItem(
                korean = " 이제까지 섭섭함은 어떤 것인가\n 다리를 저는 것이 첫째가는 섭섭함 1-31",
                japanese = "これまでのざんねんなるハはにの事\nあしのちんばか一のさんねん",
                english = "koremade no zannen naru wa nanino koto\nashi no chinba ga ichi no zannen",
                commentary = "三十一, 이제까지 어버이신이 몹시 섭섭하게 여기고 있는 일이 무엇인가 하면 그것은 슈우지의 다리병인 것이다."
            )
        )
        allContent.add(
            ContentItem(
                korean = " 이 다리는 질병이라 말하고 있지만\n 질병이 아니라 신의 노여움 1-32",
                japanese = "こんあしハやまいとゆうているけれど\nやまいでハない神のりいふく",
                english = "kono ashi wa yamai to yute iru keredo\nyamai dewa nai Kami no rippuku",
                commentary = ""
            )
        )
        allContent.add(
            ContentItem(
                korean = " 노여움도 예삿일이 아닌 만큼\n 첩첩이 쌓인 까닭이야 1-33",
                japanese = "りいふくも一寸の事でハないほどに\nつもりかさなりゆへの事なり",
                english = "rippuku mo chotto no koto dewa nai hodoni\ntsumori kasanari yue no koto nari",
                commentary = ""
            )
        )
        allContent.add(
            ContentItem(
                korean = " 노여움도 무슨 까닭인가 하면\n 나쁜 일 제거되지 않는 까닭이다 1-34",
                japanese = "りいふくもなにゆへなるどゆうならハ\nあくじがのかんゆへの事なり",
                english = "rippuku mo naniyue naru to yu naraba\nakuji ga nokan yue no koto nari",
                commentary = "제 一호 三十九의 주석 참조."
            )
        )
        allContent.add(
            ContentItem(
                korean = " 이 나쁜 일 깨끗이 제거되지 않고서는\n 역사에 방해가 되는 줄로 알아라 1-35",
                japanese = "このあくじすきやかのけん事にてハ\nふしんのしやまになるとこそしれ",
                english = "kono akuji sukiyaka noken koto nitewa\nfushin no jama ni naru to koso shire",
                commentary = "三十五, 이 나쁜 일이 집터에서 깨끗이 제거되지 않는 한 어버이신이 세계구제를 위한 마음의 역사를 수행하는 데 방해가 되는 줄로 알아라.\n역사란 슈우지가 어버이신님의 뜻에 따라 교조님과 마음을 하나로 하여 만가지 구제의 가르침을 널리 전하는 일에 노력하는 것을 의미하며, 또 하나는 일에 노력하는 것을 의미하여, 또 하나는 집터에서하는 건축이란 의미를 내포하고 있다."
            )
        )
        allContent.add(
            ContentItem(
                korean = " 이 나쁜 일 아무리 끈덕진 것이라 해도\n 신이 깨우쳐서 제거해 보일 테다 1-36",
                japanese = "このあくじなんぼしぶといものやどて\n神がせめきりのけてみせるで",
                english = "kono akuji nanbo shibutoi mono ya tote\nKami ga semekiri nokete miseru de",
                commentary = "三十六, 끈덕진 것이란 집착이 강하다는 뜻이다."
            )
        )
        allContent.add(
            ContentItem(
                korean = " 이 나쁜 일 깨끗이 제거하게 되면\n 다리를 저는 것도 깨끗해진다 1-37",
                japanese = "このあくじすきやかのけた事ならバ\nあしのちんバもすきやかとなる",
                english = "kono akuji sukiyaka noketa koto naraba\nsashi no chinba mo sukiyaka to naru",
                commentary = ""
            )
        )
        allContent.add(
            ContentItem(
                korean = " 다리만 깨끗이 낫게 되면\n 다음에는 역사 준비만을 1-38",
                japanese = "あしちいかすきやかなをりしたならバ\nあとハふしんのもよふハかりを",
                english = "ashi saika sukiyaka naori shita naraba\nato wa fushin no moyo bakari o",
                commentary = "三十八, 나쁜 일이 제거되어 다리병이 깨끗이 낫게 되면 그 다음에는 오직 역사 준비만을 한다.\n역사란 마음의 역사, 죽 세계인류의 마음을 맑히는 것을 뜻한다."
            )
        )
        allContent.add(
            ContentItem(
                korean = " 잠깐 한마디 정월 三十일로 날을 정해서\n 보내는 것도 신의 마음에서이니 1-39",
                japanese = "一寸はなし正月三十日とひをきりて\nをくるも神の心からとて",
                english = "choto hanashi shogatsu misoka to hi o kirite\nokuru mo Kami no kokoro kara tote",
                commentary = "三十九, 수유지는 오랫동안 정실이 없이 오찌에란 내연의 처와 살면서 오또지로오(音次郎)란 아들까지 두었었다. 그리고 이들은 집터에서 동거하고 있었는데, 이것은 본래 어버이신님의 의도에 맞지 않는 나쁜 일이었으므로, 이 오찌에를 친정으로 돌려보내라고 말씀하신 것이다."
            )
        )
        allContent.add(
            ContentItem(
                korean = " 곁의 사람들은 무슨 영문인가 생각하겠지만\n 앞일을 알 수 없는 까닭이다 1-40",
                japanese = "そバなものなに事するとをもへども\nさきなる事をしらんゆへなり",
                english = "soba na mono nani goto suru to omoedomo\nsaki naru koto o shiran yue nari",
                commentary = "四十, 정월 三十일로 날을 정해서 친정으로 돌려보내는 것을 사람들은 무엇 때문일까하고 이상하게 여기겠지만, 이것은 뒤에 나타날 사실을 모르기 때문이다.\n곁의 사람들이란 교조님 측근에 있는 사람들인데, 좁게는 나까야마 댁의 사람들이며 넓게는 이 길을 찾아 모여 온 사람들을 말한다."
            )
        )
        allContent.add(
            ContentItem(
                korean = " 그날이 와서 나타나면 곁의 사람들\n 신이 하는 말은 무엇이든 틀림이 없다 1-41",
                japanese = "そのひきてみへたるならバそばなもの\n神のゆう事なにもちがハん",
                english = "sono hi kite mietaru naraba soba na mono\nKami no yu koto nanimo chigawan",
                commentary = ""
            )
        )
        allContent.add(
            ContentItem(
                korean = " 지금까지는 신이 하는 말을 의심하고서\n 무엇이든 거짓이라 말하고 있었다 1-42",
                japanese = "いまゝでハ神のゆう事うたこふて\nなにもうそやとゆうていたなり",
                english = "imamade wa Kami no yu koto utagote\nnanimo uso ya to yute ita nari",
                commentary = " "
            )
        )
        allContent.add(
            ContentItem(
                korean = " 이 세상을 창조한 신이 하는 말은 \n 천의 하나도 틀림이 없다 1-43",
                japanese = "このよふをはじめた神のゆう事に\nせんに一つもちがう事なし",
                english = "kono yo o hajimeta Kami no yu koto ni\nsen ni hitotsu mo chigau koto nashi",
                commentary = " "
            )
        )
        allContent.add(
            ContentItem(
                korean = " 차츰차츰 나바나거든 납득하라\n 어떤 마음도 모두 나타날 테니 1-44",
                japanese = "だん／＼とみへてきたならとくしんせ\nいかな心もみなあらハれる",
                english = "dandan to miete kita nara tokushin se\nikana kokoro mo mina arawareru",
                commentary = "四十一～四十四, 오찌에는 친정으로 돌아간후, 며칠이 안되어 병으로  자리에 눕게되고 끝내 다시 일어나지 못했다. 만약 인정에 끌려 기일을 늦추었더라면 집터의 청소는 결국 할 수가 없었을 것이다."
            )
        )
        allContent.add(
            ContentItem(
                korean = " 모든 시대 온 세상을 살펴보면\n 인간이 가는 길도 가지각색이다 1-45",
                japanese = "よろづよのせかいぢふうをみハたせバ\nみちのしだいもいろ／＼にある",
                english = "yorozuyo no sekaiju o miwataseba\nmichi no shidai mo iroiro ni aru",
                commentary = "四十五, 예나 지금이나 세상 사는 모습을 살펴보면 인생행로는 천태만상이다."
            )
        )
        allContent.add(
            ContentItem(
                korean = " 앞으로는 길에 비유해서 말한다\n 누구 일이라고 새삼 말하지 않아 1-46",
                japanese = "このさきハみちにたとへてはなしする\nどこの事ともさらにゆハんで",
                english = "konosaki wa michi ni tatoete hanashi suru\ndoko no koto tomo sarani yuwan de",
                commentary = "四十六, 사람이 사는 길은 여러 갈래가 있는데, 앞으로는 길에 비유해서 일러줄 터이니, 어디 누구의 일이라고 말하지 않지만 하나하나 모두 단단히 듣고 잘 생각하라.\n 남의 일로 여겨 흘려 들어서는 안된다."
            )
        )
        allContent.add(
            ContentItem(
                korean = " 산언덕 가시밭 낭떠러 비탈길도\n 칼날 갈은 험한 길도 헤쳐 나가면 1-47",
                japanese = "やまさかやいばらぐろふもがけみちも\nつるぎのなかもとふりぬけたら",
                english = "yamasaka ya ibara guro mo gakemichi mo\ntsurugi no naka mo torinuke tara",
                commentary = "四十七, 산언덕을 넘고 가시덤불을 지나 낭떠러지 비탈길도, 칼날 같은 험한 길도 지나가면."
            )
        )
        allContent.add(
            ContentItem(
                korean = " 아직도 보이는 불속 깊은 물속을\n 그것을 지나가면 좁은 길이 보이느니 1-48",
                japanese = "まだみへるひのなかもありふちなかも\nそれをこしたらほそいみちあり",
                english = "mada mieru hi no naka mo ari fuchinaka mo\nsore o koshitara hosoi michi ari",
                commentary = "四十八, 아직도 불속이 있고 깊은 물속도 있으나 그것을 차츰 지나가면 비로소 좁은 길이 나온다."
            )
        )
        allContent.add(
            ContentItem(
                korean = " 좁은 길을 차츰차츰 넘어가면 큰길이야\n 이것이 확실한 본길이니라 1-49",
                japanese = "ほそみちをだん／＼こせばをふみちや\nこれがたしかなほんみちである",
                english = "hosomichi o dandan koseba omichi ya\nkore ga tashikana honmichi de aru",
                commentary = "四十九, 이좁은 길을 차츰 지나가면 마침내 큰길이 나온다. 이처럼 온갖 어려운 길을 지나서 나타나는 큰길이야말로 참으로 틀림없는 한길이다. \n四十七～四十九,이상 3수는 길에 비유해서 사람들이 걸어가여 할 길의 과정을 가르치신 것으로서, 이같은 시련을 견디며 끝까지 곤경을 극복해 나간다면 반드시 좋은 길로 나아가게 됨을 잘 깨달아야 한다."
            )
        )
        allContent.add(
            ContentItem(
                korean = " 이 이야기는 남의 일이 아닌 만큼\n 신한줄기로서 이것은 자신의 일이야 1-50",
                japanese = "このはなしほかの事でわないほとに\n神一ぢよでこれわが事",
                english = "kono hanashi hoka no koto dewa nai hodoni\nKami ichijo de kore waga koto",
                commentary = "五十, 이 이야기는 결코 남의 이기가 아니다. 인간들 을 구제하려는 진실한 어버이신의 가르침으로서, 이것은 바로 너희들 자신의 이야기다.\n이러한 길을 교조님은 몸소 걸어 우리들에게 모본의 길을 보여주셨다. 그러므로 이 모본을 그리며 따르는 이 길의 자녀들은 모두 이것을 자기 일로 생각하라는 가르침이다."
            )
        )
        allContent.add(
            ContentItem(
                korean = " 이제까지는 안의 일만을 말했다\n 이제 앞으로는 이야기를 바꿀 테다  1-51",
                japanese = "いまゝでハうちなる事をばかりなり\n、もふこれからハもんくがハるぞ",
                english = "imamade wa uchi naru koto o bakari nari\nmo korekara wa monku kawaru zo",
                commentary = "五十一, 이제까지는 주로 집터 안의 일에 대해서만 여러 가지로 일러주었으나, 앞으로는 널리 일반 세상일에 대해서도 일러주겠다."
            )
        )
        allContent.add(
            ContentItem(
                korean = " 모든 시대 온 세상을 살표보아도\n 악이란 전혀 없는 거야 1-52",
                japanese = "よろづよにせかいのところみハたせど\nあしきのものハちらにないぞや",
                english = "yorozuyo ni sekai no tokoro miwatasedo\nashiki no mono wa sara ni nai zo ya",
                commentary = ""
            )
        )
        allContent.add(
            ContentItem(
                korean = " 모든 사람들에게 악이란 없는 것이지만\n 다만 조금 티끌이 묻었을 뿐이다 1-53",
                japanese = "一れつにあしきとゆうてないけれど\n一寸のほこりがついたゆへなり",
                english = "ichiretsu ni ashiki to yute nai keredo\nchoto no hokori ga tsuita yue nari",
                commentary = " "
            )
        )
        allContent.add(
            ContentItem(
                korean = " 앞으로는 마음을 가다듬어 생각하라\n 나중에 후회 없도록 하라 1-54",
                japanese = "このさきハ心しづめてしやんせよ\nあとでこふくハいなきよふにせど",
                english = "konosaki wa kokoro shizumete shiyan seyo\nato de kokwai naki yoni seyo",
                commentary = " "
            )
        )
        allContent.add(
            ContentItem(
                korean = " 지금까지 먼 길 걸어온 과정을\n 매우 지루하게 여겼을 테지 1-55",
                japanese = "いまゝでハながいどふちふみちすがら\nよほどたいくつしたであろをな",
                english = "imamade wa nagai dochu michisugara\nyohodo taikutsu shita de aro na",
                commentary = "五十五, 어버이신이 원래 없던 세계 없던 인간을 창조한 이래 아주 오랜 세월이 흘렀는데, 그 동안 사람들은 진실한 가르침을 들을 수 없었기 때문에 의지할 어버이도 모른 채 무척 지루하게 살아왔을 것이다.\n이 노래는 지금까지 오랫동안 인간이 진실한 천계(天啓)를 들을 수 없었던 것을 길에 비유해서 가르치신 것이다."
            )
        )
        allContent.add(
            ContentItem(
                korean = " 이번에는 이미 확실한 참배장소가\n 이룩되었으니 납득하라 1-56",
                japanese = "このたびハもふたしかなるほいりしよ\nみへてきたぞへとくしんをせよ",
                english = "konotabi wa mo tashika naru mairisho\nmiete kita zoe tokushin o seyo",
                commentary = "五十六, 순각한의 도래로 진실한 어버이신이 이 세상에 나타나서 인간창조의 본고장에 참배장소인 근행장소도 이룩했으므로, 사람들은 망설임 없이 안심하고 신앙을 하라."
            )
        )
        allContent.add(
            ContentItem(
                korean = " 이제부터는 먼 길 걸어온 과정을\n 일러줄 테니 깊이 생각하라 1-57",
                japanese = "これからハながいどふちふみちすがら\nといてきかするとくとしやんを",
                english = "korekara wa nagai dochu michisugara\ntoite kikasuru tokuto shiyan o",
                commentary = "五十七, 인간들은 창조한 이래 오랜 세월 동안 여러 경로를 거쳐왔는데, 이제부터는, 이제부터는 그 거쳐온 과정을 일러줄 터이니 잘 듣고 생각하라. 그러면 어버이신이 어떻게 마음을 기울여 왔는지도 알게 될 것이다."
            )
        )
        allContent.add(
            ContentItem(
                korean = " 앞으로는 안을 다스릴 준비\n 신은 마음 서두르고 있다 1-58",
                japanese = "このさきハうちをおさめるもよふだて\n神のほふにハ心せきこむ",
                english = "konosaki wa uchi o osameru moyodate\nKami no ho niwa kokoro sekikomu",
                commentary = "五十八, 이제부터는 집터 안을 맑히는 준비를 시작하겠는데, 이것이 하루라도 빨리 이루어지도록 어버이신은 서두르고 있다."
            )
        )
        allContent.add(
            ContentItem(
                korean = " 차츰차츰 신이 하는 말을 들어 다오\n 그럿된 말은 결코 하지 않아 1-59",
                japanese = "だん／＼と神のゆふ事きいてくれ\nあしものことハさらにゆハんで",
                english = "dandan to Kami no yu koto kiite kure\nashiki no koto wa sarani yuwan de",
                commentary = ""
            )
        )
        allContent.add(
            ContentItem(
                korean = " 이 아이 二년 三년 가르치려고\n 애 쓰고 있지만 신의 손 뗌 1-60",
                japanese = "このこ共二ねん三ねんしこもふと\nゆうていれども神のてはなれ",
                english = "kono kodomo ni nen san nen shikomo to\nyute iredomo Kami no tebanare",
                commentary = "六十, 이 아이를 二, 三년 가르치려고 그 부모는 애쓰고 있지만 어버이신은 이 아이가 이미 수명이 다 되었음을 잘 알고 있다.\n이 아이란 슈우지의 서녀(庶女)인 오슈우를 말하는데, 당시에 부모는 신부수업을 가르치려 하고 있었는데, 어버이신님은 그가 출직할 것을 예언하신 것이다."
            )
        )
        allContent.add(
            ContentItem(
                korean = " 생각하라 어버이가 아무리 애를 써도\n 신의 손 뗌 이에는 당할 수 없다 1-61",
                japanese = "いやんせよをやがいかほどをもふても\n神のてばなれこれハかなハん",
                english = "shiyan seyo oya ga ika hodo omotemo\nKami no tebanare kore wa kanawan",
                commentary = "六十一, 잘 생각해 보라, 부모가 아무리 자식 귀여운 마음에서 오래 살기를 바랄지라도 어버이신이 수호하지 않는다면 어쩔 수 없는 것이니 이 점을 잘 깨달아야 한다."
            )
        )
        allContent.add(
            ContentItem(
                korean = " 이 세상에는 악한 것 섞여 있으므로\n 인연을 쌓아서는 안되는 거야 1-62",
                japanese = "このよふハあくしもじりであるからに\nいんねんつけ事ハいかんで",
                english = "kono yo wa akuji majiri de aru karani\ninnen tsukeru koto wa ikan de",
                commentary = "六十二, 이 세상은 자칫하면 악에 물들기 쉬운 곳이므로 주의해서 악인연을 짓지 않도록 해야 한다."
            )
        )
        allContent.add(
            ContentItem(
                korean = " 자신으로는 벌써 五十이라 생각하지만\n 신의 눈으로는 아직 장래가 있다 1-63",
                japanese = "わかるにハもふ五十うやとをもへとも\n神めへにハまださきがある",
                english = "waga mi niwa mo goju ya to omoedomo\nKami no me niwa mada saki ga aru",
                commentary = "六十三, 이제 곧 나이 五十이므로 자신으로서는 꽤나 나이가 많다고 생각하겠지만 어버이신이 볼 때는 아직도 장래가 있다."
            )
        )
        allContent.add(
            ContentItem(
                korean = " 올해부터 六十년은 확실하게\n 신이 단단히 맡을 테다 1-64",
                japanese = "ことしより六十ねんハしいかりと\n神のほふにハしかとうけやう",
                english = "kotoshi yori rokuju nen wa shikkari to\nKami no ho niwa shikato ukeyau",
                commentary = "六十四, 올해부터 앞으로 六十년은 어버이신이 확실히 맡는다.\n이것은 슈우지에게 하신 말씀으로서, 당시 四十九세였다."
            )
        )
        allContent.add(
            ContentItem(
                korean = " 이제부터는 마음 단단히 바꿔라\n 나쁜 일 치우고 젊은 아내를 1-65",
                japanese = "これからハ心しいかりいれかへよ\nあくじはろふてハかきによほふ",
                english = "korekara wa kokoro shikkari irekae yo\nakuji harote wakaki nyobo",
                commentary = "六十五, 젊은 아내란 인연이 있어 슈우지의 부인이 된 야마또(大和) 지방 헤구리(平群)군 뵤도도오지(平等寺) 마을의 고히가시 마사끼찌(小東政吉)의 차녀 마쯔에로서 당시 十九세였다."
            )
        )
        allContent.add(
            ContentItem(
                korean = " 이것 역시 어려운 듯하지만\n 신이 나가면 데려올 거야 1-66",
                japanese = "これとてもむつかしよふにあるけれど\n神がでたならもろてくるそや",
                english = "kore totemo mutsukashi yoni aru keredo\nKami ga deta nara morote kuru zo ya",
                commentary = "六十六, 이것은 나이 차이로 어려운 일처럼 생각되겠지만 어버이신이 나간다면 반드시 혼담을 성사시킬 것이다.\n이 혼담은 처음에 닷다(龍田) 마을의 감베에란 중매인이 고히가시 댁에 교섭을 했으나 일이 잘되지 않았다. 그래서 교조님이 몸소 행차하여 여러 가지로 일러주시자, 저쪽에서도 비로소 납득하게 되어 이윽고 혼담이 성립되었다."
            )
        )
        allContent.add(
            ContentItem(
                korean = " 나날이 마음 다한 그 다음에는\n 앞으로 책임을 모두 맡길 테다 1-67",
                japanese = "にち／＼に心尽くしたそのゑハ\nあとのしはいをよろづまかせる",
                english = "nichinichi ni kokoro tsukushita sono ue wa\nato no shihai o yorozu makaseru",
                commentary = "六十七, 마음을 다해서 매일 어버이신의 일에 전념한다면 앞으로 집터의 책임을 모두 맡긴다.\n이것은 슈우지에게 하신 말씀이다."
            )
        )
        allContent.add(
            ContentItem(
                korean = " 다섯 사람 가운데 둘은 집에 두라\n 나머지 세 사람은 신이 맡는다 1-68",
                japanese = "五人あるなかのにゝんハうちにをけ\nあと三人ハ神のひきうけ",
                english = "go nin aru naka no ni nin wa uchi ni oke\nato san nin wa Kami no hikiuke",
                commentary = "六十八, 이것은 슈우지의 부인 마쯔에의 친정 고히가시 댁에 대해 하신 말씀으로 고히가시 마사끼찌에게는 오사꾸, 마쯔에, 마사따로오(政太郎), 가메끼찌〔龜吉․후에 사다지로오(政次郎)로 개명〕, 오또끼찌〔音吉․후에 센지로오(仙次郎)로 개명〕등, 다섯 자녀가 있었다. 어버이신님은 이들 중 두 사람은 집안일을 시키고, 나머지 세 사람은 어버이신님께 바치면 그 뒷일은 모두 맡겠다고 말씀하셨다."
            )
        )
        allContent.add(
            ContentItem(
                korean = " 모든 시대 온 세상의 일을 살펴보고\n 마음을 가다듬어 생각해 보라 1-69",
                japanese = "よろづよのせかいの事をみはらして\n心しづめてしやんしてみよ",
                english = "yorozuyo no sekai no koto o miharashite\nkokoro shizumete shiyan shite miyo",
                commentary = "六十九, 세상에서 일어나는 여러 가지 일들을들을 살펴보고 마음을 가다듬어 이제부터 가르치는 것을 잘 생각해 보라 "
            )
        )
        allContent.add(
            ContentItem(
                korean = " 지금까지도 신의 세상이었지만\n 중매하는 일은 이것이 처음이야 1-70",
                japanese = "いまゝても神のせかいであるけれど\nあかだちするハ今がはじめや",
                english = "imamade mo Kami no sekai de aru keredo\nnakadachi suru wa ima ga hajime ya",
                commentary = "七十, 지금까지도 이 세상의 모든 일은 어버이신이 지배해 왔지만, 어버이신이 세상밖으로 나가서 부부의 연을 맺어 주는 것은 이것이 처음이다."
            )
        )
        allContent.add(
            ContentItem(
                korean = " 이제부터 세상 사람들은 비웃을 거야\n 중매하는 일은 이것이 처음이야 1-71",
                japanese = "これからハせかいの人ハをかしがる\nなんぼハろてもこれが大一",
                english = "korekara wa sekai no hito wa okashigaru\nnanbo warotemo kore ga daiichi",
                commentary = "七十一, 세상 사람들은 이 결혼을 나이 차이로 합당치 않다고 비웃고 있지만, 그것은 사람들이 어버이신의 참뜻을 모르기 때문이다. 그리고 이 결혼은 서로 같은 인연을 모아 부부의 연을 맺는 것으로서, 이는 인생의 근본인 만큼 무엇보다도 중요한 것이다."
            )
        )
        allContent.add(
            ContentItem(
                korean = " 세상에서는 무엇을 하느냐고 말하겠지\n 사람이 비웃어도 신은 좋아한다 1-72",
                japanese = "せかいにハなに事するとゆうとである\n人のハらいを神がたのしむ",
                english = "sekai niwa nani goto suru to yu de aro\nhito no warai o Kami ga tanoshimu",
                commentary = "七十二, 세상에서는 왜 저런 일을 하는가고 이상하게 여기겠지만, 이는 전혀 인연에 대해 모르기 때문이다. 그러나 머지않아 이를 깨닫는 날이 올 것이므로 어버이신으로서는 이 같은 한때의 웃음거리가 오히려 즐거운 일이다."
            )
        )
        allContent.add(
            ContentItem(
                korean = " 각자가 생각하는 마음은 안돼\n 신의 마음은 전연 틀리는 거야 1-73",
                japanese = "めへめへのをもふ心ハいかんでな\n神の心ハみなちがうでな",
                english = "meme no omou kokoro wa ikan de na\nKami no kokoro wa mina chigau de na",
                commentary = "七十三, 이 결혼에 대해 사람들은 인간마음으로 여러 가지 해석들을 하지만, 어버이신은 이와는 전혀 다른 깊은 뜻이 있다.\n이 결혼에는 근행을 서두르시는, 즉 근행 준비로서 인원을 갖추려고 서두르시는 깊은 신의(神意)가 내포되어 있다."
            )
        )
        allContent.add(
            ContentItem(
                korean = " 전생 인연 모아서 수호한다\n 이것으로 영원히 확실하게 다스려진다 1-74",
                japanese = "せんしよのいんねんよせでしうごふする\nこれハもつだいしとをちまる",
                english = "zensho no innen yosete shugo suru\nkore wa matsudai shikato osamaru",
                commentary = "七十四, 전생에서부터 깊은 인연이 있는 사람을 이 접터에 이끌어 들여 부부가 되도록 영원히 확실하게 다스린다.\n 전생인연이란 슈지 선생과 마쓰에님과의 관계를 두고 하신 말씀으로, 두 사람은 전생의 인연에 의해서 금생에 부부가 되어야 할 사이였으며, 또 터전에 깊은 인연이 있는 사람들이었다."
            )
        )
        allContent.add(
            ContentItem(
                korean = " 이제부터는 한길을 내기 시작한다\n 세상사람들의 마음 모두 용솟음치게 할 거야 2-1",
                japanese = "これからハをくハんみちをつけかける\nせかいの心みないさめるで",
                english = "korekara wa okwan michi o tsuke kakeru\nsekai no kokoro mina isameru de",
                commentary = "一, 지금까지는 주로 이 길을 그리며 찾아오는 사람들을 가르쳐 왔으나, 이제부터는 세계에 널리 가르침을 펴서 세상 사람들의 마음을 용솟음치게 한다.\n한길이란 많은 사람이 아무런 위험 없이 안심하고 다닐 수 있는 큰길이라는 뜻이데, 여기서는 빈부 귀천이나 민족 여하를 불문하고 모두 빠짐없이 전인류를 구제하는 길이란 뜻이다."
            )
        )
        allContent.add(
            ContentItem(
                korean = " 윗사람들은 마음 용솟음치게 될 것이니\n 언제라 할 것 없이 각한이 다가왔다 2-2",
                japanese = "上たるハ心いさんでくるほとに\nなんどきにくるこくけんがきた",
                english = "kami taru wa kokoro isande kuru hodoni\nnandoki ni kuru kokugen ga kita",
                commentary = "二, 한길을 닦아 가면 윗사람들도 이 가름침을 듣고 마음이 용솟음쳐 찾아오게 된다. 더욱이 이것은 머지않은 일로서 이제 곧 실현될 단계에 와 있다."
            )
        )
        allContent.add(
            ContentItem(
                korean = " 찻잎을 따고 나서 고르기를 마치면\n 다음에 할 일은 즐거운근행이야 2-3",
                japanese = "ちやつんであとかりとりてしもたなら\nあといでるのハよふきづとめや",
                english = "cha tsunde ato karitorite shimota nara\nato i deru no wa Yoki-zutome ya",
                commentary = "三, 찻잎을 따고 나서 가지 고르기를 마치면 그 다음에는 드디어 즐거운근행을 시작한다.\n이 지역에서 찻잎을 따는 시기는 대체로 음력 5월 중순경이다."
            )
        )
        allContent.add(
            ContentItem(
                korean = " 이 근행 어떻게 이루어진다고 생각하는가\n 윗사람들 마음 용솟음칠 거야 2-4",
                japanese = "このつとめとこからくるとをもうかな\n上たるところいさみくるぞや",
                english = "kono Tsutome doko kara kuru to omou kana\nkami taru tokoro isami kuru zo ya",
                commentary = "四, 이 즐거운근행은 누가 행하는가 하면 마음이 바꿔진 사람들부터 행하게 된다. 그렇게 되면 윗사람들도 저절로 마음이 용솟음치게 될 것이다."
            )
        )
        allContent.add(
            ContentItem(
                korean = " 차츰차츰 신의 수호는\n 모두 진기한 일만 시작할 거야 2-5",
                japanese = "たん／＼と神のしゆごふとゆうものハ\nめつらし事をみなしかけるで",
                english = "dandan to Kami no shugo to yu mono wa\nmezurashi koto o mina shikakeru de",
                commentary = "五, 어버이신은 영묘한 수호로써 차츰 사람들이 생각지도 못했던 신기한 힘을 나타내어 지금까지 모르던 진기한 일을 시작한다."
            )
        )
        allContent.add(
            ContentItem(
                korean = " 나날이 신이 마음 서두르는 것을\n 모든 사람들은 어떻게 생각하는가 2-6",
                japanese = "にち／＼に神の心のせきこみを\nみないちれつハなんとをもてる",
                english = "nichinichi ni Kami no kokoro no sekikomi o\nmina ichiretsu wa nanto omoteru",
                commentary = " "
            )
        )
        allContent.add(
            ContentItem(
                korean = " 어떤 것이든 질병이나 아픔이란 전혀 없다\n 신의 서두름 인도인 거야 2-7",
                japanese = "なにゝてもやまいいたみハさらになし\n神のせきこみてびきなるそや",
                english = "nani nitemo yamai itami wa sarani nashi\nKami no sekikomi tebiki naru zo ya",
                commentary = "七, 세상 사람들은 질병이니 통증이니 하고들 있으나 결코 그런 것이 아니다. 그것은 모두 어버이신의 깃은 의도에 의한 서두름이며 인도이다."
            )
        )
        allContent.add(
            ContentItem(
                korean = " 서두르는 것도 무슨 까닭인가 하면\n 근행인원이 필요하므로 2-8",
                japanese = "せきこみもなにゆへなるとゆうならば\nつとめのにんぢうほしい事から",
                english = "sekikomi mo naniyue naru to yu naraba\nTsutome no ninju hoshii koto kara",
                commentary = "八, 어버이신이 왜 서두르고 있는가 하면, 그것은 빨리 근행인원을 이끌어 들이고 싶기 때문이다.\n근행인원은 第十호 二十五～二十七의 주석 참조."
            )
        )
        allContent.add(
            ContentItem(
                korean = " 이 근행 어떤 것이라 생각하는가\n 만가지구제의 준비만을 2-9",
                japanese = "このつとめなんの事やとをもている\nよろづたすけのもよふばかりを",
                english = "kono Tsutome nanno koto ya to omote iru\nyorozu tasuke no moyo bakari o",
                commentary = "九, 어버이신이 바라고 있는 즐거운근행은 무엇 때문이라고 생각하는가, 이것으로 세상의 만가지를 구제하고 싶기 때문이다."
            )
        )
        allContent.add(
            ContentItem(
                korean = " 이 구제 지금뿐이라고는 생각 말라\n 이것이 영원한 고오끼인 거야 2-10",
                japanese = "このたすけいまばかりとハをもうなよ\nこれまつたいのこふきなるぞや",
                english = "kono tasuke ima bakari towa omouna yo\nkore matsudai no Koki naru zo ya",
                commentary = "十, 이 구제는 현재뿐이라고 생각해서는 안된다. 이것은 영원한 본보기가 되어 언제까지나 구제의 결실을 거두게 된다.\n영원한 고오끼란 영원히 후세에까지 전해져 구제한줄기의 가르침의 토대가 됨을 말한다."
            )
        )
        allContent.add(
            ContentItem(
                korean = " 잠깐 한마디 신경증이다 광증이다 말하고 있다\n 질병이 아니라 신의 서두름 2-11",
                japanese = "一寸はなしのぼせかんてきゆうている\nやまいでハない神のせきこみ",
                english = "choto hanashi nobose kanteki yute iru\nyamai dewa nai Kami no sekikomi",
                commentary = "十一, 세상 사람들은 흔히들 저 사람은 신경증이다 광증이다고 말하고 있으나 결코 정신이 돈 것도 아니요, 질병도 아니다. 빨리 이 길로 이끌어 들이려는 어버이신의 서두름이다.\n다음 노래의 주석을 참조"
            )
        )
        allContent.add(
            ContentItem(
                korean = " 차츰차츰 진실한 신한줄기를\n 일러주어도 아직도 몰라 2-12",
                japanese = "たん／＼としんぢつ神の一ちよふ\nといてきかせどまだハかりない",
                english = "dandan to shinjitsu Kami no ichijo o\ntoite kikasedo mada wakari nai",
                commentary = "十二, 여러가지로 진실한 신한줄기의 길을 일러주어도 아직 깨닫지 못하고 있다.\n 쯔지 추우사꾸(辻忠作)는 1863년에 입신했는데, 그 동기는 누이동생 구라의 발광 때문이었다. 그는 어느 날, 친척되는 이찌노모또(櫟本)마을의 가지모또(梶本)가에서 쇼야시끼의 신님은 만가지구제를 하시는 신님이란 말을 듣고 비로소 신앙할 마음이 생겼다. 추우사꾸는 즉시 교조님을 찾아 뵙고 여러 가지 가르침을 들은 후 신앙을 했던 바, 구라의 발광은 씻은 듯이 나았다. 그래서 그는 이 길을 열심히 신앙하게 되었으며, 구라는 그 뒤에 혼담이 이루어져 센조꾸(千束)라는 곳으로 시집을 갔다. 그러나 그 후 추우사꾸는 집안 사람들의 반대에 부딪쳐 차차 신앙심이 약해짐에 따라 교조님께 전혀 발걸음을 하지 않게 되었는데. 그러자 이상하게도 구라의 병이 다시 재발하여 시집에서 쫒겨나게 되었다.\n이에 사람들은 구라가 정신이 돌았기 때문에 쫓겨 났다느니, 혹은 이혼을 당했기 때문에 돌았다느니 하는 여러 가지 말들을 하고 있었으나, 그것은 결코 사람들이 흔히 말하는 질병도 광증도 아닌, 오직 신앙에서 멀어진 추우사꾸를 이 길로 다시 이끌어 들이시려는 어버이신님의 깊은 의도였던 것이다. 이러한 가르침을 들은 추우사꾸는 자신의 잘못을 참회하고 다시 열심히 어버이신님의 일에 힘쓰게 되었다. 그러자 구라의 발광은 씻은 듯이 나아 다시 시집으로 돌아가게 되었다."
            )
        )
        allContent.add(
            ContentItem(
                korean = " 어서어서 세상에 나가려고 생각하지만\n 길이 없어서는 나가려야 나갈 수 없다 2-13",
                japanese = "といてきかせどまだハかりなも\nみちがのふてハでるにでられん",
                english = "hayabaya to omote deyo to omoedomo\nmichi ga note wa deru ni deraren",
                commentary = "十三, 지금까지는 여기 찾아오는 사람에게만 가르침을 일러주었으나, 이제는 한시라도 빨리 이쪽에서 밖으로 나아가 세상 사람들에게 널리 가르침을 일러주고자 한다. 그러나 길이 억어서는 나아갈 수가 없다."
            )
        )
        allContent.add(
            ContentItem(
                korean = " 이 길을 빨리 내려고 생각하지만\n 다른 곳에서는 낼 데가 없다 2-14",
                japanese = "このみちをはやくつけよとをもへとも\nほかなるとこでつけるとこなし",
                english = "kono michi o hayaku tsukeyo to omoedomo\nhoka naru toko de tsukeru toko nashi",
                commentary = "十四, 세상에 널리 이 가르침을 전하고자 하나, 이 길은 아무데서나 낼 수 있는 것이 아니기 때문에 이곳 아닌 다른 곳에서는 낼 수가 없다."
            )
        )
        allContent.add(
            ContentItem(
                korean = " 이 길을 진실로 생각한다면\n 가슴속으로 만가지를 생각하라 2-15",
                japanese = "このみちをしんぢつをもう事ならば\nむねのうちよりよろづしやんせ",
                english = "kono michi o shinjitsu omou koto naraba\nmune no uchi yori yorozu shiyan se",
                commentary = "十五, 이 신한줄기의 길을 진실로 생각한다면 마음을 가다듬어 만사를 잘 생각해 보라."
            )
        )
        allContent.add(
            ContentItem(
                korean = " 이 이야기 무엇을 말한다고 생각하는가\n 신의 구제장소를 서두르는 것이 2-16",
                japanese = "このはなしなんの事やとをもている\n神のうちわけばしよせきこむ",
                english = "kono hanashi nanno koto ya to omote iru\nKami no uchiwake basho sekikomu",
                commentary = "十六, 지금 가르친 이야기의 참뜻은 무엇이라 생각하는가, 그것은 방방곡곡에 구제장소를 마련하기를 서두르는 것이다.\n구제장소란 장래 안과 중간과 밖에 각각33개소, 도합 93개소가 생기게 되는데, 어떤 어려운 질병이라도 이 구제장소를 도는 동안에 구제받게 되며, 이 가운데 한 곳은 아주 먼 벽지에 있다. 그러나 이 곳을 빠뜨려서는 구제받지 못한다. 그리고 가령 도중에서 구제를 받았더라도 탈것이나 지팡이를 버리지 말고, 고맙게 도움받았다는 사실을 사람들에게 알리며, 이것을 마지막에는 터전에 올려야 한다. 만약 도중에서 이를 버릴 경우에는 일단 구제를 받았더라도 다시 본래대로 된다고 말씀하셨다."
            )
        )
        allContent.add(
            ContentItem(
                korean = " 이 길이 조금 나타나기 시작하면\n 세상 사람들의 마음 모두 용솟음친다 2-17",
                japanese = "このみちが一寸みゑかけた事ならば\nせかいの心みないさみてる",
                english = "kono michi ga choto miekaketa koto naraba\nsekai no kokoro mina isami deru",
                commentary = "十七, 이 가르침이 나타나기 시작하면 세상 사람들의 마음은 모두 용솟음치게 된다."
            )
        )
        allContent.add(
            ContentItem(
                korean = " 무엇이든 신이 하는 말 단단히 들어라\n 집터의 청소가 되었으면 2-18",
                japanese = "なにゝても神のゆう事しかときけ\nやしきのそふぢでけた事なら",
                english = "nani nitemo Kami no yu koto shikato kike\nyashiki no soji deketa koto nara",
                commentary = ""
            )
        )
        allContent.add(
            ContentItem(
                korean = " 벌써 보인다 한눈 팔 새 없을 만큼\n 꿈결같이 티끌이 털리는 거야 2-19",
                japanese = "もふみへるよこめふるまないほどに\nゆめみたよふにほこりちるぞや",
                english = "mo mieru yokome furu ma mo nai hodoni\nyume mita yoni hokori chiru zo ya",
                commentary = "十八, 十九, 어떤 일이라도 어버이신의 가르침을 단단히 듣도록 하라. 집터 안의 사람들이 마음을 청소하여 이것이 어버이신이게 통하게 되면 눈 깜빡할 사이에 티끌은 털리고 만다."
            )
        )
        allContent.add(
            ContentItem(
                korean = " 이 티끌 깨끗하게 털어 버리면\n 앞으로는 만가지 구제한줄기 2-20",
                japanese = "このほこりすきやかはろた事ならば\nあとハよろづのたすけ一ちよ",
                english = "kono hokori sukiyaka harota koto naraba\nato wa yorozu no tasuke ichijo",
                commentary = ""
            )
        )
        allContent.add(
            ContentItem(
                korean = " 앞으로는 차츰차츰 근행 서둘러서\n 만가지구제의 준비만을 2-21",
                japanese = "このさきハたん／＼つとめせきこんで\nよろづたすけのもよふばかりを",
                english = "konosaki wa dandan Tsutome sekikonde\nyorozu tasuke no moyo bakari o",
                commentary = ""
            )
        )
        allContent.add(
            ContentItem(
                korean = " 온 세상 어디가 나쁘다 아프다 한다\n 신의 길잡이 인도임을 모르고서 2-22",
                japanese = "せかいぢうとこがあしきやいたみしよ\n神のみちをせてびきしらすに",
                english = "sekaiju doko ga ashiki ya itamisho\nKami no michiose tebiki shirazu ni",
                commentary = "二十二, 세상에서는 어디가 나쁘다, 어디가 아프다고 말하고 있으나, 사실은 질병이라 전혀 없다. 설사 나쁜 데나 아픈데가 있더라도 그것은 어버이신의 가르침이며 인도에다. 그런데도 세상에서는 이를 전혀 깨닫지 못하고 질병이라고만 생각하고 있다."
            )
        )
        allContent.add(
            ContentItem(
                korean = " 이 세상에 질병이란 없는 것이니\n 몸의 장애 모두 생각해 보라 2-23",
                japanese = "このよふにやまいとゆうてないほどに\nみのうちさハりみなしやんせよ",
                english = "kono yoni yamai to yute nai hodoni\nminouchi sawari mina shiyan seyo",
                commentary = "二十三, 이 세상에는 질병이란 전혀 없다. 따라서 만약 몸에 이상이 생길 때는 어버이신이 무엇 때문에 이같은 가르침이나 인도를 나타내 보이는가를 잘 생각해 보라."
            )
        )
        allContent.add(
            ContentItem(
                korean = " 나날이 신이 서두르는 이 구제\n 모든 사람든은 어떻게 생각하는가 2-24",
                japanese = "にち／＼に神のせきこみこのたすけ\nみな一れつハなんとをもてる",
                english = "nichinichi ni Kami no sekikomi kono tasuke\nmina ichiretsu wa nanto omoteru",
                commentary = "二十四, 나날이 어버이신이 사람들의 맘의 장애를 인도로 하여 구제를 서두르고 있음을 세상 사람들은 어떻게 생각하고 있는가. 이러한 어버이신의 간절한 마음을 어서 깨달아 주었으면."
            )
        )
        allContent.add(
            ContentItem(
                korean = " 녺은 산의 못은 맑은 물이지만\n 첫머리는 탁하고 찌꺼기 섞여 있다 2-25",
                japanese = "高山のをいけにハいた水なれど\nてバなハにこりごもくまぢりで",
                english = "takayama no oike ni waita mizu naredo\ndebana wa nigori gomoku majiri de",
                commentary = "二十五, 깊은 산속에 있는 못의 맑은 물이란 사람에 비유해서 하신 말씀으로, 인간이 처음 태어날 때는 누구나가 다 청청한 마음을 지니고 있으나, 세월이 지남에 따라 자신만을 생각하는 욕심 때문에 티끌이 쌓여 차츰 마음이 흐려짐을 뜻한다. 첫머리란 입신 당시의 혼탁한 마음을 가리키는 것으로 해석된다."
            )
        )
        allContent.add(
            ContentItem(
                korean = " 차츰차츰 마음을 가다듬어 생각하면\n 맑은 물로 바꿔질거야 2-26",
                japanese = "だん／＼と心しづめてしやんする\nすんだる水とかハりくるぞや",
                english = "dandan to kokoro shizumete shiyan suru\nsundaru mizu to kawari kuru zo ya",
                commentary = "二十六, 처음에는 티끌이 섞인 탁한 마음일지라도 어버이신의 가르침을 듣고 깊이 반성하여 마음의 티끌을 제거하도록 노력하면, 차츰 물이 맑아지듯 마음이 청정하게 바꿔진다."
            )
        )
        allContent.add(
            ContentItem(
                korean = " 산중에 있는 물속에 들어가서\n 어떤 물이라도 맑히리라 2-27",
                japanese = "山なかのみづのなかいと入こんで\nいかなる水もすます事なり",
                english = "yama naka no mizu no naka i to irikonde\nika naru mizu mo sumasu koto nari",
                commentary = "二十七, 산중에 있는 물속에 들어가서 그 물이 아무리 탁할지라도 깨끗이 맑히겠다.\n혼탁한 세상을 정화하는 것이 본교의 사명임을 말씀하신 것이다."
            )
        )
        allContent.add(
            ContentItem(
                korean = " 나날이 마음 다하는 사람들은\n 가슴속 진정하라 끝에는 믿음직하리 2-28",
                japanese = "にち／＼に心つくするそのかたわ\nむねをふさめよすゑハたのもし",
                english = "nichinichi ni kokoro tsukusuru sono kata wa\nmune o osame yo sue wa tanomoshi",
                commentary = "二十八, 나날이 이 길을 위해 마음을 다하여 이바지해 온 사람은 이젠 머지않았으니 어떤 가운데서도 마음을 쓰러뜨리지 말고 따라오라. 끝에는 반드시 믿음직한 길이 나타날 것이다."
            )
        )
        allContent.add(
            ContentItem(
                korean = " 이제부터는 높은산의 못에 뛰어들어가\n 어떤 찌꺼기도 청소하리라 2-29",
                japanese = "これからハ高山いけいとびはいり\nいかなごもくもそうぢするなり",
                english = "korekara wa takayama ike i tobihairi\nikana gomoku mo soji suru nari",
                commentary = "二十九, 이제부터는 길을 내기 어려운 곳에도 들어가 아무리 마음이 혼탁한 자라도 티끌을 털어내어 청정하게 할 것이다."
            )
        )
        allContent.add(
            ContentItem(
                korean = " 찌꺼기만 깨끗이 치워버리면\n 그 다음에 물은 맑아지리라 2-30",
                japanese = "こもくさいすきやかだしてしもたなら\nあとなる水ハすんであるなり",
                english = "gomoku sai sukiyaka dashite shimota nara\nato naru mizu wa sunde aru nari",
                commentary = "三十, 아무리 티끌이 쌓여 있는 자라도 그 마음의 더러움을 완전히 씻어 버린다면, 그 다음은 맑은물처럼 깨끗해진다."
            )
        )
        allContent.add(
            ContentItem(
                korean = " 이제부터는 미칠곳과 미친곳에 대해 말한다\n 무슨 말을 하는지 모를 테지 2-31",
                japanese = "これからハからとにほんのはなしする\nなにをゆうともハかりあるまい",
                english = "korekara wa kara to nihon no hanashi suru\nnani o yu tomo wakari arumai",
                commentary = "三十一, 지금부터는 미칠곳과 미친곳에 대해 이야기를 하겠는데, 어버이신이 무슨 말을 할지 잘 모를 것이다.\n이 노래 이하 三十四의 노래까지는 第二호 四十七의 주석 참조."
            )
        )
        allContent.add(
            ContentItem(
                korean = " 모르는자가 미친곳의 땃에 들어와서\n 멋대로 하는 것이 신의 노여움 2-32",
                japanese = "とふぢんがにほんのぢいゝ入こんで\nまゝにするのが神のりいふく",
                english = "tojin ga nihon no jii i irikonde\nmamani suru no ga Kami no rippuku",
                commentary = "三十二, 어버이신의 가르침을 아직 모르는 자가 미친 곳에 들어가 뽐내며 제멋대로 하고 있는 것이 어비이신으로서는 참으로 안타까운 일이다."
            )
        )
        allContent.add(
            ContentItem(
                korean = " 차츰차츰 미친곳을 구제할 준비를 하니\n 모르는자도 신이 마음대로 하리라 2-33",
                japanese = "たん／＼とにほんたすけるもよふだて\nとふじん神のまゝにするなり",
                english = "dandan to nihon tasukeru moyodate\ntojin Kami no mamani suru nari",
                commentary = "三十三, 어버이신은 차츰 미친곳에 어버이신의 참뜻을 널리 알려서 세계 인류를 구제할 준비를 하고 있으므로, 아직 어버이신의 가르침을 모르는 자에게도 머지않아 신의를 납득시켜 용솟을치며 신은(神恩)을 입도록 한다."
            )
        )
        allContent.add(
            ContentItem(
                korean = " 앞으로는 미칠곳과 미친곳을 구분한다\n 이것을 분간하게 되면 온 세상 안정이 된다 2-34",
                japanese = "このさきハからとにほんをハけるてな\nこれハかりたらせかいをさまる",
                english = "konosaki wa kara to nihon o wakeru de na\nkore wakaritara sekai osamaru",
                commentary = "三十四, 금후는 미칠곳과 미친곳을 구분하는데 이것만 알게 되면 사람들의 마음은 맑아져 세상은 절로 안정이 된다.\n미친곳이란 태초에 어버이신님이 이 세상 인간을 창조하신 터전이 있는 곳, 따라서 이 가르침을 맨 먼저 편 세계구제의 본고장이 있는 곳을 말한다. 또 어버이신님의 뜻을 이미 알고 있는 자를 말한다. 미칠곳이란 어버이신님의 뜻이 전해질 곳, 또는 다음에 어버이신님의 가르침을 전해 듣게 될 자를 말한다.\n아는자란 어버이신님의 가르침을 먼저 들은 자, 즉 어버이신님의 가르침을 이미 깨달은 자를 만한다.\n모르는자란 이 가르침을 다음에 듣게 될 자, 즉 아직 어버이신님의 가르침을 모르는 자를 말한다.미칠곳과 미친곳에 관한 일련의 노래는 친필을 집필하실 당시, 과학기술을 도입하기에 급급한 나머지, 물질문명에만 현혹되어 문명 본래의 생명인 인류애와 공존공영(共存共榮)의 정신은 이해하려 하지 않고 오직 물질주의, 이기주의로만 흐르고 있던 당시 사람들에게 엄한 경고를 하시고, 빨리 어버이신님의 뜻을 깨달아 구제한줄기의 정신으로 나아가라고 격려하신 노래이다. 즉, 어버이신님의 눈으로 보시면 온 세상 사람들은 모두 귀여운 자녀이다. 따라서 어버이신님의 뜻을 알든 모르든, 또 가르침을 먼저 들은 자든 나중에 듣는 자든 여기에는 아무런 차별도 없이 온 세상 사람들을 궁극적으로 똑같이 구제하시려는 것이 어버이신님의 어버이마음이다. 그러므로 어버이신은 사람들의 마음이 맑아져서 서로가 형제자며임을 자각하고, 서로 위하고 서로 돕는 마음이 되어 하루라도 빨리 즐거운 삻을 누릴 날이 오기를 서두르고 계신다.(第十호 五五, 五六의 주석, 제 十二호 七의 주석 참조)"
            )
        )
        allContent.add(
            ContentItem(
                korean = " 이제까지 윗사람들은 신의 마음 모르고서\n 세상 보통일로 생각하고 있었다 2-35",
                japanese = "いまゝでハ上たる心ハからいで\nせかいなみやとをもていたなり",
                english = "imamade wa kami taru kokoro wakaraide\nsekainami ya to omote ita nari",
                commentary = "三十五, 지금까지 윗사람들은 어버이신의 마음을 모르기 때문에 이 길의 참뜻을 이해하지 못하고 세상에 흔히 있는 보통 일로 생각하고 있었다."
            )
        )
        allContent.add(
            ContentItem(
                korean = " 이제부터는 신이 몸 안에 들아가서\n 마음을 속히 깨닫도록 할 테다 2-36",
                japanese = "これからハ神がたいない入こんで\n心すみやかわけてみせるで",
                english = "korekara wa Kami ga tainai irikonde\nkokoro sumiyaka wakete miseru de",
                commentary = "三十六, 이제부터는 어버이신이 그러한 사람들의 몸속에 들어가서 이 길의 진가를 깨닫도록 할 것이다."
            )
        )
        allContent.add(
            ContentItem(
                korean = " 나날이 모여드는 사람에게 오지 마라\n 해도 도리어 점점 더 불어난다 2-37",
                japanese = "にち／＼によりくる人にことハりを\nゆへばだん／＼なをもまあすで",
                english = "nichinichi ni yorikuru hito ni kotowari o\nyueba dandan naomo masu de",
                commentary = "三十七, 매일 교조를 그리며 오는 사람들에게 오지 마라고 거절을 해도, 오히려 찾아오는 사람들은 점차 불어날 뿐이다. 교조님을 그리며 모여 오는 사람들에게 교조님은 이 길의 가르침을 일러주셨는데, 세상에는 이 가르침을 올바로 이해하는 사람이 적었기 때문에 여러 가지 오해를 사고, 그로 인하여 각계 각층으로부터 자주 방해를 받았다. 교조님의 측근들은, 이래 서는 교조님께 누를 끼칠 뿐이라는 생에서 참배하러 오는 사람들을 못오도록 거절 했으나, 어버이신님의 의도는 이 가르침을 널리 펴려는 것이었으므로, 아무래도 교조님을 그리며 돌아오는 사람들을 제지할 수 없을뿐더러, 도리어 점차 불어나게 될 뿐이라는 뜻이다."
            )
        )
        allContent.add(
            ContentItem(
                korean = " 아무리 많은 사람이 오더라도\n 조금도 염려 말라 신이 맡는다 2-38",
                japanese = "いかほどのをふくの人がきたるとも\nなにもあんぢな神のひきうけ",
                english = "ika hodono oku no hito ga kitaru tomo\nnanimo anjina Kami no hikiuke",
                commentary = ""
            )
        )
        allContent.add(
            ContentItem(
                korean = " 이 세상 창조의 진기한 감로대\n 이것으로 온 세상 안정이 된다 2-39",
                japanese = "めつらしいこのよはじめのかんろたい\nこれがにほんのをさまりとなる",
                english = "mezurashii kono yo hajime no Kanrodai\nkore ga nihon no osamari to naru",
                commentary = "三十九, 이 세상 인간창조의 리를 나타내는 진기한 감로대가 세워져서 감로대근행을 올리게 되면, 그 영덕(靈德)에 의해 어버이신의 참뜻이 온 세상에 널리 알려지게 되고, 그로 인해 온 세상 사람들은 용솟음치며 즐거운 삶을 누리게 된다."
            )
        )
        allContent.add(
            ContentItem(
                korean = " 높은산에 물과 불이 보인다\n 누구의 눈에도 이것이 안 보이는가 2-40",
                japanese = "高山に火と水とがみへてある\nたれがめへにもこれがみへんか",
                english = "takayama ni hii to mizu to ga miete aru\ntare ga me nimo kore ga mien ka",
                commentary = "四十, 윗사람들에게도 어버이신의 수호가 알려지고 있는데도 모두들은 이것을 모르는가.\n물과 불이란 '물과 불은 근본의 수호'라고 가르치신 바와 같이, 어버이신님의 절대 하신 수호를 의미한다. 물이라면 음료수도 물, 비도 물, 몸의 수분도 물, 해일도 물이다. 불이라면 등불도 불, 체온도 불, 화재도 불이다. "
            )
        )
        allContent.add(
            ContentItem(
                korean = " 차츰차츰 어떤 말도 일러주었다\n 확실한 것이 보이고 있으므로 2-41",
                japanese = "たん／＼といかなはなしもといてある\nたしかな事がみゑてあるから",
                english = "dandan to ikana hanashi mo toite aru\ntashikana koto ga miete aru kara",
                commentary = "四十一, 지금까지도 여러 가지로 장래의 일을 일러주었는데, 그것은 어버이신이 무엇이든 장래의 일을 잘 알고 있기 때문이다."
            )
        )
        allContent.add(
            ContentItem(
                korean = " 행복을 누리도록 충분히 수호하고 있다\n 몸에 받게 될 테니 이것을 즐거워하라 2-42",
                japanese = "しやハせをよきよふにとてじうぶんに\nみについてくるこれをたのしめ",
                english = "shiyawase o yoki yoni tote jubun ni\nmi ni tsuite kuru kore o tanoshime",
                commentary = "四十二, 모든 사람들이 행복해지도록 어버이신은 충분히 수호하고 있는 만큼, 마음을 바르게 하여 어버이신의 뜻에 따르는 자는 누구나 행복을 누리게 된다. 이것을 낙으로 삼아 살아가도록."
            )
        )
        allContent.add(
            ContentItem(
                korean = " 무엇이든 허욕 부린 그 다음에는\n 신의 노여움 나타날 거야 2-43",
                japanese = "なにもかもごふよくつくしそのゆへハ\n神のりいふくみへてくるぞや",
                english = "nanimo kamo goyoku tsukushi sono ue wa\nKami no rippuku miete kuru zo ya",
                commentary = "四十三, 무엇이든지 어버이신의 가르침을 지키지 않고 사리 사욕을 부린 자는 반드시 어버이신의 노여움이 나타나 괴로움을 겪게 될 것이다."
            )
        )
        allContent.add(
            ContentItem(
                korean = " 차츰차츰 15일부터 나타나기 시작하리라\n 선과 악이 모두 나타날 것이니 2-44",
                japanese = "たん／＼と十五日よりみゑかける\n善とあくとハみなあらハれる",
                english = "dandan to ju go nichi yori miekakeru\nzen to aku towa mina arawareru",
                commentary = "四十四, 차츰 15일부터 여러 가지 일이  나타날 것이다. 선은 선, 악은 악으로 어버이신이 선명하게 구분해 보이리라."
            )
        )
        allContent.add(
            ContentItem(
                korean = " 이 이야기는 누구예 일이라 말하지 않아\n 나타나거든 모두 납득하라 2-45",
                japanese = "このはなしとこの事ともゆハんてな\nみへてきたればみなとくしんせ",
                english = "kono hanashi doko no koto tomo yuwan de na\nmiete kitareba mina tokushin se",
                commentary = "四十五, 지금까지 말한 이 가르침은 특별히 누구를 두고 한 것이 아니지만 조만간 나타날 것이니 그때가 되면 납득하게 될 것이다.\n이상 3수에 관련된 사료(史料)로는 1872년 야마또 지방 히가시와까이(東若井) 마을에 살던 마쯔오 이찌베에(松尾市兵衛)의 이야기가 전해지고 있다. 이찌베에는 이 길의 신앙에 매우 열성적이어서 당시 뛰어난 제자 가운데 한 사람으로 열심히 가르침을 전하고 있었는데, 매사에 이유가 많고 성질이 강한 사람인지라 막상 자신에 관한 중요한 일에 부딪치면 어버이신님의 가르침을 순직하게 그대로 받아들이지를 못했다. 그로 말미암아 어버이신님으로부터 손질을 받고 있던 그의 장남이 음력 7월 보름 백중날에 출직하고 말았던 것이다."
            )
        )
        allContent.add(
            ContentItem(
                korean = " 높은산에 있는 아는자와 모르는자를\n 구분하는 것도 이것도 기둥이야 2-46",
                japanese = "高山のにほんのものととふぢんと\nわけるもよふもこれもはしらや",
                english = "takayama no nihon no mono to tojin to\nwakeru moyo mo kore mo hashira ya",
                commentary = "四十六, 지도층에 있는 사람 가운데, 어버이신의 뜻을 깨달아 마음이 맑아진 사람과 인간의 지혜와 힘만을 믿고 아직 어버이신의 가르침을 모르는 사람을 구분하는 것도 이 감로대의 영덕으로 하는 것이다."
            )
        )
        allContent.add(
            ContentItem(
                korean = " 모르는자와 아는자를 구분하는 것은\n 물과 불을 넣어서 구분하리라 2-47",
                japanese = "とふじんとにほんのものとハけるのハ\n火と水とをいれてハけるで",
                english = "tojin to nihon no mono to wakeru no wa\nhii to mizu to o irete wakeru de",
                commentary = "四十七, 아직 어버이신의 가르침을 모르는 사람과 어버이신의 뜻을 깨달은 사람을 구분하는 것은, 어버이신의 절대한 힘에 의한 것이다."
            )
        )
        allContent.add(
            ContentItem(
                korean = " 이번에는 문 안에 있는 건물을\n 서둘러서 헐어 버려라 3-1",
                japanese = "このたびハもんのうちよりたちものを\nはやくいそいでとりはらいせよ",
                english = "konotabi wa mon no uchi yori tachimono o\n" +
                        "hayaku isoide toriharai seyo",
                commentary = "一,이번에는 이 길의 발전에 방해가 되는 진터 안의 건물을 철거해 버려라.\n어버이신님은 교조님이 거처할 건물을 짓도록 서두르셨다. 그래서 이해에는 대문과 이에 붙은 거처방 및 창고를 짓기 시작했다. 그러기 위해서는 먼저 새끼줄을 쳐서 건축할 위치를 정해야 했는데, 그 무렵 집터 안에는 방해가 되는 건물이 있었으므로 이를 철거하여 빨리 깨끗이 청소하다록 서두르셨던 것이다."
            )
        )
        allContent.add(
            ContentItem(
                korean = " 빨리 청소를 끝낸 다음에는\n 새로 건축할 준비를 서두르기 부탁이야 3-2",
                japanese = "すきやかにそふぢしたてた事ならば\nなハむねいそぎたのみいるそや",
                english = "sukiyaka ni soji shitateta koto naraba\n" +
                        "nawamune isogi tanomi iru zo ya",
                commentary = "二,빨리 집터 안의 구석구석까지 빠짐없이 청소를 하고 나면 새 집을 지을 준비를 서두르도록.\n이리하여 새로 지은 건물은 1875년에 준공되었는데, 교조님은 이해부터 1883년까지 8년 동안 여기서 가르침을 배푸셨다. 중남의 문간채라 불리는 건물이 바로 이것이다."
            )
        )
        allContent.add(
            ContentItem(
                korean = " 진실로 청소를 한 그 다음에는\n 신한줄기로 마음 용솟음친다 3-3",
                japanese = "しんぢつにそふぢをしたるそのゝちハ\n神一ぢよで心いさむる",
                english = "shinjitsu ni soji o shitaru sono nochi wa\n" +
                        "Kami ichijo de kokoro isamuru",
                commentary = "三,진실로 개끗이 청소를 하고 나면 그 다음에는 오직 이 길만을 항해 나아가게 되어 마음이 저절로 용솟음친다."
            )
        )
        allContent.add(
            ContentItem(
                korean = " 차츰차츰 세상 사람들의 마음 용솟음치면\n 이것으로 온 세상 안정이 된다 3-4",
                japanese = "だん／＼とせかいの心いさむなら\nこれがにほんのをさまりとなる",
                english = "dandan to sekai no kokoro isamu nara\n" +
                        "kore ga nihon no osamari to naru",
                commentary = "四,점차로 이 길이 퍼져 세상 사람들의 마음이 용솟음치게 되면, 어버이신의 뜻을 사람들이 깨닫게 되어 온 세상은 절로 안정이 된다."
            )
        )
        allContent.add(
            ContentItem(
                korean = " 이제까지는 어떤 일도 분간 못했다\n 이제부터 보이는 신기한 징조가 3-5",
                japanese = "いまゝでハなによの事もハかりない\nこれからみゑるふしぎあいづが",
                english = "imamade wa nani yono koto mo wakari nai\n" +
                        "korekara mieru fushigi aizu ga",
                commentary = "五,이제까지는 어떤 일에 대해서도 어버이의 마음을 깨닫지 못했으나, 앞으로는 신기한 징조가 나타날 것이니 그것으로 어버이신이 뜻하는 바를 잘 납득해야 한다."
            )
        )
        allContent.add(
            ContentItem(
                korean = " 아니 오는 자에게 무리로 오라고는 않는다\n 따라온다면 언제까지나 좋아 3-6",
                japanese = "こんものにむりにこいとハゆうでなし\nつきくるならばいつまでもよし",
                english = "kon mono ni muri ni koi towa yu de nashi\n" +
                        "tsukikuru naraba itsu made mo yoshi",
                commentary = "六,신앙은 자유이므로 이 길의 가르침에 귀의하기를 싫어하는 사람에게 무리로 신앙하라고는 결코 말하지 않는다. 그러나 교조님을 흠모하여 따라오는 사람에게는 언제까지나 좋은 길이 열린다는 뜻이다."
            )
        )
        allContent.add(
            ContentItem(
                korean = " 이제부터는 물에 비유해서 말한다\n 맑음과 탁함으로 깨닫도록 하라 3-7",
                japanese = "これからハ水にたとゑてはなしする\nすむとにごりでさとりとるなり",
                english = "korekara wa mizu ni tatoete hanashi suru\n" +
                        "sumu to nigori de satori toru nari",
                commentary = "七,이제부터는 물을 예로 들어 일러주겠는데, 물이 맑다는 것은 사람들의 마음에 티끌이 없이 깨끗하다는 뜻이며, 탁하다는 것은 사람들의 마음에 티끌이 쌓여 있다는 뜻이므로, 이에 따라 각자의 마음을 반성해야 한다."
            )
        )
        allContent.add(
            ContentItem(
                korean = " 진실로 신이 마음 서두르는 것은\n 중심의 기둥을 빨리 세우고 싶다 3-8",
                japanese = "しんぢつに神の心のせきこみわ\nしんのはしらをはやくいれたい",
                english = "shinjitsu ni Kami no kokoro no sekikomi wa\n" +
                        "shin no hashira o hayaku iretai",
                commentary = "八,어버이신이 진실로 서두르고 있는 것은 하루라도 빨리 중심을 정하고 싶은 것이다.\n중심의 기둥이란 원래 건축용어인데, 무슨 일에서나 가장 중심이 되는 것을 뜻한다. 본교에서는 근행인 경우 김로대를 가르키고, 사람인 경우 이 길의 중심이 되는 분을 가리키며, 마음인 경우에는 중심되는 생각을 말한다. 즉, 인간창조의 리를 나타내며, 구제한줄기의 신앙의 중심 지점을 나타내는 감로대를 '온 세상의 중심의 기둥이야'라고 말씀하셨고.(第八호 八十五 참조)또 본교의 중심되는 분을 '안을 다스릴 중심의 기둥'이라 말씀하셨다.(第三호 五十六 참조)따라서 이 노래는 감로대근행의 완성을 목표로 앞에서 말한 두가지를 건설, 확립할 것을 서두르시는 노래이다."
            )
        )
        allContent.add(
            ContentItem(
                korean = " 이 기둥 빨리 세우려고 생각하지만\n 탁한 물이라 장소를 모르겠다 3-9",
                japanese = "このはしらはやくいれよとをもへども\nにごりの水でところわからん",
                english = "kono hashira hayaku ireyo to omoedomo\n" +
                        "nigori no mizu de tokoro wakaran",
                commentary = "九,이 기둥을 빨리 세우려고 서두르게 있으나, 모든 사람들의 마음이 흐리기 때문에 세 울 수가 없다.\n당시 감로대의 모형은 만들어져 있었으나, 이를 세워야 할 터전은 아직 정해지지 않았다. 그리고 어버이신님은 나까야마(中山)가의 후계자로서, 앞으로 이 길의 진주(眞柱)가 될 사람을 이찌노모또 마을에 사는 가지모또가의 3남인 신노스께(真之亮)님으로 정하시고, 빨리 집터에 정주시키려 하셨으나, 측근에서는 이러한 어버이신님의 뜻을 깨닫지 못하고 각자 제멋대로 생각하기 때문에 사람들의 의견이 일치되지 않았는데, 이를 두고 하신 말씀이다."
            )
        )
        allContent.add(
            ContentItem(
                korean = " 이 물을 빨리 맑힐 준비\n 숯과 모래로 걸러서 맑혀라 3-10",
                japanese = "この水をはやくすまするもよふだて\nすいのとすなにかけてすませよ",
                english = "kono mizu o hayaku sumasuru moyodate\n" +
                        "suino to suna ni kakete sumase yo",
                commentary = ""
            )
        )
        allContent.add(
            ContentItem(
                korean = " 이 숯은 다른 것이라 생각 말라\n 가슴속과 입이 모래요 숯이라 3-11",
                japanese = "このすいのどこにあるやとをもうなよ\nむねとくちとがすなとすいのや",
                english = "kono suino doko ni aru ya to omouna yo\n" +
                        "mune to kuchi to ga suna to suino ya",
                commentary = "十一,이 숯은 다른 것이 아니다. 각자의 마음과 입이 솣이다. 즉, 깨닫고 개ㄱ우침으로써 마음을 작 닦아 어버이신의 뜻에 순응해야 한다.\n가슴속과 입이란 깨닫고 깨우친다는 뜻."
            )
        )
        allContent.add(
            ContentItem(
                korean = " 이 이야기를 속히 깨닫게 되면\n 당장에 세울 중심의 기둥 3-12",
                japanese = "このはなしすみやかさとりついたなら\nそのまゝいれるしんのはしらを",
                english = "kono hanashi sumiyaka satori tsuita nara\n" +
                        "sono mama ireru shin no hashira o",
                commentary = "十二,사람들이 이러한 어버이신의 말을 듣고 빨리 깨달아, 어버이신의 의도에 마음을 모은다면 당장이라도 중심의 기둥을 세울 것이다."
            )
        )
        allContent.add(
            ContentItem(
                korean = " 기둥만 단단히 세우게 되면\n 이 세상 확실히 안정이 된다 3-13",
                japanese = "はしらさいしいかりいれた事ならば\nこのよたしかにをさまりがつく",
                english = "hashira sai shikkari ireta koto naraba\n" +
                        "kono yo tashikani osamari ga tsuku",
                commentary = "十三,이 기둥만 터전에 단단히 세우게 되면, 그것으로 이 길의 기초가 확립되고, 가르침은 점차로 널리 퍼져 세상은 틀림없이 안정이 된다."
            )
        )
        allContent.add(
            ContentItem(
                korean = " 이 이야기는 깨달아야만 하는 만큼\n 이것을 깨달으면 증거가 나타날 거야 3-14",
                japanese = "このはなしさとりばかりであるほどに\nこれさとりたらしよこだめしや",
                english = "kono hanashi satori bakari de aru hodoni\n" +
                        "kore satoritara shoko dameshi ya",
                commentary = "十四,이 길은 깨닫고 깨우치는 길이므로 비유해서 하는 말을 잘 깨달아, 진주를 정하고 감로대를 세우며 인원을 갖추어서 감로대근행을 하게 된다면, 그것으로 세상은 확실히 안정이 된다는 어버이신의 의도가 실증될 것이다."
            )
        )
        allContent.add(
            ContentItem(
                korean = " 이 세상 인간을 창조한 으뜸인 신\n 누구도 아는 자는 없으리라 3-15",
                japanese = "このよふのにんけんはじめもとの神\nたれもしりたるものハあるまい",
                english = "kono yo no ningen hajime moto no Kami\n" +
                        "tare mo shiritaru mono wa arumai",
                commentary = ""
            )
        )
        allContent.add(
            ContentItem(
                korean = " 진흙바다 속에서 수호를 가르치\n 그것이 차츰차츰 번성해진 거야 3-16",
                japanese = "どろうみのなかよりしゆごふをしへかけ\nそれがたん／＼さかんなるぞや",
                english = "doroumi no naka yori shugo oshiekake\n" +
                        "sore ga dandan sakan naru zo ya",
                commentary = "十六,태초에 月日 어버이신님이 진흙바다 가운데서 도구들을 끌어모아 인간을 창조하신 이래 영묘한 수호를 베풀어 주심으로써 마침내 오늘날과 같은 인간으로 성장한 것이다.(第六호 二十九～五十一 참조)"
            )
        )
        allContent.add(
            ContentItem(
                korean = " 이번에 구제한줄기를 가르치는\n 이것도 없던 일 시작하는 거야 3-17",
                japanese = "このたびハたすけ一ぢよをしゑるも\nこれもない事はしめかけるで",
                english = "konotabi wa tasuke ichijo oshieru mo\n" +
                        "kore mo nai koto hajime kakeru de",
                commentary = ""
            )
        )
        allContent.add(
            ContentItem(
                korean = " 이제까지 없던 일을 시작하는 것은\n 근본을 창조한 신이기 때문에 3-18",
                japanese = "いまゝでにない事はじめかけるのわ\nもとこしらゑた神であるから",
                english = "imamade ni nai koto hajime kakeru no wa\n" +
                        "moto koshiraeta Kami de aru kara",
                commentary = ""
            )
        )
        allContent.add(
            ContentItem(
                korean = " 나날이 신의 이야기가 첩첩이\n 쌓여 있어도 말하려야 말할 수 없다 3-19",
                japanese = "にち／＼に神のはなしがやま／＼と\nつかゑてあれどとくにとかれん",
                english = "nichinichi ni Kami no hanashi ga yamayama to\n" +
                        "tsukaete aredo toku ni tokaren",
                commentary = ""
            )
        )
        allContent.add(
            ContentItem(
                korean = " 무엇이든 말 못할 것은 없겠지만\n 마음을 맑혀서 듣는 자가 없다 3-20",
                japanese = "なにゝてもとかれん事ハないけれど\n心すましてきくものハない",
                english = "nani nitemo tokaren koto wa nai keredo\n" +
                        "kokoro sumashite kiku mono wa nai",
                commentary = ""
            )
        )
        allContent.add(
            ContentItem(
                korean = " 빨리 마음 맑혀서 듣는다면\n 만가지 이야기 모두 일러준다 3-21",
                japanese = "すみやかに心すましてきくならば\nよろづのはなしみなときゝかす",
                english = "sumiyaka ni kokoro sumashite kiku naraba\n" +
                        "yorozu no hanashi mina tokikikasu",
                commentary = ""
            )
        )
        allContent.add(
            ContentItem(
                korean = " 이 세상에 확실한 징험이 나타난다\n 이것만은 틀릴이 없다고 생각하라 3-22",
                japanese = "このよふのたしかためしかかけてある\nこれにまちがいないとをもゑよ",
                english = "kono yo no tashika tameshi ga kakete aru\n" +
                        "kore ni machigai nai to omoe yo",
                commentary = ""
            )
        )
        allContent.add(
            ContentItem(
                korean = " 이 징험이 빨리 나타나기만 하면\n 어떤 이야기도 모두 참인 거야 3-23",
                japanese = "このためしすみやかみゑた事ならば\nいかなはなしもみなまことやで",
                english = "kono tameshi sumiyaka mieta koto naraba\n" +
                        "ikana hanashi mo mina makoto ya de",
                commentary = "二十三,이 징험이 실제로 빨리 나타나기만 하면 어버이신의 이야기는 어떤 것도 틀림이 없음을 알게 될 것이다."
            )
        )
        allContent.add(
            ContentItem(
                korean = " 무엇이든 어떤 이야기도 일러줄 테니\n 무슨 말을 해도 거짓이라 생각 말라 3-24",
                japanese = "なにもかもいかなはなしもとくほどに\nなにをゆうてもうそとをもうな",
                english = "nanimo kamo ikana hanashi mo toku hodoni\n" +
                        "nani o yutemo uso to omouna",
                commentary = "二十四,지금까지 사람이 모르는, 사람의 능력으로는 생각지도 못하는 것을 깨우쳐 주려고 하는데, 어떤 것이라도 어버이신이 하는 말은 틀림이 없다고 생각해야 한다."
            )
        )
        allContent.add(
            ContentItem(
                korean = " 눈에 안 보이는 신이 하는 말 하는 일\n 무엇을 하는지 좀처럼 모르겠지 3-25",
                japanese = "めへにめん神のゆう事なす事わ\nなにをするとも一寸にしれまい",
                english = "me ni men Kami no yu koto nasu koto wa\n" +
                        "nani o suru tomo chotto ni shiremai",
                commentary = "二十五,사람의 눈에 보이지 않는 어버이신이 하는 말, 하는 일이므로 좀처럼 깨달을 수도 없고, 또 앞으로 어떤 일을 할지 전혀 모를 것이다."
            )
        )
        allContent.add(
            ContentItem(
                korean = " 이내 곧 나타나는 이야기인 만큼\n 이것이 확실한 증거인 거야 3-26",
                japanese = "はや／＼とみへるはなしであるほどに\nこれがたしかなしよこなるぞや",
                english = "hayabaya to mieru hanashi de aru hodoni\n" +
                        "kore ga tashikana shoko naru zo ya",
                commentary = "二十六,어버이신이 일러준 것은 이내 나타나기 때문에, 이것이야말로 어버이신이 하는 말은 틀림이 없다는 좋은 증거가 될 것이다."
            )
        )
        allContent.add(
            ContentItem(
                korean = " 이것을 보고 무엇을 듣든지 즐거워하라\n 어떤 이야기도 모두 이와 같으니 3-27",
                japanese = "これをみてなにをきいてもたのしめよ\nいかなはなしもみなこのどふり",
                english = "kore o mite nani o kiitemo tanoshime yo\n" +
                        "ikana hanashi mo mina kono dori",
                commentary = "二十七,이렇게 나타나는 증거를 보고 장래를 낙으로 삼아라, 어버이신의 가르침은 모두 이와 같으리라."
            )
        )
        allContent.add(
            ContentItem(
                korean = " 남의 것을 빌리면 이자가 붙는다\n 빨리 갚고 치사하도록 하라 3-28",
                japanese = "人のものかりたるならばりかいるで\nはやくへんさいれゑをゆうなり",
                english = "hito no mono karitaru naraba ri ga iru de\n" +
                        "hayaku hensai re o yu nari",
                commentary = ""
            )
        )
        allContent.add(
            ContentItem(
                korean = " 아이가 밤에 운다는 생각은 틀린 거야\n 아이가 우는 것이 아니라 신의 타이름이니 3-29",
                japanese = "子のよなきをもふ心ハちがうでな\nこがなくでな神のくときや",
                english = "ko no yonaki omou kokoro wa chigau de na\n" +
                        "ko ga nakude nai Kami no kudoki ya",
                commentary = ""
            )
        )
        allContent.add(
            ContentItem(
                korean = " 빨리 신이 알려 주는 것이니\n 무슨 일이든 단단히 분간하라 3-30",
                japanese = "はや／＼と神がしらしてやるほどに\nいかな事でもしかときゝわけ",
                english = "hayabaya to Kami ga shirashite yaru hodoni\n" +
                        "ikana koto demo shikato kikiwake",
                commentary = ""
            )
        )
        allContent.add(
            ContentItem(
                korean = " 부모들의 마음 틀리지 않도록\n 어서 생각해 보는 것이 좋을 거야 3-31",
                japanese = "をや／＼の心ちがいのないよふに\nはやくしやんをするがよいぞや",
                english = "oyaoya no kokoro chigai no nai yoni\n" +
                        "hayaku shiyan o suru ga yoi zo ya",
                commentary = ""
            )
        )
        allContent.add(
            ContentItem(
                korean = " 진실로 남을 구제할 마음이라면\n 신의 타이름은 아무것도 없는 거야 3-32",
                japanese = "しんぢつに人をたすける心なら\n神のくときハなにもないぞや",
                english = "shinjitsu ni hito o tasukeru kokoro nara\n" +
                        "Kami no kudoki wa nanimo nai zo ya",
                commentary = ""
            )
        )
        allContent.add(
            ContentItem(
                korean = " 각자가 지금만 좋으면 그만이라고\n 생각하는 마음은 모두 틀리는 거야 3-33",
                japanese = "めへ／＼にいまさいよくばよき事と\nをもふ心ハみなちがうでな",
                english = "meme ni imasai yokuba yoki koto to\n" +
                        "omou kokoro wa mina chigau de na",
                commentary = ""
            )
        )
        allContent.add(
            ContentItem(
                korean = " 처음부터 아무리 큰길을 걷고 있어도\n 장래 있을 좁은 길 보지 못하니 3-34",
                japanese = "てがけからいかなをふみちとふりても\nすゑのほそみちみゑてないから",
                english = "degake kara ikana omichi toritemo\n" +
                        "sue ni hosomichi miete nai kara",
                commentary = "三十四,누구든지 처음부터 편안한 길을 걷고 싶어하는 것은 앞으로 다가올 고생의 길을 모르기 때문이다."
            )
        )
        allContent.add(
            ContentItem(
                korean = " 인간안 어리석기 때문에\n 앞으로 닥칠 길을 전연 모른다 3-35",
                japanese = "にんけんハあざないものであるからに\nすゑのみちすじさらにわからん",
                english = "ningen wa azanai mono de aru karani\n" +
                        "sue no michisuji sarani wakaran",
                commentary = "三十五,인간은 어리석기 때문에 장래 어떤 길을 걸을지 전혀 모른다."
            )
        )
        allContent.add(
            ContentItem(
                korean = " 지금의 일을 무엇이라 말하지 마라\n 장래에 한길이 나타나리라 3-36",
                japanese = "いまの事なにもゆうでハないほどに\nさきのをふくハんみちがみへるで",
                english = "ima no koto nanimo yu dewa nai hodoni\n" +
                        "saki no okwan michi ga mieru de",
                commentary = ""
            )
        )
        allContent.add(
            ContentItem(
                korean = " 지금 어떤 길을 걸더도 탄식하지 마라\n 장래 있을 본길을 낙으로 삼아라 3-37",
                japanese = "いまのみちいかなみちでもなけくなよ\nさきのほんみちたのしゆでいよ",
                english = "ima no michi ikana michi demo nagekuna yo\n" +
                        "saki no honmichi tanoshun de iyo",
                commentary = "三十六, 三十七,어버이신은 현재 일만을 가리켜 말하는 것이 아니므로, 지금 걷고 있는 길이 아무리 괴롭더라도 절대로 불평을 하거나 불만을 품어서는 안된다. 장래에는 반드시 큰길이 나타날 것이니 그것을 낙으로 삼고 즐겁게 걸어가야 한다."
            )
        )
        allContent.add(
            ContentItem(
                korean = " 진실로 구제한줄기 마음일 것 같으면\n 아무 말 아니해도 확실히 받아들인다 3-38",
                japanese = "しんぢつにたすけ一ぢよの心なら\nなにゆハいでもしかとうけとる",
                english = "shinjitsu ni tasuke ichijo no kokoro nara\n" +
                        "nani yuwai demo shikato uketoru",
                commentary = "三十八,어떻든 구제하려는 마음만 있다면, 입으로 말하지 않더라도 어버이신은 그 마음을 받아들여 반드시 리에 맞는 수호를 한다."
            )
        )
        allContent.add(
            ContentItem(
                korean = " 입으로만 아첨하는 것 쓸데없어\n 마음 가운데 정성만 있다면 3-39",
                japanese = "口さきのついしよはかりハいらんもの\nしんの心にまことあるなら",
                english = "kuchi saki no tsuisho bakari wa iran mono\n" +
                        "shin no kokoro ni makoto aru nara",
                commentary = ""
            )
        )
        allContent.add(
            ContentItem(
                korean = " 차츰차츰 무엇이든 이 세상은\n 신의 몸이야 생각해 보라 3-40",
                japanese = "たん／＼となに事にてもこのよふわ\n神のからだやしやんしてみよ",
                english = "dandan to nani goto nitemo kono yo wa\n" +
                        "Kami no karada ya shiyan shite miyo",
                commentary = ""
            )
        )
        allContent.add(
            ContentItem(
                korean = " 인간은 모두가 신의 대물이야\n 무엇으로 알고 쓰고 있는가 3-41",
                japanese = "にんけんハみな／＼神のかしものや\nなんとをもふてつこているやら",
                english = "ningen wa minamina Kami no kashimono ya\n" +
                        "nanto omote tsukote iru yara",
                commentary = "四十, 四十一,이 세상 만물은 모두 어버이신님이 창조하신 것이며, 전우주는 어버이신님의 몸이다. 따라서 인간도 자신의 능력으로 된 것이 아니다. 어버이신님이 만드신 것을 어버이신님으로부터 빌려 받아, 어버이신님의 품속인 이 세계에서 어버이신님의 수호에 의해 살아가고 있는 것이다."
            )
        )
        allContent.add(
            ContentItem(
                korean = " 올해는 진기한 일을 시작해서\n 이제까지 모르던 일을 할 거야 3-42",
                japanese = "ことしにハめつらし事をはじめかけ\nいまゝでしらぬ事をするぞや",
                english = "kotoshi niwa mezurashi koto o hajime kake\n" +
                        "imamade shiranu koto o suru zo ya",
                commentary = "四十二,교조님은 1874년 음력 5월에 신악탈을 가지러 삼마이뎅(三昧田)에 가셨다. 그리고 일반 사람들 뿐만 아니라 높은산에도 포교를 하여 이 길의 가르침을 알리시려고, 이해 10월에 나까따(仲田), 마쯔오 두 사람을 오야마또(大和)신사에 보내셨다. 그 결과 신직(神職)과 관헌의 주의를 끌게 되어, 음력 11월 15일에는 야마무라고뗑(山村御殿)에 불려 가신 것을 비롯하여, 그 뒤에도 자주 핍박을 받으셨는데, 어버이신님은 이것을 오히려 이 길을 넗혀 나가는 하나의 방법으로 여기셨다. 또 붉은 옷을 입으신 것도 이해부터였다.(第五호 五十六, 五十七의 주석 참조)"
            )
        )
        allContent.add(
            ContentItem(
                korean = " 지금까지는 무슨 일도 세상 보통 일로 여겼으나\n 이제부터는 진심으로 납득하리라 3-43",
                japanese = "いまゝでハなによの事もせかいなみ\nこれからわかるむねのうちより",
                english = "imamade wa nani yono koto mo sekainami\n" +
                        "korekara wakaru mune no uchi yori",
                commentary = "四十三,지금까지는 어떤 일에 대해서도 이러한 정신을 몰랐으나, 이제부터는 충분히 일러줄 것이므로 확실히 알게 될 것이다."
            )
        )
        allContent.add(
            ContentItem(
                korean = " 이번에 구제한줄기 시작하는 것도\n 내 몸으로 체험을 하고 나서 3-44",
                japanese = "このたびハたすけ一ちよにかゝるのも\nわがみのためしかゝりたるうゑ",
                english = "konotabi wa tasuke ichijo ni kakaru no mo\n" +
                        "waga mi no tameshi kakaritaru ue",
                commentary = "四十四,이번에 구제한줄기의 길을 가르침에 있어 몸소 겪으신 체험을 바탕으로 하여 사람들을 구제한 것을 말씀하신 것이다. 순산허락도 몸소 순산시험을 체험하신 다음 내려 주셨고, 또 히가시와까이 마을에 사는 마쯔오 이쩨베에 대에 구제하러 가셨을 때도 단식한 지 30여일이 지난 무렵으로서, 먹으려야 먹을 수 없는 질별의 괴로움을 체험하고 나서 가셨던 것이다."
            )
        )
        allContent.add(
            ContentItem(
                korean = " 구제라 해서 절하고 비는 것이 아니오\n 물어 보고 하는 것도 아니지만 3-45",
                japanese = "たすけでもをかみきとふでいくてなし\nうかがいたてゝいくでなけれど",
                english = "tasuke demo ogami kito de ikude nashi\n" +
                        "ukagai tatete iku de nakeredo",
                commentary = "四十五,사람을 구제하는 것도 지금까지처럼 단순히 절을 하거나, 기도를 하거나, 물어 보거나 해서 구제하는 그런 것이 아니다."
            )
        )
        allContent.add(
            ContentItem(
                korean = " 여기서 만가지 일을 일러준다\n 신한줄기로 가슴속에서 3-46",
                japanese = "このところよろつの事をときゝかす\n神いちじよでむねのうちより",
                english = "kono tokoro yorozu no koto o tokikikasu\n" +
                        "Kami ichijo de mune no uchi yori",
                commentary = ""
            )
        )
        allContent.add(
            ContentItem(
                korean = " 잘 깨닫도록 가슴속 깊이 생각하라\n 남을 구제하면 제 몸 구제받는다 3-47",
                japanese = "わかるよふむねのうちよりしやんせよ\n人たすけたらわがみたすかる",
                english = "wakaru yo mune no uchi yori shiyan seyo\n" +
                        "hito tasuketara waga mi tasukaru",
                commentary = "四十六, 四十七,이 터전에서는 인간창 이래의 어버이신의 수호를 자세히 일러주고 있는데, 이야기를 깊이 잘 깨달아서 신한줄기의 마음을 작정하고 구제한줄기의 길로 나아가라. 남을 구제해야만 그 리에 의해서 제 몸이 구제받게 된다."
            )
        )
        allContent.add(
            ContentItem(
                korean = " 높은산은 온 세상 사람들을 생각대로\n 제멋대로 하지만 앞을 보지 못한다 3-48",
                japanese = "高山ハせかい一れつをもうよふ\nまゝにすれともさきハみゑんで",
                english = "takayama wa sekai ichiretsu omou yo\n" +
                        "mamani suredomo saki wa mien de",
                commentary = "四十八,어느 사회에서나 상류층 사람들은 자신만을 위해 제멋대로 행동하고 있지만, 장래에 닥쳐 올 일을 모르는 것이다."
            )
        )
        allContent.add(
            ContentItem(
                korean = " 차츰차츰 많이 모아 둔 이 나무들\n 용재가 될 것은 없는 거야 3-49",
                japanese = "だん／＼とをふくよせたるこのたちき\nよふほくになるものハないぞや",
                english = "dandan to oku yosetaru kono tachiki\n" +
                        "yoboku ni naru mono wa nai zo ya",
                commentary = "四十九,용재(用材)란 어버이신님의 일에 종사하는 사람을 뜻한다."
            )
        )
        allContent.add(
            ContentItem(
                korean = " 어떤 나무도 많이 모으기는 했으나\n 비뚤어지고 구부저려 이것 마땅찮다 3-50",
                japanese = "いかなきもをふくよせてハあるけれど\nいがみかゞみハこれわかなハん",
                english = "ikana ki mo oku yosete wa aru keredo\n" +
                        "igami kagami wa kore wa kanawan",
                commentary = "五十,여러 종류의 나무를 많이 모기는 했지만, 비뚤어지고 구부러진 나무는 아무래도 쓰기에 마땅치 않다\n 비뚤어지고 구부러져란 나무에 비유하여 마음이 비뚤어져 순직하지 못한 사람을 가리킨 말이다."
            )
        )
        allContent.add(
            ContentItem(
                korean = " 세상 사람들의 가슴속에 중심의 기둥\n 신은 서두르고 있다 빨리 보이고 싶어서 3-51",
                japanese = "せかいぢうむねのうちよりしんばしら\n神のせきこみはやくみせたい",
                english = "sekaiju mune no uchi yori Shinbashira\n" +
                        "Kami no sekikomi hayaku misetai",
                commentary = "五十一,온 세상 사람들이 마음을 바꾸어 모두가 맑고 깨끗한 마음이 된다면, 이 세상은 어버이신이 바라는 즐거운 삶의 세계가 되고, 터전에는 감로대가 서게 된다. 어버이신은 일각이라도 빨리 사람들에게 이것을 보이려고 서두르고 있다."
            )
        )
        allContent.add(
            ContentItem(
                korean = " 세상 사람들의 가슴속을 맑히는 이 청소\n 신이 빗자루야 단단히 두고 보라 3-52",
                japanese = "せかいぢうむねのうちよりこのそふぢ\n神がほふけやしかとみでいよ",
                english = "sekaiju mune no uchi yori kono soji\n" +
                        "Kami ga hoke ya shikato mite iyo",
                commentary = "五十二,그러기 위해서는 세상 사람들의 마음을 청소하지 않으면 안된다. 그래서 어버이신은 스스로 빗자루가 되어 청소하ㅐ 테니, 나타나는 리를 단단히 주의해서 보도록 하라."
            )
        )
        allContent.add(
            ContentItem(
                korean = " 이제부터는 신이 이 세상에 나타나서\n 산에까지 청소할 거야 3-53",
                japanese = "これからハ神がをもていあらわれて\n山いかゝりてそふちするぞや",
                english = "korekara wa Kami ga omote i arawarete\n" +
                        "yama i kakarite soji suru zo ya",
                commentary = "五十三,앞으로는 어버이신이 이 세상에 나타나서 상류층 사람들의 마음도 청소를 한다."
            )
        )
        allContent.add(
            ContentItem(
                korean = " 모든 사람들을 신이 청소하게 되면\n 마음 용솟음쳐 즐거움이 넘칠 거야 3-54",
                japanese = "いちれつに神がそうちをするならば\n心いさんてよふきつくめや",
                english = "ichiretsu ni Kami ga soji o suru naraba\n" +
                        "kokoro isande yokizukume ya",
                commentary = "五十四,온 세상 사람들의 마음을 어버이신이 청소하여 깨끗이 맑힌다면 이 세상은 평화롭고 즐거운 세계가 되고, 사람들의 마음도 용솟음치며 즐겁게 살게 된다."
            )
        )
        allContent.add(
            ContentItem(
                korean = " 무엇이든 신이 맡았으니까\n 무슨 일이든 자유자재로 3-55",
                japanese = "なにもかも神がひきうけするからハ\nどんな事でもぢうよぢさを",
                english = "nanimo kamo Kami ga hikiuke suru kara wa\n" +
                        "donna koto demo juyojizai o",
                commentary = ""
            )
        )
        allContent.add(
            ContentItem(
                korean = " 이번에는 안을 다스릴 중심의 기둥\n 빨리 세우고 싶다 물을 맑혀서 3-56",
                japanese = "このたびハうちをふさめるしんばしら\nはやくいれたい水をすまして",
                english = "konotabi wa uchi o osameru Shinbashira\n" +
                        "hayaku iretai mizu o sumashite",
                commentary = "五十六,이번에는 사람들의 마음을 맑혀서 나까야마가의 후계자이며 이 길의 중심인 진주가 될 신노스께를 빨리 맞아들이고 싶다.\n 안을 다스릴 중심의 기둥 이 노래를 집필한 1874년에 신노스께님은 9세였는데, 출생 전부터 진주가 될 신노스께라 불리어 장래 나까아먀가의 후계자로, 또 이 길의 중심이 될 진주로 정해졌 있었다. 그래서 신노스께님은 일찍부터 집터에 와 있었는데, 어버이신님은 하루라도 빨리 집터에 정주시키려고 서두르고 계셨다. 본교에서는 이 길의 중심이 되는 사람을 진주님이라고 한다.(第三호 八의 주석 참조)"
            )
        )
        allContent.add(
            ContentItem(
                korean = " 높은산의 중심의 기둥은 모르는자야\n 이것이 첫째가는 신의 노여움 3-57",
                japanese = "高山のしんのはしらハとふじんや\nこれが大一神のりいふく",
                english = "takayama no shin no hashira wa tojin ya\n" +
                        "kore ga daiichi Kami no rippuku",
                commentary = "五十七,지도층에 있는 사람들은 아직 어버이신의 뜻을 깨닫지 못하고 인간생각에만 흐르고 있는데, 이것이 무엇보다 어버이신을 노엽게 하고 있다.\n 이것은 당시의 관헌이 종교를 이해하려고 하지 않는 태도에 대해서 하신 말씀이다. 높은 산의 중심의 기둥이란 상류층의 중심되는 인물을 말한다. 모르는자란 아직 어버이신님의 뜻을 모르는 사람(第三호 八, 九의 주석 참조)"
            )
        )
        allContent.add(
            ContentItem(
                korean = " 윗사람들은 점점 더 세상을 멋대로 한다\n 신의 섭섭함 어떻게 생각하는가 3-58",
                japanese = "上たるハだん／＼せかいまゝにする\n神のざんねんなんとをもうぞ",
                english = "kami taru wa dandan sekai mamani suru\n" +
                        "Kami no zannen nanto omou zo",
                commentary = "윗사람들 가운데는 세상 모든 일이 인간의 힘으로 좌우되는 줄로 생각하고 제멋대로 행동하는 자가 있는데, 이는 이 세상을 창조한 어버이신의 깊은 마음을 모르기 때문으로 심히 유감스러운 일이다."
            )
        )
        allContent.add(
            ContentItem(
                korean = " 지금까지는 무슨 말을 해도 나타나지 않았다\n 벌써 이제는 시기가 왔다 3-59",
                japanese = "いまゝでハなにをゆうてもみへてない\nもふこのたびハせへつうがきた",
                english = "imamade wa nani o yutemo miete nai\n" +
                        "mo konotabi wa setsu ga kita",
                commentary = "五十九,지금까지 여러 가지로 일러주었지만 그것이 아직은 실제로 나타나지 않았다. 그러나 이제는 누구나 알 수 있는 시기가 다가왔다."
            )
        )
        allContent.add(
            ContentItem(
                korean = " 이제부터는 즐거운 근행을 또 시작한다\n 무슨 일인지 좀처럼 모르겠지 3-60",
                japanese = "これからハよふきづとめにまたかゝる\nなんの事やら一寸にしれまい",
                english = "korekara wa Yoki-zutome ni mata kakaru\n" +
                        "nanno koto yara choto ni shiremai",
                commentary = "六十,이제부터는 즐거운근행을 다시 서두를 것이데, 그것은 무슨 까닭인지 좀처럼 모를 것이다."
            )
        )
        allContent.add(
            ContentItem(
                korean = " 지금까지도 신의 뜻을 되풀이 이야기하고\n 일러주어도 무슨 말일지 3-61",
                japanese = "今までもしりてはなしてはなしとも\nといてあれどもなんの事やら",
                english = "imamade mo shirite hanashite hanashi tomo\n" +
                        "toite aredomo nanno koto yara",
                commentary = ""
            )
        )
        allContent.add(
            ContentItem(
                korean = " 이제까지는 어떤 말을 일러주어도\n 날이 오지 않아서 나타나지 않은 거야 3-62",
                japanese = "これまでハいかなはなしをといたとて\nひがきたらんでみへてないぞや",
                english = "koremade wa ikana hanashi o toita tote\n" +
                        "hi ga kitarande miete nai zo ya",
                commentary = ""
            )
        )
        allContent.add(
            ContentItem(
                korean = " 이제부터는 이미 시기가 왔으므로\n 말하면 그대로 나타나는 거야 3-63",
                japanese = "これからわもふせへつうがきたるから\nゆへばそのまゝみへてくるぞや",
                english = "korekara wa mo setsu ga kitaru kara\n" +
                        "yueba sono mama miete kuru zo ya",
                commentary = ""
            )
        )
        allContent.add(
            ContentItem(
                korean = " 단단히 들어라 36 25의 저물 무렵에\n 가슴속의 청소를 신이 하는 거야 3-64",
                japanese = "しかときけ三六二五のくれやいに\nむねのそふぢを神がするぞや",
                english = "shikato kike san roku ni go no kureyai ni\n" +
                        "mune no soji o Kami ga suru zo ya",
                commentary = "六十四,이것은 입교 후 36년이 되는 어느 달 25일의 저녁나절에 집터 청소를 하러 올 사람이 있음을 말씀하신 것이다. 그 무렵에는 밖에서 집터 총소를 하러 올 사람이 별로 없었는데, 이날 저녁나절에 닷다에 사는 요스께(與助)라는 사람의 부인 또요와 감베에라는 사람의 모친후사가 참배하러 와서, 집터 안이 지저분한 것을 보고 내일이 26일 제일인데 이렇게 지저분해서야 신님께 죄송한 일이라며 깨끗이 청소를 하고 돌아갔다. 그 전날 밤, 또요는 가슴이 아파 괴로워하던 중, 전에도 한번 한 적이 있는 터전의 신님께 참배를 하여 도움을 받아야겠다고 결심을 하자, 가슴의 통증이 씼은 듯 나았다. 그래서 이날 사례참배를 하러 왔던 것이다. 이것은 일례를 들어 말씀하신 것인데, 앞으로는 이처럼 사람들이 마음을 청소하도록 어버이신님이 손질을 하신다는 뜻이다."
            )
        )
        allContent.add(
            ContentItem(
                korean = " 생각하라 아무리 맑은 물이라 해도\n 진흙을 넣으면 탁해지느니라 3-65",
                japanese = "しやんせよなんぼすんだる水やとて　とろをいれたらにごる事なり",
                english = "shiyan seyo nanbo sundaru mizu ya tote\n" +
                        "doro o iretara nigoru koto nari",
                commentary = ""
            )
        )
        allContent.add(
            ContentItem(
                korean = " 탁한 물을 빨리 맑히지 않으면\n 중심의 기둥을 세울 수 없다 3-66",
                japanese = "にごり水はやくすまさん事にてわ　しんのはしらのいれよふがない",
                english = "nigori mizu hayaku sumasan koto nitewa\n" +
                        "shin no hashira no ireyo ga nai",
                commentary = ""
            )
        )
        allContent.add(
            ContentItem(
                korean = " 기둥만 빨리 세우게 되면\n 영원히 확실하게 안정이 된다 3-67",
                japanese = "はしらさいはやくいれたる事ならば\nまつたいしかとをさまりがつく",
                english = "hashira sai hayaku iretaru koto naraba\n" +
                        "matsudai shikato osamari ga tsuku",
                commentary = "六十六, 六十七,사람들이 마음을 빨리 맑히지 않으면 싱앙의 중심인 감래대를 세울 소도 억고, 또 이 길의 중심이 되는 자를 집터에 정주시킬 수도 없다. 이것만 빨리 된다면 이 길의 기초는 확립되는 것이다.\n 이것은 감로대의 터전 결정과 이 길의 중심이 될 초대 진주님을 집터에 정주시키고자 서두르신 것이다.(第三호 八, 九의 주석 참조)"
            )
        )
        allContent.add(
            ContentItem(
                korean = " 이 세상을 창조한 신의 진실을\n 일러줄 테니 거짓이라 생각 말라",
                japanese = "このよふをはじめた神のしんぢつを\nといてきかするうそとをもうな",
                english = "kono yo o hajimeta Kami no shinjitsu o\n" +
                        "toite kikasuru uso to omouna",
                commentary = ""
            )
        )
        allContent.add(
            ContentItem(
                korean = " 지금까지도 심학 고기 있지만\n 근본을 아는 자는 없는 거야 3-69",
                japanese = "いまゝでもしんがくこふきあるけれど\nもとをしりたるものハないぞや",
                english = "imamade mo shingaku koki aru keredo\n" +
                        "moto o shiritaru mono wa nai zo ya",
                commentary = "六十九,종래에도 사람들은 심학(心學)이니 고기(古記)니 말들을 하고 있지만, 이 세상이 만들어진 과정을 참으로 아는 자는 아무도 없다.\n심학(心學)이란 당시 사회에 널리 퍼져 있던 윤리사상으로, 심학도화(心學道話)를 말한다.\n고기(古記)란 여기서는 예부터 전해져 오는 건국설화를 말한다."
            )
        )
        allContent.add(
            ContentItem(
                korean = " 그러리라 진흙바다 속의 과정을\n 아는 자는 없을 테니까 3-70",
                japanese = "そのはづやどろうみなかのみちすがら\nしりたるものハないはづの事",
                english = "sono hazu ya doroumi naka no michisugara\n" +
                        "shiritaru mono wa nai hazu no koto",
                commentary = "七十,그도 그럴 것이 태초에 진흙바다 가운데서 인간을 창조한 과정을 알고 있는 사람은 아무도 없을 것이기 때문이다."
            )
        )
        allContent.add(
            ContentItem(
                korean = " 이제부터는 이 세상 창조 이래 없던 일을\n 차츰차츰 일러줄 것이니 3-71",
                japanese = "これまでハこのよはじめてない事を\nたん／＼といてきかす事なり",
                english = "koremade wa kono yo hajimete nai koto o\n" +
                        "dandan toite kikasu koto nari",
                commentary = ""
            )
        )
        allContent.add(
            ContentItem(
                korean = " 무엇이든 없던 일만을 일러주지만\n 여기에 틀림은 없는 거야 3-72",
                japanese = "なにもかもない事はかりとくけれど\nこれにまちごた事ハないぞや",
                english = "nanimo kamo nai koto bakari toku keredo\n" +
                        "kore ni machigota koto wa nai zo ya",
                commentary = ""
            )
        )
        allContent.add(
            ContentItem(
                korean = " 11에 9가 없어지고 괴로움이 없는\n 정월 26일을 기다린다 3-73",
                japanese = "十一に九がなくなりてしんわすれ\n正月廿六日をまつ",
                english = "u ichi ni ku ga nakunarite shin wasure\n" +
                        "shogatsu niju roku nichi o matsu",
                commentary = ""
            )
        )
        allContent.add(
            ContentItem(
                korean = " 그 동안에 중심도 이루어 욕심 버리고\n 인원을 갖추어서 근행할 준비하라 3-74",
                japanese = "このあいだしんもつきくるよくハすれ\nにんぢうそろふてつとめこしらゑ",
                english = "kono aida shin mo tsukikuru yoku wasure\n" +
                        "ninju sorote Tsutome koshirae",
                commentary = "七十三, 七十四,이 노래는 교조님이 현신을 감추실 것을 예고하신 것이다. 즉, 세상에서는 교조님을 묙표로 하여 점점 심하게 박해를 가해 오고, 그로 인해 차츰 이 길이 늦어지자 교조님은 자신의 수명을 25년 줄여 몸을 감춤으로써 세상의 압박을 작게 하여 이 길을 넗히려 한다. 그리고 그때까지는 진주도 정해지고 감로대도 세워질 것이므로, 모두들은 마음을 맑히고 빨리 인원을 갖추어서 근행을 할 준비를 하라고 깨우치신 것이다. 그러나 당시 사람들은 이것을 몰랐으며, 후일 지도말씀에 의해서 비로서 어버이신님의 깊은 뜻을 알게 되었다. '자아 자아, 26일이라 붓으로 적어 놓고 시작한 리를 보라. 자아 자아, 또 정월 26일부터 현신의 문을 열고 세계를 평탄한 땅으로 밞아 고르러 나가서 시작한 리와, 자아 자와, 없애버리겠다는 말을 듣고 한 리와, 두 가지를 비교해서 리를 분간하면, 자아 자아, 라는 선명하게 깨달아질 것이다.'\t (1889. 3. 10)"
            )
        )
        allContent.add(
            ContentItem(
                korean = " 나닐이 신이 마음 서두르는 것은\n 자유자재를 빨리 보이고 싶어서 3-75",
                japanese = "にち／＼に神の心のせきこみハ\nぢうよじざいをはやくみせたい",
                english = "nichinichi ni Kami no kokoro no sekikomi wa\n" +
                        "juyojizai o hayaku misetai",
                commentary = ""
            )
        )
        allContent.add(
            ContentItem(
                korean = " 이제부터는 인원 갖추어 근행한다\n 이것으로 확실히 온 세상 안정이 된다 3-76",
                japanese = "これからハにんぢうそろをてつとめする　これでたしかににほんをさまる",
                english = "korekara wa ninju sorote Tsutome suru\n" +
                        "kore de tashikani nihon osamaru",
                commentary = "七十六,이제부터 인원을 갖추어 신악근행을 하면 사람들의 마음도 맑아지고 어버이신의 마음도 용솟음치기 때문에, 이것으로 온 세상은 학실히 안정이 된다."
            )
        )
        allContent.add(
            ContentItem(
                korean = " 진실로 구제한줄기이기 때문에\n 전혀 두려움 억는 거야 3-77",
                japanese = "しんぢつにたすけ一ぢよてあるからに\nなにもこわみハさらにないぞや",
                english = "shinjitsu ni tasuke ichijo de aru karani\n" +
                        "nanimo kowami wa sarani nai zo ya",
                commentary = ""
            )
        )
        allContent.add(
            ContentItem(
                korean = " 무엇이든 구제한줄기 멈추게 하면\n 신의 섭섭함이 몸의 장애로 나타난다 3-78",
                japanese = "なにもかもたすけ一ぢよとめるなら\n神のさんねんみにさハりつく",
                english = "nanimo kamo tasuke ichijo tomeru nara\n" +
                        "Kami no zannen mi ni sawari tsuku",
                commentary = "七十七, 七十八,이 길을 진실한 구제한줄기의 길인 만큼 이것을 빨리 넒히도록 노력하라고 어버이신님은 서두르고 있지만, 결의 사람들은 세상이 겁나고 관헌이 두려운 나머지 곧잘 움츠러들기가 일쑤였다. 그러나 이래서는 이 길이 늦어지므로 앞으로 누구든지 이 구제한줄기의 활동을 저지한다면, 어버이신님을 섭섭케하여 그 결과 반드시 몸에 장애를 방게 될 것이니 잘 생각하라고 곁의 사람들에게 깨우치신 것이다."
            )
        )
        allContent.add(
            ContentItem(
                korean = " 생각하라 만가지구제의 이 준비\n 인간의 재주라고 전혀 생각 말라 3-79",
                japanese = "しやんせよ万たすけのこのもよふ\nにんけんハざとさらにをもうな",
                english = "shiyan seyo yorozu tasuke no kono moyo\n" +
                        "ningen waza to sarani omouna",
                commentary = "七十九,잘 생각해 보라, 만가지구제를 위하여 어버이신이 마음을 다하고있는 이 준비가 과연 인간의 기교로써 되겠는가, 그런데도 지금까지 어버이신이 하는 일은 아무것도 모른 채 이것을 인간마음으로 의심하고 있는 것이다."
            )
        )
        allContent.add(
            ContentItem(
                korean = " 지금까지는 만가지를 전혀 모르는 채\n 모두 인간마음뿐이야 3-80",
                japanese = "いまゝでハなにかよろづがハからいで\nみなにんけんの心ばかりで",
                english = "imamade wa nanika yorozu ga wakaraide\n" +
                        "mina ningen no kokoro bakari de",
                commentary = ""
            )
        )
        allContent.add(
            ContentItem(
                korean = " 이제부터는 신의 마음과 욋사람의\n 마음과 마음을 비교한다 3-81",
                japanese = "これからハ神の心と上たるの\n心と心のひきやハせする",
                english = "korekara wa Kami no kokoro to kami taru no\n" +
                        "kokoro to kokoro no hikiyawase suru",
                commentary = ""
            )
        )
        allContent.add(
            ContentItem(
                korean = " 이 이야기를 예삿일로 생각 말라\n 신이 차마 볼 수 없기 때문이니 3-82",
                japanese = "このはなし一寸の事やとをもうなよ\n神がしんぢつみかねたるゆへ",
                english = "kono hanashi chotto no koto ya to omouna yo\n" +
                        "Kami ga shinjitsu mikanetaru yue",
                commentary = ""
            )
        )
        allContent.add(
            ContentItem(
                korean = " 이제부터는 신의 힘과 윗사람의\n 힘을 서로 겨룬다고 생각하라 3-83",
                japanese = "これからハ神のちからと上たるの\nちからくらべをするとをもへよ",
                english = "korekara wa Kami no chikara to kami taru no\n" +
                        "chikara kurabe o suru to omoe yo",
                commentary = ""
            )
        )
        allContent.add(
            ContentItem(
                korean = " 아무리 힘센 자도 있으면 나와 보라\n 신의 쪽에서는 갑절의 힘을 3-84",
                japanese = "いかほどのごふてきあらばだしてみよ\n神のほふにもばいのちからを",
                english = "ika hodono goteki araba dashite miyo\n" +
                        "Kami no ho nimo bai no chikara o",
                commentary = ""
            )
        )
        allContent.add(
            ContentItem(
                korean = " 진실한 신이 이 세상에 나왔으니\n 어떤 준비도 한다고 생각하라 3-85",
                japanese = "しんぢつの神がをもていでるからハ\nいかなもよふもするとをもゑよ",
                english = "shinjitsu no Kami ga omote i deru kara wa\n" +
                        "ikana moyo mo suru to omoe yo",
                commentary = "八十一～八十五,이 구제한줄기의 길을 어서 넒히려고 어버이신님은 서두르고 있지만, 지방 관헌이 이 길에 대한 몰이해로 여러 가지 압박을 가하고 있고, 또 곁의 사람들은 이것을 두려워한 나머지 망설이고만 있으니 어버ㅇ신님은 차마 볼 수가 없다. 윗사람들의 몰이해는 진실로 이 길의 진수를 모르는 탓이므로, 앞으로는 윗사람들에게도 어버이신님의 뜻을 깨닫게 할 준비를 한다. 그리고 신직과 승려들이 인간의 지혜나 힘으로 아무리 강력하게 비난하고 반대하더라도, 어버이신님은 그것을 녹이고도 남을 따뜻한 어버이마음이 있고, 또 이러한 어버이신님이 세상에 나타나 있는 이상 어떠한 수호도 할 것이니, 안심하고 구제한줄기의 길로 나가라고 격려하고 계신다."
            )
        )
        allContent.add(
            ContentItem(
                korean = " 지금까지는 미칠곳이 미친곳을 멋대로 했다\n 신의 섭섭함을 어찌해야 할지 3-86",
                japanese = "いまゝでわからがにほんをまゝにした\n神のざんねんなんとしよやら",
                english = "imamade wa kara ga nihon o mamani shita\n" +
                        "Kami no zannen nanto shiyo yara",
                commentary = "八十六,지금까지는 인간생각에 사로잡혀 어버이신의 가르침을 멋대로 방해해 왔는데, 어버이신은 이것을 참으로 안타깝게 여기고 있다.\n 미친곳, 미칠곳은 第二호 三十四의 주석 참조."
            )
        )
        allContent.add(
            ContentItem(
                korean = " 앞으로는 미친곳이 미칠곳을 생각대로 한다\n 모든 사람들은 명심해 두라 3-87",
                japanese = "このさきハにほんがからをまゝにする\nみな一れつハしよちしていよ",
                english = "konosaki wa nihon ga kara o mamani suru\n" +
                        "mina ichiretsu wa shochi shite iyo",
                commentary = "八十七,앞으로는 어버이신의 뜻을 일러주어 지금까지 전혀 모르던 곳에도 구제한줄기의 어버이마음이 고루 미치도록 자유자재한 섭리를 나타낼 터이니, 모두들은 이 점을 잘 명심해 두도록 하라."
            )
        )
        allContent.add(
            ContentItem(
                korean = " 같은 나무의 뿌리와 가지라면\n가지는 꺾여도 뿌리는 번성해진다 3-88",
                japanese = "をなじきのねへとゑだとの事ならバ\nゑたハをれくるねハさかいでる",
                english = "onaji ki no ne to eda to no koto naraba\n" +
                        "eda wa orekuru ne wa sakai deru",
                commentary = "八十八,한 나무의 뿌리와 가지라면, 설사 가지는 꺽일지라도 뿌리는 그대로 남아 번성해진다.\n 교조님이 단식을 하실 때, 그렇게 단식을 하셔도 괜찮습니까 하고 걱정하는 사람에게\n'마음의 뿌리야, 마음만 어버이신님께 통하고 있으면 결코 시들지 않는 거야.'라는 의미의 말씀을 들려주신 적이 있다."
            )
        )
        allContent.add(
            ContentItem(
                korean = " 지금까지는 미칠곳을 위대하다 했지만\n 이제부터 앞으로는 꺾일 뿐이야 3-89",
                japanese = "いまゝでわからハゑらいとゆうたれど\nこれからさきハをれるはかりや",
                english = "imamade wa kara wa erai to yu taredo\n" +
                        "korekara saki wa oreru bakari ya",
                commentary = "八十九,지금까지는 어버이신의 가르침을 듣지 않아도 단지 지혜나 기술 등만 뛰어나면 훌륭한 사람이라고 해 왔지만, 앞으로는 그런 사람도 인간생각을 버리고 어버이신의 뜻을 깨달아 따라오게 된다.\n 꺾인다는 것은 인간생각을 꺾고 어버이신님의 가르침을 그리며 찾아온다는 뜻이다."
            )
        )
        allContent.add(
            ContentItem(
                korean = " 미친곳을 보라 약한 듯 생각했지만\n 뿌리가 나타나면 굴복하게 될 것이다 3-90",
                japanese = "にほんみよちいさいよふにをもたれど\nねがあらハればをそれいるぞや",
                english = "nihon miyo chiisai yoni omotaredo\n" +
                        "ne ga arawareba osore iru zo ya",
                commentary = "九十,지금까지는 어버이신의 가르침이 전해진 곳을 가볍게 보고 업신여겨 왔으나, 어버이신의 뜻이 세상에 나타나 구제한줄기에 의해 사람들의 마음이 맑아지면 어떤 사람도 모두 그곳을 그리워하고 흠모하게 된다."
            )
        )
        allContent.add(
            ContentItem(
                korean = " 이 힘 인간의 재주라 생각말라\n 신의 힘이라 이에는 당할 수 없다 3-91",
                japanese = "このちからにんけんハさとをもハれん\n神のちからやこれハかなわん",
                english = "kono chikara ningen waza to omowaren\n" +
                        "Kami no chikara ya kore wa kanawan",
                commentary = "九十一,이 절대한 섭리는 인간의 힘이라고는 생각할 수 없다. 모두가 어버이신의 힘이 나타난 것이므로 누구든 마음으로 감동하지 않을 수 없게 된다.\n八十六～九十一,위의 노래들은 어느 것이나 집필 당시인 1874년경의 사회상을 우려한 것으로, 인간창조시 어버이신님을 진실한 어비이로 하여 태어난 형제자매들이 서로 돕고 서로 위하는 마음이 없이, 저마다 이기주의에 빠져 서로 싸우고 서로 해치며 살아가고 있는데 대해 강력하게 반성을 촉구하시면서, 모두가 구제한줄기의 길로 나아가 서로 화목한 가운데 즐거운 삶을 누리도록 하라고 사람들의 신앙심을 고무하신 내용이다.(제2호 四十의 주석 참조)"
            )
        )
        allContent.add(
            ContentItem(
                korean = " 이 세상은 번창하게 살고 있지만\n 근본을 아는 자는 없으므로 3-92",
                japanese = "このよふハにぎハしくらしいるけれど\nもとをしりたるものハないので",
                english = "kono yo wa nigiwashi kurashi iru keredo\n" +
                        "moto o shiritaru mono wa nai node",
                commentary = "九十二,세상 사람들은 아무 생각 없이 흥청거리며 살고 있지만, 태초의 어버이의 수호에 대해서 알고 있는 사람은 아무도 없다."
            )
        )
        allContent.add(
            ContentItem(
                korean = " 이 근본을 자세히 알게 되면\n 질병이 생길 리는 없을 텐데 3-93",
                japanese = "このもとをくハしくしりた事ならバ\nやまいのをこる事わないのに",
                english = "kono moto o kuwashiku shirita koto naraba\n" +
                        "yamai no okoru koto wa nai no ni",
                commentary = ""
            )
        )
        allContent.add(
            ContentItem(
                korean = " 아무것도 모르고 지내는 이 자녀들\n 신의 눈에는 애처로운 일 3-94",
                japanese = "なにもかもしらずにくらすこの子共\n神のめへにハいぢらき事",
                english = "nanimo kamo shirazu ni kurasu kono kodomo\n" +
                        "Kami no me niwa ijirashiki koto",
                commentary = "九十三, 九十四, 어버이신이 인간을 창조한 으뜸인 리를 잘 알고 있지만, 나쁜 마음도 생기지 않고 질병을 앓는 사람도 없을 터인데, 사람들은 아무것도 모른 채 제멋대로 마음을 쓰고 있다. 어버이신의 눈으로 볼 때 이것이 매우 가엾은 일이다."
            )
        )
        allContent.add(
            ContentItem(
                korean = " 어떤 것이든 질병이란 전혀 없다\n 마음 잘못 쓴 길이 있으므로 3-95",
                japanese = "なにゝてもやまいとゆうてさらになし\n心ちがいのみちがあるから",
                english = "nani nitemo yamai to yute sarani nashi\n" +
                        "kokoro chigai no michi ga aru kara",
                commentary = ""
            )
        )
        allContent.add(
            ContentItem(
                korean = " 이 길은 인색 탐 편애\n 욕심과 교만이 이것이 티끌이야 3-96",
                japanese = "このみちハをしいほしいとかハいと\nよくとこふまんこれがほこりや",
                english = "kono michi wa oshii hoshii to kawaii to\n" +
                        "yoku to koman kore ga hokori ya",
                commentary = "九十六,인간이 쓰는 마음 가운데 인색, 탐, 편애, 욕심, 교만 등, 이것이 티끌이 되는 것이다.\n 본교에서는 인간의 괴로움은 마음속에 티끌이 쌓여 있기 때문에 생간다고 말한다. 이 티끌은 위의 노래에 나타난 것 이외에 미움, 원망, 분노 등, 세 가지가 더 있으며, 이것이 이른바 여덦가지 티끌이다. 대개 이것들이 인간의 나쁜 마음을 유도하는 근본이 된다."
            )
        )
        allContent.add(
            ContentItem(
                korean = " 이 세상의 인간은 모두 신의 자녀야\n 신이 하는 말 단단히 분간하라 3-97",
                japanese = "このよふのにんけんハみな神のこや\n神のゆう事しかときゝわけ",
                english = "kono yo no ningen wa mina Kami no ko ya\n" +
                        "Kami no yu koto shikato kikiwake",
                commentary = ""
            )
        )
        allContent.add(
            ContentItem(
                korean = " 티끌만 깨끗하게 털어 버리면\n 다음에는 진기한 구제할 거야 3-98",
                japanese = "ほこりさいすきやかはろた事ならば\nあとハめづらしたすけするぞや",
                english = "hokori sai sukiyaka harota koto naraba\n" +
                        "ato wa mezurashi tasuke suru zo ya",
                commentary = ""
            )
        )
        allContent.add(
            ContentItem(
                korean = " 진실한 마음에 따른 이 구제\n 앎지 않고 죽지 않고 쇠하지 않도록 3-99",
                japanese = "しんぢつの心しだいのこのたすけ\nやますしなずによハりなきよふ",
                english = "shinjitsu no kokoro shidai no kono tasuke\n" +
                        "yamazu shinazu ni yowari naki yo",
                commentary = "九十九,어버이신이 구제한다고 해도 그것은 원하는 사람의 마음은 보고 하는 것으로, 진실한 마음이 어버이신의 뜻에 맞는다면 누구나 정명(定命)까지 앎지 않고 죽지 않고 쇠하지도 않고 살아갈 수가 있다."
            )
        )
        allContent.add(
            ContentItem(
                korean = " 이 구제 115세 정명으로\n 하고 싶은 한결같은 신의 마음 3-100",
                japanese = "このたすけ百十五才ぢよみよと\nさだめつけたい神の一ぢよ",
                english = "kono tasuke hyaku ju go sai jomyo to\n" +
                        "sadame tsuketai Kami no ichijo",
                commentary = "百,어버이신으로서는 인간의 정명을 115세로 정하고 싶은 것이다.\n 감로대가 완성되어 하늘에서 내려 주는 감로를 받아 먹게 되면 누구나 정명을 누리게 되고, 또 마음에 따라서는 언제까지나 이 세상에서 살아갈 수가 있다."
            )
        )
        allContent.add(
            ContentItem(
                korean = " 나닐이 신이 마음 서두르는 것을\n 곁의 사람들은 어떻게 생각하는가 3-101",
                japanese = "にち／＼に神の心のせきこみを\nそばなるものハなんとをもてる",
                english = "nichinichi ni Kami no kokoro no sekikomi o\n" +
                        "soba naru mono wa nanto omoteru",
                commentary = ""
            )
        )
        allContent.add(
            ContentItem(
                korean = " 윗사람이 두려워 침울하고 있다\n 신의 서두름이라 두려울 것 없는 거야 3-102",
                japanese = "上たるをこわいとをもていすみいる\n神のせきこみこわみないぞや",
                english = "kami taru o kowai to omote izumi iru\n" +
                        "Kami no sekikomi kowami nai zo ya",
                commentary = "百二,당시 지방 관헌이나 세상의 오해를 두려워하고 있는 곁의 사람들에 대해서 신앙을 고무하신 노래이다."
            )
        )
        allContent.add(
            ContentItem(
                korean = " 가슴앎이를 질병이라 생각 말라\n 신의 서두름이 쌓여 있기 때문이니 3-103",
                japanese = "むねあしくこれをやまいとをもうなよ\n神のせきこみつかゑたるゆへ",
                english = "mune ashiku kore o yamai to omouna yo\n" +
                        "Kami no sekikomi tsukaetaru yue",
                commentary = "百三,가슴이 답답하여 기분이 나쁘더라도 이것을 졀병이라고 생각해서는 안된다. 그것은 어버이신의 서두름이 쌓여 있는 표시이다."
            )
        )
        allContent.add(
            ContentItem(
                korean = " 차츰차츰 신의 마음은\n 신기함을 나타내어 구제를 서두른다 3-104",
                japanese = "たん／＼と神の心とゆうものわ\nふしぎあらハしたすけせきこむ",
                english = "dandan to Kami no kokoro to yu mono wa\n" +
                        "fushigi arawashi tasuke sekikomu",
                commentary = ""
            )
        )
        allContent.add(
            ContentItem(
                korean = " 이 신기함은 어떤 것이라 생각하는가\n 티끌을 털어내어 깨끗이 청소한다 3-105",
                japanese = "このふしきなんの事やとをもている\nほこりはろふてそふぢしたてる",
                english = "kono fushigi nanno koto ya to omote iru\n" +
                        "hokori harote soji shitateru",
                commentary = ""
            )
        )
        allContent.add(
            ContentItem(
                korean = " 다음에 빨리 기둥을 세우게 되면\n 이것으로 이 세상은 안정이 된다 3-106",
                japanese = "あとなるにはやくはしらをいれたなら\nこれでこのよのさだめつくなり",
                english = "ato naru ni hayaku hashira o ireta nara\n" +
                        "kore de kono yo no sadame tsuku nari",
                commentary = "百六,사람들이 마음을 청소하고 난 다음에는 신앙의 중심을 정하고 싶다. 이것만 이루어지면 이 세상은 안전이 된다."
            )
        )
        allContent.add(
            ContentItem(
                korean = " 이 이야기가 빨리 나타나게 되면\n 어떤 자도 모두 납득하라 3-107",
                japanese = "このはなしはやくみへたる事ならば\nいかなものでもみなとくしんせ",
                english = "kono hanashi hayaku mietaru koto naraba\n" +
                        "ikana mono demo mina tokushin se",
                commentary = ""
            )
        )
        allContent.add(
            ContentItem(
                korean = " 지금까지는 증거 징험이라 하고 있지만\n 감로대란 무엇인지 3-108",
                japanese = "いまゝでハしよこためしとゆへあれど\nかんろふだいもなんの事やら",
                english = "imamade wa shoko tameshi to yue aredo\n" +
                        "Kanrodai mo nanno koto yara",
                commentary = "百八,지금까지 증거 징험이라 말하게 있지만 아직 아무것도 나타나지 않으므로, 모든 사람들은 감로대에 대한 어버이신의 뜻도 잘 이해하지 못할 것이다.\n증거 징험이란 태초에 인간을 창제한 증거로 터전에 감래대를 세우게 되면, 이 세상에는 참으로 평화로운 즐거운 삶의 세계가 실현된다는 실현된다는 신언을 실증하는 것을 말한다.(第三호 十四, 第十七호 九 참조) 감로대에 대해서는 第八호 七十八～八十六, 第九호 十八～二十, 四十四～六十四, 第十호 二十一, 二十二, 七十九, 第十七호 二～十 참조)"
            )
        )
        allContent.add(
            ContentItem(
                korean = " 이 사람을 4년 전에 데려가서\n 신이 안았다 이것이 증거야 3-109",
                japanese = "このものを四ねんいせんにむかいとり\n神がだきしめこれがしよこや",
                english = "kono mono o yo nen izen ni mukaitori\n" +
                        "Kami ga dakishime kore ga shoko ya",
                commentary = "百九,이 사람을 4년 전에 데려가서 그 혼을 다시 으뜸인 인연이 있는 터전에 태어나게 하려고 어버이신이 품에 안고 있는데, 이 것이 곧 어버이신의 자유자재한 섭리를 나타내는 증거이다."
            )
        )
        allContent.add(
            ContentItem(
                korean = " 진실로 빨리 돌려줄 준비\n 신의 서두름 이것이 첫째야 3-110",
                japanese = "しんぢつにはやくかやするもよふたて\n神のせきこみこれがたい一",
                english = "shinjitsu ni hayaku kayasuru moyodate\n" +
                        "Kami no sekikomi kore ga daiichi",
                commentary = "百十,이 혼이 빨리 으뜸인 터전에 다시 태어나도록 준비하고 있는데, 이것이 어버이신의 첫째가는 서두름이다.\n 이것은 4년 전인 1870년 음력 3월 15일에 출직한 슈지 선생의 서녀인 오슈우님을 말씀하신 것이다. 그는 이 길을 위해 없어서는 안될 중요한 혼의 인연을 지니고 있었기 때문에 어버이신님이 그 혼을 품에 안고 계시면서, 빨리 인연이 있는 으뜸인 터전에 다시 태어나게 하려고 서두려셨던 것이다. 여기 한가지 덧붙여 둘 것은, 一八七三년 양력 一월 一일은 一八七三년 음력 二월 十三일에 해당한다.(第一호 六十, 六十一의 주석 참조) "
            )
        )
        allContent.add(
            ContentItem(
                korean = " 이제까지는 자유자재라고 자주 말했어도\n 아무것도 나타난 것은 없었지만 3-111",
                japanese = "これまでハぢうよじざいとまゝとけど\nなにもみへたる事わなけれど",
                english = "koremade wa juyojizai to mama tokedo\n" +
                        "nanimo mietaru koto wa nakeredo",
                commentary = ""
            )
        )
        allContent.add(
            ContentItem(
                korean = " 이제부터는 어떤 이야기도 해 두고서\n 그것이 나타나면 자유자재야 3-112",
                japanese = "これからハいかなはなしもときをいて\nそれみゑたならじうよぢざいや",
                english = "korekara wa ikana hanashi mo tokioite\n" +
                        "sore mieta nara juyojizai ya",
                commentary = "百十一, 百十二,이제까지 어버이신의 섭리는 자유자재라고 자주 일러주었지만, 그 자유자재한 섭리가 아직  사람들의 눈앞에 나타난 적은 없었다. 앞으로는 어떤 이야기도 미리 해 두고서 그 이야기대로 증거가 나타나면 그것으로 어버이신의 섭리는 자유자재임을 분명히 깨달아야 한다."
            )
        )
        allContent.add(
            ContentItem(
                korean = " 지금까지의 일은 아무 말도 하지 않도록\n 26일에 시작할 테야 3-113",
                japanese = "いまゝでの事ハなんにもゆてくれな\n廿六日にはじめかけるで",
                english = "imamade no koto wa nan nimo yute kurena\n" +
                        "niju roku nichi ni hajime kakeru de",
                commentary = "百十三,이듬해인 1885년 음력 5월 26일에 감로대의 터전이 결정되었다."
            )
        )
        allContent.add(
            ContentItem(
                korean = " 이제부터는 세상 사람들의 마음 용솟음치게 해서\n 온 세상을 안정시킬 준비할 거야 3-114",
                japanese = "これからハせかいの心いさめかけ\nにほんをさめるもよふするぞや",
                english = "korekara wa sekai no kokoro isame kake\n" +
                        "nihon osameru moyo suru zo ya",
                commentary = "百十四,이제부터는 세상 사람들의 마음을 용솟음치게 해서 온 세상을 원만히 안정시킬 준비를 한다."
            )
        )
        allContent.add(
            ContentItem(
                korean = " 인간의 마음이란 어리석어서\n 나타난 것만 이야기한다 3-115",
                japanese = "にんけんの心とゆうハあざのふて\nみへたる事をばかりゆうなり",
                english = "ningen no kokoro to yu wa azanote\n" +
                        "mietaru koto o bakari yu nari",
                commentary = ""
            )
        )
        allContent.add(
            ContentItem(
                korean = " 이제부터는 없던 일만 일러둔다\n 이제부터 앞날을 똑똑히 두고 보라 3-116",
                japanese = "これからハない事ばかりといてをく\nこれからさきをたしかみていよ",
                english = "korekara wa nai koto bakari toite oku\n" +
                        "korekara saki o tashika mite iyo",
                commentary = ""
            )
        )
        allContent.add(
            ContentItem(
                korean = " 무슨 일이든 차츰차츰 말하기 시작하리라\n 나타난 것은 새삼 말하지 않아 3-117",
                japanese = "どのよふな事もたん／＼ゆいかける\nみへたる事ハさらにゆハんで",
                english = "dono yona koto mo dandan yuikakeru\n" +
                        "mietaru koto wa sarani yuwan de",
                commentary = ""
            )
        )
        allContent.add(
            ContentItem(
                korean = " 이 세상을 창조한 신의 중심의 기둥\n 빨리 세우고 싶다 한결같은 신의 마음 3-118",
                japanese = "このよふをはじめた神のしんばしら\nはやくつけたい神の一ぢよ",
                english = "kono yo o hajimeta Kami no Shinbashira\n" +
                        "hayaku tsuketai Kami no ichijo",
                commentary = "百十八,이 세상 인간을 창조한 리를 나타내는 감로대를 일각이라도 빨리 세우고자 하는 것이 어버이신의 한결같은 마음이다."
            )
        )
        allContent.add(
            ContentItem(
                korean = " 눈에 안 보이는 신이 하는 말 하는 일을\n 차츰차츰 듣고서 생각해 보라 3-119",
                japanese = "めへにめん神のゆう事なす事を\nたん／＼きいてしやんしてみよ",
                english = "me ni men Kami no yu koto nasu koto o\n" +
                        "dandan kiite shiyan shite miyo",
                commentary = ""
            )
        )
        allContent.add(
            ContentItem(
                korean = " 지금의 길 윗사람 멋대로라 생각하고 있다\n 틀린 생각이야 신의 마음대로야 3-120",
                japanese = "いまのみち上のまゝやとをもている\n心ちがうで神のまゝなり",
                english = "ima no michi kami no mama ya to omote iru\n" +
                        "kokoro chigau de Kami no mama nari",
                commentary = "百二十,윗사람들은 이 길을 멋대로 할 수 있다고 생각하고 억누르려 하지만, 이 길은 구제한줄기의 길이므로 인간생각대로 되는 것이 아니라 어버이신의 의도대로 되는 것이다."
            )
        )
        allContent.add(
            ContentItem(
                korean = " 윗사람은 온 세상을 멋대로 한다\n 신의 섭섭함 이것을 모르는가 3-121",
                japanese = "上たるハせかいぢううをまゝにする\n神のざんねんこれをしらんか",
                english = "kami taru wa sekaiju o mamani suru\n" +
                        "Kami no zannen kore o shiran ka",
                commentary = ""
            )
        )
        allContent.add(
            ContentItem(
                korean = " 이제까지는 온 세상을 윗사람 멋대로\n 이제 앎으로는 양상이 바뀔 거야 3-122",
                japanese = "これまでハよろづせかいハ上のまゝ\nもふこれからハもんくかハるぞ",
                english = "koremade wa yorozu sekai wa kami no mama\n" +
                        "mo korekara wa monku kawaru zo",
                commentary = "百二一, 百二二,온갖 세상 일은 모두가 인간의 힘으로 되는 줄로 생각하고 윗사람 가운데는 메사를 제멋대로 하는 자가 있으나, 그것은 이 세상을 창조한 어버이신의 뜻을 모르기 때문이며, 따라서 본래 없던 세계 억던 인간을 만든 어버이신의 눈으로 볼 때, 참으로 유감스런 일이 아닐 수 없다. 그러나 으뜸인 어버어신이 이 세상에 나타난 이상, 앞으로는 어버이신의 뜻을 깨닫게 하여 인간들이 제멋대로 행동하지 못하도록 한다."
            )
        )
        allContent.add(
            ContentItem(
                korean = " 이 세상을 창조한 이래 아무것도\n 일러준 일이 없으므로 3-123",
                japanese = "このよふをはじめてからハなにもかも\nといてきかした事ハないので",
                english = "kono yo o hajimete kara wa nanimo kamo\n" +
                        "toite kikashita koto wa nai node",
                commentary = ""
            )
        )
        allContent.add(
            ContentItem(
                korean = " 윗사람은 온 세상을 제멋대로\n 생각하고 있는 것은 틀리는 거야 3-124",
                japanese = "上たるハせかいぢううをハがまゝに\nをもているのハ心ちかうで",
                english = "kami taru wa sekaiju o waga mamani\n" +
                        "omote iru no wa kokoro chigau de",
                commentary = "百二四,윗사람들 가운데는 세상 모든 일이 자기 생각대로 돈다고 여기고 있는 자가 있으나, 이것은 틀린 생각이다."
            )
        )
        allContent.add(
            ContentItem(
                korean = " 높은산에서 자라난 나무나 골짜기에서\n 자라난 나무나 모두 다 같은 것 3-125",
                japanese = "高山にそだつる木もたにそこに\nそたつる木もみなをなじ事",
                english = "takayama ni sodatsuru kii mo tanisoko ni\n" +
                        "sodatsuru kii mo mina onaji koto",
                commentary = "百二五,상류사회 사람들이나 하류사회 사람들이나 생활 수준은 다르지만 모두가 어버이신의 자녀라는 점에서는 하등 차별이 없다."
            )
        )
        allContent.add(
            ContentItem(
                korean = " 인간은 모두가 신의 대물이야\n 신의 자유자재 이것을 모르는가 3-126",
                japanese = "にんけんハみな／＼神のかしものや\n神のぢうよふこれをしらんか",
                english = "ningen wa minamina Kami no kashimono ya\n" +
                        "Kami no juyo kore o shiran ka",
                commentary = "百二六,인간의 육체는 인간 자신이 만든 것이 아니고, 이 세상을 창조한 어버이신이 만들어서 인간에게 빌려 주고 있는 것이다. 인간이 살아가고 있는 것도 모두 어버이신의 자유자재한 수호가 있기 때문인데 이것을 모르는가."
            )
        )
        allContent.add(
            ContentItem(
                korean = " 사람들은 모두 제 몸 조심하라\n 신이 언제 어디로 나갈는지 3-127",
                japanese = "いちれつハみな／＼わがみきをつけよ\n神がなんどきとこへいくやら",
                english = "ichiretsu wa minamina waga mi ki o tsuke yo\n" +
                        "Kami ga nandoki doko e iku yara",
                commentary = "百二七,사람들은 누구나 자신의 몸을 돌이켜보고 그것이 어버이신의 차물(借物)임을 잘 깨달아 부디 조심하도록 하라. 마음에 그릇됨이 있으면 어버이신의 수호는 언제 중지될지도 모른다."
            )
        )
        allContent.add(
            ContentItem(
                korean = " 잠깐 한마디 신이 마음 서두르는 것은\n 용재 모을 준비만을 3-128",
                japanese = "一寸はなし神の心のせきこみハ\nよふぼくよせるもよふばかりを",
                english = "choto hanashi Kami no kokoro no sekikomi wa\n" +
                        "yoboku yoseru moyo bakari o",
                commentary = ""
            )
        )
        allContent.add(
            ContentItem(
                korean = " 차츰차츰 많은 나무들이 있지만\n 어느 것이 용재가 될지 모르겠지 3-129",
                japanese = "たん／＼とをふくたちきもあるけれど\nどれがよふほくなるしれまい",
                english = "dandan to oku tachiki mo aru keredo\n" +
                        "dore ga yoboku naru ya shiremai",
                commentary = "百二九,여러 가지 나무들이 많이 있으나 그 가운데서 어느 것이 용재가 될지 모를 것이다."
            )
        )
        allContent.add(
            ContentItem(
                korean = " 용재도 적은 수가 아닌 만큼\n 많은 용재가 필요하므로 3-130",
                japanese = "よふぼくも一寸の事でハないほどに\nをふくよふきがほしい事から",
                english = "yoboku mo chotto no koto dewa nai hodoni\n" +
                        "oku yoki ga hoshii koto kara",
                commentary = "百三十,용재도 적은 수로서는 안되므로 많은 수를 필요로 한다."
            )
        )
        allContent.add(
            ContentItem(
                korean = " 나날이 용재에게는 손질을 한다\n 어디가 나쁘다고 전혀 생각 말라 3-131",
                japanese = "にち／＼によふほくにてわていりする\nどこがあしきとさらにおもうな",
                english = "nichinichi ni yoboku nitewa teiri suru\n" +
                        "doko ga ashiki to sarani omouna",
                commentary = "百三一,어버이신이 용재로 쓰려고 생각하는 사람에게는 항상 손질을 하므로, 몸에 어딘가 좋지 않은 데가 있더라도 질병으로 여기지 말고 깊이 생각도록 하라."
            )
        )
        allContent.add(
            ContentItem(
                korean = " 같은 나무도 차츰차츰 손질하는 것도 있고\n 그대로 넘어뜨리는 나무도 있다 3-132",
                japanese = "をなじきもたん／＼ていりするもあり\nそのまゝこかすきいもあるなり",
                english = "onaji ki mo dandan teiri suru mo ari\n" +
                        "sono mama kokasu kii mo aru nari",
                commentary = "百三二,같은 인간이라도 차츰 손질을 해서 유용하게 쓰는 자가 있는가 하면 그렇지 못한 자도 있는데, 그것은 각자의 마음쓰기 나름이다."
            )
        )
        allContent.add(
            ContentItem(
                korean = " 온갖 자유자재한 이 징험\n 다른 곳에서는 결코 안 하는 거야 3-133",
                japanese = "いかなるのぢうよじざいのこのためし\nほかなるとこでさらにせんぞや",
                english = "ika naru no juyojizai no kono tameshi\n" +
                        "hoka naru toko de sarani sen zo ya",
                commentary = "百三三,자유자재한 온갖 이 징험들은 터전이기 때문에 할 수 있는 것이지 다른 곳에서는 결코 못한다."
            )
        )
        allContent.add(
            ContentItem(
                korean = " 지금까지도 징험이라 말해 왔지만\n 이제 이번이 마지막 징험인 거야 3-134",
                japanese = "いまゝでもためしとゆうてといたれど\nもふこのたびハためしをさめや",
                english = "imamade mo tameshi to yute toitaredo\n" +
                        "mo konotabi wa tameshi osame ya",
                commentary = ""
            )
        )
        allContent.add(
            ContentItem(
                korean = " 차츰차츰 무엇이든 이 세상은\n 신의 몸이야 생각해보라 3-135",
                japanese = "たん／＼となに事にてもこのよふわ\n神のからだやしやんしてみよ",
                english = "dandan to nani goto nitemo kono yo wa\n" +
                        "Kami no karada ya shiyan shite miyo",
                commentary = "百三五, 제三호 四十, 四十一, 참조."
            )
        )
        allContent.add(
            ContentItem(
                korean = " 이번에는 신이 이 세상에 나왔으니\n 만가지 일을 모두 가르칠 테다 3-136",
                japanese = "このたびハ神がをもていでゝるから\nよろづの事をみなをしへるで",
                english = "konotabi wa Kami ga omote i deteru kara\n" +
                        "yorozu no koto o mina oshieru de",
                commentary = ""
            )
        )
        allContent.add(
            ContentItem(
                korean = " 각자의 몸은 차물임을\n 모르고 있어서는 아무것도 모른다 3-137",
                japanese = "めへ／＼のみのうちよりのかりものを\nしらずにいてハなにもわからん",
                english = "meme no minouchi yori no karimono o\n" +
                        "shirazu ni ite wa nanimo wakaran",
                commentary = "百三七,사람들이 각자의 몸은 어버이신으로부터 빌린 것이라는 사실을 모르고서는 그 밖에 다른 것도 결코 알 리가 없다."
            )
        )
        allContent.add(
            ContentItem(
                korean = " 생각하라 질병이라 전혀 없다\n 신의 길잡이 훈계인 거예 3-138",
                japanese = "しやんせよやまいとゆうてさらになし\n神のみちをせいけんなるぞや",
                english = "shiyan seyo yamai to yute sarani nashi\n" +
                        "Kami no michiose iken naru zo ya",
                commentary = "百三八, 제2호 二十二, 二十三 참조."
            )
        )
        allContent.add(
            ContentItem(
                korean = " 조그마한 눈병도 부스럼도\n 신경증도 아픔도 신의 인도야 3-139",
                japanese = "一寸したるめへのあしくもできものや\nのぼせいたみハ神のてびきや",
                english = "choto shitaru me no ashiku mo dekimono ya\n" +
                        "nobose itami wa Kami no tebiki ya",
                commentary = "百三九"
            )
        )
        allContent.add(
            ContentItem(
                korean = " 지금까지는 높은산이라 할지라도\n 용재가 나온 일은 없었지만 3-140",
                japanese = "いまゝでハ高い山やとゆうたとて\nよふほくみへた事ハなけれど",
                english = "imamade wa takai yama ya to yuta tote\n" +
                        "yoboku mieta koto wa nakeredo",
                commentary = ""
            )
        )
        allContent.add(
            ContentItem(
                korean = " 앞으로는 높은산에서도 차츰차츰\n 용재 찾아낼 준비를 할 거야 3-141",
                japanese = "このさきハ高山にてもたん／＼と\nよふぼくみだすもよふするぞや",
                english = "konosaki wa takayama nitemo dandan to\n" +
                        "yoboku midasu moyo suru zo ya",
                commentary = "百四一,지금까지는 상류사회 사람으로서 이 길에 이바지한 사람은 거의 없었으나, 앞으로는 그 사회에서도 용재가 될 사람을 찾아낼 준비를 한다."
            )
        )
        allContent.add(
            ContentItem(
                korean = " 온 세상을 빨리 구제할 이 준비\n 위나 아래나 모두 용솟음치게 해서 3-142",
                japanese = "いちれつにはやくたすけるこのもよふ\n上下ともに心いさめで",
                english = "ichiretsu ni hayaku tasukeru kono moyo\n" +
                        "kami shimo tomoni kokoro isamete",
                commentary = "百四二,온 세상 사람들을 빨리 돕기 위한 준비로서 어버이신은 윗사람이나 아랫사람이나 모두 용솟음치게 한다."
            )
        )
        allContent.add(
            ContentItem(
                korean = " 나날이 세상 사람들의 마음 용솟음치면\n 농작물도 모두 용솟음친다 3-143",
                japanese = "にち／＼にせかいの心いさむなら\nものゝりうけハみないさみでる",
                english = "nichinichi ni sekai no kokoro isamu nara\n" +
                        "mono no ryuke wa mina isami deru",
                commentary = "百四三,세상 사람들이 즐겁게 용솟음치면 어버이신의 마음도 용솟음치게 되고, 그로 인해 농작물도 자연히 풍작이 된다. 第一호 十二, 十三, 十四 참조"
            )
        )
        allContent.add(
            ContentItem(
                korean = " 무엇이든 구제한줄기이기 때문에\n 모반의 뿌리를 빨리 끊고 싶다 3-144",
                japanese = "なにゝてもたすけ一ちよであるからに\nむほんねへをはやくきりたい",
                english = "nani nitemo tasuke ichijo de aru karani\n" +
                        "muhon no ne o hayaku kiritai",
                commentary = "百四四,이 길은 오로지 구제한줄기의 가르침이기 때문에, 리에 항거하는 사람들이 생기지 않도록 나쁜 마음의 뿌리를 빨리 끊어주고 싶다."
            )
        )
        allContent.add(
            ContentItem(
                korean = " 지금의 길은 티끌투성이므로\n 빗자루로 청소 시작하라 3-145",
                japanese = "いまのみちほこりだらけであるからに\nほふけをもちてそうぢふしたて",
                english = "ima no michi hokori darake de aru karani\n" +
                        "hoke o mochite soji o shitate",
                commentary = "百四五,지금 나아가고 있는 길은 어버이신의 눈으로 보면 티끌투성이므로 빗자루를 가지고 청소를 하라."
            )
        )
        allContent.add(
            ContentItem(
                korean = " 다음 길은 넓고 티끌이 없으니\n 몇 사람이라도 데리고 걸어라 3-146",
                japanese = "あとなるハみちハひろくでごもくなし\nいくたりなりとつれてとふれよ",
                english = "ato naru wa michi wa hirokute gomoku nashi\n" +
                        "ikutari nari to tsurete tore yo",
                commentary = "百四六,청소를 한 길은 넓고도 개끗해서 즐겁게 걸을 수 있으니 몇 사람이라도 데리고 가라."
            )
        )
        allContent.add(
            ContentItem(
                korean = " 22의 2의 5에 이야기 시작하여\n 만가지 인연 모두 일러준다 3-147",
                japanese = "二二の二の五つにはなしかけ\nよろついんねんみなときゝかす",
                english = "nii nii no nii no itsutsu ni hanashi kake\n" +
                        "yorozu innen mina tokikikasu",
                commentary = "百四七,이것은 1874년 2월 22일 밤 5각(오후 8시경)에 집필된 노래이다. 당시 쯔지 추우사꾸는 낮에는 집안 일을 돌보고 있었는데, 이날은 이가 아파 괴로워하다가 터전에 참배를 해서 도움을 받을 생각으로 집을 나서자 금방 씻은 듯이 치통이 나았다. 그래서 추우사꾸는 고맙게 생각하며 서둘러 참배하고는 교조님께 그 경위를 아뢰자, 교조님은\n\"금방 이것을 썻어요. 잘 보아요.\"\n하고 그에게 이 노래를 보이시면서, 어버이신님의 말씀을 순순히 일러주셨다."
            )
        )
        allContent.add(
            ContentItem(
                korean = " 높은산의 설교를 듣고 진실한\n 신의 이야기를 듣고서 생각하라 3-148",
                japanese = "高山のせきゝよきいてしんしつの\n神のはなしをきいてしやんせ",
                english = "takayama no sekkyo kiite shinjitsu no\n" +
                        "Kami no hanashi o kiite shiyan se",
                commentary = "百四八,신직이나 승려들의 설교를 듣고, 또 이 길의 이야기를 들은 다음, 어느 것이 진실한 어버이신의 뜻을 전하고 있는가를 비교해서 생각해 봐야 한다."
            )
        )
        allContent.add(
            ContentItem(
                korean = " 나날이 신의 이야기를 차츰차츰\n 듣고 즐거워하라 고오끼인 거야 3-149",
                japanese = "にち／＼に神のはなしをたん／＼と\nきいてたのしめこふきなるぞや",
                english = "nichinichi ni Kami no hanashi o dandan to\n" +
                        "kiite tanoshime Koki naru zo ya",
                commentary = "百四九,나날이 진실한 어버이신의 이야기를 듣고 즐거워하라. 이 이야기야말로 언제까지나 변함없이 영원히 전해질 세계구제의 가르침인 것이다."
            )
        )
        allContent.add(
            ContentItem(
                korean = " 지금의 길은 어떤 길이라 생각하는가\n 무엇인지 모르는 길이지만 4-1",
                japanese = "",
                english = "",
                commentary = ""
            )
        )
        allContent.add(
            ContentItem(
                korean = " 눈앞에 한길이 보인다\n 저기 있던 것이 벌써 여기 다가와 4-2",
                japanese = "",
                english = "",
                commentary = "一, 二,지금 나아가고 있는 길은 좁은 길로서 사람들의 눈에는 믿음직하게 보이지 않겠지만, 그러나 벌써 눈앞에 한길이 보이고있으며, 더구나 저만치 있던 것이 금세 여기까지 다가와 있다."
            )
        )

        allContent.add(
            ContentItem(
                korean = " 이 날짜 언제라고 생각하는가\n 5월 5일에 반드시 온다 4-3",
                japanese = "",
                english = "",
                commentary = "三,교조님 친정에서 만들고 있던 신악탈이 완성되어, 교조님이 이것을 가지러 가신 1874년 6월 18일로서 음력 5월 5일에 해당한다. 한편, 같은 해 음력 4월경에는 야마자와 료오스께(山澤良助)와 야마나까 추우시찌(山中忠七) 등, 두 사람이 병으로 누워 있었다. 또 삼마이뎅 마을에 사는 마에가와 한자브로오(前川半三郞)의 부인 다끼는 당시 수족이 부자유하여 거의 앉으뱅이와 같은 상태에 놓여 있었다. 그런데 이들이 공교롭게도 5월 5일 교조님께 가르침을 받고 도움을 청하기 위해 터전에 참배를 하러 왔었는데, 어버이신님은 이 노래를 통해 미리 이것을 예언하신 것이다."
            )
        )

        allContent.add(
            ContentItem(
                korean = " 그로부터 사례참배 시작이다 이것을 보라\n 밤낮을 가리지 않게 될 거야 4-4",
                japanese = "",
                english = "",
                commentary = "四,앞으로는 어버이신의 진기한 구제를 받고 터전에 사례참배를 하러 오는 사람들이 밤낮없이 끊이지 않을 것이다."
            )
        )

        allContent.add(
            ContentItem(
                korean = " 차츰차츰 6월이 되면\n 증거수호부를 줄 것이라 생각하라 4-5",
                japanese = "",
                english = "",
                commentary = "五,증거수호부(證據守護符)란 본고장인 터전에 돌아온 사람이 출원하면 귀참한 증거로서 내려 주는 신부(神符)이다. 이것은 1874년 6월부터 내려 주게 되었다."
            )
        )

        allContent.add(
            ContentItem(
                korean = " 그로부터는 차츰차츰 역사 서둘러서\n 무엇인가 바쁘게 될 것이다 4-6",
                japanese = "",
                english = "",
                commentary = "六,여기서 역사란 나까야마가의 대문과 이에 붙은 교조님의 거처방 및 창고 건축을 말한다. 어버이신님은 이 건축을 서두르시면서, 역사가 시작되면 사람들이 많이 모여 와서 매우 바쁘게 된다고 말씀하신 것이다.(第三호 一의 주석 참조)"
            )
        )

        allContent.add(
            ContentItem(
                korean = " 이제부터 신의 마음은 나날이\n 서두르고 있다고 요량하라 4-7",
                japanese = "",
                english = "",
                commentary = ""
            )
        )

        allContent.add(
            ContentItem(
                korean = " 아무리 급히 서두르고 있을지라도\n 입으로는 아무것도 말하지 않는 거야 4-8",
                japanese = "",
                english = "",
                commentary = "七, 八,어버이신이 인류구제를 위해 아무리 서두를지라도 결코 입으로 이렇게 하라, 저렇게 하라고 지시하지는 않는다."
            )
        )

        allContent.add(
            ContentItem(
                korean = " 앞으로는 많이 모여올 사람들을\n 빨리 알려 두고 싶지만 4-9",
                japanese = "",
                english = "",
                commentary = "九,이 길이 점차 세상에 전해져 한길이 됨에 따라 많은 사람들이 터전을 그리며 찾아오게 될 터인데, 그 가운데서 용재될 사람을 곁의 사람들에게 미리 알려 두고자 하나 아마 믿지 않을 것이다."
            )
        )

        allContent.add(
            ContentItem(
                korean = " 차츰차츰 진기한 사람 보인다\n 누구의 눈에도 이것이 안 보이는가 4-10",
                japanese = "",
                english = "",
                commentary = ""
            )
        )

        allContent.add(
            ContentItem(
                korean = " 이제부터 다음 이야기를 하마 여러가지\n 길을 두고 보라 진기한 길 4-11",
                japanese = "",
                english = "",
                commentary = "十一,이제부터 다음 이야기를 일러주겠는데, 어버이신이 하는 이야기에는 결코 거짓이 없다. 차차로 "
            )
        )

        allContent.add(
            ContentItem(
                korean = " 재미있다 많은 사람 모여들어서\n 하늘의 혜택이라 말할 거야 4-12",
                japanese = "",
                english = "",
                commentary = "十二,터전의 리가 나타나서 어버이신의 의도를 알게 되면, 많은 사람들이 입을 모아 하늘의 덕을 찬양하며 그 혜택을 받고자 터전으로 참배하러 오게 된다."
            )
        )

        allContent.add(
            ContentItem(
                korean = " 나날이 몸의 장애로 또 오는구나\n 신이 기다리고 있음을 모르고서 4-13",
                japanese = "",
                english = "",
                commentary = "十三,몸에 장애를 받을 때마다 터전으로 돌아오지만 시간이 지나면 그 고마움을 잊어비리고 만다. 어버이신이 이렇게 자주 몸을 통해 알리는 것은 신의 용재로 쓰려고 기다리고 있기 때문인데, 이 마음도 모르고 멍청히 살고 있다."
            )
        )

        allContent.add(
            ContentItem(
                korean = " 차츰차츰 근행인원의 손을 갖추면\n 이것을 계기로 무엇이든 시작한다 4-14",
                japanese = "",
                english = "",
                commentary = "十四,근행인원을 차차로 갖추게 된면 그것을 계기로 만가지구제의 길을 시작한다.\n근행인원은 第六호 二十九～五十一의 주석 참조."
            )
        )

        allContent.add(
            ContentItem(
                korean = " 나날이 신의 마음을 차츰차츰\n 윗사람들의 마음에 빨리 알린다면 4-15",
                japanese = "",
                english = "",
                commentary = "十五,나날이 구제를 서두르는 어버이신의 마음을 빨리 윗사람들에게 알린다면 이 길은 세상에 널리 전파될 것이다."
            )
        )

        allContent.add(
            ContentItem(
                korean = " 윗사람들은 아무것도 모르고서 모르는자를\n 따르는 마음 이것이 안타깝다 4-16",
                japanese = "",
                english = "",
                commentary = "十六,지도청에 있는 사람들은 아무것도 모른 채 아직 어버이신의 가르침을 모르는 자의 말만 듣고 그대로 따르고 있는데, 이러한 마음자세가 참으로 안타깝기만 하다.\n모르는자는 제2호 三十四의 주석 참조."
            )
        )

        allContent.add(
            ContentItem(
                korean = " 나날이 신이 마음 서두르는 것은\n 모르는자들 마음 바꾸기를 기다린다 4-17",
                japanese = "",
                english = "",
                commentary = "十七,나날이 어버이신이 서두르고 있는 것은 아직 어버이신의 가르침을 모르는 자들도 깨끗하게 마음을 바꾸어 신의를 깨닫도록 하는 것으로서, 그날이 빨리 오기를 기다리고 있다."
            )
        )

        allContent.add(
            ContentItem(
                korean = " 지금까지 소의 병에 의한 알림을 생각해 보고\n 윗사람들 모두 조심하라 4-18",
                japanese = "",
                english = "",
                commentary = "十八,종전에 유행했던 비참한 소의 병을 잘 생각해 보라, 그때 나쁜 병이 유행했던 것은 윗사람들이 어버이신의 마음을 깨닫지 못하고 오직 인간생각에만 흘렀기 때문이니, 앞으로 모두들은 이 점을 각별히 조심하도록 하라.\n 소의 병에 의한 알림 전해 오는 말에 의하면 그해 야마또 지방에는 급성 소의 병이 유형하여 갑자기 많은 소들이 쓰러졌으며, 그 이듬해에는 사람들에게도 악성전염병이 크게 유형했다고 한다."
            )
        )

        allContent.add(
            ContentItem(
                korean = " 이것만 모두 납득하게 된다면\n 세상 사람들의 마음 모두 용솟음친다 4-19",
                japanese = "",
                english = "",
                commentary = "十九,이것만 사람들이  알게 된다면 세상 사람들의 마음은 모두 용솟음치게 된다."
            )
        )

        allContent.add(
            ContentItem(
                korean = " 무엇이든 세상 사람들의 마음 용솟음치면\n 신의 마음도 용솟음친다 4-20",
                japanese = "",
                english = "",
                commentary = ""
            )
        )

        allContent.add(
            ContentItem(
                korean = " 오늘의 길 어떤 길이라 생각하는가\n 진기한 일이 나타날 거야 4-21",
                japanese = "",
                english = "",
                commentary = "二十一,현재 걷고 있는 이 길이 어떤 길이라 생각하는가, 멍청히 걷고 있을 때가 아니다. 곧 진기한 일이 눈앞에 나타나게 된다."
            )
        )

        allContent.add(
            ContentItem(
                korean = " 차츰차츰 어떤 일도 나타난다\n 어떤 길이건 모두 즐거워하라 4-22",
                japanese = "",
                english = "",
                commentary = "二十二,모든 일이 어버이신의 의도대로 차츰 나타날 것이니, 현재는 아무리 어려운 길이라도 마음을 움츠리지 말고 장래를 낙으로 삼아 걸어가야 한다."
            )
        )

        allContent.add(
            ContentItem(
                korean = " 나날이 즐거운근행의 손짓을 익히게 되면\n 신의 즐거움 어떠하리오 4-23",
                japanese = "",
                english = "",
                commentary = ""
            )
        )

        allContent.add(
            ContentItem(
                korean = " 어서어서 근행인원을 고대한다\n 곁의 사람들의 마음은 무엇을 생각하는가 4-24",
                japanese = "",
                english = "",
                commentary = "二十三, 二十四,어버이신이 바라고 있는 즐거운근행의 손짓을 모두가 나날이 익히게 된다면, 어버이신은 얼마나 기쁘랴. 이토록 근행인원이 하루라도 빨리 모이기를 고대하고 있는 어버이신의 마음을 모른 채, 곁의 사람들은 대체 무엇을 생각하고 있는가."
            )
        )

        allContent.add(
            ContentItem(
                korean = " 원래 질병이란 없는 것이지만\n 몸의 장애 나타나는 것은 신의 소용 4-25",
                japanese = "",
                english = "",
                commentary = "二十五,원래 질병이란 이 세상에 없는 것이지만, 몸에 장애가 나타나는 것은 어버이신이 소용에 쓰려고 인도하는 것이므로 이 점을 깊이 깨닫지 않으면 안된다."
            )
        )

        allContent.add(
            ContentItem(
                korean = " 소둉도 어떤 것인지 좀처럼 몰라\n 신의 의도 태산 같아서 4-26",
                japanese = "",
                english = "",
                commentary = ""
            )
        )

        allContent.add(
            ContentItem(
                korean = " 무엇이든 신의 의도 어쨌든\n 모두 일러준다면 마음 용솟음칠 거야 4-27",
                japanese = "",
                english = "",
                commentary = ""
            )
        )

        allContent.add(
            ContentItem(
                korean = " 차츰차츰 무엇이든 의도를 일러주면\n 몸의 장애도 깨끗해진다 4-28",
                japanese = "",
                english = "",
                commentary = "二十八,차츰 어버이신이 의도를 일러줄 것인데, 이것을 사람들이 깨닫게 된다면 저절로 마음도 용솟음치고 몸도 건강하게 된다."
            )
        )

        allContent.add(
            ContentItem(
                korean = " 또 앞으로 즐거운근행 고대한다\n 무엇이냐 하면 신악근행인 거야 4-29",
                japanese = "",
                english = "",
                commentary = "二十九,또 앞으로는 하루빨리 인원을 갖추어 즐거운근행을 올려 주기를 고대하고 있는데, 그것은 어떤 근행인가 하면 신악근행이다."
            )
        )

        allContent.add(
            ContentItem(
                korean = " 온 세상에 많은 사람들이 있지만\n 신의 마음을 아는 자는 없다 4-30",
                japanese = "",
                english = "",
                commentary = ""
            )
        )

        allContent.add(
            ContentItem(
                korean = " 이번에는 신의 마음의 진실을\n 무엇이든 자세히 모두 가르칠테다 4-31",
                japanese = "",
                english = "",
                commentary = ""
            )
        )

        allContent.add(
            ContentItem(
                korean = " 무엇이든 신한줄기를 알게 되면\n 미칠곳에 못 이길 리는 없는 거야 4-32",
                japanese = "",
                english = "",
                commentary = "三十二,무슨 일이든지 신한줄기의 진실한 가르침을 참으로 깨닫게 된다면, 어버이신의 가르침을 아직 모르는 사람들의 인간생각 따위에 휘둘릴 리는 없다."
            )
        )

        allContent.add(
            ContentItem(
                korean = " 앞으로는 미칠곳과 미친곳을 빨리\n 차츰차츰 구분할 준비만을 4-33",
                japanese = "",
                english = "",
                commentary = "三十三,앞으로는 어버이신의 가르침을 아는 자와 아직 모르는 자를 구분해서 점차 온 세상 사람들의 마음을 맑힐 준비를 한다."
            )
        )

        allContent.add(
            ContentItem(
                korean = " 이것만 빨리 알게 되면\n 신의 섭섭함 풀어지리라 4-34",
                japanese = "",
                english = "",
                commentary = ""
            )
        )

        allContent.add(
            ContentItem(
                korean = " 진실한 신의 섭섭함이 풀어지면\n 세상 사람들의 마음 모두 용솟음친다 4-35",
                japanese = "",
                english = "",
                commentary = "三十四, 三十五,이것만 사람들이 잘 깨닫게 되면 어버이신의 섭섭한 마음도 풀려 활짝 개게 될 것이고, 그에 따라 세상 사람들의 마음도 저절로 용솟음치게 될 것이다."
            )
        )

        allContent.add(
            ContentItem(
                korean = " 차츰차츰 온 세상을 진실하게\n 구제할 준비만을 하는 거야 4-36",
                japanese = "",
                english = "",
                commentary = "三十六,어버이신은 모든 인간알 진실로 구제하고자 하는 일념뿐이기 때문에 이제부터는 오로지 구제할 준비만 한다.\n구제할 준비란 감로대를 세워 구제근행인 감로대근행을 올릴 준비를 말한다.(다음 노래 참조)"
            )
        )

        allContent.add(
            ContentItem(
                korean = " 그 후로는 앎지 않고 죽지 않고 쇠하지 않고\n 마음에 따라 언제까지나 살리라 4-37",
                japanese = "",
                english = "",
                commentary = "三十七,그 후로는 질병으로 신음하지도 않고 죽지도 않고 노쇠하지도 않고 언제까지나 제 생긱대로 즐거운 삶을 누리게 된다.\n 언제까지나는 第三호 百의 주석 참조."
            )
        )

        allContent.add(
            ContentItem(
                korean = " 또 앞으로 연수가 지나게 되면\n 늙어지는 것은 전혀 없는 거야 4-38",
                japanese = "",
                english = "",
                commentary = "三十八,그 뿐만 아니라, 앞으로 차츰 연수가 지나 사람들의 마음이 맑아지면 언제까지나 늙지 안고 원기왕성하게 활동할 수 있도록 수호한다."
            )
        )

        allContent.add(
            ContentItem(
                korean = " 지금까지는 아무것도 몰랐었다\n 이제부터 앞으로 모두 가르칠 테다 4-39",
                japanese = "",
                english = "",
                commentary = ""
            )
        )

        allContent.add(
            ContentItem(
                korean = " 지금까지는 모두들의 마음과 안의\n 마음이 크게 달랐었지만 4-40",
                japanese = "",
                english = "",
                commentary = ""
            )
        )

        allContent.add(
            ContentItem(
                korean = " 내일부터는 무엇이든 부탁할 테니\n 신의 말대로 따르지 않으면 안돼 4-41",
                japanese = "",
                english = "",
                commentary = "四十, 四十一,지금까지는 이 길 안의 사람들의 마음과 밖의 사람들의 마음이 크게 달랐지만, 앞으로는 다같이 어버이신의 말대로 따르지 않으면 안된다.\n 당시는 본교가 초창기였으므로 이 길 안의 사람이나 밖의 사람이나 어버이신님이 구제한줄기와 근행을 서두르시는 뜻을 충분히 이해하지 못했고, 또 양쪽 의견도 서로 달랐다 그래서 앞으로는 이러한 인간마음을 버리고 다 같이 어버이신님의 말씀대로 구제한줄기에 매진하지 않으면 안된다고 말씀하신 것이다."
            )
        )

        allContent.add(
            ContentItem(
                korean = " 나날이 몸의 장애로 납득하라\n 마음 틀린 바를 신이 알린다 4-42",
                japanese = "",
                english = "",
                commentary = ""
            )
        )

        allContent.add(
            ContentItem(
                korean = " 각자의 몸으로부터 생각해서\n 마음작정하고 신에게 의탁하라 4-43",
                japanese = "",
                english = "",
                commentary = ""
            )
        )

        allContent.add(
            ContentItem(
                korean = " 무엇이든 신의 의도 깊은 것이니\n 곁의 사람들은 그것을 모르고서 4-44",
                japanese = "",
                english = "",
                commentary = "四十四,곁의 사람들에게 신상으로 알리는 것은 어버이신의 깊은 의도가 있기 때문이다. 따라서 이것을 단순한 질병이라고만 생각하지 말고 그 근본을 깨달아야 할 것이다."
            )
        )

        allContent.add(
            ContentItem(
                korean = " 오늘까지는 어떤 길도 보이지 않지만\n 곧 보일 것이니 마음작정을 하라 4-45",
                japanese = "",
                english = "",
                commentary = "四十五,지금까지는 어떤 길도 아직 보이지 않았기 때문에 이 진실한 가르침을 깊이 믿는 자가 없었으나, 이제부터는 마음쓰는대로 곧 리가 나타날 것이니 잘 생각해서 마음 작정을 해야 한다."
            )
        )

        allContent.add(
            ContentItem(
                korean = " 이 길을 속히 알리려고 생각하지만\n 깨달음이 억어서는 이것이 어렵다 4-46",
                japanese = "",
                english = "",
                commentary = "四十六,이 구제한줄기의 길을 빨리 세상에 알려서 즐거운 삶을 누리게 하고 싶지만, 인간 마음이 방해가 되어 어버이신의 뜻을 깨닫지 못하기 때문에 그것을 실현하기가 매우 어렵다."
            )
        )

        allContent.add(
            ContentItem(
                korean = " 차츰차츰 붓으로 알려 두었지만\n 깨달음이 없는 것이 신의 섭섭함 4-47",
                japanese = "",
                english = "",
                commentary = "四十七,차츰차츰 장래의 일을 이 붓끝으로 알려 주고 있는데도 곁의 사람들이 그 뜻을 전혀 깨닫지 못하니, 어버이신으로서는 이 점이 참으로 안타까운 일이다."
            )
        )

        allContent.add(
            ContentItem(
                korean = " 무엇이든 신이 하는 말 단단히 들어라\n 모두가 각자의 마음 나름이야 4-48",
                japanese = "",
                english = "",
                commentary = ""
            )
        )

        allContent.add(
            ContentItem(
                korean = " 진실로 마음 용솟음치며 생각해서\n 신에게 의탁하여 즐거운근행을 4-49",
                japanese = "",
                english = "",
                commentary = "四十九,진실로 마음 용솟음치며 어버이신의 가르침을 잘 깨닫는 한편, 매사에 인간생각을 버리고 어버이신에게 의탁해서 즐거운 근행을 해야 한다."
            )
        )

        allContent.add(
            ContentItem(
                korean = " 이 이야기 다른 일로 생각 말라\n 오직 거름에 대한 이야기인 거야 4-50",
                japanese = "",
                english = "",
                commentary = "五十,지금 이야기하고자 하는 것은 다른 것이 아니라, 오직 거름에 대한 이야기일 뿐이다.\n 터전에서 올리는 만가지구제의 근행가운데 거름의 근행이 있다. 이 거름의 근행은 한번에 겨 서말, 재 서말, 흙 서말을 혼합한 것을 감로대에 올려 기원하는 것인데, 이때 금비 4천 관에 상당하는 거름의 수호를 받게 된다. 그런 다음, 원하는 사람에게 필요한 분량만큼 내려 주는데, 받은 사람은 이것을 자기 전답에 뿌리는 것이다. 한편, 거름의 수훈이란 겨 서홉, 재 서홉, 흙 서홉을 신전에 올리고 거름의 수훈을 받은 사람이 이 수훈을 전한 다음 전답에 뿌리면 금비 40관에 상당하는 효과가 있다고 가르치셨다."
            )
        )

        allContent.add(
            ContentItem(
                korean = " 거름이라 하여 다른 것이 효과 있다 생각 말라\n 마음의 성진실이 효과인 거야 4-51",
                japanese = "",
                english = "",
                commentary = "五十一,거름이라 해도 겨, 재, 흙 그 자체가 효과를 내는 것이 아니라, 진심으로 어버이신에게 의탁하는 그 마음이 어버이신에게 통함으로써 효과가 나타나는 것이다."
            )
        )

        allContent.add(
            ContentItem(
                korean = " 진실한 마음을 살펴본 뒤에는\n 어떤 수호도 한다고 생각하라 4-52",
                japanese = "",
                english = "",
                commentary = ""
            )
        )

        allContent.add(
            ContentItem(
                korean = " 단단히 들어라 만가지 일을 다 가르쳐\n 어디나 차별은 전혀 없는 거야 4-53",
                japanese = "",
                english = "",
                commentary = ""
            )
        )

        allContent.add(
            ContentItem(
                korean = " 어떤 곳에서 오는 사람이라도\n 모두 인연이 있기 때문에 4-54",
                japanese = "",
                english = "",
                commentary = "五十四,어떤 곳에서도 많은 사람들이 터전을 그리며 돌아오게 될 터인데, 그것은 그들이 모두 진실한 어버이신의 자녀이기 때문에 그 인연으로 해서 귀참하는 것인 만큼, 결코 우연이라 생각해서는 안된다."
            )
        )

        allContent.add(
            ContentItem(
                korean = " 인간을 창조한 집터이니라\n 그 인연으로써 하강했다 4-55",
                japanese = "",
                english = "",
                commentary = "五十五,터전은 인간이 잉태된 본고장이다. 그러한 안태본(安胎本)의 인연으로 해서 어버이신이 이곳에 하강한 것이다.\n 이것이 곧 본교에서 말하는 터전의 인연(집터의 인연)이다. 본교의 발상(發祥)은 이 터전의 인연과 순각한의 인연 및 교조 혼의 인연 등, 이 세 가지 인연이 일치함으로써 비롯된 것이다."
            )
        )

        allContent.add(
            ContentItem(
                korean = " 앞으로는 온 세상을 한결같이\n 구제할 길을 모두 가르칠 테다 4-56",
                japanese = "",
                english = "",
                commentary = ""
            )
        )

        allContent.add(
            ContentItem(
                korean = " 차츰차츰 만가지 구제를 모두 가르쳐\n 미칠곳과 미친곳을 구분할 뿐이야 4-57",
                japanese = "",
                english = "",
                commentary = ""
            )
        )

        allContent.add(
            ContentItem(
                korean = " 나날이 미칠곳과 미친곳을 구분하는 길\n 이것이 신이 서두르는 한결같은 마음 4-58",
                japanese = "",
                english = "",
                commentary = "五十七, 五十八,점차 만가지구제의 근행을 가르치고 리를 일러주어서, 어버이신의 가르침을 아는 자와 아직 모르는 자를 구분해서 밝힌다. 어버이신이 한결같은 마음으로 서두르고 있는 것은 바로 이것뿐이다. \n제2호 三十四의 주석 참조."
            )
        )

        allContent.add(
            ContentItem(
                korean = " 이 길을 빨리 구분하게 되면\n 나머지 만가지는 신의 마음대로야 4-59",
                japanese = "",
                english = "",
                commentary = "五十九,이것만 빨리 구분해서 밝히게 되면 그 다음에는 만사를 어버이신이 자유자재로 수호한다."
            )
        )

        allContent.add(
            ContentItem(
                korean = " 오늘은 무엇인가 진기한 일을 시작하므로\n 만가지 인연 모두 따라온다 4-60",
                japanese = "",
                english = "",
                commentary = "六十,이번에는 어떻든 진기한 구제한줄기의 길을 시작하여 이 세상 태초의 진실을 일러준다. 그러면 사람들은 모두 어버이신의 자녀임을 깨닫고 이 길을 따라온다."
            )
        )

        allContent.add(
            ContentItem(
                korean = " 인연도 많은 사람이기 때문에\n 어디나 차별이 있다고 생각 말라 4-61",
                japanese = "",
                english = "",
                commentary = "六十一,가지각색의 인연으로 말미암아 서로 인종이 다르고 처지가 다르긴 하나 그렇더라도 어버이신의 수호에 차별이 있는 것은 아니다. 그리고 나라나 사람에 따라서도 차별하지 않는다. 공평무사야말로 어버이신의 마음이다."
            )
        )

        allContent.add(
            ContentItem(
                korean = " 이 세상을 창조한 신이기에\n 온 세상 사람들이 모두 다 자녀다 4-62",
                japanese = "",
                english = "",
                commentary = ""
            )
        )

        allContent.add(
            ContentItem(
                korean = " 온 세상 자녀가 귀엽기 때문에\n 여러가지로 마음 기울이는 거야 4-63",
                japanese = "",
                english = "",
                commentary = ""
            )
        )

        allContent.add(
            ContentItem(
                korean = " 이 자녀에게 무엇이든 가르쳐서 어서어서\n 신이 마음 서두르고 있음을 보라 4-64",
                japanese = "",
                english = "",
                commentary = "六十二～六十四,온 세상 사람들은 똑같이 어버어신의 자녀이다. 그러므로 이들에게 무엇이든 가르쳐서 즐겁게 살도록 하려고 서두르고 있는 어버이신의 마음을 너희들은 알아야 한다."
            )
        )

        allContent.add(
            ContentItem(
                korean = " 차츰차츰 자녀의 성인을 고대한다\n 신의 의도 이것뿐이다 4-65",
                japanese = "",
                english = "",
                commentary = "六十五,자녀의 성인이란 어버이신님의 자녀인 인간이 점차 어버이신님으로부터 가르침을 받아 욕심을 잊고 어버이신님의 의도를 잘 깨닫게 되는 것을 말한다."
            )
        )

        allContent.add(
            ContentItem(
                korean = " 자녀만 어서 세상에 나가게 되면\n 미칠곳을 미친곳의 땅으로 하리라 4-66",
                japanese = "",
                english = "",
                commentary = "六十六,사람들의 신앙이 향상되어 널리 구제한줄기의 활동을 하게만 된다면, 지금까지 어버이신의 뜻을 모르던 곳까지도 어버이신의 구제한줄기의 길을 빠짐억이 전하여 모두가 평화롭고 즐거운 삶을 누릴 수 있도록 수호할 것이다. 이것이야말로 어버이신의 이상이다.\n第2호 三十四의 주석 참조. "
            )
        )

        allContent.add(
            ContentItem(
                korean = " 진실로 자녀들은 마음 단단히 가져라\n 신의 마음은 서두름뿐이야 4-67",
                japanese = "",
                english = "",
                commentary = "六十七,자녀들아, 진실로 마음을 단단히 정해서 어버이신을 따라오라. 어버이신은 오직 구제한줄기만을 서두르고 있을 뿐이다."
            )
        )

        allContent.add(
            ContentItem(
                korean = " 나날이 신이 서두르고 있는 이 고민\n 어서 구제할 준비를 해 다오 4-68",
                japanese = "",
                english = "",
                commentary = "六十八,나날이 어버이신이 온 세상 사람들을 구제하기 위해 서두르고 있는 이 마음을 헤아려서, 어서 만가지구제의 준비를 서둘러 다오."
            )
        )

        allContent.add(
            ContentItem(
                korean = " 안의 사람은 윗사람 때문에 침울하고 있다\n 두려울 것 없다 신이 맡았으니 4-69",
                japanese = "",
                english = "",
                commentary = "六十九,당시 내부 사람들은 관헌의 탄압이 두려워 마음이 움츠리고 있었는데, 어버이신님은 그들에게 결코 두려워할 것 없다. 신이 단단히 맡아 줄 것이니 안심하고 매진하라고 말씀하신 것이다."
            )
        )

        allContent.add(
            ContentItem(
                korean = " 지금까지와는 길이 바뀌었으니\n 속히 서둘러서 한길을 내도록 4-70",
                japanese = "",
                english = "",
                commentary = ""
            )
        )

        allContent.add(
            ContentItem(
                korean = " 이 길은 언제 이루어지리라 생각하는가\n 빨리 나와 보라 벌써 눈앞에 다가와 4-71",
                japanese = "",
                english = "",
                commentary = "七十一,언제 한길로 나가게 될 것인가고 궁금하게 여기겠지만, 그것은 먼 장래의 일이 아니다. 당장이라도 눈앞에 나타난다."
            )
        )

        allContent.add(
            ContentItem(
                korean = " 차츰차츰 붓으로 알려 두었으니\n 어서 마음에 깨닫도록 하라 4-72",
                japanese = "",
                english = "",
                commentary = ""
            )
        )

        allContent.add(
            ContentItem(
                korean = " 이것만 빨리 깨닫게 되면\n 몸의 괴로움도 깨끗이 없어진다 4-73",
                japanese = "",
                english = "",
                commentary = "七十二, 七十三,붓으로 이미 여러 가지 알려 두엇으니 이것을 어서 깨닫도록 하라. 어버이신의 마음만 깨닫게 되면 몸의 장애도 도움받고 마음도 깨끗이 맑아진다."
            )
        )

        allContent.add(
            ContentItem(
                korean = " 근행도 시작은 손춤 또 신악\n 조금 좁은 길 내어 두었지만 4-74",
                japanese = "",
                english = "",
                commentary = ""
            )
        )

        allContent.add(
            ContentItem(
                korean = " 차츰차츰 잡초가 우거져 길을 모르니\n 속히 본길 낼 준비를 4-75",
                japanese = "",
                english = "",
                commentary = "七十四, 七十五,어버이신님은 처음에는 손춤을, 다음에는 신악근행을, 이렇게 차츰 가르치면서 빨리 즐거운근행을 올리기를 바라고 계셨으나, 당시 신자들 가운데는 관헌의 압박과 간섭 때문에 불안을 느끼고 신앙을 그만둘 생각을 하거나, 또는 태만한 마음에서 근행을 게을리하고 있었는데, 이것은 마치 좁은 길에 잡초가 우거져 길을 덮어 버린 것과 같은 이치로서, 이래서는 이 길이 늦어질 뿐이므로 빨리 본길로 나갈 준비를 하라고 말씀하신 것이다."
            )
        )

        allContent.add(
            ContentItem(
                korean = " 나날이 마음 용솟음치며 서둘러라\n 속히 본길을 내게 되면 4-76",
                japanese = "",
                english = "",
                commentary = ""
            )
        )

        allContent.add(
            ContentItem(
                korean = " 진실로 이 본길을 내게 되면\n 장래에는 오로지 즐거움만 넘칠 거야 4-77",
                japanese = "",
                english = "",
                commentary = ""
            )
        )

        allContent.add(
            ContentItem(
                korean = " 마을 사람들을 구제하고자 더욱 서두르고 있다\n 어서 깨달아 주도록 4-78",
                japanese = "",
                english = "",
                commentary = "七十八,특히 마을 사람들을 하루빨리 구제하고자 어버이신은 서두르고 있으니, 이 마음을 속히 깨달아 다오.\n 마을 사람들이란 당시 쇼야시까 마을(현재 교회본부 소재지) 사람들을 말한다."
            )
        )

        allContent.add(
            ContentItem(
                korean = " 온 세상 신에게는 모두 다 자녀\n 사람들은 모두 어버이로 생각하라 4-79",
                japanese = "",
                english = "",
                commentary = ""
            )
        )

        allContent.add(
            ContentItem(
                korean = " 온 세상에서는 설교랍시고 시작해서\n 일러주니 들어러 가다 4-80",
                japanese = "",
                english = "",
                commentary = ""
            )
        )

        allContent.add(
            ContentItem(
                korean = " 아무리 보이는 것을 말할지라도\n 근본을 모르면 알지 못하리 4-81",
                japanese = "",
                english = "",
                commentary = "八十, 八十一,세상에서는 설교랍시고 사람이 행해야 할 길을 일러주고, 또 이것을 들으러 가는 사람도 많다. 그러나 눈에 보이는, 이미 알고 있는 일만을 설교할 뿐 태초의 인연에 관해서는 일러주지 않기 때문에, 사람들이 마음속으로 납득할 리가 없다. "
            )
        )

        allContent.add(
            ContentItem(
                korean = " 차츰차츰 없던 일만 말해 두고서\n 그것 나타나면 이것이 진실이야 4-82",
                japanese = "",
                english = "",
                commentary = "八十二,어버이신은 눈에 안 보이는 일이나 장래 있을 일만을 말하기 때문에 듣는 사람 가운데는 의심하는 사람도 있으나, 그것이 사실로 나타나게 되면 그때는진실임을 믿게 될 것이다."
            )
        )

        allContent.add(
            ContentItem(
                korean = " 한결같이 신에 의탁하는 이 자녀\n 어서 세상에 나갈 준비를 하라 4-83",
                japanese = "",
                english = "",
                commentary = "八十三,육친의 어버이를 의지하듯, 어버이신에게 의탁하여 따라오는 자녀들아, 빨리 밝은 길로 나갈 준비를 하라."
            )
        )

        allContent.add(
            ContentItem(
                korean = " 진실로 세상에 나가려거든\n 마음을 가다듬어 중심을 찾아라 4-84",
                japanese = "",
                english = "",
                commentary = "八十四,진실로 밝은 길로 나가려고 생각한다면 마음을 가다듬어 중심을 찾아라\n 중심이란 이 길의 진수(眞髓)로서 사람인 경우 교조님을 뜻하며, 장소인 경우에는 집터의 중심이 되는 터전을 가리키는데, 터전이 정해진 것은 1875년 터전 결정에 의해서이다."
            )
        )

        allContent.add(
            ContentItem(
                korean = " 이 자녀의 진실한 가슴속을\n 살펴본 뒤 어떤 준비도 4-85",
                japanese = "",
                english = "",
                commentary = "八十五,어버이신을 그리며 도움받기를 원하는 자녀들의 진실한 마음을 살펴본 다음, 구제를 위한 어떠한 준비도 해 주리라."
            )
        )

        allContent.add(
            ContentItem(
                korean = " 나날이 신의 마음은 서둘러도\n 자녀의 마음으로는 아는 바 없으므로 4-86",
                japanese = "",
                english = "",
                commentary = ""
            )
        )

        allContent.add(
            ContentItem(
                korean = " 자녀도 적은 수가 아닌 만큼\n 많은 사람 가슴속이라 더욱 모른다 4-87",
                japanese = "",
                english = "",
                commentary = "八十六, 八十七,어버이신은 나날이 구제한줄기의 길을 서두르고 있으나, 자녀들은 이것을 모르니 참으로 안타깝다. 자녀도 적은 수가 아닌 많은 사람이 어버이신의 마음을 모르므로 세상은 더욱 혼동할 수 밖에 없6ㅏ."
            )
        )

        allContent.add(
            ContentItem(
                korean = " 지금까지는 학문 등을 말할지라도\n 나타나지 않은 일은 결코 모르겠지 4-88",
                japanese = "",
                english = "",
                commentary = ""
            )
        )

        allContent.add(
            ContentItem(
                korean = " 앞으로는 나타나지 않은 일 차츰차츰\n 만가지 일을 모두 일러둔다 4-89",
                japanese = "",
                english = "",
                commentary = ""
            )
        )

        allContent.add(
            ContentItem(
                korean = " 이제부터는 이 세상 창조 이래 없던 근행\n 차츰차츰 가르쳐 손짓을 익히게 한다 4-90",
                japanese = "",
                english = "",
                commentary = "九十,이제부터는 이 세상 창조 이래 일찍이 본 적이 없는 즐거운근행을 차츰 가르치고 손짓도 익히게 하여 이것을 널리 펴나가리라."
            )
        )

        allContent.add(
            ContentItem(
                korean = " 이 근행을 온 세상을 구제하는 길\n 벙어리도 말하게 하리라 4-91",
                japanese = "",
                english = "",
                commentary = "九十一,즐거운근행이야말로 온 세상 사람들을 구제하는 진실한 길로서, 이 근행을 통해 벙어리도 말할 수 있도록 신기한 구제를 한다."
            )
        )

        allContent.add(
            ContentItem(
                korean = " 나날이 근행인원 단단히 하라\n 마음을 가다듬어 빨리 손짓을 익혀라 4-92",
                japanese = "",
                english = "",
                commentary = "九十二,나날이 근행인원들은 단단히 하라. 마음을 가다듬어 즐거운근행의 손짓을 빨리 익히도록 하라."
            )
        )

        allContent.add(
            ContentItem(
                korean = " 이 근행 어떤 것이라 생각하는가\n 세상을 안정시켜 구제할 뿐이다 4-93",
                japanese = "",
                english = "",
                commentary = "九十三,이 즐거운근행은 무엇 때문에 하는 것이라 생각하는가. 온 세상 사람들의 마음을 깨끗이 맑혀서 구제하고 세상을 안정시키려는 일념에서 어버이신이 시작한 근행이다."
            )
        )

        allContent.add(
            ContentItem(
                korean = " 이 길이 확실히 나타난다면\n 질병의 뿌리는 끊어져 버린다 4-94",
                japanese = "",
                english = "",
                commentary = "九十四,이 구제한줄기의 길이 빨리 세상에 나타나서 사람들이 믿게 되면, 질병의 근본이 되는 마음의 티끌이 털려 이 세상은 즐거운 삶의 세계가 된다."
            )
        )

        allContent.add(
            ContentItem(
                korean = " 진실한 마음에 따라 누구에게나\n 어떤 수호도 안 한다고는 말하지 않아 4-95",
                japanese = "",
                english = "",
                commentary = "九十五,진실한 마음에 따라 누구에게나 차별없이 어떤 수호도 나타내 준다."
            )
        )

        allContent.add(
            ContentItem(
                korean = " 지금의 길 신의 서두름 안의 사람들\n 염려할 것 없다 단단히 두고 보라 4-96",
                japanese = "",
                english = "",
                commentary = "九十六,이 길을 빨리 세상에 펴려고 서두르고 있는 데 대해 내부 사람들은 이러니저러니하며 염려들을 하고 있으나, 결코 걱정할 것 없다. 앞으로 되어가는 모습을 단단히 두고 보라."
            )
        )

        allContent.add(
            ContentItem(
                korean = " 이제까지와는 길이 바뀐다고 말해 두었다\n 신은 틀린 말은 하지 않아 4-97",
                japanese = "",
                english = "",
                commentary = ""
            )
        )

        allContent.add(
            ContentItem(
                korean = " 앞으로는 신의 서두르는 마음을\n 입으로는 아무래도 말하려야 말할 수 없다 4-98",
                japanese = "",
                english = "",
                commentary = ""
            )
        )

        allContent.add(
            ContentItem(
                korean = " 아무리 어려운 일이라 할지라도\n 일러주지 않고서는 알 수가 없다 4-99",
                japanese = "",
                english = "",
                commentary = "九十八, 九十九,온 세상 사람들을 구제하려고 서두르는 어버이신의 마음을 말로써는 아무래도 다 설명하기가 어렵다. 그러나 아무리 어렵다 할지라도 일러주지 않으면 아무도 모를 것이다."
            )
        )

        allContent.add(
            ContentItem(
                korean = " 나날이 신의 의도 차츰차츰\n 일러줄 테니 이것 들어 다오 4-100",
                japanese = "",
                english = "",
                commentary = "百,그래서 좀처럼 입으로써는 다 말할 수 없지만, 점차로 어버이신의 의도를 일러줄 테니, 이것을 듣고 모두 잘 생각하라."
            )
        )

        allContent.add(
            ContentItem(
                korean = " 이 길은 무엇인가 어렵고 진기한\n 길인 만큼 단단히 두고 보라 4-101",
                japanese = "",
                english = "",
                commentary = "百一,이 길은 쉽사리 알기 어려운 길이긴 하나 세상에 빌할 데 없는 참으로 훌룡한 가르침인 만큼, 어버이신이 일러둔 것은 반드시 사실로 나타날 것이니 장래를 단단히 두고 보라"
            )
        )

        allContent.add(
            ContentItem(
                korean = " 이 길을 헤쳐나가면 그 다음은\n 미칠곳을 미친곳의 땅으로 하리라 4-102",
                japanese = "",
                english = "",
                commentary = "百二,이 길을 헤쳐 나가 사람들의 마음이 맑아지면, 그 다음에는 어버이신의 뜻을 아직 알지 못하는 곳에 구제한줄기의 어버이마음을 빠짐없이 전하도록 하라. 이미 신의 혜택을 풍성하게 입도록 해 두었다."
            )
        )

        allContent.add(
            ContentItem(
                korean = " 미칠곳의 땅을 미친곳의 땅으로 하면\n 이것은 영원히 이어져 나간다 4-103",
                japanese = "",
                english = "",
                commentary = "百三,어버이신의 가르침이 아직 미치지 않은 곳에 빠짐억이 이 길이 전해져 온 세상 사람들이 어버이신의 구제한줄기의 마음을 깨닫게 되면, 거기서 오는 기쁨은 이 지상에 넘칠 것이고, 그것은 또한 영원히 변함없이 이어져 나가게 된다.\n제2호 三十四의 주석 참조."
            )
        )

        allContent.add(
            ContentItem(
                korean = " 이 세상을 다스리는 것도 위 하늘도 위\n 윗사람과 신의 마음 구분하리라 4-104",
                japanese = "",
                english = "",
                commentary = "百四,이 세상을 다스리는 사람도 위라 하고, 어버이신도 위라 하는데, 비록 말은 같을지라도 그 마음은 반다시 같다고 할 수 없다. 그것은 세상을 다스리는 사람의 마음이나 어버이신의 마음이 사람들의 행복을 염원하는 점에 있어서는 같지만, 장래를 내다보고 못 보는 점에 있어서는 다르기 때문이다. 이 점을 분명히 구분하겠다."
            )
        )

        allContent.add(
            ContentItem(
                korean = " 차츰차츰 보이지 않는 것을 말해 두고서\n 장차 보이면 이것이 신이야 4-105",
                japanese = "",
                english = "",
                commentary = "百五,현재로는 사람들의 눈에 보이지 않는 것을 차차로 말해 두고서, 그것이 장래에 실현된다면 이것이 곧 어버이신의 이야기인 증거이다."
            )
        )

        allContent.add(
            ContentItem(
                korean = " 아무리 보이는 것을 말할지라도\n 장차 안 보이면 안다고 할 수 없어 4-106",
                japanese = "",
                english = "",
                commentary = "百六,아무리 현재의 일을 자신 있게 말할지라도, 장래에 말한 바가 그대로 나타나지 않는다면 참으로 만사를 알 고 있다고는 할 수 없다."
            )
        )

        allContent.add(
            ContentItem(
                korean = " 이제부터는 온 세상 사람들의 가슴속\n 위나 아래나 모두 깨닫게 할 테다 4-107",
                japanese = "",
                english = "",
                commentary = "百七,이제부터는 윗사람이나 아랫사람이나 차별 없이 시비선악(是非善惡)을 밝혀서 어버이신의 뜻을 깨닫도록 할 것이다."
            )
        )

        allContent.add(
            ContentItem(
                korean = " 이것을 보라 세상이나 안이나 차별 없다\n 가슴속부터 청소할 거야 4-108",
                japanese = "",
                english = "",
                commentary = "百八,이를 위해서 세상 사람들이나 내부 사람들이나 차별 없이 모두 깨끗이 마음을 청소하게 할 테니 잘 두고 보라."
            )
        )

        allContent.add(
            ContentItem(
                korean = " 이 청소 어려운 일이지만\n 질병이라는 것은 없다고 말해 둔다 4-109",
                japanese = "",
                english = "",
                commentary = "百九,이처럼 온 세상 사람들의 마음을 청소하기는 어려운 일이지만, 그러나 신상을 통해 그것을 해 보이겠다. 그러므로 신상이 나타나더라도 그것은 질병이 아니라 어버이신의 인도이며 꾸지람이란 것을 미리 일러둔다."
            )
        )

        allContent.add(
            ContentItem(
                korean = " 어떤 아픔도 괴로움도 부스럼도\n 열도 설사도 모두가 티끌이니라 4-110",
                japanese = "",
                english = "",
                commentary = "百十,어떤 아픔도, 괴로움도, 부스럼도, 열도, 설사도 모두 질병이 아니라 마음의 티끌이다."
            )
        )

        allContent.add(
            ContentItem(
                korean = " 이 세상을 창조한 이래 아무것도\n 윗사람에게 가르친 일은 없으리라 4-111",
                japanese = "",
                english = "",
                commentary = "百十一,태초에 어버이신이 없던 세계 없던 인간을 창조한 이래, 지금까지 윗사람들에게 어버이신의 의도를 일러준 일이 없다."
            )
        )

        allContent.add(
            ContentItem(
                korean = " 이번에는 무엇이든 만가지를 윗사람에게\n 알려 주게 되면 4-112",
                japanese = "",
                english = "",
                commentary = ""
            )
        )

        allContent.add(
            ContentItem(
                korean = " 그러면 더러는 생각하는 사람도 있겠지\n 모두 모여들어 이야기하면 4-113",
                japanese = "",
                english = "",
                commentary = ""
            )
        )

        allContent.add(
            ContentItem(
                korean = " 그중에는 진실로 마음 든든하다고\n 생각하는 사람도 있을 거야 4-114",
                japanese = "",
                english = "",
                commentary = "百十二～百十四,그러나 이제는 이미 시기가 다가왔으므로 인간창조 이래의 어버이신의 의도를 모두 윗사람들에게 가르쳐 주려고 한다. 그러면 개중에는 이에 대해 깊이 생각해 보는 사람도 있을 것이고, 또 여러 사람이 모여 와서 서로 이야기를 하다 보면 그 가운데는 과연 이 길은 진실로 믿음직한 가르침이라고 깨닫는 사람도 있을 것이다."
            )
        )

        allContent.add(
            ContentItem(
                korean = " 이 길이 윗사람에게 통하게 된다면\n 신의 자유자재 곧 나타내리라 4-115",
                japanese = "",
                english = "",
                commentary = "百十五,이 길이 윗사람들에게 이해되기만 한다면 어버이신은 자유자재한 섭리를 즉시 나타내 보이겠다."
            )
        )

        allContent.add(
            ContentItem(
                korean = " 이 세상을 창조한 신의 자유자재를\n 보여 준 일은 전혀 없으므로 4-116",
                japanese = "",
                english = "",
                commentary = ""
            )
        )

        allContent.add(
            ContentItem(
                korean = " 무엇이든 모르는 동안은 그대로야\n 신의 자유자재 알린다면 4-117",
                japanese = "",
                english = "",
                commentary = ""
            )
        )

        allContent.add(
            ContentItem(
                korean = " 이것을 듣고 모든 사람든은 생각하라\n 무엇이든 만가지는 마음 나름이야 4-118",
                japanese = "",
                english = "",
                commentary = "百十六～百十八,이 세상을 창조한 어버이신의 자유자재한 섭리를 지금까지 알린 적도 보인 적도 전혀 없으므로, 사람들이 모르는 것도 무리는 아니다. 그러므로 모르고 있는 동안은 그런대로 좋지만, 일단 어버이신이 이 세상에 나타나 자유자재한 섭리를 해 보일 때는 그것을 보고 세상 사람들은 모두 깊이 생각해야 한다. 신상이나 사정 등, 만사는 모두 각자의 마음가짐에 따른 어버이신의 수호이다."
            )
        )

        allContent.add(
            ContentItem(
                korean = " 오늘은 아무것도 보이지 않지만\n 6월에 가서 보라 모두 나타나리라 4-119",
                japanese = "",
                english = "",
                commentary = "百十九,지금으로서는 아무것도 보이지 않지만 6월에 가서 보라. 진기한 일이 나타날 것이다.\n 1874년부터 6월부터 증거수호부를 내려주셨다.(第四호 五의 주석 참조)"
            )
        )

        allContent.add(
            ContentItem(
                korean = " 지금까지는 높은산이라 뽐내고 있다\n 골짜기에서는 위축되어 있을 뿐 4-120",
                japanese = "",
                english = "",
                commentary = "百二十,지금까지 상류층 사람들은 제멋대로 뽐내고 있고, 하류층 사람들은 짓눌려서 위축되어 있다."
            )
        )

        allContent.add(
            ContentItem(
                korean = " 이제부터는 높은산이나 골짜기나\n 태초의 이야기를 일러주리라 4-121",
                japanese = "",
                english = "",
                commentary = "百二一,자, 이제부터는 상류층 사람들에게도 하류층 사람들에게도 다같이 태초의 이야기를 확실히 일러주겠다.\n 태초에 창조시에는 모든 인간이 차별 없이 다 똑같았지만, 나중에 상류층 사람으로 태어나거나 하류층 사람으로 태어나게 된 것은 모두 각자의 전생 인연 때문이다."
            )
        )

        allContent.add(
            ContentItem(
                korean = " 이 세상을 창조한 태초는 진흙바다\n 그 가운데는 미꾸라지뿐이었다 4-122",
                japanese = "",
                english = "",
                commentary = ""
            )
        )

        allContent.add(
            ContentItem(
                korean = " 이 미꾸라지를 무엇이라 생각하는가\n 이것이 인간의 씨앗인 거야 4-123",
                japanese = "",
                english = "",
                commentary = ""
            )
        )

        allContent.add(
            ContentItem(
                korean = " 이것을 신이 끌어올려 먹고 나서\n 차츰차츰 수호하여 인간으로 삼아 4-124",
                japanese = "",
                english = "",
                commentary = ""
            )
        )

        allContent.add(
            ContentItem(
                korean = " 그로부터 신의 수호는\n 여간한 것이 아니었던 거야 4-125",
                japanese = "",
                english = "",
                commentary = "百二二～百二五,이 세상 태초는 진흙바다와 같은 혼돈한 상태였으며, 그 가운데 미꾸라지가 많이 살고 있었다. 어버이신은 이 미꾸라지를 끌어올려 모두 먹고 그 마음씨를 알아본 다음, 이것들을 씨앗으로 삼아 차츰 수호하여 인간을 창조했다. 그러므로 오늘날과 같은 인간으로 키우기까지 어버이신의 고심은 참으로 여간한 것이 아니었다.\n 이 일련의 노래의 주요한 뜻은 신님의 은혜의 위대함과 세상 사람들은 모두 형제 자매 임을 가르쳐 주신 것이다.(第六호 二十九～五十一 참조)"
            )
        )

        allContent.add(
            ContentItem(
                korean = " 이 이야기를 예삿일로 생각 말라\n 온 세상 사람들을 구제하고 싶어서 4-126",
                japanese = "",
                english = "",
                commentary = "百二六,인간창조에 관한 이 이야기를 대수롭지 않게 생각해서는 안된다. 온 세상 자녀들을 구제하려는 어버이신의 의도를 전하는 것이므로, 잘 들어 명심해 두지 않으면 안된다.\n 百二二～百二六 : 태초이야기입니다."
            )
        )

        allContent.add(
            ContentItem(
                korean = " 나날이 신의 마음의 진실은\n 깊은 의도가 있다고 생각하라 4-127",
                japanese = "",
                english = "",
                commentary = ""
            )
        )

        allContent.add(
            ContentItem(
                korean = " 지금까지는 아는자가 모르는자에 이끌려\n 시달려 온 것이 신의 섭섭함 4-128",
                japanese = "",
                english = "",
                commentary = "百二八,지금까지는 어버이신의 가르침이 아직 어버이신의 뜻을 전혀 모르는 사람들의 인간생각에 의해 무시당해 왔는데, 이것이 어버이신으로서는 참으로 안타까운 일이다."
            )
        )

        allContent.add(
            ContentItem(
                korean = " 이 갚음 신의 섭리 이것을 보라\n 어떤 자도 흉내를 못 내리라 4-129",
                japanese = "",
                english = "",
                commentary = "百二九,이에 대한 갚음은 어버이신이 섭리로써 할 것이니 잘 두고 보라. 이것은 인간 힘으로써는 흉내내지 못하리라."
            )
        )

        allContent.add(
            ContentItem(
                korean = " 아무리 힘센 자라 할지라도\n 신이 물러나면 이에는 당하지 못해 4-130",
                japanese = "",
                english = "",
                commentary = "百三十,아무리 힘센 자라도 만약 몸에 어버이신의 수호가 사라진다면, 힘을 쓰는 것은 물론 움직이기조차 할 수 없게 될 것이다."
            )
        )

        allContent.add(
            ContentItem(
                korean = " 무엇이든 모든 사람들은 이와 같이\n 신이 자요자재로 한다고 생각하라 4-131",
                japanese = "",
                english = "",
                commentary = "百三一,온 세상 사랑들은 무슨 일이든지 모두 이와 갈이 어버이신이 자유자재로 수호하고 있음을 잘 깨달아야 한다."
            )
        )

        allContent.add(
            ContentItem(
                korean = " 생각하라 젊은이 노인 약한 자라도\n 마음에 따라 어떤 자유자재도 4-132",
                japanese = "",
                english = "",
                commentary = "百三二,어버이신의 자유자재한 섭리를 깊이 인삭하라. 젊은이나 노인이나, 또 아무리 약한자라도 각자의 마음가짐에 따라 어떤 수호도 해 줄 것이다."
            )
        )
        allContent.add(
            ContentItem(
                korean = " 지금까지도 한결같이 살아왔으나\n 신의 자유자재 아는 자 없다 4-133",
                japanese = "",
                english = "",
                commentary = "百三三,이 가르침을 시작하기 전에도 똑같이 어버이신의 수호로 살아왔으나, 일러준 일이 없었기 때문에 누구도 어버이신의 자유자재한 섭리를 아는 자가 없었다."
            )
        )
        allContent.add(
            ContentItem(
                korean = " 이제부터은 만가지를 모두 일러줄 테니\n 마음 틀리지 않도록 하라 4-134",
                japanese = "",
                english = "",
                commentary = "百三四,어버이신이 이 세상에 나타난 이상, 만가지 리를 일러줄 테니, 모두들은 마음가짐이 틀리지 않도록 하라."
            )
        )

    }
} // class QuizActivity가 여기서 끝납니다.