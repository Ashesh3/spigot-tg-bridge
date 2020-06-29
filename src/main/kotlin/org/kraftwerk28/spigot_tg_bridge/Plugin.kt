package org.kraftwerk28.spigot_tg_bridge

import com.vdurmont.emoji.EmojiParser
import org.bukkit.Location
import org.bukkit.Statistic
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.player.AsyncPlayerChatEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.plugin.java.JavaPlugin
import org.telegram.telegrambots.ApiContextInitializer
import org.telegram.telegrambots.meta.TelegramBotsApi
import java.io.File
import java.util.*
import java.util.concurrent.TimeUnit


class Plugin : JavaPlugin(), Listener {

    private var tgBot: Bot? = null
    private var chatToTG: Boolean = false

    override fun onEnable() {
        val configFile = File(
            server.pluginManager.getPlugin(name)!!.dataFolder,
            "config.yml"
        )
        if (!configFile.exists()) {
            logger.warning("No config file found! Saving default one.")
            saveDefaultConfig()
            return
        }

        ApiContextInitializer.init()
        val botsApi = TelegramBotsApi()
        tgBot = Bot(this)
        chatToTG = config.getBoolean("logFromMCtoTG", false)

        botsApi.registerBot(tgBot)

        server.pluginManager.registerEvents(this, this)

        // Notify everything about server start
        val startMsg = config.getString("serverStartMessage", null)
        if (startMsg != null) tgBot?.broadcastToTG(startMsg)
        logger.info("Plugin started")
    }

    override fun onDisable() {
        val stopMsg = config.getString("serverStopMessage", null)
        if (stopMsg != null) tgBot?.broadcastToTG(stopMsg)
        logger.info("Plugin stopped")
    }

    fun sendMessageToMC(text: String) {
        val prep = EmojiParser.parseToAliases(text)
        server.broadcastMessage(prep)
    }

    private fun formatTime(mill: Long): String? {
        var millis = mill
        val days = TimeUnit.MILLISECONDS.toDays(millis)
        millis -= TimeUnit.DAYS.toMillis(days)
        val hours = TimeUnit.MILLISECONDS.toHours(millis)
        millis -= TimeUnit.HOURS.toMillis(hours)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(millis)
        millis -= TimeUnit.MINUTES.toMillis(minutes)
        val seconds = TimeUnit.MILLISECONDS.toSeconds(millis)
        return "$days days $hours hours $minutes mintues $seconds seconds"
    }

    private fun formatLocation(loc: Location): String? {
        return "{" + "world=" + (loc.world
            ?.name ?: "") + ",x=" + loc.x + ",y=" + loc.y + ",z=" + loc.z + "}"
    }

    fun sendMessageToMCFrom(username: String, text: String) {
        val prep = EmojiParser.parseToAliases("<$username> $text")
        server.broadcastMessage(prep)
    }

    @EventHandler
    fun onPlayerChat(event: AsyncPlayerChatEvent) {
        if (chatToTG)
            tgBot?.sendMessageToTGFrom(event.player.displayName, event.message)
    }

    @EventHandler
    fun onPlayerDeath(event: PlayerDeathEvent){
        if (config.getBoolean("logDeath", false)) {
            tgBot?.broadcastToTG("<b>${event.deathMessage}</b>")
        }
    }

    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        if (config.getBoolean("logJoinLeave", false)) {
            val joinStr = config.getString("strings.joined", "joined")
            tgBot?.broadcastToTG("<b>${event.player.displayName} $joinStr.</b>\n<b>IP:</b> <code>${event.player.address.toString().replace("/", "")}</code>\n<b>UUID:</b> <code>${Date().toString()}</code>")
        }
    }

    @EventHandler
    fun onPlayerLeave(event: PlayerQuitEvent) {
        if (config.getBoolean("logJoinLeave", false)) {
            val leftStr = config.getString("strings.left", "joined")
            tgBot?.broadcastToTG("<b>${event.player.displayName} $leftStr</b><b>Total Time Played: </b><code>${formatTime(((event.player.getStatistic( Statistic.PLAY_ONE_MINUTE ) * 0.05 * 1000).toLong()))}</code>\n<b>Last Location: </b><code>${formatLocation(event.player.location)}</code>\n<b>UUID: </b><code>${event.player.uniqueId}/code>")
        }
    }
}
