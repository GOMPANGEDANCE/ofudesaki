package tenrikyo.ofudesaki.kr

import android.os.Build
import android.os.Bundle
import android.util.TypedValue
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class ContentDetailActivity : AppCompatActivity() {

    // --- 모든 변수를 여기서 먼저 선언합니다 ---
    private lateinit var contentList: ArrayList<ContentItem>
    private var currentPosition: Int = 0

    private lateinit var koreanTextView: TextView
    private lateinit var japaneseTextView: TextView
    private lateinit var englishTextView: TextView
    private lateinit var commentaryTextView: TextView
    private lateinit var prevButton: Button
    private lateinit var nextButton: Button

    private var currentFontSize = 16f
    // --- 여기까지 ---

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

        // Intent에서 전체 목록과 현재 위치 데이터 받아오기
        currentPosition = intent.getIntExtra("EXTRA_POSITION", 0)
        contentList = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableArrayListExtra("EXTRA_CONTENT_LIST", ContentItem::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableArrayListExtra("EXTRA_CONTENT_LIST")
        } ?: arrayListOf()

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

            updateFontSizes() // 내용이 바뀔 때마다 글자 크기 다시 적용
        }
    }

    private fun updateFontSizes() {
        koreanTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, currentFontSize)
        japaneseTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, currentFontSize)
        englishTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, currentFontSize)
        commentaryTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, currentFontSize)
    }
}