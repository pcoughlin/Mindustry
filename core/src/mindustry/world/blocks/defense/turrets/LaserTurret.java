package mindustry.world.blocks.defense.turrets;

import arc.math.*;
import arc.util.*;
import mindustry.entities.bullet.*;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.world.consumers.*;
import mindustry.world.meta.*;
import mindustry.world.meta.values.*;

import static mindustry.Vars.tilesize;

public class LaserTurret extends PowerTurret{
    public float firingMoveFract = 0.25f;
    public float shootDuration = 100f;

    public LaserTurret(String name){
        super(name);
        canOverdrive = false;

        consumes.add(new ConsumeLiquidFilter(liquid -> liquid.temperature <= 0.5f && liquid.flammability < 0.1f, 0.01f)).update(false);
        coolantMultiplier = 1f;
    }

    @Override
    public void setStats(){
        super.setStats();

        stats.remove(BlockStat.booster);
        stats.add(BlockStat.input, new BoosterListValue(reloadTime, consumes.<ConsumeLiquidBase>get(ConsumeType.liquid).amount, coolantMultiplier, false, l -> consumes.liquidfilters.get(l.id)));
        stats.remove(BlockStat.damage);
        //damages every 5 ticks, at least in meltdown's case
        stats.add(BlockStat.damage, shootType.damage * 60f / 5f, StatUnit.perSecond);
    }

    public class LaserTurretEntity extends PowerTurretEntity{
        Bulletc bullet;
        float bulletLife;

        @Override
        public void updateTile(){
            super.updateTile();

            if(bulletLife > 0 && bullet != null){
                tr.trns(rotation, size * tilesize / 2f, 0f);
                bullet.rotation(rotation);
                bullet.set(x + tr.x, y + tr.y);
                bullet.time(0f);
                heat = 1f;
                recoil = recoilAmount;
                bulletLife -= Time.delta();
                if(bulletLife <= 0f){
                    bullet = null;
                }
            }
        }

        @Override
        protected void updateShooting(){
            if(bulletLife > 0 && bullet != null){
                return;
            }

            if(reload >= reloadTime && (consValid() || tile.isEnemyCheat())){
                BulletType type = peekAmmo();

                shoot(type);

                reload = 0f;
            }else{
                Liquid liquid = liquids().current();
                float maxUsed = consumes.<ConsumeLiquidBase>get(ConsumeType.liquid).amount;

                float used = baseReloadSpeed() * (tile.isEnemyCheat() ? maxUsed : Math.min(liquids().get(liquid), maxUsed * Time.delta())) * liquid.heatCapacity * coolantMultiplier;
                reload += used;
                liquids().remove(liquid, used);

                if(Mathf.chance(0.06 * used)){
                    coolEffect.at(x + Mathf.range(size * tilesize / 2f), y + Mathf.range(size * tilesize / 2f));
                }
            }
        }

        @Override
        protected void turnToTarget(float targetRot){
            rotation = Angles.moveToward(rotation, targetRot, rotatespeed * delta() * (bulletLife > 0f ? firingMoveFract : 1f));
        }

        @Override
        protected void bullet(BulletType type, float angle){
            bullet = type.create(tile.entity, team, x + tr.x, y + tr.y, angle);
            bulletLife = shootDuration;
        }

        @Override
        public boolean shouldActiveSound(){
            return bulletLife > 0 && bullet != null;
        }
    }
}
