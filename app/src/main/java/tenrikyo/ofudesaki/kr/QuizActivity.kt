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
                english = "yorozuyo no sekai ichiretsu miharasedo\nmune no wakaru mono wa sarai nai",
                commentary = " 一. 어버이신이 이 세상을 창조한 이래\n장구한 세월이 흐르는 동안\n수없이 많은 사람들이 살아왔으나,\n어느 시대를 보아도 넓은 세상 가운데\n누구 하나 신의 뜻을 아는 사람이 없었다."
            )
        )
        allContent.add(
            ContentItem(
                korean = " 그러리라 일러준 일이 없으니\n 아무것도 모르는 것이 무리는 아니야 1-2",
                japanese = "そのはずやしらしてないにこれまでハ\nなにもわからんでむりでわない",
                english = "sono hazuya shirashite nai ni kore made wa\nnani mo wakaran de muride wa nai",
                commentary = "二. 그도 그럴것이 지금까지 이 진실한 가르침을\n일러준 일이 없었으니 무리가 아니다.\n가끔 그 시대의 성현을 통해서 일러주긴 했으나.\n그것은 모두가 시의에 적합하신 신의(神意)의\n표현일 뿐 최후의 가르침은 아니다.\n그것은 아직 시순이 오지 않았기 때문에\n부득이한 것이었다. "
            )
        )
        allContent.add(
            ContentItem(
                korean = " 이번에는 신이 이 세상에 나타나서\n 무엇이든 자세한 뜻을 일러준다1-3",
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
                japanese = "いちれつにはやくたすけをいそぐから\nせかいの心いさめかゝりて",
                english = "ichiretsu ni hayaku tasuke o isogu kara\nsekai no kokoro isame kakarite",
                commentary = "十九. 이제부터 윗사람들은 서로 마음을 맞추어 화목하지 않으면 안된다.\n"
            )
        )
        allContent.add(
            ContentItem(
                korean = " 이 화목 어려운 듯하지만\n 차츰차츰 신이 수호하리라 1-20",
                japanese = "いちれつにはやくたすけをいそぐから\nせかいの心いさめかゝりて",
                english = "ichiretsu ni hayaku tasuke o isogu kara\nsekai no kokoro isame kakarite",
                commentary = "二十, 이 화목은 어려운 듯하나 차차로 어버이신이 수호함에 따라 머지않아 틀림없이 실현될 것이다."
            )
        )
        allContent.add(
            ContentItem(
                korean = " 이 세상은 리로써 되어 있는 세상이다\n 무엇이든 만가지를 노래의 리로써 깨우쳐 1-21",
                japanese = "いちれつにはやくたすけをいそぐから\nせかいの心いさめかゝりて",
                english = "ichiretsu ni hayaku tasuke o isogu kara\nsekai no kokoro isame kakarite",
                commentary = "二十一, 이 세상은 어버이신의 의도, 즉 천리에 의해 성립되어 있으므로, 인간의 행위는 물론 그 밖의 모든 리를 노래로써 깨우치겠다."
            )
        )
        allContent.add(
            ContentItem(
                korean = " 깨우친다 하여 손질로써 하는 것이 아니니\n 입으로도 아니고 붓끝으로 깨우쳐 1-22",
                japanese = "いちれつにはやくたすけをいそぐから\nせかいの心いさめかゝりて",
                english = "ichiretsu ni hayaku tasuke o isogu kara\nsekai no kokoro isame kakarite",
                commentary = "二十二, 깨우친다 해도 인간들처럼 완력으로 하는 것도 아니요, 말로 꾸짖는 것도 아니다. 다만 붓으로 깨우치는 것이다.\n붓끝이란 친필을 말한다."
            )
        )
        allContent.add(
            ContentItem(
                korean = " 무엇이든 잘한 것은 좋으나\n 잘못함이 있으면 노래로써 알린다 1-23",
                japanese = "いちれつにはやくたすけをいそぐから\nせかいの心いさめかゝりて",
                english = "ichiretsu ni hayaku tasuke o isogu kara\nsekai no kokoro isame kakarite",
                commentary = "二十三, 모든 것이 어버이신의 뜻에 맞으면 좋으나, 만약 어버이신의 뜻에 맞지 않는 일이 있으면 노래로써 알릴테니 잘 깨달아 마음이 잘못되지 않도록 해야 한다."
            )
        )
        allContent.add(
            ContentItem(
                korean = " 알려 주면 나타나니 안타까운 일\n 어떤 질병도 마음으로부터 1-24",
                japanese = "いちれつにはやくたすけをいそぐから\nせかいの心いさめかゝりて",
                english = "ichiretsu ni hayaku tasuke o isogu kara\nsekai no kokoro isame kakarite",
                commentary = "二十四, 잘못된 마음을 노래로써 알린다면 곧 나타나는데, 이는 가엾기는 하지만 어떤 질병도 각자의 마음에서 비롯되는 것인 만큼 어쩔 수 없는 일이다."
            )
        )
        allContent.add(
            ContentItem(
                korean = " 질병이라 해도 세상 보통 것과 다르니\n 신의 노여움 이제야 나타났다 1-25",
                japanese = "いちれつにはやくたすけをいそぐから\nせかいの心いさめかゝりて",
                english = "ichiretsu ni hayaku tasuke o isogu kara\nsekai no kokoro isame kakarite",
                commentary = "二十五, 질병이라 해도 세상에 흔이 있는 예사로운 질병이라 생각해서는 안된다. 어버이신의 뜻에 맞지 않기 때문에 지금 노여움을 나타낸 것이다."
            )
        )
        allContent.add(
            ContentItem(
                korean = " 지금까지 신이 하는 말을 듣지 않으니\n 부득이 표면에 나타낸 것이다. 1-26",
                japanese = "いちれつにはやくたすけをいそぐから\nせかいの心いさめかゝりて",
                english = "ichiretsu ni hayaku tasuke o isogu kara\nsekai no kokoro isame kakarite",
                commentary = "二十六, 지금까지 여러 번 훈계를 했으나 전혀 듣지 않으므로 부득이 사람들의 눈에 뛰게 표면에 나타낸 것이다.\n교조님의 장남 슈우지는 오랫동안 앓고 있던 다리병이 쉽사리 낫지 않을 뿐만아니라 가끔 통증이 심해 괴로워했다.\n" +
                        "교조님은 이를 질병이 아니라 어버이신님의 꾸지람이므로 깊이 참회하여 마음을 고치도록 깨우치시는 동시에 집터의 청소를 서두르셨다. 이하 슈우지에 대한 어버이신님의 엄한 가르침은, 슈우지 개인에 대한 꾸지람이라 생각지 말고 이를 본보기로 모든 사람들을 깨우치신 것이라 해석해야 할 것이다. "
            )
        )
        allContent.add(
            ContentItem(
                korean = " 이토록 신의 섭섭함이 나타났으니\n 의사도 약도 이에는 당할 수 없다 1-27",
                japanese = "いちれつにはやくたすけをいそぐから\nせかいの心いさめかゝりて",
                english = "ichiretsu ni hayaku tasuke o isogu kara\nsekai no kokoro isame kakarite",
                commentary = "二十七, 가벼운 질병은 간단히 치료가 되지만 어버이신의 엄한 꾸지람을 받았을 때에는 의사나 약으로도 근본적인 치료가 불가능하다."
            )
        )
        allContent.add(
            ContentItem(
                korean = " 이것만은 예삿일로 생각 말라\n 어떻든 이것은 노래로 깨우친다 1-28",
                japanese = "いちれつにはやくたすけをいそぐから\nせかいの心いさめかゝりて",
                english = "ichiretsu ni hayaku tasuke o isogu kara\nsekai no kokoro isame kakarite",
                commentary = "二十八, 이 다리병만은 흔이 있는 예사로운 질병이라 가볍게 여겨서는 안된다.\n" +
                        "그러므로 어디까지나 그 근본을 노래로써 깨우칠 것이다."
            )
        )
        allContent.add(
            ContentItem(
                korean = " 이번에는 집터의 청소를 깨끗하게\n 해 보일 테니 이것을 보아다오 1-29",
                japanese = "いちれつにはやくたすけをいそぐから\nせかいの心いさめかゝりて",
                english = "ichiretsu ni hayaku tasuke o isogu kara\nsekai no kokoro isame kakarite",
                commentary = "二十九, 이번에는 터전의 리를 밝히기 위해 집터를 깨끗이 청소할 것이니 모두 명심하라.\n집터란 교조님이 사시는 곳으로서  인류의 본고장인 터전이 있는 곳이다"
            )
        )
        allContent.add(
            ContentItem(
                korean = " 청소만 깨끗이 하게 되면 신의 뜻을\n 알게 되어 말하고 말하게 되는 거야 1-30",
                japanese = "いちれつにはやくたすけをいそぐから\nせかいの心いさめかゝりて",
                english = "ichiretsu ni hayaku tasuke o isogu kara\nsekai no kokoro isame kakarite",
                commentary = "三十, 이 집터의 청소가 깨끗이 된 다음에는 터전의 리가 나타나서 저절로 이 길은 널리 퍼져간다.\n신의 뜻을 알게 되어 말하고 말하게 되는 거야란 나타난 신님의 뜻을 깨닫고 그것을 다음 또 다음 또 다음으로 말해서 전하게 된다는 뜻."
            )
        )
        allContent.add(
            ContentItem(
                korean = " 이제까지 섭섭함은 어떤 것인가\n 다리를 저는 것이 첫째가는 섭섭함 1-31",
                japanese = "いちれつにはやくたすけをいそぐから\nせかいの心いさめかゝりて",
                english = "ichiretsu ni hayaku tasuke o isogu kara\nsekai no kokoro isame kakarite",
                commentary = "三十一, 이제까지 어버이신이 몹시 섭섭하게 여기고 있는 일이 무엇인가 하면 그것은 슈우지의 다리병인 것이다."
            )
        )
        allContent.add(
            ContentItem(
                korean = " 이 다리는 질병이라 말하고 있지만\n 질병이 아니라 신의 노여움 1-32",
                japanese = "いちれつにはやくたすけをいそぐから\nせかいの心いさめかゝりて",
                english = "ichiretsu ni hayaku tasuke o isogu kara\nsekai no kokoro isame kakarite",
                commentary = ""
            )
        )
        allContent.add(
            ContentItem(
                korean = " 노여움도 무슨 까닭인가 하면\n 나쁜 일 제거되지 않는 까닭이다 1-34",
                japanese = "いちれつにはやくたすけをいそぐから\nせかいの心いさめかゝりて",
                english = "ichiretsu ni hayaku tasuke o isogu kara\nsekai no kokoro isame kakarite",
                commentary = ""
            )
        )
        allContent.add(
            ContentItem(
                korean = " 이 나쁜 일 깨끗이 제거되지 않고서는\n 역사에 방해가 되는 줄로 알아라 1-35",
                japanese = "いちれつにはやくたすけをいそぐから\nせかいの心いさめかゝりて",
                english = "ichiretsu ni hayaku tasuke o isogu kara\nsekai no kokoro isame kakarite",
                commentary = "제 一호 三十九의 주석 참조."
            )
        )
        allContent.add(
            ContentItem(
                korean = " 이 나쁜 일 아루리 끈덕진 것이라 해도\n 신이 깨우쳐서 제거해 보일 테다 1-36",
                japanese = "いちれつにはやくたすけをいそぐから\nせかいの心いさめかゝりて",
                english = "ichiretsu ni hayaku tasuke o isogu kara\nsekai no kokoro isame kakarite",
                commentary = "三十五, 이 나쁜 일이 집터에서 깨끗이 제거되지 않는 한 어버이신이 세계구제를 위한 마음의 역사를 수행하는 데 방해가 되는 줄로 알아라.\n역사란 슈우지가 어버이신님의 뜻에 따라 교조님과 마음을 하나로 하여 만가지 구제의 가르침을 널리 전하는 일에 노력하는 것을 의미하며, 또 하나는 일에 노력하는 것을 의미하여, 또 하나는 집터에서하는 건축이란 의미를 내포하고 있다."
            )
        )
        allContent.add(
            ContentItem(
                korean = " 이 나쁜 일 깨끗이 제거하게 되면\n 다리를 저는 것도 깨끗해진다 1-37",
                japanese = "いちれつにはやくたすけをいそぐから\nせかいの心いさめかゝりて",
                english = "ichiretsu ni hayaku tasuke o isogu kara\nsekai no kokoro isame kakarite",
                commentary = "三十六, 끈덕진 것이란 집착이 강하다는 뜻이다."
            )
        )
        allContent.add(
            ContentItem(
                korean = " 다리만 깨끗이 낫게 되면\n 다음에는 역사 준비만을 1-38",
                japanese = "いちれつにはやくたすけをいそぐから\nせかいの心いさめかゝりて",
                english = "ichiretsu ni hayaku tasuke o isogu kara\nsekai no kokoro isame kakarite",
                commentary = " "
            )
        )
        allContent.add(
            ContentItem(
                korean = " 잠깐 한마디 정월 三十일로 날을 정해서\n 보내는 것도 신의 마음에서이니 1-39",
                japanese = "いちれつにはやくたすけをいそぐから\nせかいの心いさめかゝりて",
                english = "ichiretsu ni hayaku tasuke o isogu kara\nsekai no kokoro isame kakarite",
                commentary = "三十八, 나쁜 일이 제거되어 다리병이 깨끗이 낫게 되면 그 다음에는 오직 역사 준비만을 한다.\n역사란 마음의 역사, 죽 세계인류의 마음을 맑히는 것을 뜻한다."
            )
        )
        allContent.add(
            ContentItem(
                korean = " 곁의 사람들은 무슨 영문인가 생각하겠지만\n 앞일을 알 수 없는 까닭이다. 1-40",
                japanese = "いちれつにはやくたすけをいそぐから\nせかいの心いさめかゝりて",
                english = "ichiretsu ni hayaku tasuke o isogu kara\nsekai no kokoro isame kakarite",
                commentary = "三十九, 수유지는 오랫동안 정실이 없이 오찌에란 내연의 처와 살면서 오또지로오(音次郎)란 아들까지 두었었다. 그리고 이들은 집터에서 동거하고 있었는데, 이것은 본래 어버이신님의 의도에 맞지 않는 나쁜 일이었으므로, 이 오찌에를 친정으로 돌려보내라고 말씀하신 것이다."
            )
        )
        allContent.add(
            ContentItem(
                korean = " 그날이 와서 나타나면 곁의 사람들\n 신이 하는 말은 무엇이든 틀림이 없다 1-41",
                japanese = "いちれつにはやくたすけをいそぐから\nせかいの心いさめかゝりて",
                english = "ichiretsu ni hayaku tasuke o isogu kara\nsekai no kokoro isame kakarite",
                commentary = "四十, 정월 三十일로 날을 정해서 친정으로 돌려보내는 것을 사람들은 무엇 때문일까하고 이상하게 여기겠지만, 이것은 뒤에 나타날 사실을 모르기 때문이다.\n곁의 사람들이란 교조님 측근에 있는 사람들인데, 좁게는 나까야마 댁의 사람들이며 넓게는 이 길을 찾아 모여 온 사람들을 말한다."
            )
        )
        allContent.add(
            ContentItem(
                korean = " 지금까지는 신이 하는 말을 의심하고서\n 무엇이든 거짓이라 말하고 있었다 1-42",
                japanese = "いちれつにはやくたすけをいそぐから\nせかいの心いさめかゝりて",
                english = "ichiretsu ni hayaku tasuke o isogu kara\nsekai no kokoro isame kakarite",
                commentary = " "
            )
        )
        allContent.add(
            ContentItem(
                korean = " 이 세상을 창조한 신이 하는 말은\n 천의 하나도 틀림이 없다 1-43",
                japanese = "いちれつにはやくたすけをいそぐから\nせかいの心いさめかゝりて",
                english = "ichiretsu ni hayaku tasuke o isogu kara\nsekai no kokoro isame kakarite",
                commentary = " "
            )
        )
        allContent.add(
            ContentItem(
                korean = " 차츰차츰 나타나거든 납득하라\n 어떤 마음도 모두 나타날 테니 1-44",
                japanese = "いちれつにはやくたすけをいそぐから\nせかいの心いさめかゝりて",
                english = "ichiretsu ni hayaku tasuke o isogu kara\nsekai no kokoro isame kakarite",
                commentary = " "
            )
        )
        allContent.add(
            ContentItem(
                korean = " 모든 시대 온 세상을 살펴보면\n 인간이 가는 길도 가지각색이다 1-45",
                japanese = "いちれつにはやくたすけをいそぐから\nせかいの心いさめかゝりて",
                english = "ichiretsu ni hayaku tasuke o isogu kara\nsekai no kokoro isame kakarite",
                commentary = "四十一～四十四, 오찌에는 친정으로 돌아간후, 며칠이 안되어 병으로  자리에 눕게되고 끝내 다시 일어나지 못했다. 만약 인정에 끌려 기일을 늦추었더라면 집터의 청소는 결국 할 수가 없었을 것이다."
            )
        )
        allContent.add(
            ContentItem(
                korean = " 앞으로는 길에 비유해서 말한다\n 누구 일이라고 새삼 말하지 않아 1-46",
                japanese = "いちれつにはやくたすけをいそぐから\nせかいの心いさめかゝりて",
                english = "ichiretsu ni hayaku tasuke o isogu kara\nsekai no kokoro isame kakarite",
                commentary = "四十五, 예나 지금이나 세상 사는 모습을 살펴보면 인생행로는 천태만상이다."
            )
        )
        allContent.add(
            ContentItem(
                korean = " 산언덕 가시밭 낭떠러지 비탈길도\n 칼날 같은 험한 길도 헤쳐 나가면 1-47",
                japanese = "いちれつにはやくたすけをいそぐから\nせかいの心いさめかゝりて",
                english = "ichiretsu ni hayaku tasuke o isogu kara\nsekai no kokoro isame kakarite",
                commentary = "四十六, 사람이 사는 길은 여러 갈래가 있는데, 앞으로는 길에 비유해서 일러줄 터이니, 어디 누구의 일이라고 말하지 않지만 하나하나 모두 단단히 듣고 잘 생각하라.\n" +
                        "남의 일로 여겨 흘려 들어서는 안된다. "
            )
        )
        allContent.add(
            ContentItem(
                korean = " 아직도 보이는 불속 깊은 물속을\n 그것을 지나가면 좁은 길이 보이느니 1-48",
                japanese = "いちれつにはやくたすけをいそぐから\nせかいの心いさめかゝりて",
                english = "ichiretsu ni hayaku tasuke o isogu kara\nsekai no kokoro isame kakarite",
                commentary = "四十七, 산언덕을 넘고 가시덤불을 지나 낭떠러지 비탈길도, 칼날 같은 험한 길도 지나가면."
            )
        )
        allContent.add(
            ContentItem(
                korean = " 좁은 길을 차츰차츰 넘어가면 큰길이야\n 이것이 확실한 본길이니라 1-49",
                japanese = "いちれつにはやくたすけをいそぐから\nせかいの心いさめかゝりて",
                english = "ichiretsu ni hayaku tasuke o isogu kara\nsekai no kokoro isame kakarite",
                commentary = "四十八, 아직도 불속이 있고 깊은 물속도 있으나 그것을 차츰 지나가면 비로소 좁은 길이 나온다."
            )
        )
        allContent.add(
            ContentItem(
                korean = " 이 이야기는 남의 일이 아닌 만큼\n 신한줄기로서 이것을 자신의 일이야 1-50",
                japanese = "いちれつにはやくたすけをいそぐから\nせかいの心いさめかゝりて",
                english = "ichiretsu ni hayaku tasuke o isogu kara\nsekai no kokoro isame kakarite",
                commentary = "四十九, 이좁은 길을 차츰 지나가면 마침내 큰길이 나온다. 이처럼 온갖 어려운 길을 지나서 나타나는 큰길이야말로 참으로 틀림없는 한길이다. \n四十七～四十九,이상 3수는 길에 비유해서 사람들이 걸어가여 할 길의 과정을 가르치신 것으로서, 이같은 시련을 견디며 끝까지 곤경을 극복해 나간다면 반드시 좋은 길로 나아가게 됨을 잘 깨달아야 한다."
            )
        )
        allContent.add(
            ContentItem(
                korean = " 이제까지는 안의 일만을 말했다\n 이제 앞으로는 이야기를  1-8",
                japanese = "いちれつにはやくたすけをいそぐから\nせかいの心いさめかゝりて",
                english = "ichiretsu ni hayaku tasuke o isogu kara\nsekai no kokoro isame kakarite",
                commentary = "五十, 이 이야기는 결코 남의 이기가 아니다. 인간들 을 구제하려는 진실한 어버이신의 가르침으로서, 이것은 바로 너희들 자신의 이야기다.\n이러한 길을 교조님은 몸소 걸어 우리들에게 모본의 길을 보여주셨다. 그러므로 이 모본을 그리며 따르는 이 길의 자녀들은 모두 이것을 자기 일로 생각하라는 가르침이다."
            )
        )
        allContent.add(
            ContentItem(
                korean = " 모든 사람들을 속히 도우려고 서두르니\n 온 세상 사람들의 마음도 용솟음쳐라 1-8",
                japanese = "いちれつにはやくたすけをいそぐから\nせかいの心いさめかゝりて",
                english = "ichiretsu ni hayaku tasuke o isogu kara\nsekai no kokoro isame kakarite",
                commentary = "五十一, 이제까지는 주로 집터 안의 일에 대해서만 여러 가지로 일러주었으나, 앞으로는 널리 일반 세상일에 대해서도 일러주겠다."
            )
        )
        allContent.add(
            ContentItem(
                korean = " 모든 사람들을 속히 도우려고 서두르니\n 온 세상 사람들의 마음도 용솟음쳐라 1-8",
                japanese = "いちれつにはやくたすけをいそぐから\nせかいの心いさめかゝりて",
                english = "ichiretsu ni hayaku tasuke o isogu kara\nsekai no kokoro isame kakarite",
                commentary = " "
            )
        )
        allContent.add(
            ContentItem(
                korean = " 모든 사람들을 속히 도우려고 서두르니\n 온 세상 사람들의 마음도 용솟음쳐라 1-8",
                japanese = "いちれつにはやくたすけをいそぐから\nせかいの心いさめかゝりて",
                english = "ichiretsu ni hayaku tasuke o isogu kara\nsekai no kokoro isame kakarite",
                commentary = " "
            )
        )
        allContent.add(
            ContentItem(
                korean = " 모든 사람들을 속히 도우려고 서두르니\n 온 세상 사람들의 마음도 용솟음쳐라 1-8",
                japanese = "いちれつにはやくたすけをいそぐから\nせかいの心いさめかゝりて",
                english = "ichiretsu ni hayaku tasuke o isogu kara\nsekai no kokoro isame kakarite",
                commentary = " "
            )
        )
        allContent.add(
            ContentItem(
                korean = " 모든 사람들을 속히 도우려고 서두르니\n 온 세상 사람들의 마음도 용솟음쳐라 1-8",
                japanese = "いちれつにはやくたすけをいそぐから\nせかいの心いさめかゝりて",
                english = "ichiretsu ni hayaku tasuke o isogu kara\nsekai no kokoro isame kakarite",
                commentary = "五十五, 어버이신이 원래 없던 세계 없던 인간을 창조한 이래 아주 오랜 세월이 흘렀는데, 그 동안 사람들은 진실한 가르침을 들을 수 없었기 때문에 의지할 어버이도 모른 채 무척 지루하게 살아왔을 것이다.\n이 노래는 지금까지 오랫동안 인간이 진실한 천계(天啓)를 들을 수 없었던 것을 길에 비유해서 가르치신 것이다."
            )
        )
        allContent.add(
            ContentItem(
                korean = " 모든 사람들을 속히 도우려고 서두르니\n 온 세상 사람들의 마음도 용솟음쳐라 1-8",
                japanese = "いちれつにはやくたすけをいそぐから\nせかいの心いさめかゝりて",
                english = "ichiretsu ni hayaku tasuke o isogu kara\nsekai no kokoro isame kakarite",
                commentary = "五十六, 순각한의 도래로 진실한 어버이신이 이 세상에 나타나서 인간창조의 본고장에 참배장소인 근행장소도 이룩했으므로, 사람들은 망설임 없이 안심하고 신앙을 하라."
            )
        )
        allContent.add(
            ContentItem(
                korean = " 모든 사람들을 속히 도우려고 서두르니\n 온 세상 사람들의 마음도 용솟음쳐라 1-8",
                japanese = "いちれつにはやくたすけをいそぐから\nせかいの心いさめかゝりて",
                english = "ichiretsu ni hayaku tasuke o isogu kara\nsekai no kokoro isame kakarite",
                commentary = "五十七, 인간들은 창조한 이래 오랜 세월 동안 여러 경로를 거쳐왔는데, 이제부터는, 이제부터는 그 거쳐온 과정을 일러줄 터이니 잘 듣고 생각하라. 그러면 어버이신이 어떻게 마음을 기울여 왔는지도 알게 될 것이다.\n"
            )
        )
        allContent.add(
            ContentItem(
                korean = " 모든 사람들을 속히 도우려고 서두르니\n 온 세상 사람들의 마음도 용솟음쳐라 1-8",
                japanese = "いちれつにはやくたすけをいそぐから\nせかいの心いさめかゝりて",
                english = "ichiretsu ni hayaku tasuke o isogu kara\nsekai no kokoro isame kakarite",
                commentary = "五十八, 이제부터는 집터 안을 맑히는 준비를 시작하겠는데, 이것이 하루라도 빨리 이루어지도록 어버이신은 서두르고 있다."
            )
        )
        allContent.add(
            ContentItem(
                korean = " 모든 사람들을 속히 도우려고 서두르니\n 온 세상 사람들의 마음도 용솟음쳐라 1-8",
                japanese = "いちれつにはやくたすけをいそぐから\nせかいの心いさめかゝりて",
                english = "ichiretsu ni hayaku tasuke o isogu kara\nsekai no kokoro isame kakarite",
                commentary = " "
            )
        )
        allContent.add(
            ContentItem(
                korean = " 모든 사람들을 속히 도우려고 서두르니\n 온 세상 사람들의 마음도 용솟음쳐라 1-8",
                japanese = "いちれつにはやくたすけをいそぐから\nせかいの心いさめかゝりて",
                english = "ichiretsu ni hayaku tasuke o isogu kara\nsekai no kokoro isame kakarite",
                commentary = "六十, 이 아이를 二, 三년 가르치려고 그 부모는 애쓰고 있지만 어버이신은 이 아이가 이미 수명이 다 되었음을 잘 알고 있다.\n이 아이란 슈우지의 서녀(庶女)인 오슈우를 말하는데, 당시에 부모는 신부수업을 가르치려 하고 있었는데, 어버이신님은 그가 출직할 것을 예언하신 것이다."
            )
        )
        allContent.add(
            ContentItem(
                korean = " 모든 사람들을 속히 도우려고 서두르니\n 온 세상 사람들의 마음도 용솟음쳐라 1-8",
                japanese = "いちれつにはやくたすけをいそぐから\nせかいの心いさめかゝりて",
                english = "ichiretsu ni hayaku tasuke o isogu kara\nsekai no kokoro isame kakarite",
                commentary = "六十一, 잘 생각해 보라, 부모가 아무리 자식 귀여운 마음에서 오래 살기를 바랄지라도 어버이신이 수호하지 않는다면 어쩔 수 없는 것이니 이 점을 잘 깨달아야 한다."
            )
        )
        allContent.add(
            ContentItem(
                korean = " 모든 사람들을 속히 도우려고 서두르니\n 온 세상 사람들의 마음도 용솟음쳐라 1-8",
                japanese = "いちれつにはやくたすけをいそぐから\nせかいの心いさめかゝりて",
                english = "ichiretsu ni hayaku tasuke o isogu kara\nsekai no kokoro isame kakarite",
                commentary = "六十二, 이 세상은 자칫하면 악에 물들기 쉬운 곳이므로 주의해서 악인연을 짓지 않도록 해야 한다."
            )
        )
        allContent.add(
            ContentItem(
                korean = " 모든 사람들을 속히 도우려고 서두르니\n 온 세상 사람들의 마음도 용솟음쳐라 1-8",
                japanese = "いちれつにはやくたすけをいそぐから\nせかいの心いさめかゝりて",
                english = "ichiretsu ni hayaku tasuke o isogu kara\nsekai no kokoro isame kakarite",
                commentary = "六十三, 이제 곧 나이 五十이므로 자신으로서는 꽤나 나이가 많다고 생각하겠지만 어버이신이 볼 때는 아직도 장래가 있다."
            )
        )
        allContent.add(
            ContentItem(
                korean = " 모든 사람들을 속히 도우려고 서두르니\n 온 세상 사람들의 마음도 용솟음쳐라 1-8",
                japanese = "いちれつにはやくたすけをいそぐから\nせかいの心いさめかゝりて",
                english = "ichiretsu ni hayaku tasuke o isogu kara\nsekai no kokoro isame kakarite",
                commentary = "六十四, 올해부터 앞으로 六十년은 어버이신이 확실히 맡는다.\n이것은 슈우지에게 하신 말씀으로서, 당시 四十九세였다."
            )
        )
        allContent.add(
            ContentItem(
                korean = " 모든 사람들을 속히 도우려고 서두르니\n 온 세상 사람들의 마음도 용솟음쳐라 1-8",
                japanese = "いちれつにはやくたすけをいそぐから\nせかいの心いさめかゝりて",
                english = "ichiretsu ni hayaku tasuke o isogu kara\nsekai no kokoro isame kakarite",
                commentary = "六十五, 젊은 아내란 인연이 있어 슈우지의 부인이 된 야마또(大和) 지방 헤구리(平群)군 뵤도도오지(平等寺) 마을의 고히가시 마사끼찌(小東政吉)의 차녀 마쯔에로서 당시 十九세였다."
            )
        )
        allContent.add(
            ContentItem(
                korean = " 모든 사람들을 속히 도우려고 서두르니\n 온 세상 사람들의 마음도 용솟음쳐라 1-8",
                japanese = "いちれつにはやくたすけをいそぐから\nせかいの心いさめかゝりて",
                english = "ichiretsu ni hayaku tasuke o isogu kara\nsekai no kokoro isame kakarite",
                commentary = "六十六, 이것은 나이 차이로 어려운 일처럼 생각되겠지만 어버이신이 나간다면 반드시 혼담을 성사시킬 것이다.\n이 혼담은 처음에 닷다(龍田) 마을의 감베에란 중매인이 고히가시 댁에 교섭을 했으나 일이 잘되지 않았다. 그래서 교조님이 몸소 행차하여 여러 가지로 일러주시자, 저쪽에서도 비로소 납득하게 되어 이윽고 혼담이 성립되었다."
            )
        )
        allContent.add(
            ContentItem(
                korean = " 모든 사람들을 속히 도우려고 서두르니\n 온 세상 사람들의 마음도 용솟음쳐라 1-8",
                japanese = "いちれつにはやくたすけをいそぐから\nせかいの心いさめかゝりて",
                english = "ichiretsu ni hayaku tasuke o isogu kara\nsekai no kokoro isame kakarite",
                commentary = "六十七, 마음을 다해서 매일 어버이신의 일에 전념한다면 앞으로 집터의 책임을 모두 맡긴다.\n이것은 슈우지에게 하신 말씀이다."
            )
        )
        allContent.add(
            ContentItem(
                korean = " 모든 사람들을 속히 도우려고 서두르니\n 온 세상 사람들의 마음도 용솟음쳐라 1-8",
                japanese = "いちれつにはやくたすけをいそぐから\nせかいの心いさめかゝりて",
                english = "ichiretsu ni hayaku tasuke o isogu kara\nsekai no kokoro isame kakarite",
                commentary = "六十八, 이것은 슈우지의 부인 마쯔에의 친정 고히가시 댁에 대해 하신 말씀으로 고히가시 마사끼찌에게는 오사꾸, 마쯔에, 마사따로오(政太郎), 가메끼찌〔龜吉․후에 사다지로오(政次郎)로 개명〕, 오또끼찌〔音吉․후에 센지로오(仙次郎)로 개명〕등, 다섯 자녀가 있었다. 어버이신님은 이들 중 두 사람은 집안일을 시키고, 나머지 세 사람은 어버이신님께 바치면 그 뒷일은 모두 맡겠다고 말씀하셨다. "
            )
        )
        allContent.add(
            ContentItem(
                korean = " 모든 사람들을 속히 도우려고 서두르니\n 온 세상 사람들의 마음도 용솟음쳐라 1-8",
                japanese = "いちれつにはやくたすけをいそぐから\nせかいの心いさめかゝりて",
                english = "ichiretsu ni hayaku tasuke o isogu kara\nsekai no kokoro isame kakarite",
                commentary = "六十九, 세상에서 일어나는 여러 가지 일들을들을 살펴보고 마음을 가다듬어 이제부터 가르치는 것을 잘 생각해 보라"
            )
        )
        allContent.add(
            ContentItem(
                korean = " 모든 사람들을 속히 도우려고 서두르니\n 온 세상 사람들의 마음도 용솟음쳐라 1-8",
                japanese = "いちれつにはやくたすけをいそぐから\nせかいの心いさめかゝりて",
                english = "ichiretsu ni hayaku tasuke o isogu kara\nsekai no kokoro isame kakarite",
                commentary = "七十, 지금까지도 이 세상의 모든 일은 어버이신이 지배해 왔지만, 어버이신이 세상밖으로 나가서 부부의 연을 맺어 주는 것은 이것이 처음이다."
            )
        )
        allContent.add(
            ContentItem(
                korean = " 모든 사람들을 속히 도우려고 서두르니\n 온 세상 사람들의 마음도 용솟음쳐라 1-8",
                japanese = "いちれつにはやくたすけをいそぐから\nせかいの心いさめかゝりて",
                english = "ichiretsu ni hayaku tasuke o isogu kara\nsekai no kokoro isame kakarite",
                commentary = "七十一, 세상 사람들은 이 결혼을 나이 차이로 합당치 않다고 비웃고 있지만, 그것은 사람들이 어버이신의 참뜻을 모르기 때문이다. 그리고 이 결혼은 서로 같은 인연을 모아 부부의 연을 맺는 것으로서, 이는 인생의 근본인 만큼 무엇보다도 중요한 것이다."
            )
        )
        allContent.add(
            ContentItem(
                korean = " 모든 사람들을 속히 도우려고 서두르니\n 온 세상 사람들의 마음도 용솟음쳐라 1-8",
                japanese = "いちれつにはやくたすけをいそぐから\nせかいの心いさめかゝりて",
                english = "ichiretsu ni hayaku tasuke o isogu kara\nsekai no kokoro isame kakarite",
                commentary = "七十二, 세상에서는 왜 저런 일을 하는가고 이상하게 여기겠지만, 이는 전혀 인연에 대해 모르기 때문이다. 그러나 머지않아 이를 깨닫는 날이 올 것이므로 어버이신으로서는 이 같은 한때의 웃음거리가 오히려 즐거운 일이다."
            )
        )
        allContent.add(
            ContentItem(
                korean = " 모든 사람들을 속히 도우려고 서두르니\n 온 세상 사람들의 마음도 용솟음쳐라 1-8",
                japanese = "いちれつにはやくたすけをいそぐから\nせかいの心いさめかゝりて",
                english = "ichiretsu ni hayaku tasuke o isogu kara\nsekai no kokoro isame kakarite",
                commentary = "七十三, 이 결혼에 대해 사람들은 인간마음으로 여러 가지 해석들을 하지만, 어버이신은 이와는 전혀 다른 깊은 뜻이 있다.\n이 결혼에는 근행을 서두르시는, 즉 근행 준비로서 인원을 갖추려고 서두르시는 깊은 신의(神意)가 내포되어 있다."
            )
        )
        allContent.add(
            ContentItem(
                korean = " 모든 사람들을 속히 도우려고 서두르니\n 온 세상 사람들의 마음도 용솟음쳐라 1-8",
                japanese = "いちれつにはやくたすけをいそぐから\nせかいの心いさめかゝりて",
                english = "ichiretsu ni hayaku tasuke o isogu kara\nsekai no kokoro isame kakarite",
                commentary = "七十四, 전생에서부터 깊은 인연이 있는 사람을 이 집터에 이끌어 들여 부부가 되도록 영원히 확실하게 다스린다.\n전생인연이란 슈우지와 마쯔에와의 관계를 두고 하신 말씀으로, 두 사람은 전생의 인연에 의해서 금생에 부부가 되어야 할 사이였으며, 또 터전에 깊은 인연이 있는 사람들이었다."
            )
        )

    }
} // class QuizActivity가 여기서 끝납니다.