package com.anticheat.speedhack;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.entity.Player;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.logging.Logger;

public class SpeedHackDetector implements Listener {

    private static final double MAX_SPEED = 0.3; // Limite de vitesse autorisée (en blocs par tick)
    private static final double MAX_ACCELERATION = 0.1; // Limite d'accélération autorisée
    private static final long CHECK_INTERVAL = 50; // Intervalle de vérification en millisecondes
    private HashMap<Player, PlayerMovementData> playerMovementDataMap = new HashMap<>();
    private Logger logger = Logger.getLogger("Minecraft");

    // Quand un joueur bouge
    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();

        if (isPlayerCheating(player)) {
            flagPlayer(player, "Speed hack détecté!");
        }
    }

    // Fonction de détection des cheats
    public boolean isPlayerCheating(Player player) {
        PlayerMovementData data = playerMovementDataMap.get(player);
        if (data == null) {
            data = new PlayerMovementData();
            playerMovementDataMap.put(player, data);
        }

        // Calcul de la vitesse
        double currentSpeed = calculateSpeed(player);
        double currentAcceleration = calculateAcceleration(player);

        // Si la vitesse ou l'accélération dépasse la limite
        if (currentSpeed > MAX_SPEED || currentAcceleration > MAX_ACCELERATION) {
            return true;
        }

        // Vérification des objets custom dans l'inventaire
        if (hasSpeedBoostItems(player)) {
            return false;
        }

        // Mise à jour des données de mouvement
        data.update(player.getLocation(), System.currentTimeMillis());

        // Analyse réseau : détection d'anomalies de positions envoyées par le client
        if (hasSuspiciousNetworkActivity(player)) {
            return true;
        }

        return false;
    }

    // Calcul de la vitesse actuelle du joueur
    private double calculateSpeed(Player player) {
        Vector velocity = player.getVelocity();
        return velocity.length();
    }

    // Calcul de l'accélération basée sur les déplacements successifs
    private double calculateAcceleration(Player player) {
        PlayerMovementData data = playerMovementDataMap.get(player);
        long currentTime = System.currentTimeMillis();

        if (currentTime - data.getLastMoveTime() < CHECK_INTERVAL) {
            return 0;
        }

        double speedChange = calculateSpeed(player) - data.getLastSpeed();
        long timeDifference = currentTime - data.getLastMoveTime();

        // Calcul de l'accélération : changement de vitesse / temps écoulé
        return speedChange / timeDifference;
    }

    // Vérification des items custom dans l'inventaire du joueur (boosters de vitesse)
    private boolean hasSpeedBoostItems(Player player) {
        for (ItemStack item : player.getInventory()) {
            if (item != null && item.getType() == Material.DIAMOND_BOOTS) { // Exemple d'item custom
                return true;
            }
        }
        return false;
    }

    // Vérification des anomalies réseau : positions trop rapides ou incohérentes
    private boolean hasSuspiciousNetworkActivity(Player player) {
        // Exemple de détection : vérifier que la position du joueur ne change pas de manière irréaliste
        Vector currentPosition = player.getLocation().toVector();
        PlayerMovementData data = playerMovementDataMap.get(player);
        
        // Si la position change trop rapidement (plus qu'un certain seuil)
        if (currentPosition.distance(data.getLastPosition()) > 50) {
            logger.warning("Suspicious movement detected from player " + player.getName());
            return true;
        }
        return false;
    }

    // Fonction pour marquer un joueur comme suspect
    private void flagPlayer(Player player, String reason) {
        player.sendMessage("Vous avez été marqué pour un comportement suspect : " + reason);
        // Ajout de la logique pour bannir ou avertir
    }

    // Classe qui stocke les données de mouvement du joueur
    private class PlayerMovementData {
        private long lastMoveTime;
        private double lastSpeed;
        private double lastX;
        private double lastZ;
        private Vector lastPosition;

        public PlayerMovementData() {
            this.lastMoveTime = System.currentTimeMillis();
            this.lastSpeed = 0;
            this.lastX = 0;
            this.lastZ = 0;
            this.lastPosition = new Vector(0, 0, 0);
        }

        public void update(org.bukkit.Location currentLocation, long currentTime) {
            double deltaX = currentLocation.getX() - lastX;
            double deltaZ = currentLocation.getZ() - lastZ;
            this.lastSpeed = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ) / (currentTime - lastMoveTime);

            this.lastX = currentLocation.getX();
            this.lastZ = currentLocation.getZ();
            this.lastPosition = currentLocation.toVector();
            this.lastMoveTime = currentTime;
        }

        public double getLastSpeed() {
            return lastSpeed;
        }

        public long getLastMoveTime() {
            return lastMoveTime;
        }

        public Vector getLastPosition() {
            return lastPosition;
        }
    }

    // Planification de la détection des mouvements sur le long terme
    public void startPeriodicChecks() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : playerMovementDataMap.keySet()) {
                    if (isPlayerCheating(player)) {
                        flagPlayer(player, "Speed hack détecté via vérification périodique");
                    }
                }
            }
        }.runTaskTimer(Main.getPlugin(), 0, CHECK_INTERVAL);
    }
    
    // Analyse de la performance : surveiller les threads et le temps d'exécution
    public void monitorPerformance() {
        new BukkitRunnable() {
            @Override
            public void run() {
                long startTime = System.currentTimeMillis();
                
                // Simuler un contrôle de performance en analysant les données
                long duration = System.currentTimeMillis() - startTime;
                if (duration > 100) {
                    logger.warning("Performance anomaly detected in anti-cheat system: Took too long to check.");
                }
            }
        }.runTaskTimer(Main.getPlugin(), 0, 200); // Exécuter toutes les 10 secondes
    }
}
