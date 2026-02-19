package com.android.personal.pentago

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.android.personal.pentago.databinding.FragmentProfileSettingsBinding
import com.android.personal.pentago.model.Marble
import com.android.personal.pentago.model.PlayerProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class ProfileSettingsFragment : Fragment()
{
    private var _binding: FragmentProfileSettingsBinding? = null
    private val binding
        get() = checkNotNull(_binding) { "The FragmentProfileSettingsBinding instance could not be accessed because it is currently null." }
    private val profileSettingsViewModel: ProfileSettingsViewModel by viewModels()
    private lateinit var player1Profile: PlayerProfile
    private lateinit var player2Profile: PlayerProfile
    /* I decided to created parameters to avoid having to repeatedly access the database as that could be an expensive process. */


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View?
    {
        _binding = FragmentProfileSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?)
    {
        super.onViewCreated(view, savedInstanceState)
        Log.d("onViewCreated PSF1", "onViewCreated was called")

        viewLifecycleOwner.lifecycleScope.launch() //Lifecycle scope is used because this work can be stopped if the view is destroyed.
        {
            withContext(Dispatchers.IO)
            {
                PentagoRepository.get().mutex.withLock()
                {
                    var players = PentagoRepository.get().getPlayerProfiles()
                    player1Profile = players.get(0) //With the current implementation, player 1 will always be the user with lowest playerId (incase the app ever gets extended to allow multiple users)
                    player2Profile = players.get(1) //Player 2 will always be the second lowest playerId
                    Log.d("onViewCreated PSF2", "Retrieved both player profiles")
                }
            }

            withContext(Dispatchers.Main)
            {
                binding.apply()
                {
                    if(!profileSettingsViewModel.hasP1UsernameStringInitialised()) //The first time the Fragment is created
                    {
                        profileSettingsViewModel.currentPlayer1UsernameString = player1Profile.userName
                    }
                    if(!profileSettingsViewModel.hasP2UsernameStringInitialised()) //For the first time the fragment is created
                    {
                        profileSettingsViewModel.currentPlayer2UsernameString = player2Profile.userName
                    }

                    player1UsernameEditText.setText(profileSettingsViewModel.currentPlayer1UsernameString)
                    player2UsernameEditText.setText(profileSettingsViewModel.currentPlayer2UsernameString)

                    player1UsernameEditText.doOnTextChanged()
                    { text, _, _, _ -> //The underscores are unneeded parameters
                        profileSettingsViewModel.currentPlayer1UsernameString = text.toString()
                    }
                    player2UsernameEditText.doOnTextChanged()
                    { text, _, _, _ ->
                        profileSettingsViewModel.currentPlayer2UsernameString = text.toString()
                    }
                    player1AvatarImageView.setOnClickListener()
                    {
                        findNavController().navigate(ProfileSettingsFragmentDirections.actionProfileSettingsFragmentToProfileAvatarSelectFragment(player1Profile.playerId))
                    }
                    player2AvatarImageView.setOnClickListener()
                    {
                        findNavController().navigate(ProfileSettingsFragmentDirections.actionProfileSettingsFragmentToProfileAvatarSelectFragment(player2Profile.playerId))
                    }
                    player1MarbleColourImageView.setOnClickListener()
                    {
                        findNavController().navigate(ProfileSettingsFragmentDirections.actionProfileSettingsFragmentToMarbleColourSelectFragment(player1Profile.playerId))
                    }
                    player2MarbleColourImageView.setOnClickListener()
                    {
                        findNavController().navigate(ProfileSettingsFragmentDirections.actionProfileSettingsFragmentToMarbleColourSelectFragment(player2Profile.playerId))
                    }
                    helpButton.setOnClickListener()
                    {
                        profileSettingHelpDesc.visibility = View.VISIBLE
                    }
                    backButton.setOnClickListener()
                    {
                        if((player1UsernameEditText.text.toString() in PlayerProfile.activeUserNameSet && player1UsernameEditText.text.toString() != player1Profile.userName) || (player2UsernameEditText.text.toString() in PlayerProfile.activeUserNameSet && player2UsernameEditText.text.toString() != player2Profile.userName)) //Uniqueness Check. Makes sure that database is only updated if the username isnt actively being used and also isnt the username that was there initially (cause even if a username wasnt changed, it will be in the active set)
                        {
                            Toast.makeText(context, "Cannot update usernames! Player usernames must be unique.", Toast.LENGTH_LONG).show()
                        }
                        else
                        {
                            GlobalScope.launch()
                            {
                                withContext(Dispatchers.IO)
                                {
                                    PentagoRepository.get().mutex.withLock()
                                    {
                                        player1Profile.userName = player1UsernameEditText.text.toString()
                                        PentagoRepository.get().updatePlayerProfileUserName(player1Profile.playerId, player1Profile.userName)
                                    }
                                    PentagoRepository.get().mutex.withLock()
                                    {
                                        player2Profile.userName = player2UsernameEditText.text.toString()
                                        PentagoRepository.get().updatePlayerProfileUserName(player2Profile.playerId, player2Profile.userName)
                                    }
                                }
                            }
                            findNavController().popBackStack()
                        }
                    }

                    //Setting the Player Avatars

                    //Player 1 profile picture logic
                    when(player1Profile.profilePicture)
                    {
                        PlayerProfile.ANDROID_ROBOT_PP -> player1AvatarImageView.setImageResource(R.drawable.android)
                        PlayerProfile.BEACH_PP -> player1AvatarImageView.setImageResource(R.drawable.beach)
                        PlayerProfile.DEFAULT_PP -> player1AvatarImageView.setImageResource(R.drawable.default_avatar)
                        PlayerProfile.DESERT_PP -> player1AvatarImageView.setImageResource(R.drawable.desert)
                        PlayerProfile.GIRAFFE_PP -> player1AvatarImageView.setImageResource(R.drawable.giraffe)
                        PlayerProfile.LION_PP -> player1AvatarImageView.setImageResource(R.drawable.lion)
                        PlayerProfile.MOUNTAIN_PP -> player1AvatarImageView.setImageResource(R.drawable.mountain)
                        PlayerProfile.OSTRICH_PP -> player1AvatarImageView.setImageResource(R.drawable.ostrich)
                        PlayerProfile.TIGER_PP -> player1AvatarImageView.setImageResource(R.drawable.tiger)
                        PlayerProfile.TREE_PP -> player1AvatarImageView.setImageResource(R.drawable.tree)
                        PlayerProfile.ZEBRA_PP -> player1AvatarImageView.setImageResource(R.drawable.zebra)
                        else -> throw IllegalArgumentException("There is no valid image resource for Player 1 profile picture ${player1Profile.profilePicture}")
                    }

                    //Player 2 profile picture logic
                    when(player2Profile.profilePicture)
                    {
                        PlayerProfile.ANDROID_ROBOT_PP -> player2AvatarImageView.setImageResource(R.drawable.android)
                        PlayerProfile.BEACH_PP -> player2AvatarImageView.setImageResource(R.drawable.beach)
                        PlayerProfile.DEFAULT_PP -> player2AvatarImageView.setImageResource(R.drawable.default_avatar)
                        PlayerProfile.DESERT_PP -> player2AvatarImageView.setImageResource(R.drawable.desert)
                        PlayerProfile.GIRAFFE_PP -> player2AvatarImageView.setImageResource(R.drawable.giraffe)
                        PlayerProfile.LION_PP -> player2AvatarImageView.setImageResource(R.drawable.lion)
                        PlayerProfile.MOUNTAIN_PP -> player2AvatarImageView.setImageResource(R.drawable.mountain)
                        PlayerProfile.OSTRICH_PP -> player2AvatarImageView.setImageResource(R.drawable.ostrich)
                        PlayerProfile.TIGER_PP -> player2AvatarImageView.setImageResource(R.drawable.tiger)
                        PlayerProfile.TREE_PP -> player2AvatarImageView.setImageResource(R.drawable.tree)
                        PlayerProfile.ZEBRA_PP -> player2AvatarImageView.setImageResource(R.drawable.zebra)
                        else -> throw IllegalArgumentException("There is no valid image resource for Player 2 profile picture ${player2Profile.profilePicture}")
                    }

                    //Setting the Marble Colours

                    //Player 1 Marble colour logic
                    when(player1Profile.marbleColour)
                    {
                        Marble.RED_MARBLE -> player1MarbleColourImageView.setImageResource(R.drawable.red_baseline_circle_24)
                        Marble.ORANGE_MARBLE -> player1MarbleColourImageView.setImageResource(R.drawable.orange_baseline_circle_24)
                        Marble.YELLOW_MARBLE -> player1MarbleColourImageView.setImageResource(R.drawable.yellow_baseline_circle_24)
                        Marble.GREEN_MARBLE -> player1MarbleColourImageView.setImageResource(R.drawable.green_baseline_circle_24)
                        Marble.BLUE_MARBLE -> player1MarbleColourImageView.setImageResource(R.drawable.blue_baseline_circle_24)
                        Marble.PURPLE_MARBLE -> player1MarbleColourImageView.setImageResource(R.drawable.purple_baseline_circle_24)
                        Marble.PINK_MARBLE -> player1MarbleColourImageView.setImageResource(R.drawable.pink_baseline_circle_24)
                        Marble.BLACK_MARBLE -> player1MarbleColourImageView.setImageResource(R.drawable.black_baseline_circle_24)
                        else -> throw IllegalArgumentException("There is no valid image resource for Player 1 marble colour ${player1Profile.marbleColour}")
                    }

                    //Player 2 Marble colour logic
                    when(player2Profile.marbleColour)
                    {
                        Marble.RED_MARBLE -> player2MarbleColourImageView.setImageResource(R.drawable.red_baseline_circle_24)
                        Marble.ORANGE_MARBLE -> player2MarbleColourImageView.setImageResource(R.drawable.orange_baseline_circle_24)
                        Marble.YELLOW_MARBLE -> player2MarbleColourImageView.setImageResource(R.drawable.yellow_baseline_circle_24)
                        Marble.GREEN_MARBLE -> player2MarbleColourImageView.setImageResource(R.drawable.green_baseline_circle_24)
                        Marble.BLUE_MARBLE -> player2MarbleColourImageView.setImageResource(R.drawable.blue_baseline_circle_24)
                        Marble.PURPLE_MARBLE -> player2MarbleColourImageView.setImageResource(R.drawable.purple_baseline_circle_24)
                        Marble.PINK_MARBLE -> player2MarbleColourImageView.setImageResource(R.drawable.pink_baseline_circle_24)
                        Marble.BLACK_MARBLE -> player2MarbleColourImageView.setImageResource(R.drawable.black_baseline_circle_24)
                        else -> throw IllegalArgumentException("There is no valid image resource for Player 2 marble colour ${player1Profile.marbleColour}")
                    }
                }
            }
        }
    }

    override fun onDestroyView()
    {
        super.onDestroyView()

        _binding = null
    }
}