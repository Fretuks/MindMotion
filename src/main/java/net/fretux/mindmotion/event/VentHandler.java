package net.fretux.mindmotion.event;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;

public class VentHandler {

    public static void performVent(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        double radius = 6.0D;
        AABB area = new AABB(
                player.getX() - radius, player.getY() - 2, player.getZ() - radius,
                player.getX() + radius, player.getY() + 2, player.getZ() + radius
        );
        for (Entity e : level.getEntities(player, area, e -> e != player)) {
            double dx = e.getX() - player.getX();
            double dz = e.getZ() - player.getZ();
            double dist = Math.sqrt(dx * dx + dz * dz) + 0.01;
            double strength = 1.6 / dist;
            e.push(dx * strength, 0.3, dz * strength);
            e.hurtMarked = true;
        }
        for (int i = 0; i < 40; i++) {
            double angle = level.random.nextDouble() * Math.PI * 2;
            double speed = 0.3 + level.random.nextDouble() * 0.3;
            double vx = Math.cos(angle) * speed;
            double vz = Math.sin(angle) * speed;
            level.sendParticles(
                    ParticleTypes.SOUL,
                    player.getX(),
                    player.getY() + 1.0,
                    player.getZ(),
                    1,
                    vx, 0.02, vz,
                    0.1
            );
        }
    }
}
