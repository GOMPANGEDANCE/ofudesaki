package tenrikyo.ofudesaki.kr

import android.os.Bundle
import android.util.TypedValue
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class ContentDetailActivity : AppCompatActivity() {

    // [수정 1] ArrayList -> List로 변경 (AppData가 List 타입이므로 맞춤)
    // 초기값은 빈 리스트로 설정해서 lateinit 에러 방지
    private var contentList: List<ContentItem> = emptyList()
    private var currentPosition: Int = 0

    private lateinit var koreanTextView: TextView
    private lateinit var japaneseTextView: TextView
    private lateinit var englishTextView: TextView
    private lateinit var commentaryTextView: TextView
    private lateinit var prevButton: Button
    private lateinit var nextButton: Button

    private var currentFontSize = 16f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_content_detail)

        // UI 요소 연결
        val backButton: Button = findViewById(R.id.backButton)
        val increaseButton: Button = findViewById(R.id.increaseFontButton)
        val decreaseButton: Button = findViewById(R.id.decreaseFontButton)
        koreanTextView = findViewById(R.id.koreanTextView)
        japaneseTextView = findViewById(R.id.japaneseTextView)
        englishTextView = findViewById(R.id.englishTextView)
        commentaryTextView = findViewById(R.id.commentaryTextView)
        prevButton = findViewById(R.id.prevButton)
        nextButton = findViewById(R.id.nextButton)

        // [수정 2] 복잡한 Intent 수신 코드 삭제 -> AppData에서 바로 가져오기
        // 번호표(position)는 Intent로 받고, 데이터 뭉치(list)는 AppData에서 꺼냅니다.
        currentPosition = intent.getIntExtra("EXTRA_POSITION", 0)
        contentList = AppData.currentList

        // 혹시 리스트가 비어있으면 안전하게 종료 (에러 방지)
        if (contentList.isEmpty()) {
            finish()
            return
        }

        updateContent(currentPosition)
        updateFontSizes()

        // 버튼 이벤트 리스너 설정
        backButton.setOnClickListener { finish() }
        increaseButton.setOnClickListener {
            currentFontSize += 2f
            updateFontSizes()
        }
        decreaseButton.setOnClickListener {
            if (currentFontSize > 10f) {
                currentFontSize -= 2f
                updateFontSizes()
            }
        }
        prevButton.setOnClickListener {
            if (currentPosition > 0) {
                currentPosition--
                updateContent(currentPosition)
            }
        }
        nextButton.setOnClickListener {
            if (currentPosition < contentList.size - 1) {
                currentPosition++
                updateContent(currentPosition)
            }
        }
    }

    private fun updateContent(position: Int) {
        if (contentList.isNotEmpty() && position < contentList.size) {
            val item = contentList[position]
            koreanTextView.text = item.korean
            japaneseTextView.text = item.japanese
            englishTextView.text = item.english
            commentaryTextView.text = item.commentary

            prevButton.isEnabled = position > 0
            nextButton.isEnabled = position < contentList.size - 1

            updateFontSizes()
        }
    }

    private fun updateFontSizes() {
        koreanTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, currentFontSize)
        japaneseTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, currentFontSize)
        englishTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, currentFontSize)
        commentaryTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, currentFontSize)
    }
}