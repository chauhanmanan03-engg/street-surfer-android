package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.AudioManager
import com.example.data.StreetSurferDatabase
import com.example.engine.StreetSurferRenderer
import com.example.game.GameManager
import com.example.game.HudSnapshot
import com.example.game.InputAction
import com.example.progression.PowerUpKind
import com.example.progression.ProgressionManager
import com.example.progression.ProgressionSnapshot
import com.example.progression.RunStatsPayload
import com.example.progression.StreetCatalog
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class ShopTab(val title: String) {
    UPGRADES("Upgrades"),
    GEAR("Gear"),
    CHARACTERS("Runners"),
    BOARDS("Boards")
}

data class StreetSurferUiState(
    val hud: HudSnapshot = HudSnapshot(),
    val progression: ProgressionSnapshot? = null,
    val isMissionsOpen: Boolean = false,
    val isShopOpen: Boolean = false,
    val selectedShopTab: ShopTab = ShopTab.UPGRADES
)

class GameViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = StreetSurferDatabase.getInstance(application).streetSurferDao()
    private val progressionManager = ProgressionManager(dao)

    val audioManager = AudioManager()
    val gameManager = GameManager(audioManager)
    val renderer = StreetSurferRenderer(gameManager)

    private val hudFlow = MutableStateFlow(HudSnapshot())
    private val isMissionsOpenFlow = MutableStateFlow(false)
    private val isShopOpenFlow = MutableStateFlow(false)
    private val selectedShopTabFlow = MutableStateFlow(ShopTab.UPGRADES)

    val uiState: StateFlow<StreetSurferUiState> = combine(
        hudFlow,
        progressionManager.progressionFlow,
        isMissionsOpenFlow,
        isShopOpenFlow,
        selectedShopTabFlow
    ) { hud, prog, missionsOpen, shopOpen, tab ->
        StreetSurferUiState(
            hud = hud,
            progression = prog,
            isMissionsOpen = missionsOpen,
            isShopOpen = shopOpen,
            selectedShopTab = tab
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = StreetSurferUiState()
    )

    init {
        viewModelScope.launch {
            progressionManager.ensureInitialized()
        }

        viewModelScope.launch {
            progressionManager.progressionFlow.collect { prog ->
                audioManager.setEnabled(prog.save.soundEnabled)
                renderer.equippedCharacter = StreetCatalog.findCharacter(prog.save.equippedCharacterId)
                renderer.equippedBoard = StreetCatalog.findBoard(prog.save.equippedBoardId)
                gameManager.syncProgressionState(
                    highScore = prog.save.highScore,
                    permMultiplier = prog.save.permanentMultiplier,
                    hoverboards = prog.save.hoverboardsOwned,
                    headstarts = prog.save.headstartsOwned,
                    keys = prog.save.keys,
                    upgradeLevels = prog.upgradeLevels
                )
            }
        }

        gameManager.onHudSnapshot = { snapshot ->
            hudFlow.value = snapshot
        }

        gameManager.onConsumeHoverboard = {
            viewModelScope.launch { progressionManager.consumeHoverboard() }
            true
        }

        gameManager.onConsumeHeadstart = {
            viewModelScope.launch { progressionManager.consumeHeadstart() }
            true
        }

        gameManager.onCommitRunResult = { report ->
            viewModelScope.launch {
                val leveledUp = progressionManager.recordRunOutcome(
                    stats = RunStatsPayload(
                        runScore = report.score,
                        runCoins = report.coins,
                        jumps = report.jumps,
                        rolls = report.rolls,
                        powerUpsUsed = report.powerUpsUsed,
                        hoverboardsUsed = report.hoverboardsUsed,
                        rooftopMeters = report.rooftopMeters,
                        laneSwitches = report.laneSwitches
                    ),
                    keysCollectedInRun = report.keysCollected
                )
                if (leveledUp) {
                    gameManager.markMissionTierLeveledUp()
                }
            }
        }
    }

    fun startRun() {
        isMissionsOpenFlow.value = false
        isShopOpenFlow.value = false
        gameManager.startNewRun()
    }

    fun pauseRun() = gameManager.pauseRun()
    fun resumeRun() = gameManager.resumeRun()
    fun returnToMenu() = gameManager.returnToMenu()
    fun sendInput(action: InputAction) = gameManager.enqueueInput(action)
    fun triggerHeadstart() = gameManager.triggerHeadstartFromHud()
    fun triggerHoverboard() = gameManager.enqueueInput(InputAction.DoubleTap)

    fun acceptRevive() {
        val cost = hudFlow.value.reviveCostKeys
        viewModelScope.launch {
            if (progressionManager.consumeReviveKeys(cost)) {
                gameManager.acceptRevive()
            } else {
                gameManager.declineRevive()
            }
        }
    }

    fun declineRevive() = gameManager.declineRevive()

    fun toggleSound() {
        val currentlyEnabled = uiState.value.progression?.save?.soundEnabled ?: true
        val next = !currentlyEnabled
        audioManager.setEnabled(next)
        if (next && hudFlow.value.phase == com.example.game.GamePhase.PLAYING) {
            audioManager.startMusic()
        }
        viewModelScope.launch {
            progressionManager.setSoundEnabled(next)
        }
    }

    fun openMissions(open: Boolean) {
        isMissionsOpenFlow.value = open
    }

    fun openShop(open: Boolean) {
        isShopOpenFlow.value = open
    }

    fun selectShopTab(tab: ShopTab) {
        selectedShopTabFlow.value = tab
    }

    fun buyUpgrade(kind: PowerUpKind) {
        viewModelScope.launch {
            if (progressionManager.buyUpgrade(kind)) {
                audioManager.playPowerUp()
            }
        }
    }

    fun buyHoverboard() {
        viewModelScope.launch {
            if (progressionManager.buyHoverboard()) {
                audioManager.playCoin(4)
            }
        }
    }

    fun buyHeadstart() {
        viewModelScope.launch {
            if (progressionManager.buyHeadstart()) {
                audioManager.playCoin(5)
            }
        }
    }

    fun buyKey() {
        viewModelScope.launch {
            if (progressionManager.buyKey()) {
                audioManager.playCoin(7)
            }
        }
    }

    fun selectCharacter(id: String) {
        viewModelScope.launch {
            if (progressionManager.unlockOrEquipCharacter(id)) {
                audioManager.playPowerUp()
            }
        }
    }

    fun selectBoard(id: String) {
        viewModelScope.launch {
            if (progressionManager.unlockOrEquipBoard(id)) {
                audioManager.playHoverboardActivate()
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        audioManager.release()
    }
}
