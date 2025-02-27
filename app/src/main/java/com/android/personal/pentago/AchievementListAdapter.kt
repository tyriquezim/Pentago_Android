package com.android.personal.pentago

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.transition.Visibility
import com.android.personal.pentago.databinding.AchievementItemBinding
import com.android.personal.pentago.model.Achievement

class AchievementHolder(private val binding: AchievementItemBinding): RecyclerView.ViewHolder(binding.root)
{
    fun bind(achievement: Achievement, onAchievementClicked: (achievement: Achievement) -> Unit)
    {
        binding.apply()
        {
            achievementTitleTextview.text = achievement.achievementTitle
            achievementDescTextview.text = achievement.achievementDescription

            if(achievement.hasBeenEarned)
            {
                achievementImageview.setImageResource(R.drawable.earned_baseline_stars_24)
                achievementCheckbox.isChecked = true
                achievementCheckbox.setText(R.string.checkbox_earned_text)
                achievementDate.text = achievement.date.toString()
                achievementDate.visibility = View.VISIBLE
            }
            else
            {
                achievementImageview.setImageResource(R.drawable.unearned_baseline_stars_24)
                achievementCheckbox.isChecked = false
                achievementCheckbox.setText(R.string.checkbox_unearned_text)
                achievementDate.visibility = View.GONE
            }

            root.setOnClickListener()
            {
                onAchievementClicked(achievement)
            }
        }
    }
}

class AchievementListAdapter(private val achievements: List<Achievement>, private val onAchievementClicked: (achievement: Achievement) -> Unit): RecyclerView.Adapter<AchievementHolder>()
{
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AchievementHolder
    {
        val inflater = LayoutInflater.from(parent.context)
        val binding = AchievementItemBinding.inflate(inflater, parent, false)

        return AchievementHolder(binding)
    }

    override fun onBindViewHolder(holder: AchievementHolder, position: Int)
    {
        val achievement = achievements[position]
        holder.bind(achievement, onAchievementClicked)
    }

    override fun getItemCount(): Int
    {
        return achievements.size
    }
}