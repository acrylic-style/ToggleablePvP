package xyz.acrylicstyle.pvper

import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.entity.Projectile
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.entity.ProjectileHitEvent
import org.bukkit.plugin.java.JavaPlugin
import java.util.Objects
import java.util.UUID

class ToggleablePvPPlugin : JavaPlugin(), Listener {
    companion object {
        val pvpAllowed = ArrayList<UUID>()
    }

    override fun onEnable() {
        Bukkit.getPluginCommand("pvp")!!.setExecutor(object : CommandExecutor {
            override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
                if (sender !is Player) {
                    sender.sendMessage(ChatColor.RED.toString() + "This command cannot be invoked from console.")
                    return true
                }
                if (args.isNotEmpty() && sender.isOp) {
                    val p = Bukkit.getPlayer(args[0])
                    if (p == null) {
                        sender.sendMessage(ChatColor.RED.toString() + "プレイヤーが見つかりません。")
                        return true
                    }
                    if (pvpAllowed.contains(p.uniqueId)) {
                        pvpAllowed.remove(p.uniqueId)
                        sender.sendMessage(ChatColor.GREEN.toString() + "PvPをオフにしました。")
                        p.sendMessage(ChatColor.YELLOW.toString() + sender.name + "によってPvPがオフにされました。")
                    } else {
                        pvpAllowed.add(p.uniqueId)
                        sender.sendMessage(ChatColor.GREEN.toString() + "PvPをオンにしました。")
                        p.sendMessage(ChatColor.YELLOW.toString() + sender.name + "によってPvPがオンにされました。")
                    }
                    return true
                }
                if (pvpAllowed.contains(sender.uniqueId)) {
                    pvpAllowed.remove(sender.uniqueId)
                    sender.sendMessage(ChatColor.GREEN.toString() + "PvPをオフにしました。")
                } else {
                    pvpAllowed.add(sender.uniqueId)
                    sender.sendMessage(ChatColor.GREEN.toString() + "PvPをオンにしました。")
                }
                return true
            }
        })
        Bukkit.getPluginManager().registerEvents(this, this)
    }

    @EventHandler
    fun onBlockBreak(e: BlockBreakEvent) {
        if (pvpAllowed.contains(e.player.uniqueId)) {
            e.player.sendMessage(ChatColor.RED.toString() + "PvPモードがオンになっているためブロックの破壊はできません。")
            e.isCancelled = true
        }
    }

    @EventHandler
    fun onBlockPlace(e: BlockPlaceEvent) {
        if (pvpAllowed.contains(e.player.uniqueId)) {
            e.player.sendMessage(ChatColor.RED.toString() + "PvPモードがオンになっているためブロックの設置はできません。")
            e.isCancelled = true
        }
    }

    @EventHandler
    fun onPlayerDamageByEntity(e: EntityDamageByEntityEvent) {
        val damager = if (e.damager is Projectile && (e.damager as Projectile).shooter is Player) (e.damager as Projectile).shooter as Player else e.damager
        if (e.entity !is Player || damager !is Player) return
        if (!pvpAllowed.contains(e.entity.uniqueId) || !pvpAllowed.contains(damager.uniqueId)) {
            e.isCancelled = true
            return
        }
    }

    @EventHandler
    fun onProjectileHit(e: ProjectileHitEvent) {
        if (e.entity.shooter is Player && e.hitEntity is Player) {
            if (!pvpAllowed.contains((e.entity.shooter as Player).uniqueId) || !pvpAllowed.contains((e.hitEntity as Player).uniqueId)) {
                e.entity.remove()
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    @EventHandler
    fun onPlayerDeath(e: PlayerDeathEvent) {
        try {
            if (e.entity.killer != null) {
                val recipients = PlayerDeathEvent::class.java.getField("recipients").apply { isAccessible = true }.get(e) as MutableList<Player>
                recipients.clear()
                recipients.addAll((pvpAllowed.clone() as ArrayList<UUID>).map { u -> Bukkit.getPlayer(u) }.filter(Objects::nonNull) as List<Player>)
            }
        } catch (e: NoSuchFieldException) {}
    }
}
