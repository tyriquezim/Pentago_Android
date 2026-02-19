package com.android.personal.pentago

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.Toast
import androidx.collection.emptyLongSet
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.postDelayed
import androidx.core.view.setPadding
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.transition.Visibility
import com.android.personal.pentago.databinding.FragmentGamePlayBinding
import com.android.personal.pentago.model.Achievement
import com.android.personal.pentago.model.Marble
import com.android.personal.pentago.model.PentagoBoard
import com.android.personal.pentago.model.PlayerProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class GamePlayFragment : Fragment()
{
    private var _binding: FragmentGamePlayBinding? = null
    private val binding: FragmentGamePlayBinding
        get() = checkNotNull(_binding) { "The FragmentGameplayBinding instance could not be accessed because it is currently null." }
    private val arguments: GamePlayFragmentArgs by navArgs()
    private lateinit var pentagoGameBoard: PentagoBoard
    private lateinit var uiPentagoBoard: Array<Array<ImageView?>>
    private lateinit var player1Profile: PlayerProfile
    private lateinit var player2Profile: PlayerProfile

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View?
    {
        // Inflate the layout for this fragment
        _binding = FragmentGamePlayBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?)
    {
        super.onViewCreated(view, savedInstanceState)

        viewLifecycleOwner.lifecycleScope.launch()
        {
            withContext(Dispatchers.IO)
            {
                PentagoRepository.get().mutex.withLock()
                {
                    val players = PentagoRepository.get().getPlayerProfiles()

                    player1Profile = players.get(0)

                    if(arguments.isAgainstAiOpponent)
                    {
                        player2Profile = players.get(2) //If the match is against the Ai opponent, set player 2 to the AI profile
                    }
                    else
                    {
                        player2Profile = players.get(1) //Else use the second human player profile
                    }
                    pentagoGameBoard = PentagoBoard(player1Profile, player2Profile, arguments.isAgainstAiOpponent)
                }
            }
            withContext(Dispatchers.Main)
            {
                binding.apply()
                {
                    initialiseUIGameBoard()
                    player1MatchName.text = player1Profile.userName
                    player2MatchName.text = player2Profile.userName
                    playerTurnTextview.text = pentagoGameBoard.currentTurnPlayerProfile.userName + "'s Turn"
                    gameStateHelpTextview.text = getText(R.string.game_state_help_place_marble)

                    activateGridCellClickListeners()

                    //Player 1 Profile Picture logic
                    when(player1Profile.profilePicture)
                    {
                        PlayerProfile.ANDROID_ROBOT_PP -> player1MatchIcon.setImageResource(R.drawable.android)
                        PlayerProfile.BEACH_PP -> player1MatchIcon.setImageResource(R.drawable.beach)
                        PlayerProfile.DEFAULT_PP -> player1MatchIcon.setImageResource(R.drawable.default_avatar)
                        PlayerProfile.DESERT_PP -> player1MatchIcon.setImageResource(R.drawable.desert)
                        PlayerProfile.GIRAFFE_PP -> player1MatchIcon.setImageResource(R.drawable.giraffe)
                        PlayerProfile.LION_PP -> player1MatchIcon.setImageResource(R.drawable.lion)
                        PlayerProfile.MOUNTAIN_PP -> player1MatchIcon.setImageResource(R.drawable.mountain)
                        PlayerProfile.OSTRICH_PP -> player1MatchIcon.setImageResource(R.drawable.ostrich)
                        PlayerProfile.TIGER_PP -> player1MatchIcon.setImageResource(R.drawable.tiger)
                        PlayerProfile.TREE_PP -> player1MatchIcon.setImageResource(R.drawable.tree)
                        PlayerProfile.ZEBRA_PP -> player1MatchIcon.setImageResource(R.drawable.zebra)
                        else -> throw IllegalArgumentException("There is no valid image resource for Player 1 profile picture ${player1Profile.profilePicture}")
                    }

                    //Player 2 profile picture logic
                    when(player2Profile.profilePicture)
                    {
                        PlayerProfile.ANDROID_ROBOT_PP -> player2MatchIcon.setImageResource(R.drawable.android)
                        PlayerProfile.BEACH_PP -> player2MatchIcon.setImageResource(R.drawable.beach)
                        PlayerProfile.DEFAULT_PP -> player2MatchIcon.setImageResource(R.drawable.default_avatar)
                        PlayerProfile.DESERT_PP -> player2MatchIcon.setImageResource(R.drawable.desert)
                        PlayerProfile.GIRAFFE_PP -> player2MatchIcon.setImageResource(R.drawable.giraffe)
                        PlayerProfile.LION_PP -> player2MatchIcon.setImageResource(R.drawable.lion)
                        PlayerProfile.MOUNTAIN_PP -> player2MatchIcon.setImageResource(R.drawable.mountain)
                        PlayerProfile.OSTRICH_PP -> player2MatchIcon.setImageResource(R.drawable.ostrich)
                        PlayerProfile.TIGER_PP -> player2MatchIcon.setImageResource(R.drawable.tiger)
                        PlayerProfile.TREE_PP -> player2MatchIcon.setImageResource(R.drawable.tree)
                        PlayerProfile.ZEBRA_PP -> player2MatchIcon.setImageResource(R.drawable.zebra)
                        PlayerProfile.AI_ROBOT_PP -> player2MatchIcon.setImageResource(R.drawable.ai_profile_picture)
                        else -> throw IllegalArgumentException("There is no valid image resource for Player 2 profile picture ${player2Profile.profilePicture}")
                    }

                    exitButton.setOnClickListener()
                    {
                        findNavController().navigate(GamePlayFragmentDirections.actionGamePlayFragmentToMainMenuFragment())
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

    private fun initialiseUIGameBoard()
    {
        uiPentagoBoard = Array<Array<ImageView?>>(6) { Array<ImageView?>(6) { null } }

        var currentChildIndex = 0

        binding.apply()
        {
            //Adding the upper left subgrid image views
            for(i in 0..2)
            {
                for(j in 0..2)
                {
                    uiPentagoBoard[i][j] = pentagoUpperLeftSubgrid.getChildAt(currentChildIndex) as ImageView
                    ++currentChildIndex
                }
            }

            currentChildIndex = 0

            //Adding the upper right subgrid image views
            for(i in 0..2)
            {
                for(j in 3..5)
                {
                    uiPentagoBoard[i][j] = pentagoUpperRightSubgrid.getChildAt(currentChildIndex) as ImageView
                    ++currentChildIndex
                }
            }

            currentChildIndex = 0

            //Adding the upper left subgrid image views
            for(i in 3..5)
            {
                for(j in 0..2)
                {
                    uiPentagoBoard[i][j] = pentagoLowerLeftSubgrid.getChildAt(currentChildIndex) as ImageView
                    ++currentChildIndex
                }
            }

            currentChildIndex = 0

            //Adding the upper left subgrid image views
            for(i in 3..5)
            {
                for(j in 3..5)
                {
                    uiPentagoBoard[i][j] = pentagoLowerRightSubgrid.getChildAt(currentChildIndex) as ImageView
                    ++currentChildIndex
                }
            }
        }
    }

    private fun activateGridCellClickListeners()
    {
        var winner: PlayerProfile? = null

        binding.apply()
        {
            for(i in 0..5)
            {
                for(j in 0..5)
                {
                    uiPentagoBoard[i][j]!!.setOnClickListener()
                    {
                        try
                        {
                            pentagoGameBoard.placeMarble(i, j)
                            onAchievementEarned(pentagoGameBoard.currentTurnPlayerProfile, pentagoGameBoard.currentTurnPlayerProfile.updateTotalMovesMade())
                            updateSingleMarbleCell(i, j)
                            winner = pentagoGameBoard.checkWinCondition(i, j)

                            if(winner != null)
                            {
                                onSinglePlayerWin(winner!!)
                            }
                            else
                            {
                                gameStateHelpTextview.text = getString(R.string.game_state_help_rotate_subgrid)
                                activateWholeSubgridClickListeners()
                            }
                        }
                        catch(e: IllegalArgumentException)
                        {
                            Toast.makeText(context, "There is already a marble here.", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }

    private fun onSinglePlayerWin(winner: PlayerProfile)
    {
        binding.playerTurnTextview.text = winner!!.userName + " Wins!!!"
        binding.gameStateHelpTextview.text = null

        onAchievementEarned(winner, winner.updateWins())

        if(winner == pentagoGameBoard.player1Profile)
        {
            onAchievementEarned(pentagoGameBoard.player2Profile, pentagoGameBoard.player2Profile.updateLosses())
        }
        else
        {
            if(winner == pentagoGameBoard.player2Profile)
            {
                onAchievementEarned(pentagoGameBoard.player1Profile, pentagoGameBoard.player1Profile.updateLosses())
            }
        }
        disableGridCellClickListeners()

        GlobalScope.launch()
        {
            PentagoRepository.get().mutex.withLock()
            {
                PentagoRepository.get().updatePlayerProfilePlayerStats(player1Profile.playerId, player1Profile.playerStats)
                PentagoRepository.get().updatePlayerProfilePlayerStats(player2Profile.playerId, player2Profile.playerStats)
            }
        }
    }

    private fun onDraw()
    {
        binding.playerTurnTextview.text =  "Draw!!!"
        binding.gameStateHelpTextview.text = null

        onAchievementEarned(pentagoGameBoard.player1Profile, pentagoGameBoard.player1Profile.updateDraws())
        onAchievementEarned(pentagoGameBoard.player2Profile, pentagoGameBoard.player2Profile.updateDraws())

        disableGridCellClickListeners()

        GlobalScope.launch()
        {
            PentagoRepository.get().mutex.withLock()
            {
                PentagoRepository.get().updatePlayerProfilePlayerStats(player1Profile.playerId, player1Profile.playerStats)
                PentagoRepository.get().updatePlayerProfilePlayerStats(player2Profile.playerId, player2Profile.playerStats)
            }
        }
    }

    private fun onAchievementEarned(playerProfile: PlayerProfile, earnedAchievements: List<Achievement>)
    {
        for(achievement in earnedAchievements)
        {
            if(!achievement.hasBeenDisplayed)
            {
                Toast.makeText(context, playerProfile.userName + " earned " + achievement.achievementTitle + "!", Toast.LENGTH_LONG).show()
                achievement.hasBeenDisplayed = true
            }
        }
    }

    private fun disableGridCellClickListeners()
    {
        for(i in 0..5)
        {
            for(j in 0..5)
            {
                uiPentagoBoard[i][j]!!.setOnClickListener(null)
            }
        }
    }

    private fun disableRotationClickListeners()
    {
        binding.apply()
        {
            clockwiseImageview.setOnClickListener(null)
            anticlockwiseImageview.setOnClickListener(null)
        }
    }

    //Changes the all grid cell clicklisteners to rotate the grid. I applied it to all cells because applying it to the subgrids themselves wasnt working
    private fun activateWholeSubgridClickListeners()
    {
        var winner: PlayerProfile? = null
        var didDrawOccur = false

        binding.apply()
        {
            //Upper Left Subgrid
            for(i in 0..2)
            {
                for(j in 0..2)
                {
                    uiPentagoBoard[i][j]!!.setOnClickListener()
                    {
                        rotateLinearLayout.visibility = View.VISIBLE
                        gameStateHelpTextview.text = getString(R.string.game_state_help_cw_aw)
                        removeAllSubgridBackgrounds()
                        pentagoUpperLeftSubgrid.background = ContextCompat.getDrawable(requireContext(), R.color.highlight_background_yellow)
                        clockwiseImageview.setOnClickListener()
                        {
                            disableRotationClickListeners()
                            pentagoUpperLeftSubgrid.background = null
                            pentagoGameBoard.rotateSubGrid(PentagoBoard.TOP_LEFT_SUBGRID, PentagoBoard.CLOCKWISE_ROTATION)
                            resizeSubGridAnimation(pentagoUpperLeftSubgrid, SHRINK_ANIMATION)
                            pentagoUpperLeftSubgrid.postDelayed(SHRINK_ANIMATION_DURATION)
                            {
                                rotateSubgridAnimation(pentagoUpperLeftSubgrid, PentagoBoard.CLOCKWISE_ROTATION)
                                pentagoUpperLeftSubgrid.postDelayed(ROTATION_ANIMATION_DURATION)
                                {
                                    instantUndoRotationAnimation(pentagoUpperLeftSubgrid, PentagoBoard.CLOCKWISE_ROTATION)
                                    updateUpperLeftSubGrid()
                                    resizeSubGridAnimation(pentagoUpperLeftSubgrid, GROW_ANIMATION)
                                    rotateLinearLayout.visibility = View.GONE
                                    gameStateHelpTextview.text = getString(R.string.game_state_help_place_marble)
                                    playerTurnTextview.text = pentagoGameBoard.currentTurnPlayerProfile.userName

                                    didDrawOccur = pentagoGameBoard.didDrawHappen()

                                    if(didDrawOccur)
                                    {
                                        onDraw()
                                    }
                                    else
                                    {
                                        winner = pentagoGameBoard.checkWinConditionPostRotation()
                                        if(winner != null)
                                        {
                                            onSinglePlayerWin(winner!!)
                                        }
                                        else
                                        {
                                            if(arguments.isAgainstAiOpponent)
                                            {
                                                pentagoUpperLeftSubgrid.postDelayed(GROW_ANIMATION_DURATION + AI_PLACE_MARBLE_WAIT_DURATION)
                                                {
                                                    lateinit var marbleLocation: Array<Int?>
                                                    lateinit var rotationInfo: Array<Int?>
                                                    marbleLocation = pentagoGameBoard.aiPlaceMarble()
                                                    updateSingleMarbleCell(marbleLocation[0]!!, marbleLocation[1]!!)
                                                    winner = pentagoGameBoard.checkWinCondition(marbleLocation[0]!!, marbleLocation[1]!!)

                                                    if(winner != null)
                                                    {
                                                        onSinglePlayerWin(winner!!)
                                                    }
                                                    else
                                                    {
                                                        pentagoUpperLeftSubgrid.postDelayed(AI_ROTATION_WAIT_DURATION)
                                                        {
                                                            rotationInfo = pentagoGameBoard.aiRotateSubGrid()
                                                            aiRotationAnimation(rotationInfo)
                                                        }
                                                    }
                                                }

                                            }
                                            else
                                            {
                                                activateGridCellClickListeners()
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        anticlockwiseImageview.setOnClickListener()
                        {
                            disableRotationClickListeners()
                            pentagoUpperLeftSubgrid.background = null
                            pentagoGameBoard.rotateSubGrid(PentagoBoard.TOP_LEFT_SUBGRID, PentagoBoard.ANTI_CLOCKWISE_ROTATION)
                            resizeSubGridAnimation(pentagoUpperLeftSubgrid, SHRINK_ANIMATION)
                            pentagoUpperLeftSubgrid.postDelayed(SHRINK_ANIMATION_DURATION)
                            {
                                rotateSubgridAnimation(pentagoUpperLeftSubgrid, PentagoBoard.ANTI_CLOCKWISE_ROTATION)
                                pentagoUpperLeftSubgrid.postDelayed(ROTATION_ANIMATION_DURATION)
                                {
                                    instantUndoRotationAnimation(pentagoUpperLeftSubgrid, PentagoBoard.ANTI_CLOCKWISE_ROTATION)
                                    updateUpperLeftSubGrid()
                                    resizeSubGridAnimation(pentagoUpperLeftSubgrid, GROW_ANIMATION)
                                    rotateLinearLayout.visibility = View.GONE
                                    gameStateHelpTextview.text = getString(R.string.game_state_help_place_marble)
                                    playerTurnTextview.text = pentagoGameBoard.currentTurnPlayerProfile.userName

                                    didDrawOccur = pentagoGameBoard.didDrawHappen()

                                    if(didDrawOccur)
                                    {
                                        onDraw()
                                    }
                                    else
                                    {
                                        winner = pentagoGameBoard.checkWinConditionPostRotation()
                                        if(winner != null)
                                        {
                                            onSinglePlayerWin(winner!!)
                                        }
                                        else
                                        {
                                            if(arguments.isAgainstAiOpponent)
                                            {
                                                pentagoUpperLeftSubgrid.postDelayed(GROW_ANIMATION_DURATION + AI_PLACE_MARBLE_WAIT_DURATION)
                                                {
                                                    lateinit var marbleLocation: Array<Int?>
                                                    lateinit var rotationInfo: Array<Int?>
                                                    marbleLocation = pentagoGameBoard.aiPlaceMarble()
                                                    updateSingleMarbleCell(marbleLocation[0]!!, marbleLocation[1]!!)
                                                    winner = pentagoGameBoard.checkWinCondition(marbleLocation[0]!!, marbleLocation[1]!!)

                                                    if(winner != null)
                                                    {
                                                        onSinglePlayerWin(winner!!)
                                                    }
                                                    else
                                                    {
                                                        pentagoUpperLeftSubgrid.postDelayed(AI_ROTATION_WAIT_DURATION)
                                                        {
                                                            rotationInfo = pentagoGameBoard.aiRotateSubGrid()
                                                            aiRotationAnimation(rotationInfo)
                                                        }
                                                    }
                                                }
                                            }
                                            else
                                            {
                                                activateGridCellClickListeners()
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            //Upper Right Subgrid
            for(i in 0..2)
            {
                for(j in 3..5)
                {
                    uiPentagoBoard[i][j]!!.setOnClickListener()
                    {
                        rotateLinearLayout.visibility = View.VISIBLE
                        gameStateHelpTextview.text = getString(R.string.game_state_help_cw_aw)
                        removeAllSubgridBackgrounds()
                        pentagoUpperRightSubgrid.background = ContextCompat.getDrawable(requireContext(), R.color.highlight_background_yellow)
                        clockwiseImageview.setOnClickListener()
                        {
                            disableRotationClickListeners()
                            pentagoUpperRightSubgrid.background = null
                            pentagoGameBoard.rotateSubGrid(PentagoBoard.TOP_RIGHT_SUBGRID, PentagoBoard.CLOCKWISE_ROTATION)
                            resizeSubGridAnimation(pentagoUpperRightSubgrid, SHRINK_ANIMATION)
                            pentagoUpperRightSubgrid.postDelayed(SHRINK_ANIMATION_DURATION)
                            {
                                rotateSubgridAnimation(pentagoUpperRightSubgrid, PentagoBoard.CLOCKWISE_ROTATION)
                                pentagoUpperRightSubgrid.postDelayed(ROTATION_ANIMATION_DURATION)
                                {
                                    instantUndoRotationAnimation(pentagoUpperRightSubgrid, PentagoBoard.CLOCKWISE_ROTATION)
                                    updateUpperRightSubGrid()
                                    resizeSubGridAnimation(pentagoUpperRightSubgrid, GROW_ANIMATION)
                                    rotateLinearLayout.visibility = View.GONE
                                    gameStateHelpTextview.text = getString(R.string.game_state_help_place_marble)
                                    playerTurnTextview.text = pentagoGameBoard.currentTurnPlayerProfile.userName

                                    didDrawOccur = pentagoGameBoard.didDrawHappen()

                                    if(didDrawOccur)
                                    {
                                        onDraw()
                                    }
                                    else
                                    {
                                        winner = pentagoGameBoard.checkWinConditionPostRotation()
                                        if(winner != null)
                                        {
                                            onSinglePlayerWin(winner!!)
                                        }
                                        else
                                        {
                                            if(arguments.isAgainstAiOpponent)
                                            {
                                                pentagoUpperRightSubgrid.postDelayed(GROW_ANIMATION_DURATION + AI_PLACE_MARBLE_WAIT_DURATION)
                                                {
                                                    lateinit var marbleLocation: Array<Int?>
                                                    lateinit var rotationInfo: Array<Int?>
                                                    marbleLocation = pentagoGameBoard.aiPlaceMarble()
                                                    updateSingleMarbleCell(marbleLocation[0]!!, marbleLocation[1]!!)
                                                    winner = pentagoGameBoard.checkWinCondition(marbleLocation[0]!!, marbleLocation[1]!!)

                                                    if(winner != null)
                                                    {
                                                        onSinglePlayerWin(winner!!)
                                                    }
                                                    else
                                                    {
                                                        pentagoUpperRightSubgrid.postDelayed(AI_ROTATION_WAIT_DURATION)
                                                        {
                                                            rotationInfo = pentagoGameBoard.aiRotateSubGrid()
                                                            aiRotationAnimation(rotationInfo)
                                                        }
                                                    }
                                                }
                                            }
                                            else
                                            {
                                                activateGridCellClickListeners()
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        anticlockwiseImageview.setOnClickListener()
                        {
                            disableRotationClickListeners()
                            pentagoUpperRightSubgrid.background = null
                            pentagoGameBoard.rotateSubGrid(PentagoBoard.TOP_RIGHT_SUBGRID, PentagoBoard.ANTI_CLOCKWISE_ROTATION)
                            resizeSubGridAnimation(pentagoUpperRightSubgrid, SHRINK_ANIMATION)
                            pentagoUpperRightSubgrid.postDelayed(SHRINK_ANIMATION_DURATION)
                            {
                                rotateSubgridAnimation(pentagoUpperRightSubgrid, PentagoBoard.ANTI_CLOCKWISE_ROTATION)
                                pentagoUpperRightSubgrid.postDelayed(ROTATION_ANIMATION_DURATION)
                                {
                                    instantUndoRotationAnimation(pentagoUpperRightSubgrid, PentagoBoard.ANTI_CLOCKWISE_ROTATION)
                                    updateUpperRightSubGrid()
                                    resizeSubGridAnimation(pentagoUpperRightSubgrid, GROW_ANIMATION)
                                    rotateLinearLayout.visibility = View.GONE
                                    gameStateHelpTextview.text = getString(R.string.game_state_help_place_marble)
                                    playerTurnTextview.text = pentagoGameBoard.currentTurnPlayerProfile.userName

                                    didDrawOccur = pentagoGameBoard.didDrawHappen()

                                    if(didDrawOccur)
                                    {
                                        onDraw()
                                    }
                                    else
                                    {
                                        winner = pentagoGameBoard.checkWinConditionPostRotation()
                                        if(winner != null)
                                        {
                                            onSinglePlayerWin(winner!!)
                                        }
                                        else
                                        {
                                            if(arguments.isAgainstAiOpponent)
                                            {
                                                pentagoUpperRightSubgrid.postDelayed(GROW_ANIMATION_DURATION + AI_PLACE_MARBLE_WAIT_DURATION)
                                                {
                                                    lateinit var marbleLocation: Array<Int?>
                                                    lateinit var rotationInfo: Array<Int?>
                                                    marbleLocation = pentagoGameBoard.aiPlaceMarble()
                                                    updateSingleMarbleCell(marbleLocation[0]!!, marbleLocation[1]!!)
                                                    winner = pentagoGameBoard.checkWinCondition(marbleLocation[0]!!, marbleLocation[1]!!)

                                                    if(winner != null)
                                                    {
                                                        onSinglePlayerWin(winner!!)
                                                    }
                                                    else
                                                    {
                                                        pentagoUpperRightSubgrid.postDelayed(AI_ROTATION_WAIT_DURATION)
                                                        {
                                                            rotationInfo = pentagoGameBoard.aiRotateSubGrid()
                                                            aiRotationAnimation(rotationInfo)
                                                        }
                                                    }
                                                }
                                            }
                                            else
                                            {
                                                activateGridCellClickListeners()
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            //Lower Left subgrid
            for(i in 3..5)
            {
                for(j in 0..2)
                {
                    uiPentagoBoard[i][j]!!.setOnClickListener()
                    {
                        rotateLinearLayout.visibility = View.VISIBLE
                        gameStateHelpTextview.text = getString(R.string.game_state_help_cw_aw)
                        removeAllSubgridBackgrounds()
                        pentagoLowerLeftSubgrid.background = ContextCompat.getDrawable(requireContext(), R.color.highlight_background_yellow)
                        clockwiseImageview.setOnClickListener()
                        {
                            disableRotationClickListeners()
                            pentagoLowerLeftSubgrid.background = null
                            pentagoGameBoard.rotateSubGrid(PentagoBoard.BOTTOM_LEFT_SUBGRID, PentagoBoard.CLOCKWISE_ROTATION)
                            resizeSubGridAnimation(pentagoLowerLeftSubgrid, SHRINK_ANIMATION)
                            pentagoLowerLeftSubgrid.postDelayed(SHRINK_ANIMATION_DURATION)
                            {
                                rotateSubgridAnimation(pentagoLowerLeftSubgrid, PentagoBoard.CLOCKWISE_ROTATION)
                                pentagoLowerLeftSubgrid.postDelayed(ROTATION_ANIMATION_DURATION)
                                {
                                    instantUndoRotationAnimation(pentagoLowerLeftSubgrid, PentagoBoard.CLOCKWISE_ROTATION)
                                    updateLowerLeftSubGrid()
                                    resizeSubGridAnimation(pentagoLowerLeftSubgrid, GROW_ANIMATION)
                                    rotateLinearLayout.visibility = View.GONE
                                    gameStateHelpTextview.text = getString(R.string.game_state_help_place_marble)
                                    playerTurnTextview.text = pentagoGameBoard.currentTurnPlayerProfile.userName

                                    didDrawOccur = pentagoGameBoard.didDrawHappen()

                                    if(didDrawOccur)
                                    {
                                        onDraw()
                                    }
                                    else
                                    {
                                        winner = pentagoGameBoard.checkWinConditionPostRotation()
                                        if(winner != null)
                                        {
                                            onSinglePlayerWin(winner!!)
                                        }
                                        else
                                        {
                                            if(arguments.isAgainstAiOpponent)
                                            {
                                                pentagoLowerLeftSubgrid.postDelayed(GROW_ANIMATION_DURATION + AI_PLACE_MARBLE_WAIT_DURATION)
                                                {
                                                    lateinit var marbleLocation: Array<Int?>
                                                    lateinit var rotationInfo: Array<Int?>
                                                    marbleLocation = pentagoGameBoard.aiPlaceMarble()
                                                    updateSingleMarbleCell(marbleLocation[0]!!, marbleLocation[1]!!)
                                                    winner = pentagoGameBoard.checkWinCondition(marbleLocation[0]!!, marbleLocation[1]!!)

                                                    if(winner != null)
                                                    {
                                                        onSinglePlayerWin(winner!!)
                                                    }
                                                    else
                                                    {
                                                        pentagoLowerLeftSubgrid.postDelayed(AI_ROTATION_WAIT_DURATION)
                                                        {
                                                            rotationInfo = pentagoGameBoard.aiRotateSubGrid()
                                                            aiRotationAnimation(rotationInfo)
                                                        }
                                                    }
                                                }
                                            }
                                            else
                                            {
                                                activateGridCellClickListeners()
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        anticlockwiseImageview.setOnClickListener()
                        {
                            disableRotationClickListeners()
                            pentagoLowerLeftSubgrid.background = null
                            pentagoGameBoard.rotateSubGrid(PentagoBoard.BOTTOM_LEFT_SUBGRID, PentagoBoard.ANTI_CLOCKWISE_ROTATION)
                            resizeSubGridAnimation(pentagoLowerLeftSubgrid, SHRINK_ANIMATION)
                            pentagoLowerLeftSubgrid.postDelayed(SHRINK_ANIMATION_DURATION)
                            {
                                rotateSubgridAnimation(pentagoLowerLeftSubgrid, PentagoBoard.ANTI_CLOCKWISE_ROTATION)
                                pentagoLowerLeftSubgrid.postDelayed(ROTATION_ANIMATION_DURATION)
                                {
                                    instantUndoRotationAnimation(pentagoLowerLeftSubgrid, PentagoBoard.ANTI_CLOCKWISE_ROTATION)
                                    updateLowerLeftSubGrid()
                                    resizeSubGridAnimation(pentagoLowerLeftSubgrid, GROW_ANIMATION)
                                    rotateLinearLayout.visibility = View.GONE
                                    gameStateHelpTextview.text = getString(R.string.game_state_help_place_marble)
                                    playerTurnTextview.text = pentagoGameBoard.currentTurnPlayerProfile.userName

                                    didDrawOccur = pentagoGameBoard.didDrawHappen()

                                    if(didDrawOccur)
                                    {
                                        onDraw()
                                    }
                                    else
                                    {
                                        winner = pentagoGameBoard.checkWinConditionPostRotation()
                                        if(winner != null)
                                        {
                                            onSinglePlayerWin(winner!!)
                                        }
                                        else
                                        {
                                            if(arguments.isAgainstAiOpponent)
                                            {
                                                pentagoLowerLeftSubgrid.postDelayed(GROW_ANIMATION_DURATION + AI_PLACE_MARBLE_WAIT_DURATION)
                                                {
                                                    lateinit var marbleLocation: Array<Int?>
                                                    lateinit var rotationInfo: Array<Int?>
                                                    marbleLocation = pentagoGameBoard.aiPlaceMarble()
                                                    updateSingleMarbleCell(marbleLocation[0]!!, marbleLocation[1]!!)
                                                    winner = pentagoGameBoard.checkWinCondition(marbleLocation[0]!!, marbleLocation[1]!!)

                                                    if(winner != null)
                                                    {
                                                        onSinglePlayerWin(winner!!)
                                                    }
                                                    else
                                                    {
                                                        pentagoLowerLeftSubgrid.postDelayed(AI_ROTATION_WAIT_DURATION)
                                                        {
                                                            rotationInfo = pentagoGameBoard.aiRotateSubGrid()
                                                            aiRotationAnimation(rotationInfo)
                                                        }
                                                    }
                                                }
                                            }
                                            else
                                            {
                                                activateGridCellClickListeners()
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            //Lower Right Subgrid
            for(i in 3..5)
            {
                for(j in 3..5)
                {
                    uiPentagoBoard[i][j]!!.setOnClickListener()
                    {
                        rotateLinearLayout.visibility = View.VISIBLE
                        gameStateHelpTextview.text = getString(R.string.game_state_help_cw_aw)
                        removeAllSubgridBackgrounds()
                        pentagoLowerRightSubgrid.background = ContextCompat.getDrawable(requireContext(), R.color.highlight_background_yellow)
                        clockwiseImageview.setOnClickListener()
                        {
                            disableRotationClickListeners()
                            pentagoLowerRightSubgrid.background = null
                            pentagoGameBoard.rotateSubGrid(PentagoBoard.BOTTOM_RIGHT_SUBGRID, PentagoBoard.CLOCKWISE_ROTATION)
                            resizeSubGridAnimation(pentagoLowerRightSubgrid, SHRINK_ANIMATION)
                            pentagoLowerRightSubgrid.postDelayed(SHRINK_ANIMATION_DURATION)
                            {
                                rotateSubgridAnimation(pentagoLowerRightSubgrid, PentagoBoard.CLOCKWISE_ROTATION)
                                pentagoLowerRightSubgrid.postDelayed(ROTATION_ANIMATION_DURATION)
                                {
                                    instantUndoRotationAnimation(pentagoLowerRightSubgrid, PentagoBoard.CLOCKWISE_ROTATION)
                                    updateLowerRightSubGrid()
                                    resizeSubGridAnimation(pentagoLowerRightSubgrid, GROW_ANIMATION)
                                    rotateLinearLayout.visibility = View.GONE
                                    gameStateHelpTextview.text = getString(R.string.game_state_help_place_marble)
                                    playerTurnTextview.text = pentagoGameBoard.currentTurnPlayerProfile.userName

                                    didDrawOccur = pentagoGameBoard.didDrawHappen()

                                    if(didDrawOccur)
                                    {
                                        onDraw()
                                    }
                                    else
                                    {
                                        winner = pentagoGameBoard.checkWinConditionPostRotation()
                                        if(winner != null)
                                        {
                                            onSinglePlayerWin(winner!!)
                                        }
                                        else
                                        {
                                            if(arguments.isAgainstAiOpponent)
                                            {
                                                pentagoLowerRightSubgrid.postDelayed(GROW_ANIMATION_DURATION + AI_PLACE_MARBLE_WAIT_DURATION)
                                                {
                                                    lateinit var marbleLocation: Array<Int?>
                                                    lateinit var rotationInfo: Array<Int?>
                                                    marbleLocation = pentagoGameBoard.aiPlaceMarble()
                                                    updateSingleMarbleCell(marbleLocation[0]!!, marbleLocation[1]!!)
                                                    winner = pentagoGameBoard.checkWinCondition(marbleLocation[0]!!, marbleLocation[1]!!)

                                                    if(winner != null)
                                                    {
                                                        onSinglePlayerWin(winner!!)
                                                    }
                                                    else
                                                    {
                                                        pentagoLowerRightSubgrid.postDelayed(AI_ROTATION_WAIT_DURATION)
                                                        {
                                                            rotationInfo = pentagoGameBoard.aiRotateSubGrid()
                                                            aiRotationAnimation(rotationInfo)
                                                        }
                                                    }
                                                }
                                            }
                                            else
                                            {
                                                activateGridCellClickListeners()
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        anticlockwiseImageview.setOnClickListener()
                        {
                            disableRotationClickListeners()
                            pentagoLowerRightSubgrid.background = null
                            pentagoGameBoard.rotateSubGrid(PentagoBoard.BOTTOM_RIGHT_SUBGRID, PentagoBoard.ANTI_CLOCKWISE_ROTATION)
                            resizeSubGridAnimation(pentagoLowerRightSubgrid, SHRINK_ANIMATION)
                            pentagoLowerRightSubgrid.postDelayed(SHRINK_ANIMATION_DURATION)
                            {
                                rotateSubgridAnimation(pentagoLowerRightSubgrid, PentagoBoard.ANTI_CLOCKWISE_ROTATION)
                                pentagoLowerRightSubgrid.postDelayed(ROTATION_ANIMATION_DURATION)
                                {
                                    instantUndoRotationAnimation(pentagoLowerRightSubgrid, PentagoBoard.ANTI_CLOCKWISE_ROTATION)
                                    updateLowerRightSubGrid()
                                    resizeSubGridAnimation(pentagoLowerRightSubgrid, GROW_ANIMATION)
                                    rotateLinearLayout.visibility = View.GONE
                                    gameStateHelpTextview.text = getString(R.string.game_state_help_place_marble)
                                    playerTurnTextview.text = pentagoGameBoard.currentTurnPlayerProfile.userName

                                    didDrawOccur = pentagoGameBoard.didDrawHappen()

                                    if(didDrawOccur)
                                    {
                                        onDraw()
                                    }
                                    else
                                    {
                                        winner = pentagoGameBoard.checkWinConditionPostRotation()
                                        if(winner != null)
                                        {
                                            onSinglePlayerWin(winner!!)
                                        }
                                        else
                                        {
                                            if(arguments.isAgainstAiOpponent)
                                            {
                                                pentagoLowerRightSubgrid.postDelayed(GROW_ANIMATION_DURATION + AI_PLACE_MARBLE_WAIT_DURATION)
                                                {
                                                    lateinit var marbleLocation: Array<Int?>
                                                    lateinit var rotationInfo: Array<Int?>
                                                    marbleLocation = pentagoGameBoard.aiPlaceMarble()
                                                    updateSingleMarbleCell(marbleLocation[0]!!, marbleLocation[1]!!)
                                                    winner = pentagoGameBoard.checkWinCondition(marbleLocation[0]!!, marbleLocation[1]!!)

                                                    if(winner != null)
                                                    {
                                                        onSinglePlayerWin(winner!!)
                                                    }
                                                    else
                                                    {
                                                        pentagoLowerRightSubgrid.postDelayed(AI_ROTATION_WAIT_DURATION)
                                                        {
                                                            rotationInfo = pentagoGameBoard.aiRotateSubGrid()
                                                            aiRotationAnimation(rotationInfo)
                                                        }
                                                    }
                                                }
                                            }
                                            else
                                            {
                                                activateGridCellClickListeners()
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun removeAllSubgridBackgrounds()
    {
        binding.apply()
        {
            pentagoUpperLeftSubgrid.background = null
            pentagoUpperRightSubgrid.background = null
            pentagoLowerLeftSubgrid.background = null
            pentagoLowerRightSubgrid.background = null
        }
    }

    private fun updateAllUIGrids()
    {
        updateUpperLeftSubGrid()
        updateUpperRightSubGrid()
        updateLowerLeftSubGrid()
        updateLowerRightSubGrid()
    }

    private fun updateUpperLeftSubGrid()
    {
        for(i in 0..2)
        {
            for(j in 0..2)
            {
                if(pentagoGameBoard.pentagoBoard[i][j] != null)
                {
                    updateSingleMarbleCell(i, j)
                }
                else
                {
                    updateEmptyCell(i, j)
                }
            }
        }
    }

    private fun updateUpperRightSubGrid()
    {
        for(i in 0..2)
        {
            for(j in 3..5)
            {
                if(pentagoGameBoard.pentagoBoard[i][j] != null)
                {
                    updateSingleMarbleCell(i, j)
                }
                else
                {
                    updateEmptyCell(i, j)
                }
            }
        }
    }

    private fun updateLowerLeftSubGrid()
    {
        for(i in 3..5)
        {
            for(j in 0..2)
            {
                if(pentagoGameBoard.pentagoBoard[i][j] != null)
                {
                    updateSingleMarbleCell(i, j)
                }
                else
                {
                    updateEmptyCell(i, j)
                }
            }
        }
    }

    private fun updateLowerRightSubGrid()
    {
        for(i in 3..5)
        {
            for(j in 3..5)
            {
                if(pentagoGameBoard.pentagoBoard[i][j] != null)
                {
                    updateSingleMarbleCell(i, j)
                }
                else
                {
                    updateEmptyCell(i, j)
                }
            }
        }
    }

    private fun updateSingleMarbleCell(rowIndex: Int, columnIndex: Int)
    {
        if(rowIndex < 0 || rowIndex > 5)
        {
            throw IllegalArgumentException("The rowIndex must be between 0 and 5. rowIndex received $rowIndex")
        }
        if(columnIndex < 0 || columnIndex > 5)
        {
            throw IllegalArgumentException("The columnIndex must be between 0 and 5. columnIndex received $rowIndex")
        }

        when(rowIndex)
        {
            0 ->
            {
                when(columnIndex)
                {
                    0 ->
                    {
                        when(pentagoGameBoard.pentagoBoard[rowIndex][columnIndex]!!.marbleColour)
                        {
                            Marble.RED_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.red_upper_left_cell)
                            Marble.ORANGE_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.orange_upper_left_cell)
                            Marble.YELLOW_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.yellow_upper_left_cell)
                            Marble.GREEN_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.green_upper_left_cell)
                            Marble.BLUE_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.blue_upper_left_cell)
                            Marble.PURPLE_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.purple_upper_left_cell)
                            Marble.PINK_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.pink_upper_left_cell)
                            Marble.BLACK_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.black_upper_left_cell)
                            Marble.METALLIC_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.metallic_upper_left_cell)
                        }
                    }

                    1 ->
                    {
                        when(pentagoGameBoard.pentagoBoard[rowIndex][columnIndex]!!.marbleColour)
                        {
                            Marble.RED_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.red_general_cell)
                            Marble.ORANGE_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.orange_general_cell)
                            Marble.YELLOW_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.yellow_general_cell)
                            Marble.GREEN_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.green_general_cell)
                            Marble.BLUE_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.blue_general_cell)
                            Marble.PURPLE_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.purple_general_cell)
                            Marble.PINK_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.pink_general_cell)
                            Marble.BLACK_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.black_general_cell)
                            Marble.METALLIC_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.metallic_general_cell)
                        }
                    }

                    2 ->
                    {
                        when(pentagoGameBoard.pentagoBoard[rowIndex][columnIndex]!!.marbleColour)
                        {
                            Marble.RED_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.red_upper_right_cell)
                            Marble.ORANGE_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.orange_upper_right_cell)
                            Marble.YELLOW_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.yellow_upper_right_cell)
                            Marble.GREEN_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.green_upper_right_cell)
                            Marble.BLUE_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.blue_upper_right_cell)
                            Marble.PURPLE_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.purple_upper_right_cell)
                            Marble.PINK_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.pink_upper_right_cell)
                            Marble.BLACK_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.black_upper_right_cell)
                            Marble.METALLIC_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.metallic_upper_right_cell)
                        }
                    }

                    3 ->
                    {
                        when(pentagoGameBoard.pentagoBoard[rowIndex][columnIndex]!!.marbleColour)
                        {
                            Marble.RED_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.red_upper_left_cell)
                            Marble.ORANGE_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.orange_upper_left_cell)
                            Marble.YELLOW_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.yellow_upper_left_cell)
                            Marble.GREEN_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.green_upper_left_cell)
                            Marble.BLUE_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.blue_upper_left_cell)
                            Marble.PURPLE_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.purple_upper_left_cell)
                            Marble.PINK_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.pink_upper_left_cell)
                            Marble.BLACK_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.black_upper_left_cell)
                            Marble.METALLIC_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.metallic_upper_left_cell)
                        }
                    }

                    4 ->
                    {
                        when(pentagoGameBoard.pentagoBoard[rowIndex][columnIndex]!!.marbleColour)
                        {
                            Marble.RED_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.red_general_cell)
                            Marble.ORANGE_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.orange_general_cell)
                            Marble.YELLOW_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.yellow_general_cell)
                            Marble.GREEN_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.green_general_cell)
                            Marble.BLUE_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.blue_general_cell)
                            Marble.PURPLE_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.purple_general_cell)
                            Marble.PINK_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.pink_general_cell)
                            Marble.BLACK_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.black_general_cell)
                            Marble.METALLIC_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.metallic_general_cell)
                        }
                    }

                    5 ->
                    {
                        when(pentagoGameBoard.pentagoBoard[rowIndex][columnIndex]!!.marbleColour)
                        {
                            Marble.RED_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.red_upper_right_cell)
                            Marble.ORANGE_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.orange_upper_right_cell)
                            Marble.YELLOW_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.yellow_upper_right_cell)
                            Marble.GREEN_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.green_upper_right_cell)
                            Marble.BLUE_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.blue_upper_right_cell)
                            Marble.PURPLE_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.purple_upper_right_cell)
                            Marble.PINK_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.pink_upper_right_cell)
                            Marble.BLACK_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.black_upper_right_cell)
                            Marble.METALLIC_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.metallic_upper_right_cell)
                        }
                    }
                }
            }

            1 ->
            {
                when(columnIndex)
                {
                    0 ->
                    {
                        when(pentagoGameBoard.pentagoBoard[rowIndex][columnIndex]!!.marbleColour)
                        {
                            Marble.RED_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.red_general_cell)
                            Marble.ORANGE_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.orange_general_cell)
                            Marble.YELLOW_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.yellow_general_cell)
                            Marble.GREEN_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.green_general_cell)
                            Marble.BLUE_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.blue_general_cell)
                            Marble.PURPLE_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.purple_general_cell)
                            Marble.PINK_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.pink_general_cell)
                            Marble.BLACK_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.black_general_cell)
                            Marble.METALLIC_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.metallic_general_cell)
                        }
                    }

                    1 ->
                    {
                        when(pentagoGameBoard.pentagoBoard[rowIndex][columnIndex]!!.marbleColour)
                        {
                            Marble.RED_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.red_general_cell)
                            Marble.ORANGE_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.orange_general_cell)
                            Marble.YELLOW_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.yellow_general_cell)
                            Marble.GREEN_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.green_general_cell)
                            Marble.BLUE_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.blue_general_cell)
                            Marble.PURPLE_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.purple_general_cell)
                            Marble.PINK_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.pink_general_cell)
                            Marble.BLACK_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.black_general_cell)
                            Marble.METALLIC_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.metallic_general_cell)
                        }
                    }

                    2 ->
                    {
                        when(pentagoGameBoard.pentagoBoard[rowIndex][columnIndex]!!.marbleColour)
                        {
                            Marble.RED_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.red_general_cell)
                            Marble.ORANGE_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.orange_general_cell)
                            Marble.YELLOW_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.yellow_general_cell)
                            Marble.GREEN_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.green_general_cell)
                            Marble.BLUE_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.blue_general_cell)
                            Marble.PURPLE_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.purple_general_cell)
                            Marble.PINK_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.pink_general_cell)
                            Marble.BLACK_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.black_general_cell)
                            Marble.METALLIC_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.metallic_general_cell)
                        }
                    }

                    3 ->
                    {
                        when(pentagoGameBoard.pentagoBoard[rowIndex][columnIndex]!!.marbleColour)
                        {
                            Marble.RED_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.red_general_cell)
                            Marble.ORANGE_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.orange_general_cell)
                            Marble.YELLOW_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.yellow_general_cell)
                            Marble.GREEN_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.green_general_cell)
                            Marble.BLUE_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.blue_general_cell)
                            Marble.PURPLE_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.purple_general_cell)
                            Marble.PINK_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.pink_general_cell)
                            Marble.BLACK_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.black_general_cell)
                            Marble.METALLIC_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.metallic_general_cell)
                        }
                    }

                    4 ->
                    {
                        when(pentagoGameBoard.pentagoBoard[rowIndex][columnIndex]!!.marbleColour)
                        {
                            Marble.RED_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.red_general_cell)
                            Marble.ORANGE_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.orange_general_cell)
                            Marble.YELLOW_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.yellow_general_cell)
                            Marble.GREEN_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.green_general_cell)
                            Marble.BLUE_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.blue_general_cell)
                            Marble.PURPLE_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.purple_general_cell)
                            Marble.PINK_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.pink_general_cell)
                            Marble.BLACK_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.black_general_cell)
                            Marble.METALLIC_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.metallic_general_cell)
                        }
                    }

                    5 ->
                    {
                        when(pentagoGameBoard.pentagoBoard[rowIndex][columnIndex]!!.marbleColour)
                        {
                            Marble.RED_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.red_general_cell)
                            Marble.ORANGE_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.orange_general_cell)
                            Marble.YELLOW_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.yellow_general_cell)
                            Marble.GREEN_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.green_general_cell)
                            Marble.BLUE_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.blue_general_cell)
                            Marble.PURPLE_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.purple_general_cell)
                            Marble.PINK_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.pink_general_cell)
                            Marble.BLACK_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.black_general_cell)
                            Marble.METALLIC_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.metallic_general_cell)
                        }
                    }
                }
            }

            2 ->
            {
                when(columnIndex)
                {
                    0 ->
                    {
                        when(pentagoGameBoard.pentagoBoard[rowIndex][columnIndex]!!.marbleColour)
                        {
                            Marble.RED_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.red_lower_left_cell)
                            Marble.ORANGE_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.orange_lower_left_cell)
                            Marble.YELLOW_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.yellow_lower_left_cell)
                            Marble.GREEN_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.green_lower_left_cell)
                            Marble.BLUE_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.blue_lower_left_cell)
                            Marble.PURPLE_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.purple_lower_left_cell)
                            Marble.PINK_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.pink_lower_left_cell)
                            Marble.BLACK_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.black_lower_left_cell)
                            Marble.METALLIC_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.metallic_lower_left_cell)
                        }
                    }

                    1 ->
                    {
                        when(pentagoGameBoard.pentagoBoard[rowIndex][columnIndex]!!.marbleColour)
                        {
                            Marble.RED_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.red_general_cell)
                            Marble.ORANGE_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.orange_general_cell)
                            Marble.YELLOW_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.yellow_general_cell)
                            Marble.GREEN_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.green_general_cell)
                            Marble.BLUE_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.blue_general_cell)
                            Marble.PURPLE_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.purple_general_cell)
                            Marble.PINK_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.pink_general_cell)
                            Marble.BLACK_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.black_general_cell)
                            Marble.METALLIC_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.metallic_general_cell)
                        }
                    }

                    2 ->
                    {
                        when(pentagoGameBoard.pentagoBoard[rowIndex][columnIndex]!!.marbleColour)
                        {
                            Marble.RED_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.red_lower_right_cell)
                            Marble.ORANGE_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.orange_lower_right_cell)
                            Marble.YELLOW_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.yellow_lower_right_cell)
                            Marble.GREEN_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.green_lower_right_cell)
                            Marble.BLUE_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.blue_lower_right_cell)
                            Marble.PURPLE_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.purple_lower_right_cell)
                            Marble.PINK_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.pink_lower_right_cell)
                            Marble.BLACK_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.black_lower_right_cell)
                            Marble.METALLIC_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.metallic_lower_right_cell)
                        }
                    }

                    3 ->
                    {
                        when(pentagoGameBoard.pentagoBoard[rowIndex][columnIndex]!!.marbleColour)
                        {
                            Marble.RED_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.red_lower_left_cell)
                            Marble.ORANGE_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.orange_lower_left_cell)
                            Marble.YELLOW_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.yellow_lower_left_cell)
                            Marble.GREEN_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.green_lower_left_cell)
                            Marble.BLUE_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.blue_lower_left_cell)
                            Marble.PURPLE_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.purple_lower_left_cell)
                            Marble.PINK_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.pink_lower_left_cell)
                            Marble.BLACK_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.black_lower_left_cell)
                            Marble.METALLIC_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.metallic_lower_left_cell)
                        }
                    }

                    4 ->
                    {
                        when(pentagoGameBoard.pentagoBoard[rowIndex][columnIndex]!!.marbleColour)
                        {
                            Marble.RED_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.red_general_cell)
                            Marble.ORANGE_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.orange_general_cell)
                            Marble.YELLOW_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.yellow_general_cell)
                            Marble.GREEN_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.green_general_cell)
                            Marble.BLUE_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.blue_general_cell)
                            Marble.PURPLE_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.purple_general_cell)
                            Marble.PINK_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.pink_general_cell)
                            Marble.BLACK_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.black_general_cell)
                            Marble.METALLIC_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.metallic_general_cell)
                        }
                    }

                    5 ->
                    {
                        when(pentagoGameBoard.pentagoBoard[rowIndex][columnIndex]!!.marbleColour)
                        {
                            Marble.RED_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.red_lower_right_cell)
                            Marble.ORANGE_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.orange_lower_right_cell)
                            Marble.YELLOW_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.yellow_lower_right_cell)
                            Marble.GREEN_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.green_lower_right_cell)
                            Marble.BLUE_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.blue_lower_right_cell)
                            Marble.PURPLE_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.purple_lower_right_cell)
                            Marble.PINK_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.pink_lower_right_cell)
                            Marble.BLACK_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.black_lower_right_cell)
                            Marble.METALLIC_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.metallic_lower_right_cell)
                        }
                    }
                }
            }

            3 ->
            {
                when(columnIndex)
                {
                    0 ->
                    {
                        when(pentagoGameBoard.pentagoBoard[rowIndex][columnIndex]!!.marbleColour)
                        {
                            Marble.RED_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.red_upper_left_cell)
                            Marble.ORANGE_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.orange_upper_left_cell)
                            Marble.YELLOW_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.yellow_upper_left_cell)
                            Marble.GREEN_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.green_upper_left_cell)
                            Marble.BLUE_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.blue_upper_left_cell)
                            Marble.PURPLE_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.purple_upper_left_cell)
                            Marble.PINK_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.pink_upper_left_cell)
                            Marble.BLACK_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.black_upper_left_cell)
                            Marble.METALLIC_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.metallic_upper_left_cell)
                        }
                    }

                    1 ->
                    {
                        when(pentagoGameBoard.pentagoBoard[rowIndex][columnIndex]!!.marbleColour)
                        {
                            Marble.RED_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.red_general_cell)
                            Marble.ORANGE_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.orange_general_cell)
                            Marble.YELLOW_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.yellow_general_cell)
                            Marble.GREEN_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.green_general_cell)
                            Marble.BLUE_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.blue_general_cell)
                            Marble.PURPLE_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.purple_general_cell)
                            Marble.PINK_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.pink_general_cell)
                            Marble.BLACK_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.black_general_cell)
                            Marble.METALLIC_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.metallic_general_cell)
                        }
                    }

                    2 ->
                    {
                        when(pentagoGameBoard.pentagoBoard[rowIndex][columnIndex]!!.marbleColour)
                        {
                            Marble.RED_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.red_upper_right_cell)
                            Marble.ORANGE_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.orange_upper_right_cell)
                            Marble.YELLOW_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.yellow_upper_right_cell)
                            Marble.GREEN_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.green_upper_right_cell)
                            Marble.BLUE_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.blue_upper_right_cell)
                            Marble.PURPLE_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.purple_upper_right_cell)
                            Marble.PINK_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.pink_upper_right_cell)
                            Marble.BLACK_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.black_upper_right_cell)
                            Marble.METALLIC_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.metallic_upper_right_cell)
                        }
                    }

                    3 ->
                    {
                        when(pentagoGameBoard.pentagoBoard[rowIndex][columnIndex]!!.marbleColour)
                        {
                            Marble.RED_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.red_upper_left_cell)
                            Marble.ORANGE_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.orange_upper_left_cell)
                            Marble.YELLOW_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.yellow_upper_left_cell)
                            Marble.GREEN_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.green_upper_left_cell)
                            Marble.BLUE_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.blue_upper_left_cell)
                            Marble.PURPLE_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.purple_upper_left_cell)
                            Marble.PINK_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.pink_upper_left_cell)
                            Marble.BLACK_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.black_upper_left_cell)
                            Marble.METALLIC_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.metallic_upper_left_cell)
                        }
                    }

                    4 ->
                    {
                        when(pentagoGameBoard.pentagoBoard[rowIndex][columnIndex]!!.marbleColour)
                        {
                            Marble.RED_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.red_general_cell)
                            Marble.ORANGE_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.orange_general_cell)
                            Marble.YELLOW_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.yellow_general_cell)
                            Marble.GREEN_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.green_general_cell)
                            Marble.BLUE_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.blue_general_cell)
                            Marble.PURPLE_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.purple_general_cell)
                            Marble.PINK_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.pink_general_cell)
                            Marble.BLACK_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.black_general_cell)
                            Marble.METALLIC_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.metallic_general_cell)
                        }
                    }

                    5 ->
                    {
                        when(pentagoGameBoard.pentagoBoard[rowIndex][columnIndex]!!.marbleColour)
                        {
                            Marble.RED_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.red_upper_right_cell)
                            Marble.ORANGE_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.orange_upper_right_cell)
                            Marble.YELLOW_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.yellow_upper_right_cell)
                            Marble.GREEN_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.green_upper_right_cell)
                            Marble.BLUE_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.blue_upper_right_cell)
                            Marble.PURPLE_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.purple_upper_right_cell)
                            Marble.PINK_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.pink_upper_right_cell)
                            Marble.BLACK_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.black_upper_right_cell)
                            Marble.METALLIC_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.metallic_upper_right_cell)
                        }
                    }
                }
            }

            4 ->
            {
                when(columnIndex)
                {
                    0 ->
                    {
                        when(pentagoGameBoard.pentagoBoard[rowIndex][columnIndex]!!.marbleColour)
                        {
                            Marble.RED_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.red_general_cell)
                            Marble.ORANGE_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.orange_general_cell)
                            Marble.YELLOW_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.yellow_general_cell)
                            Marble.GREEN_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.green_general_cell)
                            Marble.BLUE_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.blue_general_cell)
                            Marble.PURPLE_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.purple_general_cell)
                            Marble.PINK_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.pink_general_cell)
                            Marble.BLACK_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.black_general_cell)
                            Marble.METALLIC_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.metallic_general_cell)
                        }
                    }

                    1 ->
                    {
                        when(pentagoGameBoard.pentagoBoard[rowIndex][columnIndex]!!.marbleColour)
                        {
                            Marble.RED_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.red_general_cell)
                            Marble.ORANGE_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.orange_general_cell)
                            Marble.YELLOW_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.yellow_general_cell)
                            Marble.GREEN_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.green_general_cell)
                            Marble.BLUE_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.blue_general_cell)
                            Marble.PURPLE_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.purple_general_cell)
                            Marble.PINK_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.pink_general_cell)
                            Marble.BLACK_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.black_general_cell)
                            Marble.METALLIC_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.metallic_general_cell)
                        }
                    }

                    2 ->
                    {
                        when(pentagoGameBoard.pentagoBoard[rowIndex][columnIndex]!!.marbleColour)
                        {
                            Marble.RED_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.red_general_cell)
                            Marble.ORANGE_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.orange_general_cell)
                            Marble.YELLOW_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.yellow_general_cell)
                            Marble.GREEN_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.green_general_cell)
                            Marble.BLUE_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.blue_general_cell)
                            Marble.PURPLE_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.purple_general_cell)
                            Marble.PINK_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.pink_general_cell)
                            Marble.BLACK_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.black_general_cell)
                            Marble.METALLIC_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.metallic_general_cell)
                        }
                    }

                    3 ->
                    {
                        when(pentagoGameBoard.pentagoBoard[rowIndex][columnIndex]!!.marbleColour)
                        {
                            Marble.RED_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.red_general_cell)
                            Marble.ORANGE_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.orange_general_cell)
                            Marble.YELLOW_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.yellow_general_cell)
                            Marble.GREEN_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.green_general_cell)
                            Marble.BLUE_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.blue_general_cell)
                            Marble.PURPLE_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.purple_general_cell)
                            Marble.PINK_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.pink_general_cell)
                            Marble.BLACK_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.black_general_cell)
                            Marble.METALLIC_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.metallic_general_cell)
                        }
                    }

                    4 ->
                    {
                        when(pentagoGameBoard.pentagoBoard[rowIndex][columnIndex]!!.marbleColour)
                        {
                            Marble.RED_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.red_general_cell)
                            Marble.ORANGE_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.orange_general_cell)
                            Marble.YELLOW_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.yellow_general_cell)
                            Marble.GREEN_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.green_general_cell)
                            Marble.BLUE_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.blue_general_cell)
                            Marble.PURPLE_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.purple_general_cell)
                            Marble.PINK_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.pink_general_cell)
                            Marble.BLACK_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.black_general_cell)
                            Marble.METALLIC_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.metallic_general_cell)
                        }
                    }

                    5 ->
                    {
                        when(pentagoGameBoard.pentagoBoard[rowIndex][columnIndex]!!.marbleColour)
                        {
                            Marble.RED_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.red_general_cell)
                            Marble.ORANGE_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.orange_general_cell)
                            Marble.YELLOW_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.yellow_general_cell)
                            Marble.GREEN_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.green_general_cell)
                            Marble.BLUE_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.blue_general_cell)
                            Marble.PURPLE_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.purple_general_cell)
                            Marble.PINK_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.pink_general_cell)
                            Marble.BLACK_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.black_general_cell)
                            Marble.METALLIC_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.metallic_general_cell)
                        }
                    }
                }
            }

            5 ->
            {
                when(columnIndex)
                {
                    0 ->
                    {
                        when(pentagoGameBoard.pentagoBoard[rowIndex][columnIndex]!!.marbleColour)
                        {
                            Marble.RED_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.red_lower_left_cell)
                            Marble.ORANGE_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.orange_lower_left_cell)
                            Marble.YELLOW_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.yellow_lower_left_cell)
                            Marble.GREEN_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.green_lower_left_cell)
                            Marble.BLUE_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.blue_lower_left_cell)
                            Marble.PURPLE_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.purple_lower_left_cell)
                            Marble.PINK_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.pink_lower_left_cell)
                            Marble.BLACK_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.black_lower_left_cell)
                            Marble.METALLIC_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.metallic_lower_left_cell)
                        }
                    }

                    1 ->
                    {
                        when(pentagoGameBoard.pentagoBoard[rowIndex][columnIndex]!!.marbleColour)
                        {
                            Marble.RED_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.red_general_cell)
                            Marble.ORANGE_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.orange_general_cell)
                            Marble.YELLOW_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.yellow_general_cell)
                            Marble.GREEN_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.green_general_cell)
                            Marble.BLUE_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.blue_general_cell)
                            Marble.PURPLE_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.purple_general_cell)
                            Marble.PINK_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.pink_general_cell)
                            Marble.BLACK_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.black_general_cell)
                            Marble.METALLIC_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.metallic_general_cell)
                        }
                    }

                    2 ->
                    {
                        when(pentagoGameBoard.pentagoBoard[rowIndex][columnIndex]!!.marbleColour)
                        {
                            Marble.RED_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.red_lower_right_cell)
                            Marble.ORANGE_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.orange_lower_right_cell)
                            Marble.YELLOW_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.yellow_lower_right_cell)
                            Marble.GREEN_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.green_lower_right_cell)
                            Marble.BLUE_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.blue_lower_right_cell)
                            Marble.PURPLE_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.purple_lower_right_cell)
                            Marble.PINK_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.pink_lower_right_cell)
                            Marble.BLACK_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.black_lower_right_cell)
                            Marble.METALLIC_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.metallic_lower_right_cell)
                        }
                    }

                    3 ->
                    {
                        when(pentagoGameBoard.pentagoBoard[rowIndex][columnIndex]!!.marbleColour)
                        {
                            Marble.RED_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.red_lower_left_cell)
                            Marble.ORANGE_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.orange_lower_left_cell)
                            Marble.YELLOW_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.yellow_lower_left_cell)
                            Marble.GREEN_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.green_lower_left_cell)
                            Marble.BLUE_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.blue_lower_left_cell)
                            Marble.PURPLE_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.purple_lower_left_cell)
                            Marble.PINK_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.pink_lower_left_cell)
                            Marble.BLACK_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.black_lower_left_cell)
                            Marble.METALLIC_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.metallic_lower_left_cell)
                        }
                    }

                    4 ->
                    {
                        when(pentagoGameBoard.pentagoBoard[rowIndex][columnIndex]!!.marbleColour)
                        {
                            Marble.RED_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.red_general_cell)
                            Marble.ORANGE_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.orange_general_cell)
                            Marble.YELLOW_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.yellow_general_cell)
                            Marble.GREEN_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.green_general_cell)
                            Marble.BLUE_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.blue_general_cell)
                            Marble.PURPLE_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.purple_general_cell)
                            Marble.PINK_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.pink_general_cell)
                            Marble.BLACK_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.black_general_cell)
                            Marble.METALLIC_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.metallic_general_cell)
                        }
                    }

                    5 ->
                    {
                        when(pentagoGameBoard.pentagoBoard[rowIndex][columnIndex]!!.marbleColour)
                        {
                            Marble.RED_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.red_lower_right_cell)
                            Marble.ORANGE_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.orange_lower_right_cell)
                            Marble.YELLOW_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.yellow_lower_right_cell)
                            Marble.GREEN_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.green_lower_right_cell)
                            Marble.BLUE_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.blue_lower_right_cell)
                            Marble.PURPLE_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.purple_lower_right_cell)
                            Marble.PINK_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.pink_lower_right_cell)
                            Marble.BLACK_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.black_lower_right_cell)
                            Marble.METALLIC_MARBLE -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.metallic_lower_right_cell)
                        }
                    }
                }
            }
        }
    }

    private fun updateEmptyCell(rowIndex: Int, columnIndex: Int)
    {
        when(rowIndex)
        {
            0 ->
            {
                when(columnIndex)
                {
                    0 -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.empty_upper_left_cell)
                    1 -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.empty_general_cell)
                    2 -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.empty_upper_right_cell)
                    3 -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.empty_upper_left_cell)
                    4 -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.empty_general_cell)
                    5 -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.empty_upper_right_cell)
                }
            }

            1 ->
            {
                when(columnIndex)
                {
                    0 -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.empty_general_cell)
                    1 -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.empty_general_cell)
                    2 -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.empty_general_cell)
                    3 -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.empty_general_cell)
                    4 -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.empty_general_cell)
                    5 -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.empty_general_cell)
                }
            }

            2 ->
            {
                when(columnIndex)
                {
                    0 -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.empty_lower_left_cell)
                    1 -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.empty_general_cell)
                    2 -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.empty_lower_right_cell)
                    3 -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.empty_lower_left_cell)
                    4 -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.empty_general_cell)
                    5 -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.empty_lower_right_cell)
                }
            }

            3 ->
            {
                when(columnIndex)
                {
                    0 -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.empty_upper_left_cell)
                    1 -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.empty_general_cell)
                    2 -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.empty_upper_right_cell)
                    3 -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.empty_upper_left_cell)
                    4 -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.empty_general_cell)
                    5 -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.empty_upper_right_cell)
                }
            }

            4 ->
            {
                when(columnIndex)
                {
                    0 -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.empty_general_cell)
                    1 -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.empty_general_cell)
                    2 -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.empty_general_cell)
                    3 -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.empty_general_cell)
                    4 -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.empty_general_cell)
                    5 -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.empty_general_cell)
                }
            }

            5 ->
            {
                when(columnIndex)
                {
                    0 -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.empty_lower_left_cell)
                    1 -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.empty_general_cell)
                    2 -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.empty_lower_right_cell)
                    3 -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.empty_lower_left_cell)
                    4 -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.empty_general_cell)
                    5 -> uiPentagoBoard[rowIndex][columnIndex]!!.setImageResource(R.drawable.empty_lower_right_cell)
                }
            }
        }
    }

    private fun rotateSubgridAnimation(subgrid: GridLayout, rotationString: String)
    {
        var subgridAnimator: ObjectAnimator? = null

        if(rotationString == PentagoBoard.CLOCKWISE_ROTATION)
        {
            subgridAnimator = ObjectAnimator.ofFloat(subgrid, "rotation", 0f, 90f)
            subgridAnimator.setDuration(ROTATION_ANIMATION_DURATION)
        }
        else
        {
            if(rotationString == PentagoBoard.ANTI_CLOCKWISE_ROTATION)
            {
                subgridAnimator = ObjectAnimator.ofFloat(subgrid, "rotation", 0f, -90f)
                subgridAnimator.setDuration(ROTATION_ANIMATION_DURATION)
            }
        }
        subgridAnimator!!.start()
    }

    //Pass the rotationString yo want to undo. E.g if you want to undo a clockwise rotation pass the clockwise string
    private fun instantUndoRotationAnimation(subgrid: GridLayout, rotationString: String)
    {
        var subgridAnimator: ObjectAnimator? = null

        if(rotationString == PentagoBoard.CLOCKWISE_ROTATION)
        {
            subgridAnimator = ObjectAnimator.ofFloat(subgrid, "rotation", 90f, 0f)
            subgridAnimator.setDuration(0)
        }
        else
        {
            if(rotationString == PentagoBoard.ANTI_CLOCKWISE_ROTATION)
            {
                subgridAnimator = ObjectAnimator.ofFloat(subgrid, "rotation", -90f, 0f)
                subgridAnimator.setDuration(0)
            }
        }
        subgridAnimator!!.start()
    }

    private fun resizeSubGridAnimation(subgrid: GridLayout, resizeString: String)
    {
        var horizontalSubgridAnimator: ObjectAnimator? = null
        var verticalSubgridAnimator: ObjectAnimator? = null
        var animatorCombination = AnimatorSet()

        if(resizeString == SHRINK_ANIMATION)
        {
            horizontalSubgridAnimator = ObjectAnimator.ofFloat(subgrid, "scaleX", 1f, 0.6f)
            verticalSubgridAnimator = ObjectAnimator.ofFloat(subgrid, "scaleY", 1f, 0.6f)
            horizontalSubgridAnimator.setDuration(SHRINK_ANIMATION_DURATION)
            verticalSubgridAnimator.setDuration(SHRINK_ANIMATION_DURATION)
        }
        else
        {
            if(resizeString == GROW_ANIMATION)
            {
                horizontalSubgridAnimator = ObjectAnimator.ofFloat(subgrid, "scaleX", 0.6f, 1f)
                verticalSubgridAnimator = ObjectAnimator.ofFloat(subgrid, "scaleY", 0.6f, 1f)
                horizontalSubgridAnimator.setDuration(GROW_ANIMATION_DURATION)
                verticalSubgridAnimator.setDuration(GROW_ANIMATION_DURATION)
            }
        }

        animatorCombination.playTogether(horizontalSubgridAnimator, verticalSubgridAnimator)
        animatorCombination.start()
    }

    private fun aiRotationAnimation(subgridRotationArray: Array<Int?>)
    {
        var didDrawOccur = false
        var winner: PlayerProfile?

        if(subgridRotationArray[1] == 0)
        {
            if(subgridRotationArray[0] == 0)
            {
                resizeSubGridAnimation(binding.pentagoUpperLeftSubgrid, SHRINK_ANIMATION)
                binding.pentagoUpperLeftSubgrid.postDelayed(SHRINK_ANIMATION_DURATION)
                {
                    rotateSubgridAnimation(binding.pentagoUpperLeftSubgrid, PentagoBoard.CLOCKWISE_ROTATION)
                    binding.pentagoUpperLeftSubgrid.postDelayed(ROTATION_ANIMATION_DURATION)
                    {
                        instantUndoRotationAnimation(binding.pentagoUpperLeftSubgrid, PentagoBoard.CLOCKWISE_ROTATION)
                        updateUpperLeftSubGrid()
                        resizeSubGridAnimation(binding.pentagoUpperLeftSubgrid, GROW_ANIMATION)
                        binding.gameStateHelpTextview.text = getString(R.string.game_state_help_place_marble)
                        binding.playerTurnTextview.text = pentagoGameBoard.currentTurnPlayerProfile.userName

                        didDrawOccur = pentagoGameBoard.didDrawHappen()

                        if(didDrawOccur)
                        {
                            onDraw()
                        }
                        else
                        {
                            winner = pentagoGameBoard.checkWinConditionPostRotation()
                            if(winner != null)
                            {
                                onSinglePlayerWin(winner!!)
                            }
                            else
                            {
                                activateGridCellClickListeners()
                            }
                        }
                    }
                }
            }
            else
            {
                if(subgridRotationArray[0] == 1)
                {
                    resizeSubGridAnimation(binding.pentagoUpperRightSubgrid, SHRINK_ANIMATION)
                    binding.pentagoUpperRightSubgrid.postDelayed(SHRINK_ANIMATION_DURATION)
                    {
                        rotateSubgridAnimation(binding.pentagoUpperRightSubgrid, PentagoBoard.CLOCKWISE_ROTATION)
                        binding.pentagoUpperRightSubgrid.postDelayed(ROTATION_ANIMATION_DURATION)
                        {
                            instantUndoRotationAnimation(binding.pentagoUpperRightSubgrid, PentagoBoard.CLOCKWISE_ROTATION)
                            updateUpperRightSubGrid()
                            resizeSubGridAnimation(binding.pentagoUpperRightSubgrid, GROW_ANIMATION)
                            binding.gameStateHelpTextview.text = getString(R.string.game_state_help_place_marble)
                            binding.playerTurnTextview.text = pentagoGameBoard.currentTurnPlayerProfile.userName

                            didDrawOccur = pentagoGameBoard.didDrawHappen()

                            if(didDrawOccur)
                            {
                                onDraw()
                            }
                            else
                            {
                                winner = pentagoGameBoard.checkWinConditionPostRotation()
                                if(winner != null)
                                {
                                    onSinglePlayerWin(winner!!)
                                }
                                else
                                {
                                    activateGridCellClickListeners()
                                }
                            }
                        }
                    }
                }
                else
                {
                    if(subgridRotationArray[0] == 2)
                    {
                        resizeSubGridAnimation(binding.pentagoLowerLeftSubgrid, SHRINK_ANIMATION)
                        binding.pentagoLowerLeftSubgrid.postDelayed(SHRINK_ANIMATION_DURATION)
                        {
                            rotateSubgridAnimation(binding.pentagoLowerLeftSubgrid, PentagoBoard.CLOCKWISE_ROTATION)
                            binding.pentagoLowerLeftSubgrid.postDelayed(ROTATION_ANIMATION_DURATION)
                            {
                                instantUndoRotationAnimation(binding.pentagoLowerLeftSubgrid, PentagoBoard.CLOCKWISE_ROTATION)
                                updateLowerLeftSubGrid()
                                resizeSubGridAnimation(binding.pentagoLowerLeftSubgrid, GROW_ANIMATION)
                                binding.gameStateHelpTextview.text = getString(R.string.game_state_help_place_marble)
                                binding.playerTurnTextview.text = pentagoGameBoard.currentTurnPlayerProfile.userName

                                didDrawOccur = pentagoGameBoard.didDrawHappen()

                                if(didDrawOccur)
                                {
                                    onDraw()
                                }
                                else
                                {
                                    winner = pentagoGameBoard.checkWinConditionPostRotation()
                                    if(winner != null)
                                    {
                                        onSinglePlayerWin(winner!!)
                                    }
                                    else
                                    {
                                        activateGridCellClickListeners()
                                    }
                                }
                            }
                        }
                    }
                    else
                    {
                        if(subgridRotationArray[0] == 3)
                        {
                            resizeSubGridAnimation(binding.pentagoLowerRightSubgrid, SHRINK_ANIMATION)
                            binding.pentagoLowerRightSubgrid.postDelayed(SHRINK_ANIMATION_DURATION)
                            {
                                rotateSubgridAnimation(binding.pentagoLowerRightSubgrid, PentagoBoard.CLOCKWISE_ROTATION)
                                binding.pentagoLowerRightSubgrid.postDelayed(ROTATION_ANIMATION_DURATION)
                                {
                                    instantUndoRotationAnimation(binding.pentagoLowerRightSubgrid, PentagoBoard.CLOCKWISE_ROTATION)
                                    updateLowerRightSubGrid()
                                    resizeSubGridAnimation(binding.pentagoLowerRightSubgrid, GROW_ANIMATION)
                                    binding.gameStateHelpTextview.text = getString(R.string.game_state_help_place_marble)
                                    binding.playerTurnTextview.text = pentagoGameBoard.currentTurnPlayerProfile.userName

                                    didDrawOccur = pentagoGameBoard.didDrawHappen()

                                    if(didDrawOccur)
                                    {
                                        onDraw()
                                    }
                                    else
                                    {
                                        winner = pentagoGameBoard.checkWinConditionPostRotation()
                                        if(winner != null)
                                        {
                                            onSinglePlayerWin(winner!!)
                                        }
                                        else
                                        {
                                            activateGridCellClickListeners()
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        else
        {
            if(subgridRotationArray[1] == 1)
            {
                if(subgridRotationArray[0] == 0)
                {
                    resizeSubGridAnimation(binding.pentagoUpperLeftSubgrid, SHRINK_ANIMATION)
                    binding.pentagoUpperLeftSubgrid.postDelayed(SHRINK_ANIMATION_DURATION)
                    {
                        rotateSubgridAnimation(binding.pentagoUpperLeftSubgrid, PentagoBoard.ANTI_CLOCKWISE_ROTATION)
                        binding.pentagoUpperLeftSubgrid.postDelayed(ROTATION_ANIMATION_DURATION)
                        {
                            instantUndoRotationAnimation(binding.pentagoUpperLeftSubgrid, PentagoBoard.ANTI_CLOCKWISE_ROTATION)
                            updateUpperLeftSubGrid()
                            resizeSubGridAnimation(binding.pentagoUpperLeftSubgrid, GROW_ANIMATION)
                            binding.gameStateHelpTextview.text = getString(R.string.game_state_help_place_marble)
                            binding.playerTurnTextview.text = pentagoGameBoard.currentTurnPlayerProfile.userName

                            didDrawOccur = pentagoGameBoard.didDrawHappen()

                            if(didDrawOccur)
                            {
                                onDraw()
                            }
                            else
                            {
                                winner = pentagoGameBoard.checkWinConditionPostRotation()
                                if(winner != null)
                                {
                                    onSinglePlayerWin(winner!!)
                                }
                                else
                                {
                                    activateGridCellClickListeners()
                                }
                            }
                        }
                    }
                }
                else
                {
                    if(subgridRotationArray[0] == 1)
                    {
                        resizeSubGridAnimation(binding.pentagoUpperRightSubgrid, SHRINK_ANIMATION)
                        binding.pentagoUpperRightSubgrid.postDelayed(SHRINK_ANIMATION_DURATION)
                        {
                            rotateSubgridAnimation(binding.pentagoUpperRightSubgrid, PentagoBoard.ANTI_CLOCKWISE_ROTATION)
                            binding.pentagoUpperRightSubgrid.postDelayed(ROTATION_ANIMATION_DURATION)
                            {
                                instantUndoRotationAnimation(binding.pentagoUpperRightSubgrid, PentagoBoard.ANTI_CLOCKWISE_ROTATION)
                                updateUpperRightSubGrid()
                                resizeSubGridAnimation(binding.pentagoUpperRightSubgrid, GROW_ANIMATION)
                                binding.gameStateHelpTextview.text = getString(R.string.game_state_help_place_marble)
                                binding.playerTurnTextview.text = pentagoGameBoard.currentTurnPlayerProfile.userName

                                didDrawOccur = pentagoGameBoard.didDrawHappen()

                                if(didDrawOccur)
                                {
                                    onDraw()
                                }
                                else
                                {
                                    winner = pentagoGameBoard.checkWinConditionPostRotation()
                                    if(winner != null)
                                    {
                                        onSinglePlayerWin(winner!!)
                                    }
                                    else
                                    {
                                        activateGridCellClickListeners()
                                    }
                                }
                            }
                        }
                    }
                    else
                    {
                        if(subgridRotationArray[0] == 2)
                        {
                            resizeSubGridAnimation(binding.pentagoLowerLeftSubgrid, SHRINK_ANIMATION)
                            binding.pentagoLowerLeftSubgrid.postDelayed(SHRINK_ANIMATION_DURATION)
                            {
                                rotateSubgridAnimation(binding.pentagoLowerLeftSubgrid, PentagoBoard.ANTI_CLOCKWISE_ROTATION)
                                binding.pentagoLowerLeftSubgrid.postDelayed(ROTATION_ANIMATION_DURATION)
                                {
                                    instantUndoRotationAnimation(binding.pentagoLowerLeftSubgrid, PentagoBoard.ANTI_CLOCKWISE_ROTATION)
                                    updateLowerLeftSubGrid()
                                    resizeSubGridAnimation(binding.pentagoLowerLeftSubgrid, GROW_ANIMATION)
                                    binding.gameStateHelpTextview.text = getString(R.string.game_state_help_place_marble)
                                    binding.playerTurnTextview.text = pentagoGameBoard.currentTurnPlayerProfile.userName

                                    didDrawOccur = pentagoGameBoard.didDrawHappen()

                                    if(didDrawOccur)
                                    {
                                        onDraw()
                                    }
                                    else
                                    {
                                        winner = pentagoGameBoard.checkWinConditionPostRotation()
                                        if(winner != null)
                                        {
                                            onSinglePlayerWin(winner!!)
                                        }
                                        else
                                        {
                                            activateGridCellClickListeners()
                                        }
                                    }
                                }
                            }
                        }
                        else
                        {
                            if(subgridRotationArray[0] == 3)
                            {
                                resizeSubGridAnimation(binding.pentagoLowerRightSubgrid, SHRINK_ANIMATION)
                                binding.pentagoLowerRightSubgrid.postDelayed(SHRINK_ANIMATION_DURATION)
                                {
                                    rotateSubgridAnimation(binding.pentagoLowerRightSubgrid, PentagoBoard.ANTI_CLOCKWISE_ROTATION)
                                    binding.pentagoLowerRightSubgrid.postDelayed(ROTATION_ANIMATION_DURATION)
                                    {
                                        instantUndoRotationAnimation(binding.pentagoLowerRightSubgrid, PentagoBoard.ANTI_CLOCKWISE_ROTATION)
                                        updateLowerRightSubGrid()
                                        resizeSubGridAnimation(binding.pentagoLowerRightSubgrid, GROW_ANIMATION)
                                        binding.gameStateHelpTextview.text = getString(R.string.game_state_help_place_marble)
                                        binding.playerTurnTextview.text = pentagoGameBoard.currentTurnPlayerProfile.userName

                                        didDrawOccur = pentagoGameBoard.didDrawHappen()

                                        if(didDrawOccur)
                                        {
                                            onDraw()
                                        }
                                        else
                                        {
                                            winner = pentagoGameBoard.checkWinConditionPostRotation()
                                            if(winner != null)
                                            {
                                                onSinglePlayerWin(winner!!)
                                            }
                                            else
                                            {
                                                activateGridCellClickListeners()
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    companion object
    {
        const val ROTATION_ANIMATION_DURATION: Long = 1500
        const val SHRINK_ANIMATION = "Shrink Subgrid"
        const val GROW_ANIMATION = "Grow Subgrid"
        const val SHRINK_ANIMATION_DURATION: Long = 500
        const val GROW_ANIMATION_DURATION: Long = 500
        const val AI_PLACE_MARBLE_WAIT_DURATION: Long = 750
        const val AI_ROTATION_WAIT_DURATION: Long = 1000
    }
}