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
            val selectedItem = adapter.getItem(position)
            if (selectedItem != null) {
                // intent를 한 번만 선언하고, 필요한 모든 데이터를 여기에 담습니다.
                val intent = Intent(this, ContentDetailActivity::class.java).apply {
                    // 이전/다음 기능을 위한 데이터
                    putExtra("EXTRA_POSITION", position)
                    putParcelableArrayListExtra("EXTRA_CONTENT_LIST", ArrayList(allContent))

                    // (혹시 모를 경우를 대비한 개별 데이터도 함께 전달)
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
                english = "",
                commentary = "三, 찻잎을 따고 나서 가지 고르기를 마치면 그 다음에는 드디어 즐거운근행을 시작한다.\n이 지역에서 찻잎을 따는 시기는 대체로 음력 5월 중순경이다."
            )
        )
        allContent.add(
            ContentItem(
                korean = " 이 근행 어떻게 이루어진다고 생각하는가\n 윗사람들 마음 용솟음칠 거야 2-4",
                japanese = "このつとめとこからくるとをもうかな\n上たるところいさみくるぞや",
                english = "",
                commentary = "四, 이 즐거운근행은 누가 행하는가 하면 마음이 바꿔진 사람들부터 행하게 된다. 그렇게 되면 윗사람들도 저절로 마음이 용솟음치게 될 것이다."
            )
        )
        allContent.add(
            ContentItem(
                korean = " 차츰차츰 신의 수호는\n 모두 진기한 일만 시작할 거야 2-5",
                japanese = "たん／＼と神のしゆごふとゆうものハ\nめつらし事をみなしかけるで",
                english = "",
                commentary = "五, 어버이신은 영묘한 수호로써 차츰 사람들이 생각지도 못했던 신기한 힘을 나타내어 지금까지 모르던 진기한 일을 시작한다."
            )
        )
        allContent.add(
            ContentItem(
                korean = " 나날이 신이 마음 서두르는 것을\n 모든 사람들은 어떻게 생각하는가 2-6",
                japanese = "にち／＼に神の心のせきこみを\nみないちれつハなんとをもてる",
                english = "",
                commentary = " "
            )
        )
        allContent.add(
            ContentItem(
                korean = " 어떤 것이든 질병이나 아픔이란 전혀 없다\n 신의 서두름 인도인 거야 2-7",
                japanese = "なにゝてもやまいいたみハさらになし\n神のせきこみてびきなるそや",
                english = "",
                commentary = "七, 세상 사람들은 질병이니 통증이니 하고들 있으나 결코 그런 것이 아니다. 그것은 모두 어버이신의 깃은 의도에 의한 서두름이며 인도이다."
            )
        )
        allContent.add(
            ContentItem(
                korean = " 서두르는 것도 무슨 까닭인가 하면\n 근행인원이 필요하므로 2-8",
                japanese = "せきこみもなにゆへなるとゆうならば\nつとめのにんぢうほしい事から",
                english = "",
                commentary = "八, 어버이신이 왜 서두르고 있는가 하면, 그것은 빨리 근행인원을 이끌어 들이고 싶기 때문이다.\n근행인원은 第十호 二十五～二十七의 주석 참조."
            )
        )
        allContent.add(
            ContentItem(
                korean = " 이 근행 어떤 것이라 생각하는가\n 만가지구제의 준비만을 2-9",
                japanese = "このつとめなんの事やとをもている\nよろづたすけのもよふばかりを",
                english = "",
                commentary = "九, 어버이신이 바라고 있는 즐거운근행은 무엇 때문이라고 생각하는가, 이것으로 세상의 만가지를 구제하고 싶기 때문이다."
            )
        )
        allContent.add(
            ContentItem(
                korean = " 이 구제 지금뿐이라고는 생각 말라\n 이것이 영원한 고오끼인 거야 2-10",
                japanese = "このたすけいまばかりとハをもうなよ\nこれまつたいのこふきなるぞや",
                english = "",
                commentary = "十, 이 구제는 현재뿐이라고 생각해서는 안된다. 이것은 영원한 본보기가 되어 언제까지나 구제의 결실을 거두게 된다.\n영원한 고오끼란 영원히 후세에까지 전해져 구제한줄기의 가르침의 토대가 됨을 말한다."
            )
        )
        allContent.add(
            ContentItem(
                korean = " 잠깐 한마디 신경증이다 광증이다 말하고 있다\n 질병이 아니라 신의 서두름 2-11",
                japanese = "一寸はなしのぼせかんてきゆうている\nやまいでハない神のせきこみ",
                english = "",
                commentary = "十一, 세상 사람들은 흔히들 저 사람은 신경증이다 광증이다고 말하고 있으나 결코 정신이 돈 것도 아니요, 질병도 아니다. 빨리 이 길로 이끌어 들이려는 어버이신의 서두름이다.\n다음 노래의 주석을 참조"
            )
        )
        allContent.add(
            ContentItem(
                korean = " 차츰차츰 진실한 신한줄기를\n 일러주어도 아직도 몰라 2-12",
                japanese = "たん／＼としんぢつ神の一ちよふ\nといてきかせどまだハかりない",
                english = "",
                commentary = "十二, 여러가지로 진실한 신한줄기의 길을 일러주어도 아직 깨닫지 못하고 있다.\n 쯔지 추우사꾸(辻忠作)는 1863년에 입신했는데, 그 동기는 누이동생 구라의 발광 때문이었다. 그는 어느 날, 친척되는 이찌노모또(櫟本)마을의 가지모또(梶本)가에서 쇼야시끼의 신님은 만가지구제를 하시는 신님이란 말을 듣고 비로소 신앙할 마음이 생겼다. 추우사꾸는 즉시 교조님을 찾아 뵙고 여러 가지 가르침을 들은 후 신앙을 했던 바, 구라의 발광은 씻은 듯이 나았다. 그래서 그는 이 길을 열심히 신앙하게 되었으며, 구라는 그 뒤에 혼담이 이루어져 센조꾸(千束)라는 곳으로 시집을 갔다. 그러나 그 후 추우사꾸는 집안 사람들의 반대에 부딪쳐 차차 신앙심이 약해짐에 따라 교조님께 전혀 발걸음을 하지 않게 되었는데. 그러자 이상하게도 구라의 병이 다시 재발하여 시집에서 쫒겨나게 되었다.\n이에 사람들은 구라가 정신이 돌았기 때문에 쫓겨 났다느니, 혹은 이혼을 당했기 때문에 돌았다느니 하는 여러 가지 말들을 하고 있었으나, 그것은 결코 사람들이 흔히 말하는 질병도 광증도 아닌, 오직 신앙에서 멀어진 추우사꾸를 이 길로 다시 이끌어 들이시려는 어버이신님의 깊은 의도였던 것이다. 이러한 가르침을 들은 추우사꾸는 자신의 잘못을 참회하고 다시 열심히 어버이신님의 일에 힘쓰게 되었다. 그러자 구라의 발광은 씻은 듯이 나아 다시 시집으로 돌아가게 되었다."
            )
        )
        allContent.add(
            ContentItem(
                korean = " 어서어서 세상에 나가려고 생각하지만\n 길이 없어서는 나가려야 나갈 수 없다 2-13",
                japanese = "といてきかせどまだハかりなも\nみちがのふてハでるにでられん",
                english = "",
                commentary = "十三, 지금까지는 여기 찾아오는 사람에게만 가르침을 일러주었으나, 이제는 한시라도 빨리 이쪽에서 밖으로 나아가 세상 사람들에게 널리 가르침을 일러주고자 한다. 그러나 길이 억어서는 나아갈 수가 없다."
            )
        )
        allContent.add(
            ContentItem(
                korean = " 이 길을 빨리 내려고 생각하지만\n 다른 곳에서는 낼 데가 없다 2-14",
                japanese = "このみちをはやくつけよとをもへとも\nほかなるとこでつけるとこなし",
                english = "",
                commentary = "十四, 세상에 널리 이 가르침을 전하고자 하나, 이 길은 아무데서나 낼 수 있는 것이 아니기 때문에 이곳 아닌 다른 곳에서는 낼 수가 없다."
            )
        )
        allContent.add(
            ContentItem(
                korean = " 이 길을 진실로 생각한다면\n 가슴속으로 만가지를 생각하라 2-15",
                japanese = "このみちをしんぢつをもう事ならば\nむねのうちよりよろづしやんせ",
                english = "",
                commentary = "十五, 이 신한줄기의 길을 진실로 생각한다면 마음을 가다듬어 만사를 잘 생각해 보라."
            )
        )
        allContent.add(
            ContentItem(
                korean = " 이 이야기 무엇을 말한다고 생각하는가\n 신의 구제장소를 서두르는 것이 2-16",
                japanese = "このはなしなんの事やとをもている\n神のうちわけばしよせきこむ",
                english = "",
                commentary = "十六, 지금 가르친 이야기의 참뜻은 무엇이라 생각하는가, 그것은 방방곡곡에 구제장소를 마련하기를 서두르는 것이다.\n구제장소란 장래 안과 중간과 밖에 각각33개소, 도합 93개소가 생기게 되는데, 어떤 어려운 질병이라도 이 구제장소를 도는 동안에 구제받게 되며, 이 가운데 한 곳은 아주 먼 벽지에 있다. 그러나 이 곳을 빠뜨려서는 구제받지 못한다. 그리고 가령 도중에서 구제를 받았더라도 탈것이나 지팡이를 버리지 말고, 고맙게 도움받았다는 사실을 사람들에게 알리며, 이것을 마지막에는 터전에 올려야 한다. 만약 도중에서 이를 버릴 경우에는 일단 구제를 받았더라도 다시 본래대로 된다고 말씀하셨다."
            )
        )
        allContent.add(
            ContentItem(
                korean = " 이 길이 조금 나타나기 시작하면\n 세상 사람들의 마음 모두 용솟음친다 2-17",
                japanese = "このみちが一寸みゑかけた事ならば\nせかいの心みないさみてる",
                english = "",
                commentary = "十七, 이 가르침이 나타나기 시작하면 세상 사람들의 마음은 모두 용솟음치게 된다."
            )
        )
        allContent.add(
            ContentItem(
                korean = " 무엇이든 신이 하는 말 단단히 들어라\n 집터의 청소가 되었으면 2-18",
                japanese = "なにゝても神のゆう事しかときけ\nやしきのそふぢでけた事なら",
                english = "",
                commentary = ""
            )
        )
        allContent.add(
            ContentItem(
                korean = " 벌써 보인다 한눈 팔 새 없을 만큼\n 꿈결같이 티끌이 털리는 거야 2-19",
                japanese = "もふみへるよこめふるまないほどに\nゆめみたよふにほこりちるぞや",
                english = "",
                commentary = "十八, 十九, 어떤 일이라도 어버이신의 가르침을 단단히 듣도록 하라. 집터 안의 사람들이 마음을 청소하여 이것이 어버이신이게 통하게 되면 눈 깜빡할 사이에 티끌은 털리고 만다."
            )
        )
        allContent.add(
            ContentItem(
                korean = " 이 티끌 깨끗하게 털어 버리면\n 앞으로는 만가지 구제한줄기 2-20",
                japanese = "このほこりすきやかはろた事ならば\nあとハよろづのたすけ一ちよ",
                english = "",
                commentary = ""
            )
        )
        allContent.add(
            ContentItem(
                korean = " 앞으로는 차츰차츰 근행 서둘러서\n 만가지구제의 준비만을 2-21",
                japanese = "このさきハたん／＼つとめせきこんで\nよろづたすけのもよふばかりを",
                english = "",
                commentary = ""
            )
        )
        allContent.add(
            ContentItem(
                korean = " 온 세상 어디가 나쁘다 아프다 한다\n 신의 길잡이 인도임을 모르고서 2-22",
                japanese = "せかいぢうとこがあしきやいたみしよ\n神のみちをせてびきしらすに",
                english = "",
                commentary = "二十二, 세상에서는 어디가 나쁘다, 어디가 아프다고 말하고 있으나, 사실은 질병이라 전혀 없다. 설사 나쁜 데나 아픈데가 있더라도 그것은 어버이신의 가르침이며 인도에다. 그런데도 세상에서는 이를 전혀 깨닫지 못하고 질병이라고만 생각하고 있다."
            )
        )
        allContent.add(
            ContentItem(
                korean = " 이 세상에 질병이란 없는 것이니\n 몸의 장애 모두 생각해 보라 2-23",
                japanese = "このよふにやまいとゆうてないほどに\nみのうちさハりみなしやんせよ",
                english = "",
                commentary = "二十三, 이 세상에는 질병이란 전혀 없다. 따라서 만약 몸에 이상이 생길 때는 어버이신이 무엇 때문에 이같은 가르침이나 인도를 나타내 보이는가를 잘 생각해 보라."
            )
        )
        allContent.add(
            ContentItem(
                korean = " 나날이 신이 서두르는 이 구제\n 모든 사람든은 어떻게 생각하는가 2-24",
                japanese = "にち／＼に神のせきこみこのたすけ\nみな一れつハなんとをもてる",
                english = "",
                commentary = "二十四, 나날이 어버이신이 사람들의 맘의 장애를 인도로 하여 구제를 서두르고 있음을 세상 사람들은 어떻게 생각하고 있는가. 이러한 어버이신의 간절한 마음을 어서 깨달아 주었으면."
            )
        )
        allContent.add(
            ContentItem(
                korean = " 녺은 산의 못은 맑은 물이지만\n 첫머리는 탁하고 찌꺼기 섞여 있다 2-25",
                japanese = "高山のをいけにハいた水なれど\nてバなハにこりごもくまぢりで",
                english = "",
                commentary = "二十五, 깊은 산속에 있는 못의 맑은 물이란 사람에 비유해서 하신 말씀으로, 인간이 처음 태어날 때는 누구나가 다 청청한 마음을 지니고 있으나, 세월이 지남에 따라 자신만을 생각하는 욕심 때문에 티끌이 쌓여 차츰 마음이 흐려짐을 뜻한다. 첫머리란 입신 당시의 혼탁한 마음을 가리키는 것으로 해석된다."
            )
        )
        allContent.add(
            ContentItem(
                korean = " 차츰차츰 마음을 가다듬어 생각하면\n 맑은 물로 바꿔질거야 2-26",
                japanese = "だん／＼と心しづめてしやんする\nすんだる水とかハりくるぞや",
                english = "",
                commentary = "二十六, 처음에는 티끌이 섞인 탁한 마음일지라도 어버이신의 가르침을 듣고 깊이 반성하여 마음의 티끌을 제거하도록 노력하면, 차츰 물이 맑아지듯 마음이 청정하게 바꿔진다."
            )
        )
        allContent.add(
            ContentItem(
                korean = " 산중에 있는 물속에 들어가서\n 어떤 물이라도 맑히리라 2-27",
                japanese = "山なかのみづのなかいと入こんで\nいかなる水もすます事なり",
                english = "",
                commentary = "二十七, 산중에 있는 물속에 들어가서 그 물이 아무리 탁할지라도 깨끗이 맑히겠다.\n혼탁한 세상을 정화하는 것이 본교의 사명임을 말씀하신 것이다."
            )
        )
        allContent.add(
            ContentItem(
                korean = " 나날이 마음 다하는 사람들은\n 가슴속 진정하라 끝에는 믿음직하리 2-28",
                japanese = "にち／＼に心つくするそのかたわ\nむねをふさめよすゑハたのもし",
                english = "",
                commentary = "二十八, 나날이 이 길을 위해 마음을 다하여 이바지해 온 사람은 이젠 머지않았으니 어떤 가운데서도 마음을 쓰러뜨리지 말고 따라오라. 끝에는 반드시 믿음직한 길이 나타날 것이다."
            )
        )
        allContent.add(
            ContentItem(
                korean = " 이제부터는 높은산의 못에 뛰어들어가\n 어떤 찌꺼기도 청소하리라 2-29",
                japanese = "これからハ高山いけいとびはいり\nいかなごもくもそうぢするなり",
                english = "",
                commentary = "二十九, 이제부터는 길을 내기 어려운 곳에도 들어가 아무리 마음이 혼탁한 자라도 티끌을 털어내어 청정하게 할 것이다."
            )
        )
        allContent.add(
            ContentItem(
                korean = " 찌꺼기만 깨끗이 치워버리면\n 그 다음에 물은 맑아지리라 2-30",
                japanese = "こもくさいすきやかだしてしもたなら\nあとなる水ハすんであるなり",
                english = "",
                commentary = "三十, 아무리 티끌이 쌓여 있는 자라도 그 마음의 더러움을 완전히 씻어 버린다면, 그 다음은 맑은물처럼 깨끗해진다."
            )
        )
        allContent.add(
            ContentItem(
                korean = " 이제부터는 미칠곳과 미친곳에 대해 말한다\n 무슨 말을 하는지 모를 테지 2-31",
                japanese = "これからハからとにほんのはなしする\nなにをゆうともハかりあるまい",
                english = "",
                commentary = "三十一, 지금부터는 미칠곳과 미친곳에 대해 이야기를 하겠는데, 어버이신이 무슨 말을 할지 잘 모를 것이다.\n이 노래 이하 三十四의 노래까지는 第二호 四十七의 주석 참조."
            )
        )
        allContent.add(
            ContentItem(
                korean = " 모르는자가 미친곳의 땃에 들어와서\n 멋대로 하는 것이 신의 노여움 2-32",
                japanese = "とふぢんがにほんのぢいゝ入こんで\nまゝにするのが神のりいふく",
                english = "",
                commentary = "三十二, 어버이신의 가르침을 아직 모르는 자가 미친 곳에 들어가 뽐내며 제멋대로 하고 있는 것이 어비이신으로서는 참으로 안타까운 일이다."
            )
        )
        allContent.add(
            ContentItem(
                korean = " 차츰차츰 미친곳을 구제할 준비를 하니\n 모르는자도 신이 마음대로 하리라 2-33",
                japanese = "たん／＼とにほんたすけるもよふだて\nとふじん神のまゝにするなり",
                english = "",
                commentary = "三十三, 어버이신은 차츰 미친곳에 어버이신의 참뜻을 널리 알려서 세계 인류를 구제할 준비를 하고 있으므로, 아직 어버이신의 가르침을 모르는 자에게도 머지않아 신의를 납득시켜 용솟을치며 신은(神恩)을 입도록 한다."
            )
        )
        allContent.add(
            ContentItem(
                korean = " 앞으로는 미칠곳과 미친곳을 구분한다\n 이것을 분간하게 되면 온 세상 안정이 된다",
                japanese = "このさきハからとにほんをハけるてな\nこれハかりたらせかいをさまる",
                english = "",
                commentary = "三十四, 금후는 미칠곳과 미친곳을 구분하는데 이것만 알게 되면 사람들의 마음은 맑아져 세상은 절로 안정이 된다.\n미친곳이란 태초에 어버이신님이 이 세상 인간을 창조하신 터전이 있는 곳, 따라서 이 가르침을 맨 먼저 편 세계구제의 본고장이 있는 곳을 말한다. 또 어버이신님의 뜻을 이미 알고 있는 자를 말한다. 미칠곳이란 어버이신님의 뜻이 전해질 곳, 또는 다음에 어버이신님의 가르침을 전해 듣게 될 자를 말한다.\n아는자란 어버이신님의 가르침을 먼저 들은 자, 즉 어버이신님의 가르침을 이미 깨달은 자를 만한다.\n모르는자란 이 가르침을 다음에 듣게 될 자, 즉 아직 어버이신님의 가르침을 모르는 자를 말한다.미칠곳과 미친곳에 관한 일련의 노래는 친필을 집필하실 당시, 과학기술을 도입하기에 급급한 나머지, 물질문명에만 현혹되어 문명 본래의 생명인 인류애와 공존공영(共存共榮)의 정신은 이해하려 하지 않고 오직 물질주의, 이기주의로만 흐르고 있던 당시 사람들에게 엄한 경고를 하시고, 빨리 어버이신님의 뜻을 깨달아 구제한줄기의 정신으로 나아가라고 격려하신 노래이다. 즉, 어버이신님의 눈으로 보시면 온 세상 사람들은 모두 귀여운 자녀이다. 따라서 어버이신님의 뜻을 알든 모르든, 또 가르침을 먼저 들은 자든 나중에 듣는 자든 여기에는 아무런 차별도 없이 온 세상 사람들을 궁극적으로 똑같이 구제하시려는 것이 어버이신님의 어버이마음이다. 그러므로 어버이신은 사람들의 마음이 맑아져서 서로가 형제자며임을 자각하고, 서로 위하고 서로 돕는 마음이 되어 하루라도 빨리 즐거운 삻을 누릴 날이 오기를 서두르고 계신다.(第十호 五五, 五六의 주석, 제 十二호 七의 주석 참조)"
            )
        )
        allContent.add(
            ContentItem(
                korean = " 이제까지 윗사람들은 신의 마음 모르고서\n 세상 보통일로 생각하고 있었다 2-35",
                japanese = "いまゝでハ上たる心ハからいで\nせかいなみやとをもていたなり",
                english = "",
                commentary = "三十五, 지금까지 윗사람들은 어버이신의 마음을 모르기 때문에 이 길의 참뜻을 이해하지 못하고 세상에 흔히 있는 보통 일로 생각하고 있었다."
            )
        )
        allContent.add(
            ContentItem(
                korean = " 이제부터는 신이 몸 안에 들아가서\n 마음을 속히 깨닫도록 할 테다 2-36",
                japanese = "これからハ神がたいない入こんで\n心すみやかわけてみせるで",
                english = "",
                commentary = "三十六, 이제부터는 어버이신이 그러한 사람들의 몸속에 들어가서 이 길의 진가를 깨닫도록 할 것이다."
            )
        )
        allContent.add(
            ContentItem(
                korean = " 나날이 모여드는 사람에게 오지 마라\n 해도 도리어 점점 더 불어난다 2-37",
                japanese = "にち／＼によりくる人にことハりを\nゆへばだん／＼なをもまあすで",
                english = "",
                commentary = "三十七, 매일 교조를 그리며 오는 사람들에게 오지 마라고 거절을 해도, 오히려 찾아오는 사람들은 점차 불어날 뿐이다. 교조님을 그리며 모여 오는 사람들에게 교조님은 이 길의 가르침을 일러주셨는데, 세상에는 이 가르침을 올바로 이해하는 사람이 적었기 때문에 여러 가지 오해를 사고, 그로 인하여 각계 각층으로부터 자주 방해를 받았다. 교조님의 측근들은, 이래 서는 교조님께 누를 끼칠 뿐이라는 생에서 참배하러 오는 사람들을 못오도록 거절 했으나, 어버이신님의 의도는 이 가르침을 널리 펴려는 것이었으므로, 아무래도 교조님을 그리며 돌아오는 사람들을 제지할 수 없을뿐더러, 도리어 점차 불어나게 될 뿐이라는 뜻이다."
            )
        )
        allContent.add(
            ContentItem(
                korean = " 아무리 많은 사람이 오더라도\n 조금도 염려 말라 신이 맡는다 2-38",
                japanese = "いかほどのをふくの人がきたるとも\nなにもあんぢな神のひきうけ",
                english = "",
                commentary = ""
            )
        )
        allContent.add(
            ContentItem(
                korean = " 이 세상 창조의 진기한 감로대\n 이것으로 온 세상 안정이 된다 2-39",
                japanese = "めつらしいこのよはじめのかんろたい\nこれがにほんのをさまりとなる",
                english = "",
                commentary = "三十九, 이 세상 인간창조의 리를 나타내는 진기한 감로대가 세워져서 감로대근행을 올리게 되면, 그 영덕(靈德)에 의해 어버이신의 참뜻이 온 세상에 널리 알려지게 되고, 그로 인해 온 세상 사람들은 용솟음치며 즐거운 삶을 누리게 된다."
            )
        )
        allContent.add(
            ContentItem(
                korean = " 높은산에 물과 불이 보인다\n 누구의 눈에도 이것이 안 보이는가 2-40",
                japanese = "高山に火と水とがみへてある\nたれがめへにもこれがみへんか",
                english = "",
                commentary = "四十, 윗사람들에게도 어버이신의 수호가 알려지고 있는데도 모두들은 이것을 모르는가.\n물과 불이란 '물과 불은 근본의 수호'라고 가르치신 바와 같이, 어버이신님의 절대 하신 수호를 의미한다. 물이라면 음료수도 물, 비도 물, 몸의 수분도 물, 해일도 물이다. 불이라면 등불도 불, 체온도 불, 화재도 불이다. "
            )
        )
        allContent.add(
            ContentItem(
                korean = " 차츰차츰 어떤 말도 일러주었다\n 확실한 것이 보이고 있으므로 2-41",
                japanese = "たん／＼といかなはなしもといてある\nたしかな事がみゑてあるから",
                english = "",
                commentary = "四十一, 지금까지도 여러 가지로 장래의 일을 일러주었는데, 그것은 어버이신이 무엇이든 장래의 일을 잘 알고 있기 때문이다."
            )
        )
        allContent.add(
            ContentItem(
                korean = " 행복을 누리도록 충분히 수호하고 있다\n 몸에 받게 될 테니 이것을 즐거워하라 2-42",
                japanese = "しやハせをよきよふにとてじうぶんに\nみについてくるこれをたのしめ",
                english = "",
                commentary = "四十二, 모든 사람들이 행복해지도록 어버이신은 충분히 수호하고 있는 만큼, 마음을 바르게 하여 어버이신의 뜻에 따르는 자는 누구나 행복을 누리게 된다. 이것을 낙으로 삼아 살아가도록."
            )
        )
        allContent.add(
            ContentItem(
                korean = " 무엇이든 허욕 부린 그 다음에는\n 신의 노여움 나타날 거야 2-43",
                japanese = "なにもかもごふよくつくしそのゆへハ\n神のりいふくみへてくるぞや",
                english = "",
                commentary = "四十三, 무엇이든지 어버이신의 가르침을 지키지 않고 사리 사욕을 부린 자는 반드시 어버이신의 노여움이 나타나 괴로움을 겪게 될 것이다."
            )
        )
        allContent.add(
            ContentItem(
                korean = " 차츰차츰 15일부터 나타나기 시작하리라\n 선과 악이 모두 나타날 것이니 2-44",
                japanese = "たん／＼と十五日よりみゑかける\n善とあくとハみなあらハれる",
                english = "",
                commentary = "四十四, 차츰 15일부터 여러 가지 일이  나타날 것이다. 선은 선, 악은 악으로 어버이신이 선명하게 구분해 보이리라."
            )
        )
        allContent.add(
            ContentItem(
                korean = " 이 이야기는 누구예 일이라 말하지 않아\n 나타나거든 모두 납득하라 2-45",
                japanese = "このはなしとこの事ともゆハんてな\nみへてきたればみなとくしんせ",
                english = "",
                commentary = "四十五, 지금까지 말한 이 가르침은 특별히 누구를 두고 한 것이 아니지만 조만간 나타날 것이니 그때가 되면 납득하게 될 것이다.\n이상 3수에 관련된 사료(史料)로는 1872년 야마또 지방 히가시와까이(東若井) 마을에 살던 마쯔오 이찌베에(松尾市兵衛)의 이야기가 전해지고 있다. 이찌베에는 이 길의 신앙에 매우 열성적이어서 당시 뛰어난 제자 가운데 한 사람으로 열심히 가르침을 전하고 있었는데, 매사에 이유가 많고 성질이 강한 사람인지라 막상 자신에 관한 중요한 일에 부딪치면 어버이신님의 가르침을 순직하게 그대로 받아들이지를 못했다. 그로 말미암아 어버이신님으로부터 손질을 받고 있던 그의 장남이 음력 7월 보름 백중날에 출직하고 말았던 것이다."
            )
        )
        allContent.add(
            ContentItem(
                korean = " 높은산에 있는 아는자와 모르는자를\n 구분하는 것도 이것도 기둥이야 2-46",
                japanese = "高山のにほんのものととふぢんと\nわけるもよふもこれもはしらや",
                english = "",
                commentary = "四十六, 지도층에 있는 사람 가운데, 어버이신의 뜻을 깨달아 마음이 맑아진 사람과 인간의 지혜와 힘만을 믿고 아직 어버이신의 가르침을 모르는 사람을 구분하는 것도 이 감로대의 영덕으로 하는 것이다."
            )
        )
        allContent.add(
            ContentItem(
                korean = " 모르는자와 아는자를 구분하는 것은\n 물과 불을 넣어서 구분하리라 2-47",
                japanese = "とふじんとにほんのものとハけるのハ\n火と水とをいれてハけるで",
                english = "",
                commentary = "四十七, 아직 어버이신의 가르침을 모르는 사람과 어버이신의 뜻을 깨달은 사람을 구분하는 것은, 어버이신의 절대한 힘에 의한 것이다."
            )
        )

    }
} // class QuizActivity가 여기서 끝납니다.