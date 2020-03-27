package mindustry.graphics;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.util.*;
import mindustry.*;
import mindustry.gen.*;
import mindustry.input.*;
import mindustry.ui.*;
import mindustry.world.*;

import static mindustry.Vars.*;

public class OverlayRenderer{
    private static final float indicatorLength = 14f;
    private static final float spawnerMargin = tilesize*11f;
    private static final Rect rect = new Rect();
    private float buildFadeTime;

    public void drawBottom(){
        InputHandler input = control.input;

        if(player.dead()) return;

        input.drawBottom();
    }

    public void drawTop(){

        if(Core.settings.getBool("indicators")){
            for(Playerc player : Groups.player){
                if(Vars.player != player && Vars.player.team() == player.team()){
                    if(!rect.setSize(Core.camera.width * 0.9f, Core.camera.height * 0.9f)
                    .setCenter(Core.camera.position.x, Core.camera.position.y).contains(player.x(), player.y())){

                        Tmp.v1.set(player.x(), player.y()).sub(Core.camera.position.x, Core.camera.position.y).setLength(indicatorLength);

                        Lines.stroke(2f, player.team().color);
                        Lines.lineAngle(Core.camera.position.x + Tmp.v1.x, Core.camera.position.y + Tmp.v1.y, Tmp.v1.angle(), 4f);
                        Draw.reset();
                    }
                }
            }

            Groups.unit.each(unit -> {
                if(!unit.isLocal() && unit.team() != player.team() && !rect.setSize(Core.camera.width * 0.9f, Core.camera.height * 0.9f).setCenter(Core.camera.position.x, Core.camera.position.y).contains(unit.x(), unit.y())){
                    Tmp.v1.set(unit.x(), unit.y()).sub(Core.camera.position.x, Core.camera.position.y).setLength(indicatorLength);

                    Lines.stroke(1f, unit.team().color);
                    Lines.lineAngle(Core.camera.position.x + Tmp.v1.x, Core.camera.position.y + Tmp.v1.y, Tmp.v1.angle(), 3f);
                    Draw.reset();
                }
            });

            //mech pads are gone
            /*
            if(ui.hudfrag.blockfrag.currentCategory == Category.upgrade){
                for(Tile mechpad : indexer.getAllied(player.team(), BlockFlag.mechPad)){
                    if(!(mechpad.block() instanceof MechPad)) continue;
                    if(!rect.setSize(Core.camera.width * 0.9f, Core.camera.height * 0.9f)
                            .setCenter(Core.camera.position.x, Core.camera.position.y).contains(mechpad.drawx(), mechpad.drawy())){

                        Tmp.v1.set(mechpad.drawx(), mechpad.drawy()).sub(Core.camera.position.x, Core.camera.position.y).setLength(indicatorLength);

                        Lines.stroke(2f, ((MechPad) mechpad.block()).mech.engineColor);
                        Lines.lineAngle(Core.camera.position.x + Tmp.v1.x, Core.camera.position.y + Tmp.v1.y, Tmp.v1.angle(), 0.5f);
                        Draw.reset();
                    }
                }
            }*/
        }

        if(player.dead()) return; //dead players don't draw

        InputHandler input = control.input;

        //draw config selected block
        if(input.frag.config.isShown()){
            Tilec tile = input.frag.config.getSelectedTile();
            tile.drawConfigure();
        }

        input.drawTop();

        buildFadeTime = Mathf.lerpDelta(buildFadeTime, input.isPlacing() ? 1f : 0f, 0.06f);

        Draw.reset();
        Lines.stroke(buildFadeTime * 2f);

        if(buildFadeTime > 0.005f){
            state.teams.eachEnemyCore(player.team(), core -> {
                float dst = core.dst(player);
                if(dst < state.rules.enemyCoreBuildRadius * 2.2f){
                    Draw.color(Color.darkGray);
                    Lines.circle(core.x(), core.y() - 2, state.rules.enemyCoreBuildRadius);
                    Draw.color(Pal.accent, core.team().color, 0.5f + Mathf.absin(Time.time(), 10f, 0.5f));
                    Lines.circle(core.x(), core.y(), state.rules.enemyCoreBuildRadius);
                }
            });
        }

        Lines.stroke(2f);
        Draw.color(Color.gray, Color.lightGray, Mathf.absin(Time.time(), 8f, 1f));

        for(Tile tile : spawner.getSpawns()){
            if(tile.withinDst(player.x(), player.y(), state.rules.dropZoneRadius + spawnerMargin)){
                Draw.alpha(Mathf.clamp(1f - (player.dst(tile) - state.rules.dropZoneRadius) / spawnerMargin));
                Lines.dashCircle(tile.worldx(), tile.worldy(), state.rules.dropZoneRadius);
            }
        }

        Draw.reset();

        //draw selected block
        if(input.block == null && !Core.scene.hasMouse()){
            Vec2 vec = Core.input.mouseWorld(input.getMouseX(), input.getMouseY());
            Tilec tile = world.entWorld(vec.x, vec.y);

            if(tile != null && tile.team() == player.team()){
                tile.drawSelect();

                if(Core.input.keyDown(Binding.rotateplaced) && tile.block().rotate && tile.interactable(player.team())){
                    control.input.drawArrow(tile.block(), tile.tileX(), tile.tileY(), tile.rotation(), true);
                    Draw.color(Pal.accent, 0.3f + Mathf.absin(4f, 0.2f));
                    Fill.square(tile.x(), tile.y(), tile.block().size * tilesize/2f);
                    Draw.color();
                }
            }
        }

        //draw selection overlay when dropping item
        if(input.isDroppingItem()){
            Vec2 v = Core.input.mouseWorld(input.getMouseX(), input.getMouseY());
            float size = 8;
            Draw.rect(player.unit().item().icon(Cicon.medium), v.x, v.y, size, size);
            Draw.color(Pal.accent);
            Lines.circle(v.x, v.y, 6 + Mathf.absin(Time.time(), 5f, 1f));
            Draw.reset();

            Tilec tile = world.entWorld(v.x, v.y);
            if(tile != null && tile.interactable(player.team()) && tile.acceptStack(player.unit().item(), player.unit().stack().amount, player.unit()) > 0){
                Lines.stroke(3f, Pal.gray);
                Lines.square(tile.x(), tile.y(), tile.block().size * tilesize / 2f + 3 + Mathf.absin(Time.time(), 5f, 1f));
                Lines.stroke(1f, Pal.place);
                Lines.square(tile.x(), tile.y(), tile.block().size * tilesize / 2f + 2 + Mathf.absin(Time.time(), 5f, 1f));
                Draw.reset();

            }
        }
    }
}
