package tenrikyo.ofudesaki.kr

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class ContentItem(
    val korean: String,
    val japanese: String,
    val english: String,
    val commentary: String
) : Parcelable {
    override fun toString(): String {
        return korean
    }
}