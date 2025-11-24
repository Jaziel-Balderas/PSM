package com.example.psm.UI.adapter

import android.media.MediaMetadataRetriever
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.VideoView
import androidx.recyclerview.widget.RecyclerView
import com.example.psm.R

class ImagePreviewAdapter(
    private val images: MutableList<Uri>,
    private val onImageRemoved: (Int) -> Unit
) : RecyclerView.Adapter<ImagePreviewAdapter.ImageViewHolder>() {

    class ImageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageView: ImageView = view.findViewById(R.id.ivPreviewImage)
        val videoView: VideoView = view.findViewById(R.id.vvPreviewVideo)
        val videoIndicator: ImageView = view.findViewById(R.id.ivVideoIndicator)
        val btnRemove: ImageButton = view.findViewById(R.id.btnRemoveImage)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_image_preview, parent, false)
        return ImageViewHolder(view)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        val uri = images[position]
        val context = holder.itemView.context
        
        // Detectar si es video o imagen
        val mimeType = context.contentResolver.getType(uri)
        val isVideo = mimeType?.startsWith("video/") == true
        
        if (isVideo) {
            // Mostrar video
            holder.imageView.visibility = View.GONE
            holder.videoView.visibility = View.VISIBLE
            holder.videoIndicator.visibility = View.VISIBLE
            
            // Obtener miniatura del video
            try {
                val retriever = MediaMetadataRetriever()
                retriever.setDataSource(context, uri)
                val bitmap = retriever.getFrameAtTime(0)
                holder.imageView.setImageBitmap(bitmap)
                holder.imageView.visibility = View.VISIBLE
                holder.videoView.visibility = View.GONE
                retriever.release()
            } catch (e: Exception) {
                e.printStackTrace()
                // Si falla, mostrar ícono de video
                holder.imageView.visibility = View.GONE
                holder.videoView.visibility = View.VISIBLE
            }
        } else {
            // Mostrar imagen
            holder.imageView.visibility = View.VISIBLE
            holder.videoView.visibility = View.GONE
            holder.videoIndicator.visibility = View.GONE
            holder.imageView.setImageURI(uri)
        }
        
        holder.btnRemove.setOnClickListener {
            val currentPosition = holder.adapterPosition
            if (currentPosition != RecyclerView.NO_POSITION) {
                onImageRemoved(currentPosition)
            }
        }
    }

    override fun getItemCount(): Int = images.size

    fun addImage(uri: Uri) {
        images.add(uri)
        notifyItemInserted(images.size - 1)
    }

    fun removeImage(position: Int) {
        if (position >= 0 && position < images.size) {
            images.removeAt(position)
            notifyItemRemoved(position)
            notifyItemRangeChanged(position, images.size)
        }
    }
}
