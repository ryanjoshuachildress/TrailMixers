package com.ryanjoshuachildress.trailmixers

import android.content.Context
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.ryanjoshuachildress.trailmixers.models.BoardSize
import kotlin.math.min


class ImagePickerAdaptor(
    private val context: CreateActivity,
    private val imageUris: List<Uri>,
    private val boardSize: BoardSize,
    private val imageClickListener: ImageClickListener
) : RecyclerView.Adapter<ImagePickerAdaptor.ViewHolder>() {

    interface ImageClickListener {
        fun onPlaceholderClicked()
    }

    companion object{
        private const val MARGIN_SIZE = 10
        private const val TAG = "ImagePickerAdaptor"
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImagePickerAdaptor.ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.card_image, parent, false)
        val cardWidth = parent.width / boardSize.getWidth()- (2* MARGIN_SIZE)
        val cardHeight = parent.height / boardSize.getHeight()- (2* MARGIN_SIZE)
        val cardSideLength = min(cardHeight, cardWidth)

        val layoutParams: ViewGroup.LayoutParams =
            view.findViewById<ImageView>(R.id.ivCustomImage).layoutParams
        layoutParams.width = cardSideLength
        layoutParams.height = cardSideLength
        return ViewHolder(view)
    }

    override fun getItemCount() = boardSize.getNumPairs()


    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val ivCustomImage = itemView.findViewById<ImageView>(R.id.ivCustomImage)

        fun bind(uri: Uri) {
            ivCustomImage.setImageURI(uri)
            ivCustomImage.setOnClickListener(null)
        }

        fun bind() {
            ivCustomImage.setOnClickListener() {
            imageClickListener.onPlaceholderClicked()
            }
        }

    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        if (position < imageUris.size) {
            holder.bind(imageUris[position])

        } else {
            holder.bind()
        }
    }

}
